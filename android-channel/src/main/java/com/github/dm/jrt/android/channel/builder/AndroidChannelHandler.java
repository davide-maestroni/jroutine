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

package com.github.dm.jrt.android.channel.builder;

import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a class handling routine channels.
 * <br>
 * The interface provides several methods to split and merge channels together, making also possible
 * to transfer data in multiple flows, each one identified by a specific ID.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 */
public interface AndroidChannelHandler extends ChannelHandler {

  /**
   * Returns a channel transforming the input data into flow ones.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channel {@code A}, its final output will be:
   *
   * @param channel the flow channel.
   * @param id      the flow ID.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the channel instance.
   * @see ChannelHandler#inputOfFlow(Channel, int)
   */
  @NotNull
  <DATA, IN extends DATA> Channel<IN, ?> inputOfParcelableFlow(
      @NotNull Channel<? super ParcelableFlowData<DATA>, ?> channel, int id);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the array.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified array is empty.
   * @throws NullPointerException     if the specified array is null or contains a null
   *                                  object.
   * @see ChannelHandler#mergeOutputOf(Channel[])
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param startId  the flow start ID.
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified array is empty.
   * @throws NullPointerException     if the specified array is null or contains a null
   *                                  object.
   * @see ChannelHandler#mergeOutputOf(int, Channel[])
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(int startId,
      @NotNull Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param startId  the flow start ID.
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified iterable is empty.
   * @throws NullPointerException     if the specified iterable is null or contains a
   *                                  null object.
   * @see ChannelHandler#mergeOutputOf(int, Iterable)
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(int startId,
      @NotNull Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the iterable.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   * @throws IllegalArgumentException if the specified iterable is empty.
   * @throws NullPointerException     if the specified iterable is null or contains a
   *                                  null object.
   * @see ChannelHandler#mergeOutputOf(Iterable)
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> mergeParcelableOutputOf(
      @NotNull Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel making the data of the specified one a flow.
   * <br>
   * Each output will be passed along unchanged.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * one will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be read-only.
   *
   * @param channel the channel.
   * @param id      the flow ID.
   * @param <OUT>   the output data type.
   * @return the channel instance.
   * @see ChannelHandler#outputFlowOf(Channel, int)
   */
  @NotNull
  <OUT> Channel<?, ParcelableFlowData<OUT>> outputParcelableFlowOf(
      @NotNull Channel<?, ? extends OUT> channel, int id);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  Builder<? extends AndroidChannelHandler> withChannel();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  AndroidChannelHandler withConfiguration(@NotNull ChannelConfiguration configuration);
}
