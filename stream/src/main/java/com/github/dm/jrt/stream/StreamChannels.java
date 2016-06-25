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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionWrapper;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.annotation.StreamFlow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.operator.math.Numbers.toBigDecimalSafe;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.MAP;

/**
 * Utility class acting as a factory of stream channels.
 * <p>
 * Created by davide-maestroni on 11/26/2015.
 */
public class StreamChannels extends Operators {

    /**
     * Avoid explicit instantiation.
     */
    protected StreamChannels() {
        ConstantConditions.avoid();
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream channels,
     * provided by the specified function, to process input data.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> asFactory(
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends StreamChannel<?
                    super IN, ? extends OUT>> function) {
        return new StreamInvocationFactory<IN, OUT>(wrap(function));
    }

    /**
     * Returns a builder of streams blending the outputs coming from the specified channels.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#blend(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> blend(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<OUT>(Channels.blend(channels));
    }

    /**
     * Returns a builder of streams blending the outputs coming from the specified channels.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#blend(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> blend(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<OUT>(Channels.<OUT>blend(channels));
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the array.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the array of channels.
     * @param <IN>     the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a
     *                                            null object.
     * @see Channels#combine(Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Channel<?, ?>... channels) {
        return Channels.combine(channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <IN>       the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#combine(int, Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex, @NotNull final Channel<?, ?>... channels) {
        return Channels.combine(startIndex, channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param startIndex the selectable start index.
     * @param channels   the iterable of channels.
     * @param <IN>       the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#combine(int, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return Channels.combine(startIndex, channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the position in the iterable.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the iterable of channels.
     * @param <IN>     the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#combine(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return Channels.combine(channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the map of indexes and channels.
     * @param <IN>     the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     * @see Channels#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
        return Channels.combine(channels);
    }

    /**
     * Returns a builder of stream channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#concat(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> concat(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<OUT>(Channels.concat(channels));
    }

    /**
     * Returns a builder of stream channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#concat(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> concat(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<OUT>(Channels.<OUT>concat(channels));
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the array of channels.
     * @param <IN>     the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#distribute(Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Channel<?, ?>... channels) {
        return Channels.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the iterable of channels.
     * @param <IN>     the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#distribute(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return Channels.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data is smaller than the specified number of channels, the remaining ones will be fed
     * with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <IN>        the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#distribute(Object, Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder, @NotNull final Channel<?, ?>... channels) {
        return Channels.distribute(placeholder, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified ones. If the
     * list of data is smaller than the specified number of channels, the remaining ones will be fed
     * with the specified placeholder instance. While, if the list of data exceeds the number of
     * channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the iterable of channels.
     * @param <IN>        the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#distribute(Object, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return Channels.distribute(placeholder, channels);
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the iterable of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#join(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<List<? extends OUT>>(Channels.join(channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#join(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(@NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<List<? extends OUT>>(Channels.<OUT>join(channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the channels are closed, the remaining outputs will be returned by filling
     * the gaps with the specified placeholder instance, so that the generated list of data will
     * always have the same size of the channel list.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the iterable of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#join(Object, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(@Nullable final OUT placeholder,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<List<? extends OUT>>(Channels.join(placeholder, channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the channels are closed, the remaining outputs will be returned by filling
     * the gaps with the specified placeholder instance, so that the generated list of data will
     * always have the same size of the channel list.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#join(Object, Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(@Nullable final OUT placeholder,
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<List<? extends OUT>>(Channels.join(placeholder, channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param startIndex the selectable start index.
     * @param channels   the iterable of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#merge(int, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(final int startIndex,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(startIndex, channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(int, Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(final int startIndex,
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.<OUT>merge(startIndex, channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#merge(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the map of indexes and channels.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     * @see Channels#merge(Map)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(
            @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(@NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.<OUT>merge(channels));
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The generated data will start from the specified first one up to and including the specified
     * last one, by computing each next element through the specified function.
     *
     * @param start             the first element of the range.
     * @param end               the last element of the range.
     * @param incrementFunction the function incrementing the current element.
     * @param <AFTER>           the concatenation output type.
     * @return the consumer instance.
     */
    @NotNull
    @StreamFlow(MAP)
    public static <AFTER extends Comparable<? super AFTER>> Consumer<Channel<AFTER, ?>> range(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> incrementFunction) {
        return new RangeConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.notNull("end element", end), wrap(incrementFunction));
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The stream will generate a range of numbers up to and including the {@code end} element, by
     * applying a default increment of {@code +1} or {@code -1} depending on the comparison between
     * the first and the last element. That is, if the first element is less than the last, the
     * increment will be {@code +1}. On the contrary, if the former is greater than the latter, the
     * increment will be {@code -1}.
     *
     * @param start the first element of the range.
     * @param end   the last element of the range.
     * @param <N>   the number type.
     * @return the consumer instance.
     */
    @NotNull
    @StreamFlow(MAP)
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
            @NotNull final N end) {
        return (Consumer<Channel<N, ?>>) numberRange(start, end);
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The stream will generate a range of numbers by applying the specified increment up to and
     * including the {@code end} element.
     *
     * @param start     the first element of the range.
     * @param end       the last element of the range.
     * @param increment the increment to apply to the current element.
     * @param <N>       the number type.
     * @return the consumer instance.
     */
    @NotNull
    @StreamFlow(MAP)
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<Channel<N, ?>> range(@NotNull final N start,
            @NotNull final N end, @NotNull final N increment) {
        return (Consumer<Channel<N, ?>>) numberRange(start, end, increment);
    }

    /**
     * Returns a builder of streams repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting binding of several channel consumers.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channel will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channel channel instance.
     * @param <OUT>   the output data type.
     * @return the replaying stream channel builder.
     * @see Channels#replay(Channel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> replay(
            @NotNull final Channel<?, OUT> channel) {
        return new BuilderWrapper<OUT>(Channels.replay(channel));
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
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Channel<IN, ?>> selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel, final int index) {
        return Channels.selectInput(channel, index);
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
     * @see Channels#selectInput(Channel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
            @NotNull final Iterable<Integer> indexes) {
        return Channels.selectInput(channel, indexes);
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
     * @see Channels#selectInput(Channel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
            @NotNull final int... indexes) {
        return Channels.selectInput(channel, indexes);
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
     * @see Channels#selectInput(int, int, Channel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<? super Selectable<DATA>, ?> channel) {
        return Channels.selectInput(startIndex, rangeSize, channel);
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
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
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     * @see Channels#selectOutput(int, int, Channel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>>
    selectOutput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel) {
        return new MapBuilderWrapper<OUT>(Channels.selectOutput(startIndex, rangeSize, channel));
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
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
     * @see Channels#selectOutput(Channel, int...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>>
    selectOutput(
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel,
            @NotNull final int... indexes) {
        return new MapBuilderWrapper<OUT>(Channels.selectOutput(channel, indexes));
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
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
     * @see Channels#selectOutput(Channel, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>>
    selectOutput(
            @NotNull final Channel<?, ? extends Selectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {
        return new MapBuilderWrapper<OUT>(Channels.selectOutput(channel, indexes));
    }

    /**
     * Returns a builder of selectable channels feeding the specified one.
     * <br>
     * Each output will be filtered based on the specified index.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <IN>    the input data type.
     * @return the selectable channel builder.
     * @see Channels#selectableInput(Channel, int)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<IN>, ?>> selectableInput(
            @NotNull final Channel<? super IN, ?> channel, final int index) {
        return Channels.selectableInput(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channel will be bound as a result of the creation.
     * <br>
     * Note also that the returned channel will be already closed.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable stream builder.
     * @see Channels#selectableOutput(Channel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> selectableOutput(
            @NotNull final Channel<?, ? extends OUT> channel, final int index) {
        return new BuilderWrapper<Selectable<OUT>>(Channels.selectableOutput(channel, index));
    }

    /**
     * Returns a consumer generating the specified sequence of data.
     * <br>
     * The generated data will start from the specified first and will produce the specified number
     * of elements, by computing each next one through the specified function.
     *
     * @param start        the first element of the sequence.
     * @param count        the number of generated elements.
     * @param nextFunction the function computing the next element.
     * @param <AFTER>      the concatenation output type.
     * @return the consumer instance.
     * @throws java.lang.IllegalArgumentException if the count is not positive.
     */
    @NotNull
    @StreamFlow(MAP)
    public static <AFTER> Consumer<Channel<AFTER, ?>> sequence(@NotNull final AFTER start,
            final long count, @NotNull final BiFunction<AFTER, Long, AFTER> nextFunction) {
        return new SequenceConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.positive("sequence size", count), wrap(nextFunction));
    }

