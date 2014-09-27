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
package com.bmd.jrt.routine;

import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.execution.Execution;

/**
 * TODO
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @see com.bmd.jrt.routine.AsynMethod
 */
public class JRoutine {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutine() {

    }

    public static ClassRoutineBuilder on(final Class<?> target) {

        return new ClassRoutineBuilder(target);
    }

    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> on(
            final ClassToken<? extends Execution<INPUT, OUTPUT>> classToken) {

        return new RoutineBuilder<INPUT, OUTPUT>(classToken);
    }

    public static ObjectRoutineBuilder on(final Object target) {

        return new ObjectRoutineBuilder(target);
    }
}