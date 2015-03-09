/**
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
package com.gh.bmd.jrt.android.routine;

import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nullable;

/**
 * Interface defining a routine that can purge specific invocation instances, identifying them by
 * their inputs.
 * <p/>
 * Created by davide on 3/9/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface AndroidRoutine<INPUT, OUTPUT> extends Routine<INPUT, OUTPUT> {

    /**
     * Makes the builder destroy the cached invocation instance with the specified input.
     *
     * @param input the input.
     */
    public void purge(@Nullable INPUT input);

    /**
     * Makes the builder destroy the cached invocation instance with the specified inputs.
     *
     * @param inputs the inputs.
     */
    public void purge(@Nullable INPUT... inputs);

    /**
     * Makes the builder destroy the cached invocation instance with the specified inputs.
     *
     * @param inputs the inputs.
     */
    public void purge(@Nullable Iterable<? extends INPUT> inputs);
}
