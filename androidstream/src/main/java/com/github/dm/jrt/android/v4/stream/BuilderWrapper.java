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

import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of channels.
 * <p>
 * Created by davide-maestroni on 02/27/2016.
 *
 * @param <OUT> the output data type.
 */
class BuilderWrapper<OUT> implements ChannelsBuilder<LoaderStreamChannelCompat<OUT, OUT>>,
        Configurable<ChannelsBuilder<LoaderStreamChannelCompat<OUT, OUT>>> {

    private final ChannelsBuilder<? extends Channel<?, ? extends OUT>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    BuilderWrapper(@NotNull final ChannelsBuilder<? extends Channel<?, ? extends OUT>> wrapped) {
        mBuilder = ConstantConditions.notNull("wrapped instance", wrapped);
    }

    @NotNull
    @Override
    public ChannelsBuilder<LoaderStreamChannelCompat<OUT, OUT>> apply(
            @NotNull final ChannelConfiguration configuration) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mBuilder.channelConfiguration().with(null).with(configuration).apply();
        return this;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public LoaderStreamChannelCompat<OUT, OUT> buildChannels() {
        return new DefaultLoaderStreamChannelCompat<OUT, OUT>(
                (Channel<?, OUT>) mBuilder.buildChannels());
    }

    @NotNull
    @Override
    public Builder<? extends ChannelsBuilder<LoaderStreamChannelCompat<OUT, OUT>>>
    channelConfiguration() {
        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<LoaderStreamChannelCompat<OUT, OUT>>>(this, config);
    }
}
