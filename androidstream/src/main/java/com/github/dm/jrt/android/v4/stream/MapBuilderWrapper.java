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

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.ext.channel.ChannelsBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of maps of output channels.
 * <p/>
 * Created by davide-maestroni on 02/27/2016.
 *
 * @param <OUT> the output data type.
 */
class MapBuilderWrapper<OUT>
        implements ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>,
        Configurable<ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>> {

    private final ChannelsBuilder<? extends SparseArrayCompat<OutputChannel<OUT>>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    @SuppressWarnings("ConstantConditions")
    MapBuilderWrapper(
            @NotNull final ChannelsBuilder<? extends SparseArrayCompat<OutputChannel<OUT>>>
                    wrapped) {

        if (wrapped == null) {
            throw new NullPointerException("the wrapped builder must not be null");
        }

        mBuilder = wrapped;
    }

    @NotNull
    public SparseArrayCompat<LoaderStreamChannelCompat<OUT>> build() {

        final SparseArrayCompat<OutputChannel<OUT>> channels = mBuilder.build();
        final int size = channels.size();
        final SparseArrayCompat<LoaderStreamChannelCompat<OUT>> channelMap =
                new SparseArrayCompat<LoaderStreamChannelCompat<OUT>>(size);
        for (int i = 0; i < size; i++) {
            final DefaultLoaderStreamChannelCompat<OUT> stream =
                    new DefaultLoaderStreamChannelCompat<OUT>(null, channels.valueAt(i));
            channelMap.append(channels.keyAt(i), stream);
        }

        return channelMap;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        mBuilder.withChannels().with(null).with(configuration).getConfigured();
        return this;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>>
    withChannels() {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>>(this,
                                                                                               config);
    }
}
