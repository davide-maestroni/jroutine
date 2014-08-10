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
import com.bmd.wtf.flw.Dam;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Default rapid dam implementation.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> the backed gate type.
 */
class DefaultRapidDam<TYPE> implements RapidDam<TYPE> {

    private final Dam<TYPE> mDam;

    private final Class<TYPE> mType;

    private volatile ConditionEvaluator<? super TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped dam.
     */
    public DefaultRapidDam(final Dam<?> wrapped) {

        this(wrapped, null);
    }

    /**
     * Constructor.
     *
     * @param wrapped  the wrapped dam.
     * @param damClass the dam class.
     */
    public DefaultRapidDam(final Dam<?> wrapped, final Class<TYPE> damClass) {

        if (wrapped == null) {

            throw new IllegalArgumentException("the wrapped dam cannot be null");
        }

        //noinspection unchecked
        mDam = (Dam<TYPE>) wrapped;
        mType = damClass;
    }

    @Override
    public RapidDam<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(maxDelay);

        return this;
    }

    @Override
    public RapidDam<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public RapidDam<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public RapidDam<TYPE> immediately() {

        mTimeoutMs = 0;

        return this;
    }

    @Override
    public RapidDam<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    @Override
    public TYPE perform() {

        final Class<TYPE> type = mType;

        if (type == null) {

            throw new IllegalStateException("the dam type is not specified");
        }

        return performAs(type);
    }

    @Override
    public <NTYPE> NTYPE performAs(final Class<NTYPE> damClass) {

        if (!damClass.isInterface()) {

            throw new IllegalArgumentException("the dam type does not represent an interface");
        }

        final Class<TYPE> type = mType;

        if ((type != null) && !damClass.isAssignableFrom(type)) {

            throw new IllegalArgumentException(
                    "the dam is not of the specified type: " + damClass.getCanonicalName());
        }

        final Dam<NTYPE> dam = buildDam(damClass);

        //noinspection unchecked
        return (NTYPE) Proxy.newProxyInstance(damClass.getClassLoader(), new Class[]{damClass},
                                              new DamInvocationHandler(dam));
    }

    @Override
    public <NTYPE> NTYPE performAs(final Classification<NTYPE> damClassification) {

        return performAs(damClassification.getRawType());
    }

    @Override
    public RapidDam<TYPE> whenSatisfies(final Object... args) {

        return when(new RapidConditionEvaluator<TYPE>(args));
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return buildDam(mType).perform(action, args);
    }

    private <NTYPE> Dam<NTYPE> buildDam(
            @SuppressWarnings("UnusedParameters") final Class<NTYPE> damClass) {

        final long timeoutMs = mTimeoutMs;
        final RuntimeException timeoutException = mTimeoutException;
        final ConditionEvaluator<? super TYPE> conditionEvaluator = mEvaluator;

        //noinspection unchecked
        final Dam<NTYPE> dam = (Dam<NTYPE>) mDam;

        if (timeoutMs < 0) {

            dam.eventually();

        } else if (timeoutMs == 0) {

            dam.immediately();

        } else {

            dam.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        //noinspection unchecked
        return dam.eventuallyThrow(timeoutException)
                  .when((ConditionEvaluator<? super NTYPE>) conditionEvaluator);
    }
}