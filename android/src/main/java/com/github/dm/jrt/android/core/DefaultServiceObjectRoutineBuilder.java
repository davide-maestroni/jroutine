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
package com.github.dm.jrt.android.core;

import android.content.Context;

import com.github.dm.jrt.android.builder.ServiceConfiguration;
import com.github.dm.jrt.android.builder.ServiceObjectRoutineBuilder;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.annotation.ShareGroup;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.InvocationTarget;
import com.github.dm.jrt.core.RoutineBuilders.MethodInfo;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.core.RoutineBuilders.callFromInvocation;
import static com.github.dm.jrt.core.RoutineBuilders.configurationWithAnnotations;
import static com.github.dm.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.github.dm.jrt.core.RoutineBuilders.getSharedMutex;
import static com.github.dm.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.github.dm.jrt.core.RoutineBuilders.invokeRoutine;
import static com.github.dm.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p/>
 * Created by davide-maestroni on 03/29/2015.
 */
class DefaultServiceObjectRoutineBuilder implements ServiceObjectRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceObjectRoutineBuilder> {

    private static final ClassToken<ProxyInvocation> PROXY_TOKEN =
            ClassToken.tokenOf(ProxyInvocation.class);

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final ServiceContext mContext;

    private final ContextInvocationTarget mTarget;

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
    DefaultServiceObjectRoutineBuilder(@Nonnull final ServiceContext context,
            @Nonnull final ContextInvocationTarget target) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (target == null) {

            throw new NullPointerException("the invocation target must not be null");
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
    public <IN, OUT> Routine<IN, OUT> aliasMethod(@Nonnull final String name) {

        final ContextInvocationTarget target = mTarget;
        final Method targetMethod = getAnnotatedMethod(name, target.getTargetClass());

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = new Object[]{shareGroup, target, name};
        return JRoutine.with(mContext)
                       .on(InvocationFactoryTarget.invocationOf(new MethodAliasToken<IN, OUT>(),
                                                                args))
                       .invocations()
                       .with(configurationWithAnnotations(mInvocationConfiguration, targetMethod))
                       .set()
                       .service()
                       .with(mServiceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @Nonnull
    public <IN, OUT> Routine<IN, OUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        final ContextInvocationTarget target = mTarget;
        final Method targetMethod = findMethod(target.getTargetClass(), name, parameterTypes);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = new Object[]{shareGroup, target, name, toNames(parameterTypes)};
        return JRoutine.with(mContext)
                       .on(InvocationFactoryTarget.invocationOf(new MethodSignatureToken<IN, OUT>(),
                                                                args))
                       .invocations()
                       .with(configurationWithAnnotations(mInvocationConfiguration, targetMethod))
                       .set()
                       .service()
                       .with(mServiceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <IN, OUT> Routine<IN, OUT> method(@Nonnull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ServiceObjectRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceObjectRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceObjectRoutineBuilder> service() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
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
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodAliasInvocation<IN, OUT> extends FunctionContextInvocation<IN, OUT> {

        private final String mAliasName;

        private final String mShareGroup;

        private final ContextInvocationTarget mTarget;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param shareGroup the share group name.
         * @param target     the invocation target.
         * @param name       the alias name.
         */
        private MethodAliasInvocation(@Nullable final String shareGroup,
                @Nonnull final ContextInvocationTarget target, @Nonnull final String name) {

            mShareGroup = shareGroup;
            mTarget = target;
            mAliasName = name;
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutine.on(target)
                                   .proxies()
                                   .withShareGroup(mShareGroup)
                                   .set()
                                   .aliasMethod(mAliasName);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }

        @Override
        protected void onCall(@Nonnull final List<? extends IN> inputs,
                @Nonnull final ResultChannel<OUT> result) {

            final Routine<IN, OUT> routine = mRoutine;

            if ((routine == null) || (mInstance == null)) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }
    }

    /**
     * Class token of a {@link MethodAliasInvocation MethodAliasInvocation}.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodAliasToken<IN, OUT>
            extends ClassToken<MethodAliasInvocation<IN, OUT>> {

    }

    /**
     * Invocation based on method signature.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodSignatureInvocation<IN, OUT>
            extends FunctionContextInvocation<IN, OUT> {

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private final ContextInvocationTarget mTarget;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param shareGroup     the share group name.
         * @param target         the invocation target.
         * @param name           the method name.
         * @param parameterTypes the method parameter type names.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         */
        private MethodSignatureInvocation(@Nullable final String shareGroup,
                @Nonnull final ContextInvocationTarget target, @Nonnull final String name,
                @Nonnull final String[] parameterTypes) throws ClassNotFoundException {

            mShareGroup = shareGroup;
            mTarget = target;
            mMethodName = name;
            mParameterTypes = forNames(parameterTypes);
        }

        @Override
        protected void onCall(@Nonnull final List<? extends IN> inputs,
                @Nonnull final ResultChannel<OUT> result) {

            final Routine<IN, OUT> routine = mRoutine;

            if ((routine == null) || (mInstance == null)) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutine.on(target)
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
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodSignatureToken<IN, OUT>
            extends ClassToken<MethodSignatureInvocation<IN, OUT>> {

    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends FunctionContextInvocation<Object, Object> {

        private final ContextInvocationTarget mContextTarget;

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final String mShareGroup;

        private final Method mTargetMethod;

        private Object mInstance;

        private Object mMutex;

        /**
         * Constructor.
         *
         * @param shareGroup           the share group name.
         * @param target               the invocation target.
         * @param targetMethodName     the target method name.
         * @param targetParameterTypes the target method parameter type names.
         * @param inputMode            the input transfer mode.
         * @param outputMode           the output transfer mode.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         * @throws java.lang.NoSuchMethodException  if the target method is not found.
         */
        private ProxyInvocation(@Nullable final String shareGroup,
                @Nonnull final ContextInvocationTarget target,
                @Nonnull final String targetMethodName,
                @Nonnull final String[] targetParameterTypes, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) throws ClassNotFoundException,
                NoSuchMethodException {

            mShareGroup = shareGroup;
            mContextTarget = target;
            mTargetMethod = target.getTargetClass()
                                  .getMethod(targetMethodName, forNames(targetParameterTypes));
            mInputMode = inputMode;
            mOutputMode = outputMode;
            mMutex = this;
        }

        @Override
        protected void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Object targetInstance = mInstance;

            if (targetInstance == null) {

                throw new IllegalStateException("such error should never happen");
            }

            callFromInvocation(mTargetMethod, mMutex, targetInstance, objects, result, mInputMode,
                               mOutputMode);
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget target = mContextTarget.getInvocationTarget(context);
                final Object mutexTarget =
                        (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                                : target.getTarget();
                final String shareGroup = mShareGroup;

                if ((mutexTarget != null) && !ShareGroup.NONE.equals(shareGroup)) {

                    mMutex = getSharedMutex(mutexTarget, shareGroup);
                }

                mInstance = target.getTarget();

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final ServiceContext mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final ServiceConfiguration mServiceConfiguration;

        private final ContextInvocationTarget mTarget;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultServiceObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTarget = builder.mTarget;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mServiceConfiguration = builder.mServiceConfiguration;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final ContextInvocationTarget target = mTarget;
            final MethodInfo methodInfo = getTargetMethodInfo(method, target.getTargetClass());
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Class<?>[] targetParameterTypes = targetMethod.getParameterTypes();
            final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, method);
            final Object[] factoryArgs = new Object[]{shareGroup, target, targetMethod.getName(),
                                                      toNames(targetParameterTypes), inputMode,
                                                      outputMode};
            final InvocationFactoryTarget<Object, Object> targetInvocation =
                    InvocationFactoryTarget.invocationOf(PROXY_TOKEN, factoryArgs);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithAnnotations(mInvocationConfiguration, method);
            final Routine<Object, Object> routine = JRoutine.with(mContext)
                                                            .on(targetInvocation)
                                                            .invocations()
                                                            .with(invocationConfiguration)
                                                            .set()
                                                            .service()
                                                            .with(mServiceConfiguration)
                                                            .set()
                                                            .buildRoutine();
            return invokeRoutine(routine, method, (args == null) ? Reflection.NO_ARGS : args,
                                 inputMode, outputMode);
        }
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
