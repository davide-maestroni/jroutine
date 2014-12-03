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

import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

/**
 * Utility class for creating and sharing runner instances.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class Runners {

    private static final QueuedRunner sQueuedRunner = new QueuedRunner();

    private static final SequentialRunner sSequentialRunner = new SequentialRunner();

    private static volatile Runner sSharedRunner;

    /**
     * Avoid direct instantiation.
     */
    protected Runners() {

    }

    /**
     * Returns a runner employing an optimum number of threads.
     *
     * @return the runner instance.
     */
    @Nonnull
    public static Runner poolRunner() {

        return poolRunner(getBestPoolSize());
    }

    /**
     * Returns a runner employing the specified number of threads.
     *
     * @param poolSize the thread pool size.
     * @return the runner instance.
     */
    @Nonnull
    public static Runner poolRunner(final int poolSize) {

        return new ThreadPoolRunner(poolSize);
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

        if (sSharedRunner == null) {

            sSharedRunner = poolRunner();
        }

        return sSharedRunner;
    }

    private static int getBestPoolSize() {

        return Runtime.getRuntime().availableProcessors() << 1;
    }
}
