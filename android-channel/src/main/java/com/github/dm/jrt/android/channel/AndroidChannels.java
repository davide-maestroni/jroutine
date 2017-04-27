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

package com.github.dm.jrt.android.channel;

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Utility class for handling routine channels.
 * <p>
 * Created by davide-maestroni on 06/18/2015.
 */
@SuppressWarnings("WeakerAccess")
public class AndroidChannels extends Channels {

  /**
   * Avoid explicit instantiation.
   */
  protected AndroidChannels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels merging the specified channels into a flow one.
   * <br>
   * The flow IDs will be the position in the array.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   * @see Channels#mergeOutput(Channel...)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> mergeParcelableOutput(
      @NotNull final Channel<?, ?>... channels) {
    return mergeParcelableOutput(0, channels);
  }

  /**
   * Returns a builder of channels merging the specified channels into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param startId  the flow start ID.
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   * @see Channels#mergeOutput(int, Channel...)
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> mergeParcelableOutput(
      final int startId, @NotNull final Channel<?, ?>... channels) {
    return (MergeOutputBuilder<OUT>) new MergeOutputBuilder<Object>(startId,
        Arrays.asList(channels));
  }

  /**
   * Returns a builder of channels merging the specified channels into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param startId  the flow start ID.
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   * @see Channels#mergeOutput(int, Iterable)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> mergeParcelableOutput(
      final int startId, @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return new MergeOutputBuilder<OUT>(startId, channels);
  }

  /**
   * Returns a builder of channels merging the specified channels into a flow one.
   * <br>
   * The flow IDs will be the position in the iterable.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   * @see Channels#mergeOutput(Iterable)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> mergeParcelableOutput(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mergeParcelableOutput(0, channels);
  }

  /**
   * Returns a builder of channels making the data of the specified one a flow.
   * <br>
   * Each output will be passed along unchanged.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * one will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channel the channel.
   * @param id      the channel ID.
   * @param <OUT>   the output data type.
   * @return the flow channel builder.
   * @see Channels#outputFlow(Channel, int)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> outputParcelableFlow(
      @NotNull final Channel<?, ? extends OUT> channel, final int id) {
    return new OutputFlowBuilder<OUT>(channel, id);
  }

  /**
   * Returns a builder of channels transforming the input data into flow ones.
   * <p>
   * Note that the builder will successfully create several channel instances.
   *
   * @param channel the flow channel.
   * @param id      the channel ID.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the channel builder.
   * @see Channels#flowInput(Channel, int)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelBuilder<IN, ?> parcelableFlowInput(
      @NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel, final int id) {
    return new FlowInputBuilder<DATA, IN>(channel, id);
  }
}
