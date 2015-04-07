/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.runner;

import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;
import static com.gh.bmd.jrt.time.TimeDuration.nanos;

/**
 * Class maintaining a queue of executions which is local to the calling thread.
 * <p/>
 * The implementation ensures that recursive executions are broken into commands handled inside a
 * consuming loop, running in the same thread.
 * <p/>
 * Created by davide on 9/18/14.
 */
class LocalQueue {

    private static final int INITIAL_CAPACITY = 10;

    private static final ThreadLocal<LocalQueue> sQueue = new ThreadLocal<LocalQueue>() {

        @Override
        protected LocalQueue initialValue() {

            return new LocalQueue();
        }
    };

    private TimeDuration[] mDelays;

    private long[] mExecutionTimeNs;

    private Execution[] mExecutions;

    private int mFirst;

    private boolean mIsRunning;

    private int mLast;

    /**
     * Constructor.
     */
    private LocalQueue() {

        mExecutionTimeNs = new long[INITIAL_CAPACITY];
        mExecutions = new Execution[INITIAL_CAPACITY];
        mDelays = new TimeDuration[INITIAL_CAPACITY];
    }

    /**
     * Runs the specified execution.
     *
     * @param execution the execution.
     * @param delay     the execution delay.
     * @param timeUnit  the delay time unit.
     */
    public static void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        sQueue.get().addExecution(execution, delay, timeUnit);
    }

    private static <T> void resizeArray(@Nonnull final T[] src, @Nonnull final T[] dst,
            final int first) {

        final int remainder = src.length - first;
        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(@Nonnull final long[] src, @Nonnull final long[] dst,
            final int first) {

        final int remainder = src.length - first;
        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private void add(@Nonnull final Execution execution, @Nonnull final TimeDuration delay) {

        final int i = mLast;
        mExecutionTimeNs[i] = System.nanoTime();
        mExecutions[i] = execution;
        mDelays[i] = delay;
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

    private void addExecution(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        add(execution, fromUnit(delay, timeUnit));

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
        final TimeDuration[] newDelays = new TimeDuration[newSize];
        resizeArray(mDelays, newDelays, first);
        mExecutionTimeNs = newExecutionTimeNs;
        mExecutions = newExecutions;
        mDelays = newDelays;
        final int shift = newSize - size;
        mFirst = first + shift;
        mLast = (last < first) ? last : last + shift;
    }

    private void run() {

        mIsRunning = true;

        try {

            while (mFirst != mLast) {

                final int i = mFirst;
                final int last = mLast;
                final long[] executionTimeNs = mExecutionTimeNs;
                final Execution[] executions = mExecutions;
                final TimeDuration[] delays = mDelays;
                long timeNs = executionTimeNs[i];
                Execution execution = executions[i];
                TimeDuration delay = delays[i];
                final long currentTimeNs = System.nanoTime();
                long delayNs = timeNs - currentTimeNs + delay.toNanos();

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
                                executionTimeNs[j] - currentTimeNs + delays[j].toNanos();

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
                        executionTimeNs[s] = executionTimeNs[i];
                        executions[s] = executions[i];
                        delays[s] = delays[i];
                    }

                    delayNs = timeNs - System.nanoTime() + delay.toNanos();
                }

                if (delayNs > 0) {

                    try {

                        nanos(delayNs).sleepAtLeast();

                    } catch (final InterruptedException e) {

                        throw new InvocationInterruptedException(e);
                    }
                }

                try {

                    // This call could be re-entrant
                    execution.run();

                } catch (final InvocationInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }

                // Note that the field values may have changed here
                final int n = mFirst;
                mExecutions[n] = null;
                mDelays[n] = null;
                final int newFirst = mFirst + 1;

                if (newFirst >= mExecutions.length) {

                    mFirst = 0;

                } else {

                    mFirst = newFirst;
                }
            }

        } finally {

            mIsRunning = false;
        }
    }
}
