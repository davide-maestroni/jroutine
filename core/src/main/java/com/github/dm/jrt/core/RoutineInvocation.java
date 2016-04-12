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
import com.github.dm.jrt.core.routine.InvocationMode;
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
public class RoutineInvocation<IN, OUT> implements Invocation<IN, OUT> {

    private final InvocationMode mInvocationMode;

    private final Routine<IN, OUT> mRoutine;

    private IOChannel<IN> mInputChannel;

    private OutputChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param routine        the routine used to execute this invocation.
     * @param invocationMode the type of routine invocation.
     */
    private RoutineInvocation(@NotNull final Routine<IN, OUT> routine,
            @NotNull final InvocationMode invocationMode) {

        mRoutine = routine;
        mInvocationMode = invocationMode;
        mInputChannel = null;
        mOutputChannel = null;
    }

    /**
     * Returns a factory of delegating invocations.
     *
     * @param routine        the routine used to execute this invocation.
     * @param invocationMode the type of routine invocation.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Routine<IN, OUT> routine, @NotNull final InvocationMode invocationMode) {

        return new RoutineInvocationFactory<IN, OUT>(routine, invocationMode);
    }

    public void onAbort(@NotNull final RoutineException reason) {

        mInputChannel.abort(reason);
    }

    public void onDestroy() {

        mRoutine.purge();
    }

    public void onInitialize() {

        final InvocationMode invocationMode = mInvocationMode;
        final IOChannel<IN> inputChannel = JRoutineCore.io().buildChannel();
        mInputChannel = inputChannel;
        mOutputChannel = (invocationMode == InvocationMode.ASYNC) ? mRoutine.asyncCall(inputChannel)
                : (invocationMode == InvocationMode.PARALLEL) ? mRoutine.parallelCall(inputChannel)
                        : (invocationMode == InvocationMode.SYNC) ? mRoutine.syncCall(inputChannel)
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
     * Factory creating delegating invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class RoutineInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final InvocationMode mInvocationMode;

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine        the delegated routine.
         * @param invocationMode the type of routine invocation.
         */
        private RoutineInvocationFactory(@NotNull final Routine<IN, OUT> routine,
                @NotNull final InvocationMode invocationMode) {

            super(asArgs(routine, invocationMode));
            mRoutine = ConstantConditions.notNull("routine instance", routine);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new RoutineInvocation<IN, OUT>(mRoutine, mInvocationMode);
        }
    }
}
