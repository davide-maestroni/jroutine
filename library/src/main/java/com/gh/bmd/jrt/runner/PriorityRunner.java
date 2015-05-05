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

import java.util.Comparator;
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
 * age the lower priority execution will have, before getting the precedence over the higher
 * priority one.
 * <p/>
 * Note that the queue is not share between different instances of this class.
 * <p/>
 * Created by davide on 28/04/15.
 */
public class PriorityRunner {

    private static final Comparator<PriorityExecution> PRIORITY_EXECUTION_COMPARATOR =
            new Comparator<PriorityExecution>() {

                public int compare(final PriorityExecution o1, final PriorityExecution o2) {

                    final int thisPriority = o1.mPriority;
                    final long thisAge = o1.mAge;
                    final int thatPriority = o2.mPriority;
                    final long thatAge = o2.mAge;

                    final int compare = compareLong(thatAge + thatPriority, thisAge + thisPriority);
                    return (compare == 0) ? compareLong(thatAge, thisAge) : compare;
                }
            };

    private final AtomicLong mAge = new AtomicLong(Long.MAX_VALUE - Integer.MAX_VALUE);

    private final PriorityBlockingQueue<PriorityExecution> mQueue;

    private final Execution mExecution = new Execution() {

        public void run() {

            final PriorityExecution execution = mQueue.poll();

            if (execution != null) {

                execution.run();
            }
        }
    };

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    PriorityRunner(@Nonnull final Runner wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped runner must not be null");
        }

        mRunner = wrapped;
        mQueue = new PriorityBlockingQueue<PriorityExecution>(10, PRIORITY_EXECUTION_COMPARATOR);
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

        return new EnqueuingRunner(priority);
    }

    /**
     * Interface exposing constants which can be used as a common set of priorities.<br/>
     * Note that, since the priority value can be any in an integer range, it is always possible to
     * customize the values so to create a personalized set.
     */
    public interface AgingPriority {

        /**
         * High priority.
         */
        int HIGH_PRIORITY = 10;

        /**
         * Highest priority.
         */
        int HIGHEST_PRIORITY = HIGH_PRIORITY << 1;

        /**
         * Low priority.
         */
        int LOWEST_PRIORITY = -HIGHEST_PRIORITY;

        /**
         * Lowest priority.
         */
        int LOW_PRIORITY = -HIGH_PRIORITY;

        /**
         * Normal priority.
         */
        int NORMAL_PRIORITY = 0;
    }

    /**
     * Interface exposing constants which can be used as a set of priorities ignoring the aging of
     * executions.<br/>
     * Note that, since the priority value can be any in an integer range, it is always possible to
     * customize the values so to create a personalized set.
     */
    public interface NotAgingPriority {

        /**
         * Highest priority.
         */
        int HIGHEST_PRIORITY = Integer.MAX_VALUE;

        /**
         * High priority.
         */
        int HIGH_PRIORITY = HIGHEST_PRIORITY >> 1;

        /**
         * Low priority.
         */
        int LOWEST_PRIORITY = -HIGHEST_PRIORITY;

        /**
         * Lowest priority.
         */
        int LOW_PRIORITY = -HIGH_PRIORITY;

        /**
         * Normal priority.
         */
        int NORMAL_PRIORITY = 0;
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

        public void run() {

            mExecution.run();
        }
    }

    /**
     * Enqueuing runner implementation.
     */
    private class EnqueuingRunner implements Runner {

        private final int mPriority;

        /**
         * Constructor.
         *
         * @param priority the execution priority.
         */
        private EnqueuingRunner(final int priority) {

            mPriority = priority;
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            final PriorityExecution priorityExecution =
                    new PriorityExecution(execution, mPriority, mAge.getAndDecrement());

            if (delay == 0) {

                mQueue.put(priorityExecution);
                mRunner.run(mExecution, 0, timeUnit);

            } else {

                mRunner.run(new DelayedExecution(mQueue, priorityExecution), delay, timeUnit);
            }
        }
    }
}
