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
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.v11.channel.builder.AbstractChannelArrayBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * Builder implementation returning a map of channels accepting selectable data.
 * <p>
 * Created by davide-maestroni on 06/18/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class InputMapBuilder<DATA, IN extends DATA> extends AbstractChannelArrayBuilder<IN, IN> {

  private final Channel<? super ParcelableSelectable<DATA>, ?> mChannel;

  private final HashSet<Integer> mIndexes;

  /**
   * Constructor.
   *
   * @param channel the selectable channel.
   * @param indexes the set of indexes.
   * @throws java.lang.NullPointerException if the specified set of indexes is null or contains a
   *                                        null object.
   */
  InputMapBuilder(@NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
      @NotNull final HashSet<Integer> indexes) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    final HashSet<Integer> indexSet =
        new HashSet<Integer>(ConstantConditions.notNull("set of indexes", indexes));
    if (indexSet.contains(null)) {
      throw new NullPointerException("the set of indexes must not contain null objects");
    }

    mIndexes = indexSet;
  }

  @NotNull
  @Override
  public SparseArray<? extends Channel<IN, IN>> buildChannelArray() {
    final HashSet<Integer> indexes = mIndexes;
    final Channel<? super ParcelableSelectable<DATA>, ?> channel = mChannel;
    final SparseArray<Channel<IN, IN>> channelMap =
        new SparseArray<Channel<IN, IN>>(indexes.size());
    final ChannelConfiguration configuration = getConfiguration();
    for (final Integer index : indexes) {
      @SuppressWarnings("unchecked") final Channel<IN, IN> inputChannel =
          (Channel<IN, IN>) AndroidChannels.<DATA, IN>selectInputParcelable(channel, index).apply(
              configuration).buildChannel();
      channelMap.put(index, inputChannel);
    }

    return channelMap;
  }
}
