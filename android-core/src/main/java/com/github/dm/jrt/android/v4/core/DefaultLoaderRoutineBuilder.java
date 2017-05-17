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

package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationDecorator;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.builder.AbstractRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDecorator;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryFrom;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a Loader routine builder.
 * <p>
 * Created by davide-maestroni on 12/09/2014.
 */
class DefaultLoaderRoutineBuilder extends AbstractRoutineBuilder implements LoaderRoutineBuilder {

  private final LoaderSourceCompat mLoaderSource;

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultLoaderRoutineBuilder(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> of(@NotNull final InvocationFactory<IN, OUT> factory) {
    final Class<? extends InvocationFactory> factoryClass = factory.getClass();
    if (!Reflection.hasStaticScope(factoryClass)) {
      throw new IllegalArgumentException(
          "the factory class must have a static scope: " + factoryClass.getName());
    }

    return new DefaultLoaderRoutine<IN, OUT>(mLoaderSource, factoryFrom(factory),
        getConfiguration(), mLoaderConfiguration);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> ofSingleton(
      @NotNull final Invocation<IN, OUT> invocation) {
    return of(new SingletonInvocationFactory<IN, OUT>(invocation));
  }

  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> of(
      @NotNull final ContextInvocationFactory<IN, OUT> factory) {
    final Class<? extends ContextInvocationFactory> factoryClass = factory.getClass();
    if (!Reflection.hasStaticScope(factoryClass)) {
      throw new IllegalArgumentException(
          "the factory class must have a static scope: " + factoryClass.getName());
    }

    return new DefaultLoaderRoutine<IN, OUT>(mLoaderSource, factory, getConfiguration(),
        mLoaderConfiguration);
  }

  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> ofSingleton(
      @NotNull final ContextInvocation<IN, OUT> invocation) {
    return of(new SingletonContextInvocationFactory<IN, OUT>(invocation));
  }

  @NotNull
  @Override
  public LoaderRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    super.withConfiguration(configuration);
    return this;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends LoaderRoutineBuilder> withInvocation() {
    return (Builder<? extends LoaderRoutineBuilder>) super.withInvocation();
  }

  @NotNull
  @Override
  public LoaderRoutineBuilder withConfiguration(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderRoutineBuilder> withLoader() {
    final LoaderConfiguration config = mLoaderConfiguration;
    return new LoaderConfiguration.Builder<LoaderRoutineBuilder>(this, config);
  }

  /**
   * Context invocation decorator ensuring that a destroyed instance will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonContextInvocation<IN, OUT>
      extends ContextInvocationDecorator<IN, OUT> {

    private boolean mIsDestroyed;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonContextInvocation(@NotNull final ContextInvocation<IN, OUT> wrapped) {
      super(wrapped);
    }

    @Override
    public void onDestroy() throws Exception {
      mIsDestroyed = true;
      super.onDestroy();
    }

    @Override
    public void onStart() throws Exception {
      if (mIsDestroyed) {
        throw new IllegalStateException("the invocation has been destroyed");
      }

      super.onStart();
    }
  }

  /**
   * Factory wrapping an invocation instance so that, once destroyed, it will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonContextInvocationFactory<IN, OUT>
      extends ContextInvocationFactory<IN, OUT> {

    private final SingletonContextInvocation<IN, OUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonContextInvocationFactory(@NotNull final ContextInvocation<IN, OUT> wrapped) {
      super(asArgs(wrapped));
      mInvocation = new SingletonContextInvocation<IN, OUT>(wrapped);
    }

    @NotNull
    public ContextInvocation<IN, OUT> newInvocation() {
      return mInvocation;
    }
  }

  /**
   * Invocation decorator ensuring that a destroyed instance will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonInvocation<IN, OUT> extends InvocationDecorator<IN, OUT> {

    private boolean mIsDestroyed;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonInvocation(@NotNull final Invocation<IN, OUT> wrapped) {
      super(wrapped);
    }

    @Override
    public void onDestroy() throws Exception {
      mIsDestroyed = true;
      super.onDestroy();
    }

    @Override
    public void onStart() throws Exception {
      if (mIsDestroyed) {
        throw new IllegalStateException("the invocation has been destroyed");
      }

      super.onStart();
    }
  }

  /**
   * Factory wrapping an invocation instance so that, once destroyed, it will not be reused.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SingletonInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final SingletonInvocation<IN, OUT> mInvocation;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    private SingletonInvocationFactory(@NotNull final Invocation<IN, OUT> wrapped) {
      super(asArgs(wrapped));
      mInvocation = new SingletonInvocation<IN, OUT>(wrapped);
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return mInvocation;
    }
  }
}
