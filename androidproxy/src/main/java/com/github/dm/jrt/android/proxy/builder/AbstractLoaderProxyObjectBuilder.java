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
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Abstract implementation of a builder of async proxy objects, bound to a context lifecycle.
 * <p>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractLoaderProxyObjectBuilder<TYPE>
        implements LoaderProxyObjectBuilder<TYPE>,
        InvocationConfiguration.Configurable<LoaderProxyObjectBuilder<TYPE>>,
        ProxyConfiguration.Configurable<LoaderProxyObjectBuilder<TYPE>>,
        LoaderConfiguration.Configurable<LoaderProxyObjectBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
            sContextProxies =
            new WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

    @NotNull
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
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
            final ProxyInfo proxyInfo =
                    new ProxyInfo(getInterfaceClass(), invocationConfiguration, proxyConfiguration,
                            loaderConfiguration);
            final Object instance = proxies.get(proxyInfo);
            if (instance != null) {
                return (TYPE) instance;
            }

            final Runner asyncRunner = invocationConfiguration.getRunnerOr(null);
            if (asyncRunner != null) {
                invocationConfiguration.newLogger(this)
                                       .wrn("the specified runner will be ignored: %s",
                                               asyncRunner);
            }

            try {
                final TYPE newInstance =
                        newProxy(invocationConfiguration, proxyConfiguration, loaderConfiguration);
                proxies.put(proxyInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> getInvocationConfiguration() {



        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> getProxyConfiguration() {



        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> getLoaderConfiguration() {



        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public LoaderProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderProxyObjectBuilder<TYPE> setConfiguration(
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
     * Returns the context or component (Activity, Fragment, etc.) on which the invocation is based.
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
     * @param proxyConfiguration      the proxy configuration.
     * @param loaderConfiguration     the loader configuration.
     * @return the proxy instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
            @NotNull ProxyConfiguration proxyConfiguration,
            @NotNull LoaderConfiguration loaderConfiguration) throws Exception;

    /**
     * Class used as key to identify a specific proxy instance.
     */
    private static class ProxyInfo {

        private final InvocationConfiguration mInvocationConfiguration;

        private final LoaderConfiguration mLoaderConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param itf                     the proxy interface class.
         * @param invocationConfiguration the invocation configuration.
         * @param proxyConfiguration      the proxy configuration.
         * @param loaderConfiguration     the loader configuration.
         */
        private ProxyInfo(@NotNull final Class<?> itf,
                @NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final LoaderConfiguration loaderConfiguration) {

            mType = itf;
            mInvocationConfiguration = invocationConfiguration;
            mProxyConfiguration = proxyConfiguration;
            mLoaderConfiguration = loaderConfiguration;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof ProxyInfo)) {
                return false;
            }

            final ProxyInfo proxyInfo = (ProxyInfo) o;
            return mInvocationConfiguration.equals(proxyInfo.mInvocationConfiguration)
                    && mLoaderConfiguration.equals(proxyInfo.mLoaderConfiguration)
                    && mProxyConfiguration.equals(proxyInfo.mProxyConfiguration) && mType.equals(
                    proxyInfo.mType);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mInvocationConfiguration.hashCode();
            result = 31 * result + mLoaderConfiguration.hashCode();
            result = 31 * result + mProxyConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }
    }
}
