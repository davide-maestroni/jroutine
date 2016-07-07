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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Try/finally binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class BindTryFinally<OUT> implements Function<Channel<?, OUT>, Channel<?, OUT>> {

    private final ChannelConfiguration mConfiguration;

    private final Action mFinally;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param finallyAction the finally action.
     */
    BindTryFinally(@NotNull final ChannelConfiguration configuration,
            @NotNull final Action finallyAction) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mFinally = ConstantConditions.notNull("action instance", finallyAction);
    }

    public Channel<?, OUT> apply(final Channel<?, OUT> channel) {
        final Channel<OUT, OUT> outputChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(mConfiguration)
                                                            .applied()
                                                            .buildChannel();
        channel.bind(new TryFinallyChannelConsumer<OUT>(mFinally, outputChannel));
        return outputChannel;
    }
}
