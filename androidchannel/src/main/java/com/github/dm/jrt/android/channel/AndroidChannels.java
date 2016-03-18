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
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 06/18/2015.
 */
public class AndroidChannels extends Channels {

    /**
     * Avoid direct instantiation.
     */
    protected AndroidChannels() {

    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the collection of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see Channels#merge(int, Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new MergeBuilder<OUT>(startIndex, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(int, com.github.dm.jrt.core.channel.Channel.OutputChannel[])
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return (MergeBuilder<OUT>) new MergeBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see Channels#merge(Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(com.github.dm.jrt.core.channel.Channel.OutputChannel[])
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> merge(@NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the I/O channel builder.
     * @see Channels#select(com.github.dm.jrt.core.channel.Channel.InputChannel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends IOChannel<IN>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            final int index) {

        return new InputSelectBuilder<DATA, IN>(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable output channel builder.
     * @see Channels#toSelectable(com.github.dm.jrt.core.channel.Channel.OutputChannel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends
            ParcelableSelectable<OUT>>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new SelectableOutputBuilder<OUT>(channel, index);
    }
}
