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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Retry binding function.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindRetry<IN, OUT> implements Function<OutputChannel<IN>, OutputChannel<OUT>> {

    private final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
            mBackoffFunction;

    private final Function<OutputChannel<IN>, OutputChannel<OUT>> mBindingFunction;

    private final ChannelConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param configuration   the channel configuration.
     * @param bindingFunction the binding function.
     * @param backoffFunction the backoff function.
     */
    BindRetry(@NotNull final ChannelConfiguration configuration,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> bindingFunction,
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mBindingFunction = ConstantConditions.notNull("binding function", bindingFunction);
        mBackoffFunction = ConstantConditions.notNull("backoff function", backoffFunction);
    }

    public OutputChannel<OUT> apply(final OutputChannel<IN> channel) {
        final ChannelConfiguration configuration = mConfiguration;
        final OutputChannel<IN> inputChannel = Channels.replay(channel).buildChannels();
        final IOChannel<OUT> outputChannel =
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        new RetryOutputConsumer<IN, OUT>(inputChannel, outputChannel,
                configuration.getRunnerOrElse(Runners.sharedRunner()), mBindingFunction,
                mBackoffFunction).run();
        return outputChannel;
    }
}
