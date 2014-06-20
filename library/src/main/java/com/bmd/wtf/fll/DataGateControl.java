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

import com.bmd.wtf.flg.GateControl;
import com.bmd.wtf.lps.Leap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 6/13/14.
 */
class DataGateControl<TYPE> implements GateControl<TYPE> {

    private final Condition mCondition;

    private final Classification<TYPE> mGate;

    private final Leap<?, ?, ?> mLeap;

    private final ReentrantLock mLock;

    private volatile ConditionEvaluator<TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    public DataGateControl(final GateLeap<?, ?, ?> leap, final Classification<TYPE> gate) {

        mGate = gate;
        mLeap = leap.leap;
        mLock = leap.lock;
        mCondition = leap.condition;

        reset();
    }

    @Override
    public GateControl<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(Math.max(0, maxDelay));

        return this;
    }

    @Override
    public GateControl<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public GateControl<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public GateControl<TYPE> meets(final ConditionEvaluator<TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, TYPE> action, final Object... args) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            final TYPE gate = mGate.cast(mLeap);

            //noinspection unchecked
            waitForCondition(gate);

            return action.doOn(gate, args);

        } finally {

            lock.unlock();
        }
    }

    private boolean meetsCondition(final ConditionEvaluator<TYPE> evaluator, final TYPE gate) {

        return (evaluator == null) || evaluator.isSatisfied(gate);
    }

    private void reset() {

        mTimeoutMs = 0;
        mTimeoutException = null;
        mEvaluator = null;
    }

    private void waitForCondition(final TYPE gate) {

        long currentTimeout = mTimeoutMs;

        final RuntimeException timeoutException = mTimeoutException;

        final ConditionEvaluator<TYPE> evaluator = mEvaluator;

        reset();

        if ((currentTimeout == 0) || meetsCondition(evaluator, gate)) {

            return;
        }

        boolean isTimeout = false;

        try {

            final Condition condition = mCondition;

            final long startTime = System.currentTimeMillis();

            final long endTime = startTime + currentTimeout;

            do {

                if (currentTimeout >= 0) {

                    condition.await(currentTimeout, TimeUnit.MILLISECONDS);

                    currentTimeout = endTime - System.currentTimeMillis();

                    if (!meetsCondition(evaluator, gate) && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    condition.await();
                }

            } while (!meetsCondition(evaluator, gate));

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }

        if (isTimeout && (timeoutException != null)) {

            throw timeoutException;
        }
    }
}