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

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.ext.channel.ChannelsBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder implementation wrapping a builder of maps of output channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MapBuilderWrapper<OUT> implements ChannelsBuilder<Map<Integer, StreamChannel<OUT>>>,
        Configurable<ChannelsBuilder<Map<Integer, StreamChannel<OUT>>>> {

    private final ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    MapBuilderWrapper(
            @NotNull final ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> wrapped) {

        if (wrapped == null) {
            throw new NullPointerException("the wrapped builder instance must not be null");
        }

        mBuilder = wrapped;
    }

    @NotNull
    public Map<Integer, StreamChannel<OUT>> build() {

        final Map<Integer, OutputChannel<OUT>> channels = mBuilder.build();
        final HashMap<Integer, StreamChannel<OUT>> channelMap =
                new HashMap<Integer, StreamChannel<OUT>>(channels.size());
        for (final Entry<Integer, OutputChannel<OUT>> entry : channels.entrySet()) {
            channelMap.put(entry.getKey(), Streams.streamOf(entry.getValue()));
        }

        return channelMap;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ChannelsBuilder<Map<Integer, StreamChannel<OUT>>> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        mBuilder.withChannels().with(null).with(configuration).getConfigured();
        return this;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<Map<Integer, StreamChannel<OUT>>>> withChannels() {

        return new Builder<ChannelsBuilder<Map<Integer, StreamChannel<OUT>>>>(this, mConfiguration);
    }
}
