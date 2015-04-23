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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a configurable builder.
 * <p/>
 * Created by davide on 3/6/15.
 */
public interface ConfigurableBuilder {

    /**
     * Sets the specified configuration to this builder by replacing any configuration already set.
     * <br/>
     * Note that the configuration options not supported by the builder implementation might be
     * ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     */
    @Nonnull
    ConfigurableBuilder configure(@Nullable RoutineConfiguration configuration);

    /**
     * Sets the specified configuration to this builder by replacing any configuration already set.
     * <br/>
     * Note that the configuration options not supported by the builder implementation might be
     * ignored.
     *
     * @param builder the configuration builder.
     * @return this builder.
     */
    @Nonnull
    ConfigurableBuilder configure(@Nonnull Builder builder);
}
