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

import com.bmd.jrt.process.Processor;
import com.bmd.jrt.process.ResultPublisher;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by davide on 9/9/14.
 */
class ProcessorRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<?> mConstructor;

    private final int mInstanceCount;

    private final Object mMutex = new Object();

    private LinkedList<Processor<INPUT, OUTPUT>> mInstances =
            new LinkedList<Processor<INPUT, OUTPUT>>();

    public ProcessorRoutine(final Runner runner, final int instanceCount,
            final Class<? extends Processor<INPUT, OUTPUT>> processorClass, final Object... args) {

        super(runner);

        mInstanceCount = instanceCount;

        Constructor<?> bestMatch =
                ReflectionUtils.matchingConstructor(processorClass.getConstructors(), args);

        if (bestMatch == null) {

            bestMatch =
                    ReflectionUtils.matchingConstructor(processorClass.getDeclaredConstructors(),
                                                        args);

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + processorClass.getSimpleName());
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        mConstructor = bestMatch;
        mArgs = (args == null) ? NO_ARGS : args.clone();
    }

    @Override
    protected RecyclableUnitProcessor<INPUT, OUTPUT> createProcessor() {

        synchronized (mMutex) {

            try {

                final Processor<INPUT, OUTPUT> processor;

                if (mInstances.isEmpty()) {

                    //noinspection unchecked
                    processor = (Processor<INPUT, OUTPUT>) mConstructor.newInstance(mArgs);

                } else {

                    processor = mInstances.removeFirst();
                }

                return new ProcessorRoutineProcessor(processor);

            } catch (final Throwable t) {

                throw RoutineExceptionWrapper.wrap(t).raise();
            }
        }
    }

    private class ProcessorRoutineProcessor implements RecyclableUnitProcessor<INPUT, OUTPUT> {

        private final Processor<INPUT, OUTPUT> mProcessor;

        private ArrayList<INPUT> mInputs;

        public ProcessorRoutineProcessor(final Processor<INPUT, OUTPUT> processor) {

            mProcessor = processor;
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

            if (mInputs != null) {

                mInputs.clear();
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

            mProcessor.onExecute(copy, results);
        }

        @Override
        public void recycle() {

            synchronized (mMutex) {

                if (mInstances.size() < mInstanceCount) {

                    mInstances.add(mProcessor);
                }
            }
        }
    }
}