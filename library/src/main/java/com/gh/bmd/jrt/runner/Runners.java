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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Utility class for creating and sharing runner instances.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class Runners {

    private static final Object sMutex = new Object();

    private static final QueuedRunner sQueuedRunner = new QueuedRunner();

    private static final SequentialRunner sSequentialRunner = new SequentialRunner();

    private static Runner sSharedRunner;

    /**
     * Avoid direct instantiation.
     */
    protected Runners() {

    }

    /**
     * Returns a runner employing a dynamic pool of threads.<br/>
     * The number of threads may increase when needed from the core to the maximum pool size. The
     * number of threads exceeding the core size are kept alive when idle for the specified time.
     * If they stay idle for more time they will be destroyed.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime   when the number of threads is greater than the core one, this is the
     *                        maximum time that excess idle threads will wait for new tasks before
     *                        terminating.
     * @param keepAliveUnit   the time unit for the keep alive time.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner dynamicPoolRunner(final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, @Nonnull final TimeUnit keepAliveUnit) {

        return scheduledRunner(
                new DynamicScheduledThreadExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                                                   keepAliveUnit));
    }

    /**
     * Returns a runner employing the specified number of threads.
     *
     * @param poolSize the thread pool size.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner poolRunner(final int poolSize) {

        return scheduledRunner(Executors.newScheduledThreadPool(poolSize));
    }

    /**
     * Returns a runner employing an optimum number of threads.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner poolRunner() {

        return poolRunner(Runtime.getRuntime().availableProcessors() << 1);
    }

    /**
     * Returns a runner providing ordering of executions based on priority.
     *
     * @param wrapped the wrapped runner instance.
     * @return the runner instance.
     */
    @Nonnull
    public static PriorityRunner priorityRunner(@Nonnull final Runner wrapped) {

        return new PriorityRunner(wrapped);
    }

    /**
     * Returns the shared instance of a queued synchronous runner.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner queuedRunner() {

        return sQueuedRunner;
    }

    /**
     * Returns a runner employing the specified executor service.
     *
     * @param service the executor service.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner scheduledRunner(@Nonnull final ScheduledExecutorService service) {

        return new ScheduledRunner(service);
    }

    /**
     * Returns the shared instance of a sequential synchronous runner.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner sequentialRunner() {

        return sSequentialRunner;
    }

    /**
     * Returns the shared instance of a thread pool asynchronous runner.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner sharedRunner() {

        synchronized (sMutex) {

            if (sSharedRunner == null) {

                final int processors = Runtime.getRuntime().availableProcessors();
                sSharedRunner = dynamicPoolRunner((processors <= 2) ? processors : processors - 1,
                                                  (processors << 3) - 1, 3L, TimeUnit.SECONDS);
            }

            return sSharedRunner;
        }
    }
}
