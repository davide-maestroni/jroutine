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
package com.bmd.jrt.runner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/9/14.
 */
class ThreadPoolRunner implements Runner {

    private final ScheduledExecutorService mService;

    public ThreadPoolRunner(final int threadPoolSize) {

        mService = Executors.newScheduledThreadPool(threadPoolSize);
    }

    @Override
    public void onInput(final Processing processing, final long delay, final TimeUnit timeUnit) {

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                processing.onInput();
            }
        };

        if (delay > 0) {

            mService.schedule(runnable, delay, timeUnit);

        } else {

            mService.execute(runnable);
        }
    }

    @Override
    public void onReset(final Processing processing) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                processing.onReset();
            }
        });
    }
}