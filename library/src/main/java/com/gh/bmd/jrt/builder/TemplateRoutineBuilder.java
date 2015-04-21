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
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.routine.TemplateRoutine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Empty abstract implementation of a routine builder.
 * <p/>
 * This class is useful to avoid the need of implementing some of the methods defined in the
 * interface.
 * Created by davide on 3/16/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class TemplateRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutine<INPUT, OUTPUT>
        implements RoutineBuilder<INPUT, OUTPUT> {

    private RoutineConfiguration mConfiguration;

    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> configure(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> configure(@Nonnull final Builder builder) {

        return configure(builder.buildConfiguration());
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> invokeAsync() {

        return buildRoutine().invokeAsync();
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> invokeParallel() {

        return buildRoutine().invokeParallel();
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> invokeSync() {

        return buildRoutine().invokeSync();
    }

    /**
     * Returns the routine configuration.
     *
     * @return the configuration.
     */
    @Nonnull
    protected RoutineConfiguration getConfiguration() {

        return RoutineConfiguration.notNull(mConfiguration);
    }
}
