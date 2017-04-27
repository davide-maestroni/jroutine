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
import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Channel consumer sorting flow inputs among a list of channels.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 *
 * @param <IN> the input data type.
 */
class SortingArrayChannelConsumer<IN> implements ChannelConsumer<Flow<? extends IN>> {

  private final ArrayList<Channel<? extends IN, ?>> mChannelList;

  private final int mSize;

  private final int mStartId;

  /**
   * Constructor.
   *
   * @param startId  the flow start ID.
   * @param channels the list of channels.
   */
  SortingArrayChannelConsumer(final int startId,
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
