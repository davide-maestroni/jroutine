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

import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Constructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findConstructor;

/**
 * Default implementation of a routine object instantiating execution objects via reflection.
 * <p/>
 * Created by davide on 9/9/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
class DefaultRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends Execution<INPUT, OUTPUT>> mConstructor;

    private final Logger mLogger;

    /**
     * Constructor.
     *
     * @param syncRunner     the runner used for synchronous invocation.
     * @param asyncRunner    the runner used for asynchronous invocation.
     * @param maxRunning     the maximum number of parallel running executions. Must be positive.
     * @param maxRetained    the maximum number of retained execution instances. Must be 0 or a
     *                       positive number.
     * @param availTimeout   the maximum timeout while waiting for an execution instance to be
     *                       available.
     * @param orderedInput   whether the input data are forced to be delivered in insertion order.
     * @param orderedOutput  whether the output data are forced to be delivered in insertion order.
     * @param log            the log instance.
     * @param logLevel       the log level
     * @param executionClass the execution class.
     * @param executionArgs  the execution constructor arguments.
     * @throws NullPointerException if at least one of the parameter is null   @throws IllegalArgumentException if at least one of the parameter is invalid, of no
     *                              constructor matching the specified arguments is found for
     *                              the target execution class.
     */
    DefaultRoutine(@NonNull final Runner syncRunner, @NonNull final Runner asyncRunner,
            final int maxRunning, final int maxRetained, @NonNull final TimeDuration availTimeout,
            final boolean orderedInput, final boolean orderedOutput, @NonNull final Log log,
            @NonNull final LogLevel logLevel,
            @NonNull final Class<? extends Execution<INPUT, OUTPUT>> executionClass,
            @Nullable final Object... executionArgs) {

        super(syncRunner, asyncRunner, maxRunning, maxRetained, availTimeout, orderedInput,
              orderedOutput, log, logLevel);

        mArgs = (executionArgs == null) ? NO_ARGS : executionArgs.clone();
        mConstructor = findConstructor(executionClass, mArgs);
        mLogger = Logger.create(log, logLevel, this);
    }

    @Override
    @NonNull
    protected Execution<INPUT, OUTPUT> createExecution(final boolean async) {

        final Logger logger = mLogger;

        try {

            final Constructor<? extends Execution<INPUT, OUTPUT>> constructor = mConstructor;

            logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());

            return constructor.newInstance(mArgs);

        } catch (final Throwable t) {

            logger.err(t, "error creating the execution instance");

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }
}