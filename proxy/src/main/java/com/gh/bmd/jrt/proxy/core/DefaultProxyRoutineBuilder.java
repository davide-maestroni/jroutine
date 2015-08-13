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
package com.gh.bmd.jrt.proxy.core;

import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.proxy.annotation.Proxy;
import com.gh.bmd.jrt.proxy.builder.AbstractProxyBuilder;
import com.gh.bmd.jrt.proxy.builder.ProxyRoutineBuilder;
import com.gh.bmd.jrt.util.ClassToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.Reflection.findConstructor;

/**
 * Default implementation of a proxy builder.
 * <p/>
 * Created by davide-maestroni on 3/23/15.
 */
class DefaultProxyRoutineBuilder
        implements ProxyRoutineBuilder, InvocationConfiguration.Configurable<ProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ProxyRoutineBuilder> {

    private final WeakReference<?> mTargetReference;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param target the target object.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultProxyRoutineBuilder(@Nonnull final Object target) {

        if (target == null) {

            throw new NullPointerException("the target object must not be null");
        }

        mTargetReference = new WeakReference<Object>(target);
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

        if (!itfClass.isAnnotationPresent(Proxy.class)) {

            throw new IllegalArgumentException(
                    "the specified class is not annotated with " + Proxy.class.getName() + ": "
                            + itfClass.getName());
        }

        final Object target = mTargetReference.get();

        if (target == null) {

            throw new IllegalStateException("the target object has been destroyed");
        }

        final ObjectProxyBuilder<TYPE> builder = new ObjectProxyBuilder<TYPE>(target, itf);
        return builder.invocations()
                      .with(mInvocationConfiguration)
                      .set()
                      .proxies()
                      .with(mProxyConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ProxyRoutineBuilder> invocations() {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ProxyRoutineBuilder>(this, configuration);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ProxyRoutineBuilder> proxies() {

        final ProxyConfiguration configuration = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ProxyRoutineBuilder>(this, configuration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ProxyRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ProxyRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectProxyBuilder<TYPE> extends AbstractProxyBuilder<TYPE> {

        private final ClassToken<TYPE> mInterfaceToken;

        private final Object mTarget;

        /**
         * Constructor.
         *
         * @param target         the target object instance.
         * @param interfaceToken the proxy interface token.
         */
        private ObjectProxyBuilder(@Nonnull final Object target,
                @Nonnull final ClassToken<TYPE> interfaceToken) {

            mTarget = target;
            mInterfaceToken = interfaceToken;
        }

        @Nonnull
        @Override
        protected ClassToken<TYPE> getInterfaceToken() {

            return mInterfaceToken;
        }

        @Nonnull
        @Override
        protected Object getTarget() {

            return mTarget;
        }

        @Nonnull
        @Override
        protected TYPE newProxy(@Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration) {

            try {

                final Object target = mTarget;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
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

                final String fullClassName = packageName + annotation.classPrefix() + className
                        + annotation.classSuffix();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(fullClassName), target,
                                        invocationConfiguration, proxyConfiguration);
                return interfaceClass.cast(constructor.newInstance(target, invocationConfiguration,
                                                                   proxyConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
