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

import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Runner decorator employing a shared synchronous runner, when executions are enqueued with a 0
 * delay on one of the managed threads.
 * <p>
 * Created by davide-maestroni on 04/09/2016.
 */
class ZeroDelayRunner extends RunnerDecorator {

    private static final QueuedRunner sSyncRunner = new QueuedRunner();

    private final WeakIdentityHashMap<Execution, ExecutionDecorator> mExecutions =
            new WeakIdentityHashMap<Execution, ExecutionDecorator>();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    ZeroDelayRunner(@NotNull final Runner wrapped) {

        super(wrapped);
    }

    @Override
    public void cancel(@NotNull final Execution execution) {

        final ExecutionDecorator decorator;
        synchronized (mExecutions) {
            decorator = mExecutions.remove(execution);
        }

        if (decorator != null) {
            sSyncRunner.cancel(decorator);
        }

        super.cancel(execution);
    }

    @Override
    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        if ((delay == 0) && isExecutionThread()) {
            ExecutionDecorator decorator;
            synchronized (mExecutions) {
                final WeakIdentityHashMap<Execution, ExecutionDecorator> executions = mExecutions;
                decorator = executions.get(execution);
                if (decorator == null) {
                    decorator = new ExecutionDecorator(execution);
                    executions.put(execution, decorator);
                }
            }

            sSyncRunner.run(decorator, delay, timeUnit);

        } else {
            super.run(execution, delay, timeUnit);
        }
    }
}
