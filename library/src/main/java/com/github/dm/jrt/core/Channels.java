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

package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class for handling routine channels.<br/>
 * The class provides several methods to split and merge channels together, making also possible to
 * transfer data in multiple sub-channels, each one identified by a specific index.
 * <p/>
 * Created by davide-maestroni on 03/15/2015.
 */
public class Channels {

    private static final WeakIdentityHashMap<OutputChannel<?>, DefaultSelectableChannels<?>>
            sSelectableChannels =
            new WeakIdentityHashMap<OutputChannel<?>, DefaultSelectableChannels<?>>();

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Returns an output channel blending the outputs coming from the specified ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [B, A, A, C, B, C, B, A, B, ...]
     *     </code>
     * </pre>
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     */
    @NotNull
    public static <OUT> OutputChannel<OUT> blend(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
        for (final OutputChannel<? extends OUT> channel : channels) {
            channel.passTo(ioChannel);
        }

        return ioChannel.close();
    }

    /**
     * Returns an output channel blending the outputs coming from the specified ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [B, A, A, C, B, C, B, A, B, ...]
     *     </code>
     * </pre>
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> OutputChannel<OUT> blend(@NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        for (final OutputChannel<?> channel : channels) {
            channel.passTo(ioChannel);
        }

        return (OutputChannel<OUT>) ioChannel.close();
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the array ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, 0).data, Select(?, 0).data, Select(?, 0).data, ...]
     *         B - [Select(?, 1).data, Select(?, 1).data, Select(?, 1).data, ...]
     *         C - [Select(?, 2).data, Select(?, 2).data, Select(?, 2).data, ...]
     *     </code>
     * </pre>
     *
     * @param channels the array of input channels.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<Selectable<?>> combine(@NotNull final InputChannel<?>... channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, SI + 0).data, Select(?, SI + 0).data, Select(?, SI + 0).data, ...]
     *         B - [Select(?, SI + 1).data, Select(?, SI + 1).data, Select(?, SI + 1).data, ...]
     *         C - [Select(?, SI + 2).data, Select(?, SI + 2).data, Select(?, SI + 2).data, ...]
     *     </code>
     * </pre>
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of input channels.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static IOChannel<Selectable<?>> combine(final int startIndex,
            @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<IOChannel<?>> channelList = new ArrayList<IOChannel<?>>(length);
        for (final InputChannel<?> channel : channels) {
            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo((InputChannel<Object>) channel);
            channelList.add(ioChannel);
        }

        final IOChannel<Selectable<?>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingArrayOutputConsumer(startIndex, channelList));
        return ioChannel;
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, SI + 0).data, Select(?, SI + 0).data, Select(?, SI + 0).data, ...]
     *         B - [Select(?, SI + 1).data, Select(?, SI + 1).data, Select(?, SI + 1).data, ...]
     *         C - [Select(?, SI + 2).data, Select(?, SI + 2).data, Select(?, SI + 2).data, ...]
     *     </code>
     * </pre>
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> IOChannel<Selectable<? extends IN>> combine(final int startIndex,
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<IOChannel<?>> channelList = new ArrayList<IOChannel<?>>(channels.size());
        for (final InputChannel<?> channel : channels) {
            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<Selectable<? extends IN>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingArrayOutputConsumer(startIndex, channelList));
        return ioChannel;
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * same as the list ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, 0).data, Select(?, 0).data, Select(?, 0).data, ...]
     *         B - [Select(?, 1).data, Select(?, 1).data, Select(?, 1).data, ...]
     *         C - [Select(?, 2).data, Select(?, 2).data, Select(?, 2).data, ...]
     *     </code>
     * </pre>
     *
     * @param channels the list of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<Selectable<? extends IN>> combine(
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return combine(0, channels);
    }

    /**
     * Combines the specified channels into a selectable one. The selectable indexes will be the
     * keys of the specified map.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, key(A)).data, Select(?, key(A)).data, Select(?, key(A)).data, ...]
     *         B - [Select(?, key(B)).data, Select(?, key(B)).data, Select(?, key(B)).data, ...]
     *         C - [Select(?, key(C)).data, Select(?, key(C)).data, Select(?, key(C)).data, ...]
     *     </code>
     * </pre>
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> IOChannel<Selectable<? extends IN>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, IOChannel<?>> channelMap =
                new HashMap<Integer, IOChannel<?>>(channels.size());
        for (final Entry<Integer, ? extends InputChannel<?>> entry : channels.entrySet()) {
            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) entry.getValue()));
            channelMap.put(entry.getKey(), ioChannel);
        }

        final IOChannel<Selectable<? extends IN>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(new SortingMapOutputConsumer(channelMap));
        return ioChannel;
    }

    /**
     * Returns an output channel concatenating the outputs coming from the specified ones, so that,
     * all the outputs of the first channel will come before all the outputs of the second one, and
     * so on.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [A, A, A, ..., B, B, B, ..., C, C, C, ...]
     *     </code>
     * </pre>
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     */
    @NotNull
    public static <OUT> OutputChannel<OUT> concat(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<OUT> ioChannel = JRoutine.io()
                                                 .withChannels()
                                                 .withChannelOrder(OrderType.BY_CALL)
                                                 .set()
                                                 .buildChannel();
        for (final OutputChannel<? extends OUT> channel : channels) {
            channel.passTo(ioChannel);
        }

        return ioChannel.close();
    }

