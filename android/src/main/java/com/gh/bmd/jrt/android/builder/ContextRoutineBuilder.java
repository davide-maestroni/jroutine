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

import com.gh.bmd.jrt.android.routine.ContextRoutine;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines bound to a context lifecycle.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so, waiting for the outputs right after the routine invocation, will result in a deadlock.<br/>
 * The local context of the invocations will always be the application one.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface ContextRoutineBuilder<INPUT, OUTPUT>
        extends RoutineBuilder<INPUT, OUTPUT>, ContextRoutine<INPUT, OUTPUT> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ContextRoutine<INPUT, OUTPUT> buildRoutine();

    /**
     * Gets the routine configuration builder related to this builder instance.<br/>
     * All the options related to the output and input channels size and timeout will be ignored.
     * <p/>
     * Note that the builder will be initialized with the current configuration.
     *
     * @return the routine configuration builder.
     */
    @Nonnull
    RoutineConfiguration.Builder<? extends ContextRoutineBuilder<INPUT, OUTPUT>>
    routineConfiguration();

    /**
     * Gets the invocation configuration builder related to this builder instance.
     * <p/>
     * Note that the builder will be initialized with the current configuration.
     *
     * @return the invocation configuration builder.
     */
    @Nonnull
    InvocationConfiguration.Builder<? extends ContextRoutineBuilder<INPUT, OUTPUT>>
    invocationConfiguration();
}
