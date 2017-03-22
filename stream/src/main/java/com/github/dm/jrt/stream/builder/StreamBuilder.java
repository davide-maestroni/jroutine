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

package com.github.dm.jrt.stream.builder;

import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining a builder of routines backing a concatenation of mapping routines and
 * consumers.
 * <br>
 * Each routine in the stream may have its own specific configuration and invocation mode.
 * <p>
 * Note that, based on the routines which are part of the chain, the results might be propagated
 * only when the built routine invocation completes.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamBuilder<IN, OUT> extends RoutineBuilder<IN, OUT> {

  /**
   * {@inheritDoc}
   * <p>
   * The specified configuration will be applied only to the built routine implementing the whole
   * stream, and will override the stream one.
   *
   * @see #buildRoutine()
   */
  @NotNull
  StreamBuilder<IN, OUT> apply(@NotNull InvocationConfiguration configuration);

  /**
   * Returns a stream builder where the concatenated routines will be invoked in asynchronous mode
   * employing the shared asynchronous runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> async();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in asynchronous mode
   * employing the specified runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @param runner the runner instance.
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> async(@Nullable Runner runner);

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared asynchronous runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> asyncParallel();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared asynchronous runner, and the maximum allowed number of concurrent
   * invocations is the specified one.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @param maxInvocations the maximum number of invocations.
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> asyncParallel(int maxInvocations);

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the specified runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @param runner the runner instance.
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> asyncParallel(@Nullable Runner runner);

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the specified runner, and the maximum allowed number of concurrent invocations is the
   * specified one.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @param maxInvocations the maximum number of invocations.
   * @param runner         the runner instance.
   * @return the new builder.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  StreamBuilder<IN, OUT> asyncParallel(@Nullable Runner runner, int maxInvocations);

  /**
   * Builds a new invocation factory instance.
   *
   * @return the factory instance.
   */
  @NotNull
  InvocationFactory<IN, OUT> buildFactory();

  /**
   * {@inheritDoc}
   * <p>
   * Note that the stream configuration will be employed to build the routine instance.
   */
  @NotNull
  Routine<IN, OUT> buildRoutine();

  /**
   * Concatenates a routine dispatching this stream outputs through the specified runner.
   *
   * @param runner the runner instance.
   * @return the new builder.
   */
  @NotNull
  StreamBuilder<IN, OUT> consumeOn(@Nullable Runner runner);

  /**
   * Transforms this stream by applying the specified function.
   * <br>
   * The current configuration of the stream will be passed as the first parameter.
   * <p>
   * This method provides a convenient way to apply a set of configurations and concatenations
   * without breaking the fluent chain.
   *
   * @param transformingFunction the function modifying the stream.
   * @param <BEFORE>             the input type after the conversion.
   * @param <AFTER>              the output type after the conversion.
   * @return the converted builder.
   * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
   *                                                                  occurred.
   */
  @NotNull
  <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> convert(
      @NotNull BiFunction<? super com.github.dm.jrt.stream.config.StreamConfiguration, ? super
          StreamBuilder<IN, OUT>, ? extends StreamBuilder<BEFORE, AFTER>> transformingFunction);

  /**
   * Concatenates a routine mapping this stream outputs by applying the specified function to each
   * one of them.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param mappingFunction the function instance.
   * @param <AFTER>         the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> flatMap(
      @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

  /**
   * Returns a stream builder where the concatenated routines will be invoked in asynchronous mode
   * employing the shared immediate runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   * <p>
   * Unlike the default synchronous runner, the employed one makes so that each routine in the
   * chain is passed any input as soon as it is produced by the previous one. Such behavior
   * decreases memory demands at the expense of a deeper stack of calls. In fact, the default
   * synchronous runner breaks up routine calls so to perform them in a loop. The main drawback of
   * the latter approach is that all input data might be accumulated before actually being
   * processed by the next routine invocation.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#immediateRunner() Runners.immediateRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> immediate();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared immediate runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   * <p>
   * Unlike the default synchronous runner, the employed one makes so that each routine in the
   * chain is passed any input as soon as it is produced by the previous one. Such behavior
   * decreases memory demands at the expense of a deeper stack of calls. In fact, the default
   * synchronous runner breaks up routine calls so to perform them in a loop. The main drawback of
   * the latter approach is that all input data might be accumulated before actually being
   * processed by the next routine invocation.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#immediateRunner() Runners.immediateRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> immediateParallel();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared immediate runner, and the maximum allowed number of concurrent
   * invocations is the specified one.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   * <p>
   * Unlike the default synchronous runner, the employed one makes so that each routine in the
   * chain is passed any input as soon as it is produced by the previous one. Such behavior
   * decreases memory demands at the expense of a deeper stack of calls. In fact, the default
   * synchronous runner breaks up routine calls so to perform them in a loop. The main drawback of
   * the latter approach is that all input data might be accumulated before actually being
   * processed by the next routine invocation.
   *
   * @param maxInvocations the maximum number of invocations.
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#immediateRunner() Runners.immediateRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> immediateParallel(int maxInvocations);

  /**
   * {@inheritDoc}
   * <p>
   * The specified configuration will be applied only to the built routine implementing the whole
   * stream, and will override the stream one.
   *
   * @see #buildRoutine()
   */
  @NotNull
  Builder<? extends StreamBuilder<IN, OUT>> invocationConfiguration();

  /**
   * Transforms the stream by modifying the chain building function.
   * <br>
   * The current configuration of the stream will be passed as the first parameter.
   * <br>
   * The returned function will be employed when the routine instance is built (see
   * {@link #buildRoutine()}).
   *
   * @param liftingFunction the bi-function modifying the chain building one.
   * @param <BEFORE>        the input type after the lifting.
   * @param <AFTER>         the output type after the lifting.
   * @return the lifted builder.
   * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
   *                                                                  occurred.
   */
  @NotNull
  <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> lift(
      @NotNull BiFunction<? super com.github.dm.jrt.stream.config.StreamConfiguration, ? super
          Function<Channel<?, IN>, Channel<?, OUT>>, ? extends Function<? super Channel<?,
          BEFORE>, ? extends Channel<?, AFTER>>> liftingFunction);

  /**
   * Concatenates a routine mapping this stream outputs by applying the specified function.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param mappingFunction the function instance.
   * @param <AFTER>         the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull Function<? super OUT, ? extends AFTER> mappingFunction);

  /**
   * Concatenates a routine mapping this stream outputs through the specified invocation factory.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param factory the invocation factory.
   * @param <AFTER> the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

  /**
   * Concatenates a routine mapping this stream outputs through the specified routine.
   * <p>
   * Note that the stream configuration will be ignored.
   *
   * @param routine the routine instance.
   * @param <AFTER> the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

  /**
   * Concatenates a routine mapping this stream outputs through the specified routine builder.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param builder the routine builder instance.
   * @param <AFTER> the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> map(
      @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

  /**
   * Concatenates a routine mapping this stream outputs through the specified consumer.
   * <br>
   * The result channel of the backing routine will be passed to the consumer, so that multiple
   * or no results may be generated.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param mappingConsumer the bi-consumer instance.
   * @param <AFTER>         the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> mapAccept(
      @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

  /**
   * Concatenates a routine mapping the whole collection of outputs by applying the specified
   * function.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param mappingFunction the function instance.
   * @param <AFTER>         the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> mapAll(
      @NotNull Function<? super List<OUT>, ? extends AFTER> mappingFunction);

  /**
   * Concatenates a routine mapping the whole collection of outputs through the specified consumer.
   * <br>
   * The result channel of the backing routine will be passed to the consumer, so that multiple
   * or no results may be generated.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param mappingConsumer the bi-consumer instance.
   * @param <AFTER>         the concatenation output type.
   * @return the new builder.
   */
  @NotNull
  <AFTER> StreamBuilder<IN, AFTER> mapAllAccept(
      @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

  /**
   * Returns a stream builder with the specified configuration overriding the stream one.
   *
   * @param configuration the configuration.
   * @return the new builder.
   */
  @NotNull
  StreamBuilder<IN, OUT> nextApply(@NotNull InvocationConfiguration configuration);

  /**
   * Gets the invocation configuration builder related to the next concatenated routine.
   * <p>
   * Note that the configuration builder will be initialized with the current stream
   * configuration.
   *
   * @return the invocation configuration builder.
   */
  @NotNull
  Builder<? extends StreamBuilder<IN, OUT>> nextInvocationConfiguration();

  /**
   * Returns a stream builder sorting its outputs by the order they are passed to the result
   * channel.
   * <br>
   * Note, however, that the current configuration option will still be override the stream one.
   *
   * @return the new builder.
   */
  @NotNull
  StreamBuilder<IN, OUT> sorted();

  /**
   * Returns a stream builder with the specified configuration as the stream one.
   *
   * @param configuration the configuration.
   * @return the new builder.
   */
  @NotNull
  StreamBuilder<IN, OUT> streamApply(@NotNull InvocationConfiguration configuration);

  /**
   * Gets the invocation configuration builder related to the whole stream.
   * <br>
   * The configuration options will be applied to all the next concatenated routine unless
   * overwritten by specific ones.
   * <p>
   * Note that the configuration builder will be initialized with the current stream
   * configuration.
   *
   * @return the invocation configuration builder.
   * @see #nextInvocationConfiguration()
   */
  @NotNull
  Builder<? extends StreamBuilder<IN, OUT>> streamInvocationConfiguration();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in asynchronous mode
   * employing the shared synchronous runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#syncRunner() Runners.syncRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> sync();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared asynchronous runner.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#syncRunner() Runners.syncRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> syncParallel();

  /**
   * Returns a stream builder where the concatenated routines will be invoked in parallel mode
   * employing the shared asynchronous runner, and the maximum allowed number of concurrent
   * invocations is the specified one.
   * <br>
   * Note, however, that the current configuration runner will still override the stream one.
   *
   * @param maxInvocations the maximum number of invocations.
   * @return the new builder.
   * @see com.github.dm.jrt.core.runner.Runners#syncRunner() Runners.syncRunner()
   */
  @NotNull
  StreamBuilder<IN, OUT> syncParallel(int maxInvocations);

  /**
   * Returns a stream builder not sorting its outputs.
   * <br>
   * Note, however, that the current configuration option will still be override the stream one.
   *
   * @return the new builder.
   */
  @NotNull
  StreamBuilder<IN, OUT> unsorted();
}
