/*
 * Copyright 2017 Davide Maestroni
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

import com.github.dm.jrt.WrapperRoutineBuilder.ProxyStrategyType;
import com.github.dm.jrt.android.ServiceWrapperRoutineBuilder;
import com.github.dm.jrt.android.core.ServiceSource;
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
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Service reflection/proxy routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
class DefaultServiceWrapperRoutineBuilder implements ServiceWrapperRoutineBuilder {

  private final ServiceSource mServiceSource;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ProxyStrategyType mProxyStrategyType;

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param serviceSource the Service source.
   */
  DefaultServiceWrapperRoutineBuilder(@NotNull final ServiceSource serviceSource) {
    mServiceSource = ConstantConditions.notNull("Service source", serviceSource);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final String name) {
    return newReflectionBuilder().methodOf(target, name);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final String name, @NotNull final Class<?>... parameterTypes) {
    return newReflectionBuilder().methodOf(target, name, parameterTypes);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Method method) {
    return newReflectionBuilder().methodOf(target, method);
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final ClassToken<TYPE> itf) {
    return proxyOf(target, itf.getRawClass());
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Class<TYPE> itf) {
    final ProxyStrategyType proxyStrategyType = mProxyStrategyType;
    if (proxyStrategyType == null) {
      final ServiceProxy proxyAnnotation = itf.getAnnotation(ServiceProxy.class);
      if ((proxyAnnotation != null) && target.isAssignableTo(proxyAnnotation.value())) {
        return newProxyBuilder().proxyOf(target, itf);
      }

      return newReflectionBuilder().proxyOf(target, itf);

    } else if (proxyStrategyType == ProxyStrategyType.CODE_GENERATION) {
      return newProxyBuilder().proxyOf(target, itf);
    }

    return newReflectionBuilder().proxyOf(target, itf);
  }

  @NotNull
  @Override
  public ServiceWrapperRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceWrapperRoutineBuilder withConfiguration(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceWrapperRoutineBuilder> withInvocation() {
    return new InvocationConfiguration.Builder<ServiceWrapperRoutineBuilder>(
        new InvocationConfiguration.Configurable<ServiceWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceWrapperRoutineBuilder withConfiguration(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultServiceWrapperRoutineBuilder.this.withConfiguration(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends ServiceWrapperRoutineBuilder> withWrapper() {
    return new WrapperConfiguration.Builder<ServiceWrapperRoutineBuilder>(
        new WrapperConfiguration.Configurable<ServiceWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceWrapperRoutineBuilder withConfiguration(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultServiceWrapperRoutineBuilder.this.withConfiguration(configuration);
          }
        }, mWrapperConfiguration);
  }

  @NotNull
  @Override
  public ServiceWrapperRoutineBuilder withConfiguration(
      @NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceWrapperRoutineBuilder> withService() {
    return new ServiceConfiguration.Builder<ServiceWrapperRoutineBuilder>(
        new ServiceConfiguration.Configurable<ServiceWrapperRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceWrapperRoutineBuilder withConfiguration(
              @NotNull final ServiceConfiguration configuration) {
            return DefaultServiceWrapperRoutineBuilder.this.withConfiguration(configuration);
          }
        }, mServiceConfiguration);
  }

  @NotNull
  @Override
  public ServiceWrapperRoutineBuilder withStrategy(@Nullable final ProxyStrategyType strategyType) {
    mProxyStrategyType = strategyType;
    return this;
  }

  @NotNull
  private ServiceProxyRoutineBuilder newProxyBuilder() {
    return JRoutineServiceProxy.wrapperOn(mServiceSource)
                               .withConfiguration(mInvocationConfiguration)
                               .withConfiguration(mWrapperConfiguration)
                               .withConfiguration(mServiceConfiguration);
  }

  @NotNull
  private ServiceReflectionRoutineBuilder newReflectionBuilder() {
    return JRoutineServiceReflection.wrapperOn(mServiceSource)
                                    .withConfiguration(mInvocationConfiguration)
                                    .withConfiguration(mWrapperConfiguration)
                                    .withConfiguration(mServiceConfiguration);
  }
}
