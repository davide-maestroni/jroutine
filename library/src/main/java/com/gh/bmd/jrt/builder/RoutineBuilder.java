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

import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routine objects.
 * <p/>
 * Note that when the invocation is started directly from the builder, a new routine instance is
 * implicitly created.
 * <p/>
 * Created by davide on 11/11/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface RoutineBuilder<INPUT, OUTPUT>
        extends ConfigurableBuilder<RoutineBuilder<INPUT, OUTPUT>>, Routine<INPUT, OUTPUT> {

    /**
     * Builds and returns the routine.
     *
     * @return the newly created routine instance.
     */
    @Nonnull
    Routine<INPUT, OUTPUT> buildRoutine();
}
