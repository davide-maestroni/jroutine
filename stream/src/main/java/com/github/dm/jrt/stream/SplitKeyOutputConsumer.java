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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Split by key output consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class SplitKeyOutputConsumer<IN, OUT> extends BindMap<IN, OUT> implements OutputConsumer<IN> {

    private final HashMap<Object, IOChannel<IN>> mInputChannels =
            new HashMap<Object, IOChannel<IN>>();

    private final Function<? super IN, ?> mKeyFunction;

    private final IOChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param outputChannel  the output channel instance.
     * @param keyFunction    the key function.
     * @param routine        the routine instance.
     * @param invocationMode the invocation mode.
     */
    SplitKeyOutputConsumer(@NotNull final IOChannel<OUT> outputChannel,
            @NotNull final Function<? super IN, ?> keyFunction,
            @NotNull final Routine<? super IN, ? extends OUT> routine,
            @NotNull final InvocationMode invocationMode) {

        super(routine, invocationMode);
        mOutputChannel = ConstantConditions.notNull("output channel instance", outputChannel);
        mKeyFunction = ConstantConditions.notNull("key function", keyFunction);
    }

    public void onComplete() {

        mOutputChannel.close();
        for (final IOChannel<IN> channel : mInputChannels.values()) {
            channel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {

        mOutputChannel.abort(error);
    }

    public void onOutput(final IN output) throws Exception {

        final HashMap<Object, IOChannel<IN>> channels = mInputChannels;
        final Object key = mKeyFunction.apply(output);
        IOChannel<IN> inputChannel = channels.get(key);
        if (inputChannel == null) {
            inputChannel = JRoutineCore.io().buildChannel();
            mOutputChannel.pass(super.apply(inputChannel));
            channels.put(key, inputChannel);
        }

        inputChannel.pass(output);
    }
}
