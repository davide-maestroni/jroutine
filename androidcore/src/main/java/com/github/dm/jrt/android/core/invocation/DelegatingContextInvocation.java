/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.core.invocation;

import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Call invocation implementation delegating the execution to another routine.
 * <p/>
 * Created by davide-maestroni on 10/07/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class DelegatingContextInvocation<IN, OUT> extends TemplateContextInvocation<IN, OUT> {

    private final DelegationType mDelegationType;

    private final Routine<IN, OUT> mRoutine;

    private InvocationChannel<IN, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param routine    the routine used to execute this invocation.
     * @param delegation the type of routine invocation.
     */
    private DelegatingContextInvocation(@NotNull final Routine<IN, OUT> routine,
            @NotNull final DelegationType delegation) {

        mRoutine = routine;
        mDelegationType = delegation;
    }

    /**
     * Returns a factory of delegating invocations.
     * <p/>
     * Note that the specified identifier will be used to detect clashing of invocations (see
     * {@link com.github.dm.jrt.android.v11.core.JRoutineLoader} and
     * {@link com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat}).
     *
     * @param routine    the routine used to execute this invocation.
     * @param routineId  the routine identifier.
     * @param delegation the type of routine invocation.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, final int routineId,
            @NotNull final DelegationType delegation) {

        return new DelegatingContextInvocationFactory<IN, OUT>(routine, routineId, delegation);
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {

        mChannel.abort(reason);
    }

    @Override
    public void onInitialize() throws Exception {

        final DelegationType delegationType = mDelegationType;
        mChannel = (delegationType == DelegationType.ASYNC) ? mRoutine.asyncInvoke()
                : (delegationType == DelegationType.PARALLEL) ? mRoutine.parallelInvoke()
                        : mRoutine.syncInvoke();
    }

    @Override
    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws Exception {

        mChannel.pass(input);
    }

    @Override
    public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

        result.pass(mChannel.result());
    }

    @Override
    public void onTerminate() throws Exception {

        mChannel = null;
    }

    /**
     * Factory creating delegating context invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DelegatingContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final DelegationType mDelegationType;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine    the delegated routine.
         * @param routineId  the routine identifier.
         * @param delegation the type of routine invocation.
         */
        @SuppressWarnings("ConstantConditions")
        private DelegatingContextInvocationFactory(@NotNull final Routine<IN, OUT> routine,
                final int routineId, @NotNull final DelegationType delegation) {

            super(asArgs(routineId, delegation));
            if (routine == null) {
                throw new NullPointerException("the routine must not be null");
            }

            if (delegation == null) {
                throw new NullPointerException("the invocation type must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegation;
        }

        @NotNull
        public ContextInvocation<IN, OUT> newInvocation() {

            return new DelegatingContextInvocation<IN, OUT>(mRoutine, mDelegationType);
        }
    }
}