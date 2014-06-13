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
package com.bmd.wtf.xtr.dam;

import com.bmd.wtf.fll.DelayInterruptedException;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 6/12/14.
 */
class DamBasin<SOURCE, DATA> implements Leap<SOURCE, DATA, DATA>, CollectorBasin<SOURCE, DATA> {

    private final java.util.concurrent.locks.Condition mCondition;

    private final List<List<DATA>> mDrops;

    private final ReentrantLock mLock = new ReentrantLock();

    private final List<List<Throwable>> mThrowables;

    private final Waterfall<SOURCE, DATA, DATA> mWaterfall;

    private Condition<SOURCE, DATA> mAvailableCondition;

    private int mFlushCount;

    private boolean mIsOnFlush;

    private int mMaxCount;

    private RuntimeException mTimeoutException;

    // Immediate timeout by default
    private long mTimeoutMs = 0;

    public DamBasin(final Waterfall<SOURCE, DATA, DATA> waterfall) {

        mWaterfall = waterfall;
        mCondition = mLock.newCondition();

        final int size = waterfall.size();

        //noinspection unchecked
        final List<DATA>[] dropLists = new ArrayList[size];

        for (int i = 0; i < size; i++) {

            dropLists[i] = new ArrayList<DATA>();
        }

        //noinspection unchecked
        mDrops = Arrays.asList(dropLists);

        //noinspection unchecked
        final List<Throwable>[] throwableLists = new ArrayList[size];

        for (int i = 0; i < size; i++) {

            throwableLists[i] = new ArrayList<Throwable>();
        }

        //noinspection unchecked
        mThrowables = Arrays.asList(throwableLists);
    }

    @Override
    public CollectorBasin<SOURCE, DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mTimeoutMs = timeUnit.toMillis(maxDelay);

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> all() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mMaxCount = Integer.MAX_VALUE;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collect(final List<DATA> bucket) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            waitForData();

            final List<List<DATA>> dropLists = mDrops;

            for (final List<DATA> drops : dropLists) {

                final List<DATA> subList =
                        drops.subList(0, Math.max(0, Math.min(mMaxCount, drops.size())));

                bucket.addAll(subList);

                subList.clear();
            }

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collect(final int streamNumber, final List<DATA> bucket) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            final List<DATA> drops = mDrops.get(streamNumber);

            final List<DATA> subList =
                    drops.subList(0, Math.max(0, Math.min(mMaxCount, drops.size())));

            bucket.addAll(subList);

            subList.clear();

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            waitForThrowable();

            final List<List<Throwable>> throwableLists = mThrowables;

            for (final List<Throwable> throwables : throwableLists) {

                final List<Throwable> subList =
                        throwables.subList(0, Math.max(0, Math.min(mMaxCount, throwables.size())));

                bucket.addAll(subList);

                subList.clear();
            }

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            final List<Throwable> throwables = mThrowables.get(streamNumber);

            final List<Throwable> subList =
                    throwables.subList(0, Math.max(0, Math.min(mMaxCount, throwables.size())));

            bucket.addAll(subList);

            subList.clear();

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> empty() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mDrops.clear();
            mThrowables.clear();

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> eventuallyThrow(final RuntimeException exception) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mTimeoutException = exception;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> immediately() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mTimeoutMs = 0;
            mIsOnFlush = false;
            mAvailableCondition = null;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> max(final int maxCount) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mMaxCount = maxCount;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> onFlush() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mIsOnFlush = true;
            mAvailableCondition = null;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public DATA pull() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            waitForData();

            final List<List<DATA>> dropLists = mDrops;

            for (final List<DATA> drops : dropLists) {

                if (!drops.isEmpty()) {

                    return drops.remove(0);
                }
            }

        } finally {

            lock.unlock();
        }

        throw new IndexOutOfBoundsException(); // TODO
    }

    @Override
    public DATA pull(final int streamNumber) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            return mDrops.get(streamNumber).remove(0);

        } finally {

            lock.unlock();
        }
    }

    @Override
    public Throwable pullUnhandled() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            waitForThrowable();

            final List<List<Throwable>> throwableLists = mThrowables;

            for (final List<Throwable> throwables : throwableLists) {

                if (!throwables.isEmpty()) {

                    return throwables.remove(0);
                }
            }

        } finally {

            lock.unlock();
        }

        throw new IndexOutOfBoundsException(); // TODO
    }

    @Override
    public Throwable pullUnhandled(final int streamNumber) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            return mThrowables.get(streamNumber).remove(0);

        } finally {

            lock.unlock();
        }
    }

    @Override
    public Waterfall<SOURCE, DATA, DATA> release() {

        return mWaterfall;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> when(final Condition<SOURCE, DATA> condition) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mIsOnFlush = false;
            mAvailableCondition = condition;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> whenAvailable() {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mTimeoutMs = -1;
            mIsOnFlush = false;
            mAvailableCondition = null;

        } finally {

            lock.unlock();
        }

        return this;
    }

    @Override
    public void onFlush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            ++mFlushCount;

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }
    }

    @Override
    public void onPush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final DATA drop) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mDrops.get(fallNumber).add(drop);

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }
    }

    @Override
    public void onUnhandled(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            mThrowables.get(fallNumber).add(throwable);

            mCondition.signalAll();

        } finally {

            lock.unlock();
        }
    }

    private boolean matchesDataCondition() {

        if (mAvailableCondition != null) {

            return mAvailableCondition.matches(this, mDrops, mThrowables);
        }

        if (mIsOnFlush) {

            return (mFlushCount > 0);
        }

        return !mDrops.isEmpty();
    }

    private boolean matchesThrowableCondition() {

        if (mAvailableCondition != null) {

            return mAvailableCondition.matches(this, mDrops, mThrowables);
        }

        if (mIsOnFlush) {

            return (mFlushCount > 0);
        }

        return !mThrowables.isEmpty();
    }

    private void waitForData() {

        long currentTimeout = mTimeoutMs;

        if (currentTimeout == 0) {

            return;
        }

        boolean isTimeout = false;

        RuntimeException exception = null;

        try {

            if (matchesDataCondition()) {

                return;
            }

            exception = mTimeoutException;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if (!matchesDataCondition() && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (!matchesDataCondition());

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            mFlushCount = 0;
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }

    private void waitForThrowable() {

        long currentTimeout = mTimeoutMs;

        if (currentTimeout == 0) {

            return;
        }

        boolean isTimeout = false;

        RuntimeException exception = null;

        try {

            if (matchesThrowableCondition()) {

                return;
            }

            exception = mTimeoutException;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    mCondition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if (!matchesThrowableCondition() && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    mCondition.await();
                }

            } while (!matchesThrowableCondition());

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);

        } finally {

            mFlushCount = 0;
        }

        if (isTimeout && (exception != null)) {

            throw exception;
        }
    }
}