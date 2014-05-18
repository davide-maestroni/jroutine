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
package com.bmd.wtf.xtr.bsn;

import com.bmd.wtf.bdr.DelayInterruptedException;
import com.bmd.wtf.dam.CollectorDam;
import com.bmd.wtf.src.Floodgate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of a {@link com.bmd.wtf.dam.CollectorDam} which can block waiting for the data
 * to flow down asynchronously.
 * <p/>
 * The dam will wait for a flush in order to know that data have complete.
 * <p/>
 * Created by davide on 4/11/14.
 *
 * @param <DATA> The data type.
 */
class BlockingCollectorDam<DATA> extends CollectorDam<DATA> {

    private final Condition mCondition;

    private final ReentrantLock mLock = new ReentrantLock();

    private int mDropCount;

    private int mFlushCount;

    private int mPullCount;

    private int mPushCount;

    private RuntimeException mTimeoutException;

    // No timeout by default
    private long mTimeoutMs = -1;

    public BlockingCollectorDam() {

        mCondition = mLock.newCondition();
    }

    @Override
    public List<DATA> collect() {

        waitForData(true);

        return super.collect();
    }

    @Override
    public DATA collectNext() {

        waitForData(false);

        return super.collectNext();
    }

    @Override
    public Object collectNextPulledDebris() {

        waitForPull(false);

        return super.collectNextPulledDebris();
    }

    @Override
    public Object collectNextPushedDebris() {

        waitForPush(false);

        return super.collectNextPushedDebris();
    }

    @Override
    public List<Object> collectPulledDebris() {

        waitForPull(true);

        return super.collectPulledDebris();
    }

    @Override
    public List<Object> collectPushedDebris() {

        waitForPush(true);

        return super.collectPushedDebris();
    }

    @Override
    public Object onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        final Object debris = super.onDischarge(gate, drop);

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            ++mDropCount;

        } finally {

            lock.unlock();
        }

        return debris;
    }

    @Override
    public Object onPullDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        final Object next = super.onPullDebris(gate, debris);

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            ++mPullCount;

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }

        return next;
    }

    @Override
    public Object onPushDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        final Object next = super.onPushDebris(gate, debris);

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            ++mPushCount;

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }

        return next;
    }

    @Override
    public Object onFlush(final Floodgate<DATA, DATA> gate) {

        final Object debris = super.onFlush(gate);

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            ++mFlushCount;

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }

        return debris;
    }

    /**
     * Sets an indefinite timeout to wait for data to be collected.
     */
    public void setNoTimeout() {

        final ReentrantLock lock = mLock;

        lock.lock();

        mTimeoutMs = -1;

        lock.unlock();
    }

    /**
     * Sets the timeout to wait for data to be fully collected.
     *
     * @param maxDelay The maximum delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     */
    public void setTimeout(final long maxDelay, final TimeUnit timeUnit) {

        final ReentrantLock lock = mLock;

        lock.lock();

        mTimeoutMs = timeUnit.toMillis(maxDelay);

        lock.unlock();
    }

    /**
     * Sets the exception to be thrown in case the timeout elapsed before all the data have been
     * collected.<br/>
     * If <code>null</code> no exception will be thrown.
     *
     * @param exception The exception to be thrown.
     */
    public void setTimeoutException(final RuntimeException exception) {

        final ReentrantLock lock = mLock;

        lock.lock();

        mTimeoutException = exception;

        lock.unlock();
    }

    private void waitForData(final boolean drain) {

        final ReentrantLock lock = mLock;

        lock.lock();

        boolean isTimeout = false;

        RuntimeException exception = null;

        try {

            if (mFlushCount > 0) {

                return;
            }

            exception = mTimeoutException;

            long currentTimeout = mTimeoutMs;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if ((mFlushCount == 0) && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (mFlushCount == 0);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            if (drain || (--mDropCount <= 0)) {

                mFlushCount = 0;
                mDropCount = 0;
            }

            lock.unlock();
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }

    private void waitForPull(final boolean drain) {

        final ReentrantLock lock = mLock;

        lock.lock();

        boolean isTimeout = false;

        RuntimeException exception = null;

        try {

            if (mPullCount > 0) {

                return;
            }

            exception = mTimeoutException;

            long currentTimeout = mTimeoutMs;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if ((mPullCount == 0) && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (mPullCount == 0);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            if (drain || (--mPullCount <= 0)) {

                mPullCount = 0;
            }

            lock.unlock();
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }

    private void waitForPush(final boolean drain) {

        final ReentrantLock lock = mLock;

        lock.lock();

        boolean isTimeout = false;

        RuntimeException exception = null;

        try {

            if (mPushCount > 0) {

                return;
            }

            exception = mTimeoutException;

            long currentTimeout = mTimeoutMs;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if ((mPushCount == 0) && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (mPushCount == 0);

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            if (drain || (--mPushCount <= 0)) {

                mPushCount = 0;
            }

            lock.unlock();
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }
}