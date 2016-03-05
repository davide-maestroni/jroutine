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
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder implementation joining data from a set of output channels.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class JoinBuilder<OUT> extends AbstractBuilder<OutputChannel<List<? extends OUT>>> {

    private final ArrayList<OutputChannel<? extends OUT>> mChannels;

    private final boolean mIsFlush;

    private final OUT mPlaceholder;

    /**
     * Constructor.
     *
     * @param isFlush     whether to flush data.
     * @param placeholder the placeholder instance.
     * @param channels    the input channels to join.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    JoinBuilder(final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        final ArrayList<OutputChannel<? extends OUT>> channelList =
                new ArrayList<OutputChannel<? extends OUT>>(channels);
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
    protected OutputChannel<List<? extends OUT>> build(
            @NotNull final ChannelConfiguration configuration) {

        final ArrayList<OutputChannel<? extends OUT>> channels = mChannels;
        final IOChannel<List<? extends OUT>> ioChannel =
                JRoutineCore.io().withChannels().with(configuration).getConfigured().buildChannel();
        final JoinOutputConsumer<OUT> consumer =
                new JoinOutputConsumer<OUT>(mIsFlush, channels.size(), mPlaceholder, ioChannel);
        Channels.merge(channels).build().bindTo(consumer);
        return ioChannel;
    }

    /**
     * Output consumer joining the data coming from several channels.
     *
     * @param <OUT> the output data type.
     */
    private static class JoinOutputConsumer<OUT> implements OutputConsumer<Selectable<OUT>> {

        private final IOChannel<List<? extends OUT>> mChannel;

        private final boolean mIsFlush;

        private final OUT mPlaceholder;

        private final SimpleQueue<OUT>[] mQueues;

        /**
         * Constructor.
         *
         * @param isFlush     whether the inputs have to be flushed.
         * @param size        the number of channels to join.
         * @param placeholder the placeholder instance.
         * @param channel     the I/O channel.
         */
        @SuppressWarnings("unchecked")
        private JoinOutputConsumer(final boolean isFlush, final int size,
                @Nullable final OUT placeholder,
                @NotNull final IOChannel<List<? extends OUT>> channel) {

            final SimpleQueue<OUT>[] queues = (mQueues = new SimpleQueue[size]);
            mIsFlush = isFlush;
            mChannel = channel;
            mPlaceholder = placeholder;
            for (int i = 0; i < size; ++i) {
                queues[i] = new SimpleQueue<OUT>();
            }
        }

        public void onComplete() {

            if (mIsFlush) {
                flush();
            }

            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final Selectable<OUT> selectable) {

            final int index = selectable.index;
            final SimpleQueue<OUT>[] queues = mQueues;
            queues[index].add(selectable.data);
            final int length = queues.length;
            boolean isFull = true;
            for (final SimpleQueue<OUT> queue : queues) {
                if (queue.isEmpty()) {
                    isFull = false;
                    break;
                }
            }

            if (isFull) {
                final ArrayList<OUT> outputs = new ArrayList<OUT>(length);
                for (final SimpleQueue<OUT> queue : queues) {
                    outputs.add(queue.removeFirst());
                }

                mChannel.pass(outputs);
            }
        }

        private void flush() {

            final IOChannel<List<? extends OUT>> channel = mChannel;
            final SimpleQueue<OUT>[] queues = mQueues;
            final int length = queues.length;
            final OUT placeholder = mPlaceholder;
            final ArrayList<OUT> outputs = new ArrayList<OUT>(length);
            boolean isEmpty;
            do {
                isEmpty = true;
                for (final SimpleQueue<OUT> queue : queues) {
                    if (!queue.isEmpty()) {
                        isEmpty = false;
                        outputs.add(queue.removeFirst());

                    } else {
                        outputs.add(placeholder);
                    }
                }

                if (!isEmpty) {
                    channel.pass(outputs);
                    outputs.clear();

                } else {
                    break;
                }

            } while (true);
        }
    }
}
