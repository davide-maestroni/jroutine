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

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a routine creating different types of invocations for synchronous and
 * asynchronous mode.
 * <p>
 * Created by davide-maestroni on 04/17/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ConverterRoutine<IN, OUT> extends AbstractRoutine<IN, OUT> {

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    protected ConverterRoutine(@NotNull final InvocationConfiguration configuration) {

        super(configuration);
    }

    @NotNull
    @Override
    protected Invocation<IN, OUT> convertInvocation(@NotNull final Invocation<IN, OUT> invocation,
            @NotNull final InvocationType type) throws Exception {

        try {
            invocation.onDestroy();

        } catch (final Throwable t) {
            InvocationInterruptedException.throwIfInterrupt(t);
            getLogger().wrn(t, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(type);
    }
}
