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

package com.github.dm.jrt.android.v11;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;

import com.github.dm.jrt.JRoutine;
import com.github.dm.jrt.android.ServiceBuilder;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkInputStream;
import com.github.dm.jrt.channel.io.ChunkOutputStreamBuilder;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features, specific to the Android
 * platform.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.JRoutineAndroidCompat JRoutineAndroidCompat} for support
 * of API levels lower than {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineAndroid extends SparseChannels {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineAndroid() {
    ConstantConditions.avoid();
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
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
  public static ChunkInputStream getInputStream(@NotNull final ParcelableByteChunk... buffers) {
    return ParcelableByteChannel.getInputStream(buffers);
  }

  /**
   * Creates an input stream returning the concatenation of the data contained in the specified
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
  public static ChunkInputStream getInputStream(
      @NotNull final Iterable<? extends ParcelableByteChunk> buffers) {
    return ParcelableByteChannel.getInputStream(buffers);
  }

  /**
   * Creates an input stream returning the data contained in the specified buffer.
   * <p>
   * Note that only one input stream can be created for each buffer.
   *
   * @param buffer the byte buffer.
   * @return the input stream.
   * @throws java.lang.IllegalStateException if an input stream has been already created for the
   *                                         specified buffer.
   */
  @NotNull
  public static ChunkInputStream getInputStream(@NotNull final ParcelableByteChunk buffer) {
    return ParcelableByteChannel.getInputStream(buffer);
  }

  /**
   * Returns a builder of channels producing no data.
   * <p>
   * Note that the returned channels will be already closed.
   *
   * @param <OUT> the output data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> of() {
    return JRoutine.of();
  }

  /**
   * Returns a builder of channels producing the specified output.
   * <p>
   * Note that the returned channels will be already closed.
   *
   * @param output the output.
   * @param <OUT>  the output data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> of(@Nullable OUT output) {
    return JRoutine.of(output);
  }

  /**
   * Returns a builder of channels producing the specified outputs.
   * <p>
   * Note that the returned channels will be already closed.
   *
   * @param outputs the output data.
   * @param <OUT>   the output data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> of(@Nullable OUT... outputs) {
    return JRoutine.of(outputs);
  }

  /**
   * Returns a builder of channels producing the specified outputs.
   * <p>
   * Note that the returned channels will be already closed.
   *
   * @param outputs the iterable returning the output data.
   * @param <OUT>   the output data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> of(@Nullable Iterable<OUT> outputs) {
    return JRoutine.of(outputs);
  }

  /**
   * Returns a channel builder.
   *
   * @param <DATA> the data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <DATA> ChannelBuilder<DATA, DATA> ofData() {
    return JRoutine.ofData();
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param activity the Loader Activity.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilder on(@NotNull final Activity activity) {
    return on(loaderFrom(activity));
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param activity the Loader Activity.
   * @param context  the Context used to get the application one.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilder on(@NotNull final Activity activity, @NotNull final Context context) {
    return on(loaderFrom(activity, context));
  }

  /**
   * Returns a Context based builder of Service routine builders.
   *
   * @param context the Service Context.
   * @return the Context based builder.
   */
  @NotNull
  public static ServiceBuilder on(@NotNull final Context context) {
    return on(serviceFrom(context));
  }

  /**
   * Returns a Context based builder of Service routine builders.
   *
   * @param context      the Service Context.
   * @param serviceClass the Service class.
   * @return the Context based builder.
   */
  @NotNull
  public static ServiceBuilder on(@NotNull final Context context,
      @NotNull final Class<? extends InvocationService> serviceClass) {
    return on(serviceFrom(context, serviceClass));
  }

  /**
   * Returns a Context based builder of Service routine builders.
   *
   * @param context the Service Context.
   * @param service the Service Intent.
   * @return the Context based builder.
   */
  @NotNull
  public static ServiceBuilder on(@NotNull final Context context, @NotNull final Intent service) {
    return on(serviceFrom(context, service));
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param fragment the Loader Fragment.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilder on(@NotNull final Fragment fragment) {
    return on(loaderFrom(fragment));
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param fragment the Loader Fragment.
   * @param context  the Context used to get the application one.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilder on(@NotNull final Fragment fragment, @NotNull final Context context) {
    return on(loaderFrom(fragment, context));
  }

  /**
   * Returns a Context based builder of Loader routine builders.
   *
   * @param context the Loader context.
   * @return the Context based builder.
   */
  @NotNull
  public static LoaderBuilder on(@NotNull final LoaderContext context) {
    return new LoaderBuilder(context);
  }

  /**
   * Returns a Context based builder of Service routine builders.
   *
   * @param context the Service context.
   * @return the Context based builder.
   */
  @NotNull
  public static ServiceBuilder on(@NotNull final ServiceContext context) {
    return new ServiceBuilder(context) {};
  }

  /**
   * Returns a builder of buffer output streams.
   * <br>
   * Since the byte buffers generated by the channel are likely to be part of a remote procedure
   * call, be aware of the limits imposed by the Android OS architecture when choosing a specific
   * buffer size (see {@link android.os.TransactionTooLargeException}).
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @param channel the output channel to feed with data.
   * @return the output stream builder.
   */
  @NotNull
  public static ChunkOutputStreamBuilder withOutput(
      @NotNull final Channel<? super ParcelableByteChunk, ?> channel) {
    return ParcelableByteChannel.withOutput(channel);
  }

  /**
   * Returns a stream routine builder.
   *
   * @param <IN> the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStream() {
    return JRoutineStream.withStream();
  }

  /**
   * Returns a stream routine builder producing only the inputs passed by the specified consumer.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamAccept(
      @NotNull final Consumer<Channel<IN, ?>> consumer) {
    return JRoutineStream.withStreamAccept(consumer);
  }

  /**
   * Returns a stream routine builder producing only the inputs passed by the specified consumer.
   * <br>
   * The data will be produced by calling the consumer {@code count} number of times only when the
   * invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param count    the number of times the consumer is called.
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope or the specified count number is 0 or
   *                                            negative.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamAccept(final int count,
      @NotNull final Consumer<Channel<IN, ?>> consumer) {
    return JRoutineStream.withStreamAccept(count, consumer);
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified supplier.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamGet(@NotNull final Supplier<IN> supplier) {
    return JRoutineStream.withStreamGet(supplier);
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified supplier.
   * <br>
   * The data will be produced by calling the supplier {@code count} number of times only when the
   * invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param count    the number of times the supplier is called.
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope or the specified count number is 0 or
   *                                            negative.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamGet(final int count,
      @NotNull final Supplier<IN> supplier) {
    return JRoutineStream.withStreamGet(count, supplier);
  }

  /**
   * Returns a stream routine builder producing only the specified input.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param input the input.
   * @param <IN>  the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(@Nullable final IN input) {
    return JRoutineStream.withStreamOf(input);
  }

  /**
   * Returns a stream routine builder producing only the specified inputs.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param inputs the input data.
   * @param <IN>   the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(@Nullable final IN... inputs) {
    return JRoutineStream.withStreamOf(inputs);
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified iterable.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param inputs the inputs iterable.
   * @param <IN>   the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(
      @Nullable final Iterable<? extends IN> inputs) {
    return JRoutineStream.withStreamOf(inputs);
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified channel.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   * <p>
   * Note that the passed channel will be bound as a result of the call, so, in order to support
   * multiple invocations, consider wrapping the channel in a replayable one, by calling the
   * {@link Channels#replayOutput(Channel)} utility method.
   *
   * @param channel the input channel.
   * @param <IN>    the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(
      @Nullable final Channel<?, ? extends IN> channel) {
    return JRoutineStream.withStreamOf(channel);
  }
}
