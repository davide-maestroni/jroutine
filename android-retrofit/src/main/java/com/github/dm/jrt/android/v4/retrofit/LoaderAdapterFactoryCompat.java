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
import com.github.dm.jrt.android.reflect.builder.AndroidReflectionRoutineBuilders;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.retrofit.ContextAdapterFactory;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders;

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

  private final LoaderContextCompat mLoaderContext;

  /**
   * Constructor.
   *
   * @param context                 the Loader context.
   * @param delegateFactory         the delegate factory.
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   */
  private LoaderAdapterFactoryCompat(@NotNull final LoaderContextCompat context,
      @Nullable final CallAdapter.Factory delegateFactory,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final LoaderConfiguration loaderConfiguration) {
    super(delegateFactory, invocationConfiguration);
    mLoaderContext = context;
    mLoaderConfiguration = loaderConfiguration;
  }

  /**
   * Returns an adapter factory builder.
   *
   * @param context the Loader context.
   * @return the builder instance.
   */
  @NotNull
  public static Builder on(@NotNull final LoaderContextCompat context) {
    return new Builder(context);
  }

  @NotNull
  @Override
  protected Routine<? extends Call<?>, ?> buildRoutine(
      @NotNull final InvocationConfiguration configuration, @NotNull final Type returnRawType,
      @NotNull final Type responseType, @NotNull final Annotation[] annotations,
      @NotNull final Retrofit retrofit) {
    // Use annotations to configure the routine
    final InvocationConfiguration invocationConfiguration =
        ReflectionRoutineBuilders.withAnnotations(configuration, annotations);
    final LoaderConfiguration loaderConfiguration =
        AndroidReflectionRoutineBuilders.withAnnotations(mLoaderConfiguration, annotations);
    final ContextInvocationFactory<Call<Object>, Object> factory =
        getFactory(configuration, responseType, annotations, retrofit);
    return JRoutineLoaderCompat.on(mLoaderContext)
                               .with(factory)
                               .withConfiguration(invocationConfiguration)
                               .apply(loaderConfiguration)
                               .buildRoutine();
  }

  @Nullable
  @Override
  protected CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
      @NotNull final Type returnRawType, @NotNull final Type responseType,
      @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
    final CallAdapter<?> callAdapter =
        super.get(configuration, returnRawType, responseType, annotations, retrofit);
    return (callAdapter != null) ? ComparableCall.wrap(callAdapter) : null;
  }

  /**
   * Builder of routine adapter factory instances.
   * <p>
   * The options set through the builder configuration will be applied to all the routine handling
   * the Retrofit calls, unless they are overwritten by specific annotations.
   *
   * @see ReflectionRoutineBuilders#withAnnotations(InvocationConfiguration, Annotation...)
   * @see AndroidReflectionRoutineBuilders#withAnnotations(LoaderConfiguration, Annotation...)
   */
  public static class Builder
      implements InvocationConfigurable<Builder>, LoaderConfigurable<Builder> {

    private final LoaderContextCompat mLoaderContext;

    private CallAdapter.Factory mDelegateFactory;

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the Loader context.
     */
    private Builder(@NotNull final LoaderContextCompat context) {
      mLoaderContext = ConstantConditions.notNull("Loader context", context);
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
    public Builder apply(@NotNull final LoaderConfiguration configuration) {
      mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
      return this;
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public LoaderAdapterFactoryCompat buildFactory() {
      return new LoaderAdapterFactoryCompat(mLoaderContext, mDelegateFactory,
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
    public InvocationConfiguration.Builder<? extends Builder> withInvocation() {
      return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends Builder> loaderConfiguration() {
      return new LoaderConfiguration.Builder<Builder>(this, mLoaderConfiguration);
    }
  }
}