    /**
     * Builds and returns a new stream channel.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf() {
        return streamOf(JRoutineCore.io().<OUT>of());
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf(@Nullable final Iterable<OUT> outputs) {
        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new stream channel generating the specified output.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf(@Nullable final OUT output) {
        return streamOf(JRoutineCore.io().of(output));
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf(@Nullable final OUT... outputs) {
        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     * <br>
     * The specified channel will be bound as a result of the call.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param output the channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf(@Nullable final Channel<?, OUT> output) {
        final Channel<OUT, OUT> outputChannel = JRoutineCore.io().buildChannel();
        return new DefaultStreamChannel<OUT, OUT>(outputChannel.pass(output).close());
    }

    /**
     * Returns a routine builder, whose invocation instances employ the streams provided by the
     * specified function to process input data.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the routine builder.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> withStream(
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends StreamChannel<?
                    super IN, ? extends OUT>> function) {
        return JRoutineCore.with(asFactory(function));
    }

    @NotNull
    private static <N extends Number> Consumer<? extends Channel<? extends Number, ?>> numberRange(
            @NotNull final N start, @NotNull final N end) {
        if ((start instanceof BigDecimal) || (end instanceof BigDecimal)) {
            final BigDecimal startValue = toBigDecimalSafe(start);
            final BigDecimal endValue = toBigDecimalSafe(end);
            return numberRange(startValue, endValue,
                    (startValue.compareTo(endValue) <= 0) ? 1 : -1);

        } else if ((start instanceof BigInteger) || (end instanceof BigInteger)) {
            final BigDecimal startDecimal = toBigDecimalSafe(start);
            final BigDecimal endDecimal = toBigDecimalSafe(end);
            if ((startDecimal.scale() > 0) || (endDecimal.scale() > 0)) {
                return numberRange(startDecimal, endDecimal,
                        (startDecimal.compareTo(endDecimal) <= 0) ? 1 : -1);
            }

            final BigInteger startValue = startDecimal.toBigInteger();
            final BigInteger endValue = endDecimal.toBigInteger();
            return numberRange(startValue, endValue,
                    (startValue.compareTo(endValue) <= 0) ? 1 : -1);

        } else if ((start instanceof Double) || (end instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Float) || (end instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Long) || (end instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Integer) || (end instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            return numberRange(start, end, (startValue <= endValue) ? 1 : -1);

        } else if ((start instanceof Short) || (end instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            return numberRange(start, end, (short) ((startValue <= endValue) ? 1 : -1));

        } else if ((start instanceof Byte) || (end instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            return numberRange(start, end, (byte) ((startValue <= endValue) ? 1 : -1));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + "]");
    }

    @NotNull
    private static <N extends Number> Consumer<? extends Channel<? extends Number, ?>> numberRange(
            @NotNull final N start, @NotNull final N end, @NotNull final N increment) {
        if ((start instanceof BigDecimal) || (end instanceof BigDecimal)
                || (increment instanceof BigDecimal)) {
            final BigDecimal startValue = toBigDecimalSafe(start);
            final BigDecimal endValue = toBigDecimalSafe(end);
            final BigDecimal incValue = toBigDecimalSafe(increment);
            return new RangeConsumer<BigDecimal>(startValue, endValue, new BigDecimalInc(incValue));

        } else if ((start instanceof BigInteger) || (end instanceof BigInteger)
                || (increment instanceof BigInteger)) {
            final BigDecimal startDecimal = toBigDecimalSafe(start);
            final BigDecimal endDecimal = toBigDecimalSafe(end);
            final BigDecimal incDecimal = toBigDecimalSafe(increment);
            if ((startDecimal.scale() > 0) || (endDecimal.scale() > 0) || (incDecimal.scale()
                    > 0)) {
                return new RangeConsumer<BigDecimal>(startDecimal, endDecimal,
                        new BigDecimalInc(incDecimal));
            }

            final BigInteger startValue = startDecimal.toBigInteger();
            final BigInteger endValue = endDecimal.toBigInteger();
            final BigInteger incValue = incDecimal.toBigInteger();
            return new RangeConsumer<BigInteger>(startValue, endValue, new BigIntegerInc(incValue));

        } else if ((start instanceof Double) || (end instanceof Double)
                || (increment instanceof Double)) {
            final double startValue = start.doubleValue();
            final double endValue = end.doubleValue();
            final double incValue = increment.doubleValue();
            return new RangeConsumer<Double>(startValue, endValue, new DoubleInc(incValue));

        } else if ((start instanceof Float) || (end instanceof Float)
                || (increment instanceof Float)) {
            final float startValue = start.floatValue();
            final float endValue = end.floatValue();
            final float incValue = increment.floatValue();
            return new RangeConsumer<Float>(startValue, endValue, new FloatInc(incValue));

        } else if ((start instanceof Long) || (end instanceof Long)
                || (increment instanceof Long)) {
            final long startValue = start.longValue();
            final long endValue = end.longValue();
            final long incValue = increment.longValue();
            return new RangeConsumer<Long>(startValue, endValue, new LongInc(incValue));

        } else if ((start instanceof Integer) || (end instanceof Integer)
                || (increment instanceof Integer)) {
            final int startValue = start.intValue();
            final int endValue = end.intValue();
            final int incValue = increment.intValue();
            return new RangeConsumer<Integer>(startValue, endValue, new IntegerInc(incValue));

        } else if ((start instanceof Short) || (end instanceof Short)
                || (increment instanceof Short)) {
            final short startValue = start.shortValue();
            final short endValue = end.shortValue();
            final short incValue = increment.shortValue();
            return new RangeConsumer<Short>(startValue, endValue, new ShortInc(incValue));

        } else if ((start instanceof Byte) || (end instanceof Byte)
                || (increment instanceof Byte)) {
            final byte startValue = start.byteValue();
            final byte endValue = end.byteValue();
            final byte incValue = increment.byteValue();
            return new RangeConsumer<Byte>(startValue, endValue, new ByteInc(incValue));
        }

        throw new IllegalArgumentException(
                "unsupported Number class: [" + start.getClass().getCanonicalName() + ", "
                        + end.getClass().getCanonicalName() + ", " + increment.getClass()
                                                                              .getCanonicalName()
                        + "]");
    }

    /**
     * Function incrementing a big decimal of a specific value.
     */
    private static class BigDecimalInc extends NumberInc<BigDecimal> {

        private final BigDecimal mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private BigDecimalInc(final BigDecimal incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public BigDecimal apply(final BigDecimal bigDecimal) {
            return bigDecimal.add(mIncValue);
        }
    }

    /**
     * Function incrementing a big integer of a specific value.
     */
    private static class BigIntegerInc extends NumberInc<BigInteger> {

        private final BigInteger mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private BigIntegerInc(final BigInteger incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public BigInteger apply(final BigInteger bigInteger) {
            return bigInteger.add(mIncValue);
        }
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ByteInc extends NumberInc<Byte> {

        private final byte mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ByteInc(final byte incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Byte apply(final Byte aByte) {
            return (byte) (aByte + mIncValue);
        }
    }

    /**
     * Function incrementing a double of a specific value.
     */
    private static class DoubleInc extends NumberInc<Double> {

        private final double mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private DoubleInc(final double incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Double apply(final Double aDouble) {
            return aDouble + mIncValue;
        }
    }

    /**
     * Function incrementing a float of a specific value.
     */
    private static class FloatInc extends NumberInc<Float> {

        private final float mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private FloatInc(final float incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Float apply(final Float aFloat) {
            return aFloat + mIncValue;
        }
    }

    /**
     * Function incrementing an integer of a specific value.
     */
    private static class IntegerInc extends NumberInc<Integer> {

        private final int mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private IntegerInc(final int incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Integer apply(final Integer integer) {
            return integer + mIncValue;
        }
    }

    /**
     * Function incrementing a long of a specific value.
     */
    private static class LongInc extends NumberInc<Long> {

        private final long mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private LongInc(final long incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Long apply(final Long aLong) {
            return aLong + mIncValue;
        }
    }

    /**
     * Base abstract function incrementing a number of a specific value.
     * <br>
     * It provides an implementation for {@code equals()} and {@code hashCode()} methods.
     */
    private static abstract class NumberInc<N extends Number> extends DeepEqualObject
            implements Function<N, N> {

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private NumberInc(@NotNull final N incValue) {
            super(asArgs(incValue));
        }
    }

    /**
     * Consumer implementation generating a range of data.
     *
     * @param <OUT> the output data type.
     */
    private static class RangeConsumer<OUT extends Comparable<? super OUT>> extends DeepEqualObject
            implements Consumer<Channel<OUT, ?>> {

        private final OUT mEnd;

        private final Function<OUT, OUT> mIncrementFunction;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start             the first element of the range.
         * @param end               the last element of the range.
         * @param incrementFunction the function incrementing the current element.
         */
        private RangeConsumer(@NotNull final OUT start, @NotNull final OUT end,
                @NotNull final Function<OUT, OUT> incrementFunction) {
            super(asArgs(start, end, incrementFunction));
            mStart = start;
            mEnd = end;
            mIncrementFunction = incrementFunction;
        }

        public void accept(final Channel<OUT, ?> result) throws Exception {
            final OUT start = mStart;
            final OUT end = mEnd;
            final Function<OUT, OUT> increment = mIncrementFunction;
            OUT current = start;
            if (start.compareTo(end) <= 0) {
                while (current.compareTo(end) <= 0) {
                    result.pass(current);
                    current = increment.apply(current);
                }

            } else {
                while (current.compareTo(end) >= 0) {
                    result.pass(current);
                    current = increment.apply(current);
                }
            }
        }
    }

    /**
     * Consumer implementation generating a sequence of data.
     *
     * @param <OUT> the output data type.
     */
    private static class SequenceConsumer<OUT> extends DeepEqualObject
            implements Consumer<Channel<OUT, ?>> {

        private final long mCount;

        private final BiFunctionWrapper<OUT, Long, OUT> mNextFunction;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start        the first element of the sequence.
         * @param count        the size of the sequence.
         * @param nextFunction the function computing the next element.
         */
        private SequenceConsumer(@NotNull final OUT start, final long count,
                @NotNull final BiFunctionWrapper<OUT, Long, OUT> nextFunction) {
            super(asArgs(start, count, nextFunction));
            mStart = start;
            mCount = count;
            mNextFunction = nextFunction;
        }

        public void accept(final Channel<OUT, ?> result) throws Exception {
            final BiFunctionWrapper<OUT, Long, OUT> next = mNextFunction;
            OUT current = mStart;
            final long count = mCount;
            final long last = count - 1;
            for (long i = 0; i < count; ++i) {
                result.pass(current);
                if (i < last) {
                    current = next.apply(current, i);
                }
            }
        }
    }

    /**
     * Function incrementing a short of a specific value.
     */
    private static class ShortInc extends NumberInc<Short> {

        private final short mIncValue;

        /**
         * Constructor.
         *
         * @param incValue the incrementation value.
         */
        private ShortInc(final short incValue) {
            super(incValue);
            mIncValue = incValue;
        }

        public Short apply(final Short aShort) {
            return (short) (aShort + mIncValue);
        }
    }
}
