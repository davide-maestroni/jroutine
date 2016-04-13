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
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
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
 * Builder implementation returning a map of output channels returning selectable output data.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class OutputMapBuilder<OUT> extends AbstractBuilder<SparseArray<OutputChannel<OUT>>> {

    private static final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo,
            SparseArray<OutputChannel<?>>>>
            sOutputChannels =
            new WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo,
                    SparseArray<OutputChannel<?>>>>();

    private final OutputChannel<? extends ParcelableSelectable<? extends OUT>> mChannel;

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
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Set<Integer> indexes) {

        mChannel = ConstantConditions.notNull("output channel", channel);
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
    protected SparseArray<OutputChannel<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final HashSet<Integer> indexes = mIndexes;
        final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel = mChannel;
        synchronized (sOutputChannels) {
            final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo,
                    SparseArray<OutputChannel<?>>>>
                    outputChannels = sOutputChannels;
            HashMap<SelectInfo, SparseArray<OutputChannel<?>>> channelMaps =
                    outputChannels.get(channel);
            if (channelMaps == null) {
                channelMaps = new HashMap<SelectInfo, SparseArray<OutputChannel<?>>>();
                outputChannels.put(channel, channelMaps);
            }

            final int size = indexes.size();
            final SelectInfo selectInfo = new SelectInfo(configuration, indexes);
            final SparseArray<OutputChannel<OUT>> channelMap =
                    new SparseArray<OutputChannel<OUT>>(size);
            SparseArray<OutputChannel<?>> channels = channelMaps.get(selectInfo);
            if (channels != null) {
                final int channelSize = channels.size();
                for (int i = 0; i < channelSize; i++) {
                    channelMap.append(channels.keyAt(i), (OutputChannel<OUT>) channels.valueAt(i));
                }

            } else {
                final SparseArray<IOChannel<OUT>> inputMap = new SparseArray<IOChannel<OUT>>(size);
                channels = new SparseArray<OutputChannel<?>>(size);
                for (final Integer index : indexes) {
                    final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                                 .getChannelConfiguration()
                                                                 .with(configuration)
                                                                 .setConfiguration()
                                                                 .buildChannel();
                    inputMap.append(index, ioChannel);
                    channelMap.append(index, ioChannel);
                    channels.append(index, ioChannel);
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
