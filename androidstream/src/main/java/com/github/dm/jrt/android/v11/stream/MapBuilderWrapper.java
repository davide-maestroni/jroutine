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

package com.github.dm.jrt.android.v11.stream;

import android.util.SparseArray;

import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of maps of output channels.
 * <p/>
 * Created by davide-maestroni on 02/27/2016.
 *
 * @param <OUT> the output data type.
 */
class MapBuilderWrapper<OUT> implements ChannelsBuilder<SparseArray<LoaderStreamChannel<OUT>>>,
        Configurable<ChannelsBuilder<SparseArray<LoaderStreamChannel<OUT>>>> {

    private final ChannelsBuilder<? extends SparseArray<OutputChannel<OUT>>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    MapBuilderWrapper(
            @NotNull final ChannelsBuilder<? extends SparseArray<OutputChannel<OUT>>> wrapped) {

        if (wrapped == null) {
            throw new NullPointerException("the wrapped builder must not be null");
        }

        mBuilder = wrapped;
    }

    @NotNull
    public SparseArray<LoaderStreamChannel<OUT>> build() {

        final SparseArray<OutputChannel<OUT>> channels = mBuilder.build();
        final int size = channels.size();
        final SparseArray<LoaderStreamChannel<OUT>> channelMap =
                new SparseArray<LoaderStreamChannel<OUT>>(size);
        for (int i = 0; i < size; i++) {
            final DefaultLoaderStreamChannel<OUT> stream =
                    new DefaultLoaderStreamChannel<OUT>(null, channels.valueAt(i));
            channelMap.append(channels.keyAt(i), stream);
        }

        return channelMap;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ChannelsBuilder<SparseArray<LoaderStreamChannel<OUT>>> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        mBuilder.withChannels().with(null).with(configuration).setConfiguration();
        return this;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<SparseArray<LoaderStreamChannel<OUT>>>> withChannels
            () {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<SparseArray<LoaderStreamChannel<OUT>>>>(this, config);
    }
}