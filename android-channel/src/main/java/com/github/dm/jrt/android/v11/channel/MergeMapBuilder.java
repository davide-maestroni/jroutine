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
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation returning a channel merging data from a map of output channels.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeMapBuilder<OUT>
    extends AbstractChannelBuilder<ParcelableSelectable<OUT>, ParcelableSelectable<OUT>> {

  private final SparseArray<? extends Channel<?, ? extends OUT>> mChannelMap;

  /**
   * Constructor.
   *
   * @param channels the map of channels to merge.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  MergeMapBuilder(@NotNull final SparseArray<? extends Channel<?, ? extends OUT>> channels) {
    if (channels.size() == 0) {
      throw new IllegalArgumentException("the map of channels must not be empty");
    }

    final SparseArray<? extends Channel<?, ? extends OUT>> channelMap = channels.clone();
    if (channelMap.indexOfValue(null) >= 0) {
      throw new NullPointerException("the map of channels must not contain null objects");
    }

    mChannelMap = channelMap;
  }

  @NotNull
  @Override
  public Channel<ParcelableSelectable<OUT>, ParcelableSelectable<OUT>> buildChannel() {
    final SparseArray<? extends Channel<?, ? extends OUT>> channelMap = mChannelMap;
    final Channel<ParcelableSelectable<OUT>, ParcelableSelectable<OUT>> outputChannel =
        JRoutineCore.<ParcelableSelectable<OUT>>ofInputs().apply(getConfiguration()).buildChannel();
    final int size = channelMap.size();
    for (int i = 0; i < size; ++i) {
      outputChannel.pass(
          AndroidChannels.selectableOutputParcelable(channelMap.valueAt(i), channelMap.keyAt(i))
                         .buildChannel());
    }

    return outputChannel.close();
  }
}
