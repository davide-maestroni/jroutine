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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel making an output one selectable.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class SelectableOutputBuilder<OUT> extends AbstractBuilder<Channel<?, Selectable<OUT>>> {

  private final Channel<?, ? extends OUT> mChannel;

  private final int mIndex;

  /**
   * Constructor.
   *
   * @param channel the channel.
   * @param index   the selectable index.
   */
  SelectableOutputBuilder(@NotNull final Channel<?, ? extends OUT> channel, final int index) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    mIndex = index;
  }

  @NotNull
  @Override
  protected Channel<?, Selectable<OUT>> build(@NotNull final ChannelConfiguration configuration) {
    final Channel<Selectable<OUT>, Selectable<OUT>> outputChannel =
        JRoutineCore.<Selectable<OUT>>ofInputs().apply(configuration).buildChannel();
    mChannel.bind(new SelectableChannelConsumer<OUT, OUT>(outputChannel, mIndex));
    return outputChannel;
  }
}
