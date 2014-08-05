/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHDATA WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * River decorator using a lock to postpone commands when the fall method complete its execution.
 * <p/>
 * In case the river is called outside a fall, the command is immediately delegated to the wrapped
 * instance.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> The data type.
 */
class LockRiver<DATA> implements River<DATA> {

    private final ReentrantLock mLock = new ReentrantLock();

    private final River<DATA> mRiver;

    private volatile DataLock mDataLock;

    /**
     * Constructor.
     *
     * @param wrapped The wrapped river.
     */
    public LockRiver(final River<DATA> wrapped) {

        if (wrapped == null) {

            throw new IllegalArgumentException("the river cannot be null");
        }

        mRiver = wrapped;
    }

    @Override
    public void deviate() {

        mRiver.deviate();
    }

    @Override
    public void deviateStream(final int streamNumber) {

        mRiver.deviateStream(streamNumber);
    }

    @Override
    public River<DATA> discharge() {

        if (isOpen()) {

            mDataLock.discharge(mRiver);

        } else {

            mRiver.discharge();
        }

        return this;
    }

    @Override
    public River<DATA> discharge(final DATA... drops) {

        if (isOpen()) {

            mDataLock.discharge(mRiver, drops);

        } else {

            mRiver.discharge(drops);
        }

        return this;
    }

    @Override
    public River<DATA> discharge(final Iterable<? extends DATA> drops) {

        if (isOpen()) {

            mDataLock.discharge(mRiver, drops);

        } else {

            mRiver.discharge(drops);
        }

        return this;
    }

    @Override
    public River<DATA> discharge(final DATA drop) {

        if (isOpen()) {

            mDataLock.discharge(mRiver, drop);

        } else {

            mRiver.discharge(drop);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.dischargeAfter(mRiver, delay, timeUnit, drops);

        } else {

            mRiver.dischargeAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        if (isOpen()) {

            mDataLock.dischargeAfter(mRiver, delay, timeUnit, drop);

        } else {

            mRiver.dischargeAfter(delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.dischargeAfter(mRiver, delay, timeUnit, drops[0]);

            } else {

                mDataLock.dischargeAfter(mRiver, delay, timeUnit, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.dischargeAfter(delay, timeUnit, drops[0]);

            } else {

                mRiver.dischargeAfter(delay, timeUnit, drops);
            }
        }

        return this;
    }

    @Override
    public River<DATA> forward(final Throwable throwable) {

        if (isOpen()) {

            mDataLock.forward(mRiver, throwable);

        } else {

            mRiver.forward(throwable);
        }

        return this;
    }

    @Override
    public River<DATA> push(final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.push(mRiver, drops[0]);

            } else {

                mDataLock.push(mRiver, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.push(drops[0]);

            } else {

                mRiver.push(drops);
            }
        }

        return this;
    }

    @Override
    public River<DATA> push(final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.push(mRiver, drops);

        } else {

            mRiver.push(drops);
        }

        return this;
    }

