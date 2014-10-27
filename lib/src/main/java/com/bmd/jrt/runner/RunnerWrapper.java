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

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Wrapper implementation of a runner object.
 * <p/>
 * Created by davide on 9/22/14.
 */
public class RunnerWrapper implements Runner {

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     * @throws NullPointerException if the specified instance is null.
     */
    @SuppressWarnings("ConstantConditions")
    public RunnerWrapper(@Nonnull final Runner wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the wrapped runner must not be null");
        }

        mRunner = wrapped;
    }

    @Override
    public void run(@Nonnull final Execution execution, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        mRunner.run(execution, delay, timeUnit);
    }
}