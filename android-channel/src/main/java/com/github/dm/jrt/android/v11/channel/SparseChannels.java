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

package com.github.dm.jrt.android.v11.channel;

import android.util.SparseArray;

import com.github.dm.jrt.android.channel.AndroidChannels;
import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.v11.channel.builder.ChannelArrayBuilder;
import com.github.dm.jrt.channel.Flow;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;

/**
 * Utility class for handling routine channels.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.channel.SparseChannelsCompat SparseChannelsCompat} for
 * support of API levels lower than {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@SuppressWarnings("WeakerAccess")
public class SparseChannels extends AndroidChannels {

  /**
   * Avoid explicit instantiation.
   */
  protected SparseChannels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the map of IDs and channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   * @see AndroidChannels#mergeInput(Map)
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(
      @NotNull final SparseArray<? extends Channel<? extends IN, ?>> channels) {
    return new MergeInputMapBuilder<IN>(channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified sparse array.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the map of IDs and channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   * @see AndroidChannels#mergeOutput(Map)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableFlow<OUT>> mergeParcelableOutput(
      @NotNull final SparseArray<? extends Channel<?, ? extends OUT>> channels) {
    return new MergeOutputMapBuilder<OUT>(channels);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the flow channel.
   * @param ids     the array of IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   * @see AndroidChannels#flowInput(Channel, int)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> parcelableFlowInput(
      @NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel, @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return new InputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   * @see AndroidChannels#flowInput(Channel, Iterable)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> parcelableFlowInput(
      @NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      idSet.add(id);
    }

    return new InputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <DATA>    the channel data type.
   * @param <IN>      the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   * @see AndroidChannels#flowInput(int, int, Channel)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> parcelableFlowInput(
      final int startId, final int rangeSize,
      @NotNull final Channel<? super ParcelableFlow<DATA>, ?> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return new InputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param channel the flow channel.
   * @param ids     the list of IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   * @see AndroidChannels#flowOutput(Channel, int...)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> parcelableFlowOutput(
      @NotNull final Channel<?, ? extends ParcelableFlow<? extends OUT>> channel,
      @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   * @see AndroidChannels#flowOutput(Channel, Iterable)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> parcelableFlowOutput(
      @NotNull final Channel<?, ? extends ParcelableFlow<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      idSet.add(id);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <OUT>     the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   * @see AndroidChannels#flowOutput(int, int, Channel)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> parcelableFlowOutput(final int startId,
      final int rangeSize,
      @NotNull final Channel<?, ? extends ParcelableFlow<? extends OUT>> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }
}
