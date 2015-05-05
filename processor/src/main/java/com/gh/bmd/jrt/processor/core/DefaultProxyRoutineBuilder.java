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
package com.gh.bmd.jrt.processor.core;

import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.processor.builder.AbstractProxyBuilder;
import com.gh.bmd.jrt.processor.builder.ProxyRoutineBuilder;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of a proxy builder.
 * <p/>
 * Created by davide on 3/23/15.
 */
class DefaultProxyRoutineBuilder
        implements ProxyRoutineBuilder, RoutineConfiguration.Configurable<ProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ProxyRoutineBuilder> {

    private final WeakReference<?> mTargetReference;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param target the target object.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
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

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getRawClass().getName());
        }

        final Object target = mTargetReference.get();

        if (target == null) {

            throw new IllegalStateException("the target object has been destroyed");
        }

        final ObjectProxyBuilder<TYPE> builder = new ObjectProxyBuilder<TYPE>(target, itf);
        return builder.withRoutineConfiguration()
                      .with(mRoutineConfiguration)
                      .set()
                      .withProxyConfiguration()
                      .with(mProxyConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ProxyRoutineBuilder> withRoutineConfiguration() {

        return new RoutineConfiguration.Builder<ProxyRoutineBuilder>(this, mRoutineConfiguration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ProxyRoutineBuilder setConfiguration(@Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
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
    public ProxyConfiguration.Builder<? extends ProxyRoutineBuilder> withProxyConfiguration() {

        return new ProxyConfiguration.Builder<ProxyRoutineBuilder>(this, mProxyConfiguration);
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
        protected TYPE newProxy(@Nonnull final String shareGroup,
                @Nonnull final RoutineConfiguration configuration) {

            try {

                final Object target = mTarget;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final Package classPackage = interfaceClass.getPackage();
                final String packageName =
                        (classPackage != null) ? classPackage.getName() + "." : "";
                final String className = packageName + "JRoutine_" + interfaceClass.getSimpleName();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(className), target, shareGroup,
                                        configuration);
                return interfaceClass.cast(
                        constructor.newInstance(target, shareGroup, configuration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
