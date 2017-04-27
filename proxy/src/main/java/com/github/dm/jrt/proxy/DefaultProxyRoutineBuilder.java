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

package com.github.dm.jrt.proxy;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.proxy.builder.AbstractProxyObjectBuilder;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;
import com.github.dm.jrt.reflect.InvocationTarget;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

import static com.github.dm.jrt.core.util.Reflection.findBestMatchingConstructor;

/**
 * Default implementation of a proxy builder.
 * <p>
 * Created by davide-maestroni on 03/23/2015.
 */
class DefaultProxyRoutineBuilder implements ProxyRoutineBuilder {

  private final ScheduledExecutor mExecutor;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultProxyRoutineBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  private static InvocationTarget<?> validateTarget(@NotNull final InvocationTarget<?> target) {
    final Class<?> targetClass = target.getTargetClass();
    if (targetClass.isInterface()) {
      throw new IllegalArgumentException(
          "the target class must not be an interface: " + targetClass.getName());
    }

    return target;
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final InvocationTarget<?> target,
      @NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    if (!itf.isAnnotationPresent(Proxy.class)) {
      throw new IllegalArgumentException(
          "the specified class is not annotated with " + Proxy.class.getName() + ": "
              + itf.getName());
    }

    final TargetProxyObjectBuilder<TYPE> builder =
        new TargetProxyObjectBuilder<TYPE>(mExecutor, itf);
    return builder.withConfiguration(mInvocationConfiguration)
                  .withConfiguration(mWrapperConfiguration)
                  .proxyOf(validateTarget(target));
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final InvocationTarget<?> target,
      @NotNull final ClassToken<TYPE> itf) {
    return proxyOf(target, itf.getRawClass());
  }

  @NotNull
  public ProxyRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  public ProxyRoutineBuilder withConfiguration(@NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  public InvocationConfiguration.Builder<? extends ProxyRoutineBuilder> withInvocation() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ProxyRoutineBuilder>(this, config);
  }

  @NotNull
  public WrapperConfiguration.Builder<? extends ProxyRoutineBuilder> withWrapper() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<ProxyRoutineBuilder>(this, config);
  }

  /**
   * Proxy builder implementation.
   *
   * @param <TYPE> the interface type.
   */
  private static class TargetProxyObjectBuilder<TYPE> extends AbstractProxyObjectBuilder<TYPE> {

    private final Class<? super TYPE> mInterfaceClass;

    /**
     * Constructor.
     *
     * @param executor       the executor instance.
     * @param interfaceClass the proxy interface class.
     */
    private TargetProxyObjectBuilder(@NotNull final ScheduledExecutor executor,
        @NotNull final Class<? super TYPE> interfaceClass) {
      super(executor);
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
    protected TYPE newProxy(@NotNull final InvocationTarget<?> target,
        @NotNull final ScheduledExecutor executor,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final WrapperConfiguration wrapperConfiguration) throws Exception {
      final Class<? super TYPE> interfaceClass = mInterfaceClass;
      final Proxy annotation = interfaceClass.getAnnotation(Proxy.class);
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
          findBestMatchingConstructor(Class.forName(fullClassName), target, executor,
              invocationConfiguration, wrapperConfiguration);
      return (TYPE) constructor.newInstance(target, executor, invocationConfiguration,
          wrapperConfiguration);
    }
  }
}
