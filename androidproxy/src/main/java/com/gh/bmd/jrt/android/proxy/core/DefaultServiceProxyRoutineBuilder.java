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
package com.gh.bmd.jrt.android.proxy.core;

import android.content.Context;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.proxy.annotation.ServiceProxy;
import com.gh.bmd.jrt.android.proxy.builder.AbstractServiceProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.proxy.annotation.Proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of a service proxy builder.
 * <p/>
 * Created by davide-maestroni on 13/05/15.
 */
class DefaultServiceProxyRoutineBuilder implements ServiceProxyRoutineBuilder,
        RoutineConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceProxyRoutineBuilder> {

    private final WeakReference<Context> mContextReference;

    private final Class<?> mTargetClass;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context     the invocation context.
     * @param targetClass the target class.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultServiceProxyRoutineBuilder(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the invocation context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mTargetClass = targetClass;
        mContextReference = new WeakReference<Context>(context);
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

        if (itfClass.getAnnotation(ServiceProxy.class) == null) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + ServiceProxy.class.getName()
                            + ": " + itfClass.getName());
        }

        final Object context = mContextReference.get();

        if (context == null) {

            throw new IllegalStateException("the context object has been destroyed");
        }

        final ObjectContextProxyBuilder<TYPE> builder =
                new ObjectContextProxyBuilder<TYPE>(context, mTargetClass, itf);
        return builder.withRoutine()
                      .with(mRoutineConfiguration)
                      .set()
                      .withProxy()
                      .with(mProxyConfiguration)
                      .set()
                      .withService()
                      .with(mServiceConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceProxyRoutineBuilder> withService() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyRoutineBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyRoutineBuilder setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectContextProxyBuilder<TYPE> extends AbstractServiceProxyBuilder<TYPE> {

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
        private ObjectContextProxyBuilder(@Nonnull final Object context,
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
                @Nonnull final ServiceConfiguration serviceConfiguration) {

            try {

                final Object context = mContext;
                final Class<?> targetClass = mTargetClass;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final ServiceProxy annotation = interfaceClass.getAnnotation(ServiceProxy.class);
                String packageName = annotation.generatedClassPackage();

                if (packageName.equals(Proxy.DEFAULT)) {

                    final Package classPackage = interfaceClass.getPackage();
                    packageName = (classPackage != null) ? classPackage.getName() + "." : "";

                } else {

                    packageName += ".";
                }

                String className = annotation.generatedClassName();

                if (className.equals(Proxy.DEFAULT)) {

                    className = interfaceClass.getSimpleName();
                    Class<?> enclosingClass = interfaceClass.getEnclosingClass();

                    while (enclosingClass != null) {

                        className = enclosingClass.getSimpleName() + "_" + className;
                        enclosingClass = enclosingClass.getEnclosingClass();
                    }
                }

                final String fullClassName =
                        packageName + annotation.generatedClassPrefix() + className
                                + annotation.generatedClassSuffix();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(fullClassName), context, targetClass,
                                        routineConfiguration, proxyConfiguration,
                                        serviceConfiguration);
                return interfaceClass.cast(
                        constructor.newInstance(context, targetClass, routineConfiguration,
                                                proxyConfiguration, serviceConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
