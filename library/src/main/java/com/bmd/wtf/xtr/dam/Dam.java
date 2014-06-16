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

import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Reflection.Evaluator;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by davide on 6/12/14.
 */
public class Dam<SOURCE, DATA> implements Leap<SOURCE, DATA, DATA>, CollectorBasin<SOURCE, DATA> {

    private final List<List<DATA>> mDrops;

    private final List<List<Throwable>> mThrowables;

    private int mFlushCount;

    private int mMaxCount = Integer.MAX_VALUE;

    public Dam(final int fallCount) {

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

    public static <SOURCE, DATA> DamBasin<SOURCE, DATA> build(
            final Waterfall<SOURCE, DATA, DATA> waterfall) {

        return new DamBasin<SOURCE, DATA>(waterfall);
    }

    public static <SOURCE, DATA> DamEvaluatorBuilder<SOURCE, DATA> evaluator() {

        return new DamEvaluatorBuilder<SOURCE, DATA>();
    }

    @Override
    public Dam<SOURCE, DATA> all() {

        mMaxCount = Integer.MAX_VALUE;

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collectData(final List<DATA> bucket) {

        final int maxCount = mMaxCount;

        final List<List<DATA>> dropLists = mDrops;

        for (final List<DATA> drops : dropLists) {

            final List<DATA> subList =
                    drops.subList(0, Math.max(0, Math.min(maxCount, drops.size())));

            bucket.addAll(subList);

            subList.clear();
        }

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collectData(final int streamNumber, final List<DATA> bucket) {

        final List<DATA> drops = mDrops.get(streamNumber);

        final List<DATA> subList = drops.subList(0, Math.max(0, Math.min(mMaxCount, drops.size())));

        bucket.addAll(subList);

        subList.clear();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        final int maxCount = mMaxCount;

        final List<List<Throwable>> throwableLists = mThrowables;

        for (final List<Throwable> throwables : throwableLists) {

            final List<Throwable> subList =
                    throwables.subList(0, Math.max(0, Math.min(maxCount, throwables.size())));

            bucket.addAll(subList);

            subList.clear();
        }

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        final List<Throwable> throwables = mThrowables.get(streamNumber);

        final List<Throwable> subList =
                throwables.subList(0, Math.max(0, Math.min(mMaxCount, throwables.size())));

        bucket.addAll(subList);

        subList.clear();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> empty() {

        mDrops.clear();
        mThrowables.clear();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> max(final int maxCount) {

        mMaxCount = maxCount;

        return this;
    }

    @Override
    public DATA pullData() {

        final List<List<DATA>> dropLists = mDrops;

        for (final List<DATA> drops : dropLists) {

            if (!drops.isEmpty()) {

                return drops.remove(0);
            }
        }

        throw new IndexOutOfBoundsException(); // TODO
    }

    @Override
    public DATA pullData(final int streamNumber) {

        return mDrops.get(streamNumber).remove(0);
    }

    @Override
    public Throwable pullUnhandled() {

        final List<List<Throwable>> throwableLists = mThrowables;

        for (final List<Throwable> throwables : throwableLists) {

            if (!throwables.isEmpty()) {

                return throwables.remove(0);
            }
        }

        throw new IndexOutOfBoundsException(); // TODO
    }

    @Override
    public Throwable pullUnhandled(final int streamNumber) {

        return mThrowables.get(streamNumber).remove(0);
    }

    @Override
    public void onFlush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber) {

        ++mFlushCount;
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

    public interface BasinEvaluator<DATA> {

        public boolean isSatisfied(List<List<DATA>> drops, List<List<Throwable>> throwables);
    }

    public static class DamEvaluator<SOURCE, DATA>
            implements Evaluator<CollectorBasin<SOURCE, DATA>> {

        private BasinEvaluator<DATA> mEvaluator;

        private boolean mIsOnData;

        private boolean mIsOnFlush;

        private boolean mIsOnThrowable;

        private DamEvaluator(final BasinEvaluator<DATA> evaluator, final boolean onFlush,
                final boolean onData, final boolean onThrowable) {

            mEvaluator = evaluator;
            mIsOnFlush = onFlush;
            mIsOnData = onData;
            mIsOnThrowable = onThrowable;
        }

        @Override
        public <GLASS extends CollectorBasin<SOURCE, DATA>> boolean isSatisfied(final GLASS glass) {

            //noinspection unchecked
            final Dam<SOURCE, DATA> dam = (Dam<SOURCE, DATA>) glass;

            final BasinEvaluator<DATA> evaluator = mEvaluator;

            if (evaluator != null) {

                if (!evaluator.isSatisfied(dam.mDrops, dam.mThrowables)) {

                    return false;
                }
            }

            if (mIsOnFlush) {

                if (dam.mFlushCount <= 0) {

                    return false;
                }
            }

            if (mIsOnData) {

                if (dam.mDrops.isEmpty()) {

                    return false;
                }
            }

            if (mIsOnThrowable) {

                if (dam.mThrowables.isEmpty()) {

                    return false;
                }
            }

            return true;
        }
    }

    public static class DamEvaluatorBuilder<SOURCE, DATA> {

        private BasinEvaluator<DATA> mEvaluator;

        private boolean mIsOnData;

        private boolean mIsOnFlush;

        private boolean mIsOnThrowable;

        private DamEvaluatorBuilder() {

        }

        public Evaluator<CollectorBasin<SOURCE, DATA>> matches() {

            return new DamEvaluator<SOURCE, DATA>(mEvaluator, mIsOnFlush, mIsOnData,
                                                  mIsOnThrowable);
        }

        public DamEvaluatorBuilder<SOURCE, DATA> on(final BasinEvaluator<DATA> evaluator) {

            mEvaluator = evaluator;

            return this;
        }

        public DamEvaluatorBuilder<SOURCE, DATA> onDataAvailable() {

            mIsOnData = true;

            return this;
        }

        public DamEvaluatorBuilder<SOURCE, DATA> onFlush() {

            mIsOnFlush = true;

            return this;
        }

        public DamEvaluatorBuilder<SOURCE, DATA> onThrowableAvailable() {

            mIsOnThrowable = true;

            return this;
        }
    }
}