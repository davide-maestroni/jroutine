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

package com.github.dm.jrt.android.v4.stream;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Functions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of maps of output channels.
 * <p>
 * Created by davide-maestroni on 02/27/2016.
 *
 * @param <OUT> the output data type.
 */
class MapBuilderWrapper<OUT>
        implements ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>>,
        Configurable<ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>>> {

    private final ChannelsBuilder<? extends SparseArrayCompat<OutputChannel<OUT>>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    MapBuilderWrapper(
            @NotNull final ChannelsBuilder<? extends SparseArrayCompat<OutputChannel<OUT>>>
                    wrapped) {

        mBuilder = ConstantConditions.notNull("wrapped instance", wrapped);
    }

    @NotNull
    public ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>> apply(
            @NotNull final ChannelConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mBuilder.channelConfiguration().with(null).with(configuration).apply();
        return this;
    }

    @NotNull
    public SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>> buildChannels() {

        final SparseArrayCompat<OutputChannel<OUT>> channels = mBuilder.buildChannels();
        final int size = channels.size();
        final SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>> channelMap =
                new SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>(size);
        for (int i = 0; i < size; ++i) {
            final DefaultLoaderStreamChannelCompat<OUT, OUT> stream =
                    new DefaultLoaderStreamChannelCompat<OUT, OUT>(null, channels.valueAt(i),
                            Functions.<OutputChannel<OUT>>identity());
            channelMap.append(channels.keyAt(i), stream);
        }

        return channelMap;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT,
            OUT>>>> channelConfiguration() {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>>>(
                this, config);
    }
}
