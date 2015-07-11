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
package com.gh.bmd.jrt.runner;

import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

/**
 * Class providing ordering of executions based on priority.<br/>
 * Each class instance wraps a supporting runner and then provides different runner instances, each
 * one enqueuing executions with a specific priority.
 * <p/>
 * Each enqueued execution will age each time an higher priority one takes the precedence, so that
 * older executions slowly increases their priority. Such mechanism has been implemented to avoid
 * starvation of low priority executions. Hence, when assigning priority to different runners, it is
 * important to keep in mind that the difference between two priorities corresponds to the maximum
 * age the lower priority execution will have, before getting precedence over the higher priority
 * one.
 * <p/>
 * Note that the queue is not shared between different instances of this class.
 * <p/>
 * Created by davide-maestroni on 28/04/15.
 */
public class PriorityRunner {

    private static final PriorityExecutionComparator PRIORITY_EXECUTION_COMPARATOR =
            new PriorityExecutionComparator();

    private static final WeakIdentityHashMap<Runner, PriorityRunner> sRunnerMap =
            new WeakIdentityHashMap<Runner, PriorityRunner>();

    private final AtomicLong mAge = new AtomicLong(Long.MAX_VALUE - Integer.MAX_VALUE);

    private final Map<PriorityExecution, DelayedExecution> mDelayedExecutions =
            Collections.synchronizedMap(
                    new WeakIdentityHashMap<PriorityExecution, DelayedExecution>());

    private final Map<Execution, PriorityExecution> mExecutions =
            Collections.synchronizedMap(new WeakIdentityHashMap<Execution, PriorityExecution>());

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

    private final WeakHashMap<QueuingRunner, Void> mRunnerMap =
            new WeakHashMap<QueuingRunner, Void>();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    private PriorityRunner(@Nonnull final Runner wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped runner must not be null");
        }

        mRunner = wrapped;
        mQueue = new PriorityBlockingQueue<PriorityExecution>(10, PRIORITY_EXECUTION_COMPARATOR);
    }

    @Nonnull
    static PriorityRunner getInstance(@Nonnull final Runner wrapped) {

        if (wrapped instanceof QueuingRunner) {

            return ((QueuingRunner) wrapped).enclosingRunner();
        }

        synchronized (sRunnerMap) {

            final WeakIdentityHashMap<Runner, PriorityRunner> runnerMap = sRunnerMap;
            PriorityRunner runner = runnerMap.get(wrapped);

            if (runner == null) {

                runner = new PriorityRunner(wrapped);
                runnerMap.put(wrapped, runner);
            }

            return runner;
        }
    }

    private static int compareLong(long x, long y) {

        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    /**
     * Returns a runner enqueuing executions with the specified priority.
     *
     * @param priority the execution priority.
     * @return the runner instance.
     */
    @Nonnull
    public Runner getRunner(final int priority) {

        synchronized (mRunnerMap) {

            final WeakHashMap<QueuingRunner, Void> runnerMap = mRunnerMap;

            for (final QueuingRunner runner : runnerMap.keySet()) {

                if (runner.mPriority == priority) {

                    return runner;
                }
            }

            final QueuingRunner runner = new QueuingRunner(priority);
            runnerMap.put(runner, null);
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
        private DelayedExecution(@Nonnull final PriorityBlockingQueue<PriorityExecution> queue,
                @Nonnull final PriorityExecution execution) {

            mQueue = queue;
            mExecution = execution;
        }

        public boolean isCancelable() {

            return mExecution.isCancelable();
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
        private PriorityExecution(@Nonnull final Execution execution, final int priority,
                final long age) {

            mExecution = execution;
            mPriority = priority;
            mAge = age;
        }

        public boolean isCancelable() {

            return mExecution.isCancelable();
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

        // just don't care...
        private static final long serialVersionUID = -1;

        public int compare(final PriorityExecution o1, final PriorityExecution o2) {

            final int thisPriority = o1.mPriority;
            final long thisAge = o1.mAge;
            final int thatPriority = o2.mPriority;
            final long thatAge = o2.mAge;

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

        public void cancel(@Nonnull final Execution execution) {

            final PriorityExecution priorityExecution = mExecutions.remove(execution);

            if ((priorityExecution != null) && !mQueue.remove(priorityExecution)) {

                mRunner.cancel(mDelayedExecutions.remove(priorityExecution));
            }
        }

        public boolean isExecutionThread() {

            return mRunner.isExecutionThread();
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            final boolean isCancelable = execution.isCancelable();
            final PriorityExecution priorityExecution =
                    new PriorityExecution(execution, mPriority, mAge.getAndDecrement());

            if (isCancelable) {

                mExecutions.put(execution, priorityExecution);
            }

            if (delay == 0) {

                mQueue.put(priorityExecution);
                mRunner.run(mExecution, 0, timeUnit);

            } else {

                final DelayedExecution delayedExecution =
                        new DelayedExecution(mQueue, priorityExecution);

                if (isCancelable) {

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
