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
package com.bmd.wtf.crr;

import com.bmd.wtf.flw.Fall;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a {@link Current} employing a
 * {@link java.util.concurrent.ScheduledExecutorService} to run the commands in a pool of
 * threads.
 * <p/>
 * Created by davide on 6/8/14.
 */
public class ThreadPoolCurrent implements Current {

    private final ScheduledExecutorService mService;

    /**
     * Avoid instantiation outside the package.
     *
     * @param threadPoolSize The maximum size of the thread pool.
     */
    ThreadPoolCurrent(final int threadPoolSize) {

        mService = Executors.newScheduledThreadPool(threadPoolSize);
    }

    @Override
    public void discharge(final Fall<?> fall) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                fall.discharge();
            }
        });
    }

    @Override
    public void forward(final Fall<?> fall, final Throwable throwable) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                fall.forward(throwable);
            }
        });
    }

    @Override
    public <DATA> void push(final Fall<DATA> fall, final DATA drop) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                fall.push(drop);
            }
        });
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        mService.schedule(new Runnable() {

            @Override
            public void run() {

                fall.push(drop);
            }
        }, delay, timeUnit);
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        final ScheduledExecutorService service = mService;

        for (final DATA drop : drops) {

            service.schedule(new Runnable() {

                @Override
                public void run() {

                    fall.push(drop);
                }
            }, delay, timeUnit);
        }
    }
}