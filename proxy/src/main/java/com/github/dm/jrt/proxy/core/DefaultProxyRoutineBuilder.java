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

package com.github.dm.jrt.proxy.core;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.object.builder.ProxyConfiguration;
import com.github.dm.jrt.object.core.InvocationTarget;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.proxy.builder.AbstractProxyObjectBuilder;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

import static com.github.dm.jrt.core.util.Reflection.findConstructor;

/**
 * Default implementation of a proxy builder.
 * <p/>
 * Created by davide-maestroni on 03/23/2015.
 */
class DefaultProxyRoutineBuilder
        implements ProxyRoutineBuilder, InvocationConfiguration.Configurable<ProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ProxyRoutineBuilder> {

    private final InvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param target the invocation target.
     * @throws java.lang.IllegalArgumentException if the class of specified target represents an
     *                                            interface.
     */
    DefaultProxyRoutineBuilder(@NotNull final InvocationTarget<?> target) {

        final Class<?> targetClass = target.getTargetClass();
        if (targetClass.isInterface()) {
            throw new IllegalArgumentException(
                    "the target class must not be an interface: " + targetClass.getName());
        }

        mTarget = target;
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        if (!itf.isInterface()) {
            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        if (!itf.isAnnotationPresent(Proxy.class)) {
            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + Proxy.class.getName() + ": "
                            + itf.getName());
        }

        final TargetProxyObjectBuilder<TYPE> builder =
                new TargetProxyObjectBuilder<TYPE>(mTarget, itf);
        return builder.withInvocations()
                      .with(mInvocationConfiguration)
                      .getConfigured()
                      .withProxies()
                      .with(mProxyConfiguration)
                      .getConfigured()
                      .buildProxy();
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ProxyRoutineBuilder setConfiguration(@NotNull final ProxyConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ProxyRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ProxyRoutineBuilder> withInvocations() {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ProxyRoutineBuilder>(this, configuration);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ProxyRoutineBuilder> withProxies() {

        final ProxyConfiguration configuration = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ProxyRoutineBuilder>(this, configuration);
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class TargetProxyObjectBuilder<TYPE> extends AbstractProxyObjectBuilder<TYPE> {

        private final Class<? super TYPE> mInterfaceClass;

        private final InvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param target         the invocation target.
         * @param interfaceClass the proxy interface class.
         */
        private TargetProxyObjectBuilder(@NotNull final InvocationTarget<?> target,
                @NotNull final Class<? super TYPE> interfaceClass) {

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
        protected Object getTarget() {

            return mTarget.getTarget();
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected TYPE newProxy(@NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration) throws Exception {

            final Object target = mTarget;
            final Class<? super TYPE> interfaceClass = mInterfaceClass;
            final Proxy annotation = interfaceClass.getAnnotation(Proxy.class);
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
                    findConstructor(Class.forName(fullClassName), target, invocationConfiguration,
                                    proxyConfiguration);
            return (TYPE) constructor.newInstance(target, invocationConfiguration,
                                                  proxyConfiguration);
        }
    }
}
