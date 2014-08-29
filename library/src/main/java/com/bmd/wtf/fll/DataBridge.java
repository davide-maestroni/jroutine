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
import com.bmd.wtf.flw.DelayInterruptedException;
import com.bmd.wtf.gts.Gate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of a bridge.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <TYPE> the bridge type.
 */
class DataBridge<TYPE> implements Bridge<TYPE> {

    private final Classification<TYPE> mClassification;

    private final Condition mCondition;

    private final Gate<?, ?> mGate;

    private final ReentrantLock mLock;

    private volatile ConditionEvaluator<? super TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param classification the bridge classification.
     * @param gate           the bridge gate.
     * @param lock           the bridge lock.
     * @param condition      the bridge condition.
     * @throws IllegalArgumentException if any of the parameter is null, or the gate is
     *                                  not of the specified classification.
     */
    public DataBridge(final Classification<TYPE> classification, final Gate<?, ?> gate,
            final ReentrantLock lock, final Condition condition) {

        if (classification == null) {

            throw new IllegalArgumentException("the bridge classification cannot be null");
        }

        if (gate == null) {

            throw new IllegalArgumentException("the bridge fall cannot be null");
        }

        if (lock == null) {

            throw new IllegalArgumentException("the bridge lock cannot be null");
        }

        if (condition == null) {

            throw new IllegalArgumentException("the bridge condition cannot be null");
        }

        if (!classification.getRawType().isInstance(gate)) {

            throw new IllegalArgumentException(
                    "the gate is not of type: " + classification.getRawType().getCanonicalName());
        }

        mClassification = classification;
        mGate = gate;
        mLock = lock;
        mCondition = condition;
    }

    @Override
    public Bridge<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(Math.max(0, maxDelay));

        return this;
    }

    @Override
    public Bridge<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public Bridge<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public Classification<TYPE> getClassification() {

        return mClassification;
    }

    @Override
    public Bridge<TYPE> immediately() {

        mTimeoutMs = 0;

        return this;
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        final ReentrantLock lock = mLock;
        lock.lock();

        try {

            final TYPE gate = mClassification.cast(mGate);

            //noinspection unchecked
            waitForCondition(gate);

            return action.doOn(gate, args);

        } finally {

            mCondition.signalAll();

            lock.unlock();
        }
    }

    @Override
    public Bridge<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    private boolean meetsCondition(final ConditionEvaluator<? super TYPE> evaluator,
            final TYPE gate) {

        return (evaluator == null) || evaluator.isSatisfied(gate);
    }

    private void waitForCondition(final TYPE gate) {

        long currentTimeout = mTimeoutMs;

        final RuntimeException timeoutException = mTimeoutException;

        final ConditionEvaluator<? super TYPE> evaluator = mEvaluator;

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