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
package com.github.dm.jrt.functional;

import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class extending the base one in order to support functional building of a routine.
 * <p/>
 * Created by davide-maestroni on 10/22/2015.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class JRoutine extends com.github.dm.jrt.core.JRoutine {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutine() {

    }

    /**
     * Returns a functional routine builder.
     *
     * @return the routine builder instance.
     */
    @NotNull
    public static FunctionalRoutineBuilder functional() {

        return new DefaultFunctionalRoutineBuilder();
    }
}
