/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.ConfigurableBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of sharable routines.
 * <p/>
 * Created by davide on 3/7/15.
 *
 * @see com.gh.bmd.jrt.annotation.Share
 */
public interface SharableBuilder extends ConfigurableBuilder {

    @Nonnull
    SharableBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * Tells the builder to create a routine using the specified share tag.
     *
     * @param group the group name.
     * @return this builder.
     * @see com.gh.bmd.jrt.annotation.Share
     */
    @Nonnull
    SharableBuilder withShareGroup(@Nullable String group);
}
