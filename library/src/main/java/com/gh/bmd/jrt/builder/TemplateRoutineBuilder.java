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

import com.gh.bmd.jrt.builder.InvocationConfiguration.Builder;
import com.gh.bmd.jrt.builder.InvocationConfiguration.Configurable;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.routine.TemplateRoutine;

import javax.annotation.Nonnull;

/**
 * Empty abstract implementation of a routine builder.
 * <p/>
 * This class is useful to avoid the need of implementing some of the methods defined in the
 * interface.
 * Created by davide-maestroni on 3/16/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class TemplateRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutine<INPUT, OUTPUT>
        implements RoutineBuilder<INPUT, OUTPUT>, Configurable<RoutineBuilder<INPUT, OUTPUT>> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeAsync() {

        return buildRoutine().invokeAsync();
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeParallel() {

        return buildRoutine().invokeParallel();
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeSync() {

        return buildRoutine().invokeSync();
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public Builder<? extends RoutineBuilder<INPUT, OUTPUT>> withInvocation() {

        return new Builder<RoutineBuilder<INPUT, OUTPUT>>(this, mConfiguration);
    }

    /**
     * Returns the invocation configuration.
     *
     * @return the configuration.
     */
    @Nonnull
    protected InvocationConfiguration getConfiguration() {

        return mConfiguration;
    }
}
