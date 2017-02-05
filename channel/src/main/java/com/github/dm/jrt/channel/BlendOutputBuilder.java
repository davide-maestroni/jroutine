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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation returning a channel blending data from a set of output channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class BlendOutputBuilder<OUT> extends AbstractChannelBuilder<OUT, OUT> {

  private final ArrayList<Channel<?, ? extends OUT>> mChannels;

  /**
   * Constructor.
   *
   * @param channels the channels to blend.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  BlendOutputBuilder(@NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
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

    mChannels = channelList;
  }

  @NotNull
  public Channel<OUT, OUT> buildChannel() {
    final Channel<OUT, OUT> outputChannel =
        JRoutineCore.<OUT>ofInputs().apply(getConfiguration()).buildChannel();
    for (final Channel<?, ? extends OUT> channel : mChannels) {
      channel.bind(outputChannel);
    }

    return outputChannel.close();
  }
}