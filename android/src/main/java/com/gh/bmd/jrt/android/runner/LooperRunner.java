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
package com.gh.bmd.jrt.android.runner;

import android.os.Handler;
import android.os.Looper;

import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of a runner employing the Android {@link android.os.Looper} queue to execute the
 * routine invocations.
 * <p/>
 * Created by davide-maestroni on 9/28/14.
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
    LooperRunner(@Nonnull final Looper looper) {

        this(looper, new SameThreadRunner(looper));
    }

    /**
     * Constructor.
     *
     * @param looper           the looper to employ.
     * @param sameThreadRunner the runner to be used when this one is called on its own thread.
     *                         If null, the invocation will be posted on the very same looper.
     */
    LooperRunner(@Nonnull final Looper looper, @Nullable final Runner sameThreadRunner) {

        mThread = looper.getThread();
        mHandler = new Handler(looper);
        mSameThreadRunner = (sameThreadRunner != null) ? sameThreadRunner : new Runner() {

            public void cancel(@Nonnull final Execution execution) {

            }

            public boolean isExecutionThread() {

                return true;
            }

            public void run(@Nonnull final Execution execution, final long delay,
                    @Nonnull final TimeUnit timeUnit) {

                internalRun(execution, delay, timeUnit);
            }
        };
    }

    private void internalRun(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        if (delay > 0) {

            mHandler.postDelayed(execution, timeUnit.toMillis(delay));

        } else {

            mHandler.post(execution);
        }
    }

    /**
     * Runner handling execution started from the same looper thread.
     */
    private static class SameThreadRunner implements Runner {

        private final LooperRunner mLooperRunner;

        private final Runner mQueuedRunner = Runners.queuedRunner();

        private SameThreadRunner(@Nonnull final Looper looper) {

            mLooperRunner = new LooperRunner(looper, null);
        }

        public void cancel(@Nonnull final Execution execution) {

            mQueuedRunner.cancel(execution);
            mLooperRunner.cancel(execution);
        }

        public boolean isExecutionThread() {

            return true;
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            if (delay == 0) {

                mQueuedRunner.run(execution, delay, timeUnit);

            } else {

                mLooperRunner.internalRun(execution, delay, timeUnit);
            }
        }
    }

    public void cancel(@Nonnull final Execution execution) {

        mHandler.removeCallbacks(execution);
        mSameThreadRunner.cancel(execution);
    }

    public boolean isExecutionThread() {

        return (Thread.currentThread() == mThread) && mSameThreadRunner.isExecutionThread();
    }

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        if (Thread.currentThread() == mThread) {

            mSameThreadRunner.run(execution, delay, timeUnit);

        } else {

            internalRun(execution, delay, timeUnit);
        }
    }
}
