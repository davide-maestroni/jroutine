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

package com.github.dm.jrt.android.v11.core.channel;

import android.util.SparseArray;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbstractBuilder;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.Selectable;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel combining data from a map of input channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class CombineMapBuilder<IN> extends AbstractBuilder<IOChannel<Selectable<? extends IN>>> {

    private final SparseArray<? extends InputChannel<? extends IN>> mChannelMap;

    /**
     * Constructor.
     *
     * @param channels the map of channels to combine.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    CombineMapBuilder(@NotNull final SparseArray<? extends InputChannel<? extends IN>> channels) {

        if (channels.size() == 0) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final SparseArray<? extends InputChannel<? extends IN>> channelMap = channels.clone();
        if (channelMap.indexOfValue(null) >= 0) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        mChannelMap = channelMap;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected IOChannel<Selectable<? extends IN>> build(
            @NotNull final ChannelConfiguration configuration) {

        final SparseArray<? extends InputChannel<? extends IN>> channelMap = mChannelMap;
        final int size = channelMap.size();
        final SparseArray<IOChannel<IN>> ioChannelMap = new SparseArray<IOChannel<IN>>(size);
        for (int i = 0; i < size; ++i) {
            final IOChannel<IN> ioChannel = JRoutineCore.io().buildChannel();
            ioChannel.bindTo(((InputChannel<IN>) channelMap.valueAt(i)));
            ioChannelMap.put(channelMap.keyAt(i), ioChannel);
        }

        final IOChannel<Selectable<? extends IN>> ioChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        ioChannel.bindTo(new SortingMapOutputConsumer<IN>(ioChannelMap));
        return ioChannel;
    }
}
