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

import com.github.dm.jrt.builder.RoutineBuilder;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.Channels;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiConsumerWrapper;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.util.Reflection.asArgs;

/**
 * Utility class acting as a factory of stream output channels.
 * <p/>
 * Created by davide-maestroni on 11/26/2015.
 */
public class Streams extends Functions {

    private static final BiConsumerWrapper<? extends Iterable<?>, ? extends InputChannel<?>>
            sUnfold = wrapBiConsumer(new BiConsumer<Iterable<?>, InputChannel<?>>() {

        @SuppressWarnings("unchecked")
        public void accept(final Iterable<?> objects, final InputChannel<?> inputChannel) {

            inputChannel.pass((Iterable) objects);
        }
    });

    /**
     * Avoid direct instantiation.
     */
    protected Streams() {

    }

    /**
     * Returns a stream blending the outputs coming from the specified ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> blend(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.blend(channels));
    }

    /**
     * Returns a stream blending the outputs coming from the specified ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> blend(@NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>blend(channels));
    }

    /**
     * Returns a stream concatenating the outputs coming from the specified ones, so that, all the
     * outputs of the first channel will come before all the outputs of the second one, and so on.
     * <br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> concat(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.concat(channels));
    }

    /**
     * Returns a stream concatenating the outputs coming from the specified ones, so that, all the
     * outputs of the first channel will come before all the outputs of the second one, and so on.
     * <br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> concat(@NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>concat(channels));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream output channels,
     * provided by the specified function, to process input data.<br/>
     * The function should return a new instance each time it is called, starting from the passed
     * one.
     *
     * @param function the function providing the stream output channels.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factory(
            @NotNull final Function<? super StreamChannel<? extends IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        return new StreamInvocationFactory<IN, OUT>(wrapFunction(function));
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.
     *
     * @param size   the group size.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size) {

        return new GroupByInvocationFactory<DATA>(size);
    }

    /**
     * Returns a factory of invocations grouping the input data in collections of the specified
     * size.<br/>
     * If the inputs complete and the last group length is less than the target size, the missing
     * spaces will be filled with the specified placeholder instance.
     *
     * @param size        the group size.
     * @param placeholder the placeholder object used to fill the missing data needed to reach
     *                    the group size.
     * @param <DATA>      the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the size is not positive.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, List<DATA>> groupBy(final int size,
            @Nullable final DATA placeholder) {

        return new GroupByInvocationFactory<DATA>(size, placeholder);
    }

    /**
     * Returns a stream joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<List<? extends OUT>> join(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.join(channels));
    }

    /**
     * Returns a stream joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<List<? extends OUT>> join(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>join(channels));
    }

    /**
     * Returns a stream joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the list of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<List<? extends OUT>> join(@Nullable final OUT placeholder,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.join(placeholder, channels));
    }

    /**
     * Returns a stream joining the data coming from the specified list of channels.<br/>
     * An output will be generated only when at least one result is available for each channel.
     * Moreover, when all the output channels complete, the remaining outputs will be returned by
     * filling the gaps with the specified placeholder instance, so that the generated list of data
     * will always have the same size of the channel list.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param placeholder the placeholder instance.
     * @param channels    the array of channels.
     * @param <OUT>       the output data type.
     * @return the stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<List<? extends OUT>> join(@Nullable final Object placeholder,
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>join(placeholder, channels));
    }

    /**
     * Builds and returns a new lazy stream channel.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> lazyStreamOf() {

        return lazyStreamOf(JRoutine.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new lazy stream channel generating the specified outputs.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> lazyStreamOf(@Nullable final Iterable<OUT> outputs) {

        return lazyStreamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy stream channel generating the specified output.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> lazyStreamOf(@Nullable final OUT output) {

        return lazyStreamOf(JRoutine.io().of(output));
    }

    /**
     * Builds and returns a new lazy stream channel generating the specified outputs.<br/>
     * The stream will start producing results only when it is bound to another channel or an output
     * consumer or when any of the read methods is invoked.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> lazyStreamOf(@Nullable final OUT... outputs) {

        return lazyStreamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new lazy stream channel generating the specified outputs.<br/>
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
    public static <OUT> StreamChannel<OUT> lazyStreamOf(@NotNull final OutputChannel<OUT> output) {

        if (output == null) {

            throw new NullPointerException("the output channel instance must not be null");
        }

        final IOChannel<OUT> ioChannel = JRoutine.io().buildChannel();
        return new DefaultStreamChannel<OUT>(output, ioChannel);
    }

    /**
     * Returns an factory of invocations passing at max the specified number of input data and
     * discarding the following ones.
     *
     * @param count  the maximum number of data to pass.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> limit(final int count) {

        return new LimitInvocationFactory<DATA>(count);
    }

    /**
     * Merges the specified channels into a selectable stream.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.merge(startIndex, channels));
    }

    /**
     * Merges the specified channels into a selectable stream.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>merge(startIndex, channels));
    }

    /**
     * Merges the specified channels into a selectable stream. The selectable indexes will be the
     * same as the list ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> merge(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.merge(channels));
    }

    /**
     * Merges the specified channels into a selectable stream.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> merge(
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channelMap) {

        return streamOf(Channels.merge(channelMap));
    }

    /**
     * Merges the specified channels into a selectable stream. The selectable indexes will be the
     * same as the array ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>merge(channels));
    }

    /**
     * Returns a routine builder, whose invocation instances employ the streams provided by the
     * specified function, to process input data.<br/>
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
            @NotNull final Function<? super StreamChannel<? extends IN>, ? extends
                    StreamChannel<? extends OUT>> function) {

        return JRoutine.on(factory(function));
    }

    /**
     * Returns a new stream repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting binding of several output consumers.<br/>
     * Note that the passed channels will be bound as a result of the call.
     *
     * @param channel the output channel.
     * @param <OUT>   the output data type.
     * @return the repeating stream channel.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> repeat(@NotNull final OutputChannel<OUT> channel) {

        return streamOf(Channels.repeat(channel));
    }

    /**
     * Returns an factory of invocations skipping the specified number of input data.
     *
     * @param count  the number of data to skip.
     * @param <DATA> the data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    public static <DATA> InvocationFactory<DATA, DATA> skip(final int count) {

        return new SkipInvocationFactory<DATA>(count);
    }

    /**
     * Builds and returns a new stream channel.
     *
     * @param <OUT> the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> streamOf() {

        return streamOf(JRoutine.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> streamOf(@Nullable final Iterable<OUT> outputs) {

        return streamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new stream channel generating the specified output.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> streamOf(@Nullable final OUT output) {

        return streamOf(JRoutine.io().of(output));
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> streamOf(@Nullable final OUT... outputs) {

        return streamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new stream channel generating the specified outputs.
     * <p/>
     * Note that the output channel will be bound as a result of the call.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created stream instance.
     */
    @NotNull
    public static <OUT> StreamChannel<OUT> streamOf(@NotNull final OutputChannel<OUT> output) {

        return new DefaultStreamChannel<OUT>(output);
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that the passed channel will be bound as a result of the call.
     *
     * @param channel the channel to make selectable.
     * @param index   the channel index.
     * @param <OUT>   the output data type.
     * @return the selectable stream.
     */
    @NotNull
    public static <OUT> StreamChannel<? extends Selectable<OUT>> toSelectable(
            @NotNull final OutputChannel<? extends OUT> channel, final int index) {

        return streamOf(Channels.toSelectable(channel, index));
    }

    /**
     * Returns a bi-consumer wrapper unfolding iterable inputs into the returned elements.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <OUT> the output data type.
     * @return the bi-consumer instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> BiConsumerWrapper<Iterable<OUT>, InputChannel<OUT>> unfold() {

        return (BiConsumerWrapper<Iterable<OUT>, InputChannel<OUT>>) sUnfold;
    }

    /**
     * Routine invocation grouping data into collections of the same size.
     *
     * @param <DATA> the data type.
     */
    private static class GroupByInvocation<DATA> extends TemplateInvocation<DATA, List<DATA>> {

        private final ArrayList<DATA> mInputs = new ArrayList<DATA>();

        private final boolean mIsPlaceholder;

        private final DATA mPlaceholder;

        private final int mSize;

        /**
         * Constructor.
         *
         * @param size the group size.
         */
        private GroupByInvocation(final int size) {

            mSize = size;
            mPlaceholder = null;
            mIsPlaceholder = false;
        }

        /**
         * Constructor.
         *
         * @param size        the group size.
         * @param placeholder the placeholder object used to fill the missing data needed to reach
         *                    the group size.
         */
        private GroupByInvocation(final int size, @Nullable final DATA placeholder) {

            mSize = size;
            mPlaceholder = placeholder;
            mIsPlaceholder = true;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<List<DATA>> result) {

            final ArrayList<DATA> inputs = mInputs;
            final int size = mSize;
            if (inputs.size() < size) {
                inputs.add(input);
                if (inputs.size() == size) {
                    result.pass(new ArrayList<DATA>(inputs));
                    inputs.clear();
                }
            }
        }

        @Override
        public void onResult(@NotNull final ResultChannel<List<DATA>> result) {

            final ArrayList<DATA> inputs = mInputs;
            final int inputSize = inputs.size();
            if (inputSize > 0) {
                final ArrayList<DATA> data = new ArrayList<DATA>(inputs);
                final int size = mSize - inputSize;
                if (mIsPlaceholder && (size > 0)) {
                    data.addAll(Collections.nCopies(size, mPlaceholder));
                }
                result.pass(data);
            }
        }

        @Override
        public void onTerminate() {

            mInputs.clear();
        }
    }

    /**
     * Factory of grouping invocation.
     *
     * @param <DATA> the data type.
     */
    private static class GroupByInvocationFactory<DATA>
            extends ComparableInvocationFactory<DATA, List<DATA>> {

        private final boolean mIsPlaceholder;

        private final DATA mPlaceholder;

        private final int mSize;

        /**
         * Constructor.
         *
         * @param size the group size.
         * @throws java.lang.IllegalArgumentException if the size is not positive.
         */
        private GroupByInvocationFactory(final int size) {

            super(asArgs(size));
            if (size <= 0) {
                throw new IllegalArgumentException("the group size must be positive: " + size);
            }

            mSize = size;
            mPlaceholder = null;
            mIsPlaceholder = false;
        }

        /**
         * Constructor.
         *
         * @param size        the group size.
         * @param placeholder the placeholder object used to fill the missing data needed to reach
         *                    the group size.
         * @throws java.lang.IllegalArgumentException if the size is not positive.
         */
        private GroupByInvocationFactory(final int size, @Nullable final DATA placeholder) {

            super(asArgs(size, placeholder));
            if (size <= 0) {
                throw new IllegalArgumentException("the group size must be positive: " + size);
            }

            mSize = size;
            mPlaceholder = placeholder;
            mIsPlaceholder = true;
        }

        @NotNull
        @Override
        public Invocation<DATA, List<DATA>> newInvocation() {

            return (mIsPlaceholder) ? new GroupByInvocation<DATA>(mSize, mPlaceholder)
                    : new GroupByInvocation<DATA>(mSize);
        }
    }

    /**
     * Routine invocation passing only the first {@code count} input data.
     *
     * @param <DATA> the data type.
     */
    private static class LimitInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final int mCount;

        private int mCurrent;

        /**
         * Constructor.
         *
         * @param count the number of data to pass.
         */
        private LimitInvocation(final int count) {

            mCount = count;
        }

        @Override
        public void onInitialize() {

            mCurrent = 0;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            if (mCurrent < mCount) {
                ++mCurrent;
                result.pass(input);
            }
        }
    }

