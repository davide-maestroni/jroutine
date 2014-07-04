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
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.fll.WaterfallRiver;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.Condition;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Default rapid river implementation.
 * <p/>
 * Created by davide on 6/20/14.
 *
 * @param <SOURCE> The source data type.
 * @param <MOUTH>  The mouth data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 * @param <TYPE>   The gate type.
 */
public class DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> extends WaterfallRiver<SOURCE, IN>
        implements RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> {

    private final Classification<TYPE> mClassification;

    private final ConditionEvaluator<? super TYPE> mEvaluator;

    private final TYPE mGateLeap;

    private final Waterfall<SOURCE, MOUTH, OUT> mMouthWaterfall;

    private final Waterfall<SOURCE, SOURCE, ?> mSourceWaterfall;

    private final RuntimeException mTimeoutException;

    private final long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param waterfall The wrapped waterfall.
     */
    DefaultRapidRiver(final Waterfall<SOURCE, MOUTH, OUT> waterfall) {

        this(waterfall.source(), waterfall, null, null, 0, null, null);
    }

    private DefaultRapidRiver(final Waterfall<SOURCE, MOUTH, OUT> waterfall,
            final Classification<TYPE> classification, final TYPE gateLeap, final long timeoutMs,
            final RuntimeException exception, final ConditionEvaluator<? super TYPE> evaluator) {

        this(waterfall.source(), waterfall, classification, gateLeap, timeoutMs, exception,
             evaluator);
    }

    private DefaultRapidRiver(final Waterfall<SOURCE, SOURCE, ?> sourceWaterfall,
            final Waterfall<SOURCE, MOUTH, OUT> mouthWaterfall,
            final Classification<TYPE> classification, final TYPE gateLeap, final long timeoutMs,
            final RuntimeException exception, final ConditionEvaluator<? super TYPE> evaluator) {

        //noinspection unchecked
        super((Waterfall<SOURCE, IN, ?>) sourceWaterfall, true);

        //TODO: exceptions

        mSourceWaterfall = sourceWaterfall;
        mMouthWaterfall = mouthWaterfall;
        mClassification = classification;
        mGateLeap = gateLeap;
        mTimeoutMs = timeoutMs;
        mTimeoutException = exception;
        mEvaluator = evaluator;
    }

    @SuppressWarnings("ConstantConditions")
    private static Method findCondition(final Method[] methods, final Object[] args) {

        Method conditionMethod = null;

        boolean isAnnotated = false;
        boolean isClashing = false;

        int confidenceLevel = -1;

        final int length = args.length;

        final Class<?>[] argClasses = new Class[length];

        for (int i = 0; i < length; ++i) {

            argClasses[i] = args[i].getClass();
        }

        for (final Method method : methods) {

            if (boolean.class.equals(method.getReturnType())) {

                final Class<?>[] params = method.getParameterTypes();

                if (length != params.length) {

                    continue;
                }

                boolean isMatching = true;

                for (int i = 0; i < length && isMatching; ++i) {

                    final Class<?> argClass = argClasses[i];
                    final Class<?> param = params[i];

                    if ((argClass != null) ? !param.isAssignableFrom(argClass)
                            : param.isPrimitive()) {

                        isMatching = false;

                        break;
                    }
                }

                if (isMatching) {

                    if (conditionMethod == null) {

                        conditionMethod = method;

                        isAnnotated = method.isAnnotationPresent(Condition.class);

                        continue;

                    } else if (isAnnotated) {

                        if (!method.isAnnotationPresent(Condition.class)) {

                            continue;
                        }

                    } else if (method.isAnnotationPresent(Condition.class)) {

                        conditionMethod = method;

                        isAnnotated = true;
                        isClashing = false;

                        confidenceLevel = -1;

                        continue;
                    }

                    int confidence = 0;

                    for (int i = 0; i < length && isMatching; ++i) {

                        final Class<?> argClass = argClasses[i];
                        final Class<?> param = params[i];

                        if ((argClass != null) && param.equals(argClass)) {

                            ++confidence;
                        }
                    }

                    if (confidence > confidenceLevel) {

                        conditionMethod = method;

                        confidenceLevel = confidence;

                    } else if (confidence == confidenceLevel) {

                        isClashing = true;
                    }
                }
            }
        }

        if (isClashing) {

            throw new IllegalArgumentException(
                    "more than one condition method found for arguments: " + Arrays.toString(args));
        }

        return conditionMethod;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate() {

        deviate();

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate(final int streamNumber) {

        deviateStream(streamNumber);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain() {

        drain();

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain(final int streamNumber) {

        drainStream(streamNumber);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> afterMax(final long maxDelay,
            final TimeUnit timeUnit) {

        final long timeoutMs = Math.max(0, timeUnit.toMillis(maxDelay));

        if (mTimeoutMs != timeoutMs) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       timeoutMs, mTimeoutException,
                                                                       mEvaluator);
        }

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> eventually() {

        if (mTimeoutMs != -1) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       -1, mTimeoutException,
                                                                       mEvaluator);
        }

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> eventuallyThrow(
            final RuntimeException exception) {

        final RuntimeException timeoutException = mTimeoutException;

        if ((timeoutException == null) ? (exception != null)
                : !timeoutException.equals(exception)) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       mTimeoutMs, exception,
                                                                       mEvaluator);
        }

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> immediately() {

        if (mTimeoutMs != 0) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       0, mTimeoutException,
                                                                       mEvaluator);
        }

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> when(
            final ConditionEvaluator<? super TYPE> evaluator) {

        final ConditionEvaluator<? super TYPE> currentEvaluator = mEvaluator;

        if ((currentEvaluator == null) ? (evaluator != null)
                : !currentEvaluator.equals(evaluator)) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       mTimeoutMs,
                                                                       mTimeoutException,
                                                                       evaluator);
        }

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, MOUTH, OUT, TYPE> mouth() {

        return new DefaultRapidRiver<SOURCE, MOUTH, MOUTH, OUT, TYPE>(mMouthWaterfall,
                                                                      mClassification, mGateLeap,
                                                                      mTimeoutMs, mTimeoutException,
                                                                      mEvaluator);
    }

    @Override
    public TYPE perform() {

        final Classification<TYPE> classification = mClassification;

        if ((classification == null) || !classification.isInterface()) {

            throw new IllegalArgumentException(
                    "the gate classification must represent an interface");
        }

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
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> whenSatisfies(final Object... args) {

        return when(new GateConditionEvaluator<TYPE>(args));
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> discharge() {

        super.discharge();

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> discharge(final IN... drops) {

        super.discharge(drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> discharge(final Iterable<? extends IN> drops) {

        super.discharge(drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> discharge(final IN drop) {

        super.discharge(drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeAfter(final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.dischargeAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeAfter(final long delay,
            final TimeUnit timeUnit, final IN drop) {

        super.dischargeAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeAfter(final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        super.dischargeAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> forward(final Throwable throwable) {

        super.forward(throwable);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN... drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> push(final Iterable<? extends IN> drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN drop) {

        super.push(drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN drop) {

        super.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStream(final int streamNumber) {

        super.dischargeStream(streamNumber);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStream(final int streamNumber,
            final IN... drops) {

        super.dischargeStream(streamNumber, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStream(final int streamNumber,
            final Iterable<? extends IN> drops) {

        super.dischargeStream(streamNumber, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStream(final int streamNumber,
            final IN drop) {

        super.dischargeStream(streamNumber, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN drop) {

        super.dischargeStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN... drops) {

        super.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> forwardStream(final int streamNumber,
            final Throwable throwable) {

        super.forwardStream(streamNumber, throwable);

        return this;
    }

    @Override
    public <NTYPE> RapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE> on(final Class<NTYPE> gateClass) {

        return on(Classification.ofType(gateClass));
    }

    @Override
    public <NTYPE> RapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE> on(NTYPE leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the gate leap cannot be null");
        }

        if (leap != mGateLeap) {

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE>(mMouthWaterfall, null, leap,
                                                                        mTimeoutMs,
                                                                        mTimeoutException, null);
        }

        //noinspection unchecked
        return (RapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE>) this;
    }

    @Override
    public <NTYPE> RapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE> on(
            final Classification<NTYPE> gateClassification) {

        if (gateClassification == null) {

            throw new IllegalArgumentException("the gate classification cannot be null");
        }

        final Classification<TYPE> currentClassification = mClassification;

        if ((currentClassification == null) || !currentClassification.equals(gateClassification)) {

            final ConditionEvaluator<NTYPE> evaluator;

            if ((currentClassification == null) || currentClassification.isAssignableFrom(
                    gateClassification)) {

                //noinspection unchecked
                evaluator = (ConditionEvaluator<NTYPE>) mEvaluator;

            } else {

                evaluator = null;
            }

            return new DefaultRapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE>(mMouthWaterfall,
                                                                        gateClassification, null,
                                                                        mTimeoutMs,
                                                                        mTimeoutException,
                                                                        evaluator);
        }

        //noinspection unchecked
        return (RapidRiver<SOURCE, MOUTH, IN, OUT, NTYPE>) this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(final int streamNumber,
            final IN... drops) {

        super.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(final int streamNumber,
            final Iterable<? extends IN> drops) {

        super.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(final int streamNumber,
            final IN drop) {

        super.pushStream(streamNumber, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN drop) {

        super.pushStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN... drops) {

        super.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidRiver<SOURCE, MOUTH, SOURCE, OUT, TYPE> source() {

        return new DefaultRapidRiver<SOURCE, MOUTH, SOURCE, OUT, TYPE>(mSourceWaterfall,
                                                                       mMouthWaterfall,
                                                                       mClassification, mGateLeap,
                                                                       mTimeoutMs,
                                                                       mTimeoutException,
                                                                       mEvaluator);
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        return getGate().perform(action, args);
    }

    private Gate<TYPE> getGate() {

        final TYPE gateLeap = mGateLeap;

        final long timeoutMs = mTimeoutMs;

        final Gate<TYPE> gate;

        if (gateLeap == null) {

            gate = mMouthWaterfall.on(mClassification)
                                  .when(mEvaluator)
                                  .eventuallyThrow(mTimeoutException);

        } else {

            gate = mMouthWaterfall.on(gateLeap).when(mEvaluator).eventuallyThrow(mTimeoutException);
        }

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
}