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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
        ConstantConditions.avoid();
    }

    /**
     * Returns a builder of channels blending the outputs coming from the specified ones.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     * <p>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [B, A, A, C, B, C, B, A, B, ...]
     *     </code>
     * </pre>
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, OUT>> blend(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BlendBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of channels blending the outputs coming from the specified ones.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, OUT>> blend(
            @NotNull final Channel<?, ?>... channels) {
        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (BlendBuilder<OUT>) new BlendBuilder<Object>(Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Channel<?, ?>... channels) {
        return combine(0, channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex, @NotNull final Channel<?, ?>... channels) {
        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (CombineBuilder<IN>) new CombineBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @param channels   the iterable of input channels.
     * @param <IN>       the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return new CombineBuilder<IN>(startIndex, channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the iterable.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @param channels the iterable of input channels.
     * @param <IN>     the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return combine(0, channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
        return new CombineMapBuilder<IN>(channels);
    }

    /**
     * Returns a builder of channels concatenating the outputs coming from the specified ones, so
     * that, all the outputs of the first channel will come before all the outputs of the second
     * one, and so on.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     * <p>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [A, A, A, ..., B, B, B, ..., C, C, C, ...]
     *     </code>
     * </pre>
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, OUT>> concat(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new ConcatBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of channels concatenating the outputs coming from the specified ones, so
     * that, all the outputs of the first channel will come before all the outputs of the second
     * one, and so on.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, OUT>> concat(
            @NotNull final Channel<?, ?>... channels) {
        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (ConcatBuilder<OUT>) new ConcatBuilder<Object>(Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Channel<?, ?>... channels) {
        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @param channels the iterable of channels.
     * @param <IN>     the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return distribute(false, null, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data is smaller than the specified number of channels, the remaining ones will be fed
     * with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder, @NotNull final Channel<?, ?>... channels) {
        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data is smaller than the specified number of channels, the remaining ones will be fed
     * with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @param channels    the iterable of channels.
     * @param <IN>        the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return distribute(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
     * <p>
     * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
     * <pre>
     *     <code>
     *
     *         [(A, B, C), (A, B, C), ..., (B, C), ..., (B), ...]
     *     </code>
     * </pre>
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return join(false, null, channels);
    }

    /**
     * Returns a builder of output channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            @NotNull final Channel<?, ?>... channels) {
        return join(false, null, channels);
    }

    /**
     * Returns a builder of channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
     * @param channels    the iterable of channels.
     * @param <OUT>       the output data type.
     * @return the output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            @Nullable final OUT placeholder,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size as the channel list.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            @Nullable final OUT placeholder, @NotNull final Channel<?, ?>... channels) {
        return join(true, placeholder, channels);
    }

    /**
     * Returns a builder of channels merging the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
     * @param channels   the iterable of channels.
     * @param <OUT>      the output data type.
     * @return the selectable output channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>> merge(
            final int startIndex,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new MergeBuilder<OUT>(startIndex, channels);
    }

    /**
     * Returns a builder of channels merging the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>> merge(
            final int startIndex, @NotNull final Channel<?, ?>... channels) {
        if (channels.length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (MergeBuilder<OUT>) new MergeBuilder<Object>(startIndex, Arrays.asList(channels));
    }

    /**
     * Returns a builder of channels merging the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the iterable.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>> merge(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return merge(0, channels);
    }

    /**
     * Returns a builder of channels merging the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>> merge(
            @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
        return new MergeMapBuilder<OUT>(channels);
    }

    /**
     * Returns a builder of channels merging the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>> merge(
            @NotNull final Channel<?, ?>... channels) {
        return merge(0, channels);
    }

    /**
     * Returns a builder of channels repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting multiple bindings.
     * <p>
     * The {@link com.github.dm.jrt.core.channel.Channel#isBound()} method will always return false
     * and the bound methods will never fail.
     * <br>
     * Note, however, that the implementation will silently prevent the same consumer or channel
     * instance to be bound twice.
     * <p>
     * Note that the builder will successfully create only one channel instance, and that the passed
     * ones will be bound as a result of the creation.
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
     * @return the replaying channel builder.
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Channel<?, OUT>> replay(
            @NotNull final Channel<?, OUT> channel) {
        return new ReplayChannelBuilder<OUT>(channel);
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.
     * <p>
     * Note that the builder will successfully create several channel instances.
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
     * @return the channel builder.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Channel<IN, ?>> selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel, final int index) {
        return new InputSelectBuilder<DATA, IN>(channel, index);
    }

    /**
     * Returns a builder of maps of channels accepting the data identified by the specified indexes.
     * <p>
     * Note that the builder will successfully create several channel map instances.
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
     * @return the map of indexes and channels builder.
     * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
     *                                        object.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
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
     * @return the map of indexes and channels builder.
     * @throws java.lang.NullPointerException if the specified array is null or contains a null
     *                                        object.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
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
     * @return the map of indexes and channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<? super Selectable<DATA>, ?> channel) {
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
    public static <OUT> ChannelsBuilder<? extends Map<Integer, Channel<?, OUT>>> selectOutput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel) {
        ConstantConditions.positive("range size", rangeSize);
        final HashSet<Integer> indexSet = new HashSet<Integer>();
        final int endIndex = startIndex + rangeSize;
        for (int i = startIndex; i < endIndex; ++i) {
            indexSet.add(i);
        }

        return new OutputMapBuilder<OUT>(channel, indexSet);
    }

    /**
     * Returns a builder of maps of channels returning the output data filtered by the specified
     * indexes.
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
    public static <OUT> ChannelsBuilder<? extends Map<Integer, Channel<?, OUT>>> selectOutput(
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel,
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
    public static <OUT> ChannelsBuilder<? extends Map<Integer, Channel<?, OUT>>> selectOutput(
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel,
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
     * Note that the builder will return the same instance for the same input and equal
     * configuration.
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
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<IN>, ?>> selectableInput(
            @NotNull final Channel<? super IN, ?> channel, final int index) {
        return new InputFilterBuilder<IN>(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that the builder will successfully create only one channel instance.
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
    public static <OUT> ChannelsBuilder<? extends Channel<?, ? extends Selectable<OUT>>>
    selectableOutput(
            @NotNull final Channel<?, ? extends OUT> channel, final int index) {
        return new SelectableOutputBuilder<OUT>(channel, index);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final Channel<?, ?>... channels) {
        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (DistributeBuilder<IN>) new DistributeBuilder<Object>(isFlush, placeholder,
                Arrays.asList(channels));
    }

    @NotNull
    private static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            final boolean isFlush, @Nullable final IN placeholder,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return new DistributeBuilder<IN>(isFlush, placeholder, channels);
    }

    @NotNull
    private static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new JoinBuilder<OUT>(isFlush, placeholder, channels);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <OUT> ChannelsBuilder<? extends Channel<?, List<? extends OUT>>> join(
            final boolean isFlush, @Nullable final OUT placeholder,
            @NotNull final Channel<?, ?>... channels) {
        final int length = channels.length;
        if (length == 0) {
            throw new IllegalArgumentException("the array of channels must not be empty");
        }

        return (JoinBuilder<OUT>) new JoinBuilder<Object>(isFlush, placeholder,
                Arrays.asList(channels));
    }
}
