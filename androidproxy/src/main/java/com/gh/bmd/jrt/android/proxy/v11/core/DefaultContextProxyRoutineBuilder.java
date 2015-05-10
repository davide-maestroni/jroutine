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

import com.gh.bmd.jrt.android.builder.InvocationConfiguration;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration.Builder;
import com.gh.bmd.jrt.android.proxy.builder.AbstractContextProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.ContextProxyRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Created by davide on 06/05/15.
 */
class DefaultContextProxyRoutineBuilder implements ContextProxyRoutineBuilder,
        RoutineConfiguration.Configurable<ContextProxyRoutineBuilder>,
        ProxyConfiguration.Configurable<ContextProxyRoutineBuilder>,
        InvocationConfiguration.Configurable<ContextProxyRoutineBuilder> {

    private final WeakReference<?> mContextReference;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity    the invocation activity context.
     * @param targetClass the target class.
     */
    DefaultContextProxyRoutineBuilder(@Nonnull final Activity activity,
            @Nonnull final Class<?> targetClass) {

        this((Object) activity, targetClass);
    }

    /**
     * Constructor.
     *
     * @param fragment    the invocation fragment context.
     * @param targetClass the target class.
     */
    DefaultContextProxyRoutineBuilder(@Nonnull final Fragment fragment,
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
    private DefaultContextProxyRoutineBuilder(@Nonnull final Object context,
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

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getRawClass().getName());
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
                      .withInvocation()
                      .with(mInvocationConfiguration)
                      .set()
                      .buildProxy();
    }

    @Nonnull
    public Builder<? extends ContextProxyRoutineBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new Builder<ContextProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ContextProxyRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ContextProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ContextProxyRoutineBuilder> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ContextProxyRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyRoutineBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    /**
     * Proxy builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectContextProxyBuilder<TYPE> extends AbstractContextProxyBuilder<TYPE> {

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
        protected TYPE newProxy(@Nonnull final String shareGroup,
                @Nonnull final RoutineConfiguration routineConfiguration,
                @Nonnull final InvocationConfiguration invocationConfiguration) {

            try {

                final Object context = mContext;
                final Class<?> targetClass = mTargetClass;
                final Class<TYPE> interfaceClass = mInterfaceToken.getRawClass();
                final Package classPackage = interfaceClass.getPackage();
                final String packageName =
                        (classPackage != null) ? classPackage.getName() + "." : "";
                final String className =
                        packageName + "JRoutineV11Proxy_" + interfaceClass.getSimpleName();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(className), context, targetClass, shareGroup,
                                        routineConfiguration, invocationConfiguration);
                return interfaceClass.cast(constructor.newInstance(context, targetClass, shareGroup,
                                                                   routineConfiguration,
                                                                   invocationConfiguration));

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}
