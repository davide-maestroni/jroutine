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

package com.github.dm.jrt.android.v4.retrofit;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.reflect.util.ContextInvocationReflection;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.retrofit.ContextAdapterFactory;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.util.InvocationReflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Implementation of a call adapter factory supporting {@code Channel} and {@code StreamBuilder}
 * return types.
 * <br>
 * The routine invocations will run in a dedicated Android Loader.
 * <br>
 * Note that the routines generated through stream builders must be invoked and the returned channel
 * closed before any result is produced.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderAdapterFactoryCompat extends ContextAdapterFactory {

  private final LoaderConfiguration mLoaderConfiguration;

  private final LoaderSourceCompat mLoaderSource;

  /**
   * Constructor.
   *
   * @param loaderSource            the Loader source.
   * @param delegateFactory         the delegate factory.
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   */
  private LoaderAdapterFactoryCompat(@NotNull final LoaderSourceCompat loaderSource,
      @Nullable final CallAdapter.Factory delegateFactory,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final LoaderConfiguration loaderConfiguration) {
    super(delegateFactory, invocationConfiguration);
    mLoaderSource = loaderSource;
    mLoaderConfiguration = loaderConfiguration;
  }

  /**
   * Returns an adapter factory builder.
   *
   * @param loaderSource the Loader source.
   * @return the builder instance.
   */
  @NotNull
  public static Builder factoryOn(@NotNull final LoaderSourceCompat loaderSource) {
    return new Builder(loaderSource);
  }

  @NotNull
  @Override
  protected Routine<? extends Call<?>, ?> buildRoutine(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, @NotNull final Type returnRawType,
      @NotNull final Type responseType, @NotNull final Annotation[] annotations,
      @NotNull final Retrofit retrofit) {
    // Use annotations to configure the routine
    final InvocationConfiguration invocationConfiguration =
        InvocationReflection.withAnnotations(configuration, annotations);
    final LoaderConfiguration loaderConfiguration =
        ContextInvocationReflection.withAnnotations(mLoaderConfiguration, annotations);
    final ContextInvocationFactory<Call<Object>, Object> factory =
        getFactory(configuration, responseType, annotations, retrofit);
    return JRoutineLoaderCompat.routineOn(mLoaderSource)
                               .withConfiguration(invocationConfiguration)
                               .withConfiguration(loaderConfiguration)
                               .of(factory);
  }

  @Nullable
  @Override
  protected CallAdapter<?, ?> get(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, @NotNull final Type returnRawType,
      @NotNull final Type responseType, @NotNull final Annotation[] annotations,
      @NotNull final Retrofit retrofit) {
    final CallAdapter<?, ?> callAdapter =
        super.get(executor, configuration, returnRawType, responseType, annotations, retrofit);
    return (callAdapter != null) ? ComparableCall.wrap(callAdapter) : null;
  }

  /**
   * Builder of routine adapter factory instances.
   * <p>
   * The options set through the builder configuration will be applied to all the routine handling
   * the Retrofit calls, unless they are overwritten by specific annotations.
   *
   * @see InvocationReflection#withAnnotations(InvocationConfiguration, Annotation...)
   * @see ContextInvocationReflection#withAnnotations(LoaderConfiguration, Annotation...)
   */
  public static class Builder
      implements InvocationConfigurable<Builder>, LoaderConfigurable<Builder> {

    private final LoaderSourceCompat mLoaderSource;

    private CallAdapter.Factory mDelegateFactory;

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param loaderSource the Loader source.
     */
    private Builder(@NotNull final LoaderSourceCompat loaderSource) {
      mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public LoaderAdapterFactoryCompat buildFactory() {
      return new LoaderAdapterFactoryCompat(mLoaderSource, mDelegateFactory,
          mInvocationConfiguration, mLoaderConfiguration);
    }

    /**
     * Sets the delegate factory to be used to execute the calls.
     *
     * @param factory the factory instance.
     * @return this builder.
     */
    @NotNull
    public Builder delegateFactory(@Nullable final CallAdapter.Factory factory) {
      mDelegateFactory = factory;
      return this;
    }

    @NotNull
    @Override
    public Builder withConfiguration(@NotNull final InvocationConfiguration configuration) {
      mInvocationConfiguration =
          ConstantConditions.notNull("invocation configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public Builder withConfiguration(@NotNull final LoaderConfiguration configuration) {
      mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends Builder> withInvocation() {
      return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends Builder> withLoader() {
      return new LoaderConfiguration.Builder<Builder>(this, mLoaderConfiguration);
    }
  }
}