    /**
     * Returns an output channel concatenating the outputs coming from the specified ones, so that,
     * all the outputs of the first channel will come before all the outputs of the second one, and
     * so on.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [A, A, A, ..., B, B, B, ..., C, C, C, ...]
     *     </code>
     * </pre>
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> OutputChannel<OUT> concat(@NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<Object> ioChannel = JRoutine.io()
                                                    .withChannels()
                                                    .withChannelOrder(OrderType.BY_CALL)
                                                    .set()
                                                    .buildChannel();
        for (final OutputChannel<?> channel : channels) {
            channel.passTo(ioChannel);
        }

        return (OutputChannel<OUT>) ioChannel.close();
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1)]
     *         C - [list(2), list(2), list(2), ..., list(2)]
     *     </code>
     * </pre>
     *
     * @param channels the array of channels.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<List<?>> distribute(@NotNull final InputChannel<?>... channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1)]
     *         C - [list(2), list(2), list(2), ..., list(2)]
     *     </code>
     * </pre>
     *
     * @param channels the list of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<List<? extends IN>> distribute(
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller than the specified number of channels, the remaining ones will be fed with
     * the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1), PH, ...]
     *         C - [list(2), list(2), list(2), ..., list(2), PH, ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static IOChannel<List<?>> distribute(@Nullable final Object placeholder,
            @NotNull final InputChannel<?>... channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a new channel distributing the input data among the specified channels. If the list
     * of data is smaller than the specified number of channels, the remaining ones will be fed with
     * the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1), PH, ...]
     *         C - [list(2), list(2), list(2), ..., list(2), PH, ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the list of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> IOChannel<List<? extends IN>> distribute(@Nullable final IN placeholder,
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (B, C), ..., (B), ...]
     *     </code>
     * </pre>
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> join(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return join(false, null, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (B, C), ..., (B), ...]
     *     </code>
     * </pre>
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> join(
            @NotNull final OutputChannel<?>... channels) {

        return join(false, null, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (PH, B, C), ..., (PH, B, PH), ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the list of channels.
     * @param <OUT>       the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> join(@Nullable final OUT placeholder,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Returns an output channel joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (PH, B, C), ..., (PH, B, PH), ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <OUT>       the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<List<? extends OUT>> join(@Nullable final Object placeholder,
            @NotNull final OutputChannel<?>... channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, SI + 1), Select(A, SI + 0), Select(C, SI + 2), Select(A, SI + 0), ...]
     *     </code>
     * </pre>
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

        final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;
        for (final OutputChannel<? extends OUT> channel : channels) {
            ioChannel.pass(toSelectable(channel, i++));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will start from
     * the specified one.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, SI + 1), Select(A, SI + 0), Select(C, SI + 2), Select(A, SI + 0), ...]
     *     </code>
     * </pre>
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> OutputChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        if (channels.length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        int i = startIndex;
        for (final OutputChannel<?> channel : channels) {
            ioChannel.pass(toSelectable((OutputChannel<? extends OUT>) channel, i++));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the list ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, 1), Select(A, 0), Select(A, 0), Select(C, 2), Select(A, 0), ...]
     *     </code>
     * </pre>
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
     * Merges the specified channels into a selectable one. The selectable indexes will be the keys
     * of the specified map.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, key(B)), Select(A, key(A)), Select(C, key(C)), Select(A, key(A)), ...]
     *     </code>
     * </pre>
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

        final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : channelMap
                .entrySet()) {
            ioChannel.pass(toSelectable(entry.getValue(), entry.getKey()));
        }

        return ioChannel.close();
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the array ones.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, 1), Select(A, 0), Select(A, 0), Select(C, 2), Select(A, 0), ...]
     *     </code>
     * </pre>
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a new channel repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting multiple binding.<br/>
     * Note that the passed channels will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [A, A, A, ...] [A, A, A, ...] [A, A, A, ...] ...
     *     </code>
     * </pre>
     *
     * @param channel the output channel.
     * @param <OUT>   the output data type.
     * @return the repeating channel.
     */
    @NotNull
    public static <OUT> OutputChannel<OUT> repeat(@NotNull final OutputChannel<OUT> channel) {

        final RepeatedChannel<OUT> repeatedChannel = new RepeatedChannel<OUT>();
        channel.passTo((OutputConsumer<OUT>) repeatedChannel);
        return repeatedChannel.close();
    }

