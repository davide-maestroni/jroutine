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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkInputStream;
import com.github.dm.jrt.channel.io.ChunkOutputStreamBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.reflect.InvocationTarget;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;
import static com.github.dm.jrt.function.Functions.supplierFactory;
import static com.github.dm.jrt.reflect.InvocationTarget.classOfType;
import static com.github.dm.jrt.reflect.InvocationTarget.instance;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutine extends Channels {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutine() {
    ConstantConditions.avoid();
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
  public static ChunkInputStream getInputStream(@NotNull final ByteChunk... buffers) {
    return ByteChannel.getInputStream(buffers);
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
  public static ChunkInputStream getInputStream(
      @NotNull final Iterable<? extends ByteChunk> buffers) {
    return ByteChannel.getInputStream(buffers);
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
  public static ChunkInputStream getInputStream(@NotNull final ByteChunk buffer) {
    return ByteChannel.getInputStream(buffer);
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
    return JRoutineCore.of();
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
    return JRoutineCore.of(output);
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
    return JRoutineCore.of(outputs);
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
    return JRoutineCore.of(outputs);
  }

  /**
   * Returns a channel builder.
   *
   * @param <DATA> the data type.
   * @return the channel builder instance.
   */
  @NotNull
  public static <DATA> ChannelBuilder<DATA, DATA> ofInputs() {
    return JRoutineCore.ofInputs();
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class.
   *
   * @param invocationClass the invocation class.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no default constructor was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) {
    return with(factoryOf(invocationClass));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class by passing the specified arguments to the class constructor.
   * <p>
   * Note that inner and anonymous classes can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
   * first constructor parameter, and anonymous classes have both the outer instance and all the
   * variables captured in the closure.
   *
   * @param invocationClass the invocation class.
   * @param args            the invocation constructor arguments.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
   *                                            parameters was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
      @Nullable final Object... args) {
    return with(factoryOf(invocationClass, args));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class token.
   *
   * @param invocationToken the invocation class token.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no default constructor was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken) {
    return with(factoryOf(invocationToken));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class token by passing the specified arguments to the class constructor.
   * <p>
   * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
   * that Java creates synthetic constructors for such classes, so be sure to specify the correct
   * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
   * instance as first constructor parameter, and anonymous classes have both the outer instance
   * and all the variables captured in the closure.
   *
   * @param invocationToken the invocation class token.
   * @param args            the invocation constructor arguments.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
   *                                            parameters was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken,
      @Nullable final Object... args) {
    return with(factoryOf(invocationToken, args));
  }

  /**
   * Returns a routine builder based on the specified command invocation.
   *
   * @param invocation the command invocation instance.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <OUT> RoutineBuilder<Void, OUT> with(
      @NotNull final CommandInvocation<OUT> invocation) {
    return with((InvocationFactory<Void, OUT>) invocation);
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * object.
   *
   * @param invocation the invocation instance.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no default constructor was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final Invocation<IN, OUT> invocation) {
    return with(factoryOf(invocation));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * object by passing the specified arguments to the class constructor.
   * <p>
   * Note that inner and anonymous objects can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
   * first constructor parameter, and anonymous classes have both the outer instance and all the
   * variables captured in the closure.
   *
   * @param invocation the invocation instance.
   * @param args       the invocation constructor arguments.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
   *                                            parameters was found.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final Invocation<IN, OUT> invocation, @Nullable final Object... args) {
    return with(factoryOf(invocation, args));
  }

  /**
   * Returns a routine builder based on the specified invocation factory.
   * <br>
   * In order to prevent undesired leaks, the class of the specified factory should have a static
   * scope.
   *
   * @param factory the invocation factory.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final InvocationFactory<IN, OUT> factory) {
    return JRoutineCore.with(factory);
  }

  /**
   * Returns a routine builder wrapping the specified target.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors.
   *
   * @param target the invocation target.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the specified object class represents an
   *                                            interface.
   */
  @NotNull
  public static ReflectionProxyRoutineBuilder with(@NotNull final InvocationTarget<?> target) {
    return new DefaultReflectionProxyRoutineBuilder(target);
  }

  /**
   * Returns a routine builder based on the specified mapping invocation.
   *
   * @param invocation the mapping invocation instance.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> with(
      @NotNull final MappingInvocation<IN, OUT> invocation) {
    return with((InvocationFactory<IN, OUT>) invocation);
  }

  /**
   * Returns a routine builder wrapping the specified object.
   * <br>
   * The invocation target will be automatically chosen based on whether the specified object is
   * a class or an instance.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors.
   *
   * @param object the target object.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the specified object class represents an
   *                                            interface.
   */
  @NotNull
  public static ReflectionProxyRoutineBuilder with(@NotNull final Object object) {
    return (object instanceof Class) ? withClassOfType((Class<?>) object) : withInstance(object);
  }

  /**
   * Returns a routine builder based on a call invocation factory backed by the specified function.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> withCall(
      @NotNull final Function<? super List<IN>, ? extends OUT> function) {
    return with(functionCall(function));
  }

  /**
   * Returns a routine builder based on a call invocation factory backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> withCallConsumer(
      @NotNull final BiConsumer<? super List<IN>, ? super Channel<OUT, ?>> consumer) {
    return with(consumerCall(consumer));
  }

  /**
   * Returns a routine builder wrapping the specified class.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors.
   *
   * @param targetClass the target class.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the specified class represents an interface.
   */
  @NotNull
  public static ReflectionProxyRoutineBuilder withClassOfType(@NotNull final Class<?> targetClass) {
    return with(classOfType(targetClass));
  }

  /**
   * Returns a routine builder based on a command invocation backed by the specified supplier.
   *
   * @param supplier the supplier instance.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <OUT> RoutineBuilder<Void, OUT> withCommand(
      @NotNull final Supplier<? extends OUT> supplier) {
    return with(supplierCommand(supplier));
  }

  /**
   * Returns a routine builder based on a command invocation backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <OUT> RoutineBuilder<Void, OUT> withCommandConsumer(
      @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
    return with(consumerCommand(consumer));
  }

  /**
   * Returns a routine builder based on an invocation factory backed by the specified supplier.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> withFactory(
      @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {
    return with(supplierFactory(supplier));
  }

  /**
   * Returns a routine builder based on a operation invocation backed by the specified predicate.
   *
   * @param predicate the predicate instance.
   * @param <IN>      the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> RoutineBuilder<IN, IN> withFilter(
      @NotNull final Predicate<? super IN> predicate) {
    return with(predicateFilter(predicate));
  }

  /**
   * Returns a routine builder wrapping the specified object.
   * <p>
   * Note that it is responsibility of the caller to retain a strong reference to the target
   * instance to prevent it from being garbage collected.
   * <br>
   * Note also that the invocation input data will be cached, and the results will be produced
   * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
   * prevent starvation or out of memory errors.
   *
   * @param object the target object.
   * @return the routine builder instance.
   */
  @NotNull
  public static ReflectionProxyRoutineBuilder withInstance(@NotNull final Object object) {
    return with(instance(object));
  }

  /**
   * Returns a routine builder based on a mapping invocation backed by the specified function.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> withMapping(
      @NotNull final Function<? super IN, ? extends OUT> function) {
    return with(functionMapping(function));
  }

  /**
   * Returns a routine builder based on a mapping invocation backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN, OUT> RoutineBuilder<IN, OUT> withMappingConsumer(
      @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> consumer) {
    return with(consumerMapping(consumer));
  }

  /**
   * Returns a builder of buffer output streams.
   * <p>
   * The built streams will not close the underlying channel by default.
   *
   * @param channel the output channel to feed with data.
   * @return the output stream builder.
   */
  @NotNull
  public static ChunkOutputStreamBuilder withOutput(
      @NotNull final Channel<? super ByteChunk, ?> channel) {
    return ByteChannel.withOutput(channel);
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
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
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
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
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
