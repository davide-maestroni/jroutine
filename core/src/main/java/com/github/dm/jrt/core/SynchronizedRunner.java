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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.ExecutionDecorator;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Runner implementation synchronizing the passed executions.
 * <p>
 * Created by davide-maestroni on 09/05/2016.
 */
class SynchronizedRunner extends RunnerDecorator {

    private final Object mExecutionMutex = new Object();

    private final WeakIdentityHashMap<Execution, WeakReference<SynchronizedExecution>> mExecutions =
            new WeakIdentityHashMap<Execution, WeakReference<SynchronizedExecution>>();

    private final Object mMutex = new Object();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    SynchronizedRunner(@NotNull final Runner wrapped) {
        super(wrapped);
    }

    @Override
    public void cancel(@NotNull final Execution execution) {
        SynchronizedExecution synchronizedExecution = null;
        synchronized (mMutex) {
            final WeakReference<SynchronizedExecution> executionReference =
                    mExecutions.get(execution);
            if (executionReference != null) {
                synchronizedExecution = executionReference.get();
            }
        }

        if (synchronizedExecution != null) {
            super.cancel(synchronizedExecution);
        }
    }

    @Override
    public void run(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {
        SynchronizedExecution synchronizedExecution;
        synchronized (mMutex) {
            final WeakIdentityHashMap<Execution, WeakReference<SynchronizedExecution>> executions =
                    mExecutions;
            final WeakReference<SynchronizedExecution> executionReference =
                    executions.get(execution);
            synchronizedExecution = (executionReference != null) ? executionReference.get() : null;
            if (synchronizedExecution == null) {
                synchronizedExecution = new SynchronizedExecution(execution);
                executions.put(execution,
                        new WeakReference<SynchronizedExecution>(synchronizedExecution));
            }
        }

        super.run(synchronizedExecution, delay, timeUnit);
    }

    /**
     * Synchronized execution decorator.
     */
    private class SynchronizedExecution extends ExecutionDecorator {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped instance.
         */
        public SynchronizedExecution(@NotNull final Execution wrapped) {
            super(wrapped);
        }

        @Override
        public void run() {
            synchronized (mExecutionMutex) {
                super.run();
            }
        }
    }
}
