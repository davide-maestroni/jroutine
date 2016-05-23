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
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionWrapper;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.stream.annotation.StreamFlow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.stream.annotation.StreamFlow.BindingType.ROUTINE;
import static com.github.dm.jrt.stream.annotation.StreamFlow.ModificationType.CACHE;
import static com.github.dm.jrt.stream.annotation.StreamFlow.ModificationType.COLLECT;
import static com.github.dm.jrt.stream.annotation.StreamFlow.ModificationType.MAP;
import static com.github.dm.jrt.stream.annotation.StreamFlow.ModificationType.REDUCE;
import static com.github.dm.jrt.stream.util.Numbers.toBigDecimalSafe;

/**
 * Utility class acting as a factory of stream output channels.
 * <p>
 * Created by davide-maestroni on 11/26/2015.
 */
public class Streams extends Functions {

    private static final MappingInvocation<? extends Iterable<?>, ?> sUnfoldInvocation =
            new MappingInvocation<Iterable<?>, Object>(null) {

                @SuppressWarnings("unchecked")
                public void onInput(final Iterable<?> input,
                        @NotNull final ResultChannel<Object> result) {

                    result.pass((Iterable) input);
                }
            };

    /**
     * Avoid explicit instantiation.
     */
    protected Streams() {

        ConstantConditions.avoid();
    }

    /**
     * Returns a factory of invocations verifying that all the inputs satisfy a specific conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    public static <IN> InvocationFactory<IN, Boolean> allMatch(
            @NotNull final Predicate<? super IN> predicate) {

        return new AllMatchInvocationFactory<IN>(wrap(predicate));
    }

    /**
     * Returns a factory of invocations verifying that any of the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <IN> InvocationFactory<IN, Boolean> anyMatch(
            @NotNull final Predicate<? super IN> predicate) {

        return new AnyMatchInvocationFactory<IN>(wrap(predicate));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream output channels,
     * provided by the specified function, to process input data.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream output channels.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(Channels.blend(channels));
    }

    /**
     * Returns a builder of streams blending the outputs coming from the specified channels.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#blend(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> blend(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(Channels.<OUT>blend(channels));
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.
     *
     * @param channels the array of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a
     *                                            null object.
     * @see Channels#combine(Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final InputChannel<?>... channels) {

        return Channels.combine(channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#combine(int, Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex, @NotNull final InputChannel<?>... channels) {

        return Channels.combine(startIndex, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will start from the specified one.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param startIndex the selectable start index.
     * @param channels   the iterable of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#combine(int, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex,
            @NotNull final Iterable<? extends InputChannel<? extends IN>> channels) {

        return Channels.combine(startIndex, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the iterable.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the iterable of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#combine(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Iterable<? extends InputChannel<? extends IN>> channels) {

        return Channels.combine(channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     * @see Channels#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        return Channels.combine(channels);
    }

    /**
     * Returns a builder of stream channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(Channels.concat(channels));
    }

    /**
     * Returns a builder of stream channels concatenating the outputs coming from the specified
     * ones, so that, all the outputs of the first channel will come before all the outputs of the
     * second one, and so on.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#concat(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> concat(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(Channels.<OUT>concat(channels));
    }

    /**
     * Returns an factory of invocations counting the number of input data.
     *
     * @param <DATA> the data type.
     * @return the invocation factory.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <DATA> InvocationFactory<DATA, Long> count() {

        return (InvocationFactory<DATA, Long>) CountInvocation.factoryOf();
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the array of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#distribute(Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final InputChannel<?>... channels) {

        return Channels.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the iterable of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#distribute(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final Iterable<? extends InputChannel<? extends IN>> channels) {

        return Channels.distribute(channels);
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
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#distribute(Object, Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder, @NotNull final InputChannel<?>... channels) {

        return Channels.distribute(placeholder, channels);
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
     *
     * @param placeholder the placeholder instance.
     * @param channels    the iterable of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see Channels#distribute(Object, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Iterable<? extends InputChannel<? extends IN>> channels) {

        return Channels.distribute(placeholder, channels);
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [(0, 1, 2), (3, 4, 5), ..., (N, N + 1)]
     *     </code>
     * </pre>
     *
     * @param size   the group size.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size) {

        return new GroupByInvocationFactory<DATA>(size);
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.
     * <br>
     * If the inputs complete and the last group length is less than the target size, the missing
     * spaces will be filled with the specified placeholder instance.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a size of 3, the final output will
     * be:
     * <pre>
     *     <code>
     *
     *         [(0, 1, 2), (3, 4, 5), ..., (N, N + 1, PH)]
     *     </code>
     * </pre>
     *
     * @param size        the group size.
     * @param placeholder the placeholder object used to fill the missing data needed to reach
     *                    the group size.
     * @param <DATA>      the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size,
            @Nullable final DATA placeholder) {

        return new GroupByInvocationFactory<DATA>(size, placeholder);
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(Channels.join(channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#join(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(@NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(Channels.<OUT>join(channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(Channels.join(placeholder, channels));
    }

    /**
     * Returns a builder of stream channels joining the data coming from the specified ones.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#join(Object, Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<List<? extends OUT>, List<?
            extends OUT>>> join(@Nullable final OUT placeholder,
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(Channels.join(placeholder, channels));
    }

    /**
     * Returns an factory of invocations passing at max the specified number of input data and
     * discarding the following ones.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a limit count of 5, the final output
     * will be:
     * <pre>
     *     <code>
     *
     *         [0, 1, 2, 3, 4]
     *     </code>
     * </pre>
     *
     * @param count  the maximum number of data to pass.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, DATA> limit(final int count) {

        return new LimitInvocationFactory<DATA>(count);
    }

    /**
     * Returns a factory of invocations computing the mean value of the input numbers.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> mean() {

        return (InvocationFactory<N, Number>) MeanInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the mean value of the input numbers by employing a
     * {@code BigDecimal}.
     *
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    public static InvocationFactory<Number, BigDecimal> meanBig() {

        return MeanBigInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the mean value of the input numbers in floating
     * precision.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> meanFloating() {

        return (InvocationFactory<N, Number>) MeanFloatingInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the mean of the input numbers rounded to nearest
     * number.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> meanRounded() {

        return (InvocationFactory<N, Number>) MeanRoundedInvocation.factoryOf();
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(startIndex, channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(int, Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.<OUT>merge(startIndex, channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
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
            @NotNull final Iterable<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the map of indexes and output channels.
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
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.merge(channels));
    }

    /**
     * Returns a builder merging the specified channels into a selectable stream.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see Channels#merge(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> merge(@NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.<OUT>merge(channels));
    }

    /**
     * Returns a factory of invocations verifying that none of the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    public static <IN> InvocationFactory<IN, Boolean> noneMatch(
            @NotNull final Predicate<? super IN> predicate) {

        return new AllMatchInvocationFactory<IN>(wrap(predicate).negate());
    }

    /**
     * Returns a factory of invocations verifying that not all the inputs satisfy a specific
     * conditions.
     *
     * @param predicate the predicate defining the condition.
     * @param <IN>      the input data type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <IN> InvocationFactory<IN, Boolean> notAllMatch(
            @NotNull final Predicate<? super IN> predicate) {

        return new AnyMatchInvocationFactory<IN>(wrap(predicate).negate());
    }

    /**
     * Returns a routine builder, whose invocation instances employ the streams provided by the
     * specified function to process input data.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream output channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the routine builder.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> onStream(
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends StreamChannel<?
                    super IN, ? extends OUT>> function) {

        return JRoutineCore.on(asFactory(function));
    }

    /**
     * Returns a consumer generating the specified range of data.
     * <br>
     * The generated data will start from the specified first one up to and including the specified
     * last one, by computing each next element through the specified function.
     *
     * @param start     the first element of the range.
     * @param end       the last element of the range.
     * @param increment the function incrementing the current element.
     * @param <AFTER>   the concatenation output type.
     * @return the consumer instance.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <AFTER extends Comparable<? super AFTER>> Consumer<InputChannel<AFTER>> range(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment) {

        return new RangeConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.notNull("end element", end), wrap(increment));
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
    @StreamFlow(value = MAP, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<InputChannel<N>> range(@NotNull final N start,
            @NotNull final N end) {

        return (Consumer<InputChannel<N>>) numberRange(start, end);
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
    @StreamFlow(value = MAP, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> Consumer<InputChannel<N>> range(@NotNull final N start,
            @NotNull final N end, @NotNull final N increment) {

        return (Consumer<InputChannel<N>>) numberRange(start, end, increment);
    }

    /**
     * Returns a builder of streams repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting binding of several output consumers.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channel the output channel.
     * @param <OUT>   the output data type.
     * @return the replaying stream channel builder.
     * @see Channels#replay(Channel.OutputChannel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<OUT, OUT>> replay(
            @NotNull final OutputChannel<OUT> channel) {

        return new BuilderWrapper<OUT>(Channels.replay(channel));
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the I/O channel builder.
     * @see Channels#select(Channel.InputChannel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel, final int index) {

        return Channels.select(channel, index);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.
     * <p>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels builder.
     * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
     *                                        object.
     * @see Channels#select(Channel.InputChannel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return Channels.select(channel, indexes);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.
     * <p>
     * Note that the builder will successfully create several input channel map instances, and that
     * the returned channels <b>must be explicitly closed</b> in order to ensure the completion of
     * the invocation lifecycle.
     *
     * @param channel the selectable channel.
     * @param indexes the array of indexes.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the map of indexes and I/O channels builder.
     * @throws java.lang.NullPointerException if the specified array is null or contains a null
     *                                        object.
     * @see Channels#select(Channel.InputChannel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final int... indexes) {

        return Channels.select(channel, indexes);
    }

    /**
     * Returns a builder of maps of input channels accepting the data identified by the specified
     * indexes.
     * <p>
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
     * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
     * @see Channels#select(int, int, Channel.InputChannel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        return Channels.select(startIndex, rangeSize, channel);
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <OUT>      the output data type.
     * @return the map of indexes and output channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     * @see Channels#select(int, int, Channel.OutputChannel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>> select(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel) {

        return new MapBuilderWrapper<OUT>(Channels.select(startIndex, rangeSize, channel));
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the list of indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @throws java.lang.NullPointerException if the specified array is null or contains a null
     *                                        object.
     * @see Channels#select(Channel.OutputChannel, int...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        return new MapBuilderWrapper<OUT>(Channels.select(channel, indexes));
    }

    /**
     * Returns a builder of maps of stream channels returning the output data filtered by the
     * specified indexes.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
     *                                        object.
     * @see Channels#select(Channel.OutputChannel, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends Map<Integer, StreamChannel<OUT, OUT>>> select(
            @NotNull final OutputChannel<? extends Selectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return new MapBuilderWrapper<OUT>(Channels.select(channel, indexes));
    }

    /**
     * Returns a consumer generating the specified sequence of data.
     * <br>
     * The generated data will start from the specified first and will produce the specified number
     * of elements, by computing each next one through the specified function.
     *
     * @param start   the first element of the sequence.
     * @param count   the number of generated elements.
     * @param next    the function computing the next element.
     * @param <AFTER> the concatenation output type.
     * @return the consumer instance.
     * @throws java.lang.IllegalArgumentException if the count is not positive.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <AFTER> Consumer<InputChannel<AFTER>> sequence(@NotNull final AFTER start,
            final long count, @NotNull final BiFunction<AFTER, Long, AFTER> next) {

        return new SequenceConsumer<AFTER>(ConstantConditions.notNull("start element", start),
                ConstantConditions.positive("sequence size", count), wrap(next));
    }

    /**
     * Returns an factory of invocations skipping the specified number of input data.
     * <p>
     * Given a numeric sequence of inputs starting from 0, and a skip count of 5, the final output
     * will be:
     * <pre>
     *     <code>
     *
     *         [5, 6, 7, ...]
     *     </code>
     * </pre>
     *
     * @param count  the number of data to skip.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, DATA> skip(final int count) {

        return new SkipInvocationFactory<DATA>(count);
    }

    /**
     * Returns an factory of invocations sorting inputs in their natural order.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @StreamFlow(value = COLLECT, binding = ROUTINE)
    public static <IN extends Comparable<? super IN>> InvocationFactory<IN, IN> sort() {

        return SortInvocation.factoryOf();
    }

    /**
     * Returns an factory of invocations sorting input data by the specified comparator.
     *
     * @param comparator the comparator instance.
     * @param <DATA>     the data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @StreamFlow(value = COLLECT, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, DATA> sortBy(
            @NotNull final Comparator<? super DATA> comparator) {

        return new SortByInvocationFactory<DATA>(comparator);
    }

    /**
     * Builds and returns a new stream channel.
     * <p>
     * Note that the stream will start producing results only when one of the {@link OutputChannel}
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
     * Note that the stream will start producing results only when one of the {@link OutputChannel}
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
     * Note that the stream will start producing results only when one of the {@link OutputChannel}
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
     * Note that the stream will start producing results only when one of the {@link OutputChannel}
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
     * The output channel will be bound as a result of the call.
     * <p>
     * Note that the stream will start producing results only when one of the {@link OutputChannel}
     * methods is called.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT, OUT> streamOf(@NotNull final OutputChannel<OUT> output) {

        return new DefaultStreamChannel<OUT, OUT>(output);
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers.
     * <br>
     * The result will have the type matching the input with the highest precision.
     *
     * @param <N> the number type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <N extends Number> InvocationFactory<N, Number> sum() {

        return (InvocationFactory<N, Number>) SumInvocation.factoryOf();
    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers by employing a
     * {@code BigDecimal}.
     *
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = REDUCE, binding = ROUTINE)
    public static InvocationFactory<Number, BigDecimal> sumBig() {

        return SumBigInvocation.factoryOf();
    }

    /**
     * Returns an factory of invocations collecting inputs into a list.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @StreamFlow(value = COLLECT, binding = ROUTINE)
    public static <IN> InvocationFactory<? super IN, List<IN>> toList() {

        return ToListInvocation.factoryOf();
    }

    /**
     * Returns an factory of invocations collecting inputs into a map.
     *
     * @param keyFunction the key function.
     * @param <IN>        the input data type.
     * @param <KEY>       the map key type.
     * @return the invocation factory instance.
     */
    @NotNull
    @StreamFlow(value = COLLECT, binding = ROUTINE)
    public static <IN, KEY> InvocationFactory<? super IN, Map<KEY, IN>> toMap(
            @NotNull final Function<? super IN, KEY> keyFunction) {

        return new ToMapInvocationFactory<IN, KEY>(wrap(keyFunction));
    }

    /**
     * Returns a builder of selectable channels feeding the specified one.
     * <br>
     * Each output will be filtered based on the specified index.
     * <p>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the returned channels <b>must be explicitly closed</b> in order to ensure the
     * completion of the invocation lifecycle.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <IN>    the input data type.
     * @return the selectable I/O channel builder.
     * @see Channels#toSelectable(Channel.InputChannel, int)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<IN>>> toSelectable(
            @NotNull final InputChannel<? super IN> channel, final int index) {

        return Channels.toSelectable(channel, index);
    }

    /**
     * Returns a builder of channels making the specified one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable stream builder.
     * @see Channels#toSelectable(Channel.OutputChannel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends StreamChannel<? extends Selectable<OUT>, ?
            extends Selectable<OUT>>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new BuilderWrapper<Selectable<OUT>>(Channels.toSelectable(channel, index));
    }

    /**
     * Returns an factory of invocations collecting inputs into a set.
     *
     * @param <IN> the input data type.
     * @return the invocation factory instance.
     */
    @NotNull
    @StreamFlow(value = COLLECT, binding = ROUTINE)
    public static <IN> InvocationFactory<? super IN, Set<IN>> toSet() {

        return ToSetInvocation.factoryOf();
    }

    /**
     * Returns a bi-consumer unfolding iterable inputs into the returned elements.
     *
     * @param <IN> the input data type.
     * @return the bi-consumer instance.
     */
    @NotNull
    @StreamFlow(value = MAP, binding = ROUTINE)
    @SuppressWarnings("unchecked")
    public static <IN> InvocationFactory<Iterable<? extends IN>, IN> unfold() {

        return (InvocationFactory<Iterable<? extends IN>, IN>) sUnfoldInvocation;
    }

    /**
     * Returns a factory of invocations filtering inputs which are not unique.
     *
     * @param <DATA> the data type.
     * @return the factory instance.
     */
    @NotNull
    @StreamFlow(value = CACHE, binding = ROUTINE)
    public static <DATA> InvocationFactory<DATA, DATA> unique() {

        return UniqueInvocation.factoryOf();
    }

    @NotNull
    private static <N extends Number> Consumer<? extends InputChannel<? extends Number>>
    numberRange(
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
    private static <N extends Number> Consumer<? extends InputChannel<? extends Number>>
    numberRange(
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
            implements Consumer<InputChannel<OUT>> {

        private final OUT mEnd;

        private final Function<OUT, OUT> mIncrement;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start     the first element of the range.
         * @param end       the last element of the range.
         * @param increment the function incrementing the current element.
         */
        private RangeConsumer(@NotNull final OUT start, @NotNull final OUT end,
                @NotNull final Function<OUT, OUT> increment) {

            super(asArgs(start, end, increment));
            mStart = start;
            mEnd = end;
            mIncrement = increment;
        }

        public void accept(final InputChannel<OUT> result) throws Exception {

            final OUT start = mStart;
            final OUT end = mEnd;
            final Function<OUT, OUT> increment = mIncrement;
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
            implements Consumer<InputChannel<OUT>> {

        private final long mCount;

        private final BiFunctionWrapper<OUT, Long, OUT> mNext;

        private final OUT mStart;

        /**
         * Constructor.
         *
         * @param start the first element of the sequence.
         * @param count the size of the sequence.
         * @param next  the function computing the next element.
         */
        private SequenceConsumer(@NotNull final OUT start, final long count,
                @NotNull final BiFunctionWrapper<OUT, Long, OUT> next) {

            super(asArgs(start, count, next));
            mStart = start;
            mCount = count;
            mNext = next;
        }

        public void accept(final InputChannel<OUT> result) throws Exception {

            final BiFunctionWrapper<OUT, Long, OUT> next = mNext;
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
