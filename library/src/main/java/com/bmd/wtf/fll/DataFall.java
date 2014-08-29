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

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;
import com.bmd.wtf.flw.Stream.Direction;
import com.bmd.wtf.gts.Gate;

import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Instances of this class implement a fall by managing internally stored gates. Each instance has
 * a single input current, shared by all the input streams which feed the fall.
 * <p/>
 * This class ensures that the internal gate is always accessed in a thread safe way, so that the
 * implementer does not have to worry about concurrency issues.
 * <p/>
 * Besides, each instance keeps trace of the streams flushing through the fall, so to propagate
 * the flush only when all the feeding streams have no more data to push.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DataFall<IN, OUT> implements Fall<IN> {

    final Gate<IN, OUT> gate;

    final Current inputCurrent;

    final CopyOnWriteArrayList<DataStream<IN>> inputStreams =
            new CopyOnWriteArrayList<DataStream<IN>>();

    final ReentrantLock lock;

    final CopyOnWriteArrayList<DataStream<OUT>> outputStreams =
            new CopyOnWriteArrayList<DataStream<OUT>>();

    private final HashSet<Stream<IN>> mDryStreams = new HashSet<Stream<IN>>();

    private final LockRiver<IN> mInRiver;

    private final ReentrantLock mLevelLock;

    private final int mNumber;

    private final LockRiver<OUT> mOutRiver;

    private Condition mCondition;

    private int mFlushCount;

    private int mWaterline;

    /**
     * Constructor.
     *
     * @param waterfall    the containing waterfall.
     * @param inputCurrent the input current.
     * @param gate         the wrapped gate.
     * @param number       the number identifying this fall.
     * @throws IllegalArgumentException if the waterfall, the current or the gate are null.
     */
    public DataFall(final Waterfall<?, IN, OUT> waterfall, final Current inputCurrent,
            final Gate<IN, OUT> gate, final int number) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (inputCurrent == null) {

            throw new IllegalArgumentException("the fall input current cannot be null");
        }

        if (gate == null) {

            throw new IllegalArgumentException("the fall output gate cannot be null");
        }

        this.inputCurrent = inputCurrent;
        this.gate = gate;
        this.lock = new ReentrantLock();
        mNumber = number;
        mLevelLock = new ReentrantLock();
        mInRiver = new LockRiver<IN>(new WaterfallRiver<IN>(waterfall, Direction.UPSTREAM), lock);
        mOutRiver = new LockRiver<OUT>(new StreamRiver<OUT>(outputStreams), lock);
    }

    @Override
    public void exception(final Throwable throwable) {

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        try {

            inRiver.open();
            outRiver.open();

            gate.onException(inRiver, outRiver, mNumber, throwable);

        } catch (final Throwable t) {

            outRiver.exception(t);

        } finally {

            final Condition condition = mCondition;

            if (condition != null) {

                condition.signalAll();
            }

            outRiver.close();
            inRiver.close();

            lowerLevel();
        }
    }

    @Override
    public void flush(final Stream<IN> origin) {

        final ReentrantLock lock = mLevelLock;
        lock.lock();

        try {

            final HashSet<Stream<IN>> dryStreams = mDryStreams;

            if (origin != null) {

                dryStreams.add(origin);

                if (dryStreams.containsAll(inputStreams)) {

                    ++mFlushCount;
                }

            } else {

                ++mFlushCount;
            }

            if ((mFlushCount == 0) || (mWaterline > 0)) {

                return;
            }

            --mFlushCount;

            dryStreams.clear();

        } finally {

            lock.unlock();
        }

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        try {

            inRiver.open();
            outRiver.open();

            gate.onFlush(inRiver, outRiver, mNumber);

        } catch (final Throwable t) {

            outRiver.exception(t);

        } finally {

            final Condition condition = mCondition;

            if (condition != null) {

                condition.signalAll();
            }

            outRiver.close();
            inRiver.close();
        }
    }

    @Override
    public void push(final IN drop) {

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        try {

            inRiver.open();
            outRiver.open();

            gate.onPush(inRiver, outRiver, mNumber, drop);

        } catch (final Throwable t) {

            outRiver.exception(t);

        } finally {

            final Condition condition = mCondition;

            if (condition != null) {

                condition.signalAll();
            }

            outRiver.close();
            inRiver.close();

            lowerLevel();
        }
    }

    /**
     * Returns the condition linked to the fall lock.
     *
     * @return the condition.
     */
    Condition getCondition() {

        final ReentrantLock lock = this.lock;
        lock.lock();

        try {

            if (mCondition == null) {

                mCondition = lock.newCondition();
            }

            return mCondition;

        } finally {

            lock.unlock();
        }
    }

    /**
     * Lowers the water level of this fall.
     */
    void lowerLevel() {

        int flushCount = 0;

        final ReentrantLock lock = mLevelLock;
        lock.lock();

        try {

            if (--mWaterline <= 0) {

                flushCount = mFlushCount;

                mFlushCount = 0;

                mWaterline = 0;
            }

        } finally {

            lock.unlock();
        }

        for (int i = 0; i < flushCount; ++i) {

            flush(null);
        }
    }

    /**
     * Raises the water level of this fall by the specified data count.
     *
     * @param count the drop count.
     */
    void raiseLevel(final int count) {

        final ReentrantLock lock = mLevelLock;
        lock.lock();

        try {

            mWaterline += count;

        } finally {

            lock.unlock();
        }
    }
}