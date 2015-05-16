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
     * Constructor.
     *
     * @param looper           the looper to employ.
     * @param sameThreadRunner the runner to be used when this one is called on its own thread.
     *                         If null, the invocation will be posted on the specified looper.
     */
    LooperRunner(@Nonnull final Looper looper, @Nullable final Runner sameThreadRunner) {

        mThread = looper.getThread();
        mHandler = new Handler(looper);
        mSameThreadRunner = (sameThreadRunner != null) ? sameThreadRunner : new Runner() {

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

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        if (Thread.currentThread() == mThread) {

            mSameThreadRunner.run(execution, delay, timeUnit);

        } else {

            internalRun(execution, delay, timeUnit);
        }
    }
}
