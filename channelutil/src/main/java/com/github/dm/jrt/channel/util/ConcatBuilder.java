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

package com.github.dm.jrt.channel.util;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.core.JRoutine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder implementation returning a channel concatenating data from a set of output channels.
 *
 * @param <OUT> the output data type.
 */
class ConcatBuilder<OUT> extends AbstractBuilder<OutputChannel<OUT>> {

    private final ArrayList<OutputChannel<? extends OUT>> mChannels;

    /**
     * Constructor.
     *
     * @param channels the output channels to concat.
     */
    ConcatBuilder(@NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        final ArrayList<OutputChannel<? extends OUT>> channelList =
                new ArrayList<OutputChannel<? extends OUT>>(channels);
        if (channelList.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        mChannels = channelList;
    }

    @NotNull
    @Override
    protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

        final IOChannel<OUT> ioChannel = JRoutine.io()
                                                 .withChannels()
                                                 .with(configuration)
                                                 .withChannelOrder(OrderType.BY_CALL)
                                                 .getConfigured()
                                                 .buildChannel();
        for (final OutputChannel<? extends OUT> channel : mChannels) {
            channel.passTo(ioChannel);
        }

        return ioChannel.close();
    }
}
