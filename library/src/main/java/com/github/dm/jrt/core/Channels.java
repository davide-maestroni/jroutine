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
package com.github.dm.jrt.core;

import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for handling routine channels.
 * <p/>
 * Created by davide-maestroni on 03/15/2015.
 */
public class Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Creates and returns a new streaming channel by invoking the specified routine in asynchronous
     * mode.
     *
     * @param routine the routine to be invoked.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the streaming channel.
     */
    @NotNull
    public static <IN, OUT> StreamingChannel<IN, OUT> asyncStream(
            @NotNull final Routine<IN, OUT> routine) {

        final IOChannel<IN, IN> ioChannel = JRoutine.io().buildChannel();
        return stream(ioChannel, routine.asyncCall(ioChannel));
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the list ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the array of input channels.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<Selectable<?>, Selectable<?>> combine(
            @NotNull final InputChannel<?>... channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of input channels.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static IOChannel<Selectable<?>, Selectable<?>> combine(final int startIndex,
            @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<IOChannel<?, ?>> channelList = new ArrayList<IOChannel<?, ?>>(length);

        for (final InputChannel<?> channel : channels) {

            final IOChannel<?, ?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo((InputChannel<Object>) channel);
            channelList.add(ioChannel);
        }

        final IOChannel<Selectable<?>, Selectable<?>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingInputConsumer(startIndex, channelList));
        return ioChannel;
    }

    /**
     * Combines the specified channels into a selectable one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> IOChannel<Selectable<? extends IN>, Selectable<? extends IN>> combine(
            final int startIndex,
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<IOChannel<?, ?>> channelList =
                new ArrayList<IOChannel<?, ?>>(channels.size());

        for (final InputChannel<?> channel : channels) {

            final IOChannel<?, ?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<Selectable<? extends IN>, Selectable<? extends IN>> ioChannel =
                JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingInputConsumer(startIndex, channelList));
        return ioChannel;
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the list ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the list of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<Selectable<? extends IN>, Selectable<? extends IN>> combine(
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> IOChannel<Selectable<? extends IN>, Selectable<? extends IN>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, IOChannel<?, ?>> channelMap =
                new HashMap<Integer, IOChannel<?, ?>>(channels.size());

        for (final Entry<Integer, ? extends InputChannel<?>> entry : channels.entrySet()) {

            final IOChannel<?, ?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) entry.getValue()));
            channelMap.put(entry.getKey(), ioChannel);
        }

        final IOChannel<Selectable<? extends IN>, Selectable<? extends IN>> ioChannel =
                JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingInputMapConsumer(channelMap));
        return ioChannel;
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the array of channels.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<List<?>, List<?>> distribute(
            @NotNull final InputChannel<?>... channels) {

        return distribute(false, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the list of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<List<? extends IN>, List<? extends IN>> distribute(
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return distribute(false, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller of the specified number of channels, the remaining ones will be fed with
     * null objects. While, if the list of data exceeds the number of channels, the invocation will
     * be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the array of channels.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<List<?>, List<?>> distributeAndFlush(
            @NotNull final InputChannel<?>... channels) {

        return distribute(true, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller of the specified number of channels, the remaining ones will be fed with
     * null objects. While, if the list of data exceeds the number of channels, the invocation will
     * be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channels the list of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<List<? extends IN>, List<? extends IN>> distributeAndFlush(
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return distribute(true, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> join(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

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
    @NotNull
    public static OutputChannel<List<?>> join(@NotNull final OutputChannel<?>... channels) {

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
     * @param <OUT>    the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> joinAndFlush(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

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
    @NotNull
    public static OutputChannel<List<?>> joinAndFlush(@NotNull final OutputChannel<?>... channels) {

        return join(true, channels);
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<Selectable<OUT>, Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;

        for (final OutputChannel<? extends OUT> channel : channels) {

            ioChannel.pass(toSelectable(channel, i++));
        }

        return ioChannel.close();
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
    @NotNull
    public static OutputChannel<? extends Selectable<?>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        if (channels.length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<Selectable<?>, Selectable<?>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;

        for (final OutputChannel<?> channel : channels) {

            ioChannel.pass(toSelectable(channel, i++));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channelMap) {

        if (channelMap.isEmpty()) {

            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final IOChannel<Selectable<OUT>, Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();

        for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : channelMap
                .entrySet()) {

            ioChannel.pass(toSelectable(entry.getValue(), entry.getKey()));
        }

        return ioChannel.close();
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
    @NotNull
    public static OutputChannel<? extends Selectable<?>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Creates and returns a new streaming channel by invoking the specified routine in parallel
     * mode.
     *
     * @param routine the routine to be invoked.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the streaming channel.
     */
    @NotNull
    public static <IN, OUT> StreamingChannel<IN, OUT> parallelStream(
            @NotNull final Routine<IN, OUT> routine) {

        final IOChannel<IN, IN> ioChannel = JRoutine.io().buildChannel();
        return stream(ioChannel, routine.parallelCall(ioChannel));
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the I/O channel.
     */
    @NotNull
    public static <DATA, IN extends DATA> IOChannel<IN, IN> select(
            @Nullable final InputChannel<? super Selectable<DATA>> channel, final int index) {

        final IOChannel<IN, IN> inputChannel = JRoutine.io().buildChannel();

        if (channel != null) {

            final IOChannel<Selectable<DATA>, Selectable<DATA>> ioChannel =
                    JRoutine.io().buildChannel();
            ioChannel.passTo(channel);
            inputChannel.passTo(new SelectableInputConsumer<DATA, IN>(ioChannel, index));
        }

        return inputChannel;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN, IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashMap<Integer, IOChannel<IN, IN>> channelMap =
                new HashMap<Integer, IOChannel<IN, IN>>();

        for (final Integer index : indexes) {

            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN, IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final HashMap<Integer, IOChannel<IN, IN>> channelMap =
                new HashMap<Integer, IOChannel<IN, IN>>(size);

        for (final int index : indexes) {

            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <DATA>     the channel data type.
     * @param <IN>       the input data type.
     * @return the map of indexes and I/O channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN, IN>> select(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashMap<Integer, IOChannel<IN, IN>> channelMap =
                new HashMap<Integer, IOChannel<IN, IN>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
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
     * @param <OUT>      the output data type.
     * @return the map of indexes and output channels.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <OUT> Map<Integer, OutputChannel<OUT>> select(final int startIndex,
            final int rangeSize,
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel) {

        if (rangeSize <= 0) {

            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashMap<Integer, IOChannel<OUT, OUT>> inputMap =
                new HashMap<Integer, IOChannel<OUT, OUT>>(rangeSize);
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>(rangeSize);

        for (int index = startIndex; index < rangeSize; index++) {

            final Integer integer = index;
            final IOChannel<OUT, OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(integer, ioChannel);
            outputMap.put(integer, ioChannel);
        }

        channel.passTo(new SortingOutputMapConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the outputs filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel the selectable output channel.
     * @param indexes the list of indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels.
     */
    @NotNull
    public static <OUT> Map<Integer, OutputChannel<OUT>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final HashMap<Integer, IOChannel<OUT, OUT>> inputMap =
                new HashMap<Integer, IOChannel<OUT, OUT>>(size);
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>(size);

        for (final Integer index : indexes) {

            final IOChannel<OUT, OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingOutputMapConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel the selectable output channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels.
     */
    @NotNull
    public static <OUT> Map<Integer, OutputChannel<OUT>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashMap<Integer, IOChannel<OUT, OUT>> inputMap =
                new HashMap<Integer, IOChannel<OUT, OUT>>();
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>();

        for (final Integer index : indexes) {

            final IOChannel<OUT, OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingOutputMapConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Creates and returns a new streaming channel backed by the specified input and output.
     *
     * @param inputChannel  the input channel.
     * @param outputChannel the output channel.
     * @param <IN>          the input data type.
     * @param <OUT>         the output data type.
     * @return the streaming channel.
     */
    @NotNull
    public static <IN, OUT> StreamingChannel<IN, OUT> stream(
            @NotNull final IOChannel<IN, ?> inputChannel,
            @NotNull final OutputChannel<OUT> outputChannel) {

        return new DefaultStreamingChannel<IN, OUT>(inputChannel, outputChannel);
    }

    /**
     * Creates and returns a new streaming channel by invoking the specified routine in synchronous
     * mode.
     *
     * @param routine the routine to be invoked.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the streaming channel.
     */
    @NotNull
    public static <IN, OUT> StreamingChannel<IN, OUT> syncStream(
            @NotNull final Routine<IN, OUT> routine) {

        final IOChannel<IN, IN> ioChannel = JRoutine.io().buildChannel();
        return stream(ioChannel, routine.syncCall(ioChannel));
    }

    /**
     * Returns a new selectable channel feeding the specified one.<br/>
     * Each output will be filtered based on the specified index.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <IN>    the input data type.
     * @return the selectable I/O channel.
     */
    @NotNull
    public static <IN> IOChannel<Selectable<IN>, Selectable<IN>> toSelectable(
            @Nullable final InputChannel<? super IN> channel, final int index) {

        final IOChannel<Selectable<IN>, Selectable<IN>> inputChannel = JRoutine.io().buildChannel();

        if (channel != null) {

            final IOChannel<IN, IN> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(channel);
            inputChannel.passTo(new FilterInputConsumer<IN>(ioChannel, index));
        }

        return inputChannel;
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable output channel.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> toSelectable(
            @Nullable final OutputChannel<? extends OUT> channel, final int index) {

        final IOChannel<Selectable<OUT>, Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();

        if (channel != null) {

            channel.passTo(new SelectableOutputConsumer<OUT>(ioChannel, index));
        }

        return ioChannel;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static IOChannel<List<?>, List<?>> distribute(final boolean isFlush,
            @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<IOChannel<?, ?>> channelList = new ArrayList<IOChannel<?, ?>>(length);

        for (final InputChannel<?> channel : channels) {

            final IOChannel<?, ?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<List<?>, List<?>> ioChannel = JRoutine.io().buildChannel();
        return ioChannel.passTo(new DistributeInputConsumer(isFlush, channelList));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <IN> IOChannel<List<? extends IN>, List<? extends IN>> distribute(
            final boolean isFlush,
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<IOChannel<?, ?>> channelList =
                new ArrayList<IOChannel<?, ?>>(channels.size());

        for (final InputChannel<?> channel : channels) {

            final IOChannel<?, ?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<List<? extends IN>, List<? extends IN>> ioChannel =
                JRoutine.io().buildChannel();
        return ioChannel.passTo(new DistributeInputConsumer(isFlush, channelList));
    }

    @NotNull
    private static <OUT> OutputChannel<List<? extends OUT>> join(final boolean isFlush,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        final int size = channels.size();

        if (size == 0) {

            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<List<? extends OUT>, List<? extends OUT>> ioChannel =
                JRoutine.io().buildChannel();
        final JoinOutputConsumer<OUT> consumer =
                new JoinOutputConsumer<OUT>(ioChannel, size, isFlush);
        merge(channels).passTo(consumer);
        return ioChannel;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static OutputChannel<List<?>> join(final boolean isFlush,
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;

        if (length == 0) {

            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<List<?>, List<?>> ioChannel = JRoutine.io().buildChannel();
        final JoinOutputConsumer consumer = new JoinOutputConsumer(ioChannel, length, isFlush);
        merge(channels).passTo(consumer);
        return ioChannel;
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

        private final ArrayList<IOChannel<?, ?>> mChannels;

        private final boolean mIsFlush;

        /**
         * Constructor.
         *
         * @param isFlush  whether the inputs have to be flushed.
         * @param channels the list of channels.
         */
        private DistributeInputConsumer(final boolean isFlush,
                @NotNull final ArrayList<IOChannel<?, ?>> channels) {

            mChannels = channels;
            mIsFlush = isFlush;
        }

        public void onComplete() {

            for (final IOChannel<?, ?> channel : mChannels) {

                channel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final IOChannel<?, ?> channel : mChannels) {

                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final List<?> inputs) {

            final int inputSize = inputs.size();
            final ArrayList<IOChannel<?, ?>> channels = mChannels;
            final int size = channels.size();

            if (inputSize > size) {

                throw new IllegalArgumentException();
            }

            final boolean isFlush = mIsFlush;

            for (int i = 0; i < size; ++i) {

                final IOChannel<Object, Object> channel =
                        (IOChannel<Object, Object>) channels.get(i);

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
     * @param <IN> the input data type.
     */
    private static class FilterInputConsumer<IN> implements OutputConsumer<Selectable<IN>> {

        private final IOChannel<? super IN, ? super IN> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the input channel to feed.
         * @param index   the index to filter.
         */
        private FilterInputConsumer(@NotNull final IOChannel<? super IN, ? super IN> channel,
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

        public void onOutput(final Selectable<IN> selectable) {

            if (selectable.index == mIndex) {

                mChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer joining the data coming from several channels.
     *
     * @param <OUT> the output data type.
     */
    private static class JoinOutputConsumer<OUT> implements OutputConsumer<Selectable<OUT>> {

        private final IOChannel<List<? extends OUT>, List<? extends OUT>> mChannel;

        private final boolean mIsFlush;

        private final SimpleQueue<OUT>[] mQueues;

        /**
         * Constructor.
         *
         * @param channel the I/O channel.
         * @param size    the number of channels to join.
         * @param isFlush whether the inputs have to be flushed.
         */
        @SuppressWarnings("unchecked")
        private JoinOutputConsumer(
                @NotNull final IOChannel<List<? extends OUT>, List<? extends OUT>> channel,
                final int size, final boolean isFlush) {

            final SimpleQueue<OUT>[] queues = (mQueues = new SimpleQueue[size]);
            mChannel = channel;
            mIsFlush = isFlush;

            for (int i = 0; i < size; ++i) {

                queues[i] = new SimpleQueue<OUT>();
            }
        }

        protected void flush() {

            final IOChannel<List<? extends OUT>, List<? extends OUT>> inputChannel = mChannel;
            final SimpleQueue<OUT>[] queues = mQueues;
            final int length = queues.length;
            final ArrayList<OUT> outputs = new ArrayList<OUT>(length);
            boolean isEmpty;

            do {

                isEmpty = true;

                for (final SimpleQueue<OUT> queue : queues) {

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
        public void onOutput(final Selectable<OUT> selectable) {

            final int index = selectable.index;
            final SimpleQueue<OUT>[] queues = mQueues;
            queues[index].add(selectable.data);
            final int length = queues.length;
            boolean isFull = true;

            for (final SimpleQueue<OUT> queue : queues) {

                if (queue.isEmpty()) {

                    isFull = false;
                    break;
                }
            }

            if (isFull) {

                final ArrayList<OUT> outputs = new ArrayList<OUT>(length);

                for (final SimpleQueue<OUT> queue : queues) {

                    outputs.add(queue.removeFirst());
                }

                mChannel.pass(outputs);
            }
        }
    }

    /**
     * Output consumer transforming input data into selectable ones.
     *
     * @param <DATA> the channel data type.
     * @param <IN>   the input data type.
     */
    private static class SelectableInputConsumer<DATA, IN extends DATA>
            implements OutputConsumer<IN> {

        private final IOChannel<? super Selectable<DATA>, ? super Selectable<DATA>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param index   the selectable index.
         */
        private SelectableInputConsumer(
                @NotNull final IOChannel<? super Selectable<DATA>, ? super Selectable<DATA>>
                        channel,
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

        public void onOutput(final IN input) {

            mChannel.pass(new Selectable<DATA>(input, mIndex));
        }
    }

    /**
     * Output consumer transforming output data into selectable ones.
     *
     * @param <OUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUT> implements OutputConsumer<OUT> {

        private final IOChannel<Selectable<OUT>, Selectable<OUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the I/O channel.
         * @param index   the selectable index.
         */
        private SelectableOutputConsumer(
                @NotNull final IOChannel<Selectable<OUT>, Selectable<OUT>> channel,
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

        public void onOutput(final OUT output) {

            mChannel.pass(new Selectable<OUT>(output, mIndex));
        }
    }

    /**
     * Output consumer sorting selectable inputs among a list of input channels.
     */
    private static class SortingInputConsumer implements OutputConsumer<Selectable<?>> {

        private final ArrayList<IOChannel<?, ?>> mChannelList;

        private final int mSize;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the list of channels.
         */
        private SortingInputConsumer(final int startIndex,
                @NotNull final ArrayList<IOChannel<?, ?>> channels) {

            mStartIndex = startIndex;
            mChannelList = channels;
            mSize = channels.size();
        }

        public void onComplete() {

            for (final IOChannel<?, ?> inputChannel : mChannelList) {

                inputChannel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final IOChannel<?, ?> inputChannel : mChannelList) {

                inputChannel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<?> selectable) {

            final int index = selectable.index - mStartIndex;

            if ((index < 0) || (index >= mSize)) {

                return;
            }

            final IOChannel<Object, Object> inputChannel =
                    (IOChannel<Object, Object>) mChannelList.get(index);

            if (inputChannel != null) {

                inputChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer sorting selectable inputs among a map of input channels.
     */
    private static class SortingInputMapConsumer implements OutputConsumer<Selectable<?>> {

        private final HashMap<Integer, IOChannel<?, ?>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the map of indexes and input channels.
         */
        private SortingInputMapConsumer(@NotNull final HashMap<Integer, IOChannel<?, ?>> channels) {

            mChannels = channels;
        }

        public void onComplete() {

            for (final IOChannel<?, ?> inputChannel : mChannels.values()) {

                inputChannel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final IOChannel<?, ?> inputChannel : mChannels.values()) {

                inputChannel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<?> selectable) {

            final IOChannel<Object, Object> inputChannel =
                    (IOChannel<Object, Object>) mChannels.get(selectable.index);

            if (inputChannel != null) {

                inputChannel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer sorting the output data among a map of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class SortingOutputMapConsumer<OUT>
            implements OutputConsumer<Selectable<? extends OUT>> {

        private final HashMap<Integer, IOChannel<OUT, OUT>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the map of indexes and I/O channels.
         */
        private SortingOutputMapConsumer(
                @NotNull final HashMap<Integer, IOChannel<OUT, OUT>> channels) {

            mChannels = channels;
        }

        public void onComplete() {

            for (final IOChannel<OUT, OUT> channel : mChannels.values()) {

                channel.close();
            }
        }

        public void onError(@Nullable final RoutineException error) {

            for (final IOChannel<OUT, OUT> channel : mChannels.values()) {

                channel.abort(error);
            }
        }

        public void onOutput(final Selectable<? extends OUT> selectable) {

            final IOChannel<OUT, OUT> channel = mChannels.get(selectable.index);

            if (channel != null) {

                channel.pass(selectable.data);
            }
        }
    }
}
