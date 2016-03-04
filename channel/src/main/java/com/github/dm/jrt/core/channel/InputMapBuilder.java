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
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.IOChannel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder implementation returning a map of input channels accepting selectable data.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputMapBuilder<DATA, IN extends DATA> extends AbstractBuilder<Map<Integer, IOChannel<IN>>> {

    private final InputChannel<? super Selectable<DATA>> mChannel;

    private final HashSet<Integer> mIndexes;

    /**
     * Constructor.
     *
     * @param channel the selectable channel.
     * @param indexes the set of indexes.
     */
    @SuppressWarnings("ConstantConditions")
    InputMapBuilder(@NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Set<Integer> indexes) {

        if (channel == null) {
            throw new NullPointerException("the input channel must not be null");
        }

        if (indexes == null) {
            throw new NullPointerException("the set of indexes must not be null");
        }

        final HashSet<Integer> indexSet = new HashSet<Integer>(indexes);
        if (indexSet.contains(null)) {
            throw new NullPointerException("the set of indexes must not contain null objects");
        }

        mChannel = channel;
        mIndexes = indexSet;
    }

    @NotNull
    @Override
    protected Map<Integer, IOChannel<IN>> build(@NotNull final ChannelConfiguration configuration) {

        final HashSet<Integer> indexes = mIndexes;
        final InputChannel<? super Selectable<DATA>> channel = mChannel;
        final HashMap<Integer, IOChannel<IN>> channelMap =
                new HashMap<Integer, IOChannel<IN>>(indexes.size());
        for (final Integer index : indexes) {
            final IOChannel<IN> ioChannel = Channels.<DATA, IN>select(channel, index)
                                                    .withChannels()
                                                    .with(configuration)
                                                    .getConfigured()
                                                    .build();
            channelMap.put(index, ioChannel);
        }

        return channelMap;
    }
}
