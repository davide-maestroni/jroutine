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
import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.wtf.fll.Classification;

import java.lang.reflect.Constructor;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findConstructor;

/**
 * Created by davide on 9/9/14.
 */
class DefaultRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends Execution<INPUT, OUTPUT>> mConstructor;

    private final Class<ParallelExecution<INPUT, OUTPUT>> mParallelType =
            new Classification<ParallelExecution<INPUT, OUTPUT>>() {}.getRawType();

    public DefaultRoutine(final Runner syncRunner, final Runner asyncRunner, final int maxRunning,
            final int maxRetained, final TimeDuration availTimeout,
            final Class<? extends Execution<INPUT, OUTPUT>> invocationClass,
            final Object... invocationArgs) {

        super(syncRunner, asyncRunner, maxRunning, maxRetained, availTimeout);

        mConstructor = findConstructor(invocationClass, invocationArgs);
        mArgs = (invocationArgs == null) ? NO_ARGS : invocationArgs.clone();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launchParall() {

        final DefaultRoutine<INPUT, OUTPUT> parallelRoutine =
                new DefaultRoutine<INPUT, OUTPUT>(getSyncRunner(), getAsyncRunner(),
                                                  getMaxRunning(), getMaxRetained(),
                                                  getAvailTimeout(), mParallelType, this);

        return parallelRoutine.launchAsyn();
    }

    @Override
    protected Execution<INPUT, OUTPUT> createRoutineInvocation(final boolean async) {

        try {

            return mConstructor.newInstance(mArgs);

        } catch (final Throwable t) {

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }
}