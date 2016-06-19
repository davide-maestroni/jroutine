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

package com.github.dm.jrt.android.core.builder;

import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.core.builder.ChannelConfigurableBuilder;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface defining a builder of channels bound to loader invocations.
 * <br>
 * In order to be successfully bound, the specific routine invocation must have a user defined ID
 * and still be running (or cached) at the time of the channel creation.
 * <p>
 * Created by davide-maestroni on 01/14/2015.
 *
 * @see com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder LoaderRoutineBuilder
 */
public interface LoaderChannelBuilder extends ChannelConfigurableBuilder<LoaderChannelBuilder>,
        LoaderConfigurableBuilder<LoaderChannelBuilder> {

    /**
     * Builds and returns a channel bound to the routine invocation.
     *
     * @param <OUT> the output data type.
     * @return the newly created channel.
     * @throws java.lang.IllegalArgumentException if the configured loader ID is equal to AUTO.
     */
    @NotNull
    <OUT> Channel<?, OUT> buildChannel();

    /**
     * Makes the builder discard all the cached invocation instances.
     */
    void clear();

    /**
     * Makes the builder discard the cached invocation instances with the specified input.
     *
     * @param input the input.
     */
    void clear(@Nullable Object input);

    /**
     * Makes the builder discard the cached invocation instances with the specified inputs.
     *
     * @param inputs the inputs.
     */
    void clear(@Nullable Object... inputs);

    /**
     * Makes the builder discard the cached invocation instances with the specified inputs.
     *
     * @param inputs the inputs.
     */
    void clear(@Nullable Iterable<?> inputs);

    /**
     * Note that the clash resolution types will be ignored.
     *
     * @return the loader configuration builder.
     */
    @NotNull
    @Override
    Builder<? extends LoaderChannelBuilder> loaderConfiguration();
}
