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

package com.github.dm.jrt.android.v11.channel.builder;

import android.util.SparseArray;

import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.android.channel.builder.AndroidChannelHandler;
import com.github.dm.jrt.channel.FlowData;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface defining a class handling routine channels.
 * <br>
 * The interface provides several methods to split and merge channels together, making also possible
 * to transfer data in multiple flows, each one identified by a specific ID.
 * <p>
 * Created by davide-maestroni on 05/18/2017.
 */
public interface SparseChannelHandler extends AndroidChannelHandler {

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the flow channel.
   * @param ids     the array of IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels.
   * @throws NullPointerException if the specified array is null or contains a null
   *                              object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#inputOfFlow(Channel, int...)
   * ChannelHandler#inputOfFlow(Channel, int...)
   */
  @NotNull
  <DATA, IN extends DATA> SparseArray<Channel<IN, ?>> inputOfParcelableFlow(
      @NotNull Channel<? super ParcelableFlowData<DATA>, ?> channel, @NotNull int... ids);

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels.
   * @throws NullPointerException if the specified iterable is null or returns a null
   *                              object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#inputOfFlow(Channel, Iterable)
   * ChannelHandler#inputOfFlow(Channel, Iterable)
   */
  @NotNull
  <DATA, IN extends DATA> SparseArray<Channel<IN, ?>> inputOfParcelableFlow(
      @NotNull Channel<? super ParcelableFlowData<DATA>, ?> channel,
      @NotNull Iterable<Integer> ids);

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   * <p>
   * Given channel {@code A} and channels {@code IN1}, {@code IN2} and {@code IN3} in the returned
   * map, the final output of {@code A} will be:
   * <pre><code>
   * A =&gt; [FlowData(startId + 1, IN2), FlowData(startId + 0, IN1), FlowData(startId + 2, IN3),
   * ...]
   * </code></pre>
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <DATA>    the channel data type.
   * @param <IN>      the input data type.
   * @return the map of IDs and channels.
   * @throws IllegalArgumentException if the specified range size is not positive.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#inputOfFlow(int, int, Channel)
   * ChannelHandler#inputOfFlow(int, int, Channel)
   */
  @NotNull
  <DATA, IN extends DATA> SparseArray<Channel<IN, ?>> inputOfParcelableFlow(int startId,
      int rangeSize, @NotNull Channel<? super ParcelableFlowData<DATA>, ?> channel);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create several channel instances.
   *
   * @param channels the map of IDs and channels.
   * @param <IN>     the input data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified map is empty.
   * @throws NullPointerException     if the specified map is null or contains a null
   *                                  object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#mergeInputOf(Map)
   * ChannelHandler#mergeInputOf(Map)
   */
  @NotNull
  <IN> Channel<FlowData<? extends IN>, ?> mergeParcelableInputOf(
      @NotNull SparseArray<? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param channels the map of IDs and channels.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified map is empty.
   * @throws NullPointerException     if the specified map is null or contains a null
   *                                  object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#mergeOutputOf(Map)
   * ChannelHandler#mergeOutputOf(Map)
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull SparseArray<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be read-only.
   *
   * @param channel the flow channel.
   * @param ids     the list of IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels.
   * @throws NullPointerException if the specified array is null or contains a null
   *                              object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#outputOfFlow(Channel, int...)
   * ChannelHandler#outputOfFlow(Channel, int...)
   */
  @NotNull
  <OUT> SparseArray<Channel<?, OUT>> outputOfParcelableFlow(
      @NotNull Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel,
      @NotNull int... ids);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be read-only.
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels.
   * @throws NullPointerException if the specified iterable is null or returns a null
   *                              object.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#outputOfFlow(Channel, Iterable)
   * ChannelHandler#outputOfFlow(Channel, Iterable)
   */
  @NotNull
  <OUT> SparseArray<Channel<?, OUT>> outputOfParcelableFlow(
      @NotNull Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel,
      @NotNull Iterable<Integer> ids);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be read-only.
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <OUT>     the output data type.
   * @return the map of IDs and channels.
   * @throws IllegalArgumentException if the specified range size is not positive.
   * @see com.github.dm.jrt.channel.builder.ChannelHandler#outputOfFlow(int, int, Channel)
   * ChannelHandler#outputOfFlow(int, int, Channel)
   */
  @NotNull
  <OUT> SparseArray<Channel<?, OUT>> outputOfParcelableFlow(int startId, int rangeSize,
      @NotNull Channel<?, ? extends ParcelableFlowData<? extends OUT>> channel);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  Builder<? extends SparseChannelHandler> withChannel();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  SparseChannelHandler withConfiguration(@NotNull ChannelConfiguration configuration);
}
