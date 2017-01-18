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

import com.github.dm.jrt.android.LoaderReflectionProxyRoutineBuilder;
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
import com.github.dm.jrt.reflect.config.ReflectionConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Loader reflection/proxy routine builder.
 * <p>
 * Created by davide-maestroni on 03/07/2016.
 */
class DefaultLoaderReflectionProxyRoutineBuilder implements LoaderReflectionProxyRoutineBuilder {

  private final LoaderContext mContext;

  private final ContextInvocationTarget<?> mTarget;

  private BuilderType mBuilderType;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private ReflectionConfiguration mReflectionConfiguration =
      ReflectionConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the Loader context.
   * @param target  the invocation target.
   */
  DefaultLoaderReflectionProxyRoutineBuilder(@NotNull final LoaderContext context,
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
      @NotNull final ReflectionConfiguration configuration) {
    mReflectionConfiguration =
        ConstantConditions.notNull("reflection configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends LoaderReflectionProxyRoutineBuilder> applyInvocationConfiguration() {
    return new InvocationConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public ReflectionConfiguration.Builder<? extends LoaderReflectionProxyRoutineBuilder>
  applyReflectionConfiguration() {
    return new ReflectionConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new ReflectionConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final ReflectionConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mReflectionConfiguration);
  }

  @NotNull
  @Override
  public LoaderReflectionProxyRoutineBuilder withType(@Nullable final BuilderType builderType) {
    mBuilderType = builderType;
    return this;
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
  applyLoaderConfiguration() {
    return new LoaderConfiguration.Builder<LoaderReflectionProxyRoutineBuilder>(
        new LoaderConfiguration.Configurable<LoaderReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionProxyRoutineBuilder apply(
              @NotNull final LoaderConfiguration configuration) {
            return DefaultLoaderReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mLoaderConfiguration);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    final BuilderType builderType = mBuilderType;
    if (builderType == null) {
      final LoaderProxy proxyAnnotation = itf.getAnnotation(LoaderProxy.class);
      if ((proxyAnnotation != null) && mTarget.isAssignableTo(proxyAnnotation.value())) {
        return newProxyBuilder().buildProxy(itf);
      }

      return newReflectionBuilder().buildProxy(itf);

    } else if (builderType == BuilderType.CODE_GENERATION) {
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
                              .apply(mReflectionConfiguration)
                              .apply(mLoaderConfiguration);
  }

  @NotNull
  private LoaderReflectionRoutineBuilder newReflectionBuilder() {
    return JRoutineLoaderReflection.on(mContext)
                                   .with(mTarget)
                                   .apply(mInvocationConfiguration)
                                   .apply(mReflectionConfiguration)
                                   .apply(mLoaderConfiguration);
  }
}
