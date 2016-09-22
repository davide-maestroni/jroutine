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
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.JRoutineServiceObject;
import com.github.dm.jrt.android.object.builder.ServiceObjectRoutineBuilder;
import com.github.dm.jrt.android.proxy.JRoutineServiceProxy;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Service target routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
class DefaultServiceObjectProxyRoutineBuilder implements ServiceObjectProxyRoutineBuilder {

  private final ServiceContext mContext;

  private final ContextInvocationTarget<?> mTarget;

  private BuilderType mBuilderType;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the Service context.
   * @param target  the invocation target.
   */
  DefaultServiceObjectProxyRoutineBuilder(@NotNull final ServiceContext context,
      @NotNull final ContextInvocationTarget<?> target) {
    mContext = ConstantConditions.notNull("Service context", context);
    mTarget = ConstantConditions.notNull("invocation target", target);
  }

  @NotNull
  @Override
  public ServiceObjectProxyRoutineBuilder apply(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceObjectProxyRoutineBuilder apply(@NotNull final ObjectConfiguration configuration) {
    mObjectConfiguration = ConstantConditions.notNull("object configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceObjectProxyRoutineBuilder>
  applyInvocationConfiguration() {
    return new InvocationConfiguration.Builder<ServiceObjectProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<ServiceObjectProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceObjectProxyRoutineBuilder apply(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultServiceObjectProxyRoutineBuilder.this.apply(configuration);
          }
        }, mInvocationConfiguration);
  }

  @NotNull
  @Override
  public ObjectConfiguration.Builder<? extends ServiceObjectProxyRoutineBuilder>
  applyObjectConfiguration() {
    return new ObjectConfiguration.Builder<ServiceObjectProxyRoutineBuilder>(
        new ObjectConfiguration.Configurable<ServiceObjectProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceObjectProxyRoutineBuilder apply(
              @NotNull final ObjectConfiguration configuration) {
            return DefaultServiceObjectProxyRoutineBuilder.this.apply(configuration);
          }
        }, mObjectConfiguration);
  }

  @NotNull
  @Override
  public ServiceObjectProxyRoutineBuilder withType(@Nullable final BuilderType builderType) {
    mBuilderType = builderType;
    return this;
  }

  @NotNull
  @Override
  public ServiceObjectProxyRoutineBuilder apply(@NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceObjectProxyRoutineBuilder>
  applyServiceConfiguration() {
    return new ServiceConfiguration.Builder<ServiceObjectProxyRoutineBuilder>(
        new ServiceConfiguration.Configurable<ServiceObjectProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceObjectProxyRoutineBuilder apply(
              @NotNull final ServiceConfiguration configuration) {
            return DefaultServiceObjectProxyRoutineBuilder.this.apply(configuration);
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

      return newObjectBuilder().buildProxy(itf);

    } else if (builderType == BuilderType.PROXY) {
      return newProxyBuilder().buildProxy(itf);
    }

    return newObjectBuilder().buildProxy(itf);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {
    return buildProxy(itf.getRawClass());
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {
    return newObjectBuilder().method(name);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
      @NotNull final Class<?>... parameterTypes) {
    return newObjectBuilder().method(name, parameterTypes);
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {
    return newObjectBuilder().method(method);
  }

  @NotNull
  private ServiceObjectRoutineBuilder newObjectBuilder() {
    return JRoutineServiceObject.on(mContext)
                                .with(mTarget)
                                .apply(mInvocationConfiguration)
                                .apply(mObjectConfiguration)
                                .apply(mServiceConfiguration);
  }

  @NotNull
  private ServiceProxyRoutineBuilder newProxyBuilder() {
    return JRoutineServiceProxy.on(mContext)
                               .with(mTarget)
                               .apply(mInvocationConfiguration)
                               .apply(mObjectConfiguration)
                               .apply(mServiceConfiguration);
  }
}
