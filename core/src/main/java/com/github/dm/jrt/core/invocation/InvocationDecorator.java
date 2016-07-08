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

package com.github.dm.jrt.core.invocation;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Base invocation decorator implementation.
 * <p>
 * Created by davide-maestroni on 08/19/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class InvocationDecorator<IN, OUT> implements Invocation<IN, OUT> {

    private final Invocation<IN, OUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    public InvocationDecorator(@NotNull final Invocation<IN, OUT> wrapped) {
        mInvocation = ConstantConditions.notNull("wrapped invocation", wrapped);
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
        mInvocation.onAbort(reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
        mInvocation.onComplete(result);
    }

    public void onRecycle(final boolean isReused) throws Exception {
        mInvocation.onRecycle(isReused);
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
        mInvocation.onInput(input, result);
    }

    public void onRestart() throws Exception {
        mInvocation.onRestart();
    }
}
