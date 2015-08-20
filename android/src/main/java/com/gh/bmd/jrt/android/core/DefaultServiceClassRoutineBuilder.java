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
package com.gh.bmd.jrt.android.core;

import android.content.Context;

import com.gh.bmd.jrt.android.builder.ServiceClassRoutineBuilder;
import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.core.ServiceTarget.ClassServiceTarget;
import com.gh.bmd.jrt.android.invocation.FunctionContextInvocation;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.core.ServiceTarget.targetInvocation;
import static com.gh.bmd.jrt.core.InvocationTarget.targetClass;
import static com.gh.bmd.jrt.core.RoutineBuilders.configurationWithAnnotations;
import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping an object class.
 * <p/>
 * Created by davide-maestroni on 20/08/15.
 */
class DefaultServiceClassRoutineBuilder implements ServiceClassRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceClassRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceClassRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceClassRoutineBuilder> {

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final ServiceContext mContext;

    private final ClassServiceTarget mTarget;

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
    DefaultServiceClassRoutineBuilder(@Nonnull final ServiceContext context,
            @Nonnull final ClassServiceTarget target) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (target.getTargetClass().isPrimitive()) {

            // The parceling of primitive classes is broken...
            throw new IllegalArgumentException("the target class cannot be primitive");
        }

        mContext = context;
        mTarget = target;
    }

    @Nonnull
    private static Class<?>[] forNames(@Nonnull final String[] names) throws
            ClassNotFoundException {

        // The forName() of primitive classes is broken...
        final int length = names.length;
        final Class<?>[] classes = new Class[length];
        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;

        for (int i = 0; i < length; i++) {

            final String name = names[i];
            final Class<?> primitiveClass = classMap.get(name);

            if (primitiveClass != null) {

                classes[i] = primitiveClass;

            } else {

                classes[i] = Class.forName(name);
            }
        }

        return classes;
    }

    @Nullable
    private static String groupWithShareAnnotation(@Nonnull final ProxyConfiguration configuration,
            @Nonnull final Method method) {

        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            return shareGroupAnnotation.value();
        }

        return configuration.getShareGroupOr(null);
    }

    @Nonnull
    private static String[] toNames(@Nonnull final Class<?>[] classes) {

        final int length = classes.length;
        final String[] names = new String[length];

        for (int i = 0; i < length; i++) {

            names[i] = classes[i].getName();
        }

        return names;
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final ClassServiceTarget target = mTarget;
        final Method targetMethod = getAnnotatedMethod(name, target.getTargetClass());

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = new Object[]{shareGroup, target, name};
        return JRoutine.on(mContext, targetInvocation(new MethodAliasToken<INPUT, OUTPUT>(), args))
                       .invocations()
                       .with(configurationWithAnnotations(mInvocationConfiguration, targetMethod))
                       .set()
                       .service()
                       .with(mServiceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        final ClassServiceTarget target = mTarget;
        final Method targetMethod = findMethod(target.getTargetClass(), name, parameterTypes);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = new Object[]{shareGroup, target, name, toNames(parameterTypes)};
        return JRoutine.on(mContext,
                           targetInvocation(new MethodSignatureToken<INPUT, OUTPUT>(), args))
                       .invocations()
                       .with(configurationWithAnnotations(mInvocationConfiguration, targetMethod))
                       .set()
                       .service()
                       .with(mServiceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ServiceClassRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceClassRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceClassRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceClassRoutineBuilder>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceClassRoutineBuilder> service() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceClassRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceClassRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceClassRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceClassRoutineBuilder setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the service configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    /**
     * Alias method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodAliasInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final String mAliasName;

        private final ClassServiceTarget mInvocationTarget;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param shareGroup       the share group name.
         * @param invocationTarget the invocation target.
         * @param name             the alias name.
         */
        private MethodAliasInvocation(@Nullable final String shareGroup,
                @Nonnull final ClassServiceTarget invocationTarget,
                @Nonnull final String name) throws ClassNotFoundException {

            mShareGroup = shareGroup;
            mInvocationTarget = invocationTarget;
            mAliasName = name;
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(targetClass(mInvocationTarget.getTargetClass()))
                                   .proxies()
                                   .withShareGroup(mShareGroup)
                                   .set()
                                   .aliasMethod(mAliasName);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }

        @Override
        protected void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mRoutine == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.syncCall(inputs));
        }
    }

    /**
     * Class token of a {@link MethodAliasInvocation MethodAliasInvocation}.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodAliasToken<INPUT, OUTPUT>
            extends ClassToken<MethodAliasInvocation<INPUT, OUTPUT>> {

    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final ClassServiceTarget mInvocationTarget;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param shareGroup       the share group name.
         * @param invocationTarget the invocation target.
         * @param name             the method name.
         * @param parameterTypes   the method parameter type names.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         */
        private MethodSignatureInvocation(@Nullable final String shareGroup,
                @Nonnull final ClassServiceTarget invocationTarget, @Nonnull final String name,
                @Nonnull final String[] parameterTypes) throws ClassNotFoundException {

            mShareGroup = shareGroup;
            mInvocationTarget = invocationTarget;
            mMethodName = name;
            mParameterTypes = forNames(parameterTypes);
        }

        @Override
        protected void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mRoutine == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.syncCall(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(targetClass(mInvocationTarget.getTargetClass()))
                                   .proxies()
                                   .withShareGroup(mShareGroup)
                                   .set()
                                   .method(mMethodName, mParameterTypes);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Class token of a {@link MethodSignatureInvocation MethodSignatureInvocation}.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureToken<INPUT, OUTPUT>
            extends ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>> {

    }

    static {

        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;
        classMap.put(byte.class.getName(), byte.class);
        classMap.put(char.class.getName(), char.class);
        classMap.put(int.class.getName(), int.class);
        classMap.put(long.class.getName(), long.class);
        classMap.put(float.class.getName(), float.class);
        classMap.put(double.class.getName(), double.class);
        classMap.put(short.class.getName(), short.class);
        classMap.put(void.class.getName(), void.class);
    }
}
