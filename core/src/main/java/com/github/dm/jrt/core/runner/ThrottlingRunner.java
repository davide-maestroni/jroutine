/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.runner;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Runner implementation throttling the number of running executions so to keep it under a specified
 * limit.
 * <p>
 * Created by davide-maestroni on 07/18/2015.
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
    ThrottlingRunner(@NotNull final Runner wrapped, final int maxExecutions) {

        mRunner = ConstantConditions.notNull("wrapped runner", wrapped);
        mMaxRunning =
                ConstantConditions.positive("maximum number of running executions", maxExecutions);
    }

    public void cancel(@NotNull final Execution execution) {

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

    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

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

    @NotNull
    private ThrottlingExecution getThrottlingExecution(@NotNull final Execution execution) {

        final WeakIdentityHashMap<Execution, WeakReference<ThrottlingExecution>> executions =
                mExecutions;
        final WeakReference<ThrottlingExecution> executionReference = executions.get(execution);
        ThrottlingExecution throttlingExecution =
                (executionReference != null) ? executionReference.get() : null;
        if (throttlingExecution == null) {
            throttlingExecution = new ThrottlingExecution(execution);
            executions.put(execution, new WeakReference<ThrottlingExecution>(throttlingExecution));
        }

        return throttlingExecution;
    }

    /**
     * Pending execution implementation.
     */
    private class PendingExecution implements Execution {

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
        private PendingExecution(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

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
        private ThrottlingExecution(@NotNull final Execution execution) {

            mExecution = execution;
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
                    --mRunningCount;
                    if (!queue.isEmpty()) {
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
