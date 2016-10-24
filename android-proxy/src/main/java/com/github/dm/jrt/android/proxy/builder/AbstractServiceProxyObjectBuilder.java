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

import android.content.Context;

import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a builder of async proxy objects, whose methods are executed in a
 * dedicated Service.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractServiceProxyObjectBuilder<TYPE>
    implements ServiceProxyObjectBuilder<TYPE> {

  private static final WeakIdentityHashMap<Context, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
      sContextProxies =
      new WeakIdentityHashMap<Context, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>();

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  @NotNull
  @Override
  public ServiceProxyObjectBuilder<TYPE> apply(@NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceProxyObjectBuilder<TYPE> apply(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceProxyObjectBuilder<TYPE> apply(@NotNull final ObjectConfiguration configuration) {
    mObjectConfiguration = ConstantConditions.notNull("object configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
  applyInvocationConfiguration() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(
        new InvocationConfiguration.Configurable<ServiceProxyObjectBuilder<TYPE>>() {

          @NotNull
          @Override
          public ServiceProxyObjectBuilder<TYPE> apply(
              @NotNull final InvocationConfiguration configuration) {
            return AbstractServiceProxyObjectBuilder.this.apply(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public ObjectConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
  applyObjectConfiguration() {
    final ObjectConfiguration config = mObjectConfiguration;
    return new ObjectConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(
        new ObjectConfiguration.Configurable<ServiceProxyObjectBuilder<TYPE>>() {

          @NotNull
          @Override
          public ServiceProxyObjectBuilder<TYPE> apply(
              @NotNull final ObjectConfiguration configuration) {
            return AbstractServiceProxyObjectBuilder.this.apply(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public TYPE buildProxy() {
    synchronized (sContextProxies) {
      final Context context = getInvocationContext();
      if (context == null) {
        throw new IllegalStateException("the invocation context has been destroyed");
      }

      final WeakIdentityHashMap<Context, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
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
      final ObjectConfiguration objectConfiguration = mObjectConfiguration;
      final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
      final ProxyInfo proxyInfo =
          new ProxyInfo(getInterfaceClass(), invocationConfiguration, objectConfiguration,
              serviceConfiguration);
      final Object instance = proxies.get(proxyInfo);
      if (instance != null) {
        return (TYPE) instance;
      }

      try {
        final TYPE newInstance =
            newProxy(invocationConfiguration, objectConfiguration, serviceConfiguration);
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
  public ServiceConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
  applyServiceConfiguration() {
    final ServiceConfiguration config = mServiceConfiguration;
    return new ServiceConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
  }

  /**
   * Returns the builder proxy class.
   *
   * @return the proxy class.
   */
  @NotNull
  protected abstract Class<? super TYPE> getInterfaceClass();

  /**
   * Returns the Context on which the invocation is based.
   * <br>
   * Returning null means that the Context has been destroyed, so an exception will be thrown.
   *
   * @return the invocation Context.
   */
  @Nullable
  protected abstract Context getInvocationContext();

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
   * @param objectConfiguration     the object configuration.
   * @param serviceConfiguration    the Service configuration.
   * @return the proxy instance.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
      @NotNull ObjectConfiguration objectConfiguration,
      @NotNull ServiceConfiguration serviceConfiguration) throws Exception;

  /**
   * Class used as key to identify a specific proxy instance.
   */
  private static class ProxyInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param itf                     the proxy interface class.
     * @param invocationConfiguration the invocation configuration.
     * @param objectConfiguration     the object configuration.
     * @param serviceConfiguration    the Service configuration.
     */
    private ProxyInfo(@NotNull final Class<?> itf,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final ObjectConfiguration objectConfiguration,
        @NotNull final ServiceConfiguration serviceConfiguration) {
      super(asArgs(itf, invocationConfiguration, objectConfiguration, serviceConfiguration));
    }
  }
}
