package com.github.dm.jrt.channel;

import com.github.dm.jrt.channel.builder.AbstractChannelMapBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder implementation returning a map of channels accepting flow data.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <DATA> the channel data type.
 * @param <IN>   the input data type.
 */
class FlowInputMapBuilder<DATA, IN extends DATA>
    extends AbstractChannelMapBuilder<Integer, IN, IN> {

  private final Channel<? super Flow<DATA>, ?> mChannel;

  private final HashSet<Integer> mIds;

  /**
   * Constructor.
   *
   * @param channel the flow channel.
   * @param ids     the set of IDs.
   * @throws java.lang.NullPointerException if the specified set of ids is null or contains a
   *                                        null object.
   */
  FlowInputMapBuilder(@NotNull final Channel<? super Flow<DATA>, ?> channel,
      @NotNull final Set<Integer> ids) {
    mChannel = ConstantConditions.notNull("channel instance", channel);
    final HashSet<Integer> idSet =
        new HashSet<Integer>(ConstantConditions.notNull("set of ids", ids));
    if (idSet.contains(null)) {
      throw new NullPointerException("the set of ids must not contain null objects");
    }

    mIds = idSet;
  }

  @NotNull
  public Map<Integer, ? extends Channel<IN, IN>> buildChannelMap() {
    final HashSet<Integer> ids = mIds;
    final Channel<? super Flow<DATA>, ?> channel = mChannel;
    final HashMap<Integer, Channel<IN, IN>> channelMap =
        new HashMap<Integer, Channel<IN, IN>>(ids.size());
    final ChannelConfiguration configuration = getConfiguration();
    for (final Integer id : ids) {
      final Channel<IN, IN> inputChannel =
          new FlowInputBuilder<DATA, IN>(channel, id).withConfiguration(configuration).buildChannel();
      channelMap.put(id, inputChannel);
    }

    return channelMap;
  }
}
