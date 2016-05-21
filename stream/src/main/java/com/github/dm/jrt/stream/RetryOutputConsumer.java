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
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Retry output consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class RetryOutputConsumer<IN, OUT> implements Execution, OutputConsumer<OUT> {

    private final Function<OutputChannel<IN>, OutputChannel<OUT>> mBind;

    private final BiFunction<? super Integer, ? super RoutineException, ? extends Long> mFunction;

    private final OutputChannel<IN> mInputChannel;

    private final IOChannel<OUT> mOutputChannel;

    private final ArrayList<OUT> mOutputs = new ArrayList<OUT>();

    private final Runner mRunner;

    private int mCount;

    /**
     * Constructor.
     *
     * @param inputChannel  the input channel.
     * @param outputChannel the output channel.
     * @param runner        the runner instance.
     * @param bindFunction  the binding function.
     * @param function      the backoff function.
     */
    RetryOutputConsumer(@NotNull final OutputChannel<IN> inputChannel,
            @NotNull final IOChannel<OUT> outputChannel, @NotNull final Runner runner,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> bindFunction,
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    function) {

        mInputChannel = ConstantConditions.notNull("input channel instance", inputChannel);
        mOutputChannel = ConstantConditions.notNull("output channel instance", outputChannel);
        mRunner = ConstantConditions.notNull("runner instance", runner);
        mBind = ConstantConditions.notNull("binding function", bindFunction);
        mFunction = ConstantConditions.notNull("backoff function", function);
    }

    public void onComplete() {

        mOutputChannel.pass(mOutputs).close();
    }

    public void run() {

        final IOChannel<IN> channel = JRoutineCore.io().buildChannel();
        mInputChannel.bind(new SafeOutputConsumer<IN>(channel));
        try {
            mBind.apply(channel).bind(this);

        } catch (final Exception e) {
            final RoutineException error = InvocationException.wrapIfNeeded(e);
            mOutputChannel.abort(error);
            mInputChannel.abort(error);
        }
    }

    /**
     * Output consumer implementation avoiding the upstream propagation of errors.
     *
     * @param <IN> the input data type.
     */
    private static class SafeOutputConsumer<IN> implements OutputConsumer<IN> {

        private final IOChannel<IN> mChannel;

        /**
         * Constructor.
         *
         * @param channel the I/O channel.
         */
        private SafeOutputConsumer(@NotNull final IOChannel<IN> channel) {

            mChannel = channel;
        }

        public void onComplete() {

            try {
                mChannel.close();

            } catch (final Exception ignored) {

            }
        }

        public void onError(@NotNull final RoutineException error) {

            try {
                mChannel.abort(error);

            } catch (final Exception ignored) {

            }
        }

        public void onOutput(final IN output) {

            try {
                mChannel.pass(output);

            } catch (final Exception ignored) {

            }
        }
    }

    public void onError(@NotNull final RoutineException error) throws Exception {

        Long delay = null;
        if (!(error instanceof AbortException)) {
            delay = mFunction.apply(++mCount, error);
        }

        if (delay != null) {
            mOutputs.clear();
            mRunner.run(this, delay, TimeUnit.MILLISECONDS);

        } else {
            mOutputChannel.abort(error);
            mInputChannel.abort(error);
        }
    }

    public void onOutput(final OUT output) {

        mOutputs.add(output);
    }
}
