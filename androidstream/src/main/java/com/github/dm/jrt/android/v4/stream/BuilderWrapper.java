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
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of output channels.
 * <p/>
 * Created by davide-maestroni on 02/27/2016.
 *
 * @param <OUT> the output data type.
 */
class BuilderWrapper<OUT> implements ChannelsBuilder<LoaderStreamChannelCompat<OUT>>,
        Configurable<ChannelsBuilder<LoaderStreamChannelCompat<OUT>>> {

    private final ChannelsBuilder<? extends OutputChannel<? extends OUT>> mBuilder;

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    BuilderWrapper(@NotNull final ChannelsBuilder<? extends OutputChannel<? extends OUT>> wrapped) {

        mBuilder = ConstantConditions.notNull("wrapped instance", wrapped);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public LoaderStreamChannelCompat<OUT> buildChannels() {

        return new DefaultLoaderStreamChannelCompat<OUT>(null,
                (OutputChannel<OUT>) mBuilder.buildChannels());
    }

    @NotNull
    public ChannelsBuilder<LoaderStreamChannelCompat<OUT>> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mBuilder.withChannels().with(null).with(configuration).setConfiguration();
        return this;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<LoaderStreamChannelCompat<OUT>>> withChannels() {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<LoaderStreamChannelCompat<OUT>>>(this, config);
    }
}
