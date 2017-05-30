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

package com.github.dm.jrt.android.v4;

import com.github.dm.jrt.JRoutine;
import com.github.dm.jrt.android.LoaderWrapperRoutineBuilder;
import com.github.dm.jrt.android.ServiceWrapperRoutineBuilder;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel
    .ParcelableByteChunkOutputStreamBuilder;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.function.JRoutineAndroidFunction;
import com.github.dm.jrt.android.function.builder.StatefulContextFactoryBuilder;
import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessContextFactoryBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.stream.builder.LoaderStreamLifter;
import com.github.dm.jrt.android.v4.channel.JRoutineSparseChannelsCompat;
import com.github.dm.jrt.android.v4.channel.builder.SparseChannelHandlerCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.android.v4.function.JRoutineLoaderFunctionCompat;
import com.github.dm.jrt.android.v4.stream.JRoutineLoaderStreamCompat;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features, with support for the
 * Android compatibility library.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineAndroidCompat extends JRoutine {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineAndroidCompat() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a channel handler employing the default executor.
   *
   * @return the handler instance.
   */
  @NotNull
  public static SparseChannelHandlerCompat channelHandler() {
    return JRoutineSparseChannelsCompat.channelHandler();
  }

  /**
   * Returns a channel handler employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the handler instance.
   */
  @NotNull
  public static SparseChannelHandlerCompat channelHandlerOn(
      @NotNull final ScheduledExecutor executor) {
    return JRoutineSparseChannelsCompat.channelHandlerOn(executor);
  }

  /**
   * Returns a builder of channels bound to the Loader identified by the specified ID.
   * <br>
   * If no Loader with the specified ID is running at the time of the channel creation, the
   * output will be aborted with a
   * {@link com.github.dm.jrt.android.core.invocation.MissingLoaderException
   * MissingLoaderException}.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper
   * thread, thus waiting for the outputs immediately after its invocation may result in a
   * deadlock.
   *
   * @param loaderSource the Loader source.
   * @param loaderId     the Loader ID.
   * @return the channel builder instance.
   */
  @NotNull
  public static LoaderChannelBuilder channelOn(@NotNull final LoaderSourceCompat loaderSource,
      final int loaderId) {
    return JRoutineLoaderCompat.channelOn(loaderSource, loaderId);
  }

  /**
   * Returns a new Context invocation factory based on the specified supplier instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the Context invocation factory.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public static <IN, OUT> ContextInvocationFactory<IN, OUT> contextFactoryOf(
      @NotNull final Supplier<? extends ContextInvocation<? super IN, ? extends OUT>> supplier) {
    return JRoutineAndroidFunction.contextFactoryOf(supplier);
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
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final Iterable<? extends ParcelableByteChunk> buffers) {
    return ParcelableByteChannel.parcelableInputStream(buffers);
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
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final ParcelableByteChunk buffer) {
    return ParcelableByteChannel.parcelableInputStream(buffer);
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
  public static ByteChunkInputStream parcelableInputStream(
      @NotNull final ParcelableByteChunk... buffers) {
    return ParcelableByteChannel.parcelableInputStream(buffers);
  }

  /**
   * Returns a builder of chunk output streams.
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @return the output stream builder.
   */
  @NotNull
  public static ParcelableByteChunkOutputStreamBuilder parcelableOutputStream() {
    return ParcelableByteChannel.parcelableOutputStream();
  }

  /**
   * Returns a Context based builder of Loader routines.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper
   * thread, thus waiting for the outputs immediately after its invocation may result in a
   * deadlock.
   * <br>
   * Note also that the input data passed to the invocation channel will be cached, and the
   * results will be produced only after the invocation channel is closed, so be sure to avoid
   * streaming inputs in order to prevent starvation or out of memory errors.
   *
   * @param loaderSource the Loader source.
   * @return the routine builder instance.
   */
  @NotNull
  public static LoaderRoutineBuilder routineOn(@NotNull final LoaderSourceCompat loaderSource) {
    return JRoutineLoaderCompat.routineOn(loaderSource);
  }

  /**
   * Returns a Context based builder of Service routines.
   *
   * @param serviceSource the Service source.
   * @return the routine builder.
   */
  @NotNull
  public static ServiceRoutineBuilder routineOn(@NotNull final ServiceSource serviceSource) {
    return JRoutineService.routineOn(serviceSource);
  }

  /**
   * Returns a builder of stateful Context invocation factories.
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
  public static <IN, OUT, STATE> StatefulContextFactoryBuilder<IN, OUT, STATE>
  statefulContextFactory() {
    return JRoutineLoaderFunctionCompat.statefulContextFactory();
  }

  /**
   * Returns a builder of stateful Loader routines.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful routine is the one computing a final result by accumulating the
   * input data (for instance, computing the sum of input numbers).
   * <p>
   * Note that, it is advisable to set a specific invocation ID to uniquely identify the built
   * routine, so to make the invocations survive configuration changes.
   *
   * @param loaderSource the Loader source.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @param <STATE>      the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulLoaderRoutineBuilder<IN, OUT, STATE> statefulRoutineOn(
      @NotNull final LoaderSourceCompat loaderSource) {
    return JRoutineLoaderFunctionCompat.statefulRoutineOn(loaderSource);
  }

  /**
   * Returns a builder of stateless Context invocation factories.
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
  public static <IN, OUT> StatelessContextFactoryBuilder<IN, OUT> statelessContextFactory() {
    return JRoutineLoaderFunctionCompat.statelessContextFactory();
  }

  /**
   * Returns a builder of stateless Loader routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless routine is the one processing each input separately (for
   * instance, computing the square of input numbers).
   * <p>
   * Note that, it is advisable to set a specific invocation ID to uniquely identify the built
   * routine, so to make the invocations survive configuration changes.
   *
   * @param loaderSource the Loader source.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessLoaderRoutineBuilder<IN, OUT> statelessRoutineOn(
      @NotNull final LoaderSourceCompat loaderSource) {
    return JRoutineLoaderFunctionCompat.statelessRoutineOn(loaderSource);
  }

  /**
   * Returns a builder of functions making the stream routine run on the specified Loader.
   * <p>
   * The example below shows how it's possible to make the computation happen in a dedicated Loader:
   * <pre><code>
   * JRoutineStream.withStreamOf(routine)
   *               .lift(JRoutineLoaderStreamCompat.streamLifterOn(loaderOf(activity))
   *                                               .withLoader()
   *                                               .withInvocationId(INVOCATION_ID)
   *                                               .configuration()
   *                                               .runOnLoader())
   *               .invoke()
   *               .consume(getConsumer())
   *               .close();
   * </code></pre>
   * Note that the Loader ID, by default, will only depend on the inputs, so that, in order to avoid
   * clashing, it is advisable to explicitly set the invocation ID like shown in the example.
   *
   * @param loaderSource the Loader source.
   * @return the lifting function builder.
   */
  @NotNull
  public static LoaderStreamLifter streamLifterOn(@NotNull final LoaderSourceCompat loaderSource) {
    return JRoutineLoaderStreamCompat.streamLifterOn(loaderSource);
  }

  /**
   * Returns a builder of routines wrapping a target object.
   *
   * @param loaderSource the Loader source.
   * @return the routine builder instance.
   */
  @NotNull
  public static LoaderWrapperRoutineBuilder wrapperOn(
      @NotNull final LoaderSourceCompat loaderSource) {
    return new DefaultLoaderWrapperRoutineBuilderCompat(loaderSource);
  }

  /**
   * Returns a builder of routines wrapping a target object.
   *
   * @param serviceSource the Service source.
   * @return the routine builder instance.
   */
  @NotNull
  public static ServiceWrapperRoutineBuilder wrapperOn(@NotNull final ServiceSource serviceSource) {
    return new DefaultServiceWrapperRoutineBuilder(serviceSource);
  }
}
