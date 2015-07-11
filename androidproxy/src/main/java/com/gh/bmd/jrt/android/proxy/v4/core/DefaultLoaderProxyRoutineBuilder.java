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
package com.gh.bmd.jrt.android.proxy.v4.core;

import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.proxy.annotation.V4Proxy;
import com.gh.bmd.jrt.android.proxy.builder.AbstractLoaderProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.gh.bmd.jrt.android.v4.core.RoutineContext;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.proxy.annotation.Proxy;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.Reflection.findConstructor;

/**
 * Default implementation of a context proxy builder.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 */
class DefaultLoaderProxyRoutineBuilder implements LoaderProxyRoutineBuilder,
        InvocationConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        LoaderConfiguration.Configurable<LoaderProxyRoutineBuilder> {

    private final RoutineContext mContext;

    private final Object[] mFactoryArgs;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param targetClass the target class.
     * @param factoryArgs the object factory arguments.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderProxyRoutineBuilder(@Nonnull final RoutineContext context,
            @Nonnull final Class<?> targetClass, @Nullable final Object... factoryArgs) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mContext = context;
        mTargetClass = targetClass;
        mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
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

        if (itfClass.getAnnotation(V4Proxy.class) == null) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + V4Proxy.class.getName() + ": "
                            + itfClass.getName());
        }

        final RoutineContext context = mContext;

        if (context.getComponent() == null) {

            throw new IllegalStateException("the context object has been destroyed");
        }

        final ObjectLoaderProxyBuilder<TYPE> builder =
                new ObjectLoaderProxyBuilder<TYPE>(context, mTargetClass, mFactoryArgs, itf);
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
    public LoaderConfiguration.Builder<? extends LoaderProxyRoutineBuilder> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
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

        private final RoutineContext mContext;

        private final Object[] mFactoryArgs;

        private final ClassToken<TYPE> mInterfaceToken;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param context        the routine context.
         * @param targetClass    the proxy target class.
         * @param factoryArgs    the object factory arguments.
         * @param interfaceToken the proxy interface token.
         */
        private ObjectLoaderProxyBuilder(@Nonnull final RoutineContext context,
                @Nonnull final Class<?> targetClass, @Nonnull final Object[] factoryArgs,
                @Nonnull final ClassToken<TYPE> interfaceToken) {

            mContext = context;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mInterfaceToken = interfaceToken;
        }

        @Nonnull
        @Override
        protected ClassToken<TYPE> getInterfaceToken() {

            return mInterfaceToken;
        }

        @Nonnull
        @Override
        protected Class<?> getTargetClass() {

            return mTargetClass;
        }

        @Nonnull
        @Override
        protected TYPE newProxy(@Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final LoaderConfiguration loaderConfiguration) {

            try {

                final RoutineContext context = mContext;
                final Class<?> targetClass = mTargetClass;
                final Object[] factoryArgs = mFactoryArgs;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final V4Proxy annotation = interfaceClass.getAnnotation(V4Proxy.class);
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
                        findConstructor(Class.forName(fullClassName), context, targetClass,
                                        factoryArgs, invocationConfiguration, proxyConfiguration,
                                        loaderConfiguration);
                return interfaceClass.cast(
                        constructor.newInstance(context, targetClass, factoryArgs,
                                                invocationConfiguration, proxyConfiguration,
                                                loaderConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
