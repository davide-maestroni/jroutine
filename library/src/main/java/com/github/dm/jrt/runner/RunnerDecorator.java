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
package com.github.dm.jrt.runner;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Implementation of a decorator of a runner object.
 * <p/>
 * Created by davide-maestroni on 9/22/14.
 */
public class RunnerDecorator implements Runner {

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    public RunnerDecorator(@Nonnull final Runner wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped runner must not be null");
        }

        mRunner = wrapped;
    }

    public void cancel(@Nonnull final Execution execution) {

        mRunner.cancel(execution);
    }

    public boolean isExecutionThread() {

        return mRunner.isExecutionThread();
    }

    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        mRunner.run(execution, delay, timeUnit);
    }
}
