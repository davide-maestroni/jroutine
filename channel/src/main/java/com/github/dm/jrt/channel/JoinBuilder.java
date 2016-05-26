package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        final Object mutex = new Object();
        final int size = channels.size();
        final boolean[] closed = new boolean[size];
        @SuppressWarnings("unchecked") final SimpleQueue<OUT>[] queues = new SimpleQueue[size];
        for (int i = 0; i < size; ++i) {
            queues[i] = new SimpleQueue<OUT>();
        }

        int i = 0;
        final boolean isFlush = mIsFlush;
        final OUT placeholder = mPlaceholder;
        final int limit = configuration.getChannelLimitOrElse(Integer.MAX_VALUE);
        final Backoff backoff = configuration.getChannelBackoffOrElse(null);
        final int maxSize = configuration.getChannelMaxSizeOrElse(Integer.MAX_VALUE);
        for (final OutputChannel<? extends OUT> channel : channels) {
            channel.bind(new JoinOutputConsumer<OUT>(limit, backoff, maxSize, mutex, i++, isFlush,
                    closed, queues, placeholder, ioChannel));
        }

        return ioChannel;
    }

    /**
     * Output consumer joining the data coming from several channels.
     *
     * @param <OUT> the output data type.
     */
    private static class JoinOutputConsumer<OUT> implements OutputConsumer<OUT> {

        private final Backoff mBackoff;

        private final IOChannel<List<? extends OUT>> mChannel;

        private final boolean[] mClosed;

        private final int mIndex;

        private final boolean mIsFlush;

        private final int mLimit;

        private final int mMaxSize;

        private final Object mMutex;

        private final OUT mPlaceholder;

        private final SimpleQueue<OUT>[] mQueues;

        /**
         * Constructor.
         *
         * @param limit       the channel limit.
         * @param backoff     the channel backoff.
         * @param maxSize     the channel maxSize.
         * @param mutex       the object used to synchronized the shared parameters.
         * @param index       the index in the array of queues related to this consumer.
         * @param isFlush     whether the inputs have to be flushed.
         * @param closed      the array of booleans indicating whether a queue is closed.
         * @param queues      the array of queues used to store the outputs.
         * @param placeholder the placeholder instance.
         * @param channel     the I/O channel.
         */
        private JoinOutputConsumer(final int limit, @Nullable final Backoff backoff,
                final int maxSize, @NotNull final Object mutex, final int index,
                final boolean isFlush, @NotNull final boolean[] closed,
                @NotNull final SimpleQueue<OUT>[] queues, @Nullable final OUT placeholder,
                @NotNull final IOChannel<List<? extends OUT>> channel) {

            mLimit = limit;
            mBackoff = backoff;
            mMaxSize = maxSize;
            mMutex = mutex;
            mIndex = index;
            mIsFlush = isFlush;
            mClosed = closed;
            mQueues = queues;
            mChannel = channel;
            mPlaceholder = placeholder;
        }

        public void onComplete() {

            boolean isClosed = true;
            synchronized (mMutex) {
                mClosed[mIndex] = true;
                for (final boolean closed : mClosed) {
                    if (!closed) {
                        isClosed = false;
                        break;
                    }
                }
            }

            if (isClosed) {
                if (mIsFlush) {
                    flush();
                }

                mChannel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final OUT output) throws InterruptedException {

            final SimpleQueue<OUT>[] queues = mQueues;
            final SimpleQueue<OUT> myQueue = queues[mIndex];
            final ArrayList<OUT> outputs;
            synchronized (mMutex) {
                myQueue.add(output);
                boolean isFull = true;
                for (final SimpleQueue<OUT> queue : queues) {
                    if (queue.isEmpty()) {
                        isFull = false;
                        break;
                    }
                }

                if (isFull) {
                    outputs = new ArrayList<OUT>(queues.length);
                    for (final SimpleQueue<OUT> queue : queues) {
                        outputs.add(queue.removeFirst());
                    }

                } else {
                    outputs = null;
                }
            }

            if (outputs != null) {
                mChannel.pass(outputs);

            } else {
                final Backoff backoff = mBackoff;
                if (backoff != null) {
                    final int size = myQueue.size();
                    if (size > mMaxSize) {
                        mChannel.abort(new OutputDeadlockException(
                                "maximum output channel size has been reached: " + mMaxSize));
                        return;
                    }

                    final int count = size - mLimit;
                    if (count > 0) {
                        final long delay = backoff.getDelay(count);
                        UnitDuration.sleepAtLeast(delay, TimeUnit.MILLISECONDS);
                    }
                }
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
