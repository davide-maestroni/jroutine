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
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder implementation returning a channel combining data from a collection of input channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class CombineBuilder<IN> extends AbstractBuilder<IOChannel<Selectable<? extends IN>>> {

    private final ArrayList<InputChannel<? extends IN>> mChannels;

    private final int mStartIndex;

    /**
     * Constructor.
     *
     * @param startIndex the selectable start index.
     * @param channels   the input channels to combine.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    CombineBuilder(final int startIndex,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        final ArrayList<InputChannel<? extends IN>> channelList =
                new ArrayList<InputChannel<? extends IN>>(channels);
        if (channelList.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        mStartIndex = startIndex;
        mChannels = channelList;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected IOChannel<Selectable<? extends IN>> build(
            @NotNull final ChannelConfiguration configuration) {

        final ArrayList<InputChannel<? extends IN>> channels = mChannels;
        final ArrayList<IOChannel<? extends IN>> channelList =
                new ArrayList<IOChannel<? extends IN>>(channels.size());
        for (final InputChannel<?> channel : channels) {
            final IOChannel<? extends IN> ioChannel = JRoutineCore.io().buildChannel();
            ioChannel.bind(((InputChannel<IN>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<Selectable<? extends IN>> ioChannel =
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        ioChannel.bind(new SortingArrayOutputConsumer(mStartIndex, channelList));
        return ioChannel;
    }

    /**
     * Output consumer sorting selectable inputs among a list of input channels.
     */
    private static class SortingArrayOutputConsumer<IN>
            implements OutputConsumer<Selectable<? extends IN>> {

        private final ArrayList<IOChannel<? extends IN>> mChannelList;

        private final int mSize;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the list of channels.
         */
        private SortingArrayOutputConsumer(final int startIndex,
                @NotNull final ArrayList<IOChannel<? extends IN>> channels) {

            mStartIndex = startIndex;
            mChannelList = channels;
            mSize = channels.size();
        }

        public void onComplete() {

            for (final IOChannel<? extends IN> channel : mChannelList) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<? extends IN> channel : mChannelList) {
                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<? extends IN> selectable) {

            final int index = selectable.index - mStartIndex;
            if ((index < 0) || (index >= mSize)) {
                return;
            }

            final IOChannel<IN> channel = (IOChannel<IN>) mChannelList.get(index);
            if (channel != null) {
                channel.pass(selectable.data);
            }
        }
    }
}
