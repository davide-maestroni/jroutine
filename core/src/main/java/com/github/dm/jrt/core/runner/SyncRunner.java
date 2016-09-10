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

/**
 * Base abstract implementation of a synchronous runner.
 * <br>
 * For a synchronous runner any thread is an execution thread while no one is managed.
 * <p>
 * Created by davide-maestroni on 06/06/2016.
 */
public abstract class SyncRunner extends Runner {

    private static final ThreadManager sManager = new ThreadManager() {

        public boolean isManagedThread() {
            return false;
        }
    };

    /**
     * Constructor.
     */
    protected SyncRunner() {
        super(sManager);
    }

    @Override
    public void cancel(@NotNull final Execution execution) {
    }

    @Override
    public boolean isExecutionThread() {
        return true;
    }

    @Override
    public boolean isSynchronous() {
        return true;
    }
}
