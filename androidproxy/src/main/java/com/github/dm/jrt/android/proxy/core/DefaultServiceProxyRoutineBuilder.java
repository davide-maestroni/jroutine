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
package com.github.dm.jrt.android.proxy.core;

import android.content.Context;

import com.github.dm.jrt.android.builder.ServiceConfiguration;
import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.AbstractServiceProxyBuilder;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.util.ClassToken;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.Reflection.findConstructor;

/**
 * Default implementation of a service proxy builder.
 * <p/>
 * Created by davide-maestroni on 05/13/2015.
 */
class DefaultServiceProxyRoutineBuilder implements ServiceProxyRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceProxyRoutineBuilder> {

    private final ServiceContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the service context.
     * @param target  the invocation target.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultServiceProxyRoutineBuilder(@Nonnull final ServiceContext context,
            @Nonnull final ContextInvocationTarget<?> target) {

        if (context == null) {

            throw new NullPointerException("the invocation context must not be null");
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

        if (!itfClass.isAnnotationPresent(ServiceProxy.class)) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + ServiceProxy.class.getName()
                            + ": " + itfClass.getName());
        }

        final ObjectServiceProxyBuilder<TYPE> builder =
                new ObjectServiceProxyBuilder<TYPE>(mContext, mTarget, itf);
        return builder.invocations()
                      .with(mInvocationConfiguration)
                      .set()
                      .proxies()
                      .with(mProxyConfiguration)
                      .set()
                      .service()
                      .with(mServiceConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ServiceProxyRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceProxyRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceProxyRoutineBuilder> service() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
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

            throw new NullPointerException("the service configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectServiceProxyBuilder<TYPE> extends AbstractServiceProxyBuilder<TYPE> {

        private final ServiceContext mContext;

        private final ClassToken<TYPE> mInterfaceToken;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param context        the service context.
         * @param target         the invocation target.
         * @param interfaceToken the proxy interface token.
         */
        private ObjectServiceProxyBuilder(@Nonnull final ServiceContext context,
                @Nonnull final ContextInvocationTarget<?> target,
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
        protected Context getInvocationContext() {

            return mContext.getServiceContext();
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
                @Nonnull final ServiceConfiguration serviceConfiguration) {

            try {

                final ServiceContext context = mContext;
                final ContextInvocationTarget<?> target = mTarget;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final ServiceProxy annotation = interfaceClass.getAnnotation(ServiceProxy.class);
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
                                        serviceConfiguration);
                return interfaceClass.cast(
                        constructor.newInstance(context, target, invocationConfiguration,
                                                proxyConfiguration, serviceConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
