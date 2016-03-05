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
import com.github.dm.jrt.core.builder.ChannelConfiguration;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder implementation returning a channel distributing data into a set of input channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class DistributeBuilder<IN> extends AbstractBuilder<IOChannel<List<? extends IN>>> {

    private final ArrayList<InputChannel<? extends IN>> mChannels;

    private final boolean mIsFlush;

    private final IN mPlaceholder;

    /**
     * Constructor.
     *
     * @param isFlush     whether to flush data.
     * @param placeholder the placeholder instance.
     * @param channels    the list of channels.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    DistributeBuilder(final boolean isFlush, @Nullable final IN placeholder,
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

        mIsFlush = isFlush;
        mPlaceholder = placeholder;
        mChannels = channelList;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected IOChannel<List<? extends IN>> build(
            @NotNull final ChannelConfiguration configuration) {

        final ArrayList<InputChannel<? extends IN>> channels = mChannels;
        final ArrayList<IOChannel<?>> channelList = new ArrayList<IOChannel<?>>(channels.size());
        for (final InputChannel<?> channel : channels) {
            final IOChannel<?> ioChannel = JRoutineCore.io().buildChannel();
            ioChannel.bind(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<List<? extends IN>> ioChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        return ioChannel.bind(new DistributeOutputConsumer(mIsFlush, mPlaceholder, channelList));
    }

    /**
     * Output consumer distributing list of data among a list of channels.
     *
     * @param <IN> the input data type.
     */
    private static class DistributeOutputConsumer<IN>
            implements OutputConsumer<List<? extends IN>> {

        private final ArrayList<IOChannel<? extends IN>> mChannels;

        private final boolean mIsFlush;

        private final IN mPlaceholder;

        /**
         * Constructor.
         *
         * @param isFlush     whether the inputs have to be flushed.
         * @param placeholder the placeholder instance.
         * @param channels    the list of channels.
         */
        private DistributeOutputConsumer(final boolean isFlush, @Nullable final IN placeholder,
                @NotNull final ArrayList<IOChannel<? extends IN>> channels) {

            mIsFlush = isFlush;
            mChannels = channels;
            mPlaceholder = placeholder;
        }

        public void onComplete() {

            for (final IOChannel<? extends IN> channel : mChannels) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<? extends IN> channel : mChannels) {
                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final List<? extends IN> inputs) {

            final int inputSize = inputs.size();
            final ArrayList<IOChannel<? extends IN>> channels = mChannels;
            final int size = channels.size();
            if (inputSize > size) {
                throw new IllegalArgumentException();
            }

            final IN placeholder = mPlaceholder;
            final boolean isFlush = mIsFlush;
            for (int i = 0; i < size; ++i) {
                final IOChannel<IN> channel = (IOChannel<IN>) channels.get(i);
                if (i < inputSize) {
                    channel.pass(inputs.get(i));

                } else if (isFlush) {
                    channel.pass(placeholder);
                }
            }
        }
    }
}
