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

import com.gh.bmd.jrt.android.routine.LoaderRoutine;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines bound to a context lifecycle through loaders.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured looper thread, no matter the calling one
 * was, so that, waiting for the outputs right after the routine invocation, may result in a
 * deadlock.<br/>
 * The local context of the invocations will always be the application one.
 * <p/>
 * Created by davide-maestroni on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface LoaderRoutineBuilder<INPUT, OUTPUT> extends RoutineBuilder<INPUT, OUTPUT>,
        LoaderConfigurableBuilder<LoaderRoutineBuilder<INPUT, OUTPUT>>,
        LoaderRoutine<INPUT, OUTPUT> {

    //TODO: kill the loader, cache rewrite, @Priority

    /**
     * {@inheritDoc}
     */
    @Nonnull
    LoaderRoutine<INPUT, OUTPUT> buildRoutine();

    /**
     * Note that all the options related to the output and input channels size and timeout will be
     * ignored.
     *
     * @return the routine configuration builder.
     */
    @Nonnull
    Builder<? extends LoaderRoutineBuilder<INPUT, OUTPUT>> withRoutine();
}
