/*
 * Copyright 2017 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.executor;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.core.util.SimpleQueue.SimpleQueueIterator;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Executor implementation throttling the number of running commands so to keep it under a
 * specified limit.
 * <p>
 * Note that, in case the executor is backed by a synchronous one, it is possible that commands
 * are run on threads different from the calling one, so that the results will be not immediately
 * available.
 * <p>
 * Created by davide-maestroni on 07/18/2015.
 */
class ThrottlingExecutor extends ScheduledExecutorDecorator {

  private final WeakIdentityHashMap<Runnable, WeakReference<ThrottlingCommand>> mCommands =
      new WeakIdentityHashMap<Runnable, WeakReference<ThrottlingCommand>>();

  private final int mMaxRunning;

  private final Object mMutex = new Object();

  private final SimpleQueue<PendingCommand> mQueue = new SimpleQueue<PendingCommand>();

  private final VoidPendingCommand mVoidCommand = new VoidPendingCommand();

  private int mRunningCount;

  /**
   * Constructor.
   *
   * @param wrapped     the wrapped instance.
   * @param maxCommands the maximum number of running commands.
   * @throws java.lang.IllegalArgumentException if the specified max number is less than 1.
   */
  ThrottlingExecutor(@NotNull final ScheduledExecutor wrapped, final int maxCommands) {
    super(wrapped);
    mMaxRunning = ConstantConditions.positive("maximum number of running commands", maxCommands);
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    ThrottlingCommand throttlingCommand = null;
    synchronized (mMutex) {
      final SimpleQueueIterator<PendingCommand> iterator = mQueue.iterator();
      while (iterator.hasNext()) {
        final PendingCommand pendingCommand = iterator.next();
        if (pendingCommand.mCommand == command) {
          iterator.replace(mVoidCommand);
        }
      }

      final WeakReference<ThrottlingCommand> commandReference = mCommands.get(command);
      if (commandReference != null) {
        throttlingCommand = commandReference.get();
      }
    }

    if (throttlingCommand != null) {
      super.cancel(throttlingCommand);
    }
  }

  @Override
  public void execute(@NotNull final Runnable command) {
    final ThrottlingCommand throttlingCommand;
    synchronized (mMutex) {
      final SimpleQueue<PendingCommand> queue = mQueue;
      if ((mRunningCount + queue.size()) >= mMaxRunning) {
        queue.add(new PendingCommand(command, 0, TimeUnit.MILLISECONDS));
        return;
      }

      throttlingCommand = getThrottlingCommand(command);
    }

    super.execute(throttlingCommand);
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    final ThrottlingCommand throttlingCommand;
    synchronized (mMutex) {
      final SimpleQueue<PendingCommand> queue = mQueue;
      if ((mRunningCount + queue.size()) >= mMaxRunning) {
        queue.add(new PendingCommand(command, delay, timeUnit));
        return;
      }

      throttlingCommand = getThrottlingCommand(command);
    }

    super.execute(throttlingCommand, delay, timeUnit);
  }

  @NotNull
  private ThrottlingCommand getThrottlingCommand(@NotNull final Runnable command) {
    final WeakIdentityHashMap<Runnable, WeakReference<ThrottlingCommand>> commands = mCommands;
    final WeakReference<ThrottlingCommand> commandReference = commands.get(command);
    ThrottlingCommand throttlingCommand =
        (commandReference != null) ? commandReference.get() : null;
    if (throttlingCommand == null) {
      throttlingCommand = new ThrottlingCommand(command);
      commands.put(command, new WeakReference<ThrottlingCommand>(throttlingCommand));
    }

    return throttlingCommand;
  }

  /**
   * Void command implementation.
   */
  private static class VoidCommand implements Runnable {

    public void run() {
    }
  }

  /**
   * Pending command implementation.
   */
  private class PendingCommand implements Runnable {

    private final Runnable mCommand;

    private final long mDelay;

    private final long mStartTimeMillis;

    private final TimeUnit mTimeUnit;

    /**
     * Constructor.
     *
     * @param command  the command.
     * @param delay    the command delay.
     * @param timeUnit the delay time unit.
     */
    private PendingCommand(@NotNull final Runnable command, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mCommand = command;
      mDelay = delay;
      mTimeUnit = timeUnit;
      mStartTimeMillis = System.currentTimeMillis();
    }

    public void run() {
      final ThrottlingCommand throttlingCommand;
      synchronized (mMutex) {
        throttlingCommand = getThrottlingCommand(mCommand);
      }

      final long delay = mDelay;
      ThrottlingExecutor.super.execute(throttlingCommand, (delay == 0) ? 0
              : Math.max(mTimeUnit.toMillis(delay) + mStartTimeMillis - System.currentTimeMillis
                  (), 0),
          TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Runnable used to dequeue and run pending commands, when the maximum running count allows
   * it.
   */
  private class ThrottlingCommand implements Runnable {

    private final Runnable mCommand;

    /**
     * Constructor.
     *
     * @param command the command.
     */
    private ThrottlingCommand(@NotNull final Runnable command) {
      mCommand = command;
    }

    public void run() {
      final Runnable command = mCommand;
      final SimpleQueue<PendingCommand> queue = mQueue;
      synchronized (mMutex) {
        if (mRunningCount >= mMaxRunning) {
          queue.addFirst(new PendingCommand(command, 0, TimeUnit.MILLISECONDS));
          return;
        }

        ++mRunningCount;
      }

      try {
        command.run();

      } finally {
        PendingCommand pendingCommand = null;
        synchronized (mMutex) {
          --mRunningCount;
          if (!queue.isEmpty()) {
            pendingCommand = queue.removeFirst();
          }
        }

        if (pendingCommand != null) {
          pendingCommand.run();
        }
      }
    }
  }

  /**
   * Void pending command implementation.
   */
  private class VoidPendingCommand extends PendingCommand {

    /**
     * Constructor.
     */
    private VoidPendingCommand() {
      super(new VoidCommand(), 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
      final SimpleQueue<PendingCommand> queue = mQueue;
      PendingCommand pendingCommand = null;
      synchronized (mMutex) {
        if (!queue.isEmpty()) {
          pendingCommand = queue.removeFirst();
        }
      }

      if (pendingCommand != null) {
        pendingCommand.run();
      }
    }
  }
}
