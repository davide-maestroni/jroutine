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

package com.github.dm.jrt.android.proxy;

import android.content.Context;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.AbstractServiceProxyObjectBuilder;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.config.ProxyConfiguration;
import com.github.dm.jrt.proxy.annotation.Proxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

import static com.github.dm.jrt.core.util.Reflection.findConstructor;

/**
 * Default implementation of a service proxy builder.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 */
class DefaultServiceProxyRoutineBuilder implements ServiceProxyRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceProxyRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceProxyRoutineBuilder> {

    private final ServiceContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

    private ServiceConfiguration mServiceConfiguration =
            ServiceConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the service context.
     * @param target  the invocation target.
     */
    DefaultServiceProxyRoutineBuilder(@NotNull final ServiceContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("service context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        if (!itf.isInterface()) {
            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        if (!itf.isAnnotationPresent(ServiceProxy.class)) {
            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + ServiceProxy.class.getName()
                            + ": " + itf.getName());
        }

        final TargetServiceProxyObjectBuilder<TYPE> builder =
                new TargetServiceProxyObjectBuilder<TYPE>(mContext, mTarget, itf);
        return builder.getInvocationConfiguration()
                      .with(mInvocationConfiguration)
                      .setConfiguration()
                      .getProxyConfiguration()
                      .with(mProxyConfiguration)
                      .setConfiguration()
                      .getServiceConfiguration()
                      .with(mServiceConfiguration)
                      .setConfiguration()
                      .buildProxy();
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ServiceProxyRoutineBuilder>
    getInvocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ServiceProxyRoutineBuilder> getProxyConfiguration
            () {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceProxyRoutineBuilder>
    getServiceConfiguration() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceProxyRoutineBuilder>(this, config);
    }

    @NotNull
    public ServiceProxyRoutineBuilder setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceProxyRoutineBuilder setConfiguration(
            @NotNull final ServiceConfiguration configuration) {

        mServiceConfiguration = ConstantConditions.notNull("service configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceProxyRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class TargetServiceProxyObjectBuilder<TYPE>
            extends AbstractServiceProxyObjectBuilder<TYPE> {

        private final ServiceContext mContext;

        private final Class<? super TYPE> mInterfaceClass;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param context        the service context.
         * @param target         the invocation target.
         * @param interfaceClass the proxy interface class.
         */
        private TargetServiceProxyObjectBuilder(@NotNull final ServiceContext context,
                @NotNull final ContextInvocationTarget<?> target,
                @NotNull final Class<? super TYPE> interfaceClass) {

            mContext = context;
            mTarget = target;
            mInterfaceClass = interfaceClass;
        }

        @NotNull
        @Override
        protected Class<? super TYPE> getInterfaceClass() {

            return mInterfaceClass;
        }

        @Nullable
        @Override
        protected Context getInvocationContext() {

            return mContext.getServiceContext();
        }

        @NotNull
        @Override
        protected Class<?> getTargetClass() {

            return mTarget.getTargetClass();
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected TYPE newProxy(@NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ServiceConfiguration serviceConfiguration) throws Exception {

            final ServiceContext context = mContext;
            final ContextInvocationTarget<?> target = mTarget;
            final Class<? super TYPE> interfaceClass = mInterfaceClass;
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

            final String fullClassName =
                    packageName + annotation.classPrefix() + className + annotation.classSuffix();
            final Constructor<?> constructor =
                    findConstructor(Class.forName(fullClassName), context, target,
                            invocationConfiguration, proxyConfiguration, serviceConfiguration);
            return (TYPE) constructor.newInstance(context, target, invocationConfiguration,
                    proxyConfiguration, serviceConfiguration);
        }
    }
}
