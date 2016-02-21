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

import com.github.dm.jrt.android.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.v4.core.ChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Configurable;
import com.github.dm.jrt.builder.RoutineBuilder;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.core.Channels.Builder;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;

/**
 * Utility class acting as a factory of stream output channels.
 * <p/>
 * Created by davide-maestroni on 01/04/2016.
 */
public class StreamsCompat extends Streams {

    /**
     * Avoid direct instantiation.
     */
    protected StreamsCompat() {

    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#blend(Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<OUT>> blend(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(ChannelsCompat.blend(channels));
    }

    /**
     * Returns a builder of loader streams blending the outputs coming from the specified ones.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#blend(OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<OUT>> blend(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(ChannelsCompat.<OUT>blend(channels));
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#concat(Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<OUT>> concat(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<OUT>(ChannelsCompat.concat(channels));
    }

    /**
     * Returns a builder of loader stream channels concatenating the outputs coming from the
     * specified ones, so that, all the outputs of the first channel will come before all the
     * outputs of the second one, and so on.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#concat(OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<OUT>> concat(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<OUT>(ChannelsCompat.<OUT>concat(channels));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream output channels,
     * provided by the specified function, to process input data.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.<br/>
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
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> contextFactory(
            @NotNull final Function<? super StreamChannel<IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        return factoryFrom(onStream(function), wrap(function).hashCode(), DelegationType.SYNC);
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the collection of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#join(Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<List<? extends OUT>>> join(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(ChannelsCompat.join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#join(OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<List<? extends OUT>>> join(
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(ChannelsCompat.<OUT>join(channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the collection of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#join(Object, Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<List<? extends OUT>>> join(
            @Nullable final OUT placeholder,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<List<? extends OUT>>(ChannelsCompat.join(placeholder, channels));
    }

    /**
     * Returns a builder of loader streams joining the data coming from the specified channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#join(Object, OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<List<? extends OUT>>> join(
            @Nullable final OUT placeholder, @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<List<? extends OUT>>(ChannelsCompat.join(placeholder, channels));
    }

    /**
     * Builds and returns a new lazy loader stream channel.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf() {

        return lazyStreamOf(JRoutineCompat.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @Nullable final Iterable<OUT> outputs) {

        return lazyStreamOf(JRoutineCompat.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified output.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(@Nullable final OUT output) {

        return lazyStreamOf(JRoutineCompat.io().of(output));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @Nullable final OUT... outputs) {

        return lazyStreamOf(JRoutineCompat.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy loader stream channel generating the specified outputs.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     * <p/>
     * Note that the output channel will be bound as a result of the call.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <OUT> LoaderStreamChannelCompat<OUT> lazyStreamOf(
            @NotNull final OutputChannel<OUT> output) {

        if (output == null) {
            throw new NullPointerException("the output channel instance must not be null");
        }

        final IOChannel<OUT> ioChannel = JRoutineCompat.io().buildChannel();
        return new DefaultLoaderStreamChannelCompat<OUT>(null, output, ioChannel);
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the collection of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#merge(int, Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                ChannelsCompat.merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#merge(int, OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                ChannelsCompat.<OUT>merge(startIndex, channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified collection is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#merge(Collection)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final Collection<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(ChannelsCompat.merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#merge(OutputChannel[])
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(@NotNull final OutputChannel<?>... channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(ChannelsCompat.<OUT>merge(channels));
    }

    /**
     * Returns a builder of loader stream merging the specified channels into a selectable one.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channels the map of indexes and output channels.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel builder.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#merge(SparseArrayCompat)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> merge(
            @NotNull final SparseArrayCompat<? extends OutputChannel<? extends OUT>> channels) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(ChannelsCompat.merge(channels));
    }

    /**
     * Returns a routine builder, whose invocation instances employ the streams provided by the
     * specified function, to process input data.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.<br/>
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
     * the specified function, to process input data.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * scope.<br/>
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

        return JRoutineCompat.with(context).on(contextFactory(function));
    }

    /**
     * Returns a builder of streams repeating the output data to any newly bound channel or
     * consumer, thus effectively supporting binding of several output consumers.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channel the output channel.
     * @param <OUT>   the output data type.
     * @return the repeating stream channel builder.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#repeat(OutputChannel)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<OUT>> repeat(
            @NotNull final OutputChannel<OUT> channel) {

        return new BuilderWrapper<OUT>(ChannelsCompat.repeat(channel));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param startIndex the selectable start index.
     * @param rangeSize  the size of the range of indexes (must be positive).
     * @param channel    the selectable channel.
     * @param <OUT>      the output data type.
     * @return the map of indexes and output channels builder.
     * @throws java.lang.IllegalArgumentException if the specified range size is negative or 0.
     * @see com.github.dm.jrt.core.Channels#select(int, int, OutputChannel)
     */
    @NotNull
    public static <OUT> Builder<? extends SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>
    selectParcelable(
            final int startIndex, final int rangeSize,
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel) {

        return new MapBuilderWrapper<OUT>(
                ChannelsCompat.selectParcelable(startIndex, rangeSize, channel));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the list of indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @see com.github.dm.jrt.core.Channels#select(OutputChannel, int...)
     */
    @NotNull
    public static <OUT> Builder<? extends SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>
    selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final int... indexes) {

        return new MapBuilderWrapper<OUT>(ChannelsCompat.selectParcelable(channel, indexes));
    }

