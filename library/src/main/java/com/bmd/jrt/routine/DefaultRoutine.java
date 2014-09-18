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

import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.wtf.fll.Classification;

import java.lang.reflect.Constructor;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findConstructor;

/**
 * Created by davide on 9/9/14.
 */
class DefaultRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends SubRoutine<INPUT, OUTPUT>> mConstructor;

    private final Class<ParallelSubRoutine<INPUT, OUTPUT>> mParallelType =
            new Classification<ParallelSubRoutine<INPUT, OUTPUT>>() {}.getRawType();

    public DefaultRoutine(final Runner runner, final int maxRetained,
            final Class<? extends SubRoutine<INPUT, OUTPUT>> type, final Object... ctorArgs) {

        super(runner, maxRetained);

        mConstructor = findConstructor(type, ctorArgs);
        mArgs = (ctorArgs == null) ? NO_ARGS : ctorArgs.clone();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launchPar() {

        final DefaultRoutine<INPUT, OUTPUT> parallelRoutine =
                new DefaultRoutine<INPUT, OUTPUT>(getRunner(), getMaxRetained(), mParallelType,
                                                  this);

        return parallelRoutine.launchAsyn();
    }

    @Override
    protected SubRoutine<INPUT, OUTPUT> createSubRoutine(final boolean async) {

        try {

            return mConstructor.newInstance(mArgs);

        } catch (final Throwable t) {

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }
}