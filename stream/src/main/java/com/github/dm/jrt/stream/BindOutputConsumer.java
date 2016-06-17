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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Output consumer binding function.
 * <p>
 * Created by davide-maestroni on 05/14/2016.
 *
 * @param <OUT> the output data type.
 */
class BindOutputConsumer<OUT> implements Function<Channel<?, OUT>, Channel<?, Void>> {

    private final ChannelConfiguration mConfiguration;

    private final ConsumerWrapper<? super OUT> mOutputConsumer;

    /**
     * Constructor.
     *
     * @param configuration  the channel configuration.
     * @param outputConsumer the consumer instance.
     */
    BindOutputConsumer(@NotNull final ChannelConfiguration configuration,
            @NotNull final ConsumerWrapper<? super OUT> outputConsumer) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mOutputConsumer = ConstantConditions.notNull("consumer instance", outputConsumer);
    }

    public Channel<?, Void> apply(final Channel<?, OUT> channel) {
        final Channel<Void, Void> outputChannel = JRoutineCore.io()
                                                              .channelConfiguration()
                                                              .with(mConfiguration)
                                                              .apply()
                                                              .buildChannel();
        channel.bind(new ConsumingOutputConsumer<OUT>(mOutputConsumer, outputChannel));
        return outputChannel;
    }
}
