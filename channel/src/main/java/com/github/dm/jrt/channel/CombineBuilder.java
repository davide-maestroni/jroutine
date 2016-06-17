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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation returning a channel combining data from a collection of input channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class CombineBuilder<IN> extends AbstractBuilder<Channel<Selectable<? extends IN>, ?>> {

    private final ArrayList<Channel<? extends IN, ?>> mChannels;

    private final int mStartIndex;

    /**
     * Constructor.
     *
     * @param startIndex the selectable start index.
     * @param channels   the input channels to combine.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    CombineBuilder(final int startIndex,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        final ArrayList<Channel<? extends IN, ?>> channelList =
                new ArrayList<Channel<? extends IN, ?>>();
        for (final Channel<? extends IN, ?> channel : channels) {
            if (channel == null) {
                throw new NullPointerException(
                        "the collection of channels must not contain null objects");
            }

            channelList.add(channel);
        }

        if (channelList.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        mStartIndex = startIndex;
        mChannels = channelList;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected Channel<Selectable<? extends IN>, ?> build(
            @NotNull final ChannelConfiguration configuration) {
        final ArrayList<Channel<? extends IN, ?>> channels = mChannels;
        final ArrayList<Channel<? extends IN, ?>> channelList =
                new ArrayList<Channel<? extends IN, ?>>(channels.size());
        for (final Channel<? extends IN, ?> channel : channels) {
            final Channel<IN, IN> outputChannel = JRoutineCore.io()
                                                              .channelConfiguration()
                                                              .with(configuration)
                                                              .apply()
                                                              .buildChannel();
            outputChannel.bind((Channel<IN, ?>) channel);
            channelList.add(outputChannel);
        }

        final Channel<Selectable<? extends IN>, ?> inputChannel =
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        return inputChannel.bind(new SortingArrayOutputConsumer(mStartIndex, channelList));
    }

    /**
     * Output consumer sorting selectable inputs among a list of input channels.
     */
    private static class SortingArrayOutputConsumer<IN>
            implements OutputConsumer<Selectable<? extends IN>> {

        private final ArrayList<Channel<? extends IN, ?>> mChannelList;

        private final int mSize;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the list of channels.
         */
        private SortingArrayOutputConsumer(final int startIndex,
                @NotNull final ArrayList<Channel<? extends IN, ?>> channels) {
            mStartIndex = startIndex;
            mChannelList = channels;
            mSize = channels.size();
        }

        public void onComplete() {
            for (final Channel<? extends IN, ?> channel : mChannelList) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {
            for (final Channel<? extends IN, ?> channel : mChannelList) {
                channel.abort(error);
            }
        }

        public void onOutput(final Selectable<? extends IN> selectable) {
            final int index = selectable.index - mStartIndex;
            if ((index < 0) || (index >= mSize)) {
                return;
            }

            @SuppressWarnings("unchecked") final Channel<IN, ?> channel =
                    (Channel<IN, ?>) mChannelList.get(index);
            if (channel != null) {
                channel.pass(selectable.data);
            }
        }
    }
}
