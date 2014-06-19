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
import com.bmd.wtf.lps.Leap;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 6/7/14.
 */
class DataFall<SOURCE, IN, OUT> implements Fall<IN> {

    private static final ThreadLocal<DataLock> sLock = new ThreadLocal<DataLock>() {

        @Override
        protected DataLock initialValue() {

            return new DataLock();
        }
    };

    final Current inputCurrent;

    final CopyOnWriteArrayList<DataStream<IN>> inputStreams =
            new CopyOnWriteArrayList<DataStream<IN>>();

    final Leap<SOURCE, IN, OUT> leap;

    final CopyOnWriteArrayList<DataStream<OUT>> outputStreams =
            new CopyOnWriteArrayList<DataStream<OUT>>();

    private final Condition mCondition;

    private final LockRiver<SOURCE, IN> mInRiver;

    private final ReentrantLock mLock;

    private final int mNumber;

    private final LockRiver<SOURCE, OUT> mOutRiver;

    private int mWaterline;

    public DataFall(final Waterfall<SOURCE, IN, OUT> waterfall, final Current inputCurrent,
            final Leap<SOURCE, IN, OUT> leap, final int number) {

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
        mCondition = mLock.newCondition();
        mInRiver = new LockRiver<SOURCE, IN>(new WaterfallRiver<SOURCE, IN>(waterfall, false));
        mOutRiver =
                new LockRiver<SOURCE, OUT>(new StreamRiver<SOURCE, OUT>(outputStreams, waterfall));
    }

    @Override
    public void flush() {

        final DataLock dataLock = sLock.get();

        final LockRiver<SOURCE, IN> inRiver = mInRiver;
        final LockRiver<SOURCE, OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onFlush(inRiver, outRiver, mNumber);

        } catch (final Throwable t) {

            outRiver.forward(t);
        }

        outRiver.close();
        inRiver.close();

        lowerLevel();
    }

    @Override
    public void forward(final Throwable throwable) {

        final DataLock dataLock = sLock.get();

        final LockRiver<SOURCE, IN> inRiver = mInRiver;
        final LockRiver<SOURCE, OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onUnhandled(inRiver, outRiver, mNumber, throwable);

        } catch (final Throwable t) {

            outRiver.forward(t);
        }

        outRiver.close();
        inRiver.close();

        lowerLevel();
    }

    @Override
    public void push(final IN drop) {

        final DataLock dataLock = sLock.get();

        final LockRiver<SOURCE, IN> inRiver = mInRiver;
        final LockRiver<SOURCE, OUT> outRiver = mOutRiver;

        inRiver.open(dataLock);
        outRiver.open(dataLock);

        try {

            leap.onPush(inRiver, outRiver, mNumber, drop);

        } catch (final Throwable t) {

            outRiver.forward(t);
        }

        outRiver.close();
        inRiver.close();

        lowerLevel();
    }

    void raiseLevel(final int count) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mWaterline += count;

        } finally {

            lock.unlock();
        }
    }

    void waitEmpty() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            if (mWaterline <= 0) {

                return;
            }

            do {

                mCondition.await();

            } while (mWaterline > 0);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            mWaterline = 0;

            lock.unlock();
        }
    }

    private void lowerLevel() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            if (--mWaterline <= 0) {

                mCondition.signalAll();
            }

        } finally {

            lock.unlock();
        }
    }
}