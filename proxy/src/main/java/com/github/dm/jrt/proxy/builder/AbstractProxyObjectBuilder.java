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
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.reflect.InvocationTarget;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;

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

  private final ScheduledExecutor mExecutor;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  protected AbstractProxyObjectBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public TYPE proxyOf(@NotNull final InvocationTarget<?> target) {
    final Class<?> targetClass = target.getTargetClass();
    if (targetClass.isInterface()) {
      throw new IllegalArgumentException(
          "the target class must not be an interface: " + targetClass.getName());
    }

    synchronized (sProxies) {
      final Object proxyTarget = target.getTarget();
      final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> proxies = sProxies;
      HashMap<ClassInfo, Object> proxyMap = proxies.get(proxyTarget);
      if (proxyMap == null) {
        proxyMap = new HashMap<ClassInfo, Object>();
        proxies.put(proxyTarget, proxyMap);
      }

      final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
      final WrapperConfiguration wrapperConfiguration = mWrapperConfiguration;
      final ClassInfo classInfo =
          new ClassInfo(getInterfaceClass(), invocationConfiguration, wrapperConfiguration);
      final Object instance = proxyMap.get(classInfo);
      if (instance != null) {
        return (TYPE) instance;
      }

      try {
        final TYPE newInstance =
            newProxy(target, mExecutor, invocationConfiguration, wrapperConfiguration);
        proxyMap.put(classInfo, newInstance);
        return newInstance;

      } catch (final Throwable t) {
        InterruptedInvocationException.throwIfInterrupt(t);
        throw new IllegalArgumentException(t);
      }
    }
  }

  @NotNull
  public ProxyObjectBuilder<TYPE> withConfiguration(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  public ProxyObjectBuilder<TYPE> withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  public InvocationConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>> withInvocation() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
  }

  @NotNull
  public WrapperConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>> withWrapper() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
  }

  /**
   * Returns the builder proxy class.
   *
   * @return the proxy class.
   */
  @NotNull
  protected abstract Class<? super TYPE> getInterfaceClass();

  /**
   * Creates and return a new proxy instance.
   *
   * @param target                  the invocation target.
   * @param executor                the executor instance.
   * @param invocationConfiguration the invocation configuration.
   * @param wrapperConfiguration    the wrapper configuration.
   * @return the proxy instance.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract TYPE newProxy(@NotNull InvocationTarget<?> target,
      @NotNull ScheduledExecutor executor, @NotNull InvocationConfiguration invocationConfiguration,
      @NotNull WrapperConfiguration wrapperConfiguration) throws Exception;

  /**
   * Class used as key to identify a specific proxy instance.
   */
  private static class ClassInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param itf                     the proxy interface class.
     * @param invocationConfiguration the invocation configuration.
     * @param wrapperConfiguration    the wrapper configuration.
     */
    private ClassInfo(@NotNull final Class<?> itf,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final WrapperConfiguration wrapperConfiguration) {
      super(asArgs(itf, invocationConfiguration, wrapperConfiguration));
    }
  }
}
