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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base invocation transforming data coming from an input channel into an output one.
 * <p>
 * Created by davide-maestroni on 06/11/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class StreamInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private Channel<IN, IN> mInputChannel;

    private Channel<?, OUT> mOutputChannel;

    /**
     * Constructor.
     */
    public StreamInvocation() {
        mInputChannel = null;
        mOutputChannel = null;
    }

    @Override
    public final void onAbort(@NotNull final RoutineException reason) {
        mInputChannel.abort(reason);
    }

    @Override
    public final void onComplete(@NotNull final Channel<OUT, ?> result) {
        bind(result);
        mInputChannel.close();
    }

    @Override
    public void onDiscard() throws Exception {
    }

    @Override
    public final void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
        bind(result);
        mInputChannel.pass(input);
    }

    @Override
    public final void onRestart() throws Exception {
        final Channel<IN, IN> inputChannel = (mInputChannel = JRoutineCore.io().buildChannel());
        mOutputChannel = ConstantConditions.notNull("stream channel", onChannel(inputChannel));
    }

    /**
     * Transforms the channel providing the input data into the output one.
     *
     * @param channel the input channel.
     * @return the output channel.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    protected abstract Channel<?, OUT> onChannel(@NotNull Channel<?, IN> channel) throws Exception;

    private void bind(@NotNull final Channel<OUT, ?> result) {
        final Channel<?, OUT> outputChannel = mOutputChannel;
        if (!outputChannel.isBound()) {
            outputChannel.bind(result);
        }
    }
}
