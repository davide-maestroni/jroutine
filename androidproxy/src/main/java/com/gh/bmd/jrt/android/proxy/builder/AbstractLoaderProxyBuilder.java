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
package com.gh.bmd.jrt.android.proxy.builder;

import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.lang.reflect.Type;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract implementation of a builder of async proxy objects, bound to a context lifecycle.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractLoaderProxyBuilder<TYPE> implements LoaderProxyBuilder<TYPE>,
        InvocationConfiguration.Configurable<LoaderProxyBuilder<TYPE>>,
        ProxyConfiguration.Configurable<LoaderProxyBuilder<TYPE>>,
        LoaderConfiguration.Configurable<LoaderProxyBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
            sContextProxyMap =
            new WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    @Nonnull
    public TYPE buildProxy() {

        synchronized (sContextProxyMap) {

            final Object context = getInvocationContext();

            if (context == null) {

                throw new NullPointerException("the invocation context has been destroyed");
            }

            final WeakIdentityHashMap<Object, HashMap<Class<?>, HashMap<ProxyInfo, Object>>>
                    contextProxyMap = sContextProxyMap;
            HashMap<Class<?>, HashMap<ProxyInfo, Object>> proxyMap = contextProxyMap.get(context);

            if (proxyMap == null) {

                proxyMap = new HashMap<Class<?>, HashMap<ProxyInfo, Object>>();
                contextProxyMap.put(context, proxyMap);
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
            final ClassToken<TYPE> token = getInterfaceToken();
            final ProxyInfo proxyInfo =
                    new ProxyInfo(token, invocationConfiguration, proxyConfiguration,
                                  loaderConfiguration);
            final Object instance = proxies.get(proxyInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            final Runner asyncRunner = invocationConfiguration.getAsyncRunnerOr(null);

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

    @Nonnull
    public InvocationConfiguration.Builder<? extends LoaderProxyBuilder<TYPE>> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderProxyBuilder<TYPE>> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderProxyBuilder<TYPE>> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyBuilder<TYPE> setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyBuilder<TYPE> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyBuilder<TYPE> setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    /**
     * Returns the builder proxy class token.
     *
     * @return the proxy class token.
     */
    @Nonnull
    protected abstract ClassToken<TYPE> getInterfaceToken();

    /**
     * Returns the context or component (Activity, Fragment, etc.) on which the invocation is based.
     * <br/>
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
    @Nonnull
    protected abstract Class<?> getTargetClass();

    /**
     * Creates and return a new proxy instance.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param proxyConfiguration      the proxy configuration.
     * @param loaderConfiguration     the loader configuration.
     * @return the proxy instance.
     */
    @Nonnull
    protected abstract TYPE newProxy(@Nonnull InvocationConfiguration invocationConfiguration,
            @Nonnull ProxyConfiguration proxyConfiguration,
            @Nonnull LoaderConfiguration loaderConfiguration);

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
         * @param token                   the proxy interface token.
         * @param invocationConfiguration the invocation configuration.
         * @param proxyConfiguration      the proxy configuration.
         * @param loaderConfiguration     the loader configuration.
         */
        private ProxyInfo(@Nonnull final ClassToken<?> token,
                @Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final LoaderConfiguration loaderConfiguration) {

            mType = token.getRawClass();
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
