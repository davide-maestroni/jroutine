/*
 * Copyright 2017 Davide Maestroni
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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Channel consumer joining the data coming from several channels.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 *
 * @param <OUT> the output data type.
 */
class JoinChannelConsumer<OUT> implements ChannelConsumer<OUT> {

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
   * @param channel     the channel.
   * @param mutex       the object used to synchronized the shared parameters.
   * @param closed      the array of booleans indicating whether a queue is closed.
   * @param queues      the array of queues used to store the outputs.
   * @param backoff     the channel backoff.
   * @param maxSize     the channel maxSize.
   * @param index       the index in the array of queues related to this consumer.
   * @param isFlush     whether the inputs have to be flushed.
   * @param placeholder the placeholder instance.
   */
  JoinChannelConsumer(@NotNull final Channel<List<OUT>, List<OUT>> channel,
      @NotNull final Object mutex, @NotNull final boolean[] closed,
      @NotNull final SimpleQueue<OUT>[] queues, @Nullable final Backoff backoff, final int maxSize,
      final int index, final boolean isFlush, @Nullable final OUT placeholder) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mMutex = ConstantConditions.notNull("mutex instance", mutex);
    mClosed = ConstantConditions.notNull("closed array", closed);
    mQueues = ConstantConditions.notNull("queues array", queues);
    mBackoff = backoff;
    mMaxSize = maxSize;
    mIndex = index;
    mIsFlush = isFlush;
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
