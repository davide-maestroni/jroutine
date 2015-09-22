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
package com.github.dm.jrt.runner;

import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class implementing a runner employing an executor service.
 * <p/>
 * Created by davide-maestroni on 10/14/2014.
 */
class ScheduledRunner implements Runner {

    private final WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>> mFutures =
            new WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>>();

    private final ScheduledExecutorService mService;

    private final Map<Thread, Void> mThreads =
            Collections.synchronizedMap(new WeakIdentityHashMap<Thread, Void>());

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    @SuppressWarnings("ConstantConditions")
    ScheduledRunner(@NotNull final ScheduledExecutorService service) {

        if (service == null) {

            throw new NullPointerException("the executor service must not be null");
        }

        mService = service;
    }

    public void cancel(@NotNull final Execution execution) {

        synchronized (mFutures) {

            final WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures =
                    mFutures.remove(execution);

            if (scheduledFutures != null) {

                for (final ScheduledFuture<?> future : scheduledFutures.keySet()) {

                    future.cancel(false);
                }
            }
        }
    }

    public boolean isExecutionThread() {

        return mThreads.containsKey(Thread.currentThread());
    }

    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        final ScheduledFuture<?> future =
                mService.schedule(new ExecutionWrapper(execution, mThreads), delay, timeUnit);

        if (execution.mayBeCanceled()) {

            synchronized (mFutures) {

                final WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>>
                        futures = mFutures;
                WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures = futures.get(execution);

                if (scheduledFutures == null) {

                    scheduledFutures = new WeakHashMap<ScheduledFuture<?>, Void>();
                    futures.put(execution, scheduledFutures);
                }

                scheduledFutures.put(future, null);
            }
        }
    }

    /**
     * Class used to keep track of the threads employed by this runner.
     */
    private static class ExecutionWrapper implements Runnable {

        private final Thread mCurrentThread;

        private final Execution mExecution;

        private final Map<Thread, Void> mThreads;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped execution.
         * @param threads the map of runner threads.
         */
        private ExecutionWrapper(@NotNull final Execution wrapped,
                @NotNull final Map<Thread, Void> threads) {

            mExecution = wrapped;
            mThreads = threads;
            mCurrentThread = Thread.currentThread();
        }

        public void run() {

            final Thread currentThread = Thread.currentThread();

            if (currentThread != mCurrentThread) {

                mThreads.put(currentThread, null);
            }

            mExecution.run();
        }
    }
}
