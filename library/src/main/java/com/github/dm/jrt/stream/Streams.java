/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.Channels;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p/>
 * Created by davide-maestroni on 11/26/2015.
 */
public class Streams extends Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Streams() {

    }

    /**
     * Returns a stream output channel blending the outputs coming from the specified ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> blend(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.blend(channels));
    }

    /**
     * Returns a stream output channel blending the outputs coming from the specified ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> blend(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>blend(channels));
    }

    /**
     * Returns a stream output channel concatenating the outputs coming from the specified ones, so
     * that, all the outputs of the first channel will come before all the outputs of the second
     * one, and so on.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> concat(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.concat(channels));
    }

    /**
     * Returns a stream output channel concatenating the outputs coming from the specified ones, so
     * that, all the outputs of the first channel will come before all the outputs of the second
     * one, and so on.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> concat(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>concat(channels));
    }

    /**
     * Returns an invocation factory, whose invocation instances employ the stream output channels
     * provided by the specified function to process input data.<br/>
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
            @NotNull final Function<? super StreamOutputChannel<? extends IN>, ? extends
                    StreamOutputChannel<? extends OUT>> function) {

        return new StreamInvocationFactory<IN, OUT>(function);
    }

    /**
     * Returns a stream output channel joining the data coming from the specified list of channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the list of channels.
     * @param <OUT>    the output data type.
     * @return the output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<List<? extends OUT>> join(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.join(channels));
    }

    /**
     * Returns a stream output channel joining the data coming from the specified list of channels.
     * <br/>
     * An output will be generated only when at least one result is available for each channel.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the array of channels.
     * @param <OUT>    the output data type.
     * @return the stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<List<? extends OUT>> join(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>join(channels));
    }

    /**
     * Returns a stream output channel joining the data coming from the specified list of channels.
     * <br/>
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
    public static <OUT> StreamOutputChannel<List<? extends OUT>> joinAndFlush(
            @Nullable final OUT placeholder,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.joinAndFlush(placeholder, channels));
    }

    /**
     * Returns a stream output channel joining the data coming from the specified list of channels.
     * <br/>
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
    public static <OUT> StreamOutputChannel<List<? extends OUT>> joinAndFlush(
            @Nullable final Object placeholder, @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>joinAndFlush(placeholder, channels));
    }

    /**
     * Merges the specified channels into a selectable one.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the list of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.merge(startIndex, channels));
    }

    /**
     * Merges the specified channels into a selectable one.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param startIndex the selectable start index.
     * @param channels   the array of channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <OUT> StreamOutputChannel<? extends Selectable<OUT>> merge(final int startIndex,
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>merge(startIndex, channels));
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the list ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final List<? extends OutputChannel<? extends OUT>> channels) {

        return streamOf(Channels.merge(channels));
    }

    /**
     * Merges the specified channels into a selectable one.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channelMap the map of indexes and output channels.
     * @param <OUT>      the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified map is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final Map<Integer, ? extends OutputChannel<? extends OUT>> channelMap) {

        return streamOf(Channels.merge(channelMap));
    }

    /**
     * Merges the specified channels into a selectable one. The selectable indexes will be the same
     * as the array ones.<br/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUT>    the output data type.
     * @return the selectable stream channel.
     * @throws java.lang.IllegalArgumentException if the specified array is empty.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<? extends Selectable<OUT>> merge(
            @NotNull final OutputChannel<?>... channels) {

        return streamOf(Channels.<OUT>merge(channels));
    }

    /**
     * Builds and returns a new stream output channel.
     *
     * @param <OUT> the output data type.
     * @return the newly created channel instance.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> streamOf() {

        return streamOf(JRoutine.io().<OUT>buildChannel().close());
    }

    /**
     * Builds and returns a new stream output channel generating the specified outputs.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created channel instance.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> streamOf(@Nullable final Iterable<OUT> outputs) {

        return streamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new stream output channel generating the specified output.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created channel instance.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> streamOf(@Nullable final OUT output) {

        return streamOf(JRoutine.io().of(output));
    }

    /**
     * Builds and returns a new stream output channel generating the specified outputs.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created channel instance.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> streamOf(@Nullable final OUT... outputs) {

        return streamOf(JRoutine.io().of(outputs));
    }

    /**
     * Builds and returns a new stream output channel generating the specified outputs.
     *
     * @param output the output channel returning the output data.
     * @param <OUT>  the output data type.
     * @return the newly created channel instance.
     */
    @NotNull
    public static <OUT> StreamOutputChannel<OUT> streamOf(
            @NotNull final OutputChannel<OUT> output) {

        return new DefaultStreamOutputChannel<OUT>(output);
    }

    /**
     * Implementation of an invocation wrapping a stream output channel.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocation<IN, OUT> implements Invocation<IN, OUT> {

        private final Function<? super StreamOutputChannel<? extends IN>, ? extends
                StreamOutputChannel<? extends OUT>> mFunction;

        private IOChannel<IN> mInputChannel;

        private StreamOutputChannel<? extends OUT> mOutputChannel;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream output channel.
         */
        private StreamInvocation(final Function<? super StreamOutputChannel<? extends IN>, ? extends
                StreamOutputChannel<? extends OUT>> function) {

            mFunction = function;
        }

        public void onAbort(@Nullable final RoutineException reason) {

            mInputChannel.abort(reason);
        }

        public void onDestroy() {

            mInputChannel = null;
            mOutputChannel = null;
        }

        public void onInitialize() {

            final IOChannel<IN> ioChannel = JRoutine.io().buildChannel();
            mOutputChannel = mFunction.apply(streamOf(ioChannel));
            mInputChannel = ioChannel;
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            final StreamOutputChannel<? extends OUT> outputChannel = mOutputChannel;

            if (!outputChannel.isBound()) {

                outputChannel.passTo(result);
            }

            mInputChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            final StreamOutputChannel<? extends OUT> outputChannel = mOutputChannel;

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
    private static class StreamInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final Function<? super StreamOutputChannel<? extends IN>, ? extends
                StreamOutputChannel<? extends OUT>> mFunction;

        /**
         * Constructor.
         *
         * @param function the function used to instantiate the stream output channel.
         */
        private StreamInvocationFactory(
                final Function<? super StreamOutputChannel<? extends IN>, ? extends
                        StreamOutputChannel<? extends OUT>> function) {

            if (function == null) {

                throw new NullPointerException("the function instance must not be null");
            }

            mFunction = function;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return new StreamInvocation<IN, OUT>(mFunction);
        }
    }
}
