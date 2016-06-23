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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation wrapping a builder of channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class BuilderWrapper<OUT> implements ChannelsBuilder<StreamChannel<OUT, OUT>>,
        Configurable<ChannelsBuilder<StreamChannel<OUT, OUT>>> {

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
    public ChannelsBuilder<StreamChannel<OUT, OUT>> apply(
            @NotNull final ChannelConfiguration configuration) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mBuilder.channelConfiguration().with(null).with(configuration).applied();
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public StreamChannel<OUT, OUT> buildChannels() {
        return new DefaultStreamChannel<OUT, OUT>((Channel<?, OUT>) mBuilder.buildChannels());
    }

    @NotNull
    public Builder<? extends ChannelsBuilder<StreamChannel<OUT, OUT>>> channelConfiguration() {
        return new Builder<ChannelsBuilder<StreamChannel<OUT, OUT>>>(this, mConfiguration);
    }
}
