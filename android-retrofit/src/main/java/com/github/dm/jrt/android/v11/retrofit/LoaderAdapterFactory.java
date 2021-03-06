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

package com.github.dm.jrt.android.v11.retrofit;

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.android.retrofit.ContextAdapterFactory;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.JRoutineLoaderStream;
import com.github.dm.jrt.android.v11.stream.LoaderStreamBuilder;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.builder.Builders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Implementation of a call adapter factory supporting {@code Channel}, {@code StreamBuilder} and
 * {@code LoaderStreamBuilder} return types.
 * <br>
 * The routine invocations will run in a dedicated Android Loader.
 * <br>
 * Note that the routines generated through stream builders must be invoked and the returned channel
 * closed before any result is produced.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.retrofit.LoaderAdapterFactoryCompat
 * LoaderAdapterFactoryCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderAdapterFactory extends ContextAdapterFactory {

  private final LoaderConfiguration mLoaderConfiguration;

  private final LoaderContext mLoaderContext;

  /**
   * Constructor.
   *
   * @param context                 the Loader context.
   * @param delegateFactory         the delegate factory.
   * @param invocationConfiguration the invocation configuration.
   * @param loaderConfiguration     the Loader configuration.
   */
  private LoaderAdapterFactory(@NotNull final LoaderContext context,
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
  public static Builder on(@NotNull final LoaderContext context) {
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
        Builders.withAnnotations(configuration, annotations);
    final LoaderConfiguration loaderConfiguration =
        AndroidBuilders.withAnnotations(mLoaderConfiguration, annotations);
    final ContextInvocationFactory<Call<Object>, Object> factory =
        getFactory(configuration, responseType, annotations, retrofit);
    return JRoutineLoader.on(mLoaderContext)
                         .with(factory)
                         .apply(invocationConfiguration)
                         .apply(loaderConfiguration)
                         .buildRoutine();
  }

  @Nullable
  @Override
  protected Type extractResponseType(@NotNull final ParameterizedType returnType) {
    if (LoaderStreamBuilder.class == returnType.getRawType()) {
      return returnType.getActualTypeArguments()[1];
    }

    return super.extractResponseType(returnType);
  }

  @Nullable
  @Override
  protected CallAdapter<?> get(@NotNull final InvocationConfiguration configuration,
      @NotNull final Type returnRawType, @NotNull final Type responseType,
      @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
    if (LoaderStreamBuilder.class == returnRawType) {
      return new LoaderStreamBuilderAdapter(
          buildRoutine(configuration, returnRawType, responseType, annotations, retrofit),
          responseType);
    }

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
   * @see Builders#withAnnotations(InvocationConfiguration, Annotation...)
   * @see AndroidBuilders#withAnnotations(LoaderConfiguration, Annotation...)
   */
  public static class Builder
      implements InvocationConfigurable<Builder>, LoaderConfigurable<Builder> {

    private final LoaderContext mLoaderContext;

    private CallAdapter.Factory mDelegateFactory;

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the Loader context.
     */
    private Builder(@NotNull final LoaderContext context) {
      mLoaderContext = ConstantConditions.notNull("Loader context", context);
    }

    @NotNull
    @Override
    public Builder apply(@NotNull final InvocationConfiguration configuration) {
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

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends Builder> applyInvocationConfiguration() {
      return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends Builder> applyLoaderConfiguration() {
      return new LoaderConfiguration.Builder<Builder>(this, mLoaderConfiguration);
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public LoaderAdapterFactory buildFactory() {
      return new LoaderAdapterFactory(mLoaderContext, mDelegateFactory, mInvocationConfiguration,
          mLoaderConfiguration);
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
  }

  /**
   * Loader stream builder adapter implementation.
   */
  private static class LoaderStreamBuilderAdapter extends BaseAdapter<LoaderStreamBuilder> {

    /**
     * Constructor.
     *
     * @param routine      the routine instance.
     * @param responseType the response type.
     */
    private LoaderStreamBuilderAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
        @NotNull final Type responseType) {
      super(routine, responseType);
    }

    @Override
    public <OUT> LoaderStreamBuilder adapt(final Call<OUT> call) {
      return JRoutineLoaderStream.<Call<?>>withStreamOf(ComparableCall.of(call)).map(getRoutine());
    }
  }
}
