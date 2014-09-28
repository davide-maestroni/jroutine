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

import android.os.Handler;
import android.os.Looper;

import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a runner employing the Android {@link android.os.Looper} queue to execute
 * the routine invocations.
 * <p/>
 * Created by davide on 9/28/14.
 */
class LooperRunner implements Runner {

    private final Handler mHandler;

    private final Runner mQueuedRunner;

    private final Thread mThread;

    /**
     * Constructor.
     *
     * @param looper the looper to employ.
     * @throws java.lang.IllegalArgumentException if the specified looper is null.
     */
    public LooperRunner(final Looper looper) {

        if (looper == null) {

            throw new IllegalArgumentException("the looper instance must not be null");
        }

        mThread = looper.getThread();
        mHandler = new Handler(looper);
        mQueuedRunner = Runners.queued();
    }

    @Override
    public void run(final Invocation invocation, final long delay, final TimeUnit timeUnit) {

        if (Thread.currentThread().equals(mThread)) {

            mQueuedRunner.run(invocation, delay, timeUnit);

        } else {

            final Runnable runnable = new Runnable() {

                @Override
                public void run() {

                    invocation.run();
                }
            };

            if (delay > 0) {

                mHandler.postDelayed(runnable, timeUnit.toMillis(delay));

            } else {

                mHandler.post(runnable);
            }
        }
    }

    @Override
    public void runAbort(final Invocation invocation) {

        if (Thread.currentThread().equals(mThread)) {

            mQueuedRunner.runAbort(invocation);

        } else {

            mHandler.post(new Runnable() {

                @Override
                public void run() {

                    invocation.abort();
                }
            });
        }
    }
}