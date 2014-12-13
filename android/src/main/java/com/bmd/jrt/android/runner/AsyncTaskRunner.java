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
package com.bmd.jrt.android.runner;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;

import com.bmd.jrt.runner.Execution;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of a runner employing {@link android.os.AsyncTask} instances to execute the
 * routine invocations.
 * <p/>
 * Created by davide on 9/28/14.
 */
class AsyncTaskRunner extends LooperRunner {

    private final Executor mExecutor;

    /**
     * Constructor.
     * <p/>
     * Note that, in case a null executor is passed as parameter, the default one will be used.
     *
     * @param executor the executor.
     */
    AsyncTaskRunner(@Nullable final Executor executor) {

        super(Looper.getMainLooper(), null);

        mExecutor = executor;
    }

    @Override
    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        final ExecutionTask task = new ExecutionTask(mExecutor, execution);
        // the super method is called to ensure that a task is always started in the main thread
        super.run(task, delay, timeUnit);
    }

    /**
     * Implementation of an async task whose execution starts in a runnable.
     */
    private static class ExecutionTask extends AsyncTask<Void, Void, Void> implements Execution {

        private final Execution mExecution;

        private final Executor mExecutor;

        /**
         * Constructor.
         *
         * @param executor  the executor.
         * @param execution the execution instance.
         */
        private ExecutionTask(@Nullable final Executor executor,
                @Nonnull final Execution execution) {

            mExecutor = executor;
            mExecution = execution;
        }

        @Override
        @TargetApi(VERSION_CODES.HONEYCOMB)
        public void run() {

            if ((mExecutor != null) && (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)) {

                executeOnExecutor(mExecutor);

            } else {

                execute();
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            mExecution.run();
            return null;
        }
    }
}