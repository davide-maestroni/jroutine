/*
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
package com.github.dm.jrt.runner;

import com.github.dm.jrt.util.WeakIdentityHashMap;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Runner implementation throttling the number of running executions so to keep it under a specified
 * limit.
 * <p/>
 * Created by davide-maestroni on 18/07/15.
 */
class ThrottlingRunner implements Runner {

    private final WeakIdentityHashMap<Execution, WeakReference<ThrottlingExecution>> mExecutions =
            new WeakIdentityHashMap<Execution, WeakReference<ThrottlingExecution>>();

    private final int mMaxRunning;

    private final Object mMutex = new Object();

    private final LinkedList<PendingExecution> mQueue = new LinkedList<PendingExecution>();

    private final Runner mRunner;

    private int mRunningCount;

    /**
     * Constructor.
     *
     * @param wrapped       the wrapped instance.
     * @param maxExecutions the maximum number of running executions.
     * @throws java.lang.IllegalArgumentException if the specified max number is less than 1.
     */
    @SuppressWarnings("ConstantConditions")
    ThrottlingRunner(@Nonnull final Runner wrapped, final int maxExecutions) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped runner must not be null");
        }

        if (maxExecutions < 1) {

            throw new IllegalArgumentException(
                    "the maximum number of running executions must be at least 1, while it was: "
                            + maxExecutions);
        }

        mRunner = wrapped;
        mMaxRunning = maxExecutions;
    }

    public void cancel(@Nonnull final Execution execution) {

        ThrottlingExecution throttlingExecution = null;

        synchronized (mMutex) {

            final Iterator<PendingExecution> iterator = mQueue.iterator();

            while (iterator.hasNext()) {

                final PendingExecution pendingExecution = iterator.next();

                if (pendingExecution.mExecution == execution) {

                    iterator.remove();
                }
            }

            final WeakReference<ThrottlingExecution> executionReference =
                    mExecutions.get(execution);

            if (executionReference != null) {

                throttlingExecution = executionReference.get();
            }
        }

        if (throttlingExecution != null) {

            mRunner.cancel(throttlingExecution);
        }
    }

    public boolean isExecutionThread() {

        return mRunner.isExecutionThread();
    }

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        ThrottlingExecution throttlingExecution = null;

        synchronized (mMutex) {

            final LinkedList<PendingExecution> queue = mQueue;

            if ((mRunningCount + queue.size()) >= mMaxRunning) {

                queue.add(new PendingExecution(execution, delay, timeUnit));

            } else {

                throttlingExecution = getThrottlingExecution(execution);
            }
        }

        if (throttlingExecution != null) {

            mRunner.run(throttlingExecution, delay, timeUnit);
        }
    }

    @Nonnull
    private ThrottlingExecution getThrottlingExecution(@Nonnull final Execution execution) {

        final WeakReference<ThrottlingExecution> executionReference = mExecutions.get(execution);
        ThrottlingExecution throttlingExecution =
                (executionReference != null) ? executionReference.get() : null;

        if (throttlingExecution == null) {

            throttlingExecution = new ThrottlingExecution(execution);

            if (execution.mayBeCanceled()) {

                mExecutions.put(execution,
                                new WeakReference<ThrottlingExecution>(throttlingExecution));
            }
        }

        return throttlingExecution;
    }

    /**
     * Pending execution implementation.
     */
    private class PendingExecution extends TemplateExecution {

        private final long mDelay;

        private final Execution mExecution;

        private final TimeUnit mTimeUnit;

        /**
         * Constructor.
         *
         * @param execution the execution.
         * @param delay     the execution delay.
         * @param timeUnit  the delay time unit.
         */
        private PendingExecution(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mExecution = execution;
            mDelay = delay;
            mTimeUnit = timeUnit;
        }

        public void run() {

            final ThrottlingExecution throttlingExecution;

            synchronized (mMutex) {

                throttlingExecution = getThrottlingExecution(mExecution);
            }

            mRunner.run(throttlingExecution, mDelay, mTimeUnit);
        }
    }

    /**
     * Execution used to dequeue and run pending executions, when the maximum running count allows
     * it.
     */
    private class ThrottlingExecution implements Execution {

        private final Execution mExecution;

        /**
         * Constructor.
         *
         * @param execution the execution.
         */
        private ThrottlingExecution(@Nonnull final Execution execution) {

            mExecution = execution;
        }

        public boolean mayBeCanceled() {

            return mExecution.mayBeCanceled();
        }

        public void run() {

            final int maxRunning = mMaxRunning;
            final Execution execution = mExecution;
            final LinkedList<PendingExecution> queue = mQueue;

            synchronized (mMutex) {

                if (mRunningCount >= maxRunning) {

                    queue.addFirst(new PendingExecution(execution, 0, TimeUnit.MILLISECONDS));
                    return;
                }

                ++mRunningCount;
            }

            try {

                execution.run();

            } finally {

                PendingExecution pendingExecution = null;

                synchronized (mMutex) {

                    if ((--mRunningCount < maxRunning) && !queue.isEmpty()) {

                        pendingExecution = queue.removeFirst();
                    }
                }

                if (pendingExecution != null) {

                    pendingExecution.run();
                }
            }
        }
    }
}
