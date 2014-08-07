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
 * @param <TYPE> the backed leap type.
 */
class DefaultRapidGate<TYPE> implements RapidGate<TYPE> {

    private final Gate<TYPE> mGate;

    private final Class<TYPE> mType;

    private volatile ConditionEvaluator<? super TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped gate.
     */
    public DefaultRapidGate(final Gate<?> wrapped) {

        this(wrapped, null);
    }

    /**
     * Constructor.
     *
     * @param wrapped   the wrapped gate.
     * @param gateClass the gate class.
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

        mTimeoutMs = timeUnit.toMillis(maxDelay);

        return this;
    }

    @Override
    public RapidGate<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public RapidGate<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public RapidGate<TYPE> immediately() {

        mTimeoutMs = 0;

        return this;
    }

    @Override
    public RapidGate<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    @Override
    public TYPE perform() {

        final Class<TYPE> type = mType;

        if (type == null) {

            throw new IllegalStateException("the gate type is not specified");
        }

        return performAs(type);
    }

    @Override
    public <NTYPE> NTYPE performAs(final Class<NTYPE> gateClass) {

        if (!gateClass.isInterface()) {

            throw new IllegalArgumentException("the gate type does not represent an interface");
        }

        final Class<TYPE> type = mType;

        if ((type != null) && !gateClass.isAssignableFrom(type)) {

            throw new IllegalArgumentException(
                    "the gate is not of the specified type: " + gateClass.getCanonicalName());
        }

        final Gate<NTYPE> gate = buildGate(gateClass);

        //noinspection unchecked
        return (NTYPE) Proxy.newProxyInstance(gateClass.getClassLoader(), new Class[]{gateClass},
                                              new GateInvocationHandler(gate));
    }

    @Override
    public <NTYPE> NTYPE performAs(final Classification<NTYPE> gateClassification) {

        return performAs(gateClassification.getRawType());
    }

    @Override
    public RapidGate<TYPE> whenSatisfies(final Object... args) {

        return when(new GateConditionEvaluator<TYPE>(args));
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return buildGate(mType).perform(action, args);
    }

    private <NTYPE> Gate<NTYPE> buildGate(
            @SuppressWarnings("UnusedParameters") final Class<NTYPE> gateClass) {

        final long timeoutMs = mTimeoutMs;
        final RuntimeException timeoutException = mTimeoutException;
        final ConditionEvaluator<? super TYPE> conditionEvaluator = mEvaluator;

        //noinspection unchecked
        final Gate<NTYPE> gate = (Gate<NTYPE>) mGate;

        if (timeoutMs < 0) {

            gate.eventually();

        } else if (timeoutMs == 0) {

            gate.immediately();

        } else {

            gate.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        //noinspection unchecked
        return gate.eventuallyThrow(timeoutException)
                   .when((ConditionEvaluator<? super NTYPE>) conditionEvaluator);
    }
}