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

import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Configurable;
import com.gh.bmd.jrt.builder.StandaloneChannelBuilder;
import com.gh.bmd.jrt.channel.StandaloneChannel;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of standalone channel objects.
 * <p/>
 * Created by davide on 10/25/14.
 */
class DefaultStandaloneChannelBuilder
        implements StandaloneChannelBuilder, Configurable<StandaloneChannelBuilder> {

    private RoutineConfiguration mConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Avoid direct instantiation.
     */
    DefaultStandaloneChannelBuilder() {

    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public StandaloneChannelBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public <T> StandaloneChannel<T> buildChannel() {

        return new DefaultStandaloneChannel<T>(mConfiguration);
    }

    @Nonnull
    public Builder<StandaloneChannelBuilder> routineConfiguration() {

        return new Builder<StandaloneChannelBuilder>(this, mConfiguration);
    }
}