    /**
     * Returns a builder of maps of loader stream channels returning the output data filtered by the
     * specified indexes.<br/>
     * Note that the builder will return the same map for the same inputs and equal configuration,
     * and that the passed channels will be bound as a result of the creation.
     *
     * @param channel the selectable output channel.
     * @param indexes the iterable returning the channel indexes.
     * @param <OUT>   the output data type.
     * @return the map of indexes and output channels builder.
     * @see com.github.dm.jrt.core.Channels#select(OutputChannel, Iterable)
     */
    @NotNull
    public static <OUT> Builder<? extends SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>
    selectParcelable(
            @NotNull final OutputChannel<? extends ParcelableSelectable<? extends OUT>> channel,
            @NotNull final Iterable<Integer> indexes) {

        return new MapBuilderWrapper<OUT>(ChannelsCompat.selectParcelable(channel, indexes));
    }

    /**
     * Builds and returns a new loader stream channel.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf() {

        return streamOf(JRoutineCompat.io().<OUT>buildChannel().close());
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

        return streamOf(JRoutineCompat.io().of(outputs));
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

        return streamOf(JRoutineCompat.io().of(output));
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

        return streamOf(JRoutineCompat.io().of(outputs));
    }

    /**
     * Builds and returns a new loader stream channel generating the specified outputs.
     * <p/>
     * Note that the output channel will be bound as a result of the call.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> LoaderStreamChannelCompat<OUT> streamOf(
            @NotNull final OutputChannel<OUT> output) {

        return new DefaultLoaderStreamChannelCompat<OUT>(null, output);
    }

    /**
     * Returns a builder of channels making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the builder will successfully create only one stream channel instance, and that the
     * passed channels will be bound as a result of the creation.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable loader stream builder.
     * @see com.github.dm.jrt.android.v4.core.ChannelsCompat#toSelectable(OutputChannel, int)
     */
    @NotNull
    public static <OUT> Builder<? extends LoaderStreamChannelCompat<? extends
            ParcelableSelectable<OUT>>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return new BuilderWrapper<ParcelableSelectable<OUT>>(
                ChannelsCompat.toSelectable(channel, index));
    }

    /**
     * Builder implementation wrapping a builder of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class BuilderWrapper<OUT> implements Builder<LoaderStreamChannelCompat<OUT>>,
            Configurable<Builder<LoaderStreamChannelCompat<OUT>>> {

        private final Builder<? extends OutputChannel<? extends OUT>> mBuilder;

        private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped instance.
         */
        private BuilderWrapper(
                @NotNull final Builder<? extends OutputChannel<? extends OUT>> wrapped) {

            mBuilder = wrapped;
        }

        @NotNull
        @SuppressWarnings("unchecked")
        public LoaderStreamChannelCompat<OUT> build() {

            return (LoaderStreamChannelCompat<OUT>) streamOf(mBuilder.build());
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public Builder<LoaderStreamChannelCompat<OUT>> setConfiguration(
                @NotNull final ChannelConfiguration configuration) {

            if (configuration == null) {
                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            mBuilder.withChannels().with(null).with(configuration).getConfigured();
            return this;
        }

        @NotNull
        public ChannelConfiguration.Builder<? extends Builder<LoaderStreamChannelCompat<OUT>>>
        withChannels() {

            final ChannelConfiguration config = mConfiguration;
            return new ChannelConfiguration.Builder<Builder<LoaderStreamChannelCompat<OUT>>>(this,
                                                                                             config);
        }
    }

    /**
     * Builder implementation wrapping a builder of maps of output channels.
     *
     * @param <OUT> the output data type.
     */
    private static class MapBuilderWrapper<OUT>
            implements Builder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>,
            Configurable<Builder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>> {

        private final Builder<? extends SparseArrayCompat<OutputChannel<OUT>>> mBuilder;

        private ChannelConfiguration mConfiguration = ChannelConfiguration.DEFAULT_CONFIGURATION;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped instance.
         */
        private MapBuilderWrapper(
                @NotNull final Builder<? extends SparseArrayCompat<OutputChannel<OUT>>> wrapped) {

            mBuilder = wrapped;
        }

        @NotNull
        public SparseArrayCompat<LoaderStreamChannelCompat<OUT>> build() {

            final SparseArrayCompat<OutputChannel<OUT>> channels = mBuilder.build();
            final int size = channels.size();
            final SparseArrayCompat<LoaderStreamChannelCompat<OUT>> channelMap =
                    new SparseArrayCompat<LoaderStreamChannelCompat<OUT>>(size);
            for (int i = 0; i < size; i++) {
                channelMap.append(channels.keyAt(i), streamOf(channels.valueAt(i)));
            }

            return channelMap;
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        public Builder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>> setConfiguration(
                @NotNull final ChannelConfiguration configuration) {

            if (configuration == null) {
                throw new NullPointerException("the invocation configuration must not be null");
            }

            mConfiguration = configuration;
            mBuilder.withChannels().with(null).with(configuration).getConfigured();
            return this;
        }

        @NotNull
        public ChannelConfiguration.Builder<? extends
                Builder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>> withChannels() {

            final ChannelConfiguration config = mConfiguration;
            return new ChannelConfiguration
                    .Builder<Builder<SparseArrayCompat<LoaderStreamChannelCompat<OUT>>>>(
                    this, config);
        }
    }
}
