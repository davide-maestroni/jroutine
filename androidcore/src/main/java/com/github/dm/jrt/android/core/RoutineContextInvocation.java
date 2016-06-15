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

package com.github.dm.jrt.android.core;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.ContextInvocationWrapper;
import com.github.dm.jrt.core.RoutineInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Context invocation implementation delegating the execution to another routine.
 * <p>
 * Created by davide-maestroni on 10/07/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class RoutineContextInvocation<IN, OUT> extends ContextInvocationWrapper<IN, OUT> {

    /**
     * Constructor.
     *
     * @param invocation the wrapped invocation.
     */
    private RoutineContextInvocation(@NotNull final Invocation<IN, OUT> invocation) {
        super(invocation);
    }

    /**
     * Returns a factory of delegating invocations.
     * <p>
     * Note that the specified identifier will be used to detect clashing of invocations (see
     * {@link com.github.dm.jrt.android.v11.core.JRoutineLoader} and
     * {@link com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat}).
     *
     * @param routine        the routine used to execute this invocation.
     * @param routineId      the routine identifier.
     * @param invocationMode the type of routine invocation.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, final int routineId,
            @NotNull final InvocationMode invocationMode) {
        return new DelegatingContextInvocationFactory<IN, OUT>(routine, routineId, invocationMode);
    }

    /**
     * Factory creating delegating context invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DelegatingContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param routine        the delegated routine.
         * @param routineId      the routine identifier.
         * @param invocationMode the type of routine invocation.
         */
        private DelegatingContextInvocationFactory(@NotNull final Routine<IN, OUT> routine,
                final int routineId, @NotNull final InvocationMode invocationMode) {
            super(asArgs(routineId, invocationMode));
            mFactory = RoutineInvocation.factoryFrom(routine, invocationMode);
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {
            return new RoutineContextInvocation<IN, OUT>(mFactory.newInvocation());
        }
    }
}
