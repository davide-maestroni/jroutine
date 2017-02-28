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
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation returning a channel merging data from a collection of channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class MergeInputBuilder<IN> extends AbstractChannelBuilder<Flow<? extends IN>, Flow<? extends IN>> {

  private final ArrayList<Channel<? extends IN, ?>> mChannels;

  private final int mStartId;

  /**
   * Constructor.
   *
   * @param startId  the flow start ID.
   * @param channels the channels to merge.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  MergeInputBuilder(final int startId,
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

    mStartId = startId;
    mChannels = channelList;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Channel<Flow<? extends IN>, Flow<? extends IN>> buildChannel() {
    final ArrayList<Channel<? extends IN, ?>> channels = mChannels;
    final ArrayList<Channel<? extends IN, ?>> channelList =
        new ArrayList<Channel<? extends IN, ?>>(channels.size());
    final ChannelConfiguration configuration = getConfiguration();
    for (final Channel<? extends IN, ?> channel : channels) {
      final Channel<IN, IN> outputChannel =
          JRoutineCore.<IN>ofData().apply(configuration).buildChannel();
      ((Channel<IN, ?>) channel).pass(outputChannel);
      channelList.add(outputChannel);
    }

    final Channel<Flow<? extends IN>, ?> inputChannel =
        JRoutineCore.<Flow<? extends IN>>ofData().apply(configuration).buildChannel();
    return inputChannel.consume(new SortingArrayChannelConsumer(mStartId, channelList));
  }

  /**
   * Channel consumer sorting flow inputs among a list of channels.
   */
  private static class SortingArrayChannelConsumer<IN>
      implements ChannelConsumer<Flow<? extends IN>> {

    private final ArrayList<Channel<? extends IN, ?>> mChannelList;

    private final int mSize;

    private final int mStartId;

    /**
     * Constructor.
     *
     * @param startId  the flow start ID.
     * @param channels the list of channels.
     */
    private SortingArrayChannelConsumer(final int startId,
        @NotNull final ArrayList<Channel<? extends IN, ?>> channels) {
      mStartId = startId;
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

    public void onOutput(final Flow<? extends IN> flow) {
      final int id = flow.id - mStartId;
      if ((id < 0) || (id >= mSize)) {
        return;
      }

      @SuppressWarnings("unchecked") final Channel<IN, ?> channel =
          (Channel<IN, ?>) mChannelList.get(id);
      if (channel != null) {
        channel.pass(flow.data);
      }
    }
  }
}
