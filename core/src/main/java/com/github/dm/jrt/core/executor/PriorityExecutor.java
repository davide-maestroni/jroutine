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
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class providing ordering of commands based on priority.
 * <br>
 * Each class instance wraps a supporting executor and then provides different executor instances,
 * each one enqueuing commands with a specific priority.
 * <p>
 * Each enqueued command will age every time an higher priority one takes the precedence, so that
 * older commands slowly increases their priority. Such mechanism has been implemented to avoid
 * starvation of low priority commands. Hence, when assigning priority to different executors, it
 * is important to keep in mind that the difference between two priorities corresponds to the
 * maximum age the lower priority command will have, before getting precedence over the higher
 * priority one.
 * <p>
 * Note that applying a priority to a synchronous executor might make command invocations happen
 * in different threads than the calling one, thus causing the results to be not immediately
 * available.
 * <p>
 * Created by davide-maestroni on 04/28/2015.
 */
public class PriorityExecutor {

  private static final PriorityCommandComparator PRIORITY_COMMAND_COMPARATOR =
      new PriorityCommandComparator();

  private static final WeakIdentityHashMap<ScheduledExecutor, WeakReference<PriorityExecutor>>
      sExecutors = new WeakIdentityHashMap<ScheduledExecutor, WeakReference<PriorityExecutor>>();

  private final AtomicLong mAge = new AtomicLong(Long.MAX_VALUE - Integer.MAX_VALUE);

  private final WeakIdentityHashMap<Runnable, WeakHashMap<PriorityCommand, Void>> mCommands =
      new WeakIdentityHashMap<Runnable, WeakHashMap<PriorityCommand, Void>>();

  private final Map<PriorityCommand, DelayedCommand> mDelayedCommands =
      Collections.synchronizedMap(new HashMap<PriorityCommand, DelayedCommand>());

  private final ScheduledExecutor mExecutor;

  private final WeakHashMap<QueuingExecutor, Void> mExecutors =
      new WeakHashMap<QueuingExecutor, Void>();

  private final Map<PriorityCommand, ImmediateCommand> mImmediateCommands =
      Collections.synchronizedMap(new HashMap<PriorityCommand, ImmediateCommand>());

  private final PriorityBlockingQueue<PriorityCommand> mQueue;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped instance.
   */
  private PriorityExecutor(@NotNull final ScheduledExecutor wrapped) {
    mExecutor = ConstantConditions.notNull("wrapped executor", wrapped);
    mQueue = new PriorityBlockingQueue<PriorityCommand>(10, PRIORITY_COMMAND_COMPARATOR);
  }

  /**
   * Returns the priority executor wrapping the specified one.
   * <p>
   * Note that wrapping a synchronous executor may lead to unpredictable results.
   *
   * @param wrapped the wrapped instance.
   * @return the priority executor.
   */
  @NotNull
  static PriorityExecutor of(@NotNull final ScheduledExecutor wrapped) {
    if (wrapped instanceof QueuingExecutor) {
      return ((QueuingExecutor) wrapped).enclosingExecutor();
    }

    synchronized (sExecutors) {
      final WeakIdentityHashMap<ScheduledExecutor, WeakReference<PriorityExecutor>> executors =
          sExecutors;
      final WeakReference<PriorityExecutor> reference = executors.get(wrapped);
      PriorityExecutor executor = (reference != null) ? reference.get() : null;
      if (executor == null) {
        executor = new PriorityExecutor(wrapped);
        executors.put(wrapped, new WeakReference<PriorityExecutor>(executor));
      }

      return executor;
    }
  }

  private static int compareLong(final long l1, final long l2) {
    return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
  }

  /**
   * Returns an executor enqueuing commands with the specified priority.
   *
   * @param priority the command priority.
   * @return the executor instance.
   */
  @NotNull
  public ScheduledExecutor getExecutor(final int priority) {
    synchronized (mExecutors) {
      final WeakHashMap<QueuingExecutor, Void> executors = mExecutors;
      for (final QueuingExecutor executor : executors.keySet()) {
        if (executor.mPriority == priority) {
          return executor;
        }
      }

      final QueuingExecutor executor = new QueuingExecutor(priority);
      executors.put(executor, null);
      return executor;
    }
  }

  /**
   * Comparator of priority command instances.
   */
  private static class PriorityCommandComparator
      implements Comparator<PriorityCommand>, Serializable {

    // Just don't care...
    private static final long serialVersionUID = -1;

    public int compare(final PriorityCommand e1, final PriorityCommand e2) {
      final int thisPriority = e1.mPriority;
      final long thisAge = e1.mAge;
      final int thatPriority = e2.mPriority;
      final long thatAge = e2.mAge;
      final int compare = compareLong(thatAge + thatPriority, thisAge + thisPriority);
      return (compare == 0) ? compareLong(thatAge, thisAge) : compare;
    }
  }

  /**
   * Runnable implementation delaying the enqueuing of the priority command.
   */
  private class DelayedCommand implements Runnable {

    private final PriorityCommand mCommand;

    /**
     * Constructor.
     *
     * @param command the priority command.
     */
    private DelayedCommand(@NotNull final PriorityCommand command) {
      mCommand = command;
    }

