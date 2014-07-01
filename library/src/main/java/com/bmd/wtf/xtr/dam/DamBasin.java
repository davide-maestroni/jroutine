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

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.Gate.ConditionEvaluator;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/12/14.
 */
class DamBasin<SOURCE, DATA> implements Leap<SOURCE, DATA, DATA> {

    private final List<List<DATA>> mDrops;

    private final List<List<Throwable>> mThrowables;

    private int mDischargeCount;

    private BasinEvaluator<DATA> mEvaluator;

    private boolean mIsOnData;

    private boolean mIsOnDischarge;

    private boolean mIsOnThrowable;

    private int mMaxCount = Integer.MAX_VALUE;

    private RuntimeException mTimeoutException;

    private long mTimeoutMs;

    public DamBasin(final int fallCount) {

        //noinspection unchecked
        final List<DATA>[] dropLists = new ArrayList[fallCount];

        for (int i = 0; i < fallCount; i++) {

            dropLists[i] = new ArrayList<DATA>();
        }

        //noinspection unchecked
        mDrops = Arrays.asList(dropLists);

        //noinspection unchecked
        final List<Throwable>[] throwableLists = new ArrayList[fallCount];

        for (int i = 0; i < fallCount; i++) {

            throwableLists[i] = new ArrayList<Throwable>();
        }

        //noinspection unchecked
        mThrowables = Arrays.asList(throwableLists);
    }

