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

package com.github.dm.jrt.android.channel;

import com.github.dm.jrt.android.channel.builder.AndroidChannelHandler;
import com.github.dm.jrt.channel.FlowData;
import com.github.dm.jrt.channel.JRoutineChannels;
import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.Configurable;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.github.dm.jrt.core.JRoutineCore.readOnly;

/**
 * Default implementation of an Android channel handler.
 * <p>
 * Created by davide-maestroni on 05/18/2017.
 */
class DefaultAndroidChannelHandler implements AndroidChannelHandler {

  private final ChannelHandler mChannelHandler;

  private final ScheduledExecutor mExecutor;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  private final Configurable<AndroidChannelHandler> mConfigurable =
      new Configurable<AndroidChannelHandler>() {

        @NotNull
        @Override
        public AndroidChannelHandler withConfiguration(
            @NotNull final ChannelConfiguration configuration) {
          return DefaultAndroidChannelHandler.this.withConfiguration(configuration);
        }
      };

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultAndroidChannelHandler(@NotNull final ScheduledExecutor executor) {
    mChannelHandler = JRoutineChannels.channelHandlerOn(executor);
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
  @SuppressWarnings("unchecked")
  public <DATA, IN extends DATA> Channel<IN, ?> inputOfParcelableFlow(
      @NotNull final Channel<? super ParcelableFlowData<DATA>, ?> channel, final int id) {
    final Channel<ParcelableFlowData<DATA>, ParcelableFlowData<DATA>> flowChannel =
        JRoutineCore.channel().ofType();
    ((Channel<ParcelableFlowData<DATA>, ?>) channel).pass(flowChannel);
    final Channel<IN, IN> inputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    return inputChannel.consume(new ParcelableFlowChannelConsumer<DATA, IN>(flowChannel, id));
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull final Channel<?, ?>... channels) {
    return mergeParcelableOutputOf(0, channels);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    if (channels.length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    final Channel<ParcelableFlowData<OUT>, ParcelableFlowData<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    int i = startId;
    for (final Channel<?, ?> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the array of channels must not contain null objects");
      }

      outputChannel.pass(outputParcelableFlowOf((Channel<?, OUT>) channel, i++));
    }

    return outputChannel.close();
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    final Channel<ParcelableFlowData<OUT>, ParcelableFlowData<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    int i = startId;
    boolean isEmpty = true;
    for (final Channel<?, ? extends OUT> channel : channels) {
      if (channel == null) {
        throw new NullPointerException("the collection of channels must not contain null objects");
      }

      isEmpty = false;
      outputChannel.pass(outputParcelableFlowOf(channel, i++));
    }

    if (isEmpty) {
      throw new IllegalArgumentException("the collection of channels must not be empty");
    }

    return outputChannel.close();
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mergeParcelableOutputOf(0, channels);
  }

  @NotNull
  @Override
  public <OUT> Channel<?, ParcelableFlowData<OUT>> outputParcelableFlowOf(
      @NotNull final Channel<?, ? extends OUT> channel, final int id) {
    final Channel<ParcelableFlowData<OUT>, ParcelableFlowData<OUT>> outputChannel =
        JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
    channel.consume(new ParcelableFlowChannelConsumer<OUT, OUT>(outputChannel, id));
    return readOnly(outputChannel);
  }

  @NotNull
  @Override
  public Builder<? extends AndroidChannelHandler> withChannel() {
    return new Builder<AndroidChannelHandler>(mConfigurable, mConfiguration);
  }

  @NotNull
  @Override
  public AndroidChannelHandler withConfiguration(
      @NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }
}
