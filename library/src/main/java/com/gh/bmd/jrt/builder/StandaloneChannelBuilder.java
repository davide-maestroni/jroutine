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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.channel.StandaloneChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of standalone channel objects.
 * <p/>
 * Created by davide on 3/7/15.
 */
public interface StandaloneChannelBuilder extends ConfigurableBuilder {

    /**
     * Builds and returns the standalone channel instance.
     *
     * @return the newly created channel.
     */
    @Nonnull
    <T> StandaloneChannel<T> buildChannel();

    /**
     * Note that only options related to the output channel, the asynchronous runner and the logs
     * will be employed.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    StandaloneChannelBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * Note that only options related to the output channel, the asynchronous runner and the logs
     * will be employed.
     *
     * @param builder the routine configuration builder.
     * @return this builder.
     */
    @Nonnull
    StandaloneChannelBuilder withConfiguration(@Nonnull Builder builder);
}
