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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a builder of async proxy objects, whose methods are executed in a
 * dedicated service.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractServiceProxyObjectBuilder<TYPE>
        implements ServiceProxyObjectBuilder<TYPE>,
        InvocationConfiguration.Configurable<ServiceProxyObjectBuilder<TYPE>>,
        ProxyConfiguration.Configurable<ServiceProxyObjectBuilder<TYPE>>,
        ServiceConfiguration.Configurable<ServiceProxyObjectBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Context, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
            sContextProxies =
            new WeakIdentityHashMap<Context, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

    private ServiceConfiguration mServiceConfiguration =
            ServiceConfiguration.defaultConfiguration();

    @NotNull
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
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
            final ProxyInfo proxyInfo =
                    new ProxyInfo(getInterfaceClass(), invocationConfiguration, proxyConfiguration,
                            serviceConfiguration);
            final Object instance = proxies.get(proxyInfo);
            if (instance != null) {
                return (TYPE) instance;
            }

            try {
                final TYPE newInstance =
                        newProxy(invocationConfiguration, proxyConfiguration, serviceConfiguration);
                proxies.put(proxyInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
    getInvocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
    getProxyConfiguration() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>>
    getServiceConfiguration() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ServiceConfiguration configuration) {

        mServiceConfiguration = ConstantConditions.notNull("service configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    /**
     * Returns the builder proxy class.
     *
     * @return the proxy class.
     */
    @NotNull
    protected abstract Class<? super TYPE> getInterfaceClass();

    /**
     * Returns the context on which the invocation is based.
     * <br>
     * Returning null means that the context has been destroyed, so an exception will be thrown.
     *
     * @return the invocation context.
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
     * @param proxyConfiguration      the proxy configuration.
     * @param serviceConfiguration    the service configuration.
     * @return the proxy instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
            @NotNull ProxyConfiguration proxyConfiguration,
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
         * @param proxyConfiguration      the proxy configuration.
         * @param serviceConfiguration    the service configuration.
         */
        private ProxyInfo(@NotNull final Class<?> itf,
                @NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ServiceConfiguration serviceConfiguration) {

            super(asArgs(itf, invocationConfiguration, proxyConfiguration, serviceConfiguration));
        }
    }
}
