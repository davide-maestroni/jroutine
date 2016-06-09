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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled thread executor wrapping an executor service.
 * <p>
 * Created by davide-maestroni on 05/24/2016.
 */
class ScheduledThreadExecutor extends ScheduledThreadPoolExecutor {

    private final ExecutorService mExecutor;

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    ScheduledThreadExecutor(@NotNull final ExecutorService service) {

        super(1);
        mExecutor = ConstantConditions.notNull("executor service", service);
    }

    @Override
    public int hashCode() {

        return mExecutor.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final ScheduledThreadExecutor that = (ScheduledThreadExecutor) o;
        return mExecutor.equals(that.mExecutor);
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

        private final ExecutorService mService;

        /**
         * Constructor.
         *
         * @param service the executor service.
         * @param command the command to execute.
         */
        private CommandRunnable(@NotNull final ExecutorService service,
                @NotNull final Runnable command) {

            mService = service;
            mCommand = command;
        }

        public void run() {

            mService.execute(mCommand);
        }
    }
}
