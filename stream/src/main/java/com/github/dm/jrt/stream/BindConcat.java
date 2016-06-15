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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Concat binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class BindConcat<OUT> implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

    private final OutputChannel<? extends OUT> mChannel;

    private final ChannelConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param channel       the output channel to concat.
     */
    BindConcat(@NotNull final ChannelConfiguration configuration,
            @NotNull final OutputChannel<? extends OUT> channel) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mChannel = ConstantConditions.notNull("output channel", channel);
    }

    public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {
        return Channels.<OUT>concat(channel, mChannel).channelConfiguration()
                                                      .with(mConfiguration)
                                                      .apply()
                                                      .buildChannels();
    }
}
