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

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining a builder of routines backing a concatenation of mapping routines and
 * consumers.
 * <br>
 * Each function in the stream will be backed by a routine instance, which may have its own
 * specific configuration and invocation mode.
 * <br>
 * In order to prevent undesired leaks, the class of the specified functions, factories and routines
 * must have a static scope.
 * <p>
 * Note that, based on the routines which are part of the chain, the results might be propagated
 * only when the built routine invocation completes.
 * <p>
 * Created by davide-maestroni on 07/04/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface LoaderStreamBuilderCompat<IN, OUT>
    extends StreamBuilder<IN, OUT>, LoaderConfigurable<LoaderStreamBuilderCompat<IN, OUT>> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> apply(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyInvocationConfiguration();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> applyStream(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyStreamInvocationConfiguration();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> async();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> async(@Nullable Runner runner);

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> asyncParallel();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> asyncParallel(@Nullable Runner runner);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationFactory<IN, OUT> buildFactory();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> flatMap(
      @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> immediate();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> immediateParallel();

  /**
   * {@inheritDoc}
   * <p>
   * Note that the passed builder will be this one.
   * <br>
   * A {@code LoaderStreamBuilderCompat} is expected as the function result.
   */
  @NotNull
  <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> let(
      @NotNull Function<? super StreamBuilder<IN, OUT>, ? extends
          StreamBuilder<BEFORE, AFTER>> liftFunction);

  /**
   * {@inheritDoc}
   * <p>
   * Note that the passed configuration will be an instance of
   * {@code LoaderStreamConfigurationCompat} and the passed builder will be this one.
   * <br>
   * A {@code LoaderStreamBuilderCompat} is expected as the function result.
   */
  @NotNull

  @Override
  <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> letWithConfig(
      @NotNull BiFunction<? extends StreamConfiguration, ? super StreamBuilder<IN, OUT>, ?
          extends StreamBuilder<BEFORE, AFTER>> liftFunction);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> lift(
      @NotNull Function<? extends Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>, ?
          extends Function<? super Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

  /**
   * {@inheritDoc}
   * <p>
   * Note that the passed configuration will be an instance of
   * {@code LoaderStreamConfigurationCompat}.
   */
  @NotNull
  @Override
  <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> liftWithConfig(
      @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
          Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
          Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull Function<? super OUT, ? extends AFTER> mappingFunction);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull Routine<? super OUT, ? extends AFTER> routine);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAccept(
      @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAll(
      @NotNull Function<? super List<OUT>, ? extends AFTER> mappingFunction);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAllAccept(
      @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> mapOn(@Nullable Runner runner);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> sorted();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> sync();

  /**
   * {@inheritDoc}
   * <p>
   * Be aware that the configured runner will actually be ignored by the concatenated routines
   * unless no Loader context is currently set.
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> syncParallel();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderStreamBuilderCompat<IN, OUT> unsorted();

  /**
   * {@inheritDoc}
   */
  @NotNull
  LoaderConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyLoaderConfiguration();

  /**
   * Sets the specified configuration as the stream one.
   *
   * @param configuration the configuration.
   * @return this builder.
   */
  @NotNull
  LoaderStreamBuilderCompat<IN, OUT> applyStream(@NotNull LoaderConfiguration configuration);

  /**
   * Gets the Loader configuration builder related to the whole stream.
   * <br>
   * The configuration options will be applied to all the next concatenated routines unless
   * overwritten by specific ones.
   * <p>
   * Note that the configuration builder will be initialized with the current stream configuration.
   *
   * @return the invocation configuration builder.
   */
  @NotNull
  LoaderConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyStreamLoaderConfiguration();

  /**
   * Builds a new Context invocation factory instance.
   *
   * @return the factory instance.
   */
  @NotNull
  ContextInvocationFactory<IN, OUT> buildContextFactory();

  /**
   * Concatenates a routine mapping this stream outputs through the specified routine builder.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   *
   * @param builder the routine builder instance.
   * @param <AFTER> the concatenation output type.
   * @return this builder.
   */
  @NotNull
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder);

  /**
   * Concatenates a stream based on the specified mapping invocation factory to this one.
   * <p>
   * Note that the created routine will be initialized with the current configuration.
   * <br>
   * Note also that this stream will be bound as a result of the call.
   *
   * @param factory the Context invocation factory.
   * @param <AFTER> the concatenation output type.
   * @return the new stream instance.
   * @throws java.lang.IllegalStateException if the Loader context is not set.
   */
  @NotNull
  <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

  /**
   * Sets the stream Loader context.
   * <br>
   * The context will be used by all the concatenated routines until changed.
   * <br>
   * If null it will cause the next routines to employ the configured runner instead of an Android
   * Loader.
   *
   * @param context the Loader context.
   * @return the new stream instance.
   */
  @NotNull
  LoaderStreamBuilderCompat<IN, OUT> on(@Nullable LoaderContextCompat context);

  /**
   * Interface defining a Loader stream configuration.
   */
  interface LoaderStreamConfigurationCompat extends StreamConfiguration {

    /**
     * Gets the configuration that will override the stream one only for the next
     * concatenated routine.
     *
     * @return the Loader configuration.
     */
    @NotNull
    LoaderConfiguration getCurrentLoaderConfiguration();

    /**
     * Gets the stream Loader context.
     *
     * @return the Loader context.
     */
    @Nullable
    LoaderContextCompat getLoaderContext();

    /**
     * Gets the configuration that will be applied to all the concatenated routines.
     *
     * @return the Loader configuration.
     */
    @NotNull
    LoaderConfiguration getStreamLoaderConfiguration();

    /**
     * Gets the combination of stream and current configuration as a Loader one.
     *
     * @return the Loader configuration.
     */
    @NotNull
    LoaderConfiguration toLoaderConfiguration();
  }
}
