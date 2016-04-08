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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Abstract implementation of a builder of async proxy objects.
 * <p>
 * Created by davide-maestroni on 02/26/2015.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractProxyObjectBuilder<TYPE> implements ProxyObjectBuilder<TYPE>,
        InvocationConfiguration.Configurable<ProxyObjectBuilder<TYPE>>,
        ProxyConfiguration.Configurable<ProxyObjectBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sProxies =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

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
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final ClassInfo classInfo =
                    new ClassInfo(getInterfaceClass(), invocationConfiguration, proxyConfiguration);
            final Object instance = proxyMap.get(classInfo);
            if (instance != null) {
                return (TYPE) instance;
            }

            try {
                final TYPE newInstance = newProxy(invocationConfiguration, proxyConfiguration);
                proxyMap.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>>
    getInvocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ProxyObjectBuilder<TYPE>> getProxyConfiguration() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    public ProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
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
     * @param proxyConfiguration      the proxy configuration.
     * @return the proxy instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
            @NotNull ProxyConfiguration proxyConfiguration) throws Exception;

    /**
     * Class used as key to identify a specific proxy instance.
     */
    private static class ClassInfo {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param itf                     the proxy interface class.
         * @param invocationConfiguration the invocation configuration.
         * @param proxyConfiguration      the proxy configuration.
         */
        private ClassInfo(@NotNull final Class<?> itf,
                @NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration) {

            mType = itf;
            mInvocationConfiguration = invocationConfiguration;
            mProxyConfiguration = proxyConfiguration;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mProxyConfiguration.hashCode();
            result = 31 * result + mInvocationConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof ClassInfo)) {
                return false;
            }

            final ClassInfo classInfo = (ClassInfo) o;
            return mProxyConfiguration.equals(classInfo.mProxyConfiguration)
                    && mInvocationConfiguration.equals(classInfo.mInvocationConfiguration)
                    && mType.equals(classInfo.mType);
        }
    }
}
