/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.android.proxy.builder;

import android.content.Context;

import com.github.dm.jrt.android.builder.ServiceConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Abstract implementation of a builder of async proxy objects, whose methods are executed in a
 * dedicated service.
 * <p/>
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
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

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
    withInvocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>> withProxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyObjectBuilder<TYPE>>(this, config);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the service configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyObjectBuilder<TYPE> setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>> withService() {

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
     * Returns the context on which the invocation is based.
     * <br/>
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
     */
    @NotNull
    protected abstract TYPE newProxy(@NotNull InvocationConfiguration invocationConfiguration,
            @NotNull ProxyConfiguration proxyConfiguration,
            @NotNull ServiceConfiguration serviceConfiguration);

    /**
     * Class used as key to identify a specific proxy instance.
     */
    private static class ProxyInfo {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final ServiceConfiguration mServiceConfiguration;

        private final Type mType;

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

            mType = itf;
            mInvocationConfiguration = invocationConfiguration;
            mProxyConfiguration = proxyConfiguration;
            mServiceConfiguration = serviceConfiguration;
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
                    && mProxyConfiguration.equals(proxyInfo.mProxyConfiguration)
                    && mServiceConfiguration.equals(proxyInfo.mServiceConfiguration)
                    && mType.equals(proxyInfo.mType);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mInvocationConfiguration.hashCode();
            result = 31 * result + mProxyConfiguration.hashCode();
            result = 31 * result + mServiceConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }
    }
}
