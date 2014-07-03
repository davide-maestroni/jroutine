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

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.lps.Leap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of a gate.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <TYPE> The gate type.
 */
class DataGate<TYPE> implements Gate<TYPE> {

    private final Classification<TYPE> mClassification;

    private final Condition mCondition;

    private final Leap<?, ?, ?> mLeap;

    private final ReentrantLock mLock;

    private volatile ConditionEvaluator<? super TYPE> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    private volatile long mTimeoutMs;

    /**
     * Constructor.
     *
     * @param leap           The gate leap.
     * @param classification The gate classification.
     */
    public DataGate(final GateLeap<?, ?, ?> leap, final Classification<TYPE> classification) {

        if (classification == null) {

            throw new IllegalArgumentException("the gate classification cannot be null");
        }

        mClassification = classification;
        mLeap = leap.leap;
        mLock = leap.lock;
        mCondition = leap.condition;
    }

    @Override
    public Gate<TYPE> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(Math.max(0, maxDelay));

        return this;
    }

    @Override
    public Gate<TYPE> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public Gate<TYPE> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public Gate<TYPE> immediately() {

        mTimeoutMs = 0;

        return this;
    }

    @Override
    public <RESULT> RESULT perform(final Action<RESULT, ? super TYPE> action,
            final Object... args) {

        final ReentrantLock lock = mLock;

        lock.lock();

        try {

            final TYPE leap = mClassification.cast(mLeap);

            //noinspection unchecked
            waitForCondition(leap);

            return action.doOn(leap, args);

        } finally {

            mCondition.signalAll();

            lock.unlock();
        }
    }

    @Override
    public Gate<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    private boolean meetsCondition(final ConditionEvaluator<? super TYPE> evaluator,
            final TYPE leap) {

        return (evaluator == null) || evaluator.isSatisfied(leap);
    }

    private void waitForCondition(final TYPE leap) {

        long currentTimeout = mTimeoutMs;

        final RuntimeException timeoutException = mTimeoutException;

        final ConditionEvaluator<? super TYPE> evaluator = mEvaluator;

        if ((currentTimeout == 0) || meetsCondition(evaluator, leap)) {

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

                    if (!meetsCondition(evaluator, leap) && (currentTimeout <= 0)) {

                        isTimeout = true;

                        break;
                    }

                } else {

                    condition.await();
                }

            } while (!meetsCondition(evaluator, leap));

        } catch (final InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DelayInterruptedException(e);
        }

        if (isTimeout && (timeoutException != null)) {

            throw timeoutException;
        }
    }
}