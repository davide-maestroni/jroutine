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

import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder implementation wrapping a builder of maps of output channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MapBuilderWrapper<OUT> implements ChannelsBuilder<Map<Integer, StreamChannel<OUT, OUT>>>,
        Configurable<ChannelsBuilder<Map<Integer, StreamChannel<OUT, OUT>>>> {

    private final ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    MapBuilderWrapper(
            @NotNull final ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> wrapped) {

        mBuilder = ConstantConditions.notNull("wrapped instance", wrapped);
    }

    @NotNull
    public ChannelsBuilder<Map<Integer, StreamChannel<OUT, OUT>>> apply(
            @NotNull final ChannelConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        mBuilder.channelConfiguration().with(null).with(configuration).apply();
        return this;
    }

    @NotNull
    public Map<Integer, StreamChannel<OUT, OUT>> buildChannels() {

        final Map<Integer, OutputChannel<OUT>> channels = mBuilder.buildChannels();
        final HashMap<Integer, StreamChannel<OUT, OUT>> channelMap =
                new HashMap<Integer, StreamChannel<OUT, OUT>>(channels.size());
        for (final Entry<Integer, OutputChannel<OUT>> entry : channels.entrySet()) {
            channelMap.put(entry.getKey(), Streams.streamOf(entry.getValue()));
        }

        return channelMap;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<Map<Integer, StreamChannel<OUT, OUT>>>>
    channelConfiguration() {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<Map<Integer, StreamChannel<OUT, OUT>>>>(this, config);
    }
}
