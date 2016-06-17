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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class implementing a builder of channel objects.
 * <p>
 * Created by davide-maestroni on 10/25/2014.
 */
class DefaultChannelBuilder implements ChannelBuilder, Configurable<ChannelBuilder> {

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    /**
     * Constructor.
     */
    DefaultChannelBuilder() {
    }

    @NotNull
    public ChannelBuilder apply(@NotNull final ChannelConfiguration configuration) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        return this;
    }

    @NotNull
    public <DATA> Channel<DATA, DATA> buildChannel() {
        return new DefaultChannel<DATA>(mConfiguration);
    }

    @NotNull
    public <DATA> Channel<DATA, DATA> of() {
        return this.<DATA>buildChannel().close();
    }

    @NotNull
    public <DATA> Channel<DATA, DATA> of(@Nullable final DATA input) {
        return this.<DATA>buildChannel().pass(input).close();
    }

    @NotNull
    public <DATA> Channel<DATA, DATA> of(@Nullable final DATA... inputs) {
        return this.<DATA>buildChannel().pass(inputs).close();
    }

    @NotNull
    public <DATA> Channel<DATA, DATA> of(@Nullable final Iterable<DATA> inputs) {
        return this.<DATA>buildChannel().pass(inputs).close();
    }

    @NotNull
    public Builder<? extends ChannelBuilder> channelConfiguration() {
        return new Builder<ChannelBuilder>(this, mConfiguration);
    }
}
