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

package com.github.dm.jrt.android.proxy;

import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.AbstractServiceProxyObjectBuilder;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

import static com.github.dm.jrt.core.util.Reflection.findBestMatchingConstructor;

/**
 * Default implementation of a Service proxy builder.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 */
class DefaultServiceProxyRoutineBuilder implements ServiceProxyRoutineBuilder {

  private final ServiceSource mServiceSource;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param serviceSource the Service source.
   */
  DefaultServiceProxyRoutineBuilder(@NotNull final ServiceSource serviceSource) {
    mServiceSource = ConstantConditions.notNull("Service source", serviceSource);
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    if (!itf.isAnnotationPresent(ServiceProxy.class)) {
      throw new IllegalArgumentException(
          "the specified class is not annotated with " + ServiceProxy.class.getName() + ": "
              + itf.getName());
    }

    final TargetServiceProxyObjectBuilder<TYPE> builder =
        new TargetServiceProxyObjectBuilder<TYPE>(mServiceSource, itf);
    return builder.withConfiguration(mInvocationConfiguration)
                  .withConfiguration(mWrapperConfiguration)
                  .withConfiguration(mServiceConfiguration)
                  .proxyOf(target);
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final ClassToken<TYPE> itf) {
    return proxyOf(target, itf.getRawClass());
  }

  @NotNull
  @Override
  public ServiceProxyRoutineBuilder withConfiguration(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceProxyRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withInvocation() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ServiceProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<ServiceProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceProxyRoutineBuilder withConfiguration(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultServiceProxyRoutineBuilder.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withWrapper() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<ServiceProxyRoutineBuilder>(
        new WrapperConfiguration.Configurable<ServiceProxyRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceProxyRoutineBuilder withConfiguration(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultServiceProxyRoutineBuilder.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public ServiceProxyRoutineBuilder withConfiguration(
      @NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withService() {
    final ServiceConfiguration config = mServiceConfiguration;
    return new ServiceConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
  }

  /**
   * Proxy builder implementation.
   *
   * @param <TYPE> the interface type.
   */
  private static class TargetServiceProxyObjectBuilder<TYPE>
      extends AbstractServiceProxyObjectBuilder<TYPE> {

    private final Class<? super TYPE> mInterfaceClass;

    /**
     * Constructor.
     *
     * @param service        the Service source.
     * @param interfaceClass the proxy interface class.
     */
    private TargetServiceProxyObjectBuilder(@NotNull final ServiceSource service,
        @NotNull final Class<? super TYPE> interfaceClass) {
      super(service);
      mInterfaceClass = interfaceClass;
    }

    @NotNull
    @Override
    protected Class<? super TYPE> getInterfaceClass() {
      return mInterfaceClass;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected TYPE newProxy(@NotNull final ContextInvocationTarget<?> target,
        @NotNull final ServiceSource serviceSource,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ServiceConfiguration serviceConfiguration) throws Exception {
      final Class<? super TYPE> interfaceClass = mInterfaceClass;
      final ServiceProxy annotation = interfaceClass.getAnnotation(ServiceProxy.class);
      String packageName = annotation.classPackage();
      if (packageName.equals(Proxy.DEFAULT)) {
        final Package classPackage = interfaceClass.getPackage();
        packageName = (classPackage != null) ? classPackage.getName() + "." : "";

      } else {
        packageName += ".";
      }

      String className = annotation.className();
      if (className.equals(Proxy.DEFAULT)) {
        className = interfaceClass.getSimpleName();
        Class<?> enclosingClass = interfaceClass.getEnclosingClass();
        while (enclosingClass != null) {
          className = enclosingClass.getSimpleName() + "_" + className;
          enclosingClass = enclosingClass.getEnclosingClass();
        }
      }

      final String fullClassName =
          packageName + annotation.classPrefix() + className + annotation.classSuffix();
      final Constructor<?> constructor =
          findBestMatchingConstructor(Class.forName(fullClassName), target, serviceSource,
              invocationConfiguration, wrapperConfiguration, serviceConfiguration);
      return (TYPE) constructor.newInstance(target, serviceSource, invocationConfiguration,
          wrapperConfiguration, serviceConfiguration);
    }
  }
}
