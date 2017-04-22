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

import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Class maintaining a queue of commands which is local to the calling thread.
 * <p>
 * The implementation ensures that recursive commands are broken into commands handled inside a
 * consuming loop, running in the same thread.
 * <p>
 * Created by davide-maestroni on 09/18/2014.
 */
class LocalExecutor {

  private static final int INITIAL_CAPACITY = 10;

  private static final VoidCommand NO_OP = new VoidCommand();

  private static final LocalExecutorThreadLocal sExecutor = new LocalExecutorThreadLocal();

  private long[] mCommandTimeNs;

  private Runnable[] mCommands;

  private TimeUnit[] mDelayUnits;

  private long[] mDelays;

  private int mFirst;

  private boolean mIsRunning;

  private int mLast;

  /**
   * Constructor.
   */
  private LocalExecutor() {
    mCommandTimeNs = new long[INITIAL_CAPACITY];
    mCommands = new Runnable[INITIAL_CAPACITY];
    mDelays = new long[INITIAL_CAPACITY];
    mDelayUnits = new TimeUnit[INITIAL_CAPACITY];
  }

  /**
   * Cancels the specified command if not already run.
   *
   * @param command the command.
   */
  public static void cancel(@NotNull final Runnable command) {
    sExecutor.get().removeCommand(command);
  }

  /**
   * Runs the specified command.
   *
   * @param command  the command.
   * @param delay    the command delay.
   * @param timeUnit the delay time unit.
   */
  public static void run(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    sExecutor.get().addCommand(command, delay, timeUnit);
  }

  private static void resizeArray(@NotNull final long[] src, @NotNull final long[] dst,
      final int first) {
    final int remainder = src.length - first;
    System.arraycopy(src, 0, dst, 0, first);
    System.arraycopy(src, first, dst, dst.length - remainder, remainder);
  }

  private static <T> void resizeArray(@NotNull final T[] src, @NotNull final T[] dst,
      final int first) {
    final int remainder = src.length - first;
    System.arraycopy(src, 0, dst, 0, first);
    System.arraycopy(src, first, dst, dst.length - remainder, remainder);
  }

  private void add(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    final int i = mLast;
    mCommandTimeNs[i] = System.nanoTime();
    mCommands[i] = command;
    mDelays[i] = delay;
    mDelayUnits[i] = timeUnit;
    final int newLast;
    if ((i >= (mCommands.length - 1)) || (i == Integer.MAX_VALUE)) {
      newLast = 0;

    } else {
      newLast = i + 1;
    }

    if (mFirst == newLast) {
      ensureCapacity(mCommands.length + 1);
    }

    mLast = newLast;
  }

  private void addCommand(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    add(command, delay, timeUnit);
    if (!mIsRunning) {
      run();
    }
  }

  private void ensureCapacity(final int capacity) {
    final int size = mCommands.length;
    if (capacity <= size) {
      return;
    }

    int newSize = size;
    while (newSize < capacity) {
      newSize = newSize << 1;
      if (newSize < size) {
        throw new OutOfMemoryError();
      }
    }

    final int first = mFirst;
    final int last = mLast;
    final long[] newCommandTimeNs = new long[newSize];
    resizeArray(mCommandTimeNs, newCommandTimeNs, first);
    final Runnable[] newCommands = new Runnable[newSize];
    resizeArray(mCommands, newCommands, first);
    final long[] newDelays = new long[newSize];
    resizeArray(mDelays, newDelays, first);
    final TimeUnit[] newDelayUnits = new TimeUnit[newSize];
    resizeArray(mDelayUnits, newDelayUnits, first);
    mCommandTimeNs = newCommandTimeNs;
    mCommands = newCommands;
    mDelays = newDelays;
    mDelayUnits = newDelayUnits;
    final int shift = newSize - size;
    mFirst = first + shift;
    mLast = (last < first) ? last : last + shift;
  }

  private void removeCommand(@NotNull final Runnable command) {
    final Runnable[] commands = mCommands;
    final int length = commands.length;
    final int last = mLast;
    int i = mFirst;
    while (i != last) {
      if (commands[i] == command) {
        commands[i] = NO_OP;
        mDelays[i] = 0;
        mDelayUnits[i] = TimeUnit.NANOSECONDS;
      }

      if (++i >= length) {
        i = 0;
      }
    }
  }

  private void run() {
    mIsRunning = true;
    try {
      while (mFirst != mLast) {
        final int i = mFirst;
        final int last = mLast;
        final long[] commandTimeNs = mCommandTimeNs;
        final Runnable[] commands = mCommands;
        final long[] delays = mDelays;
        final TimeUnit[] delayUnits = mDelayUnits;
        long timeNs = commandTimeNs[i];
        Runnable command = commands[i];
        long delay = delays[i];
        TimeUnit delayUnit = delayUnits[i];
        final long currentTimeNs = System.nanoTime();
        long delayNs = timeNs - currentTimeNs + delayUnit.toNanos(delay);
        if (delayNs > 0) {
          final int length = commands.length;
          long minDelay = delayNs;
          int s = i;
          int j = i + 1;
          if (j >= length) {
            j = 0;
          }

          while (j != last) {
            final long nextDelayNs =
                commandTimeNs[j] - currentTimeNs + delayUnits[j].toNanos(delays[j]);
            if (nextDelayNs <= 0) {
              s = j;
              break;
            }

            if (nextDelayNs < minDelay) {
              minDelay = nextDelayNs;
              s = j;
            }

            if (++j >= length) {
              j = 0;
            }
          }

          if (s != i) {
            timeNs = commandTimeNs[s];
            command = commands[s];
            delay = delays[s];
            delayUnit = delayUnits[s];
            commandTimeNs[s] = commandTimeNs[i];
            commands[s] = commands[i];
            delays[s] = delays[i];
            delayUnits[s] = delayUnits[i];
          }

          delayNs = timeNs - System.nanoTime() + delayUnit.toNanos(delay);
        }

        if (delayNs > 0) {
          try {
            DurationMeasure.nanos(delayNs).sleepAtLeast();

          } catch (final InterruptedException e) {
            throw new InterruptedInvocationException(e);
          }
        }

        try {
          command.run();

        } finally {
          // Note that the field values may have changed here
          final int n = mFirst;
          mCommands[n] = null;
          mDelays[n] = 0;
          mDelayUnits[n] = TimeUnit.NANOSECONDS;
          final int newFirst = n + 1;
          mFirst = (newFirst < mCommands.length) ? newFirst : 0;
        }
      }

    } finally {
      mIsRunning = false;
    }
  }

  /**
   * Thread local initializing the queue instance.
   */
  private static class LocalExecutorThreadLocal extends ThreadLocal<LocalExecutor> {

    @Override
    protected LocalExecutor initialValue() {
      return new LocalExecutor();
    }
  }

  /**
   * Void command implementation.
   */
  private static class VoidCommand implements Runnable {

    public void run() {
    }
  }
}
