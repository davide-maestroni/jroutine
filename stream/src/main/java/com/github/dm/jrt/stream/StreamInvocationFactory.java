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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Implementation of a factory creating invocations wrapping a stream output channel.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class StreamInvocationFactory<IN, OUT> extends ComparableInvocationFactory<IN, OUT> {

    private final FunctionWrapper<? super StreamChannel<IN>, ? extends
            StreamChannel<? extends OUT>> mFunction;

    /**
     * Constructor.
     *
     * @param function the function used to instantiate the stream output channel.
     */
    @SuppressWarnings("ConstantConditions")
    StreamInvocationFactory(@NotNull final FunctionWrapper<? super StreamChannel<IN>, ? extends
            StreamChannel<? extends OUT>> function) {

        super(asArgs(function));
        if (function == null) {
            throw new NullPointerException("the function instance must not be null");
        }

        mFunction = function;
    }

    @NotNull
    @Override
    public Invocation<IN, OUT> newInvocation() {

        return new StreamInvocation<IN, OUT>(mFunction);
    }

    /**
     * Implementation of an invocation wrapping a stream output channel.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocation<IN, OUT> implements Invocation<IN, OUT> {

        private final Function<? super StreamChannel<IN>, ? extends
                StreamChannel<? extends OUT>> mFunction;

        private IOChannel<IN> mInputChannel;

        private StreamChannel<? extends OUT> mOutputChannel;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream output channel.
         */
        private StreamInvocation(@NotNull final Function<? super StreamChannel<IN>, ? extends
                StreamChannel<? extends OUT>> function) {

            mFunction = function;
        }

        public void onAbort(@NotNull final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

        }

        public void onInitialize() {

            final IOChannel<IN> ioChannel = JRoutineCore.io().buildChannel();
            mOutputChannel = mFunction.apply(Streams.streamOf(ioChannel));
            mInputChannel = ioChannel;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            final StreamChannel<? extends OUT> outputChannel = mOutputChannel;
            if (!outputChannel.isBound()) {
                outputChannel.bind(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            final StreamChannel<? extends OUT> outputChannel = mOutputChannel;
            if (!outputChannel.isBound()) {
                outputChannel.bind(result);
            }

            mInputChannel.close();
        }

        public void onTerminate() {

            mInputChannel = null;
            mOutputChannel = null;
        }
    }
}