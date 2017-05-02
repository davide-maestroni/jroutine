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

import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a channel handler.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 */
class DefaultChannelHandler implements ChannelHandler {

  private static final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer,
      Channel<?, ?>>>>
      sOutputChannels =
      new WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>>();

  private final ScheduledExecutor mExecutor;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultChannelHandler(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, OUT> blendOutputOf(@NotNull final Channel<?, ?>... channels) {
    if (channels.length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final Channel<OUT, OUT> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      outputChannel.pass((Channel<?, OUT>) channel);
    }

    return outputChannel.close();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, OUT> blendOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final Channel<OUT, OUT> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    boolean isEmpty = true;
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      isEmpty = false;
      outputChannel.pass((Channel<?, OUT>) channel);
    }

    if (isEmpty) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    return outputChannel.close();
  }

  @NotNull
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Callable<OUT> callable) {
    final InvocationConfiguration configuration =
        InvocationConfiguration.builderFromOutput(mConfiguration).configuration();
    return JRoutineCore.routineOn(mExecutor)
                       .withConfiguration(configuration)
                       .of(new CallableInvocation<OUT>(callable))
                       .invoke()
                       .close();
  }

  @NotNull
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future) {
    return channelOf(future, false);
  }

  @NotNull
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future,
      final boolean mayInterruptIfRunning) {
    return new FutureChannel<OUT>(mExecutor, mConfiguration, future, mayInterruptIfRunning);
  }

  @NotNull
  public <DATA> List<Channel<DATA, DATA>> channels(final int count) {
    ConstantConditions.notNegative("channel count", count);
    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final ArrayList<Channel<DATA, DATA>> list = new ArrayList<Channel<DATA, DATA>>(count);
    for (int i = 0; i < count; ++i) {
      list.add(channelBuilder.<DATA>ofType());
    }

    return list;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, OUT> concatOutputOf(@NotNull final Channel<?, ?>... channels) {
    if (channels.length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final Channel<OUT, OUT> outputChannel = JRoutineCore.channelOn(mExecutor)
                                                        .withChannel()
                                                        .withPatch(mConfiguration)
                                                        .withOrder(OrderType.SORTED)
                                                        .configuration()
                                                        .ofType();
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      outputChannel.pass((Channel<?, OUT>) channel);
    }

    return outputChannel.close();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, OUT> concatOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final Channel<OUT, OUT> outputChannel = JRoutineCore.channelOn(mExecutor)
                                                        .withChannel()
                                                        .withPatch(mConfiguration)
                                                        .withOrder(OrderType.SORTED)
                                                        .configuration()
                                                        .ofType();
    boolean isEmpty = true;
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      isEmpty = false;
      outputChannel.pass((Channel<?, OUT>) channel);
    }

    if (isEmpty) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    return outputChannel.close();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <IN> Channel<Flow<IN>, ?> inputFlowOf(@NotNull final Channel<? super IN, ?> channel,
      final int id) {
    final Channel<IN, IN> outputChannel = JRoutineCore.channel().ofType();
    ((Channel<IN, ?>) channel).pass(outputChannel);
    final Channel<Flow<IN>, Flow<IN>> inputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    return inputChannel.consume(new FilterChannelConsumer<IN>(outputChannel, id));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <DATA, IN extends DATA> Channel<IN, ?> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, final int id) {
    final Channel<Flow<DATA>, Flow<DATA>> flowChannel = JRoutineCore.channel().ofType();
    ((Channel<Flow<DATA>, ?>) channel).pass(flowChannel);
    final Channel<IN, IN> inputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    return inputChannel.consume(new FlowChannelConsumer<DATA, IN>(flowChannel, id));
  }

  @NotNull
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return internalFlowInput(channel, idSet);
  }

  @NotNull
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      if (id == null) {
        throw new NullPointerException("the set of ids must not contain null objects");
      }

      idSet.add(id);
    }

    return internalFlowInput(channel, idSet);
  }

  @NotNull
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(final int startId,
      final int rangeSize, @NotNull final Channel<? super Flow<DATA>, ?> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return internalFlowInput(channel, idSet);
  }

  @NotNull
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@NotNull final Channel<?, ?>... channels) {
    return internalJoinInput(false, null, channels);
  }

  @NotNull
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return internalJoinInput(true, placeholder, channels);
  }

  @NotNull
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return internalJoinInput(true, placeholder, channels);
  }

  @NotNull
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return internalJoinInput(false, null, channels);
  }

  @NotNull
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@NotNull final Channel<?, ?>... channels) {
    return internalJoinOutput(false, null, channels);
  }

  @NotNull
  public <OUT> Channel<?, List<OUT>> joinOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return internalJoinOutput(false, null, channels);
  }

  @NotNull
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return internalJoinOutput(true, placeholder, channels);
  }

  @NotNull
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return internalJoinOutput(true, placeholder, channels);
  }

  @NotNull
  public <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(
      @NotNull final Channel<?, ?>... channels) {
    return mergeInputOf(0, channels);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final ArrayList<Channel<? extends IN, ?>> channelList =
        new ArrayList<Channel<? extends IN, ?>>(length);
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) channel).pass(outputChannel);
      channelList.add(outputChannel);
    }

    final Channel<Flow<? extends IN>, ?> inputChannel = channelBuilder.ofType();
    return inputChannel.consume(new SortingArrayChannelConsumer(startId, channelList));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final ArrayList<Channel<? extends IN, ?>> channelList =
        new ArrayList<Channel<? extends IN, ?>>();
    for (final Channel<? extends IN, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) channel).pass(outputChannel);
      channelList.add(outputChannel);
    }

    if (channelList.isEmpty()) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    final Channel<Flow<? extends IN>, ?> inputChannel = channelBuilder.ofType();
    return inputChannel.consume(new SortingArrayChannelConsumer(startId, channelList));
  }

  @NotNull
  public <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mergeInputOf(0, channels);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(
      @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
    if (channels.isEmpty()) {
      throw new IllegalArgumentException("the map of channels must not be empty");
    }

    if (channels.containsValue(null)) {
      throw new NullPointerException("the map of channels must not contain null objects");
    }

    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final HashMap<Integer, Channel<IN, ?>> inputChannelMap =
        new HashMap<Integer, Channel<IN, ?>>(channels.size());
    for (final Entry<Integer, ? extends Channel<? extends IN, ?>> entry : channels.entrySet()) {
      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) entry.getValue()).pass(outputChannel);
      inputChannelMap.put(entry.getKey(), outputChannel);
    }

    final Channel<Flow<? extends IN>, Flow<? extends IN>> inputChannel = channelBuilder.ofType();
    return inputChannel.consume(new SortingMapChannelConsumer<IN>(inputChannelMap));
  }

  @NotNull
  public <OUT> Channel<?, Flow<OUT>> mergeOutputOf(@NotNull final Channel<?, ?>... channels) {
    return mergeOutputOf(0, channels);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, Flow<OUT>> mergeOutputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    if (channels.length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    int i = startId;
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      outputChannel.pass(outputFlowOf((Channel<?, OUT>) channel, i++));
    }

    return outputChannel.close();
  }

  @NotNull
  public <OUT> Channel<?, Flow<OUT>> mergeOutputOf(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    int i = startId;
    boolean isEmpty = true;
    for (final Channel<?, ? extends OUT> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      isEmpty = false;
      outputChannel.pass(outputFlowOf(channel, i++));
    }

    if (isEmpty) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    return outputChannel.close();
  }

  @NotNull
  public <OUT> Channel<?, Flow<OUT>> mergeOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mergeOutputOf(0, channels);
  }

  @NotNull
  public <OUT> Channel<?, Flow<OUT>> mergeOutputOf(
      @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
    if (channels.isEmpty()) {
      throw new IllegalArgumentException("the map of channels must not be empty");
    }

    if (channels.containsValue(null)) {
      throw new NullPointerException("the map of channels must not contain null objects");
    }

    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    for (final Entry<Integer, ? extends Channel<?, ? extends OUT>> entry : channels.entrySet()) {
      outputChannel.pass(outputFlowOf(entry.getValue(), entry.getKey()));
    }

    return outputChannel.close();
  }

  @NotNull
  public <OUT> Channel<?, Flow<OUT>> outputFlowOf(@NotNull final Channel<?, ? extends OUT> channel,
      final int id) {
    final Channel<Flow<OUT>, Flow<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    channel.consume(new FlowChannelConsumer<OUT, OUT>(outputChannel, id));
    return outputChannel;
  }

  @NotNull
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel, @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return internalFlowOutput(channel, idSet);
  }

  @NotNull
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      if (id == null) {
        throw new NullPointerException("the set of ids must not contain null objects");
      }

      idSet.add(id);
    }

    return internalFlowOutput(channel, idSet);
  }

  @NotNull
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(final int startId, final int rangeSize,
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return internalFlowOutput(channel, idSet);
  }

  @NotNull
  public <OUT> Channel<?, OUT> replayOutputOf(@NotNull final Channel<?, OUT> channel) {
    return new ReplayOutputChannel<OUT>(mExecutor, mConfiguration, channel);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <OUT> Map<Integer, Channel<?, OUT>> internalFlowOutput(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel,
      @NotNull final HashSet<Integer> ids) {
    synchronized (sOutputChannels) {
      final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>>
          outputChannels = sOutputChannels;
      HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>> channelMaps = outputChannels.get(channel);
      if (channelMaps == null) {
        channelMaps = new HashMap<FlowInfo, HashMap<Integer, Channel<?, ?>>>();
        outputChannels.put(channel, channelMaps);
      }

      final int size = ids.size();
      final ScheduledExecutor executor = mExecutor;
      final ChannelConfiguration configuration = mConfiguration;
      final FlowInfo flowInfo = new FlowInfo(executor, configuration, ids);
      final HashMap<Integer, Channel<?, OUT>> channelMap =
          new HashMap<Integer, Channel<?, OUT>>(size);
      HashMap<Integer, Channel<?, ?>> channels = channelMaps.get(flowInfo);
      if (channels != null) {
        for (final Entry<Integer, Channel<?, ?>> entry : channels.entrySet()) {
          channelMap.put(entry.getKey(), (Channel<OUT, OUT>) entry.getValue());
        }

      } else {
        final ChannelBuilder channelBuilder =
            JRoutineCore.channelOn(executor).withConfiguration(configuration);
        final HashMap<Integer, Channel<OUT, ?>> inputMap =
            new HashMap<Integer, Channel<OUT, ?>>(size);
        channels = new HashMap<Integer, Channel<?, ?>>(size);
        for (final Integer id : ids) {
          final Channel<OUT, OUT> outputChannel = channelBuilder.ofType();
          inputMap.put(id, outputChannel);
          channelMap.put(id, outputChannel);
          channels.put(id, outputChannel);
        }

        channel.consume(new SortingMapChannelConsumer<OUT>(inputMap));
        channelMaps.put(flowInfo, channels);
      }

      return channelMap;
    }
  }

  @NotNull
  public Builder<? extends ChannelHandler> withChannel() {
    return new Builder<ChannelHandler>(this, mConfiguration);
  }

  @NotNull
  public ChannelHandler withConfiguration(@NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }

  @NotNull
  private <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> internalFlowInput(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final HashSet<Integer> ids) {
    final HashMap<Integer, Channel<IN, ?>> channelMap =
        new HashMap<Integer, Channel<IN, ?>>(ids.size());
    for (final Integer id : ids) {
      final Channel<IN, ?> inputChannel = inputOfFlow(channel, id);
      channelMap.put(id, inputChannel);
    }

    return channelMap;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <IN> Channel<List<? extends IN>, ?> internalJoinInput(final boolean isFlush,
      @Nullable final IN placeholder, @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final ArrayList<Channel<?, ?>> channelList = new ArrayList<Channel<?, ?>>(length);
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) channel).pass(outputChannel);
      channelList.add(outputChannel);
    }

    final Channel<List<? extends IN>, ?> inputChannel = channelBuilder.ofType();
    return inputChannel.consume(new DistributeChannelConsumer(channelList, isFlush, placeholder));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <IN> Channel<List<? extends IN>, ?> internalJoinInput(final boolean isFlush,
      @Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final ArrayList<Channel<?, ?>> channelList = new ArrayList<Channel<?, ?>>();
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) channel).pass(outputChannel);
      channelList.add(outputChannel);
    }

    if (channelList.isEmpty()) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    final Channel<List<? extends IN>, ?> inputChannel = channelBuilder.ofType();
    return inputChannel.consume(new DistributeChannelConsumer(channelList, isFlush, placeholder));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> Channel<?, List<OUT>> internalJoinOutput(final boolean isFlush,
      @Nullable final OUT placeholder,
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

    final int size = channelList.size();
    final boolean[] closed = new boolean[size];
    @SuppressWarnings("unchecked") final SimpleQueue<OUT>[] queues = new SimpleQueue[size];
    for (int i = 0; i < size; ++i) {
      queues[i] = new SimpleQueue<OUT>();
    }

    int i = 0;
    final ChannelConfiguration configuration = mConfiguration;
    final Channel<List<OUT>, List<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(configuration).ofType();
    final Backoff backoff = configuration.getBackoffOrElse(null);
    final int maxSize = configuration.getMaxSizeOrElse(Integer.MAX_VALUE);
    final Object mutex = new Object();
    for (final Channel<?, ?> channel : channels) {
      ((Channel<?, OUT>) channel).consume(
          new JoinChannelConsumer<OUT>(outputChannel, mutex, closed, queues, backoff, maxSize, i++,
              isFlush, placeholder));
    }

    return outputChannel;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> Channel<?, List<OUT>> internalJoinOutput(final boolean isFlush,
      @Nullable final OUT placeholder, @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final boolean[] closed = new boolean[length];
    @SuppressWarnings("unchecked") final SimpleQueue<OUT>[] queues = new SimpleQueue[length];
    for (int i = 0; i < length; ++i) {
      queues[i] = new SimpleQueue<OUT>();
    }

    int i = 0;
    final ChannelConfiguration configuration = mConfiguration;
    final Channel<List<OUT>, List<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(configuration).ofType();
    final Backoff backoff = configuration.getBackoffOrElse(null);
    final int maxSize = configuration.getMaxSizeOrElse(Integer.MAX_VALUE);
    final Object mutex = new Object();
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      ((Channel<?, OUT>) channel).consume(
          new JoinChannelConsumer<OUT>(outputChannel, mutex, closed, queues, backoff, maxSize, i++,
              isFlush, placeholder));
    }

    return outputChannel;
  }

  /**
   * Invocation backed by a Callable instance.
   *
   * @param <OUT> the output data type.
   */
  private static class CallableInvocation<OUT> extends CommandInvocation<OUT> {

    private final Callable<OUT> mCallable;

    /**
     * Constructor.
     *
     * @param callable the Callable instance.
     */
    private CallableInvocation(@NotNull final Callable<OUT> callable) {
      super(asArgs(ConstantConditions.notNull("callable instance", callable)));
      mCallable = callable;
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      result.pass(mCallable.call());
    }
  }

  /**
   * Class used as key to identify a specific map of output channels.
   */
  private static class FlowInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param executor      the executor instance.
     * @param configuration the channel configuration.
     * @param ids           the set of IDs.
     */
    private FlowInfo(@NotNull final ScheduledExecutor executor,
        @NotNull final ChannelConfiguration configuration, @NotNull final HashSet<Integer> ids) {
      super(asArgs(executor, configuration, ids));
    }
  }
}
