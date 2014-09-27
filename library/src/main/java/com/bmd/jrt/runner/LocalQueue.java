/**
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
package com.bmd.jrt.runner;

import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.fromUnit;
import static com.bmd.jrt.time.TimeDuration.nanos;

/**
 * Class maintaining a queue of invocations which is local to the calling thread.<br/>
 * The implementation ensures that recursive invocations are broken into a consuming loop running
 * in the same thread.
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

    private int mFirst;

    private long[] mInvocationTimeNs;

    private Invocation[] mInvocations;

    private boolean mIsRunning;

    private int mLast;

    private InvocationType[] mTypes;

    /**
     * Default constructor.
     */
    private LocalQueue() {

        mInvocationTimeNs = new long[INITIAL_CAPACITY];
        mInvocations = new Invocation[INITIAL_CAPACITY];
        mDelays = new TimeDuration[INITIAL_CAPACITY];
        mTypes = new InvocationType[INITIAL_CAPACITY];
    }

    /**
     * Runs the specified invocation.
     *
     * @param invocation the invocation.
     * @param delay      the execution delay.
     * @param timeUnit   the delay time unit.
     */
    public static void run(final Invocation invocation, final long delay, final TimeUnit timeUnit) {

        sQueue.get().addRun(invocation, delay, timeUnit);
    }

    /**
     * Runs the specified abort invocation.
     *
     * @param invocation the invocation.
     */
    public static void runAbort(final Invocation invocation) {

        sQueue.get().addAbort(invocation);
    }

    private static <T> void resizeArray(final T[] src, final T[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final long[] src, final long[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private void add(final Invocation invocation, TimeDuration delay, InvocationType type) {

        if (mInvocations.length == 0) {

            mInvocationTimeNs = new long[1];
            mInvocations = new Invocation[1];
            mDelays = new TimeDuration[1];
            mTypes = new InvocationType[1];
        }

        final int i = mLast;

        mInvocationTimeNs[i] = System.nanoTime();
        mInvocations[i] = invocation;
        mDelays[i] = delay;
        mTypes[i] = type;

        final int newLast;

        if ((i >= (mInvocations.length - 1)) || (i == Integer.MAX_VALUE)) {

            newLast = 0;

        } else {

            newLast = i + 1;
        }

        if (mFirst == newLast) {

            ensureCapacity(mInvocations.length + 1);
        }

        mLast = newLast;
    }

    private void addAbort(final Invocation invocation) {

        add(invocation, ZERO, InvocationType.ABORT);

        if (!mIsRunning) {

            run();
        }
    }

    private void addRun(final Invocation invocation, final long delay, final TimeUnit timeUnit) {

        add(invocation, fromUnit(delay, timeUnit), InvocationType.RUN);

        if (!mIsRunning) {

            run();
        }
    }

    private void ensureCapacity(final int capacity) {

        final int size = mInvocations.length;

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

        final long[] newInvocationTimeNs = new long[newSize];
        resizeArray(mInvocationTimeNs, newInvocationTimeNs, first);

        final Invocation[] newInvocations = new Invocation[newSize];
        resizeArray(mInvocations, newInvocations, first);

        final TimeDuration[] newDelays = new TimeDuration[newSize];
        resizeArray(mDelays, newDelays, first);

        final InvocationType[] newTypes = new InvocationType[newSize];
        resizeArray(mTypes, newTypes, first);

        mInvocationTimeNs = newInvocationTimeNs;
        mInvocations = newInvocations;
        mDelays = newDelays;
        mTypes = newTypes;

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
                final long[] invocationTimeNs = mInvocationTimeNs;
                final Invocation[] invocations = mInvocations;
                final TimeDuration[] delays = mDelays;
                final InvocationType[] types = mTypes;

                long timeNs = invocationTimeNs[i];
                Invocation invocation = invocations[i];
                TimeDuration delay = delays[i];
                InvocationType type = types[i];

                final long currentTimeNs = System.nanoTime();
                long delayNs = timeNs - currentTimeNs + delay.toNanos();

                if (delayNs > 0) {

                    final int length = invocations.length;
                    long minDelay = delayNs;
                    int s = i;

                    int j = i + 1;

                    if (j >= length) {

                        j = 0;
                    }

                    while (j != last) {

                        final long nextDelayNs =
                                invocationTimeNs[j] - currentTimeNs + delays[j].toNanos();

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

                        timeNs = invocationTimeNs[s];
                        invocation = invocations[s];
                        delay = delays[s];
                        type = types[s];

                        invocationTimeNs[s] = invocationTimeNs[i];
                        invocations[s] = invocations[i];
                        delays[s] = delays[i];
                        types[s] = types[i];
                    }

                    delayNs = timeNs - System.nanoTime() + delay.toNanos();
                }

                if (delayNs > 0) {

                    try {

                        nanos(delayNs).sleepAtLeast();

                    } catch (final InterruptedException e) {

                        RoutineInterruptedException.interrupt(e);
                    }
                }

                try {

                    // This call could be re-entrant
                    if (type == InvocationType.RUN) {

                        invocation.run();

                    } else {

                        invocation.abort();
                    }

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }

                // Note that the field values may have changed here
                final int n = mFirst;

                mInvocations[n] = null;
                mDelays[n] = null;

                final int newFirst = mFirst + 1;

                if (newFirst >= mInvocations.length) {

                    mFirst = 0;

                } else {

                    mFirst = newFirst;
                }
            }

        } finally {

            mIsRunning = false;
        }
    }

    private enum InvocationType {

        RUN,
        ABORT
    }
}