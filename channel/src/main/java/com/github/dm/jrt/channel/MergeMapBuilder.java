package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder implementation returning a channel merging data from a map of output channels.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeMapBuilder<OUT> extends AbstractChannelBuilder<Flow<OUT>, Flow<OUT>> {

  private final HashMap<Integer, Channel<?, ? extends OUT>> mChannelMap;

  /**
   * Constructor.
   *
   * @param channels the map of channels to merge.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  MergeMapBuilder(@NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
    if (channels.isEmpty()) {
      throw new IllegalArgumentException("the map of channels must not be empty");
    }

    final HashMap<Integer, Channel<?, ? extends OUT>> channelMap =
        new HashMap<Integer, Channel<?, ? extends OUT>>(channels);
    if (channelMap.containsValue(null)) {
      throw new NullPointerException("the map of channels must not contain null objects");
    }

    mChannelMap = channelMap;
  }

  @NotNull
  public Channel<Flow<OUT>, Flow<OUT>> buildChannel() {
    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.<Flow<OUT>>ofInputs().apply(getConfiguration()).buildChannel();
    for (final Entry<Integer, ? extends Channel<?, ? extends OUT>> entry : mChannelMap.entrySet()) {
      outputChannel.pass(
          new OutputFlowBuilder<OUT>(entry.getValue(), entry.getKey()).buildChannel());
    }

    return outputChannel.close();
  }
}
