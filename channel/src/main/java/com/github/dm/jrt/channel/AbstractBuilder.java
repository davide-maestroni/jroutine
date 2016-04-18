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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base builder implementation.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <TYPE> the built object type.
 */
public abstract class AbstractBuilder<TYPE>
        implements ChannelsBuilder<TYPE>, Configurable<ChannelsBuilder<TYPE>> {

    private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

    @NotNull
    public ChannelsBuilder<TYPE> applyConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        return this;
    }

    @NotNull
    public TYPE buildChannels() {

        return build(mConfiguration);
    }

    @NotNull
    public Builder<ChannelsBuilder<TYPE>> channelConfiguration() {

        return new Builder<ChannelsBuilder<TYPE>>(this, mConfiguration);
    }

    /**
     * Builds and returns an object instance.
     *
     * @param configuration the instance configuration.
     * @return the object instance.
     */
    @NotNull
    protected abstract TYPE build(@NotNull ChannelConfiguration configuration);
}
