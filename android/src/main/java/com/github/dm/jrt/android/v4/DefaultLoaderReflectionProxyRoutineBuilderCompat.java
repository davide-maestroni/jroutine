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

import com.github.dm.jrt.android.LoaderReflectionProxyRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.reflect.builder.LoaderReflectionRoutineBuilder;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.proxy.JRoutineLoaderProxyCompat;
import com.github.dm.jrt.android.v4.reflect.JRoutineLoaderReflectionCompat;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Loader reflection/proxy routine builder.
 * <p>
 * Created by davide-maestroni on 03/07/2016.
 */
class DefaultLoaderReflectionProxyRoutineBuilderCompat
    implements LoaderReflectionProxyRoutineBuilder {

  private final LoaderContextCompat mContext;

  private final ContextInvocationTarget<?> mTarget;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private ProxyStrategyType mProxyStrategyType;

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the Loader context.
   * @param target  the invocation target.
   */
  DefaultLoaderReflectionProxyRoutineBuilderCompat(@NotNull final LoaderContextCompat context,
      @NotNull final ContextInvocationTarget<?> target) {
    mContext = ConstantConditions.notNull("Loader context", context);
    mTarget = ConstantConditions.notNull("invocation target", target);
  }

  @NotNull
  @Override
  public LoaderReflectionProxyRoutineBuilder apply(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderReflectionProxyRoutineBuilder apply(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends LoaderReflectionProxyRoutineBuilder> invocationConfiguration() {
    return new InvocationConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilderCompat.this.apply(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public LoaderReflectionProxyRoutineBuilder withStrategy(
      @Nullable final ProxyStrategyType strategyType) {
    mProxyStrategyType = strategyType;
    return this;
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends LoaderReflectionProxyRoutineBuilder>
  wrapperConfiguration() {
    return new WrapperConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new WrapperConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilderCompat.this.apply(configuration);
          }
        }, mWrapperConfiguration);
  }

  @NotNull
  @Override
  public LoaderReflectionProxyRoutineBuilder apply(
      @NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderReflectionProxyRoutineBuilder>
  loaderConfiguration() {
    return new LoaderConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new LoaderConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final LoaderConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilderCompat.this.apply(configuration);
          }
        }, mLoaderConfiguration);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    final ProxyStrategyType proxyStrategyType = mProxyStrategyType;
    if (proxyStrategyType == null) {
      final LoaderProxyCompat proxyAnnotation = itf.getAnnotation(LoaderProxyCompat.class);
      if ((proxyAnnotation != null) && mTarget.isAssignableTo(proxyAnnotation.value())) {
        return newProxyBuilder().buildProxy(itf);
      }

      return newReflectionBuilder().buildProxy(itf);

    } else if (proxyStrategyType == ProxyStrategyType.CODE_GENERATION) {
      return newProxyBuilder().buildProxy(itf);
    }

    return newReflectionBuilder().buildProxy(itf);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {
    return buildProxy(itf.getRawClass());
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name) {
    return newReflectionBuilder().method(name);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
      @NotNull final Class<?>... parameterTypes) {
    return newReflectionBuilder().method(name, parameterTypes);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {
    return newReflectionBuilder().method(method);
  }

  @NotNull
  private LoaderProxyRoutineBuilder newProxyBuilder() {
    return JRoutineLoaderProxyCompat.on(mContext)
                                    .with(mTarget)
                                    .apply(mInvocationConfiguration)
                                    .apply(mWrapperConfiguration)
                                    .apply(mLoaderConfiguration);
  }

  @NotNull
  private LoaderReflectionRoutineBuilder newReflectionBuilder() {
    return JRoutineLoaderReflectionCompat.on(mContext)
                                         .with(mTarget)
                                         .apply(mInvocationConfiguration)
                                         .apply(mWrapperConfiguration)
                                         .apply(mLoaderConfiguration);
  }
}
