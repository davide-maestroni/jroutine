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
package com.bmd.wtf.flw;

import com.bmd.wtf.src.Pool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a {@link Flow} employing a
 * {@link java.util.concurrent.ScheduledExecutorService} to run the commands in a pool of
 * threads.
 * <p/>
 * Created by davide on 2/27/14.
 */
public class ThreadPoolFlow implements Flow {

    private final ScheduledExecutorService mService;

    /**
     * Default constructor.
     * <p/>
     * Avoid instantiation outside the package.
     *
     * @param threadPoolSize The maximum size of the thread pool.
     */
    ThreadPoolFlow(final int threadPoolSize) {

        mService = Executors.newScheduledThreadPool(threadPoolSize);
    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final DATA drop) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                pool.discharge(drop);
            }
        });
    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final Iterable<? extends DATA> drops) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                for (final DATA drop : drops) {

                    pool.discharge(drop);
                }
            }
        });
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mService.schedule(new Runnable() {

            @Override
            public void run() {

                pool.discharge(drop);
            }

        }, delay, timeUnit);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mService.schedule(new Runnable() {

            @Override
            public void run() {

                for (final DATA drop : drops) {

                    pool.discharge(drop);
                }
            }

        }, delay, timeUnit);
    }

    @Override
    public void flush(final Pool<?> pool) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                pool.flush();
            }
        });
    }

    @Override
    public void pull(final Pool<?> pool, final Object debris) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                pool.pull(debris);
            }
        });
    }

    @Override
    public void push(final Pool<?> pool, final Object debris) {

        mService.execute(new Runnable() {

            @Override
            public void run() {

                pool.push(debris);
            }
        });
    }
}