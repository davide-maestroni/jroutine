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
package com.gh.bmd.jrt.android.proxy.v11.core;

import android.app.Activity;
import android.app.Fragment;

import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.proxy.annotation.V11Proxy;
import com.gh.bmd.jrt.android.proxy.builder.AbstractLoaderProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.proxy.annotation.Proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of a context proxy builder.
 * <p/>
 * Created by davide on 06/05/15.
 */
class DefaultLoaderProxyRoutineBuilder implements LoaderProxyRoutineBuilder,
        RoutineConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderProxyRoutineBuilder>,
        LoaderConfiguration.Configurable<LoaderProxyRoutineBuilder> {

    private final WeakReference<?> mContextReference;

    private final Class<?> mTargetClass;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity    the invocation activity context.
     * @param targetClass the target class.
     */
    DefaultLoaderProxyRoutineBuilder(@Nonnull final Activity activity,
            @Nonnull final Class<?> targetClass) {

        this((Object) activity, targetClass);
    }

    /**
     * Constructor.
     *
     * @param fragment    the invocation fragment context.
     * @param targetClass the target class.
     */
    DefaultLoaderProxyRoutineBuilder(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> targetClass) {

        this((Object) fragment, targetClass);
    }

    /**
     * Constructor.
     *
     * @param context     the invocation context.
     * @param targetClass the target class.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultLoaderProxyRoutineBuilder(@Nonnull final Object context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the invocation context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mTargetClass = targetClass;
        mContextReference = new WeakReference<Object>(context);
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

        if (itfClass.getAnnotation(V11Proxy.class) == null) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + V11Proxy.class.getName() + ": "
                            + itfClass.getName());
        }

        final Object context = mContextReference.get();

        if (context == null) {

            throw new IllegalStateException("the context object has been destroyed");
        }

        final ObjectLoaderProxyBuilder<TYPE> builder =
                new ObjectLoaderProxyBuilder<TYPE>(context, mTargetClass, itf);
        return builder.withRoutine()
                      .with(mRoutineConfiguration)
                      .set()
                      .withProxy()
                      .with(mProxyConfiguration)
                      .set()
                      .withLoader()
                      .with(mLoaderConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withLoader() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderProxyRoutineBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
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

            throw new NullPointerException("the proxy configuration must not be null");
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

        private final Object mContext;

        private final ClassToken<TYPE> mInterfaceToken;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param context        the invocation context.
         * @param targetClass    the proxy target class.
         * @param interfaceToken the proxy interface token.
         */
        private ObjectLoaderProxyBuilder(@Nonnull final Object context,
                @Nonnull final Class<?> targetClass,
                @Nonnull final ClassToken<TYPE> interfaceToken) {

            mContext = context;
            mTargetClass = targetClass;
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
        protected TYPE newProxy(@Nonnull final RoutineConfiguration routineConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final LoaderConfiguration loaderConfiguration) {

            try {

                final Object context = mContext;
                final Class<?> targetClass = mTargetClass;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final V11Proxy annotation = interfaceClass.getAnnotation(V11Proxy.class);
                String packageName = annotation.generatedClassPackage();

                if (packageName.equals(Proxy.DEFAULT)) {

                    final Package classPackage = interfaceClass.getPackage();
                    packageName = (classPackage != null) ? classPackage.getName() + "." : "";
                }

                String className = annotation.generatedClassName();

                if (className.equals(Proxy.DEFAULT)) {

                    className = interfaceClass.getSimpleName();
                    Class<?> enclosingClass = interfaceClass.getEnclosingClass();

                    while (enclosingClass != null) {

                        className = enclosingClass.getSimpleName() + className;
                        enclosingClass = enclosingClass.getEnclosingClass();
                    }
                }

                final String fullClassName =
                        packageName + annotation.generatedClassPrefix() + className
                                + annotation.generatedClassSuffix();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(fullClassName), context, targetClass,
                                        routineConfiguration, proxyConfiguration,
                                        loaderConfiguration);
                return interfaceClass.cast(
                        constructor.newInstance(context, targetClass, routineConfiguration,
                                                proxyConfiguration, loaderConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
