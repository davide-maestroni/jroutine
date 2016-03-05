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

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ChannelsBuilder;

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
    @SuppressWarnings("ConstantConditions")
    BuilderWrapper(@NotNull final ChannelsBuilder<? extends OutputChannel<? extends OUT>> wrapped) {

        if (wrapped == null) {
            throw new NullPointerException("the wrapped builder must not be null");
        }

        mBuilder = wrapped;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public LoaderStreamChannelCompat<OUT> build() {

        return new DefaultLoaderStreamChannelCompat<OUT>(null,
                                                         (OutputChannel<OUT>) mBuilder.build());
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ChannelsBuilder<LoaderStreamChannelCompat<OUT>> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        mBuilder.withChannels().with(null).with(configuration).getConfigured();
        return this;
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<LoaderStreamChannelCompat<OUT>>> withChannels() {

        final ChannelConfiguration config = mConfiguration;
        return new Builder<ChannelsBuilder<LoaderStreamChannelCompat<OUT>>>(this, config);
    }
}
