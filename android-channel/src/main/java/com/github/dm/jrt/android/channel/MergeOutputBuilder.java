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

package com.github.dm.jrt.android.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation merging data from a set of output channels into a flow one.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeOutputBuilder<OUT>
    extends AbstractChannelBuilder<ParcelableFlow<OUT>, ParcelableFlow<OUT>> {

  private final ArrayList<Channel<?, ? extends OUT>> mChannels;

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
  MergeOutputBuilder(final int startId,
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

    mStartId = startId;
    mChannels = channelList;
  }

  @NotNull
  @Override
  public Channel<ParcelableFlow<OUT>, ParcelableFlow<OUT>> buildChannel() {
    final Channel<ParcelableFlow<OUT>, ParcelableFlow<OUT>> outputChannel =
        JRoutineCore.<ParcelableFlow<OUT>>ofInputs().apply(getConfiguration()).buildChannel();
    int i = mStartId;
    for (final Channel<?, ? extends OUT> channel : mChannels) {
      outputChannel.pass(AndroidChannels.outputParcelableFlow(channel, i++).buildChannel());
    }

    return outputChannel.close();
  }
}
