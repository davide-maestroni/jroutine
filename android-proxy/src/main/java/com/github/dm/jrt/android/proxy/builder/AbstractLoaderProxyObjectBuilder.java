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

package com.github.dm.jrt.android.proxy.builder;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.reflect.config.ReflectionConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a builder of async proxy objects, running in dedicated Loaders.
 * <p>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractLoaderProxyObjectBuilder<TYPE>
    implements LoaderProxyObjectBuilder<TYPE> {

  private static final WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
      sContextProxies =
      new WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>();

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private ReflectionConfiguration mReflectionConfiguration =
      ReflectionConfiguration.defaultConfiguration();

  @NotNull
  @Override
  public LoaderProxyObjectBuilder<TYPE> apply(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderProxyObjectBuilder<TYPE> apply(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderProxyObjectBuilder<TYPE> apply(
      @NotNull final ReflectionConfiguration configuration) {
    mReflectionConfiguration =
        ConstantConditions.notNull("reflection configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>>
  applyInvocationConfiguration() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(
        new InvocationConfiguration.Configurable<LoaderProxyObjectBuilder<TYPE>>() {

          @NotNull
          @Override
          public LoaderProxyObjectBuilder<TYPE> apply(
              @NotNull final InvocationConfiguration configuration) {
            return AbstractLoaderProxyObjectBuilder.this.apply(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public ReflectionConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>>
  applyReflectionConfiguration() {
    final ReflectionConfiguration config = mReflectionConfiguration;
    return new ReflectionConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(
        new ReflectionConfiguration.Configurable<LoaderProxyObjectBuilder<TYPE>>() {

          @NotNull
          @Override
          public LoaderProxyObjectBuilder<TYPE> apply(
              @NotNull final ReflectionConfiguration configuration) {
            return AbstractLoaderProxyObjectBuilder.this.apply(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE buildProxy() {
    synchronized (sContextProxies) {
      final Object context = getInvocationContext();
      if (context == null) {
        throw new IllegalStateException("the invocation context has been destroyed");
      }

      final WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
          contextProxies = sContextProxies;
      HashMap<Class<?>, HashMap<ProxyInfo, Object>> proxyMap = contextProxies.get(context);
      if (proxyMap == null) {
        proxyMap = new HashMap<Class<?>, HashMap<ProxyInfo, Object>>();
        contextProxies.put(context, proxyMap);
      }

      final Class<?> targetClass = getTargetClass();
      HashMap<ProxyInfo, Object> proxies = proxyMap.get(targetClass);
      if (proxies == null) {
        proxies = new HashMap<ProxyInfo, Object>();
        proxyMap.put(targetClass, proxies);
      }

      final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
      final ReflectionConfiguration reflectionConfiguration = mReflectionConfiguration;
      final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
      final ProxyInfo proxyInfo =
          new ProxyInfo(getInterfaceClass(), invocationConfiguration, reflectionConfiguration,
              loaderConfiguration);
      final Object instance = proxies.get(proxyInfo);
      if (instance != null) {
        return (TYPE) instance;
      }

      final Runner asyncRunner = invocationConfiguration.getRunnerOrElse(null);
      if (asyncRunner != null) {
        invocationConfiguration.newLogger(this)
                               .wrn("the specified runner will be ignored: %s", asyncRunner);
      }

      try {
        final TYPE newInstance =
            newProxy(invocationConfiguration, reflectionConfiguration, loaderConfiguration);
        proxies.put(proxyInfo, newInstance);
        return newInstance;

      } catch (final Throwable t) {
        InvocationInterruptedException.throwIfInterrupt(t);
        throw new IllegalArgumentException(t);
      }
    }
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>>
  applyLoaderConfiguration() {
    final LoaderConfiguration config = mLoaderConfiguration;
    return new LoaderConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(this, config);
  }

  /**
   * Returns the builder proxy class.
   *
   * @return the proxy class.
   */
  @NotNull
  protected abstract Class<? super TYPE> getInterfaceClass();

  /**
   * Returns the Context or component (Activity, Fragment, etc.) on which the invocation is based.
   * <br>
   * Returning null means that the context has been destroyed, so an exception will be thrown.
   *
   * @return the invocation context.
   */
  @Nullable
  protected abstract Object getInvocationContext();

  /**
   * Returns the builder target class.
   *
   * @return the target class.
   */
  @NotNull
  protected abstract Class<?> getTargetClass();

  /**
   * Creates and return a new proxy instance.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param reflectionConfiguration the reflection configuration.
   * @param loaderConfiguration     the Loader configuration.
   * @return the proxy instance.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
      @NotNull ReflectionConfiguration reflectionConfiguration,
      @NotNull LoaderConfiguration loaderConfiguration) throws Exception;

  /**
   * Class used as key to identify a specific proxy instance.
   */
  private static class ProxyInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param itf                     the proxy interface class.
     * @param invocationConfiguration the invocation configuration.
     * @param reflectionConfiguration the reflection configuration.
     * @param loaderConfiguration     the Loader configuration.
     */
    private ProxyInfo(@NotNull final Class<?> itf,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final ReflectionConfiguration reflectionConfiguration,
        @NotNull final LoaderConfiguration loaderConfiguration) {
      super(asArgs(itf, invocationConfiguration, reflectionConfiguration, loaderConfiguration));
    }
  }
}
