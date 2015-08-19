/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.channel.TransportChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 3/15/15.
 */
public class Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the list ones.
     *
     * @param channels the array of input channels.
     * @return the selectable input channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static TransportChannel<Selectable<?>> combine(
            @Nonnull final InputChannel<?>... channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of input channels.
     * @return the selectable input channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static TransportChannel<Selectable<?>> combine(final int startIndex,
            @Nonnull final InputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<TransportChannel<?>> channelList =
                new ArrayList<TransportChannel<?>>(length);

        for (final InputChannel<?> channel : channels) {

            final TransportChannel<?> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo((InputChannel<Object>) channel);
            channelList.add(transportChannel);
        }

        final TransportChannel<Selectable<?>> transportChannel =
                JRoutine.transport().buildChannel();
        transportChannel.passTo(new SortingInputConsumer(startIndex, channelList));
        return transportChannel;
    }

    /**
     * Combines the specified channels into a selectable one.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of input channels.
     * @return the selectable input channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static TransportChannel<Selectable<?>> combine(final int startIndex,
            @Nonnull final List<? extends InputChannel<?>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<TransportChannel<?>> channelList =
                new ArrayList<TransportChannel<?>>(channels.size());

        for (final InputChannel<?> channel : channels) {

            final TransportChannel<?> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(transportChannel);
        }

        final TransportChannel<Selectable<?>> transportChannel =
                JRoutine.transport().buildChannel();
        transportChannel.passTo(new SortingInputConsumer(startIndex, channelList));
        return transportChannel;
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the list ones.
     *
     * @param channels the list of input channels.
     * @return the selectable input channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static TransportChannel<Selectable<?>> combine(
            @Nonnull final List<? extends InputChannel<?>> channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one.
     *
     * @param channels the map of indexes and input channels.
     * @return the selectable input channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static TransportChannel<Selectable<?>> combine(
            @Nonnull final Map<Integer, ? extends InputChannel<?>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, TransportChannel<?>> channelMap =
                new HashMap<Integer, TransportChannel<?>>(channels.size());

        for (final Entry<Integer, ? extends InputChannel<?>> entry : channels.entrySet()) {

            final TransportChannel<?> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo(((InputChannel<Object>) entry.getValue()));
            channelMap.put(entry.getKey(), transportChannel);
        }

        final TransportChannel<Selectable<?>> transportChannel =
                JRoutine.transport().buildChannel();
        transportChannel.passTo(new SortingInputMapConsumer(channelMap));
        return transportChannel;
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.
     *
     * @param channels the array of channels.
     * @return the input channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static TransportChannel<List<?>> distribute(@Nonnull final InputChannel<?>... channels) {

        return distribute(false, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.
     *
     * @param channels the list of channels.
     * @return the input channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static TransportChannel<List<?>> distribute(
            @Nonnull final List<? extends InputChannel<?>> channels) {

        return distribute(false, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller of the specified number of channels, the remaining ones will be fed with
     * null objects. While, if the list of data exceeds the number of channels, the invocation will
     * be aborted.
     *
     * @param channels the array of channels.
     * @return the input channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static TransportChannel<List<?>> distributeAndFlush(
            @Nonnull final InputChannel<?>... channels) {

        return distribute(true, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller of the specified number of channels, the remaining ones will be fed with
     * null objects. While, if the list of data exceeds the number of channels, the invocation will
     * be aborted.
     *
     * @param channels the list of channels.
     * @return the input channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static TransportChannel<List<?>> distributeAndFlush(
            @Nonnull final List<? extends InputChannel<?>> channels) {

        return distribute(true, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUTPUT> the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<List<OUTPUT>> join(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        return join(false, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<List<?>> join(@Nonnull final OutputChannel<?>... channels) {

        return join(false, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining output will be returned by
     * filling the gaps with null instances, so that the generated list of data will always have the
     * same size of the channel list.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUTPUT> the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<List<OUTPUT>> joinAndFlush(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        return join(true, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining output will be returned by
     * filling the gaps with null instances, so that the generated list of data will always have the
     * same size of the channel array.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<List<?>> joinAndFlush(@Nonnull final OutputChannel<?>... channels) {

        return join(true, channels);
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     *
     * @param channel the selectable channel.
     * @param indexes the collection of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> Map<Integer, TransportChannel<INPUT>> map(
            @Nonnull final InputChannel<? super Selectable<DATA>> channel,
            @Nonnull final Collection<Integer> indexes) {

        final int size = indexes.size();
        final HashMap<Integer, TransportChannel<INPUT>> channelMap =
                new HashMap<Integer, TransportChannel<INPUT>>(size);

        for (final Integer index : indexes) {

            channelMap.put(index, Channels.<DATA, INPUT>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the map of indexes and output channels.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> Map<Integer, TransportChannel<INPUT>> map(
            @Nonnull final InputChannel<? super Selectable<DATA>> channel,
            @Nonnull final int... indexes) {

        final int size = indexes.length;
        final HashMap<Integer, TransportChannel<INPUT>> channelMap =
                new HashMap<Integer, TransportChannel<INPUT>>(size);

        for (final int index : indexes) {

            channelMap.put(index, Channels.<DATA, INPUT>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <DATA>     the channel data type.
     * @param <INPUT>    the input data type.
     * @return the map of indexes and output channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> Map<Integer, TransportChannel<INPUT>> map(
            final int startIndex, final int rangeSize,
            @Nonnull final InputChannel<? super Selectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashMap<Integer, TransportChannel<INPUT>> channelMap =
                new HashMap<Integer, TransportChannel<INPUT>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            channelMap.put(index, Channels.<DATA, INPUT>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <OUTPUT>   the output data type.
     * @return the map of indexes and output channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @Nonnull
    public static <OUTPUT> Map<Integer, OutputChannel<OUTPUT>> map(final int startIndex,
            final int rangeSize,
            @Nonnull final OutputChannel<? extends Selectable<? extends OUTPUT>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashMap<Integer, TransportChannel<OUTPUT>> inputMap =
                new HashMap<Integer, TransportChannel<OUTPUT>>(rangeSize);
        final HashMap<Integer, OutputChannel<OUTPUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUTPUT>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            final Integer integer = index;
            final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();
            inputMap.put(integer, transportChannel);
            outputMap.put(integer, transportChannel);
        }

        channel.passTo(new SortingOutputConsumer<OUTPUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the selectable output channel.
     * @param indexes  the list of indexes.
     * @param <OUTPUT> the output data type.
     * @return the channel map.
     */
    @Nonnull
    public static <OUTPUT> Map<Integer, OutputChannel<OUTPUT>> map(
            @Nonnull final OutputChannel<? extends Selectable<? extends OUTPUT>> channel,
            @Nonnull final Collection<Integer> indexes) {

        final int size = indexes.size();
        final HashMap<Integer, TransportChannel<OUTPUT>> inputMap =
                new HashMap<Integer, TransportChannel<OUTPUT>>(size);
        final HashMap<Integer, OutputChannel<OUTPUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUTPUT>>(size);

        for (final Integer index : indexes) {

            final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();
            inputMap.put(index, transportChannel);
            outputMap.put(index, transportChannel);
        }

        channel.passTo(new SortingOutputConsumer<OUTPUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the outputs filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the selectable output channel.
     * @param indexes  the list of indexes.
     * @param <OUTPUT> the output data type.
     * @return the channel map.
     */
    @Nonnull
    public static <OUTPUT> Map<Integer, OutputChannel<OUTPUT>> map(
            @Nonnull final OutputChannel<? extends Selectable<? extends OUTPUT>> channel,
            @Nonnull final int... indexes) {

        final int size = indexes.length;
        final HashMap<Integer, TransportChannel<OUTPUT>> inputMap =
                new HashMap<Integer, TransportChannel<OUTPUT>>(size);
        final HashMap<Integer, OutputChannel<OUTPUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUTPUT>>(size);

        for (final Integer index : indexes) {

            final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();
            inputMap.put(index, transportChannel);
            outputMap.put(index, transportChannel);
        }

        channel.passTo(new SortingOutputConsumer<OUTPUT>(inputMap));
        return outputMap;
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUTPUT>   the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends Selectable<OUTPUT>> merge(final int startIndex,
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final TransportChannel<Selectable<OUTPUT>> transportChannel =
                JRoutine.transport().buildChannel();
        int i = startIndex;

        for (final OutputChannel<? extends OUTPUT> channel : channels) {

            transportChannel.pass(toSelectable(channel, i++));
        }

        return transportChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<? extends Selectable<?>> merge(final int startIndex,
            @Nonnull final OutputChannel<?>... channels) {

        if (channels.length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final TransportChannel<Selectable<Object>> transportChannel =
                JRoutine.transport().buildChannel();
        int i = startIndex;

        for (final OutputChannel<?> channel : channels) {

            transportChannel.pass(toSelectable(channel, i++));
        }

        return transportChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends Selectable<OUTPUT>> merge(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUTPUT>   the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends Selectable<OUTPUT>> merge(
            @Nonnull final Map<Integer, ? extends OutputChannel<? extends OUTPUT>> channelMap) {

        if (channelMap.isEmpty()) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final TransportChannel<Selectable<OUTPUT>> transportChannel =
                JRoutine.transport().buildChannel();

        for (final Entry<Integer, ? extends OutputChannel<? extends OUTPUT>> entry : channelMap
                .entrySet()) {

            transportChannel.pass(toSelectable(entry.getValue(), entry.getKey()));
        }

        return transportChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @Nonnull
    public static OutputChannel<? extends Selectable<?>> merge(
            @Nonnull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     * @return the input channel.
     */
    @Nonnull
    public static <DATA, INPUT extends DATA> TransportChannel<INPUT> select(
            @Nullable final InputChannel<? super Selectable<DATA>> channel, final int index) {

        final TransportChannel<INPUT> inputChannel = JRoutine.transport().buildChannel();

        if (channel != null) {

            final TransportChannel<Selectable<DATA>> transportChannel =
                    JRoutine.transport().buildChannel();
            transportChannel.passTo(channel);
            inputChannel.passTo(new SelectableInputConsumer<DATA, INPUT>(transportChannel, index));
        }

        return inputChannel;
    }

    /**
     * Returns a new channel transforming the output data into selectable ones.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the selectable channel.
     * @param index    the channel index.
     * @param <OUTPUT> the output data type.
     * @return the output channel.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<OUTPUT> select(
            @Nullable final OutputChannel<? extends Selectable<? extends OUTPUT>> channel,
            final int index) {

        final TransportChannel<OUTPUT> transportChannel = JRoutine.transport().buildChannel();

        if (channel != null) {

            channel.passTo(new FilterOutputConsumer<OUTPUT>(transportChannel, index));
        }

        return transportChannel;
    }

    /**
     * Returns a new selectable channel feeding the specified one.<br/>
     * Each output will be filtered based on the specified index.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <INPUT> the input data type.
     * @return the selectable input channel.
     */
    @Nonnull
    public static <INPUT> TransportChannel<Selectable<INPUT>> toSelectable(
            @Nullable final InputChannel<? super INPUT> channel, final int index) {

        final TransportChannel<Selectable<INPUT>> inputChannel =
                JRoutine.transport().buildChannel();

        if (channel != null) {

            final TransportChannel<INPUT> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo(channel);
            inputChannel.passTo(new FilterInputConsumer<INPUT>(transportChannel, index));
        }

        return inputChannel;
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel  the channel to make selectable.
     * @param index    the channel index.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<? extends Selectable<OUTPUT>> toSelectable(
            @Nullable final OutputChannel<? extends OUTPUT> channel, final int index) {

        final TransportChannel<Selectable<OUTPUT>> transportChannel =
                JRoutine.transport().buildChannel();

        if (channel != null) {

            channel.passTo(new SelectableOutputConsumer<OUTPUT>(transportChannel, index));
        }

        return transportChannel;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static TransportChannel<List<?>> distribute(final boolean isFlush,
            @Nonnull final InputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<TransportChannel<?>> channelList =
                new ArrayList<TransportChannel<?>>(length);

        for (final InputChannel<?> channel : channels) {

            final TransportChannel<?> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(transportChannel);
        }

        final TransportChannel<List<?>> transportChannel = JRoutine.transport().buildChannel();
        return transportChannel.passTo(new DistributeInputConsumer(isFlush, channelList));
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static TransportChannel<List<?>> distribute(final boolean isFlush,
            @Nonnull final List<? extends InputChannel<?>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<TransportChannel<?>> channelList =
                new ArrayList<TransportChannel<?>>(channels.size());

        for (final InputChannel<?> channel : channels) {

            final TransportChannel<?> transportChannel = JRoutine.transport().buildChannel();
            transportChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(transportChannel);
        }

        final TransportChannel<List<?>> transportChannel = JRoutine.transport().buildChannel();
        return transportChannel.passTo(new DistributeInputConsumer(isFlush, channelList));
    }

    @Nonnull
    private static <OUTPUT> OutputChannel<List<OUTPUT>> join(final boolean isFlush,
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        final int size = channels.size();

        if (size == 0) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final TransportChannel<List<OUTPUT>> transportChannel = JRoutine.transport().buildChannel();
        final JoinOutputConsumer<OUTPUT> consumer =
                new JoinOutputConsumer<OUTPUT>(transportChannel, size, isFlush);
        merge(channels).passTo(consumer);
        return transportChannel;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static OutputChannel<List<?>> join(final boolean isFlush,
            @Nonnull final OutputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final TransportChannel<List<?>> transportChannel = JRoutine.transport().buildChannel();
        final JoinOutputConsumer consumer =
                new JoinOutputConsumer(transportChannel, length, isFlush);
        merge(channels).passTo(consumer);
        return transportChannel;
    }

    /**
     * Data class storing information about the origin of the data.
     *
     * @param <DATA> the data type.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "this is an immutable data class")
    public static class Selectable<DATA> {

        /**
         * The data object.
         */
        public final DATA data;

        /**
         * The origin channel index.
         */
        public final int index;

        /**
         * Constructor.
         *
         * @param data  the data object.
         * @param index the channel index.
         */
        public Selectable(final DATA data, final int index) {

            this.data = data;
            this.index = index;
        }

        /**
         * Returns the data object casted to the specific type.
         *
         * @param <TYPE> the data type.
         * @return the data object.
         */
        @SuppressWarnings("unchecked")
        public <TYPE extends DATA> TYPE data() {

            return (TYPE) data;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = data != null ? data.hashCode() : 0;
            result = 31 * result + index;
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {

                return true;
            }

            if (!(o instanceof Selectable)) {

                return false;
            }

            final Selectable<?> that = (Selectable<?>) o;
            return index == that.index && !(data != null ? !data.equals(that.data)
                    : that.data != null);
        }

        @Override
        public String toString() {

            // AUTO-GENERATED CODE
            return "Selectable{" +
                    "data=" + data +
                    ", index=" + index +
                    '}';
        }
    }

    /**
     * Output consumer distributing list of inputs among a list of input channels.
     */
    private static class DistributeInputConsumer implements OutputConsumer<List<?>> {

        private final ArrayList<TransportChannel<?>> mChannels;

        private final boolean mIsFlush;

        /**
         * Constructor.
         *
         * @param isFlush  whether the inputs have to be flushed.
         * @param channels the list of channels.
         */
        private DistributeInputConsumer(final boolean isFlush,
                @Nonnull final ArrayList<TransportChannel<?>> channels) {

            mChannels = channels;
            mIsFlush = isFlush;
        }

        public void onComplete() {

            for (final TransportChannel<?> channel : mChannels) {

                channel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final TransportChannel<?> channel : mChannels) {

                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final List<?> inputs) {

            final int inputSize = inputs.size();
            final ArrayList<TransportChannel<?>> channels = mChannels;
            final int size = channels.size();

            if (inputSize > size) {

                throw new IllegalArgumentException();
            }

            final boolean isFlush = mIsFlush;

            for (int i = 0; i < size; i++) {

                final TransportChannel<Object> channel = (TransportChannel<Object>) channels.get(i);

                if (i < inputSize) {

                    channel.pass(inputs.get(i));

                } else if (isFlush) {

                    channel.pass((Object) null);
                }
            }
        }
    }

    /**
     * Output consumer filtering selectable input data.
     *
     * @param <INPUT> the input data type.
     */
    private static class FilterInputConsumer<INPUT> implements OutputConsumer<Selectable<INPUT>> {

        private final TransportChannel<? super INPUT> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the input channel to feed.
         * @param index   the index to filter.
         */
        private FilterInputConsumer(@Nonnull final TransportChannel<? super INPUT> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final Selectable<INPUT> selectable) {

            if (selectable.index == mIndex) {

                mChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer filtering selectable output data.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class FilterOutputConsumer<OUTPUT>
            implements OutputConsumer<Selectable<? extends OUTPUT>> {

        private final TransportChannel<OUTPUT> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the transport channel.
         * @param index   the index to filter.
         */
        private FilterOutputConsumer(@Nonnull final TransportChannel<OUTPUT> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final Selectable<? extends OUTPUT> selectable) {

            if (selectable.index == mIndex) {

                mChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer joining the data coming from several channels.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class JoinOutputConsumer<OUTPUT> implements OutputConsumer<Selectable<OUTPUT>> {

        protected final TransportChannel<List<OUTPUT>> mChannel;

        protected final SimpleQueue<OUTPUT>[] mQueues;

        private final boolean mIsFlush;

        /**
         * Constructor.
         *
         * @param channel the transport channel.
         * @param size    the number of channels to join.
         * @param isFlush whether the inputs have to be flushed.
         */
        @SuppressWarnings("unchecked")
        private JoinOutputConsumer(@Nonnull final TransportChannel<List<OUTPUT>> channel,
                final int size, final boolean isFlush) {

            final SimpleQueue<OUTPUT>[] queues = (mQueues = new SimpleQueue[size]);
            mChannel = channel;
            mIsFlush = isFlush;

            for (int i = 0; i < size; i++) {

                queues[i] = new SimpleQueue<OUTPUT>();
            }
        }

        protected void flush() {

            final TransportChannel<List<OUTPUT>> inputChannel = mChannel;
            final SimpleQueue<OUTPUT>[] queues = mQueues;
            final int length = queues.length;
            final ArrayList<OUTPUT> outputs = new ArrayList<OUTPUT>(length);
            boolean isEmpty;

            do {

                isEmpty = true;

                for (final SimpleQueue<OUTPUT> queue : queues) {

                    if (!queue.isEmpty()) {

                        isEmpty = false;
                        outputs.add(queue.removeFirst());

                    } else {

                        outputs.add(null);
                    }
                }

                if (!isEmpty) {

                    inputChannel.pass(outputs);
                    outputs.clear();

                } else {

                    break;
                }

            } while (true);
        }

        public void onComplete() {

            if (mIsFlush) {

                flush();
            }

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<OUTPUT> selectable) {

            final int index = selectable.index;
            final SimpleQueue<OUTPUT>[] queues = mQueues;
            queues[index].add(selectable.data);
            final int length = queues.length;
            boolean isFull = true;

            for (final SimpleQueue<OUTPUT> queue : queues) {

                if (queue.isEmpty()) {

                    isFull = false;
                    break;
                }
            }

            if (isFull) {

                final ArrayList<OUTPUT> outputs = new ArrayList<OUTPUT>(length);

                for (final SimpleQueue<OUTPUT> queue : queues) {

                    outputs.add(queue.removeFirst());
                }

                mChannel.pass(outputs);
            }
        }
    }

    /**
     * Output consumer transforming input data into selectable ones.
     *
     * @param <DATA>  the channel data type.
     * @param <INPUT> the input data type.
     */
    private static class SelectableInputConsumer<DATA, INPUT extends DATA>
            implements OutputConsumer<INPUT> {

        private final TransportChannel<? super Selectable<DATA>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param index   the selectable index.
         */
        private SelectableInputConsumer(
                @Nonnull final TransportChannel<? super Selectable<DATA>> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final INPUT input) {

            mChannel.pass(new Selectable<DATA>(input, mIndex));
        }
    }

    /**
     * Output consumer transforming output data into selectable ones.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUTPUT> implements OutputConsumer<OUTPUT> {

        private final TransportChannel<Selectable<OUTPUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the transport channel.
         * @param index   the selectable index.
         */
        private SelectableOutputConsumer(
                @Nonnull final TransportChannel<Selectable<OUTPUT>> channel, final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@Nullable final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final OUTPUT output) {

            mChannel.pass(new Selectable<OUTPUT>(output, mIndex));
        }
    }

    /**
     * Output consumer sorting selectable inputs among a list of input channels.
     */
    private static class SortingInputConsumer implements OutputConsumer<Selectable<?>> {

        private final ArrayList<TransportChannel<?>> mChannelList;

        private final int mSize;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the list of channels.
         */
        private SortingInputConsumer(final int startIndex,
                @Nonnull final ArrayList<TransportChannel<?>> channels) {

            mStartIndex = startIndex;
            mChannelList = channels;
            mSize = channels.size();
        }

        public void onComplete() {

            for (final TransportChannel<?> inputChannel : mChannelList) {

                inputChannel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final TransportChannel<?> inputChannel : mChannelList) {

                inputChannel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<?> selectable) {

            final int index = selectable.index - mStartIndex;

            if ((index < 0) || (index >= mSize)) {

                return;
            }

            final TransportChannel<Object> inputChannel =
                    (TransportChannel<Object>) mChannelList.get(index);

            if (inputChannel != null) {

                inputChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer sorting selectable inputs among a map of input channels.
     */
    private static class SortingInputMapConsumer implements OutputConsumer<Selectable<?>> {

        private final HashMap<Integer, TransportChannel<?>> mChannelMap;

        /**
         * Constructor.
         *
         * @param channelMap the map of indexes and input channels.
         */
        private SortingInputMapConsumer(
                @Nonnull final HashMap<Integer, TransportChannel<?>> channelMap) {

            mChannelMap = channelMap;
        }

        public void onComplete() {

            for (final TransportChannel<?> inputChannel : mChannelMap.values()) {

                inputChannel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final TransportChannel<?> inputChannel : mChannelMap.values()) {

                inputChannel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<?> selectable) {

            final TransportChannel<Object> inputChannel =
                    (TransportChannel<Object>) mChannelMap.get(selectable.index);

            if (inputChannel != null) {

                inputChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer sorting the output data among a map of output channels.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class SortingOutputConsumer<OUTPUT>
            implements OutputConsumer<Selectable<? extends OUTPUT>> {

        private final HashMap<Integer, TransportChannel<OUTPUT>> mChannelMap;

        /**
         * Constructor.
         *
         * @param channelMap the map of indexes and transport channels.
         */
        private SortingOutputConsumer(
                @Nonnull final HashMap<Integer, TransportChannel<OUTPUT>> channelMap) {

            mChannelMap = channelMap;
        }

        public void onComplete() {

            for (final TransportChannel<OUTPUT> channel : mChannelMap.values()) {

                channel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final TransportChannel<OUTPUT> channel : mChannelMap.values()) {

                channel.abort(error);
            }
        }

        public void onOutput(final Selectable<? extends OUTPUT> selectable) {

            final TransportChannel<OUTPUT> channel = mChannelMap.get(selectable.index);

            if (channel != null) {

                channel.pass(selectable.data);
            }
        }
    }
}
