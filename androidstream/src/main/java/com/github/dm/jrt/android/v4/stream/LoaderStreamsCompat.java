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

package com.github.dm.jrt.android.v4.stream;

import android.support.v4.util.SparseArrayCompat;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v4.channel.SparseChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.channel.ChannelsBuilder;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;

/**
 * Utility class acting as a factory of stream output channels.
 * <p>
 * Created by davide-maestroni on 01/04/2016.
 */
public class LoaderStreamsCompat extends Streams {

    /**
     * Avoid explicit instantiation.
     */
    protected LoaderStreamsCompat() {

    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#blend(Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT>> blend(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(SparseChannelsCompat.blend(channels));
    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.
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
     * @see SparseChannelsCompat#blend(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT>> blend(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(SparseChannelsCompat.<OUT>blend(channels));
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the array.
     *
     * @param channels the array of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#combine(Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final InputChannel<?>... channels) {

        return SparseChannelsCompat.combine(channels);
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
     * @see SparseChannelsCompat#combine(int, Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex, @NotNull final InputChannel<?>... channels) {

        return SparseChannelsCompat.combine(startIndex, channels);
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
     * @param channels   the collection of input channels.
     * @param <IN>       the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#combine(int, Collection)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            final int startIndex,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.combine(startIndex, channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the position in the collection.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the collection of input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#combine(Collection)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.combine(channels);
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
     * @see SparseChannelsCompat#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final Map<Integer, ? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.combine(channels);
    }

