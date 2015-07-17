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

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Scheduled thread pool executor implementing a dynamic allocation of threads.<br/>
 * When the numbers of running threads reaches the maximum pool size, further commands are queued
 * for later execution.
 * <p/>
 * Created by davide-maestroni on 1/23/15.
 */
class DynamicScheduledThreadExecutor extends ScheduledThreadPoolExecutor {

    private final ThreadPoolExecutor mExecutor;

    /**
     * Constructor.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime   when the number of threads is greater than the core, this is the
     *                        maximum time that excess idle threads will wait for new tasks before
     *                        terminating.
     * @param keepAliveUnit   the time unit for the keep alive time.
     */
    DynamicScheduledThreadExecutor(final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, @Nonnull final TimeUnit keepAliveUnit) {

        super(1);
        final RejectingBlockingQueue internalQueue = new RejectingBlockingQueue();
        final QueueRejectedExecutionHandler rejectedExecutionHandler =
                new QueueRejectedExecutionHandler(internalQueue);
        mExecutor =
                new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
                                       internalQueue, rejectedExecutionHandler);
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay,
            final TimeUnit unit) {

        return super.schedule(new CommandRunnable(mExecutor, command), delay, unit);
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
        private CommandRunnable(@Nonnull final ThreadPoolExecutor executor,
                @Nonnull final Runnable command) {

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
        private QueueRejectedExecutionHandler(@Nonnull final RejectingBlockingQueue queue) {

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

        // just don't care...
        private static final long serialVersionUID = -1;

        /**
         * Constructor.
         */
        private RejectingBlockingQueue() {

            super(Integer.MAX_VALUE);
        }

        @Override
        public boolean add(final Runnable runnable) {

            return false;
        }

        @Override
        public boolean addAll(final Collection<? extends Runnable> c) {

            return false;
        }

        @Override
        public int remainingCapacity() {

            return 0;
        }

        @Override
        public void put(final Runnable runnable) throws InterruptedException {

            throw new InterruptedException();
        }

        @Override
        public boolean offer(final Runnable runnable, final long timeout,
                @Nonnull final TimeUnit timeUnit) throws InterruptedException {

            return false;
        }

        @Override
        public boolean offer(@Nonnull final Runnable runnable) {

            return false;
        }

        private boolean push(@Nonnull final Runnable runnable) {

            return super.offer(runnable);
        }
    }
}