    public void afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(maxDelay);
    }

    public void all() {

        mMaxCount = Integer.MAX_VALUE;
    }

    public void collectData(final List<DATA> bucket) {

        final int maxCount = mMaxCount;

        final List<List<DATA>> dropLists = mDrops;

        resetWhat();

        for (final List<DATA> drops : dropLists) {

            final List<DATA> subList =
                    drops.subList(0, Math.max(0, Math.min(maxCount, drops.size())));

            bucket.addAll(subList);

            subList.clear();
        }
    }

    public void collectData(final int streamNumber, final List<DATA> bucket) {

        final int maxCount = mMaxCount;

        final List<DATA> drops = mDrops.get(streamNumber);

        resetWhat();

        final List<DATA> subList = drops.subList(0, Math.max(0, Math.min(maxCount, drops.size())));

        bucket.addAll(subList);

        subList.clear();
    }

    public void collectUnhandled(final List<Throwable> bucket) {

        final int maxCount = mMaxCount;

        final List<List<Throwable>> throwableLists = mThrowables;

        resetWhat();

        for (final List<Throwable> throwables : throwableLists) {

            final List<Throwable> subList =
                    throwables.subList(0, Math.max(0, Math.min(maxCount, throwables.size())));

            bucket.addAll(subList);

            subList.clear();
        }
    }

    public void collectUnhandled(final int streamNumber, final List<Throwable> bucket) {

        final int maxCount = mMaxCount;

        final List<Throwable> throwables = mThrowables.get(streamNumber);

        resetWhat();

        final List<Throwable> subList =
                throwables.subList(0, Math.max(0, Math.min(maxCount, throwables.size())));

        bucket.addAll(subList);

        subList.clear();
    }

    public void empty() {

        mDrops.clear();
        mThrowables.clear();

        resetWhat();
    }

    public void eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;
    }

    public void max(final int maxCount) {

        mMaxCount = maxCount;
    }

    public void on(final BasinEvaluator<DATA> evaluator) {

        mEvaluator = evaluator;
    }

    public void onDataAvailable() {

        mIsOnData = true;
    }

    public void onDischarge() {

        mIsOnDischarge = true;
    }

    @Override
    public void onDischarge(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber) {

        ++mDischargeCount;
    }

    @Override
    public void onPush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final DATA drop) {

        mDrops.get(fallNumber).add(drop);
    }

    @Override
    public void onUnhandled(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        mThrowables.get(fallNumber).add(throwable);
    }

    public void onThrowableAvailable() {

        mIsOnThrowable = true;
    }

    public DATA pullData() {

        final List<List<DATA>> dropLists = mDrops;

        resetWhat();

        for (final List<DATA> drops : dropLists) {

            if (!drops.isEmpty()) {

                return drops.remove(0);
            }
        }

        throw new IndexOutOfBoundsException("no data available");
    }

    public DATA pullData(final int streamNumber) {

        final List<List<DATA>> dropLists = mDrops;

        resetWhat();

        return dropLists.get(streamNumber).remove(0);
    }

    public Throwable pullUnhandled() {

        final List<List<Throwable>> throwableLists = mThrowables;

        resetWhat();

        for (final List<Throwable> throwables : throwableLists) {

            if (!throwables.isEmpty()) {

                return throwables.remove(0);
            }
        }

        throw new IndexOutOfBoundsException("no throwable available");
    }

    public Throwable pullUnhandled(final int streamNumber) {

        final List<List<Throwable>> throwableLists = mThrowables;

        resetWhat();

        return throwableLists.get(streamNumber).remove(0);
    }

    public void setupGate(final Gate<DamBasin<SOURCE, DATA>> gate) {

        if (mTimeoutMs >= 0) {

            gate.afterMax(mTimeoutMs, TimeUnit.MILLISECONDS);

        } else {

            gate.eventually();
        }

        gate.eventuallyThrow(mTimeoutException)
            .meeting(new DamConditionEvaluator<SOURCE, DATA>(mEvaluator, mIsOnDischarge, mIsOnData,
                                                             mIsOnThrowable));

        resetWhen();
    }

    public void whenAvailable() {

        mTimeoutMs = -1;
    }

    private void resetWhat() {

        mMaxCount = Integer.MAX_VALUE;
    }

    private void resetWhen() {

        mTimeoutMs = 0;
        mTimeoutException = null;
        mEvaluator = null;
        mIsOnData = false;
        mIsOnDischarge = false;
        mIsOnThrowable = false;
    }

    public interface BasinEvaluator<DATA> {

        public boolean isSatisfied(List<List<DATA>> drops, List<List<Throwable>> throwables,
                int dischargeCount);
    }

    private static class DamConditionEvaluator<SOURCE, DATA>
            implements ConditionEvaluator<DamBasin<SOURCE, DATA>> {

        private BasinEvaluator<DATA> mEvaluator;

        private boolean mIsOnData;

        private boolean mIsOnDischarge;

        private boolean mIsOnThrowable;

        private DamConditionEvaluator(final BasinEvaluator<DATA> evaluator,
                final boolean onDischarge, final boolean onData, final boolean onThrowable) {

            mEvaluator = evaluator;
            mIsOnDischarge = onDischarge;
            mIsOnData = onData;
            mIsOnThrowable = onThrowable;
        }

        @Override
        public boolean isSatisfied(final DamBasin<SOURCE, DATA> basin) {

            final List<List<DATA>> dropLists = basin.mDrops;
            final List<List<Throwable>> throwableLists = basin.mThrowables;
            final int dischargeCount = basin.mDischargeCount;

            final BasinEvaluator<DATA> evaluator = mEvaluator;

            if ((evaluator != null) && !evaluator.isSatisfied(dropLists, throwableLists,
                                                              dischargeCount)) {

                return false;
            }

            if (mIsOnData) {

                boolean isEmpty = false;

                for (final List<DATA> drops : dropLists) {

                    if (!drops.isEmpty()) {

                        isEmpty = true;
                    }
                }

                if (isEmpty) {

                    return false;
                }
            }

            if (mIsOnThrowable && throwableLists.isEmpty()) {

                boolean isEmpty = false;

                for (final List<Throwable> throwables : throwableLists) {

                    if (!throwables.isEmpty()) {

                        isEmpty = true;
                    }
                }

                if (isEmpty) {

                    return false;
                }
            }

            //noinspection RedundantIfStatement
            if (mIsOnDischarge && (dischargeCount <= 0)) {

                return false;
            }

            return true;
        }
    }
}