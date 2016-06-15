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
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Completion consumer binding function.
 * <p>
 * Created by davide-maestroni on 06/15/2016.
 *
 * @param <OUT> the output data type.
 */
class BindCompleteConsumer<OUT> implements Function<OutputChannel<OUT>, OutputChannel<Void>> {

    private final Runnable mCompleteAction;

    private final ChannelConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param action        the runnable instance.
     */
    BindCompleteConsumer(@NotNull final ChannelConfiguration configuration,
            @NotNull final Runnable action) {
        mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
        mCompleteAction = ConstantConditions.notNull("runnable instance", action);
    }

    public OutputChannel<Void> apply(final OutputChannel<OUT> channel) throws Exception {
        final IOChannel<Void> ioChannel = JRoutineCore.io()
                                                      .channelConfiguration()
                                                      .with(mConfiguration)
                                                      .apply()
                                                      .buildChannel();
        channel.bind(new CompleteOutputConsumer<OUT>(mCompleteAction, ioChannel));
        return ioChannel;
    }
}
