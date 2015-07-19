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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.ChannelConfiguration;
import com.gh.bmd.jrt.builder.ChannelConfiguration.Builder;
import com.gh.bmd.jrt.builder.ChannelConfiguration.Configurable;
import com.gh.bmd.jrt.builder.TransportChannelBuilder;
import com.gh.bmd.jrt.channel.TransportChannel;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of transport channel objects.
 * <p/>
 * Created by davide-maestroni on 10/25/14.
 */
class DefaultTransportChannelBuilder
        implements TransportChannelBuilder, Configurable<TransportChannelBuilder> {

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Avoid direct instantiation.
     */
    DefaultTransportChannelBuilder() {

    }

    @Nonnull
    public <DATA> TransportChannel<DATA> buildChannel() {

        return new DefaultTransportChannel<DATA>(mConfiguration);
    }

    @Nonnull
    public Builder<? extends TransportChannelBuilder> channels() {

        return new Builder<TransportChannelBuilder>(this, mConfiguration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public TransportChannelBuilder setConfiguration(
            @Nonnull final ChannelConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
