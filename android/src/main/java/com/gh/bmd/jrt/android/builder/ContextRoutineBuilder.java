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

import com.gh.bmd.jrt.builder.ConfigurableBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routines bound to a context lifecycle.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so, waiting for the outputs right after the routine invocation, will result in a deadlock.<br/>
 * The local context of the invocations will always be the application one.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * and constructor argument objects might be employed to check for clashing of invocations or
 * compute the invocation ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or the <code>ABORT_THAT</code> clash resolution should be used
 * to avoid unexpected results.
 * <p/>
 * Created by Davide on 4/6/2015.
 */
public interface ContextRoutineBuilder extends ConfigurableBuilder {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ContextRoutineBuilder withConfig(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ContextRoutineBuilder withConfig(@Nonnull RoutineConfiguration.Builder builder);

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
    ContextRoutineBuilder withInvocations(@Nullable ContextInvocationConfiguration configuration);

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
    ContextRoutineBuilder withInvocations(@Nonnull ContextInvocationConfiguration.Builder builder);
}
