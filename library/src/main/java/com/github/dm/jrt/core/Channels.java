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

import com.github.dm.jrt.builder.ChannelConfigurableBuilder;
import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final WeakIdentityHashMap<InputChannel<?>, HashMap<SelectInfo,
            HashMap<Integer, IOChannel<?>>>>
            sInputChannels =
            new WeakIdentityHashMap<InputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                    IOChannel<?>>>>();

    private static final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo,
            HashMap<Integer, OutputChannel<?>>>>
            sOutputChannels =
            new WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                    OutputChannel<?>>>>();

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Returns a builder of output channels blending the outputs coming from the specified ones.
     * <br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [B, A, A, C, B, C, B, A, B, ...]
     *     </code>
     * </pre>
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<OUT>> blend(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new BlendBuilder<OUT>(new ArrayList<OutputChannel<? extends OUT>>(channels));
    }

    /**
     * Returns a builder of output channels blending the outputs coming from the specified ones.
     * <br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> Builder<? extends OutputChannel<OUT>> blend(
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
        Collections.addAll(outputChannels, channels);
        if (outputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (BlendBuilder<OUT>) new BlendBuilder<Object>(outputChannels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final InputChannel<?>... channels) {

        return combine(0, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param <IN>       the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> Builder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex, @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<InputChannel<?>> inputChannels = new ArrayList<InputChannel<?>>();
        Collections.addAll(inputChannels, channels);
        if (inputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (CombineBuilder<IN>) new CombineBuilder<Object>(startIndex, inputChannels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param channels   the collection of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new CombineBuilder<IN>(startIndex,
                                      new ArrayList<InputChannel<? extends IN>>(channels));
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param channels the collection of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return combine(0, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> Builder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, InputChannel<? extends IN>> channelMap =
                new HashMap<Integer, InputChannel<? extends IN>>(channels);
        if (channelMap.containsValue(null)) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        return new AbstractBuilder<IOChannel<Selectable<? extends IN>>>() {

            @NotNull
            @Override
            protected IOChannel<Selectable<? extends IN>> build(
                    @NotNull final ChannelConfiguration configuration) {

                final HashMap<Integer, IOChannel<?>> ioChannelMap =
                        new HashMap<Integer, IOChannel<?>>(channelMap.size());
                for (final Entry<Integer, ? extends InputChannel<?>> entry : channelMap.entrySet
                        ()) {
                    final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
                    ioChannel.passTo(((InputChannel<Object>) entry.getValue()));
                    ioChannelMap.put(entry.getKey(), ioChannel);
                }

                final IOChannel<Selectable<? extends IN>> ioChannel = JRoutine.io()
                                                                              .withChannels()
                                                                              .with(configuration)
                                                                              .configured()
                                                                              .buildChannel();
                ioChannel.passTo(new SortingMapOutputConsumer(ioChannelMap));
                return ioChannel;
            }
        };
    }

    /**
     * Returns a builder of output channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [A, A, A, ..., B, B, B, ..., C, C, C, ...]
     *     </code>
     * </pre>
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<OUT>> concat(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new ConcatBuilder<OUT>(new ArrayList<OutputChannel<? extends OUT>>(channels));
    }

    /**
     * Returns a builder of output channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> Builder<? extends OutputChannel<OUT>> concat(
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
        Collections.addAll(outputChannels, channels);
        if (outputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (ConcatBuilder<OUT>) new ConcatBuilder<Object>(outputChannels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param <IN>     the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final InputChannel<?>... channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param channels the collection of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder, @NotNull final InputChannel<?>... channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @param channels    the collection of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (B, C), ..., (B), ...]
     *     </code>
     * </pre>
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return join(false, null, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            @NotNull final OutputChannel<?>... channels) {

        return join(false, null, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @param channels    the collection of channels.
     * @param <OUT>       the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            @Nullable final OUT placeholder, @NotNull final OutputChannel<?>... channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @param channels   the collection of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new MergeBuilder<OUT>(startIndex,
                                     new ArrayList<OutputChannel<? extends OUT>>(channels));
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            final int startIndex, @NotNull final OutputChannel<?>... channels) {

        if (channels.length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
        Collections.addAll(outputChannels, channels);
        if (outputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (MergeBuilder<OUT>) new MergeBuilder<Object>(startIndex, outputChannels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p/>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [Select(B, key(B)), Select(A, key(A)), Select(C, key(C)), Select(A, key(A)), ...]
     *     </code>
     * </pre>
     *
     * @param channels the map of indexes and output channels.
     * @param <OUT>    the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the map of channels must not be empty");
        }

        final HashMap<Integer, OutputChannel<? extends OUT>> channelMap =
                new HashMap<Integer, OutputChannel<? extends OUT>>(channels);
        if (channelMap.containsValue(null)) {
            throw new NullPointerException("the map of channels must not contain null objects");
        }

        return new AbstractBuilder<OutputChannel<? extends Selectable<OUT>>>() {

            @NotNull
            @Override
            protected OutputChannel<? extends Selectable<OUT>> build(
                    @NotNull final ChannelConfiguration configuration) {

                final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io()
                                                                     .withChannels()
                                                                     .with(configuration)
                                                                     .configured()
                                                                     .buildChannel();
                for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : channelMap
                        .entrySet()) {
                    ioChannel.pass(toSelectable(entry.getValue(), entry.getKey()).build());
                }

                return ioChannel.close();
            }
        };
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of output channels repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting multiple bindings.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the repeating channel builder.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <OUT> Builder<? extends OutputChannel<OUT>> repeat(
            @NotNull final OutputChannel<OUT> channel) {

        if (channel == null) {
            throw new NullPointerException("the output channel must not be null");
        }

        return new AbstractBuilder<OutputChannel<OUT>>() {

            @NotNull
            @Override
            protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

                final RepeatedChannel<OUT> repeatedChannel =
                        new RepeatedChannel<OUT>(configuration);
                channel.passTo((OutputConsumer<OUT>) repeatedChannel);
                return repeatedChannel.close();
            }
        };
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.<br/>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
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
     * @return the I/O channel builder.
     */
    @NotNull
    public static <DATA, IN extends DATA> Builder<? extends IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel, final int index) {

        return new AbstractBuilder<IOChannel<IN>>() {

            @NotNull
            @Override
            protected IOChannel<IN> build(@NotNull final ChannelConfiguration configuration) {

                final IOChannel<IN> inputChannel = JRoutine.io()
                                                           .withChannels()
                                                           .with(configuration)
                                                           .configured()
                                                           .buildChannel();
                final IOChannel<Selectable<DATA>> ioChannel = JRoutine.io().buildChannel();
                ioChannel.passTo(channel);
                return inputChannel.passTo(
                        new SelectableOutputConsumer<DATA, IN>(ioChannel, index));
            }
        };
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
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
     * @return the map of indexes and I/O channels builder.
     */
    @NotNull
    public static <DATA, IN extends DATA> Builder<? extends Map<Integer, IOChannel<IN>>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            if (index == null) {
                throw new NullPointerException(
                        "the iterable of indexes must not return a null object");
            }

            indexSet.add(index);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
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
     * @return the map of indexes and I/O channels builder.
     */
    @NotNull
    public static <DATA, IN extends DATA> Builder<? extends Map<Integer, IOChannel<IN>>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
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
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
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
     * @return the map of indexes and I/O channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <DATA, IN extends DATA> Builder<? extends Map<Integer, IOChannel<IN>>> select(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        if (rangeSize <= 0) {
            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        if (endIndex <= 0) {
            throw new IllegalArgumentException("range overflow: " + startIndex + "..." + endIndex);
        }

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
     * @return the map of indexes and output channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     */
    @NotNull
    public static <OUT> Builder<? extends Map<Integer, OutputChannel<OUT>>> select(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel) {

        if (rangeSize <= 0) {
            throw new IllegalArgumentException("invalid range size: " + rangeSize);
        }

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        if (endIndex <= 0) {
            throw new IllegalArgumentException("range overflow: " + startIndex + "..." + endIndex);
        }

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
     * @return the map of indexes and output channels builder.
     */
    @NotNull
    public static <OUT> Builder<? extends Map<Integer, OutputChannel<OUT>>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
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
     * @return the map of indexes and output channels builder.
     */
    @NotNull
    public static <OUT> Builder<? extends Map<Integer, OutputChannel<OUT>>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            if (index == null) {
                throw new NullPointerException(
                        "the iterable of indexes must not return a null object");
            }

            indexSet.add(index);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of selectable channels feeding the specified one.<br/>
     * Each output will be filtered based on the specified index.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
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
     * @return the selectable I/O channel builder.
     */
    @NotNull
    public static <IN> Builder<? extends IOChannel<Selectable<IN>>> toSelectable(
            @NotNull final InputChannel<? super IN> channel, final int index) {

        return new AbstractBuilder<IOChannel<Selectable<IN>>>() {

            @NotNull
            @Override
            protected IOChannel<Selectable<IN>> build(
                    @NotNull final ChannelConfiguration configuration) {

                final IOChannel<Selectable<IN>> inputChannel = JRoutine.io()
                                                                       .withChannels()
                                                                       .with(configuration)
                                                                       .configured()
                                                                       .buildChannel();
                final IOChannel<IN> ioChannel = JRoutine.io().buildChannel();
                ioChannel.passTo(channel);
                return inputChannel.passTo(new FilterOutputConsumer<IN>(ioChannel, index));
            }
        };
    }

    /**
     * Returns a builder of channels making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
     * @return the selectable output channel builder.
     */
    @NotNull
    public static <OUT> Builder<? extends OutputChannel<? extends Selectable<OUT>>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new AbstractBuilder<OutputChannel<? extends Selectable<OUT>>>() {

            @NotNull
            @Override
            protected OutputChannel<? extends Selectable<OUT>> build(
                    @NotNull final ChannelConfiguration configuration) {

                final IOChannel<Selectable<OUT>> ioChannel = JRoutine.io()
                                                                     .withChannels()
                                                                     .with(configuration)
                                                                     .configured()
                                                                     .buildChannel();
                channel.passTo(new SelectableOutputConsumer<OUT, OUT>(ioChannel, index));
                return ioChannel;
            }
        };
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<InputChannel<?>> inputChannels = new ArrayList<InputChannel<?>>();
        Collections.addAll(inputChannels, channels);
        if (inputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (DistributeBuilder<IN>) new DistributeBuilder<Object>(isFlush, placeholder,
                                                                     inputChannels);
    }

    @NotNull
    private static <IN> Builder<? extends IOChannel<List<? extends IN>>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new DistributeBuilder<IN>(isFlush, placeholder, new ArrayList<InputChannel<?
                extends IN>>(channels));
    }

    @NotNull
    private static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        final int size = channels.size();
        if (size == 0) {
            throw new IllegalArgumentException("the collection of channels must not be empty");
        }

        if (channels.contains(null)) {
            throw new NullPointerException(
                    "the collection of channels must not contain null objects");
        }

        return new JoinBuilder<OUT>(isFlush, placeholder, new ArrayList<OutputChannel<? extends
                OUT>>(channels));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <OUT> Builder<? extends OutputChannel<List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
        Collections.addAll(outputChannels, channels);
        if (outputChannels.contains(null)) {
            throw new NullPointerException("the array of channels must not contain null objects");
        }

        return (JoinBuilder<OUT>) new JoinBuilder<Object>(isFlush, placeholder, outputChannels);
    }

    /**
     * Interface defining a generic configurable builder.
     *
     * @param <TYPE> the built object type.
     */
    public interface Builder<TYPE> extends ChannelConfigurableBuilder<Builder<TYPE>> {

        /**
         * Builds and returns an object instance.
         *
         * @return the object instance.
         */
        @NotNull
        TYPE build();
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
     * Abstract base builder implementation.
     *
     * @param <TYPE> the built object type.
     */
    protected abstract static class AbstractBuilder<TYPE>
            implements Builder<TYPE>, Configurable<Builder<TYPE>> {

        private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

        @NotNull
        public TYPE build() {

            return build(mConfiguration);
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public Builder<TYPE> setConfiguration(@NotNull final ChannelConfiguration configuration) {

            if (configuration == null) {
                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            return this;
        }

        @NotNull
        public ChannelConfiguration.Builder<Builder<TYPE>> withChannels() {

            return new ChannelConfiguration.Builder<Builder<TYPE>>(this, mConfiguration);
        }

        /**
         * Builds and returns an object instance.
         *
         * @param configuration the instance configuration.
         * @return the object instance.
         */
        @NotNull
        protected abstract TYPE build(@NotNull ChannelConfiguration configuration);
    }

    /**
     * Builder implementation blending data from a set of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class BlendBuilder<OUT> extends AbstractBuilder<OutputChannel<OUT>> {

        private final ArrayList<OutputChannel<? extends OUT>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the output channels to blend.
         */
        private BlendBuilder(@NotNull final ArrayList<OutputChannel<? extends OUT>> channels) {

            mChannels = channels;
        }

        @NotNull
        @Override
        protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

            final IOChannel<OUT> ioChannel =
                    JRoutine.io().withChannels().with(configuration).configured().buildChannel();
            for (final OutputChannel<? extends OUT> channel : mChannels) {
                channel.passTo(ioChannel);
            }

            return ioChannel.close();
        }
    }

    /**
     * Builder implementation combining data from a set of input channels.
     *
     * @param <IN> the input data type.
     */
    private static class CombineBuilder<IN>
            extends AbstractBuilder<IOChannel<Selectable<? extends IN>>> {

        private final ArrayList<InputChannel<? extends IN>> mChannels;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the input channels to combine.
         */
        private CombineBuilder(final int startIndex,
                @NotNull final ArrayList<InputChannel<? extends IN>> channels) {

            mStartIndex = startIndex;
            mChannels = channels;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected IOChannel<Selectable<? extends IN>> build(
                @NotNull final ChannelConfiguration configuration) {

            final ArrayList<InputChannel<? extends IN>> channels = mChannels;
            final ArrayList<IOChannel<?>> channelList =
                    new ArrayList<IOChannel<?>>(channels.size());
            for (final InputChannel<?> channel : channels) {
                final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
                ioChannel.passTo(((InputChannel<Object>) channel));
                channelList.add(ioChannel);
            }

            final IOChannel<Selectable<? extends IN>> ioChannel =
                    JRoutine.io().withChannels().with(configuration).configured().buildChannel();
            ioChannel.passTo(new SortingArrayOutputConsumer(mStartIndex, channelList));
            return ioChannel;
        }
    }

    /**
     * Builder implementation concatenating data from a set of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class ConcatBuilder<OUT> extends AbstractBuilder<OutputChannel<OUT>> {

        private final ArrayList<OutputChannel<? extends OUT>> mChannels;

        /**
         * Constructor.
         *
         * @param channels the output channels to concat.
         */
        private ConcatBuilder(@NotNull final ArrayList<OutputChannel<? extends OUT>> channels) {

            mChannels = channels;
        }

        @NotNull
        @Override
        protected OutputChannel<OUT> build(@NotNull final ChannelConfiguration configuration) {

            final IOChannel<OUT> ioChannel = JRoutine.io()
                                                     .withChannels()
                                                     .with(configuration)
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .configured()
                                                     .buildChannel();
            for (final OutputChannel<? extends OUT> channel : mChannels) {
                channel.passTo(ioChannel);
            }

            return ioChannel.close();
        }
    }

    /**
     * Builder implementation distributing data into a set of input channels.
     *
     * @param <IN> the input data type.
     */
    private static class DistributeBuilder<IN>
            extends AbstractBuilder<IOChannel<List<? extends IN>>> {

        private final ArrayList<InputChannel<? extends IN>> mChannels;

        private final boolean mIsFlush;

        private final IN mPlaceholder;

        /**
         * Constructor.
         *
         * @param isFlush     whether to flush data.
         * @param placeholder the placeholder instance.
         * @param channels    the list of channels.
         */
        private DistributeBuilder(final boolean isFlush, @Nullable final IN placeholder,
                @NotNull final ArrayList<InputChannel<? extends IN>> channels) {

            mIsFlush = isFlush;
            mPlaceholder = placeholder;
            mChannels = channels;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected IOChannel<List<? extends IN>> build(
                @NotNull final ChannelConfiguration configuration) {

            final ArrayList<InputChannel<? extends IN>> channels = mChannels;
            final ArrayList<IOChannel<?>> channelList =
                    new ArrayList<IOChannel<?>>(channels.size());
            for (final InputChannel<?> channel : channels) {
                final IOChannel<?> ioChannel = JRoutine.io().buildChannel();
                ioChannel.passTo(((InputChannel<Object>) channel));
                channelList.add(ioChannel);
            }

            final IOChannel<List<? extends IN>> ioChannel =
                    JRoutine.io().withChannels().with(configuration).configured().buildChannel();
            return ioChannel.passTo(
                    new DistributeOutputConsumer(mIsFlush, mPlaceholder, channelList));
        }
    }

    /**
     * Output consumer distributing list of data among a list of channels.
     *
     * @param <IN> the input data type.
     */
    private static class DistributeOutputConsumer<IN>
            implements OutputConsumer<List<? extends IN>> {

        private final ArrayList<IOChannel<? extends IN>> mChannels;

        private final boolean mIsFlush;

        private final IN mPlaceholder;

        /**
         * Constructor.
         *
         * @param isFlush     whether the inputs have to be flushed.
         * @param placeholder the placeholder instance.
         * @param channels    the list of channels.
         */
        private DistributeOutputConsumer(final boolean isFlush, @Nullable final IN placeholder,
                @NotNull final ArrayList<IOChannel<? extends IN>> channels) {

            mIsFlush = isFlush;
            mChannels = channels;
            mPlaceholder = placeholder;
        }

        public void onComplete() {

            for (final IOChannel<? extends IN> channel : mChannels) {
                channel.close();
            }
        }

        public void onError(@NotNull final RoutineException error) {

            for (final IOChannel<? extends IN> channel : mChannels) {
                channel.abort(error);
            }
        }

        @SuppressWarnings("unchecked")
        public void onOutput(final List<? extends IN> inputs) {

            final int inputSize = inputs.size();
            final ArrayList<IOChannel<? extends IN>> channels = mChannels;
            final int size = channels.size();
            if (inputSize > size) {
                throw new IllegalArgumentException();
            }

            final IN placeholder = mPlaceholder;
            final boolean isFlush = mIsFlush;
            for (int i = 0; i < size; ++i) {
                final IOChannel<IN> channel = (IOChannel<IN>) channels.get(i);
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

    // TODO: 20/02/16 javadoc
    private static class InputMapBuilder<DATA, IN extends DATA>
            extends AbstractBuilder<Map<Integer, IOChannel<IN>>> {

        private final InputChannel<? super Selectable<DATA>> mChannel;

        private final HashSet<Integer> mIndexes;

        private InputMapBuilder(@NotNull final InputChannel<? super Selectable<DATA>> channel,
                @NotNull final HashSet<Integer> indexes) {

            mChannel = channel;
            mIndexes = indexes;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected Map<Integer, IOChannel<IN>> build(
                @NotNull final ChannelConfiguration configuration) {

            final HashSet<Integer> indexes = mIndexes;
            final InputChannel<? super Selectable<DATA>> channel = mChannel;
            synchronized (sInputChannels) {
                final WeakIdentityHashMap<InputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                        IOChannel<?>>>>
                        inputChannels = sInputChannels;
                HashMap<SelectInfo, HashMap<Integer, IOChannel<?>>> channelMaps =
                        inputChannels.get(channel);
                if (channelMaps == null) {
                    channelMaps = new HashMap<SelectInfo, HashMap<Integer, IOChannel<?>>>();
                    inputChannels.put(channel, channelMaps);
                }

                final int size = indexes.size();
                final SelectInfo selectInfo = new SelectInfo(configuration, indexes);
                final HashMap<Integer, IOChannel<IN>> channelMap =
                        new HashMap<Integer, IOChannel<IN>>(size);
                HashMap<Integer, IOChannel<?>> channels = channelMaps.get(selectInfo);
                if (channels != null) {
                    for (final Entry<Integer, IOChannel<?>> entry : channels.entrySet()) {
                        channelMap.put(entry.getKey(), (IOChannel<IN>) entry.getValue());
                    }

                } else {
                    channels = new HashMap<Integer, IOChannel<?>>(size);
                    for (final Integer index : indexes) {
                        final IOChannel<IN> ioChannel = Channels.<DATA, IN>select(channel, index)
                                                                .withChannels()
                                                                .with(configuration)
                                                                .configured()
                                                                .build();
                        channelMap.put(index, ioChannel);
                        channels.put(index, ioChannel);
                    }

                    channelMaps.put(selectInfo, channels);
                }

                return channelMap;
            }
        }
    }

    /**
     * Builder implementation joining data from a set of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class JoinBuilder<OUT>
            extends AbstractBuilder<OutputChannel<List<? extends OUT>>> {

        private final ArrayList<OutputChannel<? extends OUT>> mChannels;

        private final boolean mIsFlush;

        private final OUT mPlaceholder;

        /**
         * Constructor.
         *
         * @param isFlush     whether to flush data.
         * @param placeholder the placeholder instance.
         * @param channels    the input channels to join.
         */
        private JoinBuilder(final boolean isFlush, @Nullable final OUT placeholder,
                @NotNull final ArrayList<OutputChannel<? extends OUT>> channels) {

            mIsFlush = isFlush;
            mPlaceholder = placeholder;
            mChannels = channels;
        }

        @NotNull
        @Override
        protected OutputChannel<List<? extends OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final ArrayList<OutputChannel<? extends OUT>> channels = mChannels;
            final IOChannel<List<? extends OUT>> ioChannel =
                    JRoutine.io().withChannels().with(configuration).configured().buildChannel();
            final JoinOutputConsumer<OUT> consumer =
                    new JoinOutputConsumer<OUT>(mIsFlush, channels.size(), mPlaceholder, ioChannel);
            merge(channels).build().passTo(consumer);
            return ioChannel;
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
     * Builder implementation merging data from a set of output channels into selectable objects.
     *
     * @param <OUT> the output data type.
     */
    private static class MergeBuilder<OUT>
            extends AbstractBuilder<OutputChannel<? extends Selectable<OUT>>> {

        private final ArrayList<OutputChannel<? extends OUT>> mChannels;

        private final int mStartIndex;

        /**
         * Constructor.
         *
         * @param startIndex the selectable start index.
         * @param channels   the input channels to merge.
         */
        private MergeBuilder(final int startIndex,
                @NotNull final ArrayList<OutputChannel<? extends OUT>> channels) {

            mStartIndex = startIndex;
            mChannels = channels;
        }

        @NotNull
        @Override
        protected OutputChannel<? extends Selectable<OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final IOChannel<Selectable<OUT>> ioChannel =
                    JRoutine.io().withChannels().with(configuration).configured().buildChannel();
            int i = mStartIndex;
            for (final OutputChannel<? extends OUT> channel : mChannels) {
                ioChannel.pass(toSelectable(channel, i++).build());
            }

            return ioChannel.close();
        }
    }

    // TODO: 20/02/16 javadoc
    private static class OutputMapBuilder<OUT>
            extends AbstractBuilder<Map<Integer, OutputChannel<OUT>>> {

        private final OutputChannel<? extends Selectable<? extends OUT>> mChannel;

        private final HashSet<Integer> mIndexes;

        private OutputMapBuilder(
                @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
                @NotNull final HashSet<Integer> indexes) {

            mChannel = channel;
            mIndexes = indexes;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected Map<Integer, OutputChannel<OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final HashSet<Integer> indexes = mIndexes;
            final OutputChannel<? extends Selectable<? extends OUT>> channel = mChannel;
            synchronized (sOutputChannels) {
                final WeakIdentityHashMap<OutputChannel<?>, HashMap<SelectInfo, HashMap<Integer,
                        OutputChannel<?>>>>
                        outputChannels = sOutputChannels;
                HashMap<SelectInfo, HashMap<Integer, OutputChannel<?>>> channelMaps =
                        outputChannels.get(channel);
                if (channelMaps == null) {
                    channelMaps = new HashMap<SelectInfo, HashMap<Integer, OutputChannel<?>>>();
                    outputChannels.put(channel, channelMaps);
                }

                final int size = indexes.size();
                final SelectInfo selectInfo = new SelectInfo(configuration, indexes);
                final HashMap<Integer, OutputChannel<OUT>> channelMap =
                        new HashMap<Integer, OutputChannel<OUT>>(size);
                HashMap<Integer, OutputChannel<?>> channels = channelMaps.get(selectInfo);
                if (channels != null) {
                    for (final Entry<Integer, OutputChannel<?>> entry : channels.entrySet()) {
                        channelMap.put(entry.getKey(), (OutputChannel<OUT>) entry.getValue());
                    }

                } else {
                    final HashMap<Integer, IOChannel<OUT>> inputMap =
                            new HashMap<Integer, IOChannel<OUT>>(size);
                    channels = new HashMap<Integer, OutputChannel<?>>(size);
                    for (final Integer index : indexes) {
                        final IOChannel<OUT> ioChannel = JRoutine.io()
                                                                 .withChannels()
                                                                 .with(configuration)
                                                                 .configured()
                                                                 .buildChannel();
                        inputMap.put(index, ioChannel);
                        channelMap.put(index, ioChannel);
                        channels.put(index, ioChannel);
                    }

                    channel.passTo(new SortingMapOutputConsumer<OUT>(inputMap));
                    channelMaps.put(selectInfo, channels);
                }

                return channelMap;
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

        private final Logger mLogger;

        private final Object mMutex = new Object();

        private RoutineException mAbortException;

        private boolean mIsComplete;

        /**
         * Constructor.
         *
         * @param configuration the channel configuration.
         */
        private RepeatedChannel(@NotNull final ChannelConfiguration configuration) {

            super(configuration);
            mLogger = configuration.toInvocationConfiguration().newLogger(this);
        }

        @NotNull
        @Override
        public IOChannel<DATA> passTo(@NotNull final OutputConsumer<? super DATA> consumer) {

            synchronized (mMutex) {
                final Logger logger = mLogger;
                final RoutineException abortException = mAbortException;
                if (abortException != null) {
                    try {
                        consumer.onError(abortException);

                    } catch (final RoutineException e) {
                        InvocationInterruptedException.throwIfInterrupt(e);
                        logger.wrn(e, "ignoring consumer exception (%s)", consumer);

                    } catch (final Throwable t) {
                        InvocationInterruptedException.throwIfInterrupt(t);
                        logger.err(t, "ignoring consumer exception (%s)", consumer);
                    }

                } else {
                    final boolean isComplete = mIsComplete;
                    if (!isComplete) {
                        mConsumers.add(consumer);
                    }

                    try {
                        for (final DATA data : mCached) {
                            consumer.onOutput(data);
                        }

                        try {
                            if (isComplete) {
                                consumer.onComplete();
                            }

                        } catch (final RoutineException e) {
                            InvocationInterruptedException.throwIfInterrupt(e);
                            logger.wrn(e, "ignoring consumer exception (%s)", consumer);

                        } catch (final Throwable t) {
                            InvocationInterruptedException.throwIfInterrupt(t);
                            logger.err(t, "ignoring consumer exception (%s)", consumer);
                        }

                    } catch (final Throwable t) {
                        InvocationInterruptedException.throwIfInterrupt(t);
                        mAbortException = AbortException.wrapIfNeeded(t);
                        abort(t);
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

        public void onComplete() throws Exception {

            final ArrayList<OutputConsumer<? super DATA>> boundConsumers;
            synchronized (mMutex) {
                mIsComplete = true;
                final ArrayList<OutputConsumer<? super DATA>> consumers = mConsumers;
                boundConsumers = new ArrayList<OutputConsumer<? super DATA>>(consumers);
                consumers.clear();
            }

            final Logger logger = mLogger;
            for (final OutputConsumer<? super DATA> consumer : boundConsumers) {
                try {
                    consumer.onComplete();

                } catch (final RoutineException e) {
                    InvocationInterruptedException.throwIfInterrupt(e);
                    logger.wrn(e, "ignoring consumer exception (%s)", consumer);

                } catch (final Throwable t) {
                    InvocationInterruptedException.throwIfInterrupt(t);
                    logger.err(t, "ignoring consumer exception (%s)", consumer);
                }
            }
        }

        public void onError(@NotNull final RoutineException error) throws Exception {

            final ArrayList<OutputConsumer<? super DATA>> boundConsumers;
            synchronized (mMutex) {
                mAbortException = error;
                final ArrayList<OutputConsumer<? super DATA>> consumers = mConsumers;
                boundConsumers = new ArrayList<OutputConsumer<? super DATA>>(consumers);
                consumers.clear();
            }

            final Logger logger = mLogger;
            for (final OutputConsumer<? super DATA> consumer : boundConsumers) {
                try {
                    consumer.onError(error);

                } catch (final RoutineException e) {
                    InvocationInterruptedException.throwIfInterrupt(e);
                    logger.wrn(e, "ignoring consumer exception (%s)", consumer);

                } catch (final Throwable t) {
                    InvocationInterruptedException.throwIfInterrupt(t);
                    logger.err(t, "ignoring consumer exception (%s)", consumer);
                }
            }
        }

        public void onOutput(final DATA output) throws Exception {

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

    // TODO: 2/19/16 channelMap, list, builder

    // TODO: 2/19/16 javadoc
    private static class SelectInfo {

        private final ChannelConfiguration mConfiguration;

        private final HashSet<Integer> mIndexes;

        private SelectInfo(@NotNull final ChannelConfiguration configuration,
                @NotNull final HashSet<Integer> indexes) {

            mConfiguration = configuration;
            mIndexes = indexes;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof SelectInfo)) {
                return false;
            }

            final SelectInfo that = (SelectInfo) o;
            return mConfiguration.equals(that.mConfiguration) && mIndexes.equals(that.mIndexes);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mConfiguration.hashCode();
            result = 31 * result + mIndexes.hashCode();
            return result;
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
