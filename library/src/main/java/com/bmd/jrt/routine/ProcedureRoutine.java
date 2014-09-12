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

import com.bmd.jrt.procedure.LoopProcedure;
import com.bmd.jrt.procedure.Procedure;
import com.bmd.jrt.procedure.ResultPublisher;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findMatchingConstructor;

/**
 * Created by davide on 9/9/14.
 */
class ProcedureRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends Procedure<INPUT, OUTPUT>> mConstructor;

    public ProcedureRoutine(final Runner runner, final int maxRecycle, final int maxRetain,
            final Class<? extends Procedure<INPUT, OUTPUT>> procedureClass,
            final Object... ctorArgs) {

        super(runner, maxRecycle, maxRetain);

        mConstructor = findMatchingConstructor(procedureClass, ctorArgs);
        mArgs = (ctorArgs == null) ? NO_ARGS : ctorArgs.clone();
    }

    @Override
    protected LoopProcedure<INPUT, OUTPUT> createProcedure(final boolean async) {

        try {
            return new LoopProcedureWrapper(mConstructor.newInstance(mArgs));

        } catch (final Throwable t) {

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }

    private class LoopProcedureWrapper implements LoopProcedure<INPUT, OUTPUT> {

        private final Procedure<INPUT, OUTPUT> mProcedure;

        private ArrayList<INPUT> mInputs;

        public LoopProcedureWrapper(final Procedure<INPUT, OUTPUT> procedure) {

            mProcedure = procedure;
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

            mProcedure.onRun(copy, results);
        }
    }
}