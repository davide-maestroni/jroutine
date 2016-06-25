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
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.wrap;

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
        ConstantConditions.avoid();
    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.
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
     * @see SparseChannelsCompat#blend(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT, OUT>> blend(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<OUT>(SparseChannelsCompat.blend(channels));
    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.
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
     * @see SparseChannelsCompat#blend(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT, OUT>> blend(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<OUT>(SparseChannelsCompat.<OUT>blend(channels));
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
     * @see SparseChannelsCompat#combine(Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Channel<?, ?>... channels) {
        return SparseChannelsCompat.combine(channels);
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
     * @see SparseChannelsCompat#combine(int, Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex, @NotNull final Channel<?, ?>... channels) {
        return SparseChannelsCompat.combine(startIndex, channels);
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
     * @see SparseChannelsCompat#combine(int, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            final int startIndex,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.combine(startIndex, channels);
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
     * @see SparseChannelsCompat#combine(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.combine(channels);
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
     * @see SparseChannelsCompat#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.combine(channels);
    }

    /**
     * Returns a builder of channels combining the specified instances into a selectable one.
     * <br>
     * The selectable indexes will be the keys of the specified map.
     * <p>
     * Note that the builder will successfully create only one channel instance.
     *
     * @param channels the map of indexes and channels.
     * @param <IN>     the input data type.
     * @return the selectable channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @throws java.lang.NullPointerException     if the specified map is null or contains a null
     *                                            object.
     * @see SparseChannelsCompat#combine(Map)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<? extends IN>, ?>> combine(
            @NotNull final SparseArrayCompat<? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.combine(channels);
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.
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
     * @see SparseChannelsCompat#concat(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT, OUT>> concat(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<OUT>(SparseChannelsCompat.concat(channels));
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.
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
     * @see SparseChannelsCompat#concat(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT, OUT>> concat(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<OUT>(SparseChannelsCompat.<OUT>concat(channels));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream channels,
     * provided by the specified function, to process input data.
     * <br>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.
     * <br>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream channel instances.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> contextFactory(
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends
                    StreamChannel<? super IN, ? extends OUT>> function) {
        return factoryFrom(withStream(function), wrap(function).hashCode(), InvocationMode.SYNC);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the array of channels.
     * @param <IN>     the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @throws java.lang.NullPointerException     if the specified array is null or contains a null
     *                                            object.
     * @see SparseChannelsCompat#distribute(Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Channel<?, ?>... channels) {
        return SparseChannelsCompat.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data exceeds the number of channels, the invocation will be aborted.
     * <p>
     * Note that the builder will successfully create several channel instances.
     *
     * @param channels the iterable of channels.
     * @param <IN>     the input data type.
     * @return the channel builder.
     * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
     * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
     *                                            null object.
     * @see SparseChannelsCompat#distribute(Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.distribute(channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
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
     * @see SparseChannelsCompat#distribute(Object, Channel...)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder, @NotNull final Channel<?, ?>... channels) {
        return SparseChannelsCompat.distribute(placeholder, channels);
    }

    /**
     * Returns a builder of channels distributing the input data among the specified channels. If
     * the list of data is smaller than the specified number of channels, the remaining ones will be
     * fed with the specified placeholder instance. While, if the list of data exceeds the number of
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
     * @see SparseChannelsCompat#distribute(Object, Iterable)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<List<? extends IN>, ?>> distribute(
            @Nullable final IN placeholder,
            @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
        return SparseChannelsCompat.distribute(placeholder, channels);
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>,
            List<? extends OUT>>> join(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<List<? extends OUT>>(SparseChannelsCompat.join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>,
            List<? extends OUT>>> join(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<List<? extends OUT>>(SparseChannelsCompat.<OUT>join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Object, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>,
            List<? extends OUT>>> join(
            @Nullable final OUT placeholder,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<List<? extends OUT>>(
                SparseChannelsCompat.join(placeholder, channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.
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
     * @see SparseChannelsCompat#join(Object, Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<List<? extends OUT>,
            List<? extends OUT>>> join(
            @Nullable final OUT placeholder, @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<List<? extends OUT>>(
                SparseChannelsCompat.join(placeholder, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(int, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> merge(
            final int startIndex,
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(int, Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> merge(
            final int startIndex, @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.<OUT>merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> merge(
            @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(SparseChannelsCompat.merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(Channel...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> merge(
            @NotNull final Channel<?, ?>... channels) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.<OUT>merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.
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
     * @see SparseChannelsCompat#merge(SparseArrayCompat)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> merge(
            @NotNull final SparseArrayCompat<? extends Channel<?, ? extends OUT>> channels) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(SparseChannelsCompat.merge(channels));
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
     * @param channel the channel instance.
     * @param <OUT>   the output data type.
     * @return the replaying stream channel builder.
     * @see SparseChannelsCompat#replay(Channel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<OUT, OUT>> replay(
            @NotNull final Channel<?, OUT> channel) {
        return new BuilderWrapper<OUT>(SparseChannelsCompat.replay(channel));
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
     * @see SparseChannelsCompat#selectInput(Channel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Channel<IN, ?>> selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel, final int index) {
        return SparseChannelsCompat.selectInput(channel, index);
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
     * @see SparseChannelsCompat#selectInput(Channel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
            @NotNull final Iterable<Integer> indexes) {
        return SparseChannelsCompat.selectInput(channel, indexes);
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
     * @see SparseChannelsCompat#selectInput(Channel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            @NotNull final Channel<? super Selectable<DATA>, ?> channel,
            @NotNull final int... indexes) {
        return SparseChannelsCompat.selectInput(channel, indexes);
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
     * @see SparseChannelsCompat#selectInput(int, int, Channel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Map<Integer, Channel<IN, ?>>>
    selectInput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<? super Selectable<DATA>, ?> channel) {
        return SparseChannelsCompat.selectInput(startIndex, rangeSize, channel);
    }

    /**
     * Returns a builder of channels transforming the input data into selectable ones.
     * <p>
     * Note that the builder will successfully create several channel instances
     *
     * @param channel the selectable channel.
     * @param index   the channel index.
     * @param <DATA>  the channel data type.
     * @param <IN>    the input data type.
     * @return the channel builder.
     * @see SparseChannelsCompat#selectParcelableInput(Channel, int)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends Channel<IN, ?>>
    selectParcelableInput(
            @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
            final int index) {
        return SparseChannelsCompat.selectParcelableInput(channel, index);
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
     * @see SparseChannelsCompat#selectParcelableInput(Channel, int...)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<Channel<IN, ?>>> selectParcelableInput(
            @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
            @NotNull final int... indexes) {
        return SparseChannelsCompat.selectParcelableInput(channel, indexes);
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
     * @see SparseChannelsCompat#selectParcelableInput(Channel, Iterable)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<Channel<IN, ?>>> selectParcelableInput(
            @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel,
            @NotNull final Iterable<Integer> indexes) {
        return SparseChannelsCompat.selectParcelableInput(channel, indexes);
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
     * @see SparseChannelsCompat#selectParcelableInput(int, int, Channel)
     */
    @NotNull
    public static <DATA, IN extends DATA> ChannelsBuilder<? extends
            SparseArrayCompat<Channel<IN, ?>>> selectParcelableInput(final int startIndex,
            final int rangeSize,
            @NotNull final Channel<? super ParcelableSelectable<DATA>, ?> channel) {
        return SparseChannelsCompat.selectParcelableInput(startIndex, rangeSize, channel);
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#selectParcelableOutput(int, int, Channel)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>> selectParcelableOutput(
            final int startIndex, final int rangeSize,
            @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel) {
        return new MapBuilderWrapper<OUT>(
                SparseChannelsCompat.selectParcelableOutput(startIndex, rangeSize, channel));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#selectParcelableOutput(Channel, int...)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>> selectParcelableOutput(
            @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final int... indexes) {
        return new MapBuilderWrapper<OUT>(
                SparseChannelsCompat.selectParcelableOutput(channel, indexes));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
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
     * @see SparseChannelsCompat#selectParcelableOutput(Channel, Iterable)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends
            SparseArrayCompat<LoaderStreamChannelCompat<OUT, OUT>>> selectParcelableOutput(
            @NotNull final Channel<?, ? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {
        return new MapBuilderWrapper<OUT>(
                SparseChannelsCompat.selectParcelableOutput(channel, indexes));
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
     * @see SparseChannelsCompat#selectableInput(Channel, int)
     */
    @NotNull
    public static <IN> ChannelsBuilder<? extends Channel<Selectable<IN>, ?>> selectableInput(
            @NotNull final Channel<? super IN, ?> channel, final int index) {
        return SparseChannelsCompat.selectableInput(channel, index);
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
     * @return the selectable loader stream builder.
     * @see SparseChannelsCompat#selectableOutput(Channel, int)
     */
    @NotNull
    public static <OUT> ChannelsBuilder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>, ? extends ParcelableSelectable<OUT>>> selectableOutput(
            @NotNull final Channel<?, ? extends OUT> channel, final int index) {
        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                SparseChannelsCompat.selectableOutput(channel, index));
    }

    /**
     * Builds and returns a new loader stream channel.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT, OUT> streamOf() {
        return streamOf(JRoutineCore.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT, OUT> streamOf(
            @Nullable final Iterable<OUT> outputs) {
        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified output.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT, OUT> streamOf(@Nullable final OUT output) {
        return streamOf(JRoutineCore.io().of(output));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     * <p>
     * Note that the stream will start producing results only when one of the {@link Channel}
     * methods is called.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT, OUT> streamOf(
            @Nullable final OUT... outputs) {
        return streamOf(JRoutineCore.io().of(outputs));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
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
    public static <OUT> LoaderStreamChannelCompat<OUT, OUT> streamOf(
            @Nullable final Channel<?, OUT> output) {
        final Channel<OUT, OUT> outputChannel = JRoutineCore.io().buildChannel();
        return new DefaultLoaderStreamChannelCompat<OUT, OUT>(outputChannel.pass(output).close());
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
     * @param function the function providing the stream channel instances.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the routine builder.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> withStream(
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends
                    StreamChannel<? super IN, ? extends OUT>> function) {
        if (!wrap(function).hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }

        return Streams.withStream(function);
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
     * @param function the function providing the stream channel instances.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the loader routine builder.
     * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> LoaderRoutineBuilder<IN, OUT> withStreamOn(
            @NotNull final LoaderContextCompat context,
            @NotNull final Function<? super StreamChannel<IN, IN>, ? extends
                    StreamChannel<? super IN, ? extends OUT>> function) {
        return JRoutineLoaderCompat.on(context).with(contextFactory(function));
    }
}