    @Override
    public River<DATA> push(final DATA drop) {

        if (isOpen()) {

            mDataLock.push(mRiver, drop);

        } else {

            mRiver.push(drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.pushAfter(mRiver, delay, timeUnit, drops);

        } else {

            mRiver.pushAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        if (isOpen()) {

            mDataLock.pushAfter(mRiver, delay, timeUnit, drop);

        } else {

            mRiver.pushAfter(delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.pushAfter(mRiver, delay, timeUnit, drops[0]);

            } else {

                mDataLock.pushAfter(mRiver, delay, timeUnit, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.pushAfter(delay, timeUnit, drops[0]);

            } else {

                mRiver.pushAfter(delay, timeUnit, drops);
            }
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStream(final int streamNumber) {

        if (isOpen()) {

            mDataLock.dischargeStream(mRiver, streamNumber);

        } else {

            mRiver.dischargeStream(streamNumber);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStream(final int streamNumber, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.dischargeStream(mRiver, streamNumber, drops[0]);

            } else {

                mDataLock.dischargeStream(mRiver, streamNumber, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.dischargeStream(streamNumber, drops[0]);

            } else {

                mRiver.dischargeStream(streamNumber, drops);
            }
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStream(final int streamNumber,
            final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.dischargeStream(mRiver, streamNumber, drops);

        } else {

            mRiver.dischargeStream(streamNumber, drops);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStream(final int streamNumber, final DATA drop) {

        if (isOpen()) {

            mDataLock.dischargeStream(mRiver, streamNumber, drop);

        } else {

            mRiver.dischargeStream(streamNumber, drop);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.dischargeStreamAfter(mRiver, streamNumber, delay, timeUnit, drops);

        } else {

            mRiver.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        if (isOpen()) {

            mDataLock.dischargeStreamAfter(mRiver, streamNumber, delay, timeUnit, drop);

        } else {

            mRiver.dischargeStreamAfter(streamNumber, delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.dischargeStreamAfter(mRiver, streamNumber, delay, timeUnit, drops[0]);

            } else {

                mDataLock.dischargeStreamAfter(mRiver, streamNumber, delay, timeUnit, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.dischargeStreamAfter(streamNumber, delay, timeUnit, drops[0]);

            } else {

                mRiver.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);
            }
        }

        return this;
    }

    @Override
    public void drain() {

        mRiver.drain();
    }

    @Override
    public void drainStream(final int streamNumber) {

        mRiver.drainStream(streamNumber);
    }

    @Override
    public River<DATA> forwardStream(final int streamNumber, final Throwable throwable) {

        if (isOpen()) {

            mDataLock.forwardStream(mRiver, streamNumber, throwable);

        } else {

            mRiver.forwardStream(streamNumber, throwable);
        }

        return this;
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Class<TYPE> gateClass) {

        return mRiver.on(gateClass);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final TYPE leap) {

        return mRiver.on(leap);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Classification<TYPE> gateClassification) {

        return mRiver.on(gateClassification);
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.pushStream(mRiver, streamNumber, drops[0]);

            } else {

                mDataLock.pushStream(mRiver, streamNumber, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.pushStream(streamNumber, drops[0]);

            } else {

                mRiver.pushStream(streamNumber, drops);
            }
        }

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.pushStream(mRiver, streamNumber, drops);

        } else {

            mRiver.pushStream(streamNumber, drops);
        }

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final DATA drop) {

        if (isOpen()) {

            mDataLock.pushStream(mRiver, streamNumber, drop);

        } else {

            mRiver.pushStream(streamNumber, drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        if (isOpen()) {

            mDataLock.pushStreamAfter(mRiver, streamNumber, delay, timeUnit, drops);

        } else {

            mRiver.pushStreamAfter(streamNumber, delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        if (isOpen()) {

            mDataLock.pushStreamAfter(mRiver, streamNumber, delay, timeUnit, drop);

        } else {

            mRiver.pushStreamAfter(streamNumber, delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        if (isOpen()) {

            if (drops.length == 1) {

                mDataLock.pushStreamAfter(mRiver, streamNumber, delay, timeUnit, drops[0]);

            } else {

                mDataLock.pushStreamAfter(mRiver, streamNumber, delay, timeUnit, drops);
            }

        } else {

            if (drops.length == 1) {

                mRiver.pushStreamAfter(streamNumber, delay, timeUnit, drops[0]);

            } else {

                mRiver.pushStreamAfter(streamNumber, delay, timeUnit, drops);
            }
        }

        return this;
    }

    @Override
    public int size() {

        return mRiver.size();
    }

    /**
     * Closes the river lock.
     * <p/>
     * The accumulated data are released as a result.
     */
    void close() {

        final ReentrantLock lock = mLock;

        if (!lock.isHeldByCurrentThread()) {

            throw new IllegalStateException("an open lock cannot be closed in a different thread");
        }

        final DataLock dataLock = mDataLock;
        mDataLock = null;

        lock.unlock();

        if (dataLock != null) {

            dataLock.release();
        }
    }

    /**
     * Opens the river lock.
     *
     * @param lock The lock instance.
     */
    void open(final DataLock lock) {

        mLock.lock();
        mDataLock = lock;
    }

    private boolean isOpen() {

        return mLock.isHeldByCurrentThread() && (mDataLock != null);
    }
}