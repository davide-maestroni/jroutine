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

package com.github.dm.jrt.android.v4.channel;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.android.channel.JRoutineAndroidChannels;
import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.android.channel.builder.AndroidChannelHandler;
import com.github.dm.jrt.android.v4.channel.builder.SparseChannelHandlerCompat;
import com.github.dm.jrt.channel.FlowData;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.github.dm.jrt.core.JRoutineCore.readOnly;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a sparse array channel handler.
 * <p>
 * Created by davide-maestroni on 05/18/2017.
 */
class DefaultSparseChannelHandlerCompat implements SparseChannelHandlerCompat {

  private static final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo,
      SparseArrayCompat<Channel<?, ?>>>>
      sOutputChannels =
      new WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>>();

  private final AndroidChannelHandler mChannelHandler;

  private final ScheduledExecutor mExecutor;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  private final Configurable<SparseChannelHandlerCompat> mConfigurable =
      new Configurable<SparseChannelHandlerCompat>() {

        @NotNull
        @Override
        public SparseChannelHandlerCompat withConfiguration(
            @NotNull final ChannelConfiguration configuration) {
          return DefaultSparseChannelHandlerCompat.this.withConfiguration(configuration);
        }
      };

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultSparseChannelHandlerCompat(@NotNull final ScheduledExecutor executor) {
    mChannelHandler = JRoutineAndroidChannels.channelHandlerOn(executor);
    mExecutor = executor;
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> blendOutputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).blendOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> blendOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).blendOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Callable<OUT> callable) {
    return mChannelHandler.withConfiguration(mConfiguration).channelOf(callable);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future) {
    return mChannelHandler.withConfiguration(mConfiguration).channelOf(future);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future,
      final boolean mayInterruptIfRunning) {
    return mChannelHandler.withConfiguration(mConfiguration)
                          .channelOf(future, mayInterruptIfRunning);
  }

  @NotNull
  @Override
  public <DATA> List<Channel<DATA, DATA>> channels(final int count) {
    return mChannelHandler.withConfiguration(mConfiguration).channels(count);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> concatOutputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).concatOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> concatOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).concatOutputOf(channels);
  }

  @NotNull
  @Override
  public <IN> Channel<IN, ?> duplicateInputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.duplicateInputOf(channels);
  }

  @NotNull
  @Override
  public <IN> Channel<IN, ?> duplicateInputOf(
      @NotNull final Iterable<? extends Channel<? super IN, ?>> channels) {
    return mChannelHandler.duplicateInputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> List<Channel<?, OUT>> duplicateOutputOf(
      @NotNull final Channel<?, ? extends OUT> channel, final int count) {
    return mChannelHandler.duplicateOutputOf(channel, count);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<IN>, ?> inputFlowOf(@NotNull final Channel<? super IN, ?> channel,
      final int id) {
    return mChannelHandler.withConfiguration(mConfiguration).inputFlowOf(channel, id);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> Channel<IN, ?> inputOfFlow(
      @NotNull final Channel<? super FlowData<DATA>, ?> channel, final int id) {
    return mChannelHandler.withConfiguration(mConfiguration).inputOfFlow(channel, id);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super FlowData<DATA>, ?> channel, @NotNull final int... ids) {
    return mChannelHandler.withConfiguration(mConfiguration).inputOfFlow(channel, ids);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super FlowData<DATA>, ?> channel,
      @NotNull final Iterable<Integer> ids) {
    return mChannelHandler.withConfiguration(mConfiguration).inputOfFlow(channel, ids);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(final int startId,
      final int rangeSize, @NotNull final Channel<? super FlowData<DATA>, ?> channel) {
    return mChannelHandler.withConfiguration(mConfiguration)
                          .inputOfFlow(startId, rangeSize, channel);
  }

  @NotNull
  @Override
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinInputOf(channels);
  }

  @NotNull
  @Override
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinInputOf(placeholder, channels);
  }

  @NotNull
  @Override
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinInputOf(placeholder, channels);
  }

  @NotNull
  @Override
  public <IN> Channel<List<? extends IN>, ?> joinInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinInputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, List<OUT>> joinOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinOutputOf(placeholder, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).joinOutputOf(placeholder, channels);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeInputOf(
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeInputOf(channels);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeInputOf(startId, channels);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeInputOf(startId, channels);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeInputOf(channels);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeInputOf(
      @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeInputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> mergeOutputOf(@NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> mergeOutputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeOutputOf(startId, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> mergeOutputOf(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeOutputOf(startId, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> mergeOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> mergeOutputOf(
      @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, FlowData<OUT>> outputFlowOf(
      @NotNull final Channel<?, ? extends OUT> channel, final int id) {
    return mChannelHandler.withConfiguration(mConfiguration).outputFlowOf(channel, id);
  }

  @NotNull
  @Override
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends FlowData<? extends OUT>> channel,
      @NotNull final int... ids) {
    return mChannelHandler.withConfiguration(mConfiguration).outputOfFlow(channel, ids);
  }

  @NotNull
  @Override
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends FlowData<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids) {
    return mChannelHandler.withConfiguration(mConfiguration).outputOfFlow(channel, ids);
  }

  @NotNull
  @Override
  public <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(final int startId, final int rangeSize,
      @NotNull final Channel<?, ? extends FlowData<? extends OUT>> channel) {
    return mChannelHandler.withConfiguration(mConfiguration)
                          .outputOfFlow(startId, rangeSize, channel);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> replayOutputOf(@NotNull final Channel<?, OUT> channel) {
    return mChannelHandler.withConfiguration(mConfiguration).replayOutputOf(channel);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> SparseArrayCompat<Channel<IN, ?>> inputOfParcelableFlow(
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel,
      @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return internalInputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> SparseArrayCompat<Channel<IN, ?>> inputOfParcelableFlow(
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      if (id == null) {
        throw new NullPointerException("the set of ids must not contain null objects");
      }

      idSet.add(id);
    }

    return internalInputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> SparseArrayCompat<Channel<IN, ?>> inputOfParcelableFlow(
      final int startId, final int rangeSize,
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return internalInputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public <IN> Channel<FlowData<? extends IN>, ?> mergeParcelableInputOf(
      @NotNull final SparseArrayCompat<? extends Channel<? extends IN, ?>> channels) {
    if (channels.size() == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    if (channels.indexOfValue(null) >= 0) {
      throw new NullPointerException("the array of channels must not contain null objects");
    }

    final ChannelBuilder channelBuilder =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration);
    final SparseArrayCompat<Channel<IN, ?>> channelArray =
        new SparseArrayCompat<Channel<IN, ?>>(channels.size());
    final int channelSize = channels.size();
    for (int i = 0; i < channelSize; ++i) {
      final Channel<IN, IN> outputChannel = channelBuilder.ofType();
      ((Channel<IN, ?>) channels.valueAt(i)).pass(outputChannel);
      channelArray.append(channels.keyAt(i), outputChannel);
    }

    final Channel<FlowData<? extends IN>, FlowData<? extends IN>> inputChannel =
        channelBuilder.ofType();
    return inputChannel.consume(new SortingSparseArrayChannelConsumer<IN>(channelArray));
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull final SparseArrayCompat<? extends Channel<?, ? extends OUT>> channels) {
    if (channels.size() == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    if (channels.indexOfValue(null) >= 0) {
      throw new NullPointerException("the array of channels must not contain null objects");
    }

    final Channel<ParcelableFlowData<OUT>, ParcelableFlowData<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    final int channelSize = channels.size();
    for (int i = 0; i < channelSize; ++i) {
      outputChannel.pass(outputParcelableFlowOf(channels.valueAt(i), channels.keyAt(i)));
    }

    return outputChannel.close();
  }

  @NotNull
  @Override
  public <OUT> SparseArrayCompat<Channel<?, OUT>> outputOfParcelableFlow(
      @NotNull final Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel,
      @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return internalOutputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public <OUT> SparseArrayCompat<Channel<?, OUT>> outputOfParcelableFlow(
      @NotNull final Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      if (id == null) {
        throw new NullPointerException("the set of ids must not contain null objects");
      }

      idSet.add(id);
    }

    return internalOutputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public <OUT> SparseArrayCompat<Channel<?, OUT>> outputOfParcelableFlow(final int startId,
      final int rangeSize,
      @NotNull final Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return internalOutputOfFlow(channel, idSet);
  }

  @NotNull
  @Override
  public Builder<? extends SparseChannelHandlerCompat> withChannel() {
    return new Builder<SparseChannelHandlerCompat>(mConfigurable, mConfiguration);
  }

  @NotNull
  @Override
  public SparseChannelHandlerCompat withConfiguration(
      @NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public <DATA, IN extends DATA> Channel<IN, ?> inputOfParcelableFlow(
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel, final int id) {
    return mChannelHandler.withConfiguration(mConfiguration).inputOfParcelableFlow(channel, id);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeParcelableOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    return mChannelHandler.withConfiguration(mConfiguration)
                          .mergeParcelableOutputOf(startId, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration)
                          .mergeParcelableOutputOf(startId, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mChannelHandler.withConfiguration(mConfiguration).mergeParcelableOutputOf(channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> outputParcelableFlowOf(
      @NotNull final Channel<?, ? extends OUT> channel, final int id) {
    return mChannelHandler.withConfiguration(mConfiguration).outputParcelableFlowOf(channel, id);
  }

  @NotNull
  private <DATA, IN extends DATA> SparseArrayCompat<Channel<IN, ?>> internalInputOfFlow(
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel,
      @NotNull final HashSet<Integer> ids) {
    final SparseArrayCompat<Channel<IN, ?>> channelArray =
        new SparseArrayCompat<Channel<IN, ?>>(ids.size());
    for (final Integer id : ids) {
      final Channel<IN, ?> inputChannel = inputOfParcelableFlow(channel, id);
      channelArray.append(id, inputChannel);
    }

    return channelArray;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> SparseArrayCompat<Channel<?, OUT>> internalOutputOfFlow(
      @NotNull final Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel,
      @NotNull final HashSet<Integer> ids) {
    synchronized (sOutputChannels) {
      final WeakIdentityHashMap<Channel<?, ?>, HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>>
          outputChannels = sOutputChannels;
      HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>> channelArrays =
          outputChannels.get(channel);
      if (channelArrays == null) {
        channelArrays = new HashMap<FlowInfo, SparseArrayCompat<Channel<?, ?>>>();
        outputChannels.put(channel, channelArrays);
      }

      final int size = ids.size();
      final ScheduledExecutor executor = mExecutor;
      final ChannelConfiguration configuration = mConfiguration;
      final FlowInfo flowInfo = new FlowInfo(executor, configuration, ids);
      final SparseArrayCompat<Channel<?, OUT>> channelArray =
          new SparseArrayCompat<Channel<?, OUT>>(size);
      SparseArrayCompat<Channel<?, ?>> channels = channelArrays.get(flowInfo);
      if (channels != null) {
        final int channelSize = channels.size();
        for (int i = 0; i < channelSize; ++i) {
          channelArray.append(channels.keyAt(i), readOnly((Channel<OUT, OUT>) channels.valueAt(i)));
        }

      } else {
        final ChannelBuilder channelBuilder =
            JRoutineCore.channelOn(executor).withConfiguration(configuration);
        final SparseArrayCompat<Channel<OUT, ?>> inputArray =
            new SparseArrayCompat<Channel<OUT, ?>>(size);
        channels = new SparseArrayCompat<Channel<?, ?>>(size);
        for (final Integer id : ids) {
          final Channel<OUT, OUT> outputChannel = channelBuilder.ofType();
          inputArray.put(id, outputChannel);
          channelArray.put(id, readOnly(outputChannel));
          channels.put(id, outputChannel);
        }

        channel.consume(new SortingSparseArrayChannelConsumer<OUT>(inputArray));
        channelArrays.put(flowInfo, channels);
      }

      return channelArray;
    }
  }

  /**
   * Class used as key to identify a specific array of output channels.
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
