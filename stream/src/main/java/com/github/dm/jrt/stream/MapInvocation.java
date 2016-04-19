/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.FunctionWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Filter invocation implementation wrapping a map function.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class MapInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

    private final FunctionWrapper<? super IN, ? extends OutputChannel<? extends OUT>> mFunction;

    /**
     * Constructor.
     *
     * @param function the mapping function.
     */
    MapInvocation(@NotNull final FunctionWrapper<? super IN, ? extends OutputChannel<? extends
            OUT>> function) {

        super(asArgs(function));
        ConstantConditions.notNull("function wrapper", function);
        mFunction = function;
    }

    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

        final OutputChannel<? extends OUT> channel = mFunction.apply(input);
        if (channel != null) {
            channel.bind(result);
        }
    }
}