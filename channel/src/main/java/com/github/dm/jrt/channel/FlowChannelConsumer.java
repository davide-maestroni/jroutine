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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Channel consumer transforming data into flow ones.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class FlowChannelConsumer<OUT, IN extends OUT> implements ChannelConsumer<IN> {

  private final Channel<? super FlowData<OUT>, ?> mChannel;

  private final int mId;

  /**
   * Constructor.
   *
   * @param channel the flow channel.
   * @param id      the flow ID.
   */
  FlowChannelConsumer(@NotNull final Channel<? super FlowData<OUT>, ?> channel, final int id) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mId = id;
  }

  public void onComplete() {
    mChannel.close();
  }

  public void onError(@NotNull final RoutineException error) {
    mChannel.abort(error);
  }

  public void onOutput(final IN input) {
    mChannel.pass(new FlowData<OUT>(mId, input));
  }
}
