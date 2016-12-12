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
import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.v11.channel.builder.ChannelArrayBuilder;
import com.github.dm.jrt.channel.Selectable;
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
   * Returns a builder of channels combining the specified instances into a selectable one.
   * <br>
   * The selectable indexes will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the map of indexes and channels.
   * @param <IN>     the input data type.
   * @return the selectable channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   * @see AndroidChannels#combine(Map)
   */
  @NotNull
  public static <IN> ChannelBuilder<Selectable<? extends IN>, ?> combine(
      @NotNull final SparseArray<? extends Channel<? extends IN, ?>> channels) {
    return new CombineMapBuilder<IN>(channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a selectable one.
   * <br>
   * The selectable indexes will be the keys of the specified sparse array.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param channels the map of indexes and channels.
   * @param <OUT>    the output data type.
   * @return the selectable channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   * @see AndroidChannels#merge(Map)
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, ParcelableSelectable<OUT>> mergeParcelable(
      @NotNull final SparseArray<? extends Channel<?, ? extends OUT>> channels) {
    return new MergeMapBuilder<OUT>(channels);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified indexes.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the selectable channel.
   * @param indexes the array of indexes.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   * @see AndroidChannels#selectInput(Channel, int)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> selectInputParcelable(
      @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
      @NotNull final int... indexes) {
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    for (final int index : indexes) {
      indexSet.add(index);
    }

    return new InputMapBuilder<DATA, IN>(channel, indexSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified indexes.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param channel the selectable channel.
   * @param indexes the iterable returning the channel indexes.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   * @see AndroidChannels#selectInput(Channel, Iterable)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> selectInputParcelable(
      @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
      @NotNull final Iterable<Integer> indexes) {
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    for (final Integer index : indexes) {
      indexSet.add(index);
    }

    return new InputMapBuilder<DATA, IN>(channel, indexSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified indexes.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   *
   * @param startIndex the selectable start index.
   * @param rangeSize  the size of the range of indexes (must be positive).
   * @param channel    the selectable channel.
   * @param <DATA>     the channel data type.
   * @param <IN>       the input data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   * @see AndroidChannels#selectInput(int, int, Channel)
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelArrayBuilder<IN, ?> selectInputParcelable(
      final int startIndex, final int rangeSize,
      @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    final int endIndex = startIndex + rangeSize;
    for (int i = startIndex; i < endIndex; ++i) {
      indexSet.add(i);
    }

    return new InputMapBuilder<DATA, IN>(channel, indexSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified
   * indexes.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param channel the selectable channel.
   * @param indexes the list of indexes.
   * @param <OUT>   the output data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   * @see AndroidChannels#selectOutput(Channel, int...)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> selectOutputParcelable(
      @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel,
      @NotNull final int... indexes) {
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    for (final int index : indexes) {
      indexSet.add(index);
    }

    return new OutputMapBuilder<OUT>(channel, indexSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified
   * indexes.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param channel the selectable channel.
   * @param indexes the iterable returning the channel indexes.
   * @param <OUT>   the output data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   * @see AndroidChannels#selectOutput(Channel, Iterable)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> selectOutputParcelable(
      @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel,
      @NotNull final Iterable<Integer> indexes) {
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    for (final Integer index : indexes) {
      indexSet.add(index);
    }

    return new OutputMapBuilder<OUT>(channel, indexSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified
   * indexes.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   *
   * @param startIndex the selectable start index.
   * @param rangeSize  the size of the range of indexes (must be positive).
   * @param channel    the selectable channel.
   * @param <OUT>      the output data type.
   * @return the map of indexes and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   * @see AndroidChannels#selectOutput(int, int, Channel)
   */
  @NotNull
  public static <OUT> ChannelArrayBuilder<?, OUT> selectOutputParcelable(final int startIndex,
      final int rangeSize,
      @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> indexSet = new HashSet<Integer>();
    final int endIndex = startIndex + rangeSize;
    for (int i = startIndex; i < endIndex; ++i) {
      indexSet.add(i);
    }

    return new OutputMapBuilder<OUT>(channel, indexSet);
  }
}
