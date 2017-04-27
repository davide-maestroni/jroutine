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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Channel consumer distributing list of data among a list of channels.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 *
 * @param <IN> the input data type.
 */
class DistributeChannelConsumer<IN> implements ChannelConsumer<List<? extends IN>> {

  private final ArrayList<Channel<? extends IN, ?>> mChannels;

  private final boolean mIsFlush;

  private final IN mPlaceholder;

  /**
   * Constructor.
   *
   * @param channels    the list of channels.
   * @param isFlush     whether the inputs have to be flushed.
   * @param placeholder the placeholder instance.
   */
  DistributeChannelConsumer(@NotNull final ArrayList<Channel<? extends IN, ?>> channels,
      final boolean isFlush, @Nullable final IN placeholder) {
    mChannels = ConstantConditions.notNull("channel list", channels);
    mIsFlush = isFlush;
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
