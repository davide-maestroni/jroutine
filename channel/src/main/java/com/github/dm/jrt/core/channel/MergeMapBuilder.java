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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder implementation returning a channel merging data from a map of output channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeMapBuilder<OUT> extends AbstractBuilder<OutputChannel<? extends Selectable<OUT>>> {

    private final HashMap<Integer, OutputChannel<? extends OUT>> mChannelMap;

    /**
     * Constructor.
     *
     * @param channels the map of channels to merge.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    MergeMapBuilder(@NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, OutputChannel<? extends OUT>> channelMap =
                new HashMap<Integer, OutputChannel<? extends OUT>>(channels);
        if (channelMap.containsValue(null)) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        mChannelMap = channelMap;
    }

    @NotNull
    @Override
    protected OutputChannel<? extends Selectable<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final IOChannel<Selectable<OUT>> ioChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : mChannelMap
                .entrySet()) {
            ioChannel.pass(Channels.toSelectable(entry.getValue(), entry.getKey()).build());
        }

        return ioChannel.close();
    }
}
