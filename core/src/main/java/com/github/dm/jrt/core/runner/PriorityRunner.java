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

import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class providing ordering of executions based on priority.<br/>
 * Each class instance wraps a supporting runner and then provides different runner instances, each
 * one enqueuing executions with a specific priority.
 * <p/>
 * Each enqueued execution will age every time an higher priority one takes the precedence, so that
 * older executions slowly increases their priority. Such mechanism has been implemented to avoid
 * starvation of low priority executions. Hence, when assigning priority to different runners, it is
 * important to keep in mind that the difference between two priorities corresponds to the maximum
 * age the lower priority execution will have, before getting precedence over the higher priority
 * one.
 * <p/>
 * Note that the queue is not shared between different instances of this class.
 * <p/>
 * Created by davide-maestroni on 04/28/2015.
 */
public class PriorityRunner {

    private static final PriorityExecutionComparator PRIORITY_EXECUTION_COMPARATOR =
            new PriorityExecutionComparator();

    private static final WeakIdentityHashMap<Runner, PriorityRunner> sRunners =
            new WeakIdentityHashMap<Runner, PriorityRunner>();

    private final AtomicLong mAge = new AtomicLong(Long.MAX_VALUE - Integer.MAX_VALUE);

    private final Map<PriorityExecution, DelayedExecution> mDelayedExecutions =
            Collections.synchronizedMap(
                    new WeakIdentityHashMap<PriorityExecution, DelayedExecution>());

    private final WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>> mExecutions =
            new WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>>();

    private final PriorityBlockingQueue<PriorityExecution> mQueue;

    private final TemplateExecution mExecution = new TemplateExecution() {

        public void run() {

            final PriorityExecution execution = mQueue.poll();
            if (execution != null) {
                execution.run();
            }
        }
    };

    private final Runner mRunner;

    private final WeakHashMap<QueuingRunner, Void> mRunners =
            new WeakHashMap<QueuingRunner, Void>();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    private PriorityRunner(@NotNull final Runner wrapped) {

        if (wrapped == null) {
            throw new NullPointerException("the wrapped runner must not be null");
        }

        mRunner = wrapped;
        mQueue = new PriorityBlockingQueue<PriorityExecution>(10, PRIORITY_EXECUTION_COMPARATOR);
    }

    /**
     * Returns the priority runner wrapping the specified one.
     * <p/>
     * Note that wrapping a synchronous runner may lead to unpredictable results.
     *
     * @param wrapped the wrapped instance.
     * @return the priority runner.
     */
    @NotNull
    static PriorityRunner getInstance(@NotNull final Runner wrapped) {

        if (wrapped instanceof QueuingRunner) {
            return ((QueuingRunner) wrapped).enclosingRunner();
        }

        synchronized (sRunners) {
            final WeakIdentityHashMap<Runner, PriorityRunner> runners = sRunners;
            PriorityRunner runner = runners.get(wrapped);
            if (runner == null) {
                runner = new PriorityRunner(wrapped);
                runners.put(wrapped, runner);
            }

            return runner;
        }
    }

    private static int compareLong(final long l1, final long l2) {

        return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
    }

    /**
     * Returns a runner enqueuing executions with the specified priority.
     *
     * @param priority the execution priority.
     * @return the runner instance.
     */
    @NotNull
    public Runner getRunner(final int priority) {

        synchronized (mRunners) {
            final WeakHashMap<QueuingRunner, Void> runners = mRunners;
            for (final QueuingRunner runner : runners.keySet()) {
                if (runner.mPriority == priority) {
                    return runner;
                }
            }

            final QueuingRunner runner = new QueuingRunner(priority);
            runners.put(runner, null);
            return runner;
        }
    }

    /**
     * Execution implementation delaying the enqueuing of the priority execution.
     */
    private static class DelayedExecution implements Execution {

        private final PriorityExecution mExecution;

        private final PriorityBlockingQueue<PriorityExecution> mQueue;

        /**
         * Constructor.
         *
         * @param queue     the queue.
         * @param execution the priority execution.
         */
        private DelayedExecution(@NotNull final PriorityBlockingQueue<PriorityExecution> queue,
                @NotNull final PriorityExecution execution) {

            mQueue = queue;
            mExecution = execution;
        }

        public boolean canBeCancelled() {

            return mExecution.canBeCancelled();
        }

        public void run() {

            final PriorityBlockingQueue<PriorityExecution> queue = mQueue;
            queue.put(mExecution);
            final PriorityExecution execution = queue.poll();
            if (execution != null) {
                execution.run();
            }
        }
    }

    /**
     * Execution implementation providing a comparison based on priority and the wrapped execution
     * age.
     */
    private static class PriorityExecution implements Execution {

        private final long mAge;

        private final Execution mExecution;

        private final int mPriority;

        /**
         * Constructor.
         *
         * @param execution the wrapped execution.
         * @param priority  the execution priority.
         * @param age       the execution age.
         */
        private PriorityExecution(@NotNull final Execution execution, final int priority,
                final long age) {

            mExecution = execution;
            mPriority = priority;
            mAge = age;
        }

        public boolean canBeCancelled() {

            return mExecution.canBeCancelled();
        }

        public void run() {

            mExecution.run();
        }
    }

    /**
     * Comparator of priority execution instances.
     */
    private static class PriorityExecutionComparator
            implements Comparator<PriorityExecution>, Serializable {

        // Just don't care...
        private static final long serialVersionUID = -1;

        public int compare(final PriorityExecution e1, final PriorityExecution e2) {

            final int thisPriority = e1.mPriority;
            final long thisAge = e1.mAge;
            final int thatPriority = e2.mPriority;
            final long thatAge = e2.mAge;
            final int compare = compareLong(thatAge + thatPriority, thisAge + thisPriority);
            return (compare == 0) ? compareLong(thatAge, thisAge) : compare;
        }
    }

    /**
     * Enqueuing runner implementation.
     */
    private class QueuingRunner implements Runner {

        private final int mPriority;

        /**
         * Constructor.
         *
         * @param priority the execution priority.
         */
        private QueuingRunner(final int priority) {

            mPriority = priority;
        }

        public void cancel(@NotNull final Execution execution) {

            synchronized (mExecutions) {
                final WeakHashMap<PriorityExecution, Void> priorityExecutions =
                        mExecutions.remove(execution);
                if (priorityExecutions != null) {
                    final Runner runner = mRunner;
                    final PriorityBlockingQueue<PriorityExecution> queue = mQueue;
                    final Map<PriorityExecution, DelayedExecution> delayedExecutions =
                            mDelayedExecutions;
                    for (final PriorityExecution priorityExecution : priorityExecutions.keySet()) {
                        if (!queue.remove(priorityExecution)) {
                            runner.cancel(delayedExecutions.remove(priorityExecution));
                        }
                    }
                }
            }
        }

        public boolean isExecutionThread() {

            return mRunner.isExecutionThread();
        }

        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

            final boolean canBeCancelled = execution.canBeCancelled();
            final PriorityExecution priorityExecution =
                    new PriorityExecution(execution, mPriority, mAge.getAndDecrement());
            if (canBeCancelled) {
                synchronized (mExecutions) {
                    final WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>>
                            executions = mExecutions;
                    WeakHashMap<PriorityExecution, Void> priorityExecutions =
                            executions.get(execution);
                    if (priorityExecutions == null) {
                        priorityExecutions = new WeakHashMap<PriorityExecution, Void>();
                        executions.put(execution, priorityExecutions);
                    }

                    priorityExecutions.put(priorityExecution, null);
                }
            }

            if (delay == 0) {
                mQueue.put(priorityExecution);
                mRunner.run(mExecution, 0, timeUnit);

            } else {
                final DelayedExecution delayedExecution =
                        new DelayedExecution(mQueue, priorityExecution);
                if (canBeCancelled) {
                    mDelayedExecutions.put(priorityExecution, delayedExecution);
                }

                mRunner.run(delayedExecution, delay, timeUnit);
            }
        }

        private PriorityRunner enclosingRunner() {

            return PriorityRunner.this;
        }
    }
}
