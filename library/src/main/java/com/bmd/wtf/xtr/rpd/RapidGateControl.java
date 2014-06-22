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

import com.bmd.wtf.flg.GateControl;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.fll.WaterfallRiver;
import com.bmd.wtf.xtr.rpd.Rapids.Condition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/20/14.
 */
public class RapidGateControl<SOURCE, MOUTH, IN, OUT, TYPE> extends WaterfallRiver<SOURCE, IN>
        implements RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> {

    private final ConditionEvaluator<TYPE> mEvaluator;

    private final Classification<TYPE> mGate;

    private final Waterfall<SOURCE, MOUTH, OUT> mMouthWaterfall;

    private final Waterfall<SOURCE, SOURCE, ?> mSourceWaterfall;

    private final RuntimeException mTimeoutException;

    private final long mTimeoutMs;

    RapidGateControl(final Waterfall<SOURCE, MOUTH, OUT> waterfall) {

        //noinspection unchecked
        super((Waterfall<SOURCE, IN, OUT>) waterfall, true);

        mSourceWaterfall = waterfall.source();
        mMouthWaterfall = waterfall;
        mGate = null;
        mTimeoutMs = 0;
        mTimeoutException = null;
        mEvaluator = null;
    }

    private RapidGateControl(final Waterfall<SOURCE, MOUTH, OUT> waterfall,
            final Classification<TYPE> gate, final long timeoutMs, final RuntimeException exception,
            final ConditionEvaluator<TYPE> evaluator) {

        //noinspection unchecked
        super((Waterfall<SOURCE, IN, OUT>) waterfall, true);

        mSourceWaterfall = waterfall.source();
        mMouthWaterfall = waterfall;
        mGate = gate;
        mTimeoutMs = timeoutMs;
        mTimeoutException = exception;
        mEvaluator = evaluator;
    }

    private RapidGateControl(final Waterfall<SOURCE, SOURCE, ?> sourceWaterfall,
            final Waterfall<SOURCE, MOUTH, OUT> mouthWaterfall, final Classification<TYPE> gate,
            final long timeoutMs, final RuntimeException exception,
            final ConditionEvaluator<TYPE> evaluator) {

        //noinspection unchecked
        super((Waterfall<SOURCE, IN, ?>) sourceWaterfall, true);

        mSourceWaterfall = sourceWaterfall;
        mMouthWaterfall = mouthWaterfall;
        mGate = gate;
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
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain() {

        drain();

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain(final int streamNumber) {

        drain(streamNumber);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> afterDryUp() {

        dryUp();

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> afterDryUp(final int streamNumber) {

        dryUp(streamNumber);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> afterMax(final long maxDelay,
            final TimeUnit timeUnit) {

        final long timeoutMs = Math.max(0, timeUnit.toMillis(maxDelay));

        if (mTimeoutMs != timeoutMs) {

            return new RapidGateControl<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall, mGate,
                                                                      timeoutMs, mTimeoutException,
                                                                      mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> eventually() {

        if (mTimeoutMs != -1) {

            return new RapidGateControl<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall, mGate, -1,
                                                                      mTimeoutException,
                                                                      mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> eventuallyThrow(
            final RuntimeException exception) {

        final RuntimeException timeoutException = mTimeoutException;

        if ((timeoutException == null) ? (exception != null)
                : !timeoutException.equals(exception)) {

            return new RapidGateControl<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall, mGate,
                                                                      mTimeoutMs, exception,
                                                                      mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> meets(
            final ConditionEvaluator<TYPE> evaluator) {

        final ConditionEvaluator<TYPE> currentEvaluator = mEvaluator;

        if ((currentEvaluator == null) ? (evaluator != null)
                : !currentEvaluator.equals(evaluator)) {

            return new RapidGateControl<SOURCE, MOUTH, IN, OUT, TYPE>(mMouthWaterfall, mGate,
                                                                      mTimeoutMs, mTimeoutException,
                                                                      evaluator);
        }

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> meetsCondition(final Object... args) {

        return meets(new GateConditionEvaluator<TYPE>(args));
    }

    @Override
    public RapidControl<SOURCE, MOUTH, MOUTH, OUT, TYPE> mouth() {

        return new RapidGateControl<SOURCE, MOUTH, MOUTH, OUT, TYPE>(mMouthWaterfall, mGate,
                                                                     mTimeoutMs, mTimeoutException,
                                                                     mEvaluator);
    }

    @Override
    public TYPE perform() {

        final Classification<TYPE> gate = mGate;

        //noinspection unchecked
        return (TYPE) Proxy
                .newProxyInstance(gate.getClass().getClassLoader(), new Class[]{gate.getRawType()},
                                  new GateInvocationHandler(getControl()));
    }

    @Override
    public Waterfall<SOURCE, MOUTH, OUT> waterfall() {

        return mMouthWaterfall;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> flush(final int streamNumber) {

        super.flush(streamNumber);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> flush() {

        super.flush();

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> forward(final Throwable throwable) {

        super.forward(throwable);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN... drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final Iterable<? extends IN> drops) {

        super.push(drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final IN drop) {

        super.push(drop);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN drop) {

        super.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> forward(final int streamNumber,
            final Throwable throwable) {

        super.forward(streamNumber, throwable);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber,
            final IN... drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber,
            final Iterable<? extends IN> drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> push(final int streamNumber, final IN drop) {

        super.push(streamNumber, drop);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN drop) {

        super.pushAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(final int streamNumber,
            final long delay, final TimeUnit timeUnit, final IN... drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public RapidControl<SOURCE, MOUTH, SOURCE, OUT, TYPE> source() {

        return new RapidGateControl<SOURCE, MOUTH, SOURCE, OUT, TYPE>(mSourceWaterfall,
                                                                      mMouthWaterfall, mGate,
                                                                      mTimeoutMs, mTimeoutException,
                                                                      mEvaluator);
    }

    @Override
    public <NTYPE> RapidControl<SOURCE, MOUTH, IN, OUT, NTYPE> when(final Class<NTYPE> type) {

        return when(Classification.from(type));
    }

    @Override
    public <NTYPE> RapidControl<SOURCE, MOUTH, IN, OUT, NTYPE> when(
            final Classification<NTYPE> gate) {

        if (!gate.isInterface()) {

            throw new IllegalArgumentException("the gate must be an interface");
        }

        final Classification<TYPE> currentGate = mGate;

        if ((currentGate == null) || !currentGate.equals(gate)) {

            final ConditionEvaluator<NTYPE> evaluator;

            if ((currentGate == null) || currentGate.isAssignableFrom(gate)) {

                //noinspection unchecked
                evaluator = (ConditionEvaluator<NTYPE>) mEvaluator;

            } else {

                evaluator = null;
            }

            return new RapidGateControl<SOURCE, MOUTH, IN, OUT, NTYPE>(mMouthWaterfall, gate,
                                                                       mTimeoutMs,
                                                                       mTimeoutException,
                                                                       evaluator);
        }

        //noinspection unchecked
        return (RapidControl<SOURCE, MOUTH, IN, OUT, NTYPE>) this;
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, TYPE> action, final Object... args) {

        return getControl().perform(action, args);
    }

    private GateControl<TYPE> getControl() {

        final long timeoutMs = mTimeoutMs;

        final GateControl<TYPE> gateControl =
                mMouthWaterfall.when(mGate).meets(mEvaluator).eventuallyThrow(mTimeoutException);

        if (timeoutMs < 0) {

            gateControl.eventually();

        } else {

            gateControl.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        return gateControl;
    }

    private static class GateConditionEvaluator<TYPE> implements ConditionEvaluator<TYPE> {

        private final Object[] mArgs;

        private volatile Method mCondition;

        public GateConditionEvaluator(final Object[] args) {

            mArgs = args.clone();
        }

        @Override
        public boolean isSatisfied(final TYPE gate) {

            try {

                return (Boolean) getCondition(gate).invoke(gate, mArgs);

            } catch (final Throwable t) {

                throw new UndeclaredThrowableException(t);
            }
        }

        private Method getCondition(final TYPE gate) {

            if (mCondition == null) {

                final Object[] args = mArgs;

                final Class<?> type = gate.getClass();

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

        private final GateControl<TYPE> mControl;

        public GateInvocationHandler(final GateControl<TYPE> control) {

            mControl = control;
        }

        @Override
        public Object doOn(final TYPE gate, final Object... args) {

            try {

                return ((Method) args[0]).invoke(gate, (Object[]) args[1]);

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