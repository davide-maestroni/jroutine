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

import com.github.dm.jrt.channel.builder.AbstractChannelMapBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Builder implementation returning a map of channels returning flow output data.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class OutputMapBuilder<OUT> extends AbstractChannelMapBuilder<Integer, OUT, OUT> {

  private static final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer,
      Channel<?, ?>>>>
      sOutputChannels =
      new WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>>();

  private final Channel<?, ? extends Flow<? extends OUT>> mChannel;

  private final HashSet<Integer> mIds;

  /**
   * Constructor.
   *
   * @param channel the flow channel.
   * @param ids     the set of IDs.
   * @throws java.lang.NullPointerException if the specified set of IDs is null or contains a
   *                                        null object.
   */
  OutputMapBuilder(@NotNull final Channel<?, ? extends Flow<? extends OUT>> channel,
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
  @SuppressWarnings("unchecked")
  public Map<Integer, ? extends Channel<OUT, OUT>> buildChannelMap() {
    final HashSet<Integer> ids = mIds;
    final Channel<?, ? extends Flow<? extends OUT>> channel = mChannel;
    synchronized (sOutputChannels) {
      final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>>
          outputChannels = sOutputChannels;
      HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>> channelMaps = outputChannels.get(channel);
      if (channelMaps == null) {
        channelMaps = new HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>();
        outputChannels.put(channel, channelMaps);
      }

      final int size = ids.size();
      final ChannelConfiguration configuration = getConfiguration();
      final FlowInfo flowInfo = new FlowInfo(configuration, ids);
      final HashMap<Integer, Channel<OUT, OUT>> channelMap =
          new HashMap<Integer, Channel<OUT, OUT>>(size);
      HashMap<Integer, Channel<?, ?>> channels = channelMaps.get(flowInfo);
      if (channels != null) {
        for (final Entry<Integer, Channel<?, ?>> entry : channels.entrySet()) {
          channelMap.put(entry.getKey(), (Channel<OUT, OUT>) entry.getValue());
        }

      } else {
        final HashMap<Integer, Channel<OUT, ?>> inputMap =
            new HashMap<Integer, Channel<OUT, ?>>(size);
        channels = new HashMap<Integer, Channel<?, ?>>(size);
        for (final Integer id : ids) {
          final Channel<OUT, OUT> outputChannel =
              JRoutineCore.<OUT>ofInputs().apply(configuration).buildChannel();
          inputMap.put(id, outputChannel);
          channelMap.put(id, outputChannel);
          channels.put(id, outputChannel);
        }

        channel.bind(new SortingMapChannelConsumer<OUT>(inputMap));
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
