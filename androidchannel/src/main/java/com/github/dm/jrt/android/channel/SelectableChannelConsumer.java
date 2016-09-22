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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Channel consumer transforming data into selectable ones.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class SelectableChannelConsumer<OUT, IN extends OUT> implements ChannelConsumer<IN> {

  private final Channel<? super
      ParcelableSelectable<OUT>, ?> mChannel;

  private final int mIndex;

  /**
   * Constructor.
   *
   * @param channel the selectable channel.
   * @param index   the selectable index.
   */
  SelectableChannelConsumer(@NotNull final Channel<? super
      ParcelableSelectable<OUT>, ?> channel, final int index) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mIndex = index;
  }

  @Override
  public void onComplete() {
    mChannel.close();
  }

  @Override
  public void onError(@NotNull final RoutineException error) {
    mChannel.abort(error);
  }

  @Override
  public void onOutput(final IN input) {
    mChannel.pass(new ParcelableSelectable<OUT>(input, mIndex));
  }
}
