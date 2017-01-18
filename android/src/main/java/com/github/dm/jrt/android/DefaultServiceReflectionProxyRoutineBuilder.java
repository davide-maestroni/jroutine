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

package com.github.dm.jrt.android;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.proxy.JRoutineServiceProxy;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.reflect.JRoutineServiceReflection;
import com.github.dm.jrt.android.reflect.builder.ServiceReflectionRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.config.ReflectionConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Service reflection/proxy routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
class DefaultServiceReflectionProxyRoutineBuilder implements ServiceReflectionProxyRoutineBuilder {

  private final ServiceContext mContext;

  private final ContextInvocationTarget<?> mTarget;

  private BuilderType mBuilderType;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ReflectionConfiguration mReflectionConfiguration =
      ReflectionConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the Service context.
   * @param target  the invocation target.
   */
  DefaultServiceReflectionProxyRoutineBuilder(@NotNull final ServiceContext context,
      @NotNull final ContextInvocationTarget<?> target) {
    mContext = ConstantConditions.notNull("Service context", context);
    mTarget = ConstantConditions.notNull("invocation target", target);
  }

  @NotNull
  @Override
  public ServiceReflectionProxyRoutineBuilder apply(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceReflectionProxyRoutineBuilder apply(
      @NotNull final ReflectionConfiguration configuration) {
    mReflectionConfiguration =
        ConstantConditions.notNull("reflection configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceReflectionProxyRoutineBuilder>
  applyInvocationConfiguration() {
    return new InvocationConfiguration.Builder<ServiceReflectionProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<ServiceReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceReflectionProxyRoutineBuilder apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultServiceReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public ReflectionConfiguration.Builder<? extends ServiceReflectionProxyRoutineBuilder>
  applyReflectionConfiguration() {
    return new ReflectionConfiguration.Builder<ServiceReflectionProxyRoutineBuilder>(
        new ReflectionConfiguration.Configurable<ServiceReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceReflectionProxyRoutineBuilder apply(
              @NotNull final ReflectionConfiguration configuration) {
            return DefaultServiceReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mReflectionConfiguration);
  }

  @NotNull
  @Override
  public ServiceReflectionProxyRoutineBuilder withType(@Nullable final BuilderType builderType) {
    mBuilderType = builderType;
    return this;
  }

  @NotNull
  @Override
  public ServiceReflectionProxyRoutineBuilder apply(
      @NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceReflectionProxyRoutineBuilder>
  applyServiceConfiguration() {
    return new ServiceConfiguration.Builder<ServiceReflectionProxyRoutineBuilder>(
        new ServiceConfiguration.Configurable<ServiceReflectionProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceReflectionProxyRoutineBuilder apply(
              @NotNull final ServiceConfiguration configuration) {
            return DefaultServiceReflectionProxyRoutineBuilder.this.apply(configuration);
          }
        }, mServiceConfiguration);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    final BuilderType builderType = mBuilderType;
    if (builderType == null) {
      final ServiceProxy proxyAnnotation = itf.getAnnotation(ServiceProxy.class);
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
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {
    return newReflectionBuilder().method(name);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
      @NotNull final Class<?>... parameterTypes) {
    return newReflectionBuilder().method(name, parameterTypes);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {
    return newReflectionBuilder().method(method);
  }

  @NotNull
  private ServiceProxyRoutineBuilder newProxyBuilder() {
    return JRoutineServiceProxy.on(mContext)
                               .with(mTarget)
                               .apply(mInvocationConfiguration)
                               .apply(mReflectionConfiguration)
                               .apply(mServiceConfiguration);
  }

  @NotNull
  private ServiceReflectionRoutineBuilder newReflectionBuilder() {
    return JRoutineServiceReflection.on(mContext)
                                    .with(mTarget)
                                    .apply(mInvocationConfiguration)
                                    .apply(mReflectionConfiguration)
                                    .apply(mServiceConfiguration);
  }
}
