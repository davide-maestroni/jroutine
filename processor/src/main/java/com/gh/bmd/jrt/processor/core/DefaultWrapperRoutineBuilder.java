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
import com.gh.bmd.jrt.processor.builder.AbstractWrapperBuilder;
import com.gh.bmd.jrt.processor.builder.WrapperRoutineBuilder;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of a wrapper builder.
 * <p/>
 * Created by davide on 3/23/15.
 */
class DefaultWrapperRoutineBuilder
        implements WrapperRoutineBuilder, RoutineConfiguration.Configurable<WrapperRoutineBuilder>,
        ProxyConfiguration.Configurable<WrapperRoutineBuilder> {

    private final WeakReference<?> mTargetReference;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param target the target object.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultWrapperRoutineBuilder(@Nonnull final Object target) {

        if (target == null) {

            throw new NullPointerException("the target object must not be null");
        }

        mTargetReference = new WeakReference<Object>(target);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public WrapperRoutineBuilder apply(@Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public WrapperRoutineBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    public <TYPE> TYPE buildWrapper(@Nonnull final Class<TYPE> itf) {

        return buildWrapper(ClassToken.tokenOf(itf));
    }

    @Nonnull
    public <TYPE> TYPE buildWrapper(@Nonnull final ClassToken<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getRawClass()
                                                                     .getCanonicalName());
        }

        final Object target = mTargetReference.get();

        if (target == null) {

            throw new IllegalStateException("the target object has been destroyed");
        }

        final ObjectWrapperBuilder<TYPE> builder = new ObjectWrapperBuilder<TYPE>(target, itf);
        return builder.routineConfiguration()
                      .with(mRoutineConfiguration).applied()
                      .proxyConfiguration()
                      .with(mProxyConfiguration).applied()
                      .buildWrapper();
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends WrapperRoutineBuilder> proxyConfiguration() {

        return new ProxyConfiguration.Builder<WrapperRoutineBuilder>(this, mProxyConfiguration);
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends WrapperRoutineBuilder> routineConfiguration() {

        return new RoutineConfiguration.Builder<WrapperRoutineBuilder>(this, mRoutineConfiguration);
    }

    /**
     * Wrapper builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectWrapperBuilder<TYPE> extends AbstractWrapperBuilder<TYPE> {

        private final ClassToken<TYPE> mInterfaceToken;

        private final Object mTarget;

        /**
         * Constructor.
         *
         * @param target         the target object instance.
         * @param interfaceToken the wrapper interface token.
         */
        private ObjectWrapperBuilder(@Nonnull final Object target,
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
        protected TYPE newWrapper(@Nonnull final String shareGroup,
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
