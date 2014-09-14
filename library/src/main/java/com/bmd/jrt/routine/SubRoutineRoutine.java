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
import com.bmd.jrt.subroutine.ResultPublisher;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.subroutine.SubRoutineLoop;
import com.bmd.jrt.subroutine.SubRoutineLoopAdapter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findMatchingConstructor;

/**
 * Created by davide on 9/9/14.
 */
class SubRoutineRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends SubRoutine<INPUT, OUTPUT>> mConstructor;

    public SubRoutineRoutine(final Runner runner, final int maxParallel, final int maxRetained,
            final Class<? extends SubRoutine<INPUT, OUTPUT>> type, final Object... ctorArgs) {

        super(runner, maxParallel, maxRetained);

        mConstructor = findMatchingConstructor(type, ctorArgs);
        mArgs = (ctorArgs == null) ? NO_ARGS : ctorArgs.clone();
    }

    @Override
    protected SubRoutineLoop<INPUT, OUTPUT> createSubRoutine(final boolean async) {

        try {

            return new SubRoutineLoopWrapper(mConstructor.newInstance(mArgs));

        } catch (final Throwable t) {

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }

    private class SubRoutineLoopWrapper extends SubRoutineLoopAdapter<INPUT, OUTPUT> {

        private final SubRoutine<INPUT, OUTPUT> mSubRoutine;

        private ArrayList<INPUT> mInputs;

        public SubRoutineLoopWrapper(final SubRoutine<INPUT, OUTPUT> subRoutine) {

            mSubRoutine = subRoutine;
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

            if (mInputs == null) {

                mInputs = new ArrayList<INPUT>();
            }

            mInputs.add(input);
        }

        @Override
        public void onReset(final ResultPublisher<OUTPUT> results) {

            final ArrayList<INPUT> inputs = mInputs;

            if (inputs != null) {

                inputs.clear();
            }
        }

        @Override
        public void onResult(final ResultPublisher<OUTPUT> results) {

            final ArrayList<INPUT> inputs = mInputs;
            final ArrayList<INPUT> copy;

            if (inputs == null) {

                copy = new ArrayList<INPUT>(0);

            } else {

                copy = new ArrayList<INPUT>(inputs);
                inputs.clear();
            }

            mSubRoutine.run(copy, results);
        }
    }
}