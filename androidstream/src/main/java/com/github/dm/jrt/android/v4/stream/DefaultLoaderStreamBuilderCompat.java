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
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Decorator;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.AbstractStreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Default implementation of a stream Loader routine builder.
 * <p>
 * Created by davide-maestroni on 07/04/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamBuilderCompat<IN, OUT> extends AbstractStreamBuilder<IN, OUT>
    implements LoaderStreamBuilderCompat<IN, OUT> {

  private LoaderStreamConfigurationCompat mStreamConfiguration;

  /**
   * Constructor.
   */
  DefaultLoaderStreamBuilderCompat() {
    this(
        new DefaultLoaderStreamConfigurationCompat(null, LoaderConfiguration.defaultConfiguration(),
            LoaderConfiguration.defaultConfiguration(),
            InvocationConfiguration.defaultConfiguration(),
            InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC));
  }

  /**
   * Constructor.
   *
   * @param streamConfiguration the stream configuration.
   */
  private DefaultLoaderStreamBuilderCompat(
      @NotNull final LoaderStreamConfigurationCompat streamConfiguration) {
    super(streamConfiguration);
    mStreamConfiguration = streamConfiguration;
  }

  private static void checkStatic(@NotNull final String name, @NotNull final Object obj) {
    if (!Reflection.hasStaticScope(obj)) {
      throw new IllegalArgumentException(
          "the " + name + " instance does not have a static scope: " + obj.getClass().getName());
    }
  }

  private static void checkStatic(@NotNull final Decorator decorator,
      @NotNull final Object function) {
    if (!decorator.hasStaticScope()) {
      throw new IllegalArgumentException(
          "the function instance does not have a static scope: " + function.getClass().getName());
    }
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> apply(
      @NotNull final InvocationConfiguration configuration) {
    final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
    return apply(
        newConfiguration(streamConfiguration.getStreamInvocationConfiguration(), configuration,
            streamConfiguration.getInvocationMode()));
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyInvocationConfiguration() {
    return (Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>) super
        .applyInvocationConfiguration();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> applyStream(
      @NotNull final InvocationConfiguration configuration) {
    final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
    return apply(
        newConfiguration(configuration, streamConfiguration.getCurrentInvocationConfiguration(),
            streamConfiguration.getInvocationMode()));
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyStreamInvocationConfiguration() {
    return new InvocationConfiguration.Builder<LoaderStreamBuilderCompat<IN, OUT>>(
        new InvocationConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

          @NotNull
          @Override
          public LoaderStreamBuilderCompat<IN, OUT> apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderStreamBuilderCompat.this.applyStream(configuration);
          }
        }, mStreamConfiguration.getStreamInvocationConfiguration());
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> async() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.async();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> async(@Nullable final Runner runner) {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.async(runner);
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> asyncParallel() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.asyncParallel();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> asyncParallel(@Nullable final Runner runner) {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.asyncParallel(runner);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> flatMap(
      @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction) {
    checkStatic(decorate(mappingFunction), mappingFunction);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.flatMap(mappingFunction);
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> immediate() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.immediate();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> immediateParallel() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.immediateParallel();
  }

  @NotNull
  @Override
  public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> let(
      @NotNull final Function<? super StreamBuilder<IN, OUT>, ? extends
          StreamBuilder<BEFORE, AFTER>> liftFunction) {
    checkStatic(decorate(liftFunction), liftFunction);
    return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.let(liftFunction);
  }

  @NotNull
  @Override
  public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> letWithConfig(
      @NotNull final BiFunction<? extends StreamConfiguration, ? super StreamBuilder<IN, OUT>, ?
          extends StreamBuilder<BEFORE, AFTER>> liftFunction) {
    checkStatic(decorate(liftFunction), liftFunction);
    return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.letWithConfig(liftFunction);
  }

  @NotNull
  @Override
  public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> lift(
      @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
          Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
          Channel<?, AFTER>>> liftFunction) {
    checkStatic(decorate(liftFunction), liftFunction);
    return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.lift(liftFunction);
  }

  @NotNull
  @Override
  public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> liftWithConfig(
      @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
          Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
          Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
    checkStatic(decorate(liftFunction), liftFunction);
    return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.liftWithConfig(liftFunction);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
    checkStatic(decorate(mappingFunction), mappingFunction);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(mappingFunction);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
    checkStatic("factory", factory);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(factory);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    checkStatic("routine", routine);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(routine);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(builder);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAccept(
      @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
    checkStatic(decorate(mappingConsumer), mappingConsumer);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapAccept(mappingConsumer);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAll(
      @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
    checkStatic(decorate(mappingFunction), mappingFunction);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapAll(mappingFunction);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAllAccept(
      @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer) {
    checkStatic(decorate(mappingConsumer), mappingConsumer);
    return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapAllAccept(mappingConsumer);
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> mapOn(@Nullable final Runner runner) {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.mapOn(runner);
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> sorted() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.sorted();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> sync() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.sync();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> syncParallel() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.syncParallel();
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> unsorted() {
    return (LoaderStreamBuilderCompat<IN, OUT>) super.unsorted();
  }

  @NotNull
  protected LoaderStreamBuilderCompat<IN, OUT> apply(
      @NotNull final StreamConfiguration configuration) {
    super.apply(configuration);
    mStreamConfiguration = (LoaderStreamConfigurationCompat) configuration;
    return this;
  }

  @Override
  protected boolean canOptimizeBinding() {
    return (mStreamConfiguration.getLoaderContext() == null) && super.canOptimizeBinding();
  }

  @NotNull
  @Override
  protected LoaderStreamConfigurationCompat newConfiguration(
      @NotNull final InvocationConfiguration streamConfiguration,
      @NotNull final InvocationConfiguration currentConfiguration,
      @NotNull final InvocationMode invocationMode) {
    final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
    return new DefaultLoaderStreamConfigurationCompat(loaderStreamConfiguration.getLoaderContext(),
        loaderStreamConfiguration.getStreamLoaderConfiguration(),
        loaderStreamConfiguration.getCurrentLoaderConfiguration(), streamConfiguration,
        currentConfiguration, invocationMode);
  }

  @NotNull
  @Override
  protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
      @NotNull final StreamConfiguration streamConfiguration,
      @NotNull final InvocationFactory<? super BEFORE, ? extends AFTER> factory) {
    final LoaderStreamConfigurationCompat loaderStreamConfiguration =
        (LoaderStreamConfigurationCompat) streamConfiguration;
    final LoaderContextCompat loaderContext = loaderStreamConfiguration.getLoaderContext();
    if (loaderContext == null) {
      return JRoutineCore.with(factory)
                         .apply(loaderStreamConfiguration.toInvocationConfiguration())
                         .buildRoutine();
    }

    final ContextInvocationFactory<? super BEFORE, ? extends AFTER> invocationFactory = factoryFrom(
        JRoutineCore.with(factory)
                    .applyInvocationConfiguration()
                    .withRunner(Runners.syncRunner())
                    .configured()
                    .buildRoutine(), factory.hashCode(), InvocationMode.ASYNC);
    return JRoutineLoaderCompat.on(loaderContext)
                               .with(invocationFactory)
                               .apply(loaderStreamConfiguration.toInvocationConfiguration())
                               .apply(loaderStreamConfiguration.toLoaderConfiguration())
                               .buildRoutine();
  }

  @NotNull
  @Override
  protected LoaderStreamConfigurationCompat resetConfiguration(
      @NotNull final InvocationConfiguration streamConfiguration,
      @NotNull final InvocationMode invocationMode) {
    final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
    return new DefaultLoaderStreamConfigurationCompat(loaderStreamConfiguration.getLoaderContext(),
        loaderStreamConfiguration.getStreamLoaderConfiguration(),
        LoaderConfiguration.defaultConfiguration(), streamConfiguration,
        InvocationConfiguration.defaultConfiguration(), invocationMode);
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> apply(
      @NotNull final LoaderConfiguration configuration) {
    return apply(
        newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(), configuration));
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyLoaderConfiguration() {
    return new LoaderConfiguration.Builder<LoaderStreamBuilderCompat<IN, OUT>>(
        new LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

          @NotNull
          @Override
          public LoaderStreamBuilderCompat<IN, OUT> apply(
              @NotNull final LoaderConfiguration configuration) {
            return DefaultLoaderStreamBuilderCompat.this.apply(configuration);
          }
        }, mStreamConfiguration.getCurrentLoaderConfiguration());
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> applyStream(
      @NotNull final LoaderConfiguration configuration) {
    return apply(
        newConfiguration(configuration, mStreamConfiguration.getCurrentLoaderConfiguration()));
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
  applyStreamLoaderConfiguration() {
    return new LoaderConfiguration.Builder<LoaderStreamBuilderCompat<IN, OUT>>(
        new LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

          @NotNull
          @Override
          public LoaderStreamBuilderCompat<IN, OUT> apply(
              @NotNull final LoaderConfiguration configuration) {
            return DefaultLoaderStreamBuilderCompat.this.applyStream(configuration);
          }
        }, mStreamConfiguration.getStreamLoaderConfiguration());
  }

  @NotNull
  @Override
  public ContextInvocationFactory<IN, OUT> buildContextFactory() {
    final InvocationFactory<IN, OUT> factory = buildFactory();
    return factoryFrom(JRoutineCore.with(factory)
                                   .applyInvocationConfiguration()
                                   .withRunner(Runners.syncRunner())
                                   .configured()
                                   .buildRoutine(), factory.hashCode(), InvocationMode.ASYNC);
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
    return map(buildRoutine(builder));
  }

  @NotNull
  @Override
  public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
      @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
    final LoaderContextCompat loaderContext = mStreamConfiguration.getLoaderContext();
    if (loaderContext == null) {
      throw new IllegalStateException("the Loader context is null");
    }

    return map(JRoutineLoaderCompat.on(loaderContext).with(factory));
  }

  @NotNull
  @Override
  public LoaderStreamBuilderCompat<IN, OUT> on(@Nullable final LoaderContextCompat context) {
    return apply(newConfiguration(context));
  }

  @NotNull
  private <AFTER> LoaderRoutine<? super OUT, ? extends AFTER> buildRoutine(
      @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
    final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
    return builder.apply(streamConfiguration.toInvocationConfiguration())
                  .apply(streamConfiguration.toLoaderConfiguration())
                  .buildRoutine();
  }

  @NotNull
  private LoaderStreamConfigurationCompat newConfiguration(
      @Nullable final LoaderContextCompat context) {
    final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
    return new DefaultLoaderStreamConfigurationCompat(context,
        loaderStreamConfiguration.getStreamLoaderConfiguration(),
        loaderStreamConfiguration.getCurrentLoaderConfiguration(),
        loaderStreamConfiguration.getStreamInvocationConfiguration(),
        loaderStreamConfiguration.getCurrentInvocationConfiguration(),
        loaderStreamConfiguration.getInvocationMode());
  }

  @NotNull
  private LoaderStreamConfigurationCompat newConfiguration(
      @NotNull final LoaderConfiguration streamConfiguration,
      @NotNull final LoaderConfiguration configuration) {
    final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
    return new DefaultLoaderStreamConfigurationCompat(loaderStreamConfiguration.getLoaderContext(),
        streamConfiguration, configuration,
        loaderStreamConfiguration.getStreamInvocationConfiguration(),
        loaderStreamConfiguration.getCurrentInvocationConfiguration(),
        loaderStreamConfiguration.getInvocationMode());
  }

  /**
   * Default implementation of a Loader stream configuration.
   */
  private static class DefaultLoaderStreamConfigurationCompat
      implements LoaderStreamConfigurationCompat {

    private final InvocationConfiguration mCurrentConfiguration;

    private final LoaderConfiguration mCurrentLoaderConfiguration;

    private final InvocationMode mInvocationMode;

    private final LoaderContextCompat mLoaderContext;

    private final InvocationConfiguration mStreamConfiguration;

    private final LoaderConfiguration mStreamLoaderConfiguration;

    private volatile ChannelConfiguration mChannelConfiguration;

    private volatile InvocationConfiguration mInvocationConfiguration;

    private volatile LoaderConfiguration mLoaderConfiguration;

    /**
     * Constructor.
     *
     * @param context                    the Loader context.
     * @param streamLoaderConfiguration  the stream Loader configuration.
     * @param currentLoaderConfiguration the current Loader configuration.
     * @param streamConfiguration        the stream invocation configuration.
     * @param currentConfiguration       the current invocation configuration.
     * @param invocationMode             the invocation mode.
     */
    private DefaultLoaderStreamConfigurationCompat(@Nullable final LoaderContextCompat context,
        @NotNull final LoaderConfiguration streamLoaderConfiguration,
        @NotNull final LoaderConfiguration currentLoaderConfiguration,
        @NotNull final InvocationConfiguration streamConfiguration,
        @NotNull final InvocationConfiguration currentConfiguration,
        @NotNull final InvocationMode invocationMode) {
      mLoaderContext = context;
      mStreamLoaderConfiguration =
          ConstantConditions.notNull("stream Loader configuration", streamLoaderConfiguration);
      mCurrentLoaderConfiguration =
          ConstantConditions.notNull("current Loader configuration", currentLoaderConfiguration);
      mStreamConfiguration =
          ConstantConditions.notNull("stream invocation configuration", streamConfiguration);
      mCurrentConfiguration =
          ConstantConditions.notNull("current invocation configuration", currentConfiguration);
      mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
    }

    @NotNull
    @Override
    public InvocationConfiguration getCurrentInvocationConfiguration() {
      return mCurrentConfiguration;
    }

    @NotNull
    @Override
    public InvocationMode getInvocationMode() {
      return mInvocationMode;
    }

    @NotNull
    @Override
    public InvocationConfiguration getStreamInvocationConfiguration() {
      return mStreamConfiguration;
    }

    @NotNull
    public ChannelConfiguration toChannelConfiguration() {
      if (mChannelConfiguration == null) {
        mChannelConfiguration =
            toInvocationConfiguration().outputConfigurationBuilder().configured();
      }

      return mChannelConfiguration;
    }

    @NotNull
    public InvocationConfiguration toInvocationConfiguration() {
      if (mInvocationConfiguration == null) {
        mInvocationConfiguration =
            mStreamConfiguration.builderFrom().with(mCurrentConfiguration).configured();
      }

      return mInvocationConfiguration;
    }

    @NotNull
    @Override
    public LoaderConfiguration getCurrentLoaderConfiguration() {
      return mCurrentLoaderConfiguration;
    }

    @Nullable
    @Override
    public LoaderContextCompat getLoaderContext() {
      return mLoaderContext;
    }

    @NotNull
    @Override
    public LoaderConfiguration getStreamLoaderConfiguration() {
      return mStreamLoaderConfiguration;
    }

    @NotNull
    @Override
    public LoaderConfiguration toLoaderConfiguration() {
      if (mLoaderConfiguration == null) {
        mLoaderConfiguration =
            mStreamLoaderConfiguration.builderFrom().with(mCurrentLoaderConfiguration).configured();
      }

      return mLoaderConfiguration;
    }
  }
}
