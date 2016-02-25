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

package com.github.dm.jrt.ext.channel;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base builder implementation.
 *
 * @param <TYPE> the built object type.
 */
public abstract class AbstractBuilder<TYPE>
        implements ChannelsBuilder<TYPE>, Configurable<ChannelsBuilder<TYPE>> {

    private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

    @NotNull
    public TYPE build() {

        return build(mConfiguration);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ChannelsBuilder<TYPE> setConfiguration(
            @NotNull final ChannelConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @NotNull
    public Builder<ChannelsBuilder<TYPE>> withChannels() {

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
