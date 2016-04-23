/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.core.runner;

import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Class maintaining a queue of executions which is local to the calling thread.
 * <p>
 * The implementation ensures that recursive executions are broken into commands handled inside a
 * consuming loop, running in the same thread.
 * <p>
 * Created by davide-maestroni on 09/18/2014.
 */
class LocalRunner {

    private static final EmptyExecution EMPTY_EXECUTION = new EmptyExecution();

    private static final int INITIAL_CAPACITY = 10;

    private static final LocalRunnerThreadLocal sRunner = new LocalRunnerThreadLocal();

    private TimeUnit[] mDelayUnits;

    private long[] mDelays;

    private long[] mExecutionTimeNs;

    private Execution[] mExecutions;

    private int mFirst;

    private boolean mIsRunning;

    private int mLast;

    /**
     * Constructor.
     */
    private LocalRunner() {

        mExecutionTimeNs = new long[INITIAL_CAPACITY];
        mExecutions = new Execution[INITIAL_CAPACITY];
        mDelays = new long[INITIAL_CAPACITY];
        mDelayUnits = new TimeUnit[INITIAL_CAPACITY];
    }

    /**
     * Cancels the specified execution if not already run.
     *
     * @param execution the execution.
     */
    public static void cancel(@NotNull final Execution execution) {

        sRunner.get().removeExecution(execution);
    }

    /**
     * Runs the specified execution.
     *
     * @param execution the execution.
     * @param delay     the execution delay.
     * @param timeUnit  the delay time unit.
     */
    public static void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        sRunner.get().addExecution(execution, delay, timeUnit);
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

    private void add(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        final int i = mLast;
        mExecutionTimeNs[i] = System.nanoTime();
        mExecutions[i] = execution;
        mDelays[i] = delay;
        mDelayUnits[i] = timeUnit;
        final int newLast;
        if ((i >= (mExecutions.length - 1)) || (i == Integer.MAX_VALUE)) {
            newLast = 0;

        } else {
            newLast = i + 1;
        }

        if (mFirst == newLast) {
            ensureCapacity(mExecutions.length + 1);
        }

        mLast = newLast;
    }

    private void addExecution(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        add(execution, delay, timeUnit);
        if (!mIsRunning) {
            run();
        }
    }

    private void ensureCapacity(final int capacity) {

        final int size = mExecutions.length;
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
        final long[] newExecutionTimeNs = new long[newSize];
        resizeArray(mExecutionTimeNs, newExecutionTimeNs, first);
        final Execution[] newExecutions = new Execution[newSize];
        resizeArray(mExecutions, newExecutions, first);
        final long[] newDelays = new long[newSize];
        resizeArray(mDelays, newDelays, first);
        final TimeUnit[] newDelayUnits = new TimeUnit[newSize];
        resizeArray(mDelayUnits, newDelayUnits, first);
        mExecutionTimeNs = newExecutionTimeNs;
        mExecutions = newExecutions;
        mDelays = newDelays;
        mDelayUnits = newDelayUnits;
        final int shift = newSize - size;
        mFirst = first + shift;
        mLast = (last < first) ? last : last + shift;
    }

    private void removeExecution(@NotNull final Execution execution) {

        final Execution[] executions = mExecutions;
        final int length = executions.length;
        final int last = mLast;
        int i = mFirst;
        while (i != last) {
            if (executions[i] == execution) {
                executions[i] = EMPTY_EXECUTION;
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
                final long[] executionTimeNs = mExecutionTimeNs;
                final Execution[] executions = mExecutions;
                final long[] delays = mDelays;
                final TimeUnit[] delayUnits = mDelayUnits;
                long timeNs = executionTimeNs[i];
                Execution execution = executions[i];
                long delay = delays[i];
                TimeUnit delayUnit = delayUnits[i];
                final long currentTimeNs = System.nanoTime();
                long delayNs = timeNs - currentTimeNs + delayUnit.toNanos(delay);
                if (delayNs > 0) {
                    final int length = executions.length;
                    long minDelay = delayNs;
                    int s = i;
                    int j = i + 1;
                    if (j >= length) {
                        j = 0;
                    }

                    while (j != last) {
                        final long nextDelayNs =
                                executionTimeNs[j] - currentTimeNs + delayUnits[j].toNanos(
                                        delays[j]);
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
                        timeNs = executionTimeNs[s];
                        execution = executions[s];
                        delay = delays[s];
                        delayUnit = delayUnits[s];
                        executionTimeNs[s] = executionTimeNs[i];
                        executions[s] = executions[i];
                        delays[s] = delays[i];
                        delayUnits[s] = delayUnits[i];
                    }

                    delayNs = timeNs - System.nanoTime() + delayUnit.toNanos(delay);
                }

                if (delayNs > 0) {
                    try {
                        UnitDuration.nanos(delayNs).sleepAtLeast();

                    } catch (final InterruptedException e) {
                        throw new InvocationInterruptedException(e);
                    }
                }

                try {
                    execution.run();

                } finally {
                    // Note that the field values may have changed here
                    final int n = mFirst;
                    mExecutions[n] = null;
                    mDelays[n] = 0;
                    mDelayUnits[n] = TimeUnit.NANOSECONDS;
                    final int newFirst = n + 1;
                    mFirst = (newFirst < mExecutions.length) ? newFirst : 0;
                }
            }

        } finally {
            mIsRunning = false;
        }
    }

    /**
     * Empty execution implementation.
     */
    private static class EmptyExecution implements Execution {

        public void run() {

        }
    }

    /**
     * Thread local initializing the queue instance.
     */
    private static class LocalRunnerThreadLocal extends ThreadLocal<LocalRunner> {

        @Override
        protected LocalRunner initialValue() {

            return new LocalRunner();
        }
    }
}
