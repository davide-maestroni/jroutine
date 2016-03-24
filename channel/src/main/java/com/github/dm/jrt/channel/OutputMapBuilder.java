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
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Builder implementation returning a map of output channels returning selectable output data.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class OutputMapBuilder<OUT> extends AbstractBuilder<Map<Integer, OutputChannel<OUT>>> {

    private static final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo,
            HashMap<Integer, OutputChannel<?>>>>
            sOutputChannels =
            new WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                    OutputChannel<?>>>>();

    private final OutputChannel<? extends Selectable<? extends OUT>> mChannel;

    private final HashSet<Integer> mIndexes;

    /**
     * Constructor.
     *
     * @param channel the selectable channel.
     * @param indexes the set of indexes.
     * @throws java.lang.NullPointerException if the specified set of indexes is null or contains a
     *                                        null object.
     */
    @SuppressWarnings("ConstantConditions")
    OutputMapBuilder(@NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final Set<Integer> indexes) {

        if (channel == null) {
            throw new NullPointerException("the output channel must not be null");
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
    @SuppressWarnings("unchecked")
    protected Map<Integer, OutputChannel<OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final HashSet<Integer> indexes = mIndexes;
        final OutputChannel<? extends Selectable<? extends OUT>> channel = mChannel;
        synchronized (sOutputChannels) {
            final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                    OutputChannel<?>>>>
                    outputChannels = sOutputChannels;
            HashMap<SelectInfo, HashMap<Integer, OutputChannel<?>>> channelMaps =
                    outputChannels.get(channel);
            if (channelMaps == null) {
                channelMaps = new HashMap<SelectInfo, HashMap<Integer, OutputChannel<?>>>();
                outputChannels.put(channel, channelMaps);
            }

            final int size = indexes.size();
            final SelectInfo selectInfo = new SelectInfo(configuration, indexes);
            final HashMap<Integer, OutputChannel<OUT>> channelMap =
                    new HashMap<Integer, OutputChannel<OUT>>(size);
            HashMap<Integer, OutputChannel<?>> channels = channelMaps.get(selectInfo);
            if (channels != null) {
                for (final Entry<Integer, OutputChannel<?>> entry : channels.entrySet()) {
                    channelMap.put(entry.getKey(), (OutputChannel<OUT>) entry.getValue());
                }

            } else {
                final HashMap<Integer, IOChannel<OUT>> inputMap =
                        new HashMap<Integer, IOChannel<OUT>>(size);
                channels = new HashMap<Integer, OutputChannel<?>>(size);
                for (final Integer index : indexes) {
                    final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                                 .withChannels()
                                                                 .with(configuration)
                                                                 .setConfiguration()
                                                                 .buildChannel();
                    inputMap.put(index, ioChannel);
                    channelMap.put(index, ioChannel);
                    channels.put(index, ioChannel);
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
    private static class SelectInfo {

        private final ChannelConfiguration mConfiguration;

        private final HashSet<Integer> mIndexes;

        /**
         * Constructor.
         *
         * @param configuration the channel configuration.
         * @param indexes       the set of indexes,
         */
        private SelectInfo(@NotNull final ChannelConfiguration configuration,
                @NotNull final HashSet<Integer> indexes) {

            mConfiguration = configuration;
            mIndexes = indexes;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mConfiguration.hashCode();
            result = 31 * result + mIndexes.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof SelectInfo)) {
                return false;
            }

            final SelectInfo that = (SelectInfo) o;
            return mConfiguration.equals(that.mConfiguration) && mIndexes.equals(that.mIndexes);
        }
    }
}
