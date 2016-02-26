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

package com.github.dm.jrt.android.builder;

import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.RoutineBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines bound to a context lifecycle through loaders.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will be dispatched on the configured looper thread, no matter the calling one
 * was, so that, waiting for the outputs right after the routine invocation, may result in a
 * deadlock.<br/>
 * Note that the configuration of the maximum number of concurrent invocations might not work as
 * expected, since the number of running loaders cannot be computed.<br/>
 * Note also that the input data will be cached, so be sure to avoid streaming inputs in order to
 * prevent out of memory errors.<br/>
 * The local context of the invocations will always be the application one.
 * <p/>
 * Created by davide-maestroni on 12/09/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface LoaderRoutineBuilder<IN, OUT>
        extends RoutineBuilder<IN, OUT>, LoaderConfigurableBuilder<LoaderRoutineBuilder<IN, OUT>>,
        LoaderRoutine<IN, OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderRoutine<IN, OUT> buildRoutine();

    /**
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends LoaderRoutineBuilder<IN, OUT>> withInvocations();
}
