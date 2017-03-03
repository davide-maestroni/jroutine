package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Builder implementation merging data from a set of output channels into a flow one.
 * <p>
 * Created by davide-maestroni on 05/03/2016.
 *
 * @param <OUT> the output data type.
 */
class MergeOutputBuilder<OUT> extends AbstractChannelBuilder<Flow<OUT>, Flow<OUT>> {

  private final ArrayList<Channel<?, ? extends OUT>> mChannels;

  private final int mStartId;

  /**
   * Constructor.
   *
   * @param startId  the flow start ID.
   * @param channels the channels to merge.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  MergeOutputBuilder(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final ArrayList<Channel<?, ? extends OUT>> channelList =
        new ArrayList<Channel<?, ? extends OUT>>();
    for (final Channel<?, ? extends OUT> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      channelList.add(channel);
    }

    if (channelList.isEmpty()) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    mStartId = startId;
    mChannels = channelList;
  }

  @NotNull
  public Channel<Flow<OUT>, Flow<OUT>> buildChannel() {
    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.<Flow<OUT>>ofData().apply(getConfiguration()).buildChannel();
    int i = mStartId;
    for (final Channel<?, ? extends OUT> channel : mChannels) {
      outputChannel.pass(new OutputFlowBuilder<OUT>(channel, i++).buildChannel());
    }

    return outputChannel.close();
  }
}
