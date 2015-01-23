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
package com.bmd.jrt.runner;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Scheduled thread pool executor implementing a dynamic allocation of threads.<br/>
 * When the numbers of running threads reaches the maximum pool size, further command are queued
 * for later execution.
 * <p/>
 * Created by davide on 1/23/15.
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

        final LinkedBlockingQueue<Runnable> internalQueue =
                new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE);
        final RejectingBlockingQueue<Runnable> rejectingQueue =
                new RejectingBlockingQueue<Runnable>(internalQueue);
        mExecutor =
                new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
                                       rejectingQueue,
                                       new QueueRejectedExecutionHandler(internalQueue));
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay,
            final TimeUnit unit) {

        return super.schedule(new CommandRunnable(mExecutor, command), delay, unit);
    }

    @Override
    public void execute(final Runnable command) {

        mExecutor.execute(command);
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

        @Override
        public void run() {

            mExecutor.execute(mCommand);
        }
    }

    /**
     * Handler of rejected execution queueing the rejected command.
     */
    private static class QueueRejectedExecutionHandler implements RejectedExecutionHandler {

        private final BlockingQueue<Runnable> mQueue;

        /**
         * Constructor.
         *
         * @param queue the command queue.
         */
        private QueueRejectedExecutionHandler(@Nonnull final BlockingQueue<Runnable> queue) {

            mQueue = queue;
        }

        @Override
        public void rejectedExecution(final Runnable runnable,
                final ThreadPoolExecutor threadPoolExecutor) {

            mQueue.add(runnable);
        }
    }

    /**
     * Class wrapping a blocking queue instance rejecting addition of new elements.
     *
     * @param <E> the element type.
     */
    private static class RejectingBlockingQueue<E> implements BlockingQueue<E> {

        private final BlockingQueue<E> mQueue;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped queue.
         */
        private RejectingBlockingQueue(@Nonnull final BlockingQueue<E> wrapped) {

            mQueue = wrapped;
        }

        @Override
        public boolean add(final E e) {

            return false;
        }

        @Override
        public boolean offer(@Nonnull final E e) {

            return false;
        }

        @Override
        public void put(final E e) throws InterruptedException {

            throw new InterruptedException();
        }

        @Override
        public boolean offer(final E e, final long timeout, @Nonnull final TimeUnit timeUnit) throws
                InterruptedException {

            return false;
        }

        @Override
        public E take() throws InterruptedException {

            return mQueue.take();
        }

        @Override
        public E poll(final long timeout, @Nonnull final TimeUnit timeUnit) throws
                InterruptedException {

            return mQueue.poll(timeout, timeUnit);
        }

        @Override
        public int remainingCapacity() {

            return 0;
        }

        @Override
        public boolean remove(final Object o) {

            return mQueue.remove(o);
        }

        @Override
        public boolean contains(final Object o) {

            return mQueue.contains(o);
        }

        @Override
        public int drainTo(@Nonnull final Collection<? super E> c) {

            return mQueue.drainTo(c);
        }

        @Override
        public int drainTo(@Nonnull final Collection<? super E> c, final int maxElements) {

            return mQueue.drainTo(c, maxElements);
        }

        @Override
        public E remove() {

            return mQueue.remove();
        }

        @Override
        public E poll() {

            return mQueue.poll();
        }

        @Override
        public E element() {

            return mQueue.element();
        }

        @Override
        public E peek() {

            return mQueue.peek();
        }

        @Override
        public int size() {

            return mQueue.size();
        }

        @Override
        public boolean isEmpty() {

            return mQueue.isEmpty();
        }

        @Nonnull
        @Override
        public Iterator<E> iterator() {

            return mQueue.iterator();
        }

        @Nonnull
        @Override
        public Object[] toArray() {

            return mQueue.toArray();
        }

        @Nonnull
        @Override
        @SuppressWarnings("SuspiciousToArrayCall")
        public <T> T[] toArray(@Nonnull final T[] array) {

            return mQueue.toArray(array);
        }

        @Override
        public boolean containsAll(@Nonnull final Collection<?> c) {

            return mQueue.containsAll(c);
        }

        @Override
        public boolean addAll(@Nonnull final Collection<? extends E> c) {

            return false;
        }

        @Override
        public boolean removeAll(@Nonnull final Collection<?> c) {

            return mQueue.removeAll(c);
        }

        @Override
        public boolean retainAll(@Nonnull final Collection<?> c) {

            return mQueue.retainAll(c);
        }

        @Override
        public void clear() {

            mQueue.clear();
        }
    }
}
