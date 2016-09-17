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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.function.FunctionDecorator.decorate;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p>
 * A stream routine builder allows to easily build a concatenation of invocations as a single
 * routine.
 * <br>
 * For instance, a routine computing the root mean square of a number of integers can be defined as:
 * <pre>
 *     <code>
 *
 *         final Routine&lt;Integer, Double&gt; rms =
 *                 JRoutineLoaderStreamCompat.&lt;Integer&gt;withStream()
 *                                           .immediate()
 *                                           .map(i -&gt; i * i)
 *                                           .map(averageFloat())
 *                                           .map(Math::sqrt)
 *                                           .on(loaderFrom(activity))
 *                                           .buildRoutine();
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 07/04/2016.
 */
public class JRoutineLoaderStreamCompat {

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineLoaderStreamCompat() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a stream routine builder.
     *
     * @param <IN> the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStream() {
        return new DefaultLoaderStreamBuilderCompat<IN, IN>();
    }

    /**
     * Returns a stream routine builder producing only the specified input.
     * <br>
     * The data will be produced only when the invocation completes.
     * <br>
     * If any other input is passed to the build routine, the invocation will be aborted with an
     * {@link java.lang.IllegalStateException}.
     *
     * @param input the input.
     * @param <IN>  the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStreamOf(@Nullable final IN input) {
        return withStreamOf(Channels.replay(JRoutineCore.io().of(input)).buildChannels());
    }

    /**
     * Returns a stream routine builder producing only the specified inputs.
     * <br>
     * The data will be produced only when the invocation completes.
     * <br>
     * If any other input is passed to the build routine, the invocation will be aborted with an
     * {@link java.lang.IllegalStateException}.
     *
     * @param inputs the input data.
     * @param <IN>   the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStreamOf(
            @Nullable final IN... inputs) {
        return withStreamOf(Channels.replay(JRoutineCore.io().of(inputs)).buildChannels());
    }

    /**
     * Returns a stream routine builder producing only the inputs returned by the specified
     * iterable.
     * <br>
     * The data will be produced only when the invocation completes.
     * <br>
     * If any other input is passed to the build routine, the invocation will be aborted with an
     * {@link java.lang.IllegalStateException}.
     *
     * @param inputs the inputs iterable.
     * @param <IN>   the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStreamOf(
            @Nullable final Iterable<? extends IN> inputs) {
        return withStreamOf(Channels.replay(JRoutineCore.io().of(inputs)).buildChannels());
    }

    /**
     * Returns a stream routine builder producing only the inputs returned by the specified
     * channel.
     * <br>
     * The data will be produced only when the invocation completes.
     * <br>
     * If any other input is passed to the build routine, the invocation will be aborted with an
     * {@link java.lang.IllegalStateException}.
     * <p>
     * Note that the passed channel will be bound as a result of the call, so, in order to support
     * multiple invocations, consider wrapping the channel in a replayable one, by calling the
     * {@link Channels#replay(Channel)} utility method.
     *
     * @param channel the input channel.
     * @param <IN>    the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStreamOf(
            @Nullable final Channel<?, ? extends IN> channel) {
        return JRoutineLoaderStreamCompat.<IN>withStream().lift(
                new Function<Function<? super Channel<?, IN>, ? extends Channel<?, IN>>,
                        Function<? super Channel<?, IN>, ? extends Channel<?, IN>>>() {

                    public Function<? super Channel<?, IN>, ? extends Channel<?, IN>> apply(
                            final Function<? super Channel<?, IN>, ? extends Channel<?, IN>>
                                    function) {
                        return decorate(function).andThen(
                                new Function<Channel<?, IN>, Channel<?, IN>>() {

                                    public Channel<?, IN> apply(final Channel<?, IN> inputs) {
                                        final Channel<IN, IN> outputChannel =
                                                JRoutineCore.io().buildChannel();
                                        inputs.bind(new ChannelConsumer<IN>() {

                                            public void onComplete() {
                                                outputChannel.pass(channel).close();
                                            }

                                            public void onError(
                                                    @NotNull final RoutineException error) {
                                                outputChannel.abort(error);
                                            }

                                            public void onOutput(final IN output) {
                                                throw new IllegalStateException();
                                            }
                                        });
                                        return outputChannel;
                                    }
                                });
                    }
                });
    }
}
