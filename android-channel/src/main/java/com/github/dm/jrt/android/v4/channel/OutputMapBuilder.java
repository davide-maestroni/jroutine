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

package com.github.dm.jrt.android.v4.channel;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.v4.channel.builder.AbstractChannelArrayCompatBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Builder implementation returning a map of channels returning flow output data.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class OutputMapBuilder<OUT> extends AbstractChannelArrayCompatBuilder<OUT, OUT> {

  private static final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo,
      SparseArrayCompat<Channel<?, ?>>>>
      sOutputChannels =
      new WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>>();

  private final Channel<?, ? extends ParcelableFlow<? extends OUT>> mChannel;

  private final HashSet<Integer> mIds;

  /**
   * Constructor.
   *
   * @param channel the flow channel.
   * @param ids     the set of IDs.
   * @throws java.lang.NullPointerException if the specified set of IDs is null or contains a
   *                                        null object.
   */
  OutputMapBuilder(@NotNull final Channel<?, ? extends ParcelableFlow<? extends OUT>> channel,
      @NotNull final Set<Integer> ids) {
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
  @SuppressWarnings("unchecked")
  public SparseArrayCompat<? extends Channel<OUT, OUT>> buildChannelArray() {
    final HashSet<Integer> ids = mIds;
    final Channel<?, ? extends ParcelableFlow<? extends OUT>> channel = mChannel;
    synchronized (sOutputChannels) {
      final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>>
          outputChannels = sOutputChannels;
      HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>> channelMaps = outputChannels.get(channel);
      if (channelMaps == null) {
        channelMaps = new HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>();
        outputChannels.put(channel, channelMaps);
      }

      final int size = ids.size();
      final ChannelConfiguration configuration = getConfiguration();
      final FlowInfo flowInfo = new FlowInfo(configuration, ids);
      final SparseArrayCompat<Channel<OUT, OUT>> channelMap =
          new SparseArrayCompat<Channel<OUT, OUT>>(size);
      SparseArrayCompat<Channel<?, ?>> channels = channelMaps.get(flowInfo);
      if (channels != null) {
        final int channelSize = channels.size();
        for (int i = 0; i < channelSize; ++i) {
          channelMap.append(channels.keyAt(i), (Channel<OUT, OUT>) channels.valueAt(i));
        }

      } else {
        final SparseArrayCompat<Channel<OUT, ?>> inputMap =
            new SparseArrayCompat<Channel<OUT, ?>>(size);
        channels = new SparseArrayCompat<Channel<?, ?>>(size);
        for (final Integer id : ids) {
          final Channel<OUT, OUT> outputChannel =
              JRoutineCore.<OUT>ofData().apply(configuration).buildChannel();
          inputMap.append(id, outputChannel);
          channelMap.append(id, outputChannel);
          channels.append(id, outputChannel);
        }

        channel.consume(new SortingMapChannelConsumer<OUT>(inputMap));
        channelMaps.put(flowInfo, channels);
      }

      return channelMap;
    }
  }

  /**
   * Class used as key to identify a specific map of output channels.
   */
  private static class FlowInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param ids           the set of IDs.
     */
    private FlowInfo(@NotNull final ChannelConfiguration configuration,
        @NotNull final HashSet<Integer> ids) {
      super(asArgs(configuration, ids));
    }
  }
}
