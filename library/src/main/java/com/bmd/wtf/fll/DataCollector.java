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

import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.Gate.ConditionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/7/14.
 */
class DataCollector<SOURCE, DATA> implements Collector<DATA> {

    private static final ConditionEvaluator<CollectorLeap<?, ?>> HAS_DATA =
            new ConditionEvaluator<CollectorLeap<?, ?>>() {

                @Override
                public boolean isSatisfied(final CollectorLeap<?, ?> leap) {

                    return (leap.size() > 0);
                }
            };

    private static final ConditionEvaluator<CollectorLeap<?, ?>> IS_COMPLETE =
            new ConditionEvaluator<CollectorLeap<?, ?>>() {

                @Override
                public boolean isSatisfied(final CollectorLeap<?, ?> leap) {

                    return leap.isComplete();
                }
            };

    private final CollectorLeap<SOURCE, DATA> mCollectorLeap;

    private final Gate<CollectorLeap<SOURCE, DATA>> mDataGate;

    private final DataGate<CollectorLeap<SOURCE, DATA>> mSizeGate;

    public DataCollector(final GateLeap<?, DATA, DATA> gateLeap,
            final CollectorLeap<SOURCE, DATA> collectorLeap) {

        mCollectorLeap = collectorLeap;

        final Classification<CollectorLeap<SOURCE, DATA>> classification =
                new Classification<CollectorLeap<SOURCE, DATA>>() {};
        mSizeGate = new DataGate<CollectorLeap<SOURCE, DATA>>(gateLeap, classification);
        mDataGate =
                new DataGate<CollectorLeap<SOURCE, DATA>>(gateLeap, classification).eventually();
    }

    @Override
    public Collector<DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mDataGate.afterMax(maxDelay, timeUnit);

        return this;
    }

    @Override
    public List<DATA> all() {

        final ArrayList<DATA> data = new ArrayList<DATA>();

        allInto(data);

        return data;
    }

    @Override
    public Collector<DATA> allInto(final List<DATA> data) {

        mDataGate.meets(IS_COMPLETE).perform(mCollectorLeap.pullAllAction(), data);

        return this;
    }

    @Override
    public Collector<DATA> eventually() {

        mDataGate.eventually();

        return this;
    }

    @Override
    public Collector<DATA> eventuallyThrow(final RuntimeException exception) {

        mDataGate.eventuallyThrow(exception);

        return this;
    }

    @Override
    public Collector<DATA> nextInto(final List<DATA> data) {

        data.add(next());

        return this;
    }

    @Override
    public Collector<DATA> now() {

        mDataGate.immediately();

        return this;
    }

    @Override
    public boolean hasNext() {

        return !mSizeGate.perform(mCollectorLeap.isEmptyAction());
    }

    @Override
    public DATA next() {

        return mDataGate.meets(HAS_DATA).perform(mCollectorLeap.pullAction());
    }

    @Override
    public void remove() {

        throw new UnsupportedOperationException();
    }
}