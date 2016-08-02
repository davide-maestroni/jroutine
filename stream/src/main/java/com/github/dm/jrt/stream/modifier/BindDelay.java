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

package com.github.dm.jrt.stream.modifier;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Delay binding function.
 * <p>
 * Created by davide-maestroni on 06/29/2016.
 *
 * @param <OUT> the output data type.
 */
class BindDelay<OUT> implements Function<Channel<?, OUT>, Channel<?, OUT>> {

    private final ChannelConfiguration mConfiguration;

    private final long mDelay;

    private final TimeUnit mDelayUnit;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param delay         the delay value.
     * @param timeUnit      the delay time unit.
     */
    BindDelay(@NotNull final ChannelConfiguration configuration, final long delay,
            @NotNull final TimeUnit timeUnit) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mDelay = ConstantConditions.notNegative("delay value", delay);
        mDelayUnit = ConstantConditions.notNull("delay unit", timeUnit);
    }

    public Channel<?, OUT> apply(final Channel<?, OUT> channel) throws Exception {
        final ChannelConfiguration configuration = mConfiguration;
        final Channel<OUT, OUT> outputChannel = JRoutineCore.io()
                                                            .channelConfiguration()
                                                            .with(configuration)
                                                            .configured()
                                                            .buildChannel();
        channel.bind(new DelayChannelConsumer<OUT>(mDelay, mDelayUnit,
                configuration.getRunnerOrElse(Runners.sharedRunner()), outputChannel));
        return outputChannel;
    }
}
