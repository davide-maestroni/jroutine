package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builder implementation joining data from a set of channels.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <OUT> the output data type.
 */
class JoinOutputBuilder<OUT> extends AbstractChannelBuilder<List<OUT>, List<OUT>> {

  private final ArrayList<Channel<?, ? extends OUT>> mChannels;

  private final boolean mIsFlush;

  private final OUT mPlaceholder;

  /**
   * Constructor.
   *
   * @param isFlush     whether to flush data.
   * @param placeholder the placeholder instance.
   * @param channels    the channels to join.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  JoinOutputBuilder(final boolean isFlush, @Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final ArrayList<Channel<?, ? extends OUT>> channelList =
        new ArrayList<Channel<?, ? extends OUT>>();
    for (final Channel<?, ? extends OUT> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
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
  public Channel<List<OUT>, List<OUT>> buildChannel() {
    final ArrayList<Channel<?, ? extends OUT>> channels = mChannels;
    final ChannelConfiguration configuration = getConfiguration();
    final Channel<List<OUT>, List<OUT>> outputChannel =
        JRoutineCore.<List<OUT>>ofInputs().apply(configuration).buildChannel();
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
    final Backoff backoff = configuration.getBackoffOrElse(null);
    final int maxSize = configuration.getMaxSizeOrElse(Integer.MAX_VALUE);
    for (final Channel<?, ? extends OUT> channel : channels) {
      channel.consume(
          new JoinChannelConsumer<OUT>(backoff, maxSize, mutex, i++, isFlush, closed, queues,
              placeholder, outputChannel));
    }

    return outputChannel;
  }

  /**
   * Channel consumer joining the data coming from several channels.
   *
   * @param <OUT> the output data type.
   */
  private static class JoinChannelConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Backoff mBackoff;

    private final Channel<List<OUT>, List<OUT>> mChannel;

    private final boolean[] mClosed;

    private final int mIndex;

    private final boolean mIsFlush;

    private final int mMaxSize;

    private final Object mMutex;

    private final OUT mPlaceholder;

    private final SimpleQueue<OUT>[] mQueues;

    /**
     * Constructor.
     *
     * @param backoff     the channel backoff.
     * @param maxSize     the channel maxSize.
     * @param mutex       the object used to synchronized the shared parameters.
     * @param index       the index in the array of queues related to this consumer.
     * @param isFlush     whether the inputs have to be flushed.
     * @param closed      the array of booleans indicating whether a queue is closed.
     * @param queues      the array of queues used to store the outputs.
     * @param placeholder the placeholder instance.
     * @param channel     the channel.
     */
    private JoinChannelConsumer(@Nullable final Backoff backoff, final int maxSize,
        @NotNull final Object mutex, final int index, final boolean isFlush,
        @NotNull final boolean[] closed, @NotNull final SimpleQueue<OUT>[] queues,
        @Nullable final OUT placeholder, @NotNull final Channel<List<OUT>, List<OUT>> channel) {
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
        final Channel<List<OUT>, List<OUT>> channel = mChannel;
        try {
          if (mIsFlush) {
            flush();
          }

          channel.close();

        } catch (final Throwable t) {
          channel.abort(t);
          InterruptedInvocationException.throwIfInterrupt(t);
        }
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
        final boolean isFlush = mIsFlush;
        final boolean[] closed = mClosed;
        final int length = queues.length;
        boolean isFull = true;
        for (int i = 0; i < length; ++i) {
          final SimpleQueue<OUT> queue = queues[i];
          if (queue.isEmpty()) {
            if (isFlush && closed[i]) {
              continue;
            }

            isFull = false;
            break;
          }
        }

        if (isFull) {
          outputs = new ArrayList<OUT>(length);
          final OUT placeholder = mPlaceholder;
          for (int i = 0; i < length; ++i) {
            final SimpleQueue<OUT> queue = queues[i];
            if (isFlush && queue.isEmpty() && closed[i]) {
              outputs.add(placeholder);

            } else {
              outputs.add(queue.removeFirst());
            }
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
                "maximum output channel size has been exceeded: " + mMaxSize));
            return;
          }

          final long delay = backoff.getDelay(size);
          if (delay > 0) {
            DurationMeasure.sleepAtLeast(delay, TimeUnit.MILLISECONDS);
          }
        }
      }
    }

    private void flush() {
      final Channel<List<OUT>, List<OUT>> channel = mChannel;
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
