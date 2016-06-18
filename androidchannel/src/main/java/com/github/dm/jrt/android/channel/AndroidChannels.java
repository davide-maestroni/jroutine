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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Utility class for handling routine channels.
 * <p>
 * Created by davide-maestroni on 06/18/2015.
 */
public class AndroidChannels extends Channels {

    /**
     * Avoid explicit instantiation.
     */
    protected AndroidChannels() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a builder of channels merging the specified channels into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the iterable of channels.
     * @param <OUT>      the output data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#merge(int, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new MergeBuilder<OUT>(startIndex, channels);
    }

    /**
     * Returns a builder of channels merging the specified channels into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(int, Channel...)
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final Channel<?, ?>... channels) {
        return (MergeBuilder<OUT>) new MergeBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels merging the specified channels into a selectable one.
     * <br>
     * The selectable indexes will be the position in the iterable.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#merge(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return merge(0, channels);
    }

    /**
     * Returns a builder of channels merging the specified channels into a selectable one.
     * <br>
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends
            ParcelableSelectable<OUT>>> merge(@NotNull final Channel<?, ?>... channels) {
        return merge(0, channels);
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the channel builder.
     * @see Channels#selectInput(Channel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Channel<IN, ?>>
    selectParcelableInput(
            @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
            final int index) {
        return new InputSelectBuilder<DATA, IN>(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable channel builder.
     * @see Channels#selectableOutput(Channel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends
            ParcelableSelectable<OUT>>> selectableOutput(
            @NotNull final Channel<?, ? extends OUT> channel, final int index) {
        return new SelectableOutputBuilder<OUT>(channel, index);
    }
}
