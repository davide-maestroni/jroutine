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
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel passing flow data to an input one.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class FlowInputBuilder<DATA, IN extends DATA> extends AbstractChannelBuilder<IN, IN> {

  private final Channel<? super ParcelableFlow<DATA>, ?> mChannel;

  private final int mId;

  /**
   * Constructor.
   *
   * @param channel the input channel.
   * @param id      the flow ID.
   */
  FlowInputBuilder(@NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel, final int id) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mId = id;
  }

  @NotNull
  @Override
  public Channel<IN, IN> buildChannel() {
    final Channel<IN, IN> inputChannel =
        JRoutineCore.<IN>ofInputs().apply(getConfiguration()).buildChannel();
    final Channel<ParcelableFlow<DATA>, ParcelableFlow<DATA>> outputChannel =
        JRoutineCore.<ParcelableFlow<DATA>>ofInputs().buildChannel();
    outputChannel.bind(mChannel);
    return inputChannel.bind(new FlowChannelConsumer<DATA, IN>(outputChannel, mId));
  }
}