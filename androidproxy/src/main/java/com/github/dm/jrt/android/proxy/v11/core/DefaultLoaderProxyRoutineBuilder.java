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
package com.github.dm.jrt.android.proxy.v11.core;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.proxy.annotation.V11Proxy;
import com.github.dm.jrt.android.proxy.builder.AbstractLoaderProxyBuilder;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.util.ClassToken;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.Reflection.findConstructor;

/**
 * Default implementation of a context proxy builder.
 * <p/>
 * Created by davide-maestroni on 05/06/2015.
 */
class DefaultLoaderProxyRoutineBuilder implements LoaderProxyRoutineBuilder,
        InvocationConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        LoaderConfiguration.Configurable<LoaderProxyRoutineBuilder> {

    private final LoaderContext mContext;

    private final ContextInvocationTarget mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the routine context.
     * @param target  the invocation target.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderProxyRoutineBuilder(@Nonnull final LoaderContext context,
            @Nonnull final ContextInvocationTarget target) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (target == null) {

            throw new NullPointerException("the invocation target must not be null");
        }

        mContext = context;
        mTarget = target;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        return buildProxy(ClassToken.tokenOf(itf));
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        final Class<TYPE> itfClass = itf.getRawClass();

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itfClass.getName());
        }

        if (!itfClass.isAnnotationPresent(V11Proxy.class)) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + V11Proxy.class.getName() + ": "
                            + itfClass.getName());
        }

        final ObjectLoaderProxyBuilder<TYPE> builder =
                new ObjectLoaderProxyBuilder<TYPE>(mContext, mTarget, itf);
        return builder.invocations()
                      .with(mInvocationConfiguration)
                      .set()
                      .proxies()
                      .with(mProxyConfiguration)
                      .set()
                      .loaders()
                      .with(mLoaderConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends LoaderProxyRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderProxyRoutineBuilder> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyRoutineBuilder setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectLoaderProxyBuilder<TYPE> extends AbstractLoaderProxyBuilder<TYPE> {

        private final LoaderContext mContext;

        private final ClassToken<TYPE> mInterfaceToken;

        private final ContextInvocationTarget mTarget;

        /**
         * Constructor.
         *
         * @param context        the routine context.
         * @param target         the invocation target.
         * @param interfaceToken the proxy interface token.
         */
        private ObjectLoaderProxyBuilder(@Nonnull final LoaderContext context,
                @Nonnull final ContextInvocationTarget target,
                @Nonnull final ClassToken<TYPE> interfaceToken) {

            mContext = context;
            mTarget = target;
            mInterfaceToken = interfaceToken;
        }

        @Nonnull
        @Override
        protected ClassToken<TYPE> getInterfaceToken() {

            return mInterfaceToken;
        }

        @Nullable
        @Override
        protected Object getInvocationContext() {

            return mContext.getComponent();
        }

        @Nonnull
        @Override
        protected Class<?> getTargetClass() {

            return mTarget.getTargetClass();
        }

        @Nonnull
        @Override
        protected TYPE newProxy(@Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final LoaderConfiguration loaderConfiguration) {

            try {

                final LoaderContext context = mContext;
                final ContextInvocationTarget target = mTarget;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final V11Proxy annotation = interfaceClass.getAnnotation(V11Proxy.class);
                String packageName = annotation.classPackage();

                if (packageName.equals(Proxy.DEFAULT)) {

                    final Package classPackage = interfaceClass.getPackage();
                    packageName = (classPackage != null) ? classPackage.getName() + "." : "";

                } else {

                    packageName += ".";
                }

                String className = annotation.className();

                if (className.equals(Proxy.DEFAULT)) {

                    className = interfaceClass.getSimpleName();
                    Class<?> enclosingClass = interfaceClass.getEnclosingClass();

                    while (enclosingClass != null) {

                        className = enclosingClass.getSimpleName() + "_" + className;
                        enclosingClass = enclosingClass.getEnclosingClass();
                    }
                }

                final String fullClassName = packageName + annotation.classPrefix() + className
                        + annotation.classSuffix();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(fullClassName), context, target,
                                        invocationConfiguration, proxyConfiguration,
                                        loaderConfiguration);
                return interfaceClass.cast(
                        constructor.newInstance(context, target, invocationConfiguration,
                                                proxyConfiguration, loaderConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}