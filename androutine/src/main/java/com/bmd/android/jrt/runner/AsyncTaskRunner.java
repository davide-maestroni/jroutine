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
package com.bmd.android.jrt.runner;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;

import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Implementation of a runner employing {@link android.os.AsyncTask} instances to execute the
 * routine invocations.
 * <p/>
 * Created by davide on 9/28/14.
 */
class AsyncTaskRunner implements Runner {

    private final Executor mExecutor;

    private final Handler mHandler;

    /**
     * Constructor.
     * <p/>
     * Note that, in case a null executor is passed as parameter, the default one will be used.
     *
     * @param executor the executor.
     */
    AsyncTaskRunner(@Nullable final Executor executor) {

        mExecutor = executor;
        // the handler is used to ensure that a task is always started in the main thread
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run(@NonNull final Invocation invocation, final long delay,
            @NonNull final TimeUnit timeUnit) {

        final InvocationTask task = new InvocationTask(mExecutor, invocation);

        if (delay > 0) {

            mHandler.postDelayed(task, timeUnit.toMillis(delay));

        } else {

            mHandler.post(task);
        }
    }

    /**
     * Implementation of an async task whose execution starts in a runnable.
     */
    private static class InvocationTask extends AsyncTask<Void, Void, Void> implements Runnable {

        private final Executor mExecutor;

        private final Invocation mInvocation;

        /**
         * Constructor.
         *
         * @param executor   the executor.
         * @param invocation the invocation instance.
         */
        private InvocationTask(@Nullable final Executor executor,
                @NonNull final Invocation invocation) {

            mExecutor = executor;
            mInvocation = invocation;
        }

        @Override
        @TargetApi(11)
        public void run() {

            if ((mExecutor != null) && (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)) {

                executeOnExecutor(mExecutor);

            } else {

                execute();
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            mInvocation.run();

            return null;
        }
    }
}