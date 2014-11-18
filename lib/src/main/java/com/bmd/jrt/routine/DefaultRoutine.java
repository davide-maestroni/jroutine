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

import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.routine.ReflectionUtils.findConstructor;

/**
 * Default implementation of a routine object instantiating invocation objects via reflection.
 * <p/>
 * Created by davide on 9/9/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
class DefaultRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final Object[] mArgs;

    private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

    private final Logger mLogger;

    /**
     * Constructor.
     *
     * @param configuration   the routine configuration.
     * @param syncRunner      the runner used for synchronous invocation.
     * @param invocationClass the invocation class.
     * @param invocationArgs  the invocation constructor arguments.
     * @throws NullPointerException     if at least one of the parameter is null.
     * @throws IllegalArgumentException if at least one of the parameter is invalid, of no
     *                                  constructor matching the specified arguments is found for
     *                                  the target invocation class.
     */
    DefaultRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final Runner syncRunner,
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object... invocationArgs) {

        super(configuration, syncRunner);

        mArgs = (invocationArgs == null) ? NO_ARGS : (Object[]) invocationArgs.clone();
        mConstructor = findConstructor(invocationClass, mArgs);
        mLogger = Logger.create(configuration.getLog(null), configuration.getLogLevel(null), this);
    }

    @Nonnull
    @Override
    protected Invocation<INPUT, OUTPUT> createInvocation(final boolean async) {

        final Logger logger = mLogger;

        try {

            final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor = mConstructor;

            logger.dbg("creating a new instance of class: %s", constructor.getDeclaringClass());

            return constructor.newInstance(mArgs);

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");

            throw RoutineExceptionWrapper.wrap(t).raise();
        }
    }
}
