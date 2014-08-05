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
import com.bmd.wtf.lps.Leap;

import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Instances of this class implement a fall by managing internally stored leaps. Each instance has
 * a single input current, shared by all the input streams which feed the fall.
 * <p/>
 * This class ensures that the internal leap is always accessed in a thread safe way, so that the
 * implementer does not have to worry about concurrency issues.
 * <p/>
 * Besides, each instance keeps trace of the streams discharging through the fall, so to propagate
 * the discharge only when all the feeding streams have no more data to push.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class DataFall<IN, OUT> implements Fall<IN> {

    private static final ThreadLocal<DataLock> sLock = new ThreadLocal<DataLock>() {

        @Override
        protected DataLock initialValue() {

            return new DataLock();
        }
    };

    final Current inputCurrent;

    final CopyOnWriteArrayList<DataStream<IN>> inputStreams =
            new CopyOnWriteArrayList<DataStream<IN>>();

    final Leap<IN, OUT> leap;

    final CopyOnWriteArrayList<DataStream<OUT>> outputStreams =
            new CopyOnWriteArrayList<DataStream<OUT>>();

    private final HashSet<Stream<IN>> mDryStreams = new HashSet<Stream<IN>>();

    private final LockRiver<IN> mInRiver;

    private final ReentrantLock mLock;

    private final int mNumber;

    private final LockRiver<OUT> mOutRiver;

    private int mDischargeCount;

    private int mWaterline;

    /**
     * Constructor.
     *
     * @param waterfall    The containing waterfall.
     * @param inputCurrent The input current.
     * @param leap         The wrapped leap.
     * @param number       The number identifying this fall.
     */
    public DataFall(final Waterfall<?, IN, OUT> waterfall, final Current inputCurrent,
            final Leap<IN, OUT> leap, final int number) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (inputCurrent == null) {

            throw new IllegalArgumentException("the fall input current cannot be null");
        }

        if (leap == null) {

            throw new IllegalArgumentException("the fall output leap cannot be null");
        }

        this.inputCurrent = inputCurrent;
        this.leap = leap;
        mNumber = number;
        mLock = new ReentrantLock();
        mInRiver = new LockRiver<IN>(new WaterfallRiver<IN>(waterfall, Direction.UPSTREAM));
        mOutRiver = new LockRiver<OUT>(new StreamRiver<OUT>(outputStreams, waterfall));
    }

    @Override
    public void discharge(final Stream<IN> origin) {

        final ReentrantLock lock = mLock;
        lock.lock();

        try {

            final HashSet<Stream<IN>> dryStreams = mDryStreams;

            if (origin != null) {

                dryStreams.add(origin);

                if (dryStreams.containsAll(inputStreams)) {

                    ++mDischargeCount;
                }

            } else {

                ++mDischargeCount;
            }

            if ((mDischargeCount == 0) || (mWaterline > 0)) {

                return;
            }

            --mDischargeCount;

            dryStreams.clear();

        } finally {

            lock.unlock();
        }

        final DataLock dataLock = sLock.get();

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onDischarge(inRiver, outRiver, mNumber);

        } catch (final Throwable t) {

            outRiver.forward(t);

        } finally {

            outRiver.close();
            inRiver.close();
        }
    }

    @Override
    public void forward(final Throwable throwable) {

        final DataLock dataLock = sLock.get();

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onUnhandled(inRiver, outRiver, mNumber, throwable);

        } catch (final Throwable t) {

            outRiver.forward(t);

        } finally {

            outRiver.close();
            inRiver.close();

            lowerLevel();
        }
    }

    @Override
    public void push(final IN drop) {

        final DataLock dataLock = sLock.get();

        final LockRiver<IN> inRiver = mInRiver;
        final LockRiver<OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onPush(inRiver, outRiver, mNumber, drop);

        } catch (final Throwable t) {

            outRiver.forward(t);

        } finally {

            outRiver.close();
            inRiver.close();

            lowerLevel();
        }
    }

    /**
     * Lowers the water level of this fall.
     */
    void lowerLevel() {

        int dischargeCount = 0;

        final ReentrantLock lock = mLock;
        lock.lock();

        try {

            if (--mWaterline <= 0) {

                dischargeCount = mDischargeCount;

                mDischargeCount = 0;

                mWaterline = 0;
            }

        } finally {

            lock.unlock();
        }

        for (int i = 0; i < dischargeCount; ++i) {

            discharge(null);
        }
    }

    /**
     * Raises the water level of this fall by the specified data count.
     *
     * @param count The drop count.
     */
    void raiseLevel(final int count) {

        final ReentrantLock lock = mLock;
        lock.lock();

        try {

            mWaterline += count;

        } finally {

            lock.unlock();
        }
    }
}