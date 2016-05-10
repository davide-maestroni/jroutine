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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.TransformInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Operation invocation based on a function instance.
 * <p>
 * Created by davide-maestroni on 04/23/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class FunctionTransformInvocation<IN, OUT> extends TransformInvocation<IN, OUT> {

    private final FunctionWrapper<? super IN, ? extends OUT> mFunction;

    /**
     * Constructor.
     *
     * @param function the function instance.
     */
    FunctionTransformInvocation(
            @NotNull final FunctionWrapper<? super IN, ? extends OUT> function) {

        super(asArgs(ConstantConditions.notNull("function wrapper", function)));
        mFunction = function;
    }

    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws Exception {

        result.pass(mFunction.apply(input));
    }
}
