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

import com.github.dm.jrt.android.LoaderWrapperRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxy;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.reflect.builder.LoaderReflectionRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.proxy.JRoutineLoaderProxy;
import com.github.dm.jrt.android.v11.reflect.JRoutineLoaderReflection;
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
class DefaultLoaderWrapperRoutineBuilder implements LoaderWrapperRoutineBuilder {

  private final LoaderContext mContext;

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
  DefaultLoaderWrapperRoutineBuilder(@NotNull final LoaderContext context,
      @NotNull final ContextInvocationTarget<?> target) {
    mContext = ConstantConditions.notNull("Loader context", context);
    mTarget = ConstantConditions.notNull("invocation target", target);
  }

  @NotNull
  @Override
  public LoaderWrapperRoutineBuilder apply(@NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderWrapperRoutineBuilder apply(@NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends LoaderWrapperRoutineBuilder> invocationConfiguration() {
    return new InvocationConfiguration.Builder<LoaderWrapperRoutineBuilder>(
        new InvocationConfiguration.Configurable<LoaderWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderWrapperRoutineBuilder apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderWrapperRoutineBuilder.this.apply(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public LoaderWrapperRoutineBuilder withStrategy(@Nullable final ProxyStrategyType strategyType) {
    mProxyStrategyType = strategyType;
    return this;
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends LoaderWrapperRoutineBuilder> wrapperConfiguration
      () {
    return new WrapperConfiguration.Builder<LoaderWrapperRoutineBuilder>(
        new WrapperConfiguration.Configurable<LoaderWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderWrapperRoutineBuilder apply(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultLoaderWrapperRoutineBuilder.this.apply(configuration);
          }
        }, mWrapperConfiguration);
  }

  @NotNull
  @Override
  public LoaderWrapperRoutineBuilder apply(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderWrapperRoutineBuilder> loaderConfiguration() {
    return new LoaderConfiguration.Builder<LoaderWrapperRoutineBuilder>(
        new LoaderConfiguration.Configurable<LoaderWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderWrapperRoutineBuilder apply(
              @NotNull final LoaderConfiguration configuration) {
            return DefaultLoaderWrapperRoutineBuilder.this.apply(configuration);
          }
        }, mLoaderConfiguration);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    final ProxyStrategyType proxyStrategyType = mProxyStrategyType;
    if (proxyStrategyType == null) {
      final LoaderProxy proxyAnnotation = itf.getAnnotation(LoaderProxy.class);
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
    return JRoutineLoaderProxy.on(mContext)
                              .with(mTarget)
                              .apply(mInvocationConfiguration)
                              .apply(mWrapperConfiguration)
                              .apply(mLoaderConfiguration);
  }

  @NotNull
  private LoaderReflectionRoutineBuilder newReflectionBuilder() {
    return JRoutineLoaderReflection.on(mContext)
                                   .with(mTarget)
                                   .apply(mInvocationConfiguration)
                                   .apply(mWrapperConfiguration)
                                   .apply(mLoaderConfiguration);
  }
}
