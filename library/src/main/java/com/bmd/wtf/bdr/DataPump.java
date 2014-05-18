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
package com.bmd.wtf.bdr;

import com.bmd.wtf.flw.Flow;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * This class maintains a queue of commands in a lightweight circular buffer structure and executes
 * them in a non-recursive way.
 * <p/>
 * Created by davide on 3/8/14.
 */
class DataPump {

    private static final int DEFAULT_INITIAL_CAPACITY = 10;

    private static final Fluid DISCHARGE = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            final OUT out = (OUT) drop;

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                stream.discharge(out);
            }
        }
    };

    private static final Fluid DISCHARGE_AFTER = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            long currentDelay = delay;

            final OUT out = (OUT) drop;

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                currentDelay = updateDelay(currentDelay, timeUnit, dischargeTimeNs);

                stream.dischargeAfter(currentDelay, timeUnit, out);
            }
        }
    };

    private static final Fluid DISCHARGE_AFTER_ARRAY = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            long currentDelay = delay;

            final List<OUT> outs = Arrays.asList((OUT[]) drops);

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                currentDelay = updateDelay(currentDelay, timeUnit, dischargeTimeNs);

                stream.dischargeAfter(currentDelay, timeUnit, outs);
            }
        }
    };

    private static final Fluid DISCHARGE_AFTER_ITERABLE = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            long currentDelay = delay;

            final Iterable<OUT> outs = (Iterable<OUT>) drops;

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                currentDelay = updateDelay(currentDelay, timeUnit, dischargeTimeNs);

                stream.dischargeAfter(currentDelay, timeUnit, outs);
            }
        }
    };

    private static final Fluid DISCHARGE_ARRAY = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            final OUT[] outs = (OUT[]) drops;

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final OUT out : outs) {

                for (final Stream<?, IN, OUT> stream : outStreams) {

                    stream.discharge(out);
                }
            }
        }
    };

    private static final Fluid DISCHARGE_ITERABLE = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            final Iterable<OUT> outs = (Iterable<OUT>) drops;

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final OUT out : outs) {

                for (final Stream<?, IN, OUT> stream : outStreams) {

                    stream.discharge(out);
                }
            }
        }
    };

    private static final Fluid FLUSH = new Fluid() {

        @Override
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                stream.flush();
            }
        }
    };

    private static final Fluid PULL = new Fluid() {

        @Override
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            final CopyOnWriteArraySet<Stream<?, ?, IN>> inStreams = pool.inputStreams;

            for (final Stream<?, ?, IN> stream : inStreams) {

                stream.pull(drop);
            }
        }
    };

    private static final Fluid PUSH = new Fluid() {

        @Override
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            final CopyOnWriteArraySet<Stream<?, IN, OUT>> outStreams = pool.outputStreams;

            for (final Stream<?, IN, OUT> stream : outStreams) {

                stream.push(drop);
            }
        }
    };

    private static final Fluid RECHARGE_AFTER = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

            final long currentDelay = updateDelay(delay, timeUnit, dischargeTimeNs);

            final Flow inputFlow = pool.inputFlow;

            inputFlow.dischargeAfter(pool, currentDelay, timeUnit, (IN) drop);
        }
    };

    private static final Fluid RECHARGE_AFTER_ARRAY = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            final long currentDelay = updateDelay(delay, timeUnit, dischargeTimeNs);

            final Flow inputFlow = pool.inputFlow;

            inputFlow.dischargeAfter(pool, currentDelay, timeUnit, Arrays.asList((IN[]) drops));
        }
    };

    private static final Fluid RECHARGE_AFTER_ITERABLE = new Fluid() {

        @Override
        @SuppressWarnings("unchecked")
        public <IN, OUT> void discharge(final DataPool<IN, OUT> pool, final long delay,
                final TimeUnit timeUnit, final long dischargeTimeNs, final Object drops) {

            final long currentDelay = updateDelay(delay, timeUnit, dischargeTimeNs);

            final Flow inputFlow = pool.inputFlow;

            inputFlow.dischargeAfter(pool, currentDelay, timeUnit, (Iterable<IN>) drops);
        }
    };

    private Object[] mData;

    private long[] mDelays;

    private long[] mDischargeTimeNs;

    private int mFirst;

    private Fluid[] mFluids;

    private int mLast;

    private DataPool<?, ?>[] mPools;

    private boolean mPumping;

    private TimeUnit[] mTimeUnits;

    public DataPump() {

        this(DEFAULT_INITIAL_CAPACITY);
    }

    public DataPump(final int initialCapacity) {

        mData = new Object[initialCapacity];
        mPools = new DataPool[initialCapacity];
        mDelays = new long[initialCapacity];
        mTimeUnits = new TimeUnit[initialCapacity];
        mDischargeTimeNs = new long[initialCapacity];
        mFluids = new Fluid[initialCapacity];
    }

    private static long updateDelay(final long delay, final TimeUnit timeUnit,
            final long dischargeTimeNs) {

        if (delay <= 0) {

            return 0;
        }

        return (delay - timeUnit
                .convert(System.nanoTime() - dischargeTimeNs, TimeUnit.NANOSECONDS));
    }

    public <OUT> void discharge(final DataPool<?, OUT> pool, final OUT drop) {

        add(DISCHARGE, pool, 0, TimeUnit.MILLISECONDS, 0, drop);
    }

    public <OUT> void discharge(final DataPool<?, OUT> pool, final OUT... drops) {

        add(DISCHARGE_ARRAY, pool, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <OUT> void discharge(final DataPool<?, OUT> pool, final Iterable<? extends OUT> drops) {

        add(DISCHARGE_ITERABLE, pool, 0, TimeUnit.MILLISECONDS, 0, drops);
    }

    public <T> void dischargeAfter(final DataPool<?, T> pool, final long delay,
            final TimeUnit timeUnit, final T drop) {

        add(DISCHARGE_AFTER, pool, delay, timeUnit, System.nanoTime(), drop);
    }

    public <OUT> void dischargeAfter(final DataPool<?, OUT> pool, final long delay,
            final TimeUnit timeUnit, final OUT... drops) {

        add(DISCHARGE_AFTER_ARRAY, pool, delay, timeUnit, System.nanoTime(), drops);
    }

    public <OUT> void dischargeAfter(final DataPool<?, OUT> pool, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        add(DISCHARGE_AFTER_ITERABLE, pool, delay, timeUnit, System.nanoTime(), drops);
    }

    public void flush(final DataPool<?, ?> pool) {

        add(FLUSH, pool, 0, TimeUnit.MILLISECONDS, 0, null);
    }

    public void pull(final DataPool<?, ?> pool, final Object object) {

        add(PULL, pool, 0, TimeUnit.MILLISECONDS, 0, object);
    }

    public void push(final DataPool<?, ?> pool, final Object object) {

        add(PUSH, pool, 0, TimeUnit.MILLISECONDS, 0, object);
    }

    public <OUT> void rechargeAfter(final DataPool<OUT, ?> pool, final long delay,
            final TimeUnit timeUnit, final OUT drop) {

        add(RECHARGE_AFTER, pool, delay, timeUnit, System.nanoTime(), drop);
    }

    public <OUT> void rechargeAfter(final DataPool<OUT, ?> pool, final long delay,
            final TimeUnit timeUnit, final OUT... drops) {

        add(RECHARGE_AFTER_ARRAY, pool, delay, timeUnit, System.nanoTime(), drops);
    }

    public <OUT> void rechargeAfter(final DataPool<OUT, ?> pool, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        add(RECHARGE_AFTER_ITERABLE, pool, delay, timeUnit, System.nanoTime(), drops);
    }

    public void run() {

        if (mPumping) {

            return;
        }

        mPumping = true;

        try {

            while (mFirst != mLast) {

                final int i = mFirst;

                mFluids[i].discharge(mPools[i], mDelays[i], mTimeUnits[i], mDischargeTimeNs[i],
                                     mData[i]);

                // Note that the value of mFirst may have already changed here
                final int n = mFirst;

                mData[n] = null;
                mPools[n] = null;
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

    private void add(final Fluid fluid, final DataPool<?, ?> pool, final long delay,
            final TimeUnit timeUnit, final long dischargeTimeNs, final Object drop) {

        if (mData.length == 0) {

            mData = new Object[1];
            mPools = new DataPool[1];
            mDelays = new long[1];
            mTimeUnits = new TimeUnit[1];
            mDischargeTimeNs = new long[1];
            mFluids = new Fluid[1];
        }

        final int i = mLast;

        mData[i] = drop;
        mPools[i] = pool;
        mDelays[i] = delay;
        mTimeUnits[i] = timeUnit;
        mDischargeTimeNs[i] = dischargeTimeNs;
        mFluids[i] = fluid;

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

            mData = Arrays.copyOf(mData, newSize);
            mPools = Arrays.copyOf(mPools, newSize);
            mDelays = Arrays.copyOf(mDelays, newSize);
            mTimeUnits = Arrays.copyOf(mTimeUnits, newSize);
            mDischargeTimeNs = Arrays.copyOf(mDischargeTimeNs, newSize);
            mFluids = Arrays.copyOf(mFluids, newSize);

        } else {

            final int shift = newSize - size;

            final int newFirst = first + shift;

            final int length = size - first;

            final Object[] data = mData;
            final Object[] newData = new Object[newSize];

            System.arraycopy(data, 0, newData, 0, last);
            System.arraycopy(data, first, newData, newFirst, length);

            final DataPool<?, ?>[] pools = mPools;
            final DataPool<?, ?>[] newPools = new DataPool[newSize];

            System.arraycopy(pools, 0, newPools, 0, last);
            System.arraycopy(pools, first, newPools, newFirst, length);

            final long[] delays = mDelays;
            final long[] newDelays = new long[newSize];

            System.arraycopy(delays, 0, newDelays, 0, last);
            System.arraycopy(delays, first, newDelays, newFirst, length);

            final TimeUnit[] timeUnits = mTimeUnits;
            final TimeUnit[] newTimeUnits = new TimeUnit[newSize];

            System.arraycopy(timeUnits, 0, newTimeUnits, 0, last);
            System.arraycopy(timeUnits, first, newTimeUnits, newFirst, length);

            final long[] dischargeTimeNs = mDischargeTimeNs;
            final long[] newDischargeTimeNs = new long[newSize];

            System.arraycopy(dischargeTimeNs, 0, newDischargeTimeNs, 0, last);
            System.arraycopy(dischargeTimeNs, first, newDischargeTimeNs, newFirst, length);

            final Fluid[] fluids = mFluids;
            final Fluid[] newFluids = new Fluid[newSize];

            System.arraycopy(fluids, 0, newFluids, 0, last);
            System.arraycopy(fluids, first, newFluids, newFirst, length);

            mData = newData;
            mPools = newPools;
            mDelays = newDelays;
            mTimeUnits = newTimeUnits;
            mDischargeTimeNs = newDischargeTimeNs;
            mFluids = newFluids;

            mFirst = newFirst;
        }
    }

    private interface Fluid {

        public <IN, OUT> void discharge(DataPool<IN, OUT> pool, long delay, TimeUnit timeUnit,
                long dischargeTimeNs, Object drop);
    }
}