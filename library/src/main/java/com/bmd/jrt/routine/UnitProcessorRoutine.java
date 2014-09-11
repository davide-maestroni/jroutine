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

import com.bmd.jrt.process.ResultPublisher;
import com.bmd.jrt.process.UnitProcessor;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

import static com.bmd.jrt.routine.ReflectionUtils.matchingConstructor;

/**
 * Created by davide on 9/9/14.
 */
class UnitProcessorRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<?> mConstructor;

    private final int mInstanceCount;

    private final Object mMutex = new Object();

    private LinkedList<UnitProcessor<INPUT, OUTPUT>> mInstances =
            new LinkedList<UnitProcessor<INPUT, OUTPUT>>();

    public UnitProcessorRoutine(final Runner runner, final int instanceCount,
            final Class<? extends UnitProcessor<INPUT, OUTPUT>> processorClass,
            final Object... args) {

        super(runner);

        mInstanceCount = instanceCount;

        Constructor<?> bestMatch = matchingConstructor(processorClass.getConstructors(), args);

        if (bestMatch == null) {

            bestMatch = matchingConstructor(processorClass.getDeclaredConstructors(), args);

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

                final LinkedList<UnitProcessor<INPUT, OUTPUT>> instances = mInstances;
                final UnitProcessor<INPUT, OUTPUT> unitProcessor;

                if (instances.isEmpty()) {

                    //noinspection unchecked
                    unitProcessor = (UnitProcessor<INPUT, OUTPUT>) mConstructor.newInstance(mArgs);

                } else {

                    unitProcessor = instances.removeFirst();
                }

                return new RecyclableRoutineProcessor(unitProcessor);

            } catch (final Throwable t) {

                throw RoutineExceptionWrapper.wrap(t).raise();
            }
        }
    }

    private class RecyclableRoutineProcessor implements RecyclableUnitProcessor<INPUT, OUTPUT> {

        private final UnitProcessor<INPUT, OUTPUT> mUnitProcessor;

        public RecyclableRoutineProcessor(final UnitProcessor<INPUT, OUTPUT> unitProcessor) {

            mUnitProcessor = unitProcessor;
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

            mUnitProcessor.onInput(input, results);
        }

        @Override
        public void onReset(final ResultPublisher<OUTPUT> results) {

            mUnitProcessor.onReset(results);
        }

        @Override
        public void onResult(final ResultPublisher<OUTPUT> results) {

            mUnitProcessor.onResult(results);
        }

        @Override
        public void recycle() {

            synchronized (mMutex) {

                final LinkedList<UnitProcessor<INPUT, OUTPUT>> instances = mInstances;

                if (instances.size() < mInstanceCount) {

                    instances.add(mUnitProcessor);
                }
            }
        }
    }
}