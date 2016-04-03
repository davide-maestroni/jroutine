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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder implementation returning a channel concatenating data from a set of output channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class ConcatBuilder<OUT> extends AbstractBuilder<OutputChannel<OUT>> {

    private final ArrayList<OutputChannel<? extends OUT>> mChannels;

    /**
     * Constructor.
     *
     * @param channels the output channels to concat.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
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

        final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .with(configuration)
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .setConfiguration()
                                                     .buildChannel();
        for (final OutputChannel<? extends OUT> channel : mChannels) {
            channel.bind(ioChannel);
        }

        return ioChannel.close();
    }
}
