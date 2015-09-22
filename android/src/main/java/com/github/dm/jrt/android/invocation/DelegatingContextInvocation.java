/*
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
package com.github.dm.jrt.android.invocation;

import android.content.Context;

import com.github.dm.jrt.invocation.DelegatingInvocation;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

/**
 * Invocation implementation delegating the execution to another routine.
 * <p/>
 * Created by davide-maestroni on 04/19/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class DelegatingContextInvocation<IN, OUT> extends DelegatingInvocation<IN, OUT>
        implements ContextInvocation<IN, OUT> {

    /**
     * Constructor.
     *
     * @param routine        the routine used to execute this invocation.
     * @param delegationType the type of routine invocation.
     */
    public DelegatingContextInvocation(@NotNull final Routine<IN, OUT> routine,
            @NotNull final DelegationType delegationType) {

        super(routine, delegationType);
    }

    /**
     * Returns a factory of delegating invocations.<br/>
     * Note that the specified identifier will be used to detect clashing of invocations (see
     * {@link com.github.dm.jrt.android.v11.core.JRoutine} and
     * {@link com.github.dm.jrt.android.v4.core.JRoutine}).
     *
     * @param routine        the routine used to execute this invocation.
     * @param routineId      the routine identifier.
     * @param delegationType the type of routine invocation.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, final int routineId,
            @NotNull final DelegationType delegationType) {

        return new DelegatingContextInvocationFactory<IN, OUT>(routine, routineId, delegationType);
    }

    public void onContext(@NotNull final Context context) {

    }

    /**
     * Factory creating delegating context invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DelegatingContextInvocationFactory<IN, OUT>
            extends AbstractContextInvocationFactory<IN, OUT> {

        private final DelegationType mDelegationType;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the delegated routine.
         * @param routineId      the routine identifier.
         * @param delegationType the type of routine invocation.
         */
        @SuppressWarnings("ConstantConditions")
        private DelegatingContextInvocationFactory(@NotNull final Routine<IN, OUT> routine,
                final int routineId, @NotNull final DelegationType delegationType) {

            super(routineId, delegationType);

            if (routine == null) {

                throw new NullPointerException("the routine must not be null");
            }

            if (delegationType == null) {

                throw new NullPointerException("the invocation type must not be null");
            }

            mRoutine = routine;
            mDelegationType = delegationType;
        }

        @NotNull
        public ContextInvocation<IN, OUT> newInvocation() {

            return new DelegatingContextInvocation<IN, OUT>(mRoutine, mDelegationType);
        }
    }
}