    public void run() {
      final PriorityCommand command = mCommand;
      mDelayedCommands.remove(command);
      final PriorityBlockingQueue<PriorityCommand> queue = mQueue;
      queue.put(command);
      final PriorityCommand priorityCommand = queue.poll();
      if (priorityCommand != null) {
        priorityCommand.run();
      }
    }
  }

  /**
   * Runnable implementation handling the immediate enqueuing of the priority command.
   */
  private class ImmediateCommand implements Runnable {

    private final PriorityCommand mCommand;

    /**
     * Constructor.
     *
     * @param command the priority command.
     */
    private ImmediateCommand(@NotNull final PriorityCommand command) {
      mCommand = command;
    }

    public void run() {
      mImmediateCommands.remove(mCommand);
      final PriorityCommand priorityCommand = mQueue.poll();
      if (priorityCommand != null) {
        priorityCommand.run();
      }
    }
  }

  /**
   * Runnable implementation providing a comparison based on priority and the wrapped command
   * age.
   */
  private class PriorityCommand implements Runnable {

    private final long mAge;

    private final Runnable mCommand;

    private final int mPriority;

    /**
     * Constructor.
     *
     * @param command  the wrapped command.
     * @param priority the command priority.
     * @param age      the command age.
     */
    private PriorityCommand(@NotNull final Runnable command, final int priority, final long age) {
      mCommand = command;
      mPriority = priority;
      mAge = age;
    }

    public void run() {
      final Runnable command = mCommand;
      synchronized (mCommands) {
        final WeakIdentityHashMap<Runnable, WeakHashMap<PriorityCommand, Void>> commands =
            mCommands;
        final WeakHashMap<PriorityCommand, Void> priorityCommands = commands.get(command);
        if (priorityCommands != null) {
          priorityCommands.remove(this);
          if (priorityCommands.isEmpty()) {
            commands.remove(command);
          }
        }
      }

      command.run();
    }
  }

  /**
   * Enqueuing executor implementation.
   */
  private class QueuingExecutor extends ScheduledExecutorDecorator {

    private final int mPriority;

    /**
     * Constructor.
     *
     * @param priority the command priority.
     */
    private QueuingExecutor(final int priority) {
      super(mExecutor);
      mPriority = priority;
    }

    @Override
    public void cancel(@NotNull final Runnable command) {
      final WeakHashMap<PriorityCommand, Void> priorityCommands;
      synchronized (mCommands) {
        priorityCommands = mCommands.remove(command);
      }

      if (priorityCommands != null) {
        final PriorityBlockingQueue<PriorityCommand> queue = mQueue;
        final Map<PriorityCommand, ImmediateCommand> immediateCommands = mImmediateCommands;
        final Map<PriorityCommand, DelayedCommand> delayedCommands = mDelayedCommands;
        for (final PriorityCommand priorityCommand : priorityCommands.keySet()) {
          if (queue.remove(priorityCommand)) {
            final ImmediateCommand immediateCommand = immediateCommands.remove(priorityCommand);
            if (immediateCommand != null) {
              super.cancel(immediateCommand);
            }

          } else {
            final DelayedCommand delayedCommand = delayedCommands.remove(priorityCommand);
            if (delayedCommand != null) {
              super.cancel(delayedCommand);
            }
          }
        }
      }
    }

    @Override
    public void execute(@NotNull final Runnable command) {
      final PriorityCommand priorityCommand = getPriorityCommand(command);
      final ImmediateCommand immediateCommand = new ImmediateCommand(priorityCommand);
      mImmediateCommands.put(priorityCommand, immediateCommand);
      mQueue.put(priorityCommand);
      super.execute(immediateCommand);
    }

    @Override
    public void execute(@NotNull final Runnable command, final long delay,
        @NotNull final TimeUnit timeUnit) {
      final PriorityCommand priorityCommand = getPriorityCommand(command);
      if (delay == 0) {
        final ImmediateCommand immediateCommand = new ImmediateCommand(priorityCommand);
        mImmediateCommands.put(priorityCommand, immediateCommand);
        mQueue.put(priorityCommand);
        super.execute(immediateCommand, 0, timeUnit);

      } else {
        final DelayedCommand delayedCommand = new DelayedCommand(priorityCommand);
        mDelayedCommands.put(priorityCommand, delayedCommand);
        super.execute(delayedCommand, delay, timeUnit);
      }
    }

    @NotNull
    private PriorityExecutor enclosingExecutor() {
      return PriorityExecutor.this;
    }

    @NotNull
    private PriorityCommand getPriorityCommand(@NotNull final Runnable command) {
      final PriorityCommand priorityCommand =
          new PriorityCommand(command, mPriority, mAge.getAndDecrement());
      synchronized (mCommands) {
        final WeakIdentityHashMap<Runnable, WeakHashMap<PriorityCommand, Void>> commands =
            mCommands;
        WeakHashMap<PriorityCommand, Void> priorityCommands = commands.get(command);
        if (priorityCommands == null) {
          priorityCommands = new WeakHashMap<PriorityCommand, Void>();
          commands.put(command, priorityCommands);
        }

        priorityCommands.put(priorityCommand, null);
      }

      return priorityCommand;
    }
  }
}
