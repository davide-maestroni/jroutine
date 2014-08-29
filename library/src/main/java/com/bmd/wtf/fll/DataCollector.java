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

import com.bmd.wtf.flw.Bridge;
import com.bmd.wtf.flw.Bridge.ConditionEvaluator;
import com.bmd.wtf.flw.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Collector implementation.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> the data type.
 */
class DataCollector<DATA> implements Collector<DATA> {

    private static final ConditionEvaluator<CollectorGate<?>> HAS_DATA =
            new ConditionEvaluator<CollectorGate<?>>() {

                @Override
                public boolean isSatisfied(final CollectorGate<?> gate) {

                    return (gate.size() > 0) || gate.isComplete();
                }
            };

    private static final ConditionEvaluator<CollectorGate<?>> IS_COMPLETE =
            new ConditionEvaluator<CollectorGate<?>>() {

                @Override
                public boolean isSatisfied(final CollectorGate<?> gate) {

                    return gate.isComplete();
                }
            };

    private final CollectorGate<DATA> mCollectorGate;

    private final Bridge<CollectorGate<DATA>> mDataBridge;

    /**
     * Constructor.
     *
     * @param bridge        the associated bridge.
     * @param collectorGate the associated collector gate.
     * @throws IllegalArgumentException if the bridge or the collector are null.
     */
    public DataCollector(final Bridge<CollectorGate<DATA>> bridge,
            final CollectorGate<DATA> collectorGate) {

        if (bridge == null) {

            throw new IllegalArgumentException("the bridge gate cannot be null");
        }

        if (collectorGate == null) {

            throw new IllegalArgumentException("the collector gate cannot be null");
        }

        mDataBridge = bridge.eventually();
        mCollectorGate = collectorGate;
    }

    @Override
    public Collector<DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mDataBridge.afterMax(maxDelay, timeUnit);

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

        mDataBridge.when(IS_COMPLETE).perform(mCollectorGate.pullAllAction(), data);

        return this;
    }

    @Override
    public Collector<DATA> eventually() {

        mDataBridge.eventually();

        return this;
    }

    @Override
    public Collector<DATA> eventuallyThrow(final RuntimeException exception) {

        mDataBridge.eventuallyThrow(exception);

        return this;
    }

    @Override
    public DATA next() {

        return mDataBridge.when(HAS_DATA).perform(mCollectorGate.pullAction());
    }

    @Override
    public Collector<DATA> nextInto(final List<DATA> data) {

        data.add(next());

        return this;
    }

    @Override
    public Collector<DATA> now() {

        mDataBridge.immediately();

        return this;
    }

    @Override
    public boolean hasNext() {

        return !mDataBridge.when(HAS_DATA).perform(mCollectorGate.isEmptyAction());
    }

    @Override
    public void remove() {

        throw new UnsupportedOperationException();
    }
}