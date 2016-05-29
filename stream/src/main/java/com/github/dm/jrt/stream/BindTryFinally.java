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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Try/finally binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class BindTryFinally<OUT> implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

    private final ChannelConfiguration mConfiguration;

    private final Runnable mRunnable;

    /**
     * Constructor.
     *
     * @param configuration   the channel configuration.
     * @param finallyRunnable the final runnable.
     */
    BindTryFinally(@NotNull final ChannelConfiguration configuration,
            @NotNull final Runnable finallyRunnable) {

        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mRunnable = ConstantConditions.notNull("runnable instance", finallyRunnable);
    }

    public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {

        final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .with(mConfiguration)
                                                     .apply()
                                                     .buildChannel();
        channel.bind(new TryFinallyOutputConsumer<OUT>(mRunnable, ioChannel));
        return ioChannel;
    }
}
