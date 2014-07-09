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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.Gate;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Default rapid gate implementation.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> The backed leap type.
 */
class DefaultRapidGate<TYPE> implements RapidGate<TYPE> {

    private final Gate<TYPE> mGate;

    private final Class<TYPE> mType;

    /**
     * Constructor.
     *
     * @param wrapped The wrapped gate.
     */
    public DefaultRapidGate(final Gate<?> wrapped) {

        this(wrapped, null);
    }

    /**
     * Constructor.
     *
     * @param wrapped   The wrapped gate.
     * @param gateClass The gate class.
     */
    public DefaultRapidGate(final Gate<?> wrapped, final Class<TYPE> gateClass) {

        if (wrapped == null) {

            throw new IllegalArgumentException("the wrapped gate cannot be null");
        }

        //noinspection unchecked
        mGate = (Gate<TYPE>) wrapped;
        mType = gateClass;
    }

    @Override
    public RapidGate<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mGate.afterMax(maxDelay, timeUnit);

        return this;
    }

    @Override
    public RapidGate<TYPE> eventually() {

        mGate.eventually();

        return this;
    }

    @Override
    public RapidGate<TYPE> eventuallyThrow(final RuntimeException exception) {

        mGate.eventuallyThrow(exception);

        return this;
    }

    @Override
    public RapidGate<TYPE> immediately() {

        mGate.immediately();

        return this;
    }

    @Override
    public RapidGate<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mGate.when(evaluator);

        return this;
    }

    @Override
    public <NTYPE> RapidGate<NTYPE> as(final Class<NTYPE> gateClass) {

        return new DefaultRapidGate<NTYPE>(mGate, gateClass);
    }

    @Override
    public <NTYPE> RapidGate<NTYPE> as(final Classification<NTYPE> gateClassification) {

        return as(gateClassification.getRawType());
    }

    @Override
    public TYPE perform() {

        //noinspection unchecked
        return (TYPE) Proxy.newProxyInstance(mType.getClassLoader(), new Class[]{mType},
                                             new GateInvocationHandler(mGate));
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return mGate.perform(action, args);
    }
}