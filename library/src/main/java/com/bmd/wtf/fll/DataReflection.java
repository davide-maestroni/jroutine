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

import com.bmd.wtf.flw.Glass;
import com.bmd.wtf.flw.Reflection;
import com.bmd.wtf.lps.Leap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 6/13/14.
 */
class DataReflection<CLASS> implements Reflection<CLASS> {

    private final Glass<CLASS> mGlass;

    private final GlassLeap<?, ?, ?> mLeap;

    private Evaluator<CLASS> mEvaluator;

    private volatile RuntimeException mTimeoutException;

    // Immediate timeout by default
    private volatile long mTimeoutMs = 0;

    public DataReflection(final GlassLeap<?, ?, ?> leap, final Glass<CLASS> glass) {

        mLeap = leap;
        mGlass = glass;
    }

    @Override
    public Reflection<CLASS> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mTimeoutMs = timeUnit.toMillis(Math.max(0, maxDelay));

        return this;
    }

    @Override
    public Reflection<CLASS> eventually() {

        mTimeoutMs = -1;

        return this;
    }

    @Override
    public Reflection<CLASS> eventuallyThrow(final RuntimeException exception) {

        mTimeoutException = exception;

        return this;
    }

    @Override
    public Reflection<CLASS> matches(final Evaluator<CLASS> evaluator) {

        mEvaluator = evaluator;

        return this;
    }

    @Override
    public CLASS perform() {

        final Glass<CLASS> glass = mGlass;

        final GlassLeap<?, ?, ?> leap = mLeap;

        final GlassInvocationHandler<CLASS> handler =
                new GlassInvocationHandler<CLASS>(leap, mEvaluator, mTimeoutMs, mTimeoutException);

        final Object proxy = Proxy.newProxyInstance(leap.leap.getClass().getClassLoader(),
                                                    new Class[]{glass.getRawType()}, handler);

        return glass.cast(proxy);
    }

    private static class GlassInvocationHandler<CLASS> implements InvocationHandler {

        private final Condition mCondition;

        private final Evaluator<CLASS> mEvaluator;

        private final RuntimeException mException;

        private final Leap<?, ?, ?> mLeap;

        private final ReentrantLock mLock;

        private final long mTimeoutMs;

        public GlassInvocationHandler(final GlassLeap<?, ?, ?> leap,
                final Evaluator<CLASS> evaluator, final long timeoutMs,
                final RuntimeException exception) {

            mLeap = leap.leap;
            mLock = leap.lock;
            mCondition = leap.condition;
            mEvaluator = evaluator;
            mTimeoutMs = timeoutMs;
            mException = exception;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final ReentrantLock lock = mLock;

            lock.lock();

            try {

                //noinspection unchecked
                waitForCondition((CLASS) proxy);

                return method.invoke(mLeap, args);

            } finally {

                lock.unlock();
            }
        }

        private boolean matchesEvaluation(final CLASS glass) {

            final Evaluator<CLASS> evaluator = mEvaluator;

            return (evaluator == null) || evaluator.isSatisfied(glass);
        }

        private void waitForCondition(final CLASS glass) {

            long currentTimeout = mTimeoutMs;

            if ((currentTimeout == 0) || matchesEvaluation(glass)) {

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

                        if (!matchesEvaluation(glass) && (currentTimeout <= 0)) {

                            isTimeout = true;

                            break;
                        }

                    } else {

                        condition.await();
                    }

                } while (!matchesEvaluation(glass));

            } catch (final InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new DelayInterruptedException(e);
            }

            if (isTimeout && (mException != null)) {

                throw mException;
            }
        }
    }
}