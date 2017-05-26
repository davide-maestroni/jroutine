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

package com.github.dm.jrt.android.v4.proxy;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.builder.AbstractLoaderProxyObjectBuilder;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

import static com.github.dm.jrt.core.util.Reflection.findBestMatchingConstructor;

/**
 * Default implementation of a Context proxy builder.
 * <p>
 * Created by davide-maestroni on 05/06/2015.
 */
class DefaultLoaderProxyRoutineBuilderCompat implements LoaderProxyRoutineBuilder {

  private final LoaderSourceCompat mLoaderSource;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultLoaderProxyRoutineBuilderCompat(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader context", loaderSource);
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    if (!itf.isAnnotationPresent(LoaderProxyCompat.class)) {
      throw new IllegalArgumentException(
          "the specified class is not annotated with " + LoaderProxyCompat.class.getName() + ": "
              + itf.getName());
    }

    final TargetLoaderProxyObjectBuilder<TYPE> builder =
        new TargetLoaderProxyObjectBuilder<TYPE>(mLoaderSource, itf);
    return builder.withConfiguration(mInvocationConfiguration)
                  .withConfiguration(mWrapperConfiguration)
                  .withConfiguration(mLoaderConfiguration)
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
  public LoaderProxyRoutineBuilder withConfiguration(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderProxyRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withInvocation() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<LoaderProxyRoutineBuilder>(
        new InvocationConfiguration.Configurable<LoaderProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderProxyRoutineBuilder withConfiguration(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderProxyRoutineBuilderCompat.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withWrapper() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<LoaderProxyRoutineBuilder>(
        new WrapperConfiguration.Configurable<LoaderProxyRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderProxyRoutineBuilder withConfiguration(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultLoaderProxyRoutineBuilderCompat.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public LoaderProxyRoutineBuilder withConfiguration(
      @NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withLoader() {
    final LoaderConfiguration config = mLoaderConfiguration;
    return new LoaderConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
  }

  /**
   * Proxy builder implementation.
   *
   * @param <TYPE> the interface type.
   */
  private static class TargetLoaderProxyObjectBuilder<TYPE>
      extends AbstractLoaderProxyObjectBuilder<TYPE> {

    private final Class<? super TYPE> mInterfaceClass;

    private final LoaderSourceCompat mLoaderSource;

    /**
     * Constructor.
     *
     * @param loaderSource   the Loader source.
     * @param interfaceClass the proxy interface class.
     */
    private TargetLoaderProxyObjectBuilder(@NotNull final LoaderSourceCompat loaderSource,
        @NotNull final Class<? super TYPE> interfaceClass) {
      mLoaderSource = loaderSource;
      mInterfaceClass = interfaceClass;
    }

    @Nullable
    @Override
    protected Object getComponent() {
      return mLoaderSource.getComponent();
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
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) throws Exception {
      final Class<? super TYPE> interfaceClass = mInterfaceClass;
      final LoaderProxyCompat annotation = interfaceClass.getAnnotation(LoaderProxyCompat.class);
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
      final LoaderSourceCompat loaderSource = mLoaderSource;
      final Constructor<?> constructor =
          findBestMatchingConstructor(Class.forName(fullClassName), target, loaderSource,
              invocationConfiguration, wrapperConfiguration, loaderConfiguration);
      return (TYPE) constructor.newInstance(target, loaderSource, invocationConfiguration,
          wrapperConfiguration, loaderConfiguration);
    }
  }
}
