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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Utility class for handling routine channels.
 * <br>
 * The class provides several methods to split and merge channels together, making also possible to
 * transfer data in multiple sub-channels, each one identified by a specific index.
 * <p>
 * Created by davide-maestroni on 03/15/2015.
 */
public class Channels {

    /**
     * Avoid explicit instantiation.
     */
    protected Channels() {

    }

    /**
     * Returns a builder of output channels blending the outputs coming from the specified ones.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<OUT>> blend(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BlendBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of output channels blending the outputs coming from the specified ones.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> ChannelsBuilder<? extends OutputChannel<OUT>> blend(
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (BlendBuilder<OUT>) new BlendBuilder<Object>(Arrays.asList(channels));
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final InputChannel<?>... channels) {

        return combine(0, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex, @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (CombineBuilder<IN>) new CombineBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return new CombineBuilder<IN>(startIndex, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return combine(0, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        return new CombineMapBuilder<IN>(channels);
    }

    /**
     * Returns a builder of output channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<OUT>> concat(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new ConcatBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of output channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> ChannelsBuilder<? extends OutputChannel<OUT>> concat(
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (ConcatBuilder<OUT>) new ConcatBuilder<Object>(Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final InputChannel<?>... channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1), PH, PH, ...]
     *         C - [list(2), list(2), list(2), ..., list(2), PH, PH, ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder, @NotNull final InputChannel<?>... channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         A - [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
     *         B - [list(1), list(1), list(1), ..., list(1), ..., list(1), PH, PH, ...]
     *         C - [list(2), list(2), list(2), ..., list(2), PH, PH, ...]
     *     </code>
     * </pre>
     *
     * @param placeholder the placeholder instance.
     * @param channels    the collection of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return join(false, null, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            @NotNull final OutputChannel<?>... channels) {

        return join(false, null, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified channels.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            @Nullable final OUT placeholder, @NotNull final OutputChannel<?>... channels) {

        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new MergeBuilder<OUT>(startIndex, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            final int startIndex, @NotNull final OutputChannel<?>... channels) {

        if (channels.length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (MergeBuilder<OUT>) new MergeBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {

        return new MergeMapBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of output channels merging the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return merge(0, channels);
    }

    /**
     * Returns a builder of output channels repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting multiple bindings.
     * <p>
     * The {@link com.github.dm.jrt.core.channel.Channel.OutputChannel#isBound()} method will always
     * return false and the bound methods will never fail.
     * <br>
     * Note, however, that the implementation will silently prevent the same consumer or channel
     * instance to be bound twice.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
    public static <OUT> ChannelsBuilder<? extends OutputChannel<OUT>> repeat(
            @NotNull final OutputChannel<OUT> channel) {

        return new RepeatedChannelBuilder<OUT>(channel);
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     * <p>
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
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel, final int index) {

        return new InputSelectBuilder<DATA, IN>(channel, index);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.
     * <p>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
     *                                        object.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            indexSet.add(index);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.
     * <p>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     * <p>
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
     * @throws java.lang.NullPointerException if the specified array is null or contains a null
     *                                        object.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
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
     * indexes.
     * <p>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        ConstantConditions.positive("range size", rangeSize);
        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        for (int i = startIndex; i < endIndex; i++) {
            indexSet.add(i);
        }

        return new InputMapBuilder<DATA, IN>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of output channels returning the output data filtered by the
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> select(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel) {

        ConstantConditions.positive("range size", rangeSize);
        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        for (int i = startIndex; i < endIndex; i++) {
            indexSet.add(i);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of output channels returning the output data filtered by the
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException if the specified array is null or contains a null
     *                                        object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> select(
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
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     * <p>
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
     * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
     *                                        object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, OutputChannel<OUT>>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        final HashSet<Integer> indexSet = new HashSet<Integer>();
        for (final Integer index : indexes) {
            indexSet.add(index);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of selectable channels feeding the specified one.
     * <br>
     * Each output will be filtered based on the specified index.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     * <p>
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
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<IN>>> toSelectable(
            @NotNull final InputChannel<? super IN> channel, final int index) {

        return new InputFilterBuilder<IN>(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that the builder will successfully create only one output channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <p>
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
    public static <OUT> ChannelsBuilder<? extends OutputChannel<? extends Selectable<OUT>>>
    toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new SelectableOutputBuilder<OUT>(channel, index);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final InputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (DistributeBuilder<IN>) new DistributeBuilder<Object>(isFlush, placeholder,
                Arrays.asList(channels));
    }

    @NotNull
    private static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return new DistributeBuilder<IN>(isFlush, placeholder, channels);
    }

    @NotNull
    private static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new JoinBuilder<OUT>(isFlush, placeholder, channels);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <OUT> ChannelsBuilder<? extends OutputChannel<List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final OutputChannel<?>... channels) {

        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (JoinBuilder<OUT>) new JoinBuilder<Object>(isFlush, placeholder,
                Arrays.asList(channels));
    }

    /**
     * Builder implementation returning a map of input channels accepting selectable data.
     *
     * @param <DATA> the channel data type.
     * @param <IN>   the input data type.
     */
    private static class InputMapBuilder<DATA, IN extends DATA>
            extends AbstractBuilder<Map<Integer, IOChannel<IN>>> {

        private final InputChannel<? super Selectable<DATA>> mChannel;

        private final HashSet<Integer> mIndexes;

        /**
         * Constructor.
         *
         * @param channel the selectable channel.
         * @param indexes the set of indexes.
         * @throws java.lang.NullPointerException if the specified set of indexes is null or
         *                                        contains a null object.
         */
        private InputMapBuilder(@NotNull final InputChannel<? super Selectable<DATA>> channel,
                @NotNull final Set<Integer> indexes) {

            mChannel = ConstantConditions.notNull("input channel", channel);
            final HashSet<Integer> indexSet =
                    new HashSet<Integer>(ConstantConditions.notNull("set of indexes", indexes));
            if (indexSet.contains(null)) {
                throw new NullPointerException("the set of indexes must not contain null objects");
            }

            mIndexes = indexSet;
        }

        @NotNull
        @Override
        protected Map<Integer, IOChannel<IN>> build(
                @NotNull final ChannelConfiguration configuration) {

            final HashSet<Integer> indexes = mIndexes;
            final InputChannel<? super Selectable<DATA>> channel = mChannel;
            final HashMap<Integer, IOChannel<IN>> channelMap =
                    new HashMap<Integer, IOChannel<IN>>(indexes.size());
            for (final Integer index : indexes) {
                final IOChannel<IN> ioChannel = Channels.<DATA, IN>select(channel, index)
                                                        .channelConfiguration()
                                                        .with(configuration)
                                                        .apply()
                                                        .buildChannels();
                channelMap.put(index, ioChannel);
            }

            return channelMap;
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
         * @throws java.lang.IllegalArgumentException if the specified collection is empty.
         * @throws java.lang.NullPointerException     if the specified collection is null or
         *                                            contains a null object.
         */
        private JoinBuilder(final boolean isFlush, @Nullable final OUT placeholder,
                @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

            if (channels.isEmpty()) {
                throw new IllegalArgumentException("the collection of channels must not be empty");
            }

            final ArrayList<OutputChannel<? extends OUT>> channelList =
                    new ArrayList<OutputChannel<? extends OUT>>(channels);
            if (channelList.contains(null)) {
                throw new NullPointerException(
                        "the collection of channels must not contain null objects");
            }

            mIsFlush = isFlush;
            mPlaceholder = placeholder;
            mChannels = channelList;
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
        }

        @NotNull
        @Override
        protected OutputChannel<List<? extends OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final ArrayList<OutputChannel<? extends OUT>> channels = mChannels;
            final IOChannel<List<? extends OUT>> ioChannel = JRoutineCore.io()
                                                                         .channelConfiguration()
                                                                         .with(configuration)
                                                                         .apply()
                                                                         .buildChannel();
            final JoinOutputConsumer<OUT> consumer =
                    new JoinOutputConsumer<OUT>(mIsFlush, channels.size(), mPlaceholder, ioChannel);
            merge(channels).buildChannels().bind(consumer);
            return ioChannel;
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
         * @throws java.lang.IllegalArgumentException if the specified collection is empty.
         * @throws java.lang.NullPointerException     if the specified collection is null or
         *                                            contains a null object.
         */
        private MergeBuilder(final int startIndex,
                @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

            if (channels.isEmpty()) {
                throw new IllegalArgumentException("the collection of channels must not be empty");
            }

            final ArrayList<OutputChannel<? extends OUT>> channelList =
                    new ArrayList<OutputChannel<? extends OUT>>(channels);
            if (channelList.contains(null)) {
                throw new NullPointerException(
                        "the collection of channels must not contain null objects");
            }

            mStartIndex = startIndex;
            mChannels = channelList;
        }

        @NotNull
        @Override
        protected OutputChannel<? extends Selectable<OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final IOChannel<Selectable<OUT>> ioChannel = JRoutineCore.io()
                                                                     .channelConfiguration()
                                                                     .with(configuration)
                                                                     .apply()
                                                                     .buildChannel();
            int i = mStartIndex;
            for (final OutputChannel<? extends OUT> channel : mChannels) {
                ioChannel.pass(toSelectable(channel, i++).buildChannels());
            }

            return ioChannel.close();
        }
    }

