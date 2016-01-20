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
package com.github.dm.jrt.android.runner;

import android.os.Handler;
import android.os.Looper;

import com.github.dm.jrt.runner.Execution;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a runner employing the Android {@link android.os.Looper} queue to execute the
 * routine invocations.
 * <p/>
 * Created by davide-maestroni on 09/28/2014.
 */
class LooperRunner implements Runner {

    private final Handler mHandler;

    private final Runner mSameThreadRunner;

    private final Thread mThread;

    /**
     * Constructor.<br/>
     * Note that, when the invocation runs in the looper thread, the executions with a delay of 0
     * will be performed synchronously, while the ones with a positive delay will be posted on the
     * same thread.
     *
     * @param looper the looper to employ.
     */
    LooperRunner(@NotNull final Looper looper) {

        this(looper, new LooperThreadRunner(looper));
    }

    /**
     * Constructor.
     *
     * @param looper           the looper to employ.
     * @param sameThreadRunner the runner to be used when this one is called on its own thread.
     *                         If null, the invocation will be posted on the very same looper.
     */
    LooperRunner(@NotNull final Looper looper, @Nullable final Runner sameThreadRunner) {

        mThread = looper.getThread();
        mHandler = new Handler(looper);
        mSameThreadRunner = (sameThreadRunner != null) ? sameThreadRunner : new PostRunner(this);
    }

    private void internalRun(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        if (delay > 0) {
            mHandler.postDelayed(execution, timeUnit.toMillis(delay));

        } else {
            mHandler.post(execution);
        }
    }

    /**
     * Runner handling execution started from the same looper thread.
     */
    private static class LooperThreadRunner implements Runner {

        private final LooperRunner mLooperRunner;

        private final Runner mQueuedRunner = Runners.syncRunner();

        /**
         * Constructor.
         *
         * @param looper the looper instance.
         */
        private LooperThreadRunner(@NotNull final Looper looper) {

            mLooperRunner = new LooperRunner(looper, null);
        }

        public void cancel(@NotNull final Execution execution) {

            mQueuedRunner.cancel(execution);
            mLooperRunner.cancel(execution);
        }

        public boolean isExecutionThread() {

            return true;
        }

        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

            if (delay == 0) {
                mQueuedRunner.run(execution, delay, timeUnit);

            } else {
                mLooperRunner.internalRun(execution, delay, timeUnit);
            }
        }
    }

    /**
     * Runner posting execution on the runner looper.
     */
    private static class PostRunner implements Runner {

        private final LooperRunner mLooperRunner;

        /**
         * Constructor.
         *
         * @param runner the looper runner.
         */
        private PostRunner(@NotNull final LooperRunner runner) {

            mLooperRunner = runner;
        }

        public void cancel(@NotNull final Execution execution) {

        }

        public boolean isExecutionThread() {

            return true;
        }

        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

            mLooperRunner.internalRun(execution, delay, timeUnit);
        }
    }

    public void cancel(@NotNull final Execution execution) {

        mHandler.removeCallbacks(execution);
        mSameThreadRunner.cancel(execution);
    }

    public boolean isExecutionThread() {

        return (Thread.currentThread() == mThread) && mSameThreadRunner.isExecutionThread();
    }

    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        if (Thread.currentThread() == mThread) {
            mSameThreadRunner.run(execution, delay, timeUnit);

        } else {
            internalRun(execution, delay, timeUnit);
        }
    }
}
