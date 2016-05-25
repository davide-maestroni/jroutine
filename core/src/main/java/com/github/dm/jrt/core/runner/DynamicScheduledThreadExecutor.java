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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.SynchronousQueue;
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
class DynamicScheduledThreadExecutor extends ScheduledThreadExecutor {

    /**
     * Constructor.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime   when the number of threads is greater than the core, this is the
     *                        maximum time that excess idle threads will wait for new tasks before
     *                        terminating.
     * @param keepAliveUnit   the time unit for the keep alive time.
     * @throws java.lang.IllegalArgumentException if one of the following holds:<ul>
     *                                            <li>{@code corePoolSize < 0}</li>
     *                                            <li>{@code maximumPoolSize <= 0}</li>
     *                                            <li>{@code keepAliveTime < 0}</li></ul>
     */
    DynamicScheduledThreadExecutor(final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {

        super(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
                new NonRejectingQueue()));
    }

    /**
     * Implementation of a synchronous queue, which avoids rejection of tasks by forcedly waiting
     * for available threads.
     */
    private static class NonRejectingQueue extends SynchronousQueue<Runnable> {

        // Just don't care...
        private static final long serialVersionUID = -1;

        @Override
        public boolean offer(final Runnable runnable) {

            try {
                put(runnable);

            } catch (final InterruptedException ignored) {
                return false;
            }

            return true;
        }
    }
}
