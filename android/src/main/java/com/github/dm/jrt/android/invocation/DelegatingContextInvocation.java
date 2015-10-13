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

import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.util.Reflection.asArgs;

/**
 * Function invocation implementation delegating the execution to another routine.
 * <p/>
 * Created by davide-maestroni on 10/07/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class DelegatingContextInvocation<IN, OUT> extends FunctionContextInvocation<IN, OUT> {

    private final DelegationType mDelegationType;

    private final Routine<IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine    the routine used to execute this invocation.
     * @param delegation the type of routine invocation.
     */
    @SuppressWarnings("ConstantConditions")
    public DelegatingContextInvocation(@NotNull final Routine<IN, OUT> routine,
            @NotNull final DelegationType delegation) {

        if (routine == null) {

            throw new NullPointerException("the routine must not be null");
        }

        if (delegation == null) {

            throw new NullPointerException("the delegation type must not be null");
        }

        mRoutine = routine;
        mDelegationType = delegation;
    }

    /**
     * Returns a factory of delegating invocations.<br/>
     * Note that the specified identifier will be used to detect clashing of invocations (see
     * {@link com.github.dm.jrt.android.v11.core.JRoutine} and
     * {@link com.github.dm.jrt.android.v4.core.JRoutine}).
     *
     * @param routine    the routine used to execute this invocation.
     * @param routineId  the routine identifier.
     * @param delegation the type of routine invocation.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, final int routineId,
            @NotNull final DelegationType delegation) {

        return new DelegatingFunctionContextInvocationFactory<IN, OUT>(routine, routineId,
                                                                       delegation);
    }

    public void onContext(@NotNull final Context context) {

    }

    @Override
    protected void onCall(@NotNull final List<? extends IN> inputs,
            @NotNull final ResultChannel<OUT> result) {

        final DelegationType delegationType = mDelegationType;
        final OutputChannel<OUT> channel =
                (delegationType == DelegationType.SYNC) ? mRoutine.syncCall(inputs)
                        : (delegationType == DelegationType.ASYNC) ? mRoutine.asyncCall(inputs)
                                : mRoutine.parallelCall(inputs);
        result.pass(channel);
    }

    /**
     * Factory creating delegating context invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DelegatingFunctionContextInvocationFactory<IN, OUT>
            extends FunctionContextInvocationFactory<IN, OUT> {

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
        private DelegatingFunctionContextInvocationFactory(@NotNull final Routine<IN, OUT> routine,
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
        public FunctionContextInvocation<IN, OUT> newInvocation() {

            return new DelegatingContextInvocation<IN, OUT>(mRoutine, mDelegationType);
        }
    }
}
