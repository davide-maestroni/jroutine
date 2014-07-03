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
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.River;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * This class maintains a queue of commands in a lightweight circular buffer structure and executes
 * them in a non-recursive way.<br/>
 * It is used to ensure that commands issued inside a fall are executed later in the expected
 * order.
 * <p/>
 * Created by davide on 6/7/14.
 */
class DataLock {

    private static final int DEFAULT_INITIAL_CAPACITY = 10;

    private static final Fluid DISCHARGE = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drop) {

            if (streamNumber < 0) {

                river.discharge();

            } else {

                river.dischargeStream(streamNumber);
            }
        }
    };

    private static final Fluid FORWARD = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drop) {

            //noinspection unchecked
            final Throwable exception = (Throwable) drop;

            if (streamNumber < 0) {

                river.forward(exception);

            } else {

                river.forwardStream(streamNumber, exception);
            }
        }
    };

    private static final Fluid PUSH = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drop) {

            //noinspection unchecked
            final DATA out = (DATA) drop;

            if (streamNumber < 0) {

                river.push(out);

            } else {

                river.pushStream(streamNumber, out);
            }
        }
    };

    private static final Fluid PUSH_AFTER = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drop) {


            //noinspection unchecked
            final DATA out = (DATA) drop;

            final long currentDelay = updateDelay(delay, timeUnit, pushTimeNs);

            if (streamNumber < 0) {

                river.pushAfter(currentDelay, timeUnit, out);

            } else {

                river.pushStreamAfter(streamNumber, currentDelay, timeUnit, out);
            }
        }
    };

    private static final Fluid PUSH_AFTER_ARRAY = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drops) {

            //noinspection unchecked
            final DATA[] outs = (DATA[]) drops;

            final long currentDelay = updateDelay(delay, timeUnit, pushTimeNs);

            if (streamNumber < 0) {

                //noinspection unchecked
                river.pushAfter(currentDelay, timeUnit, outs);

            } else {

                //noinspection unchecked
                river.pushStreamAfter(streamNumber, currentDelay, timeUnit, outs);
            }
        }
    };

    private static final Fluid PUSH_AFTER_ITERABLE = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drops) {

            //noinspection unchecked
            final Iterable<DATA> outs = (Iterable<DATA>) drops;

            final long currentDelay = updateDelay(delay, timeUnit, pushTimeNs);

            if (streamNumber < 0) {

                river.pushAfter(currentDelay, timeUnit, outs);

            } else {

                river.pushStreamAfter(streamNumber, currentDelay, timeUnit, outs);
            }
        }
    };

    private static final Fluid PUSH_ARRAY = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drops) {

            //noinspection unchecked
            final DATA[] outs = (DATA[]) drops;

            if (streamNumber < 0) {

                //noinspection unchecked
                river.push(outs);

            } else {

                //noinspection unchecked
                river.pushStream(streamNumber, outs);
            }
        }
    };

    private static final Fluid PUSH_ITERABLE = new Fluid() {

        @Override
        public <DATA> void push(final River<?, DATA> river, final int streamNumber,
                final long delay, final TimeUnit timeUnit, final long pushTimeNs,
                final Object drops) {

            //noinspection unchecked
            final Iterable<DATA> outs = (Iterable<DATA>) drops;

            if (streamNumber < 0) {

                river.push(outs);

            } else {

                river.pushStream(streamNumber, outs);
            }
        }
    };

    private Object[] mData;

    private long[] mDelays;

    private int mFirst;

    private Fluid[] mFluids;

    private int mLast;

    private boolean mPumping;

    private long[] mPushTimeNs;

    private River<?, ?>[] mRivers;

    private int[] mStreamNumbers;

    private TimeUnit[] mTimeUnits;

    /**
     * Default constructor.
     */
    public DataLock() {

        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructor.
     *
     * @param initialCapacity The initial capacity.
     */
    public DataLock(final int initialCapacity) {

        mFluids = new Fluid[initialCapacity];
        mRivers = new River[initialCapacity];
        mStreamNumbers = new int[initialCapacity];
        mDelays = new long[initialCapacity];
        mTimeUnits = new TimeUnit[initialCapacity];
        mPushTimeNs = new long[initialCapacity];
        mData = new Object[initialCapacity];
    }

    private static long updateDelay(final long delay, final TimeUnit timeUnit,
            final long pushTimeNs) {

        if (delay <= 0) {

            return 0;
        }

        return (delay - timeUnit.convert(System.nanoTime() - pushTimeNs, TimeUnit.NANOSECONDS));
    }

    public void discharge(final River<?, ?> river) {

        add(DISCHARGE, river, -1, 0, TimeUnit.MILLISECONDS, 0, null);
    }

    public <OUT> void discharge(final River<?, ?> river, final OUT... drops) {

        add(PUSH_ARRAY, river, -1, 0, TimeUnit.MILLISECONDS, 0, drops);
        discharge(river);
    }

    public <OUT> void discharge(final River<?, ?> river, final Iterable<? extends OUT> drops) {

        add(PUSH_ITERABLE, river, -1, 0, TimeUnit.MILLISECONDS, 0, drops);
        discharge(river);
    }

    public <OUT> void discharge(final River<?, ?> river, final OUT drop) {

        add(PUSH, river, -1, 0, TimeUnit.MILLISECONDS, 0, drop);
        discharge(river);
    }

    public <OUT> void dischargeAfter(final River<?, ?> river, final long delay,
            final TimeUnit timeUnit, final OUT drop) {

        add(PUSH_AFTER, river, -1, delay, timeUnit, System.nanoTime(), drop);
        discharge(river);
    }

    public <OUT> void dischargeAfter(final River<?, ?> river, final long delay,
            final TimeUnit timeUnit, final OUT... drops) {

        add(PUSH_AFTER_ARRAY, river, -1, delay, timeUnit, System.nanoTime(), drops);
        discharge(river);
    }

    public <OUT> void dischargeAfter(final River<?, ?> river, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        add(PUSH_AFTER_ITERABLE, river, -1, delay, timeUnit, System.nanoTime(), drops);
        discharge(river);
    }

    public void dischargeStream(final River<?, ?> river, final int streamNumber) {

        add(DISCHARGE, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, null);
    }

    public <OUT> void dischargeStream(final River<?, ?> river, final int streamNumber,
            final OUT... drops) {

        add(PUSH_ARRAY, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drops);
        discharge(river, streamNumber);
    }

    public <OUT> void dischargeStream(final River<?, ?> river, final int streamNumber,
            final Iterable<? extends OUT> drops) {

        add(PUSH_ITERABLE, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drops);
        discharge(river, streamNumber);
    }

    public <OUT> void dischargeStream(final River<?, ?> river, final int streamNumber,
            final OUT drop) {

        add(PUSH, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drop);
        discharge(river, streamNumber);
    }

    public <OUT> void dischargeStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final OUT drop) {

        add(PUSH_AFTER, river, streamNumber, delay, timeUnit, System.nanoTime(), drop);
        discharge(river, streamNumber);
    }

    public <OUT> void dischargeStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final OUT... drops) {

        add(PUSH_AFTER_ARRAY, river, streamNumber, delay, timeUnit, System.nanoTime(), drops);
        discharge(river, streamNumber);
    }

    public <OUT> void dischargeStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        add(PUSH_AFTER_ITERABLE, river, streamNumber, delay, timeUnit, System.nanoTime(), drops);
        discharge(river, streamNumber);
    }

    public void forward(final River<?, ?> river, final Throwable throwable) {

        add(FORWARD, river, -1, 0, TimeUnit.MILLISECONDS, 0, throwable);
    }

    public void forwardStream(final River<?, ?> river, final int streamNumber,
            final Throwable throwable) {

        add(FORWARD, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, throwable);
    }

    public <OUT> void push(final River<?, ?> river, final OUT... drops) {

        add(PUSH_ARRAY, river, -1, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <OUT> void push(final River<?, ?> river, final Iterable<? extends OUT> drops) {

        add(PUSH_ITERABLE, river, -1, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <OUT> void push(final River<?, ?> river, final OUT drop) {

        add(PUSH, river, -1, 0, TimeUnit.MILLISECONDS, 0, drop);
    }

    public <OUT> void pushAfter(final River<?, ?> river, final long delay, final TimeUnit timeUnit,
            final OUT drop) {

        add(PUSH_AFTER, river, -1, delay, timeUnit, System.nanoTime(), drop);
    }

    public <OUT> void pushAfter(final River<?, ?> river, final long delay, final TimeUnit timeUnit,
            final OUT... drops) {

        add(PUSH_AFTER_ARRAY, river, -1, delay, timeUnit, System.nanoTime(), drops);
    }

    public <OUT> void pushAfter(final River<?, ?> river, final long delay, final TimeUnit timeUnit,
            final Iterable<? extends OUT> drops) {

        add(PUSH_AFTER_ITERABLE, river, -1, delay, timeUnit, System.nanoTime(), drops);
    }

    public <OUT> void pushStream(final River<?, ?> river, final int streamNumber,
            final OUT... drops) {

        add(PUSH_ARRAY, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <OUT> void pushStream(final River<?, ?> river, final int streamNumber,
            final Iterable<? extends OUT> drops) {

        add(PUSH_ITERABLE, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <OUT> void pushStream(final River<?, ?> river, final int streamNumber, final OUT drop) {

        add(PUSH, river, streamNumber, 0, TimeUnit.MILLISECONDS, 0, drop);
    }

    public <OUT> void pushStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final OUT drop) {

        add(PUSH_AFTER, river, streamNumber, delay, timeUnit, System.nanoTime(), drop);
    }

    public <OUT> void pushStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final OUT... drops) {

        add(PUSH_AFTER_ARRAY, river, streamNumber, delay, timeUnit, System.nanoTime(), drops);
    }

    public <OUT> void pushStreamAfter(final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        add(PUSH_AFTER_ITERABLE, river, streamNumber, delay, timeUnit, System.nanoTime(), drops);
    }

    public void release() {

        if (mPumping) {

            return;
        }

        mPumping = true;

        try {

            while (mFirst != mLast) {

                final int i = mFirst;

                final River<?, ?> river = mRivers[i];

                try {

                    // This call could be re-entrant
                    mFluids[i].push(river, mStreamNumbers[i], mDelays[i], mTimeUnits[i],
                                    mPushTimeNs[i], mData[i]);

                } catch (final Throwable t) {

                    try {

                        river.forward(t);

                    } catch (final Throwable ignored) {

                    }
                }

                // Note that the value of mFirst may have already changed here
                final int n = mFirst;

                mData[n] = null;
                mRivers[n] = null;
                mFluids[n] = null;

                final int newFirst = mFirst + 1;

                if (newFirst >= mData.length) {

                    mFirst = 0;

                } else {

                    mFirst = newFirst;
                }
            }

        } finally {

            mPumping = false;
        }
    }

    private void add(final Fluid fluid, final River<?, ?> river, final int streamNumber,
            final long delay, final TimeUnit timeUnit, final long pushTimeNs, final Object drop) {

        if (mData.length == 0) {

            mFluids = new Fluid[1];
            mRivers = new River[1];
            mStreamNumbers = new int[1];
            mDelays = new long[1];
            mTimeUnits = new TimeUnit[1];
            mPushTimeNs = new long[1];
            mData = new Object[1];
        }

        final int i = mLast;

        mFluids[i] = fluid;
        mRivers[i] = river;
        mStreamNumbers[i] = streamNumber;
        mDelays[i] = delay;
        mTimeUnits[i] = timeUnit;
        mPushTimeNs[i] = pushTimeNs;
        mData[i] = drop;

        final int newLast = i + 1;

        if (newLast >= mData.length) {

            mLast = 0;

        } else {

            mLast = newLast;
        }

        if (mFirst == mLast) {

            ensureCapacity(mData.length + 1);
        }
    }

    private void ensureCapacity(final int capacity) {

        final int size = mData.length;

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

        if (first < last) {

            mFluids = Arrays.copyOf(mFluids, newSize);
            mRivers = Arrays.copyOf(mRivers, newSize);
            mStreamNumbers = Arrays.copyOf(mStreamNumbers, newSize);
            mDelays = Arrays.copyOf(mDelays, newSize);
            mTimeUnits = Arrays.copyOf(mTimeUnits, newSize);
            mPushTimeNs = Arrays.copyOf(mPushTimeNs, newSize);
            mData = Arrays.copyOf(mData, newSize);

        } else {

            final int shift = newSize - size;

            final int newFirst = first + shift;

            final int length = size - first;

            final Fluid[] fluids = mFluids;
            final Fluid[] newFluids = new Fluid[newSize];

            System.arraycopy(fluids, 0, newFluids, 0, last);
            System.arraycopy(fluids, first, newFluids, newFirst, length);

            final River<?, ?>[] rivers = mRivers;
            final River<?, ?>[] newRivers = new River[newSize];

            System.arraycopy(rivers, 0, newRivers, 0, last);
            System.arraycopy(rivers, first, newRivers, newFirst, length);

            final int[] numbers = mStreamNumbers;
            final int[] newNumbers = new int[newSize];

            System.arraycopy(numbers, 0, newNumbers, 0, last);
            System.arraycopy(numbers, first, newNumbers, newFirst, length);

            final long[] delays = mDelays;
            final long[] newDelays = new long[newSize];

            System.arraycopy(delays, 0, newDelays, 0, last);
            System.arraycopy(delays, first, newDelays, newFirst, length);

            final TimeUnit[] timeUnits = mTimeUnits;
            final TimeUnit[] newTimeUnits = new TimeUnit[newSize];

            System.arraycopy(timeUnits, 0, newTimeUnits, 0, last);
            System.arraycopy(timeUnits, first, newTimeUnits, newFirst, length);

            final long[] dischargeTimeNs = mPushTimeNs;
            final long[] newDischargeTimeNs = new long[newSize];

            System.arraycopy(dischargeTimeNs, 0, newDischargeTimeNs, 0, last);
            System.arraycopy(dischargeTimeNs, first, newDischargeTimeNs, newFirst, length);

            final Object[] data = mData;
            final Object[] newData = new Object[newSize];

            System.arraycopy(data, 0, newData, 0, last);
            System.arraycopy(data, first, newData, newFirst, length);

            mFluids = newFluids;
            mRivers = newRivers;
            mStreamNumbers = newNumbers;
            mDelays = newDelays;
            mTimeUnits = newTimeUnits;
            mPushTimeNs = newDischargeTimeNs;
            mData = newData;

            mFirst = newFirst;
        }
    }

    private interface Fluid {

        public <DATA> void push(River<?, DATA> river, int streamNumber, long delay,
                TimeUnit timeUnit, long pushTimeNs, Object drop);
    }
}