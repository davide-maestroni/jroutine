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

package com.github.dm.jrt;

import com.github.dm.jrt.channel.JRoutineChannels;
import com.github.dm.jrt.channel.builder.ChannelHandler;
import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStreamBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.JRoutineFunction;
import com.github.dm.jrt.function.builder.FunctionalChannelConsumer;
import com.github.dm.jrt.function.builder.StatefulFactoryBuilder;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessFactoryBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.routine.StreamRoutine;
import com.github.dm.jrt.stream.transform.StreamLifter;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutine {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutine() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels running on the default executor.
   *
   * @return the channel builder instance.
   */
  @NotNull
  public static ChannelBuilder channel() {
    return JRoutineCore.channel();
  }

  /**
   * Returns a channel handler employing the default executor.
   *
   * @return the handler instance.
   */
  @NotNull
  public static ChannelHandler channelHandler() {
    return JRoutineChannels.channelHandler();
  }

  /**
   * Returns a channel handler employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the handler instance.
   */
  @NotNull
  public static ChannelHandler channelHandlerOn(@NotNull final ScheduledExecutor executor) {
    return JRoutineChannels.channelHandlerOn(executor);
  }

  /**
   * Returns a builder of channels running on the specified executor.
   *
   * @param executor the executor instance.
   * @return the channel builder instance.
   */
  @NotNull
  public static ChannelBuilder channelOn(@NotNull final ScheduledExecutor executor) {
    return JRoutineCore.channelOn(executor);
  }

  /**
   * Returns a channel pushing inputs to the specified input one and collecting outputs from the
   * specified output one.
   * <p>
   * Note that it's up to the caller to ensure that inputs and outputs of two channels are actually
   * connected.
   *
   * @param inputChannel  the input channel.
   * @param outputChannel the output channel.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the new channel instance.
   */
  @NotNull
  public static <IN, OUT> Channel<IN, OUT> flatten(@NotNull final Channel<IN, ?> inputChannel,
      @NotNull final Channel<?, OUT> outputChannel) {
    return JRoutineCore.flatten(inputChannel, outputChannel);
  }

  /**
   * Gets an input stream returning the concatenation of the data contained in the specified
   * buffers.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffers the byte buffers whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified buffers.
   */
  @NotNull
  public static ByteChunkInputStream inputStream(@NotNull final ByteChunk... buffers) {
    return ByteChannel.inputStream(buffers);
  }

  /**
   * Gets an input stream returning the concatenation of the data contained in the specified
   * buffers.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffers the byte buffers whose data have to be concatenated.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for one
   *                                         of the specified buffers.
   */
  @NotNull
  public static ByteChunkInputStream inputStream(
      @NotNull final Iterable<? extends ByteChunk> buffers) {
    return ByteChannel.inputStream(buffers);
  }

  /**
   * Gets an input stream returning the data contained in the specified buffer.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffer the byte buffer.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for the
   *                                         specified buffer.
   */
  @NotNull
  public static ByteChunkInputStream inputStream(@NotNull final ByteChunk buffer) {
    return ByteChannel.inputStream(buffer);
  }

  /**
   * Returns a channel consumer builder employing the specified action to handle the invocation
   * completion.
   *
   * @param onComplete the action instance.
   * @return the channel consumer builder.
   */
  @NotNull
  public static FunctionalChannelConsumer<Object> onComplete(@NotNull final Action onComplete) {
    return JRoutineFunction.onComplete(onComplete);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation errors.
   *
   * @param onError the consumer function.
   * @return the channel consumer builder.
   */
  @NotNull
  public static FunctionalChannelConsumer<Object> onError(
      @NotNull final Consumer<? super RoutineException> onError) {
    return JRoutineFunction.onError(onError);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation outputs.
   *
   * @param onOutput the consumer function.
   * @param onError  the consumer function.
   * @param <OUT>    the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError) {
    return JRoutineFunction.onOutput(onOutput, onError);
  }

  /**
   * Returns a channel consumer builder employing the specified functions to handle the invocation
   * outputs, errors adn completion.
   *
   * @param onOutput   the consumer function.
   * @param onError    the consumer function.
   * @param onComplete the action instance.
   * @param <OUT>      the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError, @NotNull final Action onComplete) {
    return JRoutineFunction.onOutput(onOutput, onError, onComplete);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation outputs.
   *
   * @param onOutput the consumer function.
   * @param <OUT>    the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput) {
    return JRoutineFunction.onOutput(onOutput);
  }

  /**
   * Returns a builder of chunk output streams.
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @return the output stream builder.
   */
  @NotNull
  public static ByteChunkOutputStreamBuilder outputStream() {
    return ByteChannel.outputStream();
  }

  /**
   * Returns a channel making the wrapped one read-only.
   * <br>
   * The returned channel will fail on any attempt to pass input data, and will ignore any closing
   * command.
   * <br>
   * Note, however, that abort operations will be fulfilled.
   *
   * @param channel the wrapped channel.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the new channel instance.
   */
  @NotNull
  public static <IN, OUT> Channel<IN, OUT> readOnly(@NotNull final Channel<IN, OUT> channel) {
    return JRoutineCore.readOnly(channel);
  }

  /**
   * Returns a builder of routines running on the default executor.
   *
   * @return the routine builder instance.
   */
  @NotNull
  public static RoutineBuilder routine() {
    return JRoutineCore.routine();
  }

  /**
   * Returns a builder of routines running on the specified executor.
   *
   * @param executor the executor instance.
   * @return the routine builder instance.
   */
  @NotNull
  public static RoutineBuilder routineOn(@NotNull final ScheduledExecutor executor) {
    return JRoutineCore.routineOn(executor);
  }

  /**
   * Returns a builder of stateful invocation factories.
   * <p>
   * This invocations retain a mutable state during their lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulFactoryBuilder<IN, OUT, STATE> statefulFactory() {
    return JRoutineFunction.statefulFactory();
  }

  /**
   * Returns a builder of stateful routines running on the default executor.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulRoutineBuilder<IN, OUT, STATE> statefulRoutine() {
    return JRoutineFunction.statefulRoutine();
  }

  /**
   * Returns a builder of stateful routines.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param executor the executor instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @param <STATE>  the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulRoutineBuilder<IN, OUT, STATE> statefulRoutineOn(
      @NotNull final ScheduledExecutor executor) {
    return JRoutineFunction.statefulRoutineOn(executor);
  }

  /**
   * Returns a builder of stateless invocation factories.
   * <p>
   * This invocations do not retain a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessFactoryBuilder<IN, OUT> statelessFactory() {
    return JRoutineFunction.statelessFactory();
  }

  /**
   * Returns a builder of stateless routines running on the default executor.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessRoutineBuilder<IN, OUT> statelessRoutine() {
    return JRoutineFunction.statelessRoutine();
  }

  /**
   * Returns a builder of stateless routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param executor the executor instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessRoutineBuilder<IN, OUT> statelessRoutineOn(
      @NotNull final ScheduledExecutor executor) {
    return JRoutineFunction.statelessRoutineOn(executor);
  }

  /**
   * Returns a builder of lifting functions.
   *
   * @return the builder instance.
   */
  @NotNull
  public static StreamLifter streamLifter() {
    return JRoutineStream.streamLifter();
  }

  /**
   * Returns a builder of lifting functions employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the builder instance.
   */
  @NotNull
  public static StreamLifter streamLifterOn(@NotNull final ScheduledExecutor executor) {
    return JRoutineStream.streamLifterOn(executor);
  }

  /**
   * Returns a stream routine wrapping the specified one.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the stream routine.
   */
  @NotNull
  public static <IN, OUT> StreamRoutine<IN, OUT> streamOf(@NotNull final Routine<IN, OUT> routine) {
    return JRoutineStream.streamOf(routine);
  }

  /**
   * Returns a builder of routines running on the specified executor, wrapping a target object.
   *
   * @return the routine builder instance.
   */
  @NotNull
  public static WrapperRoutineBuilder wrapper() {
    return wrapperOn(defaultExecutor());
  }

  /**
   * Returns a builder of routines wrapping a target object.
   *
   * @param executor the executor instance.
   * @return the routine builder instance.
   */
  @NotNull
  public static WrapperRoutineBuilder wrapperOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultWrapperRoutineBuilder(executor);
  }
}
