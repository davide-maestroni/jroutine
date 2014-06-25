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

import com.bmd.wtf.flg.Gate;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.fll.WaterfallRiver;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.Condition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/20/14.
 */
public class WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE> extends WaterfallRiver<SOURCE, IN>
        implements RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> {

    private final Classification<TYPE> mClassification;

    private final ConditionEvaluator<? super TYPE> mEvaluator;

    private final Waterfall<SOURCE, MOUTH, OUT> mMouthWaterfall;

    private final Waterfall<SOURCE, SOURCE, ?> mSourceWaterfall;

    private final RuntimeException mTimeoutException;

    private final long mTimeoutMs;

    WaterfallRapidGate(final Waterfall<SOURCE, MOUTH, OUT> waterfall) {

        this(waterfall.source(), waterfall, null, 0, null, null);
    }

    private WaterfallRapidGate(final Waterfall<SOURCE, MOUTH, OUT> waterfall,
            final Classification<TYPE> classification, final long timeoutMs,
            final RuntimeException exception, final ConditionEvaluator<? super TYPE> evaluator) {

        this(waterfall.source(), waterfall, classification, timeoutMs, exception, evaluator);
    }

    private WaterfallRapidGate(final Waterfall<SOURCE, SOURCE, ?> sourceWaterfall,
            final Waterfall<SOURCE, MOUTH, OUT> mouthWaterfall,
            final Classification<TYPE> classification, final long timeoutMs,
            final RuntimeException exception, final ConditionEvaluator<? super TYPE> evaluator) {

        //noinspection unchecked
        super((Waterfall<SOURCE, IN, ?>) sourceWaterfall, true);

        mSourceWaterfall = sourceWaterfall;
        mMouthWaterfall = mouthWaterfall;
        mClassification = classification;
        mTimeoutMs = timeoutMs;
        mTimeoutException = exception;
        mEvaluator = evaluator;
    }

    private static Method findCondition(final Method[] methods, final Object[] args) {

        Method annotatedConditionMethod = null;
        Method conditionMethod = null;

        final int length = args.length;

        for (final Method method : methods) {

            if (boolean.class.equals(method.getReturnType())) {

                final Class<?>[] params = method.getParameterTypes();

                if (length != params.length) {

                    continue;
                }

                boolean isMatching = true;

                for (int i = 0; i < length && isMatching; i++) {

                    final Object arg = args[i];
                    final Class<?> param = params[i];

                    if (arg == null) {

                        if (param.isPrimitive()) {

                            isMatching = false;
                        }

                    } else if (!arg.getClass().equals(param)) {

                        isMatching = false;
                    }
                }

                if (isMatching) {

                    if (method.isAnnotationPresent(Condition.class)) {

                        annotatedConditionMethod = method;

                    } else {

                        conditionMethod = method;
                    }
                }
            }
        }

        if (annotatedConditionMethod != null) {

            return annotatedConditionMethod;
        }

        return conditionMethod;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate() {

        deviate();

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate(final int streamNumber) {

        deviate(streamNumber);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain() {

        drain();

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain(final int streamNumber) {

        drain(streamNumber);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterMax(final long maxDelay,
            final TimeUnit timeUnit) {

        final long timeoutMs = Math.max(0, timeUnit.toMillis(maxDelay));

        if (mTimeoutMs != timeoutMs) {

            return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                        mClassification, timeoutMs,
                                                                        mTimeoutException,
                                                                        mEvaluator);
        }

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventually() {

        if (mTimeoutMs != -1) {

            return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                        mClassification, -1,
                                                                        mTimeoutException,
                                                                        mEvaluator);
        }

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventuallyThrow(
            final RuntimeException exception) {

        final RuntimeException timeoutException = mTimeoutException;

        if ((timeoutException == null) ? (exception != null)
                : !timeoutException.equals(exception)) {

            return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                        mClassification, mTimeoutMs,
                                                                        exception, mEvaluator);
        }

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> meets(
            final ConditionEvaluator<? super TYPE> evaluator) {

        final ConditionEvaluator<? super TYPE> currentEvaluator = mEvaluator;

        if ((currentEvaluator == null) ? (evaluator != null)
                : !currentEvaluator.equals(evaluator)) {

            return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                        mClassification, mTimeoutMs,
                                                                        mTimeoutException,
                                                                        evaluator);
        }

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> meetsCondition(final Object... args) {

        return meets(new GateConditionEvaluator<TYPE>(args));
    }

    @Override
    public RapidGate<SOURCE, MOUTH, MOUTH, OUT, TYPE> mouth() {

        return new WaterfallRapidGate<SOURCE, MOUTH, MOUTH, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mTimeoutMs,
                                                                       mTimeoutException,
                                                                       mEvaluator);
    }

    @Override
    public TYPE perform() {

        final Classification<TYPE> classification = mClassification;

        //noinspection unchecked
        return (TYPE) Proxy.newProxyInstance(classification.getClass().getClassLoader(),
                                             new Class[]{classification.getRawType()},
                                             new GateInvocationHandler(getGate()));
    }

    @Override
    public Waterfall<SOURCE, MOUTH, OUT> waterfall() {

        return mMouthWaterfall;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> discharge(final int streamNumber) {

        super.discharge(streamNumber);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> discharge() {

        super.discharge();

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forward(final Throwable throwable) {

        super.forward(throwable);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN... drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final Iterable<? extends IN> drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN drop) {

        super.push(drop);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN drop) {

        super.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forward(final int streamNumber,
            final Throwable throwable) {

        super.forward(streamNumber, throwable);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber, final IN... drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber,
            final Iterable<? extends IN> drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber, final IN drop) {

        super.push(streamNumber, drop);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN drop) {

        super.pushAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN... drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidGate<SOURCE, MOUTH, SOURCE, OUT, TYPE> source() {

        return new WaterfallRapidGate<SOURCE, MOUTH, SOURCE, OUT, TYPE>(mSourceWaterfall,
                                                                        mMouthWaterfall,
                                                                        mClassification, mTimeoutMs,
                                                                        mTimeoutException,
                                                                        mEvaluator);
    }

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> when(final Class<NTYPE> gateType) {

        return when(Classification.from(gateType));
    }

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> when(
            final Classification<NTYPE> gateClassification) {

        if (!gateClassification.isInterface()) {

            throw new IllegalArgumentException(
                    "the gate classification must represent an interface");
        }

        final Classification<TYPE> currentClassification = mClassification;

        if ((currentClassification == null) || !currentClassification.equals(gateClassification)) {

            final ConditionEvaluator<NTYPE> evaluator;

            if ((currentClassification == null) || currentClassification
                    .isAssignableFrom(gateClassification)) {

                //noinspection unchecked
                evaluator = (ConditionEvaluator<NTYPE>) mEvaluator;

            } else {

                evaluator = null;
            }

            return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, NTYPE>(mMouthWaterfall,
                                                                         gateClassification,
                                                                         mTimeoutMs,
                                                                         mTimeoutException,
                                                                         evaluator);
        }

        //noinspection unchecked
        return (RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE>) this;
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return getGate().perform(action, args);
    }

    private Gate<TYPE> getGate() {

        final long timeoutMs = mTimeoutMs;

        final Gate<TYPE> gate = mMouthWaterfall.when(mClassification).meets(mEvaluator)
                                               .eventuallyThrow(mTimeoutException);

        if (timeoutMs < 0) {

            gate.eventually();

        } else {

            gate.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        return gate;
    }

    private static class GateConditionEvaluator<TYPE> implements ConditionEvaluator<TYPE> {

        private final Object[] mArgs;

        private volatile Method mCondition;

        public GateConditionEvaluator(final Object[] args) {

            mArgs = args.clone();
        }

        @Override
        public boolean isSatisfied(final TYPE leap) {

            try {

                return (Boolean) getCondition(leap).invoke(leap, mArgs);

            } catch (final Throwable t) {

                throw new UndeclaredThrowableException(t);
            }
        }

        private Method getCondition(final TYPE leap) {

            if (mCondition == null) {

                final Object[] args = mArgs;

                final Class<?> type = leap.getClass();

                Method condition = findCondition(type.getMethods(), args);

                if (condition == null) {

                    condition = findCondition(type.getDeclaredMethods(), args);

                    if (condition == null) {

                        throw new IllegalArgumentException(
                                "no suitable method found for type " + type);
                    }
                }

                if (!condition.isAccessible()) {

                    condition.setAccessible(true);
                }

                mCondition = condition;
            }

            return mCondition;
        }
    }

    private static class GateInvocationHandler<TYPE>
            implements InvocationHandler, Action<Object, TYPE> {

        private final Gate<TYPE> mControl;

        public GateInvocationHandler(final Gate<TYPE> control) {

            mControl = control;
        }

        @Override
        public Object doOn(final TYPE leap, final Object... args) {

            try {

                return ((Method) args[0]).invoke(leap, (Object[]) args[1]);

            } catch (final Throwable t) {

                throw new UndeclaredThrowableException(t);
            }
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            return mControl.perform(this, method, args);
        }
    }
}