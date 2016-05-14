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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled thread pool executor implementing a dynamic allocation of threads.
 * <br>
 * When the number of running threads reaches the maximum pool size, further commands are queued
 * for later execution.
 * <p>
 * Created by davide-maestroni on 01/23/2015.
 */
class DynamicScheduledThreadExecutor extends ScheduledThreadPoolExecutor {

    private final ThreadPoolExecutor mExecutor;

    /**
     * Constructor.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param queueLimit      the number of scheduled tasks that must be in the queue, before a new
     *                        thread is allocated.
     * @param keepAliveTime   when the number of threads is greater than the core, this is the
     *                        maximum time that excess idle threads will wait for new tasks before
     *                        terminating.
     * @param keepAliveUnit   the time unit for the keep alive time.
     * @throws java.lang.IllegalArgumentException if one of the following holds:<br>
     *                                            {@code corePoolSize < 0}<br>
     *                                            {@code maximumPoolSize <= 0}<br>
     *                                            {@code keepAliveTime < 0}<br>
     *                                            {@code queueLimit <= 0}
     */
    DynamicScheduledThreadExecutor(final int corePoolSize, final int maximumPoolSize,
            final int queueLimit, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {

        super(1);
        final RejectingBlockingQueue internalQueue = new RejectingBlockingQueue(queueLimit);
        final QueueRejectedExecutionHandler rejectedExecutionHandler =
                new QueueRejectedExecutionHandler(internalQueue);
        mExecutor =
                new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
                        internalQueue, rejectedExecutionHandler);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay,
            final TimeUnit unit) {

        return super.schedule(new CommandRunnable(mExecutor, command), delay, unit);
    }

    @NotNull
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
            final TimeUnit unit) {

        return ConstantConditions.unsupported();
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
            final long period, final TimeUnit unit) {

        return ConstantConditions.unsupported();
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
            final long initialDelay, final long delay, final TimeUnit unit) {

        return ConstantConditions.unsupported();
    }

    /**
     * Runnable executing another runnable.
     */
    private static class CommandRunnable implements Runnable {

        private final Runnable mCommand;

        private final ThreadPoolExecutor mExecutor;

        /**
         * Constructor.
         *
         * @param executor the executor instance.
         * @param command  the command to execute.
         */
        private CommandRunnable(@NotNull final ThreadPoolExecutor executor,
                @NotNull final Runnable command) {

            mExecutor = executor;
            mCommand = command;
        }

        public void run() {

            mExecutor.execute(mCommand);
        }
    }

    /**
     * Handler of rejected execution queueing the rejected command.
     */
    private static class QueueRejectedExecutionHandler implements RejectedExecutionHandler {

        private final RejectingBlockingQueue mQueue;

        /**
         * Constructor.
         *
         * @param queue the command queue.
         */
        private QueueRejectedExecutionHandler(@NotNull final RejectingBlockingQueue queue) {

            mQueue = queue;
        }

        public void rejectedExecution(final Runnable runnable,
                final ThreadPoolExecutor threadPoolExecutor) {

            mQueue.push(runnable);
        }
    }

    /**
     * Implementation of a blocking queue rejecting the addition of any new element.
     */
    private static class RejectingBlockingQueue extends LinkedBlockingQueue<Runnable> {

        // Just don't care...
        private static final long serialVersionUID = -1;

        private final int mLimit;

        /**
         * Constructor.
         *
         * @param queueLimit the number of scheduled tasks that must be in the queue, before a new
         *                   thread is allocated.
         */
        private RejectingBlockingQueue(final int queueLimit) {

            mLimit = ConstantConditions.notNegative("queue limit", queueLimit);
        }

        @Override
        public boolean add(final Runnable runnable) {

            return (size() < mLimit) && super.add(runnable);
        }

        @Override
        public boolean addAll(final Collection<? extends Runnable> collection) {

            return (size() + collection.size() <= mLimit) && super.addAll(collection);
        }

        @Override
        public int remainingCapacity() {

            return Math.max(0, mLimit - size());
        }

        @Override
        public void put(final Runnable runnable) throws InterruptedException {

            if (size() >= mLimit) {
                throw new InterruptedException();
            }

            super.put(runnable);
        }

        @Override
        public boolean offer(final Runnable runnable, final long timeout,
                final TimeUnit timeUnit) throws InterruptedException {

            return (size() < mLimit) && super.offer(runnable, timeout, timeUnit);
        }

        @Override
        public boolean offer(@NotNull final Runnable runnable) {

            return (size() < mLimit) && super.offer(runnable);
        }

        private boolean push(@NotNull final Runnable runnable) {

            return super.offer(runnable);
        }
    }
}
