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

package com.github.dm.jrt.android.core.routine;

import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.Nullable;

/**
 * Interface defining a routine that can clear specific invocation instances, identifying them by
 * their inputs.
 * <p>
 * Created by davide-maestroni on 03/09/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface LoaderRoutine<IN, OUT> extends Routine<IN, OUT> {

    /**
     * Makes the builder destroy the cached invocation instances with the specified input.
     *
     * @param input the input.
     */
    void clear(@Nullable IN input);

    /**
     * Makes the builder destroy the cached invocation instances with the specified inputs.
     *
     * @param inputs the inputs.
     */
    void clear(@Nullable IN... inputs);

    /**
     * Makes the builder destroy the cached invocation instances with the specified inputs.
     *
     * @param inputs the inputs.
     */
    void clear(@Nullable Iterable<? extends IN> inputs);
}