    /**
     * Returns a new channel transforming the input data into selectable ones.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(A, I), Select(A, I), Select(A, I), ...]
     *     </code>
     * </pre>
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the I/O channel.
     */
    @NotNull
    public static <DATA, IN extends DATA> IOChannel<IN> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel, final int index) {

        final IOChannel<IN> inputChannel = JRoutine.io().buildChannel();
        final IOChannel<Selectable<DATA>> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(channel);
        return inputChannel.passTo(new SelectableOutputConsumer<DATA, IN>(ioChannel, index));
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [Select(B, key(B)), Select(A, key(A)), Select(C, key(C)), Select(A, key(A)), ...]
     *     </code>
     * </pre>
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashMap<Integer, IOChannel<IN>> channelMap = new HashMap<Integer, IOChannel<IN>>();
        for (final Integer index : indexes) {
            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [Select(B, key(B)), Select(A, key(A)), Select(C, key(C)), Select(A, key(A)), ...]
     *     </code>
     * </pre>
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels.
     */
    @NotNull
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final int... indexes) {

        final int size = indexes.length;
        final HashMap<Integer, IOChannel<IN>> channelMap =
                new HashMap<Integer, IOChannel<IN>>(size);
        for (final int index : indexes) {
            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of input channels accepting the input data identified by the specified indexes.
     * <br/>
     * Note that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [Select(B, key(B)), Select(A, key(A)), Select(C, key(C)), Select(A, key(A)), ...]
     *     </code>
     * </pre>
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
    public static <DATA, IN extends DATA> Map<Integer, IOChannel<IN>> select(final int startIndex,
            final int rangeSize, @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        if (rangeSize <= 0) {
            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashMap<Integer, IOChannel<IN>> channelMap =
                new HashMap<Integer, IOChannel<IN>>(rangeSize);
        for (int index = startIndex; index < rangeSize; index++) {
            channelMap.put(index, Channels.<DATA, IN>select(channel, index));
        }

        return channelMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, key(A)).data, Select(?, key(A)).data, Select(?, key(A)).data, ...]
     *         B - [Select(?, key(B)).data, Select(?, key(B)).data, Select(?, key(B)).data, ...]
     *         C - [Select(?, key(C)).data, Select(?, key(C)).data, Select(?, key(C)).data, ...]
     *     </code>
     * </pre>
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

        final HashMap<Integer, IOChannel<OUT>> inputMap =
                new HashMap<Integer, IOChannel<OUT>>(rangeSize);
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>(rangeSize);
        for (int index = startIndex; index < rangeSize; index++) {
            final Integer integer = index;
            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(integer, ioChannel);
            outputMap.put(integer, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a selectable collection filtering the data coming from the specified channel.<br/>
     * Note that the passed channel will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned channel, the final output
     * will be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, key(A)).data, Select(?, key(A)).data, Select(?, key(A)).data, ...]
     *         B - [Select(?, key(B)).data, Select(?, key(B)).data, Select(?, key(B)).data, ...]
     *         C - [Select(?, key(C)).data, Select(?, key(C)).data, Select(?, key(C)).data, ...]
     *     </code>
     * </pre>
     *
     * @param channel the selectable output channel.
     * @param <OUT>   the output data type.
     * @return the selectable collection of channels.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> SelectableChannels<OUT> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel) {

        synchronized (sSelectableChannels) {
            final WeakIdentityHashMap<OutputChannel<?>, DefaultSelectableChannels<?>>
                    selectableOutputs = sSelectableChannels;
            DefaultSelectableChannels<?> selectableOutput = selectableOutputs.get(channel);
            if (selectableOutput == null) {
                final DefaultSelectableChannels<OUT> output = new DefaultSelectableChannels<OUT>();
                channel.passTo(output);
                selectableOutputs.put(channel, output);
                return output;
            }

            return (SelectableChannels<OUT>) selectableOutput;
        }
    }

    /**
     * Returns a map of output channels returning the outputs filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, key(A)).data, Select(?, key(A)).data, Select(?, key(A)).data, ...]
     *         B - [Select(?, key(B)).data, Select(?, key(B)).data, Select(?, key(B)).data, ...]
     *         C - [Select(?, key(C)).data, Select(?, key(C)).data, Select(?, key(C)).data, ...]
     *     </code>
     * </pre>
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
        final HashMap<Integer, IOChannel<OUT>> inputMap =
                new HashMap<Integer, IOChannel<OUT>>(size);
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>(size);
        for (final Integer index : indexes) {
            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a map of output channels returning the output data filtered by the specified indexes.
     * <br/>
     * Note that the passed channel will be bound as a result of the call.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, in the returned map, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         A - [Select(?, key(A)).data, Select(?, key(A)).data, Select(?, key(A)).data, ...]
     *         B - [Select(?, key(B)).data, Select(?, key(B)).data, Select(?, key(B)).data, ...]
     *         C - [Select(?, key(C)).data, Select(?, key(C)).data, Select(?, key(C)).data, ...]
     *     </code>
     * </pre>
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

        final HashMap<Integer, IOChannel<OUT>> inputMap = new HashMap<Integer, IOChannel<OUT>>();
        final HashMap<Integer, OutputChannel<OUT>> outputMap =
                new HashMap<Integer, OutputChannel<OUT>>();
        for (final Integer index : indexes) {
            final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
            inputMap.put(index, ioChannel);
            outputMap.put(index, ioChannel);
        }

        channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
        return outputMap;
    }

    /**
     * Returns a new selectable channel feeding the specified one.<br/>
     * Each output will be filtered based on the specified index.<br/>
     * Note that the returned channel <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p/>
     * Given channel {@code A}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(?, I).data, Select(?, I).data, Select(?, I).data, ...]
     *     </code>
     * </pre>
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <IN>    the input data type.
     * @return the selectable I/O channel.
     */
    @NotNull
    public static <IN> IOChannel<Selectable<IN>> toSelectable(
            @NotNull final InputChannel<? super IN> channel, final int index) {

        final IOChannel<Selectable<IN>> inputChannel = JRoutine.io().buildChannel();
        final IOChannel<IN> ioChannel = JRoutine.io().buildChannel();
        ioChannel.passTo(channel);
        return inputChannel.passTo(new FilterOutputConsumer<IN>(ioChannel, index));
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the passed channel will be bound as a result of the call.
     * <p/>
     * Given channel {@code A}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(A, I), Select(A, I), Select(A, I), ...]
     *     </code>
     * </pre>
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable output channel.
     */
    @NotNull
    public static <OUT> OutputChannel<? extends Selectable<OUT>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io().buildChannel();
        channel.passTo(new SelectableOutputConsumer<OUT, OUT>(ioChannel, index));
        return ioChannel;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static IOChannel<List<?>> distribute(final boolean isFlush,
            @Nullable final Object placeholder, @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<IOChannel<?>> channelList = new ArrayList<IOChannel<?>>(length);
        for (final InputChannel<?> channel : channels) {
            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<List<?>> ioChannel = JRoutine.io().buildChannel();
        return ioChannel.passTo(new DistributeOutputConsumer(isFlush, placeholder, channelList));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <IN> IOChannel<List<? extends IN>> distribute(final boolean isFlush,
            @Nullable final IN placeholder,
            @NotNull final List<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final ArrayList<IOChannel<?>> channelList = new ArrayList<IOChannel<?>>(channels.size());
        for (final InputChannel<?> channel : channels) {
            final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
            ioChannel.passTo(((InputChannel<Object>) channel));
            channelList.add(ioChannel);
        }

        final IOChannel<List<? extends IN>> ioChannel = JRoutine.io().buildChannel();
        return ioChannel.passTo(new DistributeOutputConsumer(isFlush, placeholder, channelList));
    }

    @NotNull
    private static <OUT> OutputChannel<List<? extends OUT>> join(final boolean isFlush,
            @Nullable final OUT placeholder,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        final int size = channels.size();
        if (size == 0) {
            throw new IllegalArgumentException("the list of channels must not be empty");
        }

        final IOChannel<List<? extends OUT>> ioChannel = JRoutine.io().buildChannel();
        final JoinOutputConsumer<OUT> consumer =
                new JoinOutputConsumer<OUT>(isFlush, size, placeholder, ioChannel);
        merge(channels).passTo(consumer);
        return ioChannel;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <OUT> OutputChannel<List<? extends OUT>> join(final boolean isFlush,
            @Nullable final Object placeholder, @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final IOChannel<List<? extends OUT>> ioChannel = JRoutine.io().buildChannel();
        final JoinOutputConsumer consumer =
                new JoinOutputConsumer(isFlush, length, placeholder, ioChannel);
        merge(channels).passTo(consumer);
        return ioChannel;
    }

    /**
     * Interface defining a collection of selectable output channels, that is an object filtering
     * selectable data and dispatching them to a specific output channel based on their index.
     *
     * @param <OUT> the output data type.
     */
    public interface SelectableChannels<OUT> {

        /**
         * Returns an output channel returning selectable data matching the specify index.<br/>
         * New output channels can be bound until data start coming After that, any attempt to bound
         * a channel to a new index will cause an exception to be thrown.<br/>
         * Note that the returned channel will employ a synchronous runner to transfer data.
         *
         * @param index the channel index.
         * @return the output channel.
         * @throws java.lang.IllegalStateException if no more output channels can be bound to the
         *                                         output.
         */
        @NotNull
        OutputChannel<OUT> index(int index);
    }

    /**
     * Data class storing information about the origin of the data.
     *
     * @param <DATA> the data type.
     */
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
     * Default implementation of a selectable channel collection.
     *
     * @param <OUT> the output data type.
     */
    private static class DefaultSelectableChannels<OUT>
            implements SelectableChannels<OUT>, OutputConsumer<Selectable<? extends OUT>> {

        private final HashMap<Integer, IOChannel<OUT>> mChannels =
                new HashMap<Integer, IOChannel<OUT>>();

        private final Object mMutex = new Object();

        private boolean mIsOutput;

        @NotNull
        public OutputChannel<OUT> index(final int index) {

            IOChannel<OUT> channel;
            synchronized (mMutex) {
                final HashMap<Integer, IOChannel<OUT>> channels = mChannels;
                channel = channels.get(index);
                if (channel == null) {
                    if (mIsOutput) {
                        throw new IllegalStateException();
                    }

                    channel = JRoutine.io().buildChannel();
                    channels.put(index, channel);
                }
            }

            return channel;
        }

        public void onComplete() {

            synchronized (mMutex) {
                mIsOutput = true;
            }

            final HashMap<Integer, IOChannel<OUT>> channels = mChannels;
            for (final IOChannel<OUT> channel : channels.values()) {
                channel.close();
            }
        }

        public void onOutput(final Selectable<? extends OUT> selectable) {

            synchronized (mMutex) {
                mIsOutput = true;
            }

            final HashMap<Integer, IOChannel<OUT>> channels = mChannels;
            final IOChannel<OUT> channel = channels.get(selectable.index);
            if (channel != null) {
                channel.pass(selectable.data);
            }
        }

        public void onError(@NotNull final RoutineException error) {

            synchronized (mMutex) {
                mIsOutput = true;
            }

            final HashMap<Integer, IOChannel<OUT>> channels = mChannels;
            for (final IOChannel<OUT> channel : channels.values()) {
                channel.abort(error);
            }
        }
    }

    /**
     * Output consumer distributing list of data among a list of channels.
     */
    private static class DistributeOutputConsumer implements OutputConsumer<List<?>> {

        private final ArrayList<IOChannel<?>> mChannels;

        private final boolean mIsFlush;

        private final Object mPlaceholder;

        /**
         * Constructor.
         *
         * @param isFlush     whether the inputs have to be flushed.
         * @param placeholder the placeholder instance.
         * @param channels    the list of channels.
         */
        private DistributeOutputConsumer(final boolean isFlush, @Nullable final Object placeholder,
                @NotNull final ArrayList<IOChannel<?>> channels) {

            mIsFlush = isFlush;
            mChannels = channels;
            mPlaceholder = placeholder;
        }

        public void onComplete() {

            for (final IOChannel<?> channel : mChannels) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<?> channel : mChannels) {
                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final List<?> inputs) {

            final int inputSize = inputs.size();
            final ArrayList<IOChannel<?>> channels = mChannels;
            final int size = channels.size();
            if (inputSize > size) {
                throw new IllegalArgumentException();
            }

            final Object placeholder = mPlaceholder;
            final boolean isFlush = mIsFlush;
            for (int i = 0; i < size; ++i) {
                final IOChannel<Object> channel = (IOChannel<Object>) channels.get(i);
                if (i < inputSize) {
                    channel.pass(inputs.get(i));

                } else if (isFlush) {
                    channel.pass(placeholder);
                }
            }
        }
    }

    /**
     * Output consumer filtering selectable data.
     *
     * @param <IN> the input data type.
     */
    private static class FilterOutputConsumer<IN> implements OutputConsumer<Selectable<IN>> {

        private final IOChannel<? super IN> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the input channel to feed.
         * @param index   the index to filter.
         */
        private FilterOutputConsumer(@NotNull final IOChannel<? super IN> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {

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

        private final IOChannel<List<? extends OUT>> mChannel;

        private final boolean mIsFlush;

        private final OUT mPlaceholder;

        private final SimpleQueue<OUT>[] mQueues;

        /**
         * Constructor.
         *
         * @param isFlush     whether the inputs have to be flushed.
         * @param size        the number of channels to join.
         * @param placeholder the placeholder instance.
         * @param channel     the I/O channel.
         */
        @SuppressWarnings("unchecked")
        private JoinOutputConsumer(final boolean isFlush, final int size,
                @Nullable final OUT placeholder,
                @NotNull final IOChannel<List<? extends OUT>> channel) {

            final SimpleQueue<OUT>[] queues = (mQueues = new SimpleQueue[size]);
            mIsFlush = isFlush;
            mChannel = channel;
            mPlaceholder = placeholder;
            for (int i = 0; i < size; ++i) {
                queues[i] = new SimpleQueue<OUT>();
            }
        }

        private void flush() {

            final IOChannel<List<? extends OUT>> channel = mChannel;
            final SimpleQueue<OUT>[] queues = mQueues;
            final int length = queues.length;
            final OUT placeholder = mPlaceholder;
            final ArrayList<OUT> outputs = new ArrayList<OUT>(length);
            boolean isEmpty;
            do {
                isEmpty = true;
                for (final SimpleQueue<OUT> queue : queues) {
                    if (!queue.isEmpty()) {
                        isEmpty = false;
                        outputs.add(queue.removeFirst());

                    } else {
                        outputs.add(placeholder);
                    }
                }

                if (!isEmpty) {
                    channel.pass(outputs);
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

        public void onError(@NotNull final RoutineException error) {

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
     * I/O channel caching the output data and passing them to newly bound consumer, thus
     * effectively supporting binding of several output consumers.
     *
     * @param <DATA> the data type.
     */
    private static class RepeatedChannel<DATA> extends DefaultIOChannel<DATA>
            implements OutputConsumer<DATA> {

        private final ArrayList<DATA> mCached = new ArrayList<DATA>();

        private final ArrayList<OutputConsumer<? super DATA>> mConsumers =
                new ArrayList<OutputConsumer<? super DATA>>();

        private final Object mMutex = new Object();

        private RoutineException mAbortException;

        private boolean mIsComplete;

        /**
         * Constructor.
         */
        private RepeatedChannel() {

            super(ChannelConfiguration.DEFAULT_CONFIGURATION);
        }

        @NotNull
        @Override
        public IOChannel<DATA> passTo(@NotNull final OutputConsumer<? super DATA> consumer) {

            synchronized (mMutex) {
                final RoutineException abortException = mAbortException;
                if (abortException != null) {
                    consumer.onError(abortException);

                } else {
                    final boolean isComplete = mIsComplete;
                    if (!isComplete) {
                        mConsumers.add(consumer);
                    }

                    for (final DATA data : mCached) {
                        consumer.onOutput(data);
                    }

                    if (isComplete) {
                        consumer.onComplete();
                    }
                }
            }

            return this;
        }

        @NotNull
        @Override
        public <IN extends InputChannel<? super DATA>> IN passTo(@NotNull final IN channel) {

            channel.pass(this);
            return channel;
        }

        public void onComplete() {

            final ArrayList<OutputConsumer<? super DATA>> boundConsumers;
            synchronized (mMutex) {
                mIsComplete = true;
                final ArrayList<OutputConsumer<? super DATA>> consumers = mConsumers;
                boundConsumers = new ArrayList<OutputConsumer<? super DATA>>(consumers);
                consumers.clear();
            }

            for (final OutputConsumer<? super DATA> consumer : boundConsumers) {
                consumer.onComplete();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            final ArrayList<OutputConsumer<? super DATA>> boundConsumers;
            synchronized (mMutex) {
                mAbortException = error;
                final ArrayList<OutputConsumer<? super DATA>> consumers = mConsumers;
                boundConsumers = new ArrayList<OutputConsumer<? super DATA>>(consumers);
                consumers.clear();
            }

            for (final OutputConsumer<? super DATA> consumer : boundConsumers) {
                consumer.onError(error);
            }
        }

        public void onOutput(final DATA output) {

            final ArrayList<OutputConsumer<? super DATA>> boundConsumers;
            synchronized (mMutex) {
                mCached.add(output);
                boundConsumers = new ArrayList<OutputConsumer<? super DATA>>(mConsumers);
            }

            for (final OutputConsumer<? super DATA> consumer : boundConsumers) {
                consumer.onOutput(output);
            }
        }
    }

    /**
     * Output consumer transforming data into selectable ones.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SelectableOutputConsumer<OUT, IN extends OUT>
            implements OutputConsumer<IN> {

        private final IOChannel<? super Selectable<OUT>> mChannel;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param index   the selectable index.
         */
        private SelectableOutputConsumer(@NotNull final IOChannel<? super Selectable<OUT>> channel,
                final int index) {

            mChannel = channel;
            mIndex = index;
        }

        public void onComplete() {

            mChannel.close();
        }

        public void onError(@NotNull final RoutineException error) {

            mChannel.abort(error);
        }

        public void onOutput(final IN input) {

            mChannel.pass(new Selectable<OUT>(input, mIndex));
        }
    }

    /**
     * Output consumer sorting selectable inputs among a list of input channels.
     */
    private static class SortingArrayOutputConsumer implements OutputConsumer<Selectable<?>> {

        private final ArrayList<IOChannel<?>> mChannelList;

        private final int mSize;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the list of channels.
         */
        private SortingArrayOutputConsumer(final int startIndex,
                @NotNull final ArrayList<IOChannel<?>> channels) {

            mStartIndex = startIndex;
            mChannelList = channels;
            mSize = channels.size();
        }

        public void onComplete() {

            for (final IOChannel<?> channel : mChannelList) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<?> channel : mChannelList) {
                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final Selectable<?> selectable) {

            final int index = selectable.index - mStartIndex;
            if ((index < 0) || (index >= mSize)) {
                return;
            }

            final IOChannel<Object> channel = (IOChannel<Object>) mChannelList.get(index);
            if (channel != null) {
                channel.pass(selectable.data);
            }
        }
    }

    /**
     * Output consumer sorting the output data among a map of channels.
     *
     * @param <OUT> the output data type.
     */
    private static class SortingMapOutputConsumer<OUT>
            implements OutputConsumer<Selectable<? extends OUT>> {

        private final HashMap<Integer, IOChannel<OUT>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the map of indexes and I/O channels.
         */
        private SortingMapOutputConsumer(@NotNull final HashMap<Integer, IOChannel<OUT>> channels) {

            mChannels = channels;
        }

        public void onComplete() {

            for (final IOChannel<OUT> channel : mChannels.values()) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<OUT> channel : mChannels.values()) {
                channel.abort(error);
            }
        }

        public void onOutput(final Selectable<? extends OUT> selectable) {

            final IOChannel<OUT> channel = mChannels.get(selectable.index);
            if (channel != null) {
                channel.pass(selectable.data);
            }
        }
    }
}
