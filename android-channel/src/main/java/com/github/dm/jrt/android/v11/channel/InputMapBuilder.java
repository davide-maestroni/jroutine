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

package com.github.dm.jrt.android.v11.channel;

import android.util.SparseArray;

import com.github.dm.jrt.android.channel.AndroidChannels;
import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.v11.channel.builder.AbstractChannelArrayBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * Builder implementation returning a map of channels accepting flow data.
 * <p>
 * Created by davide-maestroni on 06/18/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputMapBuilder<DATA, IN extends DATA> extends AbstractChannelArrayBuilder<IN, IN> {

  private final Channel<? super ParcelableFlow<DATA>, ?> mChannel;

  private final HashSet<Integer> mIds;

  /**
   * Constructor.
   *
   * @param channel the flow channel.
   * @param ids     the set of IDs.
   * @throws java.lang.NullPointerException if the specified set of IDs is null or contains a
   *                                        null object.
   */
  InputMapBuilder(@NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel,
      @NotNull final HashSet<Integer> ids) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    final HashSet<Integer> idSet =
        new HashSet<Integer>(ConstantConditions.notNull("set of IDs", ids));
    if (idSet.contains(null)) {
      throw new NullPointerException("the set of IDs must not contain null objects");
    }

    mIds = idSet;
  }

  @NotNull
  @Override
  public SparseArray<? extends Channel<IN, IN>> buildChannelArray() {
    final HashSet<Integer> ids = mIds;
    final Channel<? super ParcelableFlow<DATA>, ?> channel = mChannel;
    final SparseArray<Channel<IN, IN>> channelMap = new SparseArray<Channel<IN, IN>>(ids.size());
    final ChannelConfiguration configuration = getConfiguration();
    for (final Integer id : ids) {
      @SuppressWarnings("unchecked") final Channel<IN, IN> inputChannel =
          (Channel<IN, IN>) AndroidChannels.<DATA, IN>parcelableFlowInput(channel, id).withConfiguration(
              configuration).buildChannel();
      channelMap.put(id, inputChannel);
    }

    return channelMap;
  }
}
