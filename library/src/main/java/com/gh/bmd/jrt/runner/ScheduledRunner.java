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
package com.gh.bmd.jrt.runner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Class implementing a runner employing an executor service.
 * <p/>
 * Created by davide on 10/14/14.
 */
class ScheduledRunner implements Runner {

    private final ScheduledExecutorService mService;

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    @SuppressWarnings("ConstantConditions")
    ScheduledRunner(@Nonnull final ScheduledExecutorService service) {

        if (service == null) {

            throw new NullPointerException("the executor service must not be null");
        }

        mService = service;
    }

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        if (delay > 0) {

            mService.schedule(execution, delay, timeUnit);

        } else {

            mService.execute(execution);
        }
    }
}
