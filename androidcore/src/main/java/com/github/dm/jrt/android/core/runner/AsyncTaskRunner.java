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

package com.github.dm.jrt.android.core.runner;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;

import com.github.dm.jrt.core.runner.AsyncRunner;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a runner employing {@link android.os.AsyncTask} instances to execute the
 * routine invocations.
 * <p>
 * Created by davide-maestroni on 09/28/2014.
 */
class AsyncTaskRunner extends AsyncRunner {

    private static final Void[] NO_PARAMS = new Void[0];

    private static final MainRunner sMainRunner = new MainRunner();

    private final Executor mExecutor;

    private final ThreadLocal<Boolean> mIsManaged = new ThreadLocal<Boolean>();

    private final WeakIdentityHashMap<Execution, WeakHashMap<ExecutionTask, Void>> mTasks =
            new WeakIdentityHashMap<Execution, WeakHashMap<ExecutionTask, Void>>();

    /**
     * Constructor.
     * <p>
     * Note that, in case a null executor is passed as parameter, the default one will be used.
     *
     * @param executor the executor.
     */
    AsyncTaskRunner(@Nullable final Executor executor) {
        mExecutor = executor;
    }

    @Override
    public void cancel(@NotNull final Execution execution) {
        synchronized (mTasks) {
            final WeakHashMap<ExecutionTask, Void> executionTasks = mTasks.remove(execution);
            if (executionTasks != null) {
                for (final ExecutionTask task : executionTasks.keySet()) {
                    sMainRunner.cancel(task);
                    task.cancel(false);
                }
            }
        }
    }

    @Override
    public boolean isManagedThread(@NotNull final Thread thread) {
        final Boolean isManaged = mIsManaged.get();
        return (isManaged != null) && isManaged;
    }

    @Override
    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {
        final ExecutionTask task = new ExecutionTask(execution, mExecutor);
        synchronized (mTasks) {
            final WeakIdentityHashMap<Execution, WeakHashMap<ExecutionTask, Void>> tasks = mTasks;
            WeakHashMap<ExecutionTask, Void> executionTasks = tasks.get(execution);
            if (executionTasks == null) {
                executionTasks = new WeakHashMap<ExecutionTask, Void>();
                tasks.put(execution, executionTasks);
            }

            executionTasks.put(task, null);
        }

        // We need to ensure that a task is always started from the main thread
        sMainRunner.run(task, delay, timeUnit);
    }

    /**
     * Implementation of an async task whose execution starts in a runnable.
     */
    private class ExecutionTask extends AsyncTask<Void, Void, Void> implements Execution {

        private final Execution mExecution;

        private final Executor mExecutor;

        /**
         * Constructor.
         *
         * @param execution the execution instance.
         * @param executor  the executor.
         */
        private ExecutionTask(@NotNull final Execution execution,
                @Nullable final Executor executor) {
            mExecution = execution;
            mExecutor = executor;
        }

        @TargetApi(VERSION_CODES.HONEYCOMB)
        @Override
        public void run() {
            final Executor executor = mExecutor;
            if ((executor != null) && (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)) {
                executeOnExecutor(executor, NO_PARAMS);

            } else {
                execute(NO_PARAMS);
            }
        }

        @Override
        protected Void doInBackground(@NotNull final Void... voids) {
            final Looper looper = Looper.myLooper();
            if (looper != Looper.getMainLooper()) {
                mIsManaged.set(true);
            }

            mExecution.run();
            return null;
        }
    }
}
