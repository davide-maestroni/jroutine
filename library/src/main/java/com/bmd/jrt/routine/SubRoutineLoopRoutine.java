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

import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.subroutine.SubRoutineLoop;

import java.lang.reflect.Constructor;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findMatchingConstructor;

/**
 * Created by davide on 9/9/14.
 */
class SubRoutineLoopRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends SubRoutineLoop<INPUT, OUTPUT>> mConstructor;

    public SubRoutineLoopRoutine(final Runner runner, final int maxParalle, final int maxRetained,
            final Class<? extends SubRoutineLoop<INPUT, OUTPUT>> type, final Object... ctorArgs) {

        super(runner, maxParalle, maxRetained);

        mConstructor = findMatchingConstructor(type, ctorArgs);
        mArgs = (ctorArgs == null) ? NO_ARGS : ctorArgs.clone();
    }

    @Override
    protected SubRoutineLoop<INPUT, OUTPUT> createSubRoutine(final boolean async) {

        try {

            return mConstructor.newInstance(mArgs);

        } catch (final Throwable t) {

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }
}