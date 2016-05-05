package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder implementation joining data from a set of output channels.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
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
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    JoinBuilder(final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        final ArrayList<OutputChannel<? extends OUT>> channelList =
                new ArrayList<OutputChannel<? extends OUT>>();
        for (final OutputChannel<? extends OUT> channel : channels) {
            if (channel == null) {
                throw new NullPointerException(
                        "the collection of channels must not contain null objects");
            }

            channelList.add(channel);
        }

        if (channelList.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
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
                JRoutineCore.io().channelConfiguration().with(configuration).apply().buildChannel();
        final JoinOutputConsumer<OUT> consumer =
                new JoinOutputConsumer<OUT>(mIsFlush, channels.size(), mPlaceholder, ioChannel);
        new MergeBuilder<OUT>(0, channels).channelConfiguration()
                                          .with(configuration)
                                          .apply()
                                          .buildChannels()
                                          .bind(consumer);
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
