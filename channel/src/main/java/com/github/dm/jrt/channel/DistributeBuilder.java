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
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder implementation returning a channel distributing data into a set of channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class DistributeBuilder<IN> extends AbstractBuilder<Channel<List<? extends IN>, ?>> {

  private final ArrayList<Channel<? extends IN, ?>> mChannels;

  private final boolean mIsFlush;

  private final IN mPlaceholder;

  /**
   * Constructor.
   *
   * @param isFlush     whether to flush data.
   * @param placeholder the placeholder instance.
   * @param channels    the channels.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  DistributeBuilder(final boolean isFlush, @Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    final ArrayList<Channel<? extends IN, ?>> channelList =
        new ArrayList<Channel<? extends IN, ?>>();
    for (final Channel<? extends IN, ?> channel : channels) {
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
  @Override
  @SuppressWarnings("unchecked")
  protected Channel<List<? extends IN>, ?> build(
      @NotNull final ChannelConfiguration configuration) {
    final ArrayList<Channel<? extends IN, ?>> channels = mChannels;
    final ArrayList<Channel<?, ?>> channelList = new ArrayList<Channel<?, ?>>(channels.size());
    for (final Channel<? extends IN, ?> channel : channels) {
      final Channel<IN, IN> outputChannel = JRoutineCore.io().apply(configuration).buildChannel();
      outputChannel.bind((Channel<IN, ?>) channel);
      channelList.add(outputChannel);
    }

    final Channel<List<? extends IN>, ?> inputChannel =
        JRoutineCore.io().apply(configuration).buildChannel();
    return inputChannel.bind(new DistributeChannelConsumer(mIsFlush, mPlaceholder, channelList));
  }

  /**
   * Channel consumer distributing list of data among a list of channels.
   *
   * @param <IN> the input data type.
   */
  private static class DistributeChannelConsumer<IN>
      implements ChannelConsumer<List<? extends IN>> {

    private final ArrayList<Channel<? extends IN, ?>> mChannels;

    private final boolean mIsFlush;

    private final IN mPlaceholder;

    /**
     * Constructor.
     *
     * @param isFlush     whether the inputs have to be flushed.
     * @param placeholder the placeholder instance.
     * @param channels    the list of channels.
     */
    private DistributeChannelConsumer(final boolean isFlush, @Nullable final IN placeholder,
        @NotNull final ArrayList<Channel<? extends IN, ?>> channels) {
      mIsFlush = isFlush;
      mChannels = channels;
      mPlaceholder = placeholder;
    }

    public void onComplete() {
      for (final Channel<? extends IN, ?> channel : mChannels) {
        channel.close();
      }
    }

    public void onError(@NotNull final RoutineException error) {
      for (final Channel<? extends IN, ?> channel : mChannels) {
        channel.abort(error);
      }
    }

    public void onOutput(final List<? extends IN> inputs) {
      final int inputSize = inputs.size();
      final ArrayList<Channel<? extends IN, ?>> channels = mChannels;
      final int size = channels.size();
      if (inputSize > size) {
        throw new IllegalArgumentException();
      }

      final IN placeholder = mPlaceholder;
      final boolean isFlush = mIsFlush;
      for (int i = 0; i < size; ++i) {
        @SuppressWarnings("unchecked") final Channel<IN, ?> channel =
            (Channel<IN, ?>) channels.get(i);
        if (i < inputSize) {
          channel.pass(inputs.get(i));

        } else if (isFlush) {
          channel.pass(placeholder);
        }
      }
    }
  }
}