    /**
     * Factory of limiting data invocations.
     *
     * @param <DATA> the data type.
     */
    private static class LimitInvocationFactory<DATA>
            extends ComparableInvocationFactory<DATA, DATA> {

        private final int mCount;

        /**
         * Constructor.
         *
         * @param count the number of data to pass.
         * @throws java.lang.IllegalArgumentException if the count is negative.
         */
        private LimitInvocationFactory(final int count) {

            super(asArgs(count));
            if (count < 0) {
                throw new IllegalArgumentException("the count must not be negative: " + count);
            }

            mCount = count;
        }

        @NotNull
        @Override
        public Invocation<DATA, DATA> newInvocation() {

            return new LimitInvocation<DATA>(mCount);
        }
    }

    /**
     * Routine invocation skipping input data.
     *
     * @param <DATA> the data type.
     */
    private static class SkipInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final int mCount;

        private int mCurrent;

        /**
         * Constructor.
         *
         * @param count the number of data to skip.
         */
        private SkipInvocation(final int count) {

            mCount = count;
        }

        @Override
        public void onInitialize() {

            mCurrent = 0;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            if (mCurrent < mCount) {
                ++mCurrent;

            } else {
                result.pass(input);
            }
        }
    }

    /**
     * Factory of skip invocations.
     *
     * @param <DATA> the data type.
     */
    private static class SkipInvocationFactory<DATA>
            extends ComparableInvocationFactory<DATA, DATA> {

        private final int mCount;

        /**
         * Constructor.
         *
         * @param count the number of data to skip.
         * @throws java.lang.IllegalArgumentException if the count is negative.
         */
        private SkipInvocationFactory(final int count) {

            super(asArgs(count));
            if (count < 0) {
                throw new IllegalArgumentException("the count must not be negative: " + count);
            }

            mCount = count;
        }

        @NotNull
        @Override
        public Invocation<DATA, DATA> newInvocation() {

            return new SkipInvocation<DATA>(mCount);
        }
    }

    /**
     * Implementation of an invocation wrapping a stream output channel.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocation<IN, OUT> implements Invocation<IN, OUT> {

        private final Function<? super StreamChannel<? extends IN>, ? extends
                StreamChannel<? extends OUT>> mFunction;

        private IOChannel<IN> mInputChannel;

        private StreamChannel<? extends OUT> mOutputChannel;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream output channel.
         */
        private StreamInvocation(
                @NotNull final Function<? super StreamChannel<? extends IN>, ? extends
                        StreamChannel<? extends OUT>> function) {

            mFunction = function;
        }

        public void onAbort(@NotNull final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

        }

        public void onInitialize() {

            final IOChannel<IN> ioChannel = JRoutine.io().buildChannel();
            mOutputChannel = mFunction.apply(streamOf(ioChannel));
            mInputChannel = ioChannel;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            final StreamChannel<? extends OUT> outputChannel = mOutputChannel;
            if (!outputChannel.isBound()) {
                outputChannel.passTo(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            final StreamChannel<? extends OUT> outputChannel = mOutputChannel;
            if (!outputChannel.isBound()) {
                outputChannel.passTo(result);
            }

            mInputChannel.close();
        }

        public void onTerminate() {

            mInputChannel = null;
            mOutputChannel = null;
        }
    }

    /**
     * Implementation of a factory creating invocations wrapping a stream output channel.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocationFactory<IN, OUT>
            extends ComparableInvocationFactory<IN, OUT> {

        private final FunctionWrapper<? super StreamChannel<? extends IN>, ? extends
                StreamChannel<? extends OUT>> mFunction;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream output channel.
         */
        private StreamInvocationFactory(
                @NotNull final FunctionWrapper<? super StreamChannel<? extends IN>, ? extends
                        StreamChannel<? extends OUT>> function) {

            super(asArgs(function));
            mFunction = function;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new StreamInvocation<IN, OUT>(mFunction);
        }
    }
}
