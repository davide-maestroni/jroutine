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
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Channel consumer duplicating the output of one channel.
 * <p>
 * Created by davide-maestroni on 05/20/2017.
 */
class DuplicateChannelConsumer<OUT> implements ChannelConsumer<OUT> {

  private final List<Channel<OUT, ?>> mChannels;

  /**
   * Constructor.
   *
   * @param channels the list of output channels.
   */
  DuplicateChannelConsumer(@NotNull final List<Channel<OUT, ?>> channels) {
    mChannels = ConstantConditions.notNull("channel list", channels);
  }

  public void onComplete() {
    for (final Channel<OUT, ?> channel : mChannels) {
      channel.close();
    }
  }

  public void onError(@NotNull final RoutineException error) {
    for (final Channel<OUT, ?> channel : mChannels) {
      channel.abort(error);
    }
  }

  public void onOutput(final OUT output) {
    for (final Channel<OUT, ?> channel : mChannels) {
      channel.pass(output);
    }
  }
}
