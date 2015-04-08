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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p/>
 * Created by Davide on 4/6/2015.
 */
public interface ObjectContextRoutineBuilder extends ContextRoutineBuilder, ObjectRoutineBuilder {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder onClash(@Nullable ClashResolutionType resolutionType);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder onComplete(@Nullable CacheStrategyType cacheStrategyType);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withArgs(@Nullable Object... args);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withId(int invocationId);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withShareGroup(@Nullable String group);
}
