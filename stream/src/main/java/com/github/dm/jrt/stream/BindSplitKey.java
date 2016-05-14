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
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Split by key binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindSplitKey<IN, OUT> implements Function<OutputChannel<IN>, OutputChannel<OUT>> {

    private final ChannelConfiguration mConfiguration;

    private final InvocationMode mInvocationMode;

    private final Function<? super IN, ?> mKey;

    private final Routine<? super IN, ? extends OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param configuration  the channel configuration.
     * @param key            the key function.
     * @param routine        the routine instance.
     * @param invocationMode the invocation mode.
     */
    BindSplitKey(@NotNull final ChannelConfiguration configuration,
            @NotNull final Function<? super IN, ?> key,
            @NotNull final Routine<? super IN, ? extends OUT> routine,
            @NotNull final InvocationMode invocationMode) {

        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mKey = ConstantConditions.notNull("key function", key);
        mRoutine = ConstantConditions.notNull("routine instance", routine);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
    }

    public OutputChannel<OUT> apply(final OutputChannel<IN> channel) {

        final IOChannel<OUT> outputChannel = JRoutineCore.io()
                                                         .channelConfiguration()
                                                         .with(mConfiguration)
                                                         .apply()
                                                         .buildChannel();
        channel.bind(new SplitKeyOutputConsumer<IN, OUT>(outputChannel, mKey, mRoutine,
                mInvocationMode));
        return outputChannel;
    }
}
