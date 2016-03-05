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

package com.github.dm.jrt.android.v11.core.channel;

import android.util.SparseArray;

import com.github.dm.jrt.android.core.channel.AndroidChannels;
import com.github.dm.jrt.android.core.channel.ParcelableSelectable;
import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 08/03/2015.
 */
public class SparseChannels extends AndroidChannels {

    /**
     * Avoid direct instantiation.
     */
    protected SparseChannels() {

    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.<br/>
     * Note that the builder will successfully create only one input channel instance, and that the
     * returned channel <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @see AndroidChannels#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final SparseArray<? extends InputChannel<? extends IN>> channels) {

        return new CombineMapBuilder<IN>(channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified sparse array.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the map of indexes and output channels.
     * @param <OUT>    the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @see AndroidChannels#merge(Map)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final SparseArray<? extends OutputChannel<? extends OUT>> channels) {

        return new MergeMapBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.<br/>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels builder.
     * @see AndroidChannels#select(com.github.dm.jrt.core.channel.Channel.InputChannel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends SparseArray<IOChannel<IN>>>
    selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final int... indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final int index : indexes) {
            indexSet.add(index);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.<br/>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels builder.
     * @see AndroidChannels#select(com.github.dm.jrt.core.channel.Channel.InputChannel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends SparseArray<IOChannel<IN>>>
    selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            indexSet.add(index);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.<br/>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <DATA>     the channel data type.
     * @param <IN>       the input data type.
     * @return the map of indexes and I/O channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     * @see AndroidChannels#select(int, int, com.github.dm.jrt.core.channel.Channel.InputChannel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends SparseArray<IOChannel<IN>>>
    selectParcelable(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        if (rangeSize <= 0) {
            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        for (int i = startIndex; i < endIndex; i++) {
            indexSet.add(i);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of output channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <OUT>      the output data type.
     * @return the map of indexes and output channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     * @see AndroidChannels#select(int, int, com.github.dm.jrt.core.channel.Channel.OutputChannel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends SparseArray<OutputChannel<OUT>>> selectParcelable(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel) {

        if (rangeSize <= 0) {
            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        for (int i = startIndex; i < endIndex; i++) {
            indexSet.add(i);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of output channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the list of indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @see AndroidChannels#select(com.github.dm.jrt.core.channel.Channel.OutputChannel, int...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends SparseArray<OutputChannel<OUT>>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final int index : indexes) {
            indexSet.add(index);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of output channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @see AndroidChannels#select(com.github.dm.jrt.core.channel.Channel.OutputChannel, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends SparseArray<OutputChannel<OUT>>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            indexSet.add(index);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }
}
