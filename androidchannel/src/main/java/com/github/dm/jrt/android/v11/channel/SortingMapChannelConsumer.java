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

package com.github.dm.jrt.android.v11.channel;

import android.util.SparseArray;

import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

/**
 * Channel consumer sorting the output data among a map of channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class SortingMapChannelConsumer<OUT> implements ChannelConsumer<Selectable<? extends OUT>> {

    private final SparseArray<Channel<OUT, ?>> mChannels;

    /**
     * Constructor.
     *
     * @param channels the map of indexes and channels.
     * @throws java.lang.NullPointerException if the specified map is null or contains a null
     *                                        object.
     */
    SortingMapChannelConsumer(@NotNull final SparseArray<Channel<OUT, ?>> channels) {
        final SparseArray<Channel<OUT, ?>> channelMap = channels.clone();
        if (channelMap.indexOfValue(null) >= 0) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        mChannels = channelMap;
    }

    @Override
    public void onComplete() {
        final SparseArray<Channel<OUT, ?>> channels = mChannels;
        final int size = channels.size();
        for (int i = 0; i < size; ++i) {
            channels.valueAt(i).close();
        }
    }

    @Override
    public void onError(@NotNull final RoutineException error) {
        final SparseArray<Channel<OUT, ?>> channels = mChannels;
        final int size = channels.size();
        for (int i = 0; i < size; ++i) {
            channels.valueAt(i).abort(error);
        }
    }

    @Override
    public void onOutput(final Selectable<? extends OUT> selectable) {
        final Channel<OUT, ?> channel = mChannels.get(selectable.index);
        if (channel != null) {
            channel.pass(selectable.data);
        }
    }
}
