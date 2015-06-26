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

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    @Nonnull
    public TYPE buildProxy() {

        synchronized (sClassMap) {

            final Object target = getTargetClass();
            final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
            final ClassToken<TYPE> token = getInterfaceToken();
            final ClassInfo classInfo =
                    new ClassInfo(token, invocationConfiguration, proxyConfiguration,
                                  loaderConfiguration);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            warn(invocationConfiguration);

            try {

                final TYPE newInstance =
                        newProxy(invocationConfiguration, proxyConfiguration, loaderConfiguration);
                classes.put(classInfo, newInstance);
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
    public LoaderConfiguration.Builder<? extends LoaderProxyBuilder<TYPE>> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderProxyBuilder<TYPE>> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyBuilder<TYPE>>(this, config);
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
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the invocation configuration.
     */
    private void warn(@Nonnull final InvocationConfiguration configuration) {

        final Runner asyncRunner = configuration.getAsyncRunnerOr(null);

        if (asyncRunner != null) {

            configuration.newLogger(this)
                         .wrn("the specified runner will be ignored: %s", asyncRunner);
        }
    }

    /**
     * Class used as key to identify a specific proxy instance.
     */
    private static class ClassInfo {

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
        private ClassInfo(@Nonnull final ClassToken<?> token,
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

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassInfo)) {

                return false;
            }

            final ClassInfo classInfo = (ClassInfo) o;
            return mLoaderConfiguration.equals(classInfo.mLoaderConfiguration)
                    && mProxyConfiguration.equals(classInfo.mProxyConfiguration)
                    && mInvocationConfiguration.equals(classInfo.mInvocationConfiguration)
                    && mType.equals(classInfo.mType);
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mLoaderConfiguration.hashCode();
            result = 31 * result + mProxyConfiguration.hashCode();
            result = 31 * result + mInvocationConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }
    }
}
