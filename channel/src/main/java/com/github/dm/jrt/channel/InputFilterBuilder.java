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
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a flow channel feeding an input one.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN> the input data type.
 */
class InputFilterBuilder<IN> extends AbstractChannelBuilder<Flow<IN>, Flow<IN>> {

  private final Channel<? super IN, ?> mChannel;

  private final int mId;

  /**
   * Constructor.
   *
   * @param channel the channel.
   * @param id      the flow ID.
   */
  InputFilterBuilder(@NotNull final Channel<? super IN, ?> channel, final int id) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mId = id;
  }

  @NotNull
  public Channel<Flow<IN>, Flow<IN>> buildChannel() {
    final Channel<Flow<IN>, Flow<IN>> inputChannel =
        JRoutineCore.<Flow<IN>>ofInputs().apply(getConfiguration()).buildChannel();
    final Channel<IN, IN> outputChannel = JRoutineCore.<IN>ofInputs().buildChannel();
    outputChannel.pipe(mChannel);
    return inputChannel.consume(new FilterChannelConsumer<IN>(outputChannel, mId));
  }

  /**
   * Channel consumer filtering flow data.
   *
   * @param <IN> the input data type.
   */
  private static class FilterChannelConsumer<IN> implements ChannelConsumer<Flow<IN>> {

    private final Channel<? super IN, ?> mChannel;

    private final int mId;

    /**
     * Constructor.
     *
     * @param channel the channel to feed.
     * @param id      the ID to filter.
     */
    private FilterChannelConsumer(@NotNull final Channel<? super IN, ?> channel, final int id) {
      mChannel = channel;
      mId = id;
    }

    public void onComplete() {
      mChannel.close();
    }

    public void onError(@NotNull final RoutineException error) {
      mChannel.abort(error);
    }

    public void onOutput(final Flow<IN> flow) {
      if (flow.id == mId) {
        mChannel.pass(flow.data);
      }
    }
  }
}
