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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class providing ordering of executions based on priority.
 * <br>
 * Each class instance wraps a supporting runner and then provides different runner instances, each
 * one enqueuing executions with a specific priority.
 * <p>
 * Each enqueued execution will age every time an higher priority one takes the precedence, so that
 * older executions slowly increases their priority. Such mechanism has been implemented to avoid
 * starvation of low priority executions. Hence, when assigning priority to different runners, it is
 * important to keep in mind that the difference between two priorities corresponds to the maximum
 * age the lower priority execution will have, before getting precedence over the higher priority
 * one.
 * <p>
 * Note that applying a priority to a synchronous runner will have no effect.
 * <p>
 * Created by davide-maestroni on 04/28/2015.
 */
public class PriorityRunner {

    private static final PriorityExecutionComparator PRIORITY_EXECUTION_COMPARATOR =
            new PriorityExecutionComparator();

    private static final WeakIdentityHashMap<Runner, WeakReference<PriorityRunner>> sRunners =
            new WeakIdentityHashMap<Runner, WeakReference<PriorityRunner>>();

    private final AtomicLong mAge = new AtomicLong(Long.MAX_VALUE - Integer.MAX_VALUE);

    private final Map<PriorityExecution, DelayedExecution> mDelayedExecutions =
            Collections.synchronizedMap(new HashMap<PriorityExecution, DelayedExecution>());

    private final WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>> mExecutions =
            new WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>>();

    private final Map<PriorityExecution, ImmediateExecution> mImmediateExecutions =
            Collections.synchronizedMap(new HashMap<PriorityExecution, ImmediateExecution>());

    private final PriorityBlockingQueue<PriorityExecution> mQueue;

    private final Runner mRunner;

    private final WeakHashMap<QueuingRunner, Void> mRunners =
            new WeakHashMap<QueuingRunner, Void>();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    private PriorityRunner(@NotNull final Runner wrapped) {
        mRunner = ConstantConditions.notNull("wrapped runner", wrapped);
        mQueue = new PriorityBlockingQueue<PriorityExecution>(10, PRIORITY_EXECUTION_COMPARATOR);
    }

    /**
     * Returns the priority runner wrapping the specified one.
     * <p>
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
            final WeakIdentityHashMap<Runner, WeakReference<PriorityRunner>> runners = sRunners;
            final WeakReference<PriorityRunner> reference = runners.get(wrapped);
            PriorityRunner runner = (reference != null) ? reference.get() : null;
            if (runner == null) {
                runner = new PriorityRunner(wrapped);
                runners.put(wrapped, new WeakReference<PriorityRunner>(runner));
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
     * Execution implementation delaying the enqueuing of the priority execution.
     */
    private class DelayedExecution implements Execution {

        private final PriorityExecution mExecution;

        /**
         * Constructor.
         *
         * @param execution the priority execution.
         */
        private DelayedExecution(@NotNull final PriorityExecution execution) {
            mExecution = execution;
        }

        public void run() {
            final PriorityExecution execution = mExecution;
            mDelayedExecutions.remove(execution);
            final PriorityBlockingQueue<PriorityExecution> queue = mQueue;
            queue.put(execution);
            final PriorityExecution priorityExecution = queue.poll();
            if (priorityExecution != null) {
                priorityExecution.run();
            }
        }
    }

    /**
     * Execution implementation handling the immediate enqueuing of the priority execution.
     */
    private class ImmediateExecution implements Execution {

        private final PriorityExecution mExecution;

        /**
         * Constructor.
         *
         * @param execution the priority execution.
         */
        private ImmediateExecution(@NotNull final PriorityExecution execution) {
            mExecution = execution;
        }

        public void run() {
            final PriorityExecution execution = mExecution;
            mImmediateExecutions.remove(execution);
            final PriorityExecution priorityExecution = mQueue.poll();
            if (priorityExecution != null) {
                priorityExecution.run();
            }
        }
    }

    /**
     * Execution implementation providing a comparison based on priority and the wrapped execution
     * age.
     */
    private class PriorityExecution implements Execution {

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

        public void run() {
            final Execution execution = mExecution;
            synchronized (mExecutions) {
                final WeakIdentityHashMap<Execution, WeakHashMap<PriorityExecution, Void>>
                        executions = mExecutions;
                final WeakHashMap<PriorityExecution, Void> priorityExecutions =
                        executions.get(execution);
                if (priorityExecutions != null) {
                    priorityExecutions.remove(this);
                    if (priorityExecutions.isEmpty()) {
                        executions.remove(execution);
                    }
                }
            }

            execution.run();
        }
    }

    /**
     * Enqueuing runner implementation.
     */
    private class QueuingRunner extends RunnerDecorator {

        private final int mPriority;

        /**
         * Constructor.
         *
         * @param priority the execution priority.
         */
        private QueuingRunner(final int priority) {
            super(mRunner);
            mPriority = priority;
        }

        @Override
        public void cancel(@NotNull final Execution execution) {
            final WeakHashMap<PriorityExecution, Void> priorityExecutions;
            synchronized (mExecutions) {
                priorityExecutions = mExecutions.remove(execution);
            }

            if (priorityExecutions != null) {
                final PriorityBlockingQueue<PriorityExecution> queue = mQueue;
                final Map<PriorityExecution, ImmediateExecution> immediateExecutions =
                        mImmediateExecutions;
                final Map<PriorityExecution, DelayedExecution> delayedExecutions =
                        mDelayedExecutions;
                for (final PriorityExecution priorityExecution : priorityExecutions.keySet()) {
                    if (queue.remove(priorityExecution)) {
                        final ImmediateExecution immediateExecution =
                                immediateExecutions.remove(priorityExecution);
                        if (immediateExecution != null) {
                            super.cancel(immediateExecution);
                        }

                    } else {
                        final DelayedExecution delayedExecution =
                                delayedExecutions.remove(priorityExecution);
                        if (delayedExecution != null) {
                            super.cancel(delayedExecution);
                        }
                    }
                }
            }
        }

        @Override
        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {
            if (!super.isManagedThread()) {
                super.run(execution, delay, timeUnit);

            } else {
                final PriorityExecution priorityExecution =
                        new PriorityExecution(execution, mPriority, mAge.getAndDecrement());
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

                if (delay == 0) {
                    final ImmediateExecution immediateExecution =
                            new ImmediateExecution(priorityExecution);
                    mImmediateExecutions.put(priorityExecution, immediateExecution);
                    mQueue.put(priorityExecution);
                    super.run(immediateExecution, 0, timeUnit);

                } else {
                    final DelayedExecution delayedExecution =
                            new DelayedExecution(priorityExecution);
                    mDelayedExecutions.put(priorityExecution, delayedExecution);
                    super.run(delayedExecution, delay, timeUnit);
                }
            }
        }

        @NotNull
        private PriorityRunner enclosingRunner() {
            return PriorityRunner.this;
        }
    }
}
