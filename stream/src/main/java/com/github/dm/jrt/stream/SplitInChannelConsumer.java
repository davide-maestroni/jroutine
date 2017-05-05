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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Parallel by count channel consumer.
 * <p>
 * Created by davide-maestroni on 05/07/2016.
 *
 * @param <OUT> the output data type.
 */
class SplitInChannelConsumer<OUT> implements ChannelConsumer<OUT> {

  private final ArrayList<Channel<OUT, ?>> mChannels;

  private final Channel<OUT, ?>[] mInputs;

  private final Random mRandom = new Random();

  /**
   * Constructor.
   *
   * @param channels the map of input and invocation channels.
   */
  @SuppressWarnings("unchecked")
  SplitInChannelConsumer(@NotNull final List<? extends Channel<OUT, ?>> channels) {
    mChannels = new ArrayList<Channel<OUT, ?>>(channels);
    mInputs = new Channel[channels.size()];
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
    int count = 0;
    int minSize = Integer.MAX_VALUE;
    final Channel<OUT, ?>[] inputs = mInputs;
    for (final Channel<OUT, ?> channel : mChannels) {
      final int channelSize = channel.size();
      if (channelSize < minSize) {
        count = 1;
        inputs[0] = channel;
        minSize = channelSize;

      } else if (channelSize == minSize) {
        inputs[count++] = channel;
      }
    }

    final int i = (count == 1) ? 0 : Math.round((count - 1) * mRandom.nextFloat());
    inputs[i].pass(output);
  }
}
