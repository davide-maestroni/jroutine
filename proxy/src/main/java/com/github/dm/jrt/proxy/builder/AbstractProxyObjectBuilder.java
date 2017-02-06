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

package com.github.dm.jrt.proxy.builder;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.reflect.config.CallConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a builder of async proxy objects.
 * <p>
 * Created by davide-maestroni on 02/26/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractProxyObjectBuilder<TYPE> implements ProxyObjectBuilder<TYPE> {

  private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sProxies =
      new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

  private CallConfiguration mCallConfiguration = CallConfiguration.defaultConfiguration();

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  @NotNull
  public ProxyObjectBuilder<TYPE> apply(@NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  public ProxyObjectBuilder<TYPE> apply(@NotNull final CallConfiguration configuration) {
    mCallConfiguration = ConstantConditions.notNull("call configuration", configuration);
    return this;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE buildProxy() {
    final Object target = getTarget();
    synchronized (sProxies) {
      final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> proxies = sProxies;
      HashMap<ClassInfo, Object> proxyMap = proxies.get(target);
      if (proxyMap == null) {
        proxyMap = new HashMap<ClassInfo, Object>();
        proxies.put(target, proxyMap);
      }

      final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
      final CallConfiguration callConfiguration = mCallConfiguration;
      final ClassInfo classInfo =
          new ClassInfo(getInterfaceClass(), invocationConfiguration, callConfiguration);
      final Object instance = proxyMap.get(classInfo);
      if (instance != null) {
        return (TYPE) instance;
      }

      try {
        final TYPE newInstance = newProxy(invocationConfiguration, callConfiguration);
        proxyMap.put(classInfo, newInstance);
        return newInstance;

      } catch (final Throwable t) {
        InterruptedInvocationException.throwIfInterrupt(t);
        throw new IllegalArgumentException(t);
      }
    }
  }

  @NotNull
  public CallConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>> callConfiguration() {
    final CallConfiguration config = mCallConfiguration;
    return new CallConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
  }

  @NotNull
  public InvocationConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>>
  invocationConfiguration() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
  }

  /**
   * Returns the builder proxy class.
   *
   * @return the proxy class.
   */
  @NotNull
  protected abstract Class<? super TYPE> getInterfaceClass();

  /**
   * Returns the builder target object.
   *
   * @return the target object.
   */
  @Nullable
  protected abstract Object getTarget();

  /**
   * Creates and return a new proxy instance.
   *
   * @param invocationConfiguration the invocation configuration.
   * @param callConfiguration       the call configuration.
   * @return the proxy instance.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
      @NotNull CallConfiguration callConfiguration) throws Exception;

  /**
   * Class used as key to identify a specific proxy instance.
   */
  private static class ClassInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param itf                     the proxy interface class.
     * @param invocationConfiguration the invocation configuration.
     * @param callConfiguration       the call configuration.
     */
    private ClassInfo(@NotNull final Class<?> itf,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final CallConfiguration callConfiguration) {
      super(asArgs(itf, invocationConfiguration, callConfiguration));
    }
  }
}
