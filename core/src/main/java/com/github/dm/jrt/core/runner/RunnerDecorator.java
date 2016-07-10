/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.runner;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a decorator of a runner object.
 * <p>
 * Created by davide-maestroni on 09/22/2014.
 */
public class RunnerDecorator extends Runner {

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    public RunnerDecorator(@NotNull final Runner wrapped) {
        super(wrapped.getThreadManager());
        mRunner = wrapped;
    }

    @Override
    public void cancel(@NotNull final Execution execution) {
        mRunner.cancel(execution);
    }

    @Override
    public boolean isExecutionThread() {
        return mRunner.isExecutionThread();
    }

    @Override
    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {
        mRunner.run(execution, delay, timeUnit);
    }
}
