/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.builder.TransportChannelBuilder;
import com.github.dm.jrt.channel.TransportChannel;

import org.jetbrains.annotations.NotNull;

/**
 * Class implementing a builder of transport channel objects.
 * <p/>
 * Created by davide-maestroni on 10/25/2014.
 */
class DefaultTransportChannelBuilder
        implements TransportChannelBuilder, Configurable<TransportChannelBuilder> {

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Avoid direct instantiation.
     */
    DefaultTransportChannelBuilder() {

    }

    @NotNull
    public <DATA> TransportChannel<DATA> buildChannel() {

        return new DefaultTransportChannel<DATA>(mConfiguration);
    }

    @NotNull
    public Builder<? extends TransportChannelBuilder> channels() {

        return new Builder<TransportChannelBuilder>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public TransportChannelBuilder setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
