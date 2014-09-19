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

import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.concurrent.TimeUnit;

/**
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

    private long[] mCallTimeNs;

    private Call[] mCalls;

    private TimeDuration[] mDelays;

    private int mFirst;

    private boolean[] mInputs;

    private boolean mIsRunning;

    private int mLast;

    /**
     * Default constructor.
     */
    private LocalQueue() {

        mCallTimeNs = new long[INITIAL_CAPACITY];
        mCalls = new Call[INITIAL_CAPACITY];
        mDelays = new TimeDuration[INITIAL_CAPACITY];
        mInputs = new boolean[INITIAL_CAPACITY];
    }

    public static void input(final Call call, final long delay, final TimeUnit timeUnit) {

        sQueue.get().runInput(call, delay, timeUnit);
    }

    public static void reset(final Call call) {

        sQueue.get().runReset(call);
    }

    private static void resizeArray(final Call[] src, final Call[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final TimeDuration[] src, final TimeDuration[] dst,
            final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final boolean[] src, final boolean[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private static void resizeArray(final long[] src, final long[] dst, final int first) {

        final int remainder = src.length - first;

        System.arraycopy(src, 0, dst, 0, first);
        System.arraycopy(src, first, dst, dst.length - remainder, remainder);
    }

    private void add(final Call call, TimeDuration delay, boolean isInput) {

        if (mCalls.length == 0) {

            mCallTimeNs = new long[1];
            mCalls = new Call[1];
            mDelays = new TimeDuration[1];
            mInputs = new boolean[1];
        }

        final int i = mLast;

        mCallTimeNs[i] = System.nanoTime();
        mCalls[i] = call;
        mDelays[i] = delay;
        mInputs[i] = isInput;

        final int newLast;

        if ((i >= (mCalls.length - 1)) || (i == Integer.MAX_VALUE)) {

            newLast = 0;

        } else {

            newLast = i + 1;
        }

        if (mFirst == newLast) {

            ensureCapacity(mCalls.length + 1);
        }

        mLast = newLast;
    }

    private void ensureCapacity(final int capacity) {

        final int size = mCalls.length;

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

        final long[] newCallTimeNs = new long[newSize];
        resizeArray(mCallTimeNs, newCallTimeNs, first);

        final Call[] newCalls = new Call[newSize];
        resizeArray(mCalls, newCalls, first);

        final TimeDuration[] newDelays = new TimeDuration[newSize];
        resizeArray(mDelays, newDelays, first);

        final boolean[] newInputs = new boolean[newSize];
        resizeArray(mInputs, newInputs, first);

        mCallTimeNs = newCallTimeNs;
        mCalls = newCalls;
        mDelays = newDelays;
        mInputs = newInputs;

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
                final long[] callTimeNs = mCallTimeNs;
                final Call[] calls = mCalls;
                final TimeDuration[] delays = mDelays;
                final boolean[] inputs = mInputs;

                long timeNs = callTimeNs[i];
                Call call = calls[i];
                TimeDuration delay = delays[i];
                boolean input = inputs[i];

                final long currentTimeNs = System.nanoTime();
                long delayNs = timeNs - currentTimeNs + delay.toNanos();

                if (delayNs > 0) {

                    final int length = calls.length;
                    long minDelay = delayNs;
                    int s = i;

                    int j = i + 1;

                    if (j >= length) {

                        j = 0;
                    }

                    while (j != last) {

                        final long nextDelayNs =
                                callTimeNs[j] - currentTimeNs + delays[j].toNanos();

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

                        timeNs = callTimeNs[s];
                        call = calls[s];
                        delay = delays[s];
                        input = inputs[s];

                        callTimeNs[s] = callTimeNs[i];
                        calls[s] = calls[i];
                        delays[s] = delays[i];
                        inputs[s] = inputs[i];
                    }

                    delayNs = timeNs - System.nanoTime() + delay.toNanos();
                }

                if (delayNs > 0) {

                    try {

                        TimeDuration.nanos(delayNs).sleepAtLeast();

                    } catch (final InterruptedException e) {

                        RoutineInterruptedException.interrupt(e);
                    }
                }

                try {

                    // This call could be re-entrant
                    if (input) {

                        call.onInput();

                    } else {

                        call.onReset();
                    }

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }

                // Note that the field values may have changed here
                final int n = mFirst;

                mCalls[n] = null;
                mDelays[n] = null;

                final int newFirst = mFirst + 1;

                if (newFirst >= mCalls.length) {

                    mFirst = 0;

                } else {

                    mFirst = newFirst;
                }
            }

        } finally {

            mIsRunning = false;
        }
    }

    private void runInput(final Call call, final long delay, final TimeUnit timeUnit) {

        add(call, TimeDuration.from(delay, timeUnit), true);

        if (!mIsRunning) {

            run();
        }
    }

    private void runReset(final Call call) {

        add(call, TimeDuration.ZERO, false);

        if (!mIsRunning) {

            run();
        }
    }
}