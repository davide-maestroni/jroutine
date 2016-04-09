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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation implementation delegating the execution to another routine.
 * <p>
 * Created by davide-maestroni on 04/18/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class DelegatingInvocation<IN, OUT> implements Invocation<IN, OUT> {

    private final DelegationType mDelegationType;

    private final Routine<IN, OUT> mRoutine;

    private IOChannel<IN> mInputChannel;

    private OutputChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param routine        the routine used to execute this invocation.
     * @param delegationType the type of routine invocation.
     */
    private DelegatingInvocation(@NotNull final Routine<IN, OUT> routine,
            @NotNull final DelegationType delegationType) {

        mRoutine = routine;
        mDelegationType = delegationType;
        mInputChannel = null;
        mOutputChannel = null;
    }

    /**
     * Returns a factory of delegating invocations.
     *
     * @param routine        the routine used to execute this invocation.
     * @param delegationType the type of routine invocation.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, @NotNull final DelegationType delegationType) {

        return new DelegatingInvocationFactory<IN, OUT>(routine, delegationType);
    }

    public void onAbort(@NotNull final RoutineException reason) {

        mInputChannel.abort(reason);
    }

    public void onDestroy() {

        mRoutine.purge();
    }

    public void onInitialize() {

        final DelegationType delegationType = mDelegationType;
        final IOChannel<IN> inputChannel = JRoutineCore.io().buildChannel();
        mInputChannel = inputChannel;
        mOutputChannel = (delegationType == DelegationType.ASYNC) ? mRoutine.asyncCall(inputChannel)
                : (delegationType == DelegationType.PARALLEL) ? mRoutine.parallelCall(inputChannel)
                        : (delegationType == DelegationType.SYNC) ? mRoutine.syncCall(inputChannel)
                                : mRoutine.serialCall(inputChannel);
    }

    public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

        final OutputChannel<OUT> outputChannel = mOutputChannel;
        if (!outputChannel.isBound()) {
            outputChannel.bind(result);
        }

        mInputChannel.pass(input);
    }

    public void onResult(@NotNull final ResultChannel<OUT> result) {

        final OutputChannel<OUT> outputChannel = mOutputChannel;
        if (!outputChannel.isBound()) {
            outputChannel.bind(result);
        }

        mInputChannel.close();
    }

    public void onTerminate() {

        mInputChannel = null;
        mOutputChannel = null;
    }

    /**
     * Delegation type enumeration.
     */
    public enum DelegationType {

        /**
         * The delegated routine is invoked in synchronous mode.
         *
         * @see com.github.dm.jrt.core.routine.Routine Routine
         */
        SYNC,
        /**
         * The delegated routine is invoked in asynchronous mode.
         *
         * @see com.github.dm.jrt.core.routine.Routine Routine
         */
        ASYNC,
        /**
         * The delegated routine is invoked in parallel mode.
         *
         * @see com.github.dm.jrt.core.routine.Routine Routine
         */
        PARALLEL,
        /**
         * The delegated routine is invoked in serial mode.
         *
         * @see com.github.dm.jrt.core.routine.Routine Routine
         */
        SERIAL
    }

    /**
     * Factory creating delegating invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DelegatingInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final DelegationType mDelegationType;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the delegated routine.
         * @param delegationType the type of routine invocation.
         */
        private DelegatingInvocationFactory(@NotNull final Routine<IN, OUT> routine,
                @NotNull final DelegationType delegationType) {

            super(asArgs(routine, delegationType));
            mRoutine = ConstantConditions.notNull("routine instance", routine);
            mDelegationType = ConstantConditions.notNull("delegation type", delegationType);
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new DelegatingInvocation<IN, OUT>(mRoutine, mDelegationType);
        }
    }
}
