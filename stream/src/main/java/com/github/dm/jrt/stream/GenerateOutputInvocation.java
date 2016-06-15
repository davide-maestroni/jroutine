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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation implementation generating a list of outputs.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 *
 * @param <OUT> the output data type.
 */
class GenerateOutputInvocation<OUT> extends GenerateInvocation<Object, OUT> {

    private final List<OUT> mOutputs;

    /**
     * Constructor.
     *
     * @param outputs the list of outputs.
     */
    GenerateOutputInvocation(@NotNull final List<OUT> outputs) {
        super(asArgs(outputs));
        mOutputs = outputs;
    }

    public void onInput(final Object input, @NotNull final ResultChannel<OUT> result) {
    }

    public void onResult(@NotNull final ResultChannel<OUT> result) {
        result.pass(mOutputs);
    }
}
