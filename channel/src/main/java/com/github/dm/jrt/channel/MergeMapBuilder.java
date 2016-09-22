package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;

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
class MergeMapBuilder<OUT> extends AbstractBuilder<Channel<?, Selectable<OUT>>> {

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
  @Override
  protected Channel<?, Selectable<OUT>> build(@NotNull final ChannelConfiguration configuration) {
    final Channel<Selectable<OUT>, Selectable<OUT>> outputChannel =
        JRoutineCore.io().apply(configuration).buildChannel();
    for (final Entry<Integer, ? extends Channel<?, ? extends OUT>> entry : mChannelMap.entrySet()) {
      outputChannel.pass(
          new SelectableOutputBuilder<OUT>(entry.getValue(), entry.getKey()).buildChannels());
    }

    return outputChannel.close();
  }
}