    /**
     * Builder implementation returning a channel merging data from a map of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class MergeMapBuilder<OUT>
            extends AbstractBuilder<OutputChannel<? extends Selectable<OUT>>> {

        private final HashMap<Integer, OutputChannel<? extends OUT>> mChannelMap;

        /**
         * Constructor.
         *
         * @param channels the map of channels to merge.
         * @throws java.lang.IllegalArgumentException if the specified map is empty.
         * @throws java.lang.NullPointerException     if the specified map is null or contains a
         *                                            null object.
         */
        private MergeMapBuilder(
                @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {

            if (channels.isEmpty()) {
                throw new IllegalArgumentException("the map of channels must not be empty");
            }

            final HashMap<Integer, OutputChannel<? extends OUT>> channelMap =
                    new HashMap<Integer, OutputChannel<? extends OUT>>(channels);
            if (channelMap.containsValue(null)) {
                throw new NullPointerException("the map of channels must not contain null objects");
            }

            mChannelMap = channelMap;
        }

        @NotNull
        @Override
        protected OutputChannel<? extends Selectable<OUT>> build(
                @NotNull final ChannelConfiguration configuration) {

            final IOChannel<Selectable<OUT>> ioChannel = JRoutineCore.io()
                                                                     .channelConfiguration()
                                                                     .with(configuration)
                                                                     .apply()
                                                                     .buildChannel();
            for (final Entry<Integer, ? extends OutputChannel<? extends OUT>> entry : mChannelMap
                    .entrySet()) {
                ioChannel.pass(toSelectable(entry.getValue(), entry.getKey()).buildChannels());
            }

            return ioChannel.close();
        }
    }
}
