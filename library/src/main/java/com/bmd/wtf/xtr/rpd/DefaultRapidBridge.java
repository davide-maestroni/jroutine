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
import com.bmd.wtf.flw.Bridge;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Default rapid bridge implementation.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> the backed gate type.
 */
class DefaultRapidBridge<TYPE> implements RapidBridge<TYPE> {

    private final Bridge<TYPE> mBridge;

    private final Class<TYPE> mType;

    private volatile ConditionEvaluator<? super TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped bridge.
     * @throws IllegalArgumentException if the wrapped bridge is null.
     */
    public DefaultRapidBridge(final Bridge<?> wrapped) {

        this(wrapped, null);
    }

    /**
     * Constructor.
     *
     * @param wrapped     the wrapped bridge.
     * @param bridgeClass the bridge class.
     * @throws IllegalArgumentException if the wrapped bridge is null.
     */
    public DefaultRapidBridge(final Bridge<?> wrapped, final Class<TYPE> bridgeClass) {

        if (wrapped == null) {

            throw new IllegalArgumentException("the wrapped bridge cannot be null");
        }

        //noinspection unchecked
        mBridge = (Bridge<TYPE>) wrapped;
        mType = bridgeClass;
    }

    @Override
    public RapidBridge<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(maxDelay);

        return this;
    }

    @Override
    public RapidBridge<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public RapidBridge<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public RapidBridge<TYPE> immediately() {

        mTimeoutMs = 0;

        return this;
    }

    @Override
    public RapidBridge<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    @Override
    public TYPE perform() {

        final Class<TYPE> type = mType;

        if (type == null) {

            throw new IllegalStateException("the bridge type is not specified");
        }

        return performAs(type);
    }

    @Override
    public <NTYPE> NTYPE performAs(final Class<NTYPE> bridgeClass) {

        if (!bridgeClass.isInterface()) {

            throw new IllegalArgumentException("the bridge type does not represent an interface");
        }

        final Class<TYPE> type = mType;

        if ((type != null) && !bridgeClass.isAssignableFrom(type)) {

            throw new IllegalArgumentException(
                    "the bridge is not of type: " + bridgeClass.getCanonicalName());
        }

        final Bridge<NTYPE> bridge = buildBridge(bridgeClass);

        //noinspection unchecked
        return (NTYPE) Proxy.newProxyInstance(bridgeClass.getClassLoader(),
                                              new Class[]{bridgeClass},
                                              new BridgeInvocationHandler(bridge));
    }

    @Override
    public <NTYPE> NTYPE performAs(final Classification<NTYPE> bridgeClassification) {

        return performAs(bridgeClassification.getRawType());
    }

    @Override
    public RapidBridge<TYPE> whenSatisfies(final Object... args) {

        return when(new RapidConditionEvaluator<TYPE>(args));
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return buildBridge(mType).perform(action, args);
    }

    private <NTYPE> Bridge<NTYPE> buildBridge(
            @SuppressWarnings("UnusedParameters") final Class<NTYPE> bridgeClass) {

        final long timeoutMs = mTimeoutMs;
        final RuntimeException timeoutException = mTimeoutException;
        final ConditionEvaluator<? super TYPE> conditionEvaluator = mEvaluator;

        //noinspection unchecked
        final Bridge<NTYPE> bridge = (Bridge<NTYPE>) mBridge;

        if (timeoutMs < 0) {

            bridge.eventually();

        } else if (timeoutMs == 0) {

            bridge.immediately();

        } else {

            bridge.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        //noinspection unchecked
        return bridge.eventuallyThrow(timeoutException)
                     .when((ConditionEvaluator<? super NTYPE>) conditionEvaluator);
    }
}