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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/20/14.
 */
public class RapidGateControl<TYPE> implements RapidControl<TYPE> {

    private final ConditionEvaluator<TYPE> mEvaluator;

    private final Classification<TYPE> mGate;

    private final RuntimeException mTimeoutException;

    private final long mTimeoutMs;

    private final Waterfall<?, ?, ?> mWaterfall;

    RapidGateControl(final Waterfall<?, ?, ?> waterfall) {

        mWaterfall = waterfall;
        mGate = null;
        mTimeoutMs = 0;
        mTimeoutException = null;
        mEvaluator = null;
    }

    private RapidGateControl(final Waterfall<?, ?, ?> waterfall, final Classification<TYPE> gate,
            final long timeoutMs, final RuntimeException exception,
            final ConditionEvaluator<TYPE> evaluator) {

        mWaterfall = waterfall;
        mGate = gate;
        mTimeoutMs = timeoutMs;
        mTimeoutException = exception;
        mEvaluator = evaluator;
    }

    @Override
    public RapidControl<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        final long timeoutMs = Math.max(0, timeUnit.toMillis(maxDelay));

        if (mTimeoutMs != timeoutMs) {

            return new RapidGateControl<TYPE>(mWaterfall, mGate, timeoutMs, mTimeoutException,
                                              mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<TYPE> eventually() {

        if (mTimeoutMs != -1) {

            return new RapidGateControl<TYPE>(mWaterfall, mGate, -1, mTimeoutException, mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<TYPE> eventuallyThrow(final RuntimeException exception) {

        final RuntimeException timeoutException = mTimeoutException;

        if ((timeoutException == null) ? (exception != null)
                : !timeoutException.equals(exception)) {

            return new RapidGateControl<TYPE>(mWaterfall, mGate, mTimeoutMs, exception, mEvaluator);
        }

        return this;
    }

    @Override
    public RapidControl<TYPE> meets(final ConditionEvaluator<TYPE> evaluator) {

        final ConditionEvaluator<TYPE> currentEvaluator = mEvaluator;

        if ((currentEvaluator == null) ? (evaluator != null)
                : !currentEvaluator.equals(evaluator)) {

            return new RapidGateControl<TYPE>(mWaterfall, mGate, mTimeoutMs, mTimeoutException,
                                              evaluator);
        }

        return this;
    }

    @Override
    public <NTYPE> RapidControl<NTYPE> as(final Class<NTYPE> gateType) {

        final Classification<TYPE> currentGate = mGate;

        if ((currentGate == null) ? (gateType != null)
                : !currentGate.getRawType().equals(gateType)) {

            final Classification<NTYPE> gate = Classification.from(gateType);

            final ConditionEvaluator<NTYPE> evaluator;

            if ((currentGate == null) || currentGate.isAssignableFrom(gate)) {

                //noinspection unchecked
                evaluator = (ConditionEvaluator<NTYPE>) mEvaluator;

            } else {

                evaluator = null;
            }

            return new RapidGateControl<NTYPE>(mWaterfall, gate, mTimeoutMs, mTimeoutException,
                                               evaluator);
        }

        //noinspection unchecked
        return (RapidControl<NTYPE>) this;
    }

    @Override
    public <NTYPE> RapidControl<NTYPE> as(final Classification<NTYPE> gate) {

        final Classification<TYPE> currentGate = mGate;

        if ((currentGate == null) ? (gate != null) : !currentGate.equals(gate)) {
            final ConditionEvaluator<NTYPE> evaluator;

            if ((currentGate == null) || currentGate.isAssignableFrom(gate)) {

                //noinspection unchecked
                evaluator = (ConditionEvaluator<NTYPE>) mEvaluator;

            } else {

                evaluator = null;
            }

            return new RapidGateControl<NTYPE>(mWaterfall, gate, mTimeoutMs, mTimeoutException,
                                               evaluator);
        }

        //noinspection unchecked
        return (RapidControl<NTYPE>) this;
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
    public <RESULT> RESULT perform(final Action<RESULT, TYPE> action, final Object... args) {

        return getControl().perform(action, args);
    }

    private GateControl<TYPE> getControl() {

        final long timeoutMs = mTimeoutMs;

        final GateControl<TYPE> gateControl =
                mWaterfall.when(mGate).meets(mEvaluator).eventuallyThrow(mTimeoutException);

        if (timeoutMs < 0) {

            gateControl.eventually();

        } else {

            gateControl.afterMax(timeoutMs, TimeUnit.MILLISECONDS);
        }

        return gateControl;
    }

    private static class GateInvocationHandler<TYPE> implements InvocationHandler {

        private final GateControl<TYPE> mControl;

        public GateInvocationHandler(final GateControl<TYPE> control) {

            mControl = control;
        }

        @Override
        public Object invoke(final Object o, final Method method, final Object[] objects) throws
                Throwable {

            return mControl.perform(new Action<Object, TYPE>() {

                @Override
                public Object doOn(final TYPE gate, final Object... args) {

                    try {

                        return method.invoke(gate, objects);

                    } catch (final Throwable t) {

                        throw new UndeclaredThrowableException(t);
                    }
                }
            });
        }
    }
}