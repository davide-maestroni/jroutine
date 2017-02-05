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

import com.github.dm.jrt.channel.builder.AbstractChannelListBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple builder of lists of channels.
 * <p>
 * Created by davide-maestroni on 02/05/2017.
 *
 * @param <DATA> the data type.
 */
class InstanceChannelListBuilder<DATA> extends AbstractChannelListBuilder<DATA, DATA> {

  private final int mCount;

  /**
   * Constructor.
   *
   * @param count the number of channels to build.
   */
  InstanceChannelListBuilder(final int count) {
    mCount = ConstantConditions.notNegative("channel count", count);
  }

  @NotNull
  public List<? extends Channel<DATA, DATA>> buildChannels() {
    final int count = mCount;
    final ArrayList<Channel<DATA, DATA>> list = new ArrayList<Channel<DATA, DATA>>(count);
    final ChannelBuilder<DATA, DATA> channelBuilder =
        JRoutineCore.<DATA>ofInputs().apply(getConfiguration());
    for (int i = 0; i < count; ++i) {
      list.add(channelBuilder.buildChannel());
    }

    return list;
  }
}
