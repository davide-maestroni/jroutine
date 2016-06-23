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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.channel.AbstractBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Builder implementation returning a map of channels returning selectable output data.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class OutputMapBuilder<OUT> extends AbstractBuilder<SparseArray<Channel<?, OUT>>> {

    private static final WeakIdentityHashMap<Channel<?, ?>, HashMap<SelectInfo,
            SparseArray<Channel<?, ?>>>>
            sOutputChannels =
            new WeakIdentityHashMap<Channel<?, ?>, HashMap<SelectInfo, SparseArray<Channel<?,
                    ?>>>>();

    private final Channel<?, ? extends ParcelableSelectable<? extends OUT>> mChannel;

    private final HashSet<Integer> mIndexes;

    /**
     * Constructor.
     *
     * @param channel the selectable channel.
     * @param indexes the set of indexes.
     * @throws java.lang.NullPointerException if the specified set of indexes is null or contains a
     *                                        null object.
     */
    OutputMapBuilder(
            @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Set<Integer> indexes) {
        mChannel = ConstantConditions.notNull("channel instance", channel);
        final HashSet<Integer> indexSet =
                new HashSet<Integer>(ConstantConditions.notNull("set of indexes", indexes));
        if (indexSet.contains(null)) {
            throw new NullPointerException("the set of indexes must not contain null objects");
        }

        mIndexes = indexSet;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected SparseArray<Channel<?, OUT>> build(
            @NotNull final ChannelConfiguration configuration) {
        final HashSet<Integer> indexes = mIndexes;
        final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel = mChannel;
        synchronized (sOutputChannels) {
            final WeakIdentityHashMap<Channel<?, ?>, HashMap<SelectInfo, SparseArray<Channel<?,
                    ?>>>>
                    outputChannels = sOutputChannels;
            HashMap<SelectInfo, SparseArray<Channel<?, ?>>> channelMaps =
                    outputChannels.get(channel);
            if (channelMaps == null) {
                channelMaps = new HashMap<SelectInfo, SparseArray<Channel<?, ?>>>();
                outputChannels.put(channel, channelMaps);
            }

            final int size = indexes.size();
            final SelectInfo selectInfo = new SelectInfo(configuration, indexes);
            final SparseArray<Channel<?, OUT>> channelMap = new SparseArray<Channel<?, OUT>>(size);
            SparseArray<Channel<?, ?>> channels = channelMaps.get(selectInfo);
            if (channels != null) {
                final int channelSize = channels.size();
                for (int i = 0; i < channelSize; ++i) {
                    channelMap.append(channels.keyAt(i), (Channel<?, OUT>) channels.valueAt(i));
                }

            } else {
                final SparseArray<Channel<OUT, ?>> inputMap =
                        new SparseArray<Channel<OUT, ?>>(size);
                channels = new SparseArray<Channel<?, ?>>(size);
                for (final Integer index : indexes) {
                    final Channel<OUT, OUT> outputChannel = JRoutineCore.io()
                                                                        .channelConfiguration()
                                                                        .with(configuration)
                                                                        .applied()
                                                                        .buildChannel();
                    inputMap.append(index, outputChannel);
                    channelMap.append(index, outputChannel);
                    channels.append(index, outputChannel);
                }

                channel.bind(new SortingMapOutputConsumer<OUT>(inputMap));
                channelMaps.put(selectInfo, channels);
            }

            return channelMap;
        }
    }

    /**
     * Class used as key to identify a specific map of output channels.
     */
    private static class SelectInfo extends DeepEqualObject {

        /**
         * Constructor.
         *
         * @param configuration the channel configuration.
         * @param indexes       the set of indexes,
         */
        private SelectInfo(@NotNull final ChannelConfiguration configuration,
                @NotNull final HashSet<Integer> indexes) {
            super(asArgs(configuration, indexes));
        }
    }
}
