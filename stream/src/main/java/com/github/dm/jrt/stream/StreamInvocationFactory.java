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

import com.github.dm.jrt.core.ChannelInvocation;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Implementation of a factory creating invocations wrapping a stream channel.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class StreamInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final FunctionDecorator<? super StreamChannel<IN, IN>, ? extends StreamChannel<? super
            IN, ? extends OUT>> mFunction;

    /**
     * Constructor.
     *
     * @param function the function used to instantiate the stream channel.
     */
    StreamInvocationFactory(
            @NotNull final FunctionDecorator<? super StreamChannel<IN, IN>, ? extends
                    StreamChannel<? super IN, ? extends OUT>> function) {
        super(asArgs(ConstantConditions.notNull("function instance", function)));
        mFunction = function;
    }

    @NotNull
    @Override
    public Invocation<IN, OUT> newInvocation() {
        return new FunctionStreamInvocation<IN, OUT>(mFunction);
    }

    /**
     * Implementation of an invocation wrapping a stream channel.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionStreamInvocation<IN, OUT> extends ChannelInvocation<IN, OUT> {

        private final Function<? super StreamChannel<IN, IN>, ? extends StreamChannel<? super IN,
                ? extends OUT>>
                mFunction;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream channel.
         */
        private FunctionStreamInvocation(
                @NotNull final Function<? super StreamChannel<IN, IN>, ? extends
                        StreamChannel<? super IN, ? extends OUT>> function) {
            mFunction = function;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected Channel<?, OUT> onChannel(@NotNull final Channel<?, IN> channel) throws
                Exception {
            return (Channel<?, OUT>) mFunction.apply(new DefaultStreamChannel<IN, IN>(channel));
        }
    }
}