    /**
     * Returns a builder of input channels combining the specified channels into a selectable one.
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create only one input channel instance, and that the
     * returned channel <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the map of indexes and input channels.
     * @param <IN>     the input data type.
     * @return the selectable I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     * @see SparseChannelsCompat#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<? extends IN>>> combine(
            @NotNull final SparseArrayCompat<? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.combine(channels);
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#concat(Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT>> concat(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(SparseChannelsCompat.concat(channels));
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.
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
     * @see SparseChannelsCompat#concat(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT>> concat(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(SparseChannelsCompat.<OUT>concat(channels));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream output channels,
     * provided by the specified function, to process input data.
     * <br>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream output channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> contextFactory(
            @NotNull final Function<? super StreamChannel<IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        return factoryFrom(onStream(function), wrap(function).hashCode(), InvocationMode.SYNC);
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
     * @see SparseChannelsCompat#distribute(Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final InputChannel<?>... channels) {

        return SparseChannelsCompat.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several input channel instances, and that the
     * returned channels <b>must be explicitly closed</b> in order to ensure the completion of the
     * invocation lifecycle.
     *
     * @param channels the collection of channels.
     * @param <IN>     the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#distribute(Collection)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.distribute(channels);
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
     * @see SparseChannelsCompat#distribute(Object, Channel.InputChannel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder, @NotNull final InputChannel<?>... channels) {

        return SparseChannelsCompat.distribute(placeholder, channels);
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
     * @param channels    the collection of channels.
     * @param <IN>        the input data type.
     * @return the I/O channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#distribute(Object, Collection)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<List<? extends IN>>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Collection<? extends InputChannel<? extends IN>> channels) {

        return SparseChannelsCompat.distribute(placeholder, channels);
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
     * <br>
     * An output will be generated only when at least one result is available for each channel.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#join(Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>>>
    join(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(SparseChannelsCompat.join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>>>
    join(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(SparseChannelsCompat.<OUT>join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @param channels    the collection of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#join(Object, Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>>>
    join(
            @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(
                SparseChannelsCompat.join(placeholder, channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Object, Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>>>
    join(
            @Nullable final OUT placeholder, @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(
                SparseChannelsCompat.join(placeholder, channels));
    }

    /**
     * Builds and returns a new lazy loader stream channel.
     * <br>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer, or when any of the read methods is invoked.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf() {

        return lazyStreamOf(JRoutineCore.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.
     * <br>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer, or when any of the read methods is invoked.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @Nullable final Iterable<OUT> outputs) {

        return lazyStreamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified output.
     * <br>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer, or when any of the read methods is invoked.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(@Nullable final OUT output) {

        return lazyStreamOf(JRoutineCore.io().of(output));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.
     * <br>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer, or when any of the read methods is invoked.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @Nullable final OUT... outputs) {

        return lazyStreamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.
     * <br>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer, or when any of the read methods is invoked.
     * <p>
     * Note that the output channel will be bound as a result of the call.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @NotNull final OutputChannel<OUT> output) {

        ConstantConditions.notNull("output channel", output);
        final IOChannel<OUT> ioChannel = JRoutineCore.io().buildChannel();
        return new DefaultLoaderStreamChannelCompat<OUT>(null, output, ioChannel);
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the collection of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#merge(int, Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(int, Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.<OUT>merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
     * <p>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @throws java.lang.NullPointerException     if the specified collection is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#merge(Collection)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(SparseChannelsCompat.merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(Channel.OutputChannel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(@NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.<OUT>merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(SparseArrayCompat)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final SparseArrayCompat<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(SparseChannelsCompat.merge(channels));
    }

    /**
     * Returns a routine builder, whose invocation instances employ the streams provided by the
     * specified function, to process input data.
     * <br>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream output channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the routine builder.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> onStream(
            @NotNull final Function<? super StreamChannel<IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        if (!wrap(function).hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }

        return Streams.onStream(function);
    }

    /**
     * Returns a loader routine builder, whose invocation instances employ the streams provided by
     * the specified function, to process input data.
     * <br>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param context  the loader context.
     * @param function the function providing the stream output channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the loader routine builder.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> LoaderRoutineBuilder<IN, OUT> onStreamWith(
            @NotNull final LoaderContextCompat context,
            @NotNull final Function<? super StreamChannel<IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        return JRoutineLoaderCompat.with(context).on(contextFactory(function));
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
     * @return the repeating stream channel builder.
     * @see SparseChannelsCompat#repeat(Channel.OutputChannel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT>> repeat(
            @NotNull final OutputChannel<OUT> channel) {

        return new BuilderWrapper<OUT>(SparseChannelsCompat.repeat(channel));
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
     * @see SparseChannelsCompat#select(Channel.InputChannel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends IOChannel<IN>> select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel, final int index) {

        return SparseChannelsCompat.select(channel, index);
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
     * @see SparseChannelsCompat#select(Channel.InputChannel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return SparseChannelsCompat.select(channel, indexes);
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
     * @see SparseChannelsCompat#select(Channel.InputChannel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            @NotNull final InputChannel<? super Selectable<DATA>> channel,
            @NotNull final int... indexes) {

        return SparseChannelsCompat.select(channel, indexes);
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
     * @see SparseChannelsCompat#select(int, int, Channel.InputChannel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, IOChannel<IN>>>
    select(
            final int startIndex, final int rangeSize,
            @NotNull final InputChannel<? super Selectable<DATA>> channel) {

        return SparseChannelsCompat.select(startIndex, rangeSize, channel);
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
     * @see SparseChannelsCompat#selectParcelable(Channel.InputChannel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends IOChannel<IN>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            final int index) {

        return SparseChannelsCompat.selectParcelable(channel, index);
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
     * @see SparseChannelsCompat#selectParcelable(Channel.InputChannel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<IOChannel<IN>>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final int... indexes) {

        return SparseChannelsCompat.selectParcelable(channel, indexes);
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
     * @see SparseChannelsCompat#selectParcelable(Channel.InputChannel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<IOChannel<IN>>> selectParcelable(
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return SparseChannelsCompat.selectParcelable(channel, indexes);
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
     * @see SparseChannelsCompat#selectParcelable(int, int, Channel.InputChannel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<IOChannel<IN>>> selectParcelable(final int startIndex,
            final int rangeSize,
            @NotNull final InputChannel<? super ParcelableSelectable<DATA>> channel) {

        return SparseChannelsCompat.selectParcelable(startIndex, rangeSize, channel);
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#select(int, int, Channel.OutputChannel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT>>> selectParcelable(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel) {

        return new MapBuilderWrapper<OUT>(
                SparseChannelsCompat.selectParcelable(startIndex, rangeSize, channel));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#select(Channel.OutputChannel, int...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT>>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        return new MapBuilderWrapper<OUT>(SparseChannelsCompat.selectParcelable(channel, indexes));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#select(Channel.OutputChannel, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT>>> selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return new MapBuilderWrapper<OUT>(SparseChannelsCompat.selectParcelable(channel, indexes));
    }

    /**
     * Builds and returns a new loader stream channel.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf() {

        return streamOf(JRoutineCore.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf(
            @Nullable final Iterable<OUT> outputs) {

        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified output.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf(@Nullable final OUT output) {

        return streamOf(JRoutineCore.io().of(output));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf(@Nullable final OUT... outputs) {

        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     * <p>
     * Note that the output channel will be bound as a result of the call.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf(
            @NotNull final OutputChannel<OUT> output) {

        if (output instanceof LoaderStreamChannelCompat) {
            return (LoaderStreamChannelCompat<OUT>) output;
        }

        return new DefaultLoaderStreamChannelCompat<OUT>(null, output);
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
     * @see SparseChannelsCompat#toSelectable(Channel.InputChannel, int)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends IOChannel<Selectable<IN>>> toSelectable(
            @NotNull final InputChannel<? super IN> channel, final int index) {

        return SparseChannelsCompat.toSelectable(channel, index);
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
     * @return the selectable loader stream builder.
     * @see SparseChannelsCompat#toSelectable(Channel.OutputChannel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.toSelectable(channel, index));
    }
}
