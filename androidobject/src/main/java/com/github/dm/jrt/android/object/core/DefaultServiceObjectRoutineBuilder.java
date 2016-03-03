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

package com.github.dm.jrt.android.object.core;

import android.content.Context;

import com.github.dm.jrt.android.builder.ServiceConfiguration;
import com.github.dm.jrt.android.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.TargetInvocationFactory;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.android.object.builder.ServiceObjectRoutineBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.annotation.SharedFields;
import com.github.dm.jrt.object.builder.ProxyConfiguration;
import com.github.dm.jrt.object.common.Mutex;
import com.github.dm.jrt.object.core.Builders.MethodInfo;
import com.github.dm.jrt.object.core.InvocationTarget;
import com.github.dm.jrt.object.core.JRoutineObject;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.object.core.Builders.callFromInvocation;
import static com.github.dm.jrt.object.core.Builders.configurationWithAnnotations;
import static com.github.dm.jrt.object.core.Builders.getAnnotatedMethod;
import static com.github.dm.jrt.object.core.Builders.getSharedMutex;
import static com.github.dm.jrt.object.core.Builders.getTargetMethodInfo;
import static com.github.dm.jrt.object.core.Builders.invokeRoutine;
import static com.github.dm.jrt.util.Reflection.asArgs;
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

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

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
    DefaultServiceObjectRoutineBuilder(@NotNull final ServiceContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        if (context == null) {
            throw new NullPointerException("the context must not be null");
        }

        if (target == null) {
            throw new NullPointerException("the invocation target must not be null");
        }

        mContext = context;
        mTarget = target;
    }

    @Nullable
    private static List<String> fieldsWithShareAnnotation(
            @NotNull final ProxyConfiguration configuration, @NotNull final Method method) {

        final SharedFields sharedFieldsAnnotation = method.getAnnotation(SharedFields.class);
        if (sharedFieldsAnnotation != null) {
            return Arrays.asList(sharedFieldsAnnotation.value());
        }

        return configuration.getSharedFieldsOr(null);
    }

    @NotNull
    private static Class<?>[] forNames(@NotNull final String[] names) throws
            ClassNotFoundException {

        // The forName() of primitive classes is broken...
        final int length = names.length;
        final Class<?>[] classes = new Class[length];
        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;
        for (int i = 0; i < length; ++i) {
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

    @NotNull
    private static String[] toNames(@NotNull final Class<?>[] classes) {

        final int length = classes.length;
        final String[] names = new String[length];
        for (int i = 0; i < length; ++i) {
            names[i] = classes[i].getName();
        }

        return names;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <IN, OUT> Routine<IN, OUT> alias(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
        if (targetMethod == null) {
            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final List<String> sharedFields =
                fieldsWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = asArgs(sharedFields, target, name);
        final TargetInvocationFactory<Object, Object> factory =
                factoryOf(MethodAliasInvocation.class, args);
        final ServiceRoutineBuilder<Object, Object> builder =
                JRoutineService.with(mContext).on(factory);
        return (Routine<IN, OUT>) builder.withInvocations()
                                         .with(configurationWithAnnotations(
                                                 mInvocationConfiguration, targetMethod))
                                         .getConfigured()
                                         .withService()
                                         .with(mServiceConfiguration)
                                         .getConfigured()
                                         .buildRoutine();
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        if (!itf.isInterface()) {
            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = findMethod(target.getTargetClass(), name, parameterTypes);
        final List<String> sharedFields =
                fieldsWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] args = asArgs(sharedFields, target, name, toNames(parameterTypes));
        final TargetInvocationFactory<Object, Object> factory =
                factoryOf(MethodSignatureInvocation.class, args);
        final ServiceRoutineBuilder<Object, Object> builder =
                JRoutineService.with(mContext).on(factory);
        return (Routine<IN, OUT>) builder.withInvocations()
                                         .with(configurationWithAnnotations(
                                                 mInvocationConfiguration, targetMethod))
                                         .getConfigured()
                                         .withService()
                                         .with(mServiceConfiguration)
                                         .getConfigured()
                                         .buildRoutine();
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @NotNull final ServiceConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the service configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withInvocations
            () {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withProxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withService() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    /**
     * Alias method invocation.
     */
    private static class MethodAliasInvocation extends FunctionContextInvocation<Object, Object> {

        private final String mAliasName;

        private final List<String> mSharedFields;

        private final ContextInvocationTarget<?> mTarget;

        private Object mInstance;

        private Routine<Object, Object> mRoutine;

        /**
         * Constructor.
         *
         * @param sharedFields the list of shared field names.
         * @param target       the invocation target.
         * @param name         the alias name.
         */
        private MethodAliasInvocation(@Nullable final List<String> sharedFields,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            mSharedFields = sharedFields;
            mTarget = target;
            mAliasName = name;
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutineObject.on(target)
                                         .withProxies()
                                         .withSharedFields(mSharedFields)
                                         .getConfigured()
                                         .alias(mAliasName);

            } catch (final Throwable t) {
                throw InvocationException.wrapIfNeeded(t);
            }
        }

        @Override
        protected void onCall(@NotNull final List<?> inputs,
                @NotNull final ResultChannel<Object> result) {

            final Routine<Object, Object> routine = mRoutine;
            if ((routine == null) || (mInstance == null)) {
                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }
    }

    /**
     * Invocation based on method signature.
     */
    private static class MethodSignatureInvocation
            extends FunctionContextInvocation<Object, Object> {

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final List<String> mSharedFields;

        private final ContextInvocationTarget<?> mTarget;

        private Object mInstance;

        private Routine<Object, Object> mRoutine;

        /**
         * Constructor.
         *
         * @param sharedFields   the list of shared field names.
         * @param target         the invocation target.
         * @param name           the method name.
         * @param parameterTypes the method parameter type names.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         */
        private MethodSignatureInvocation(@Nullable final List<String> sharedFields,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name,
                @NotNull final String[] parameterTypes) throws ClassNotFoundException {

            mSharedFields = sharedFields;
            mTarget = target;
            mMethodName = name;
            mParameterTypes = forNames(parameterTypes);
        }

        @Override
        protected void onCall(@NotNull final List<?> inputs,
                @NotNull final ResultChannel<Object> result) {

            final Routine<Object, Object> routine = mRoutine;
            if ((routine == null) || (mInstance == null)) {
                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutineObject.on(target)
                                         .withProxies()
                                         .withSharedFields(mSharedFields)
                                         .getConfigured()
                                         .method(mMethodName, mParameterTypes);

            } catch (final Throwable t) {
                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends FunctionContextInvocation<Object, Object> {

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final List<String> mSharedFields;

        private final ContextInvocationTarget<?> mTarget;

        private final Method mTargetMethod;

        private Object mInstance;

        private Mutex mMutex = Mutex.NO_MUTEX;

        /**
         * Constructor.
         *
         * @param sharedFields         the list of shared field names.
         * @param target               the invocation target.
         * @param targetMethodName     the target method name.
         * @param targetParameterTypes the target method parameter type names.
         * @param inputMode            the input transfer mode.
         * @param outputMode           the output transfer mode.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         * @throws java.lang.NoSuchMethodException  if the target method is not found.
         */
        private ProxyInvocation(@Nullable final List<String> sharedFields,
                @NotNull final ContextInvocationTarget<?> target,
                @NotNull final String targetMethodName,
                @NotNull final String[] targetParameterTypes, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) throws ClassNotFoundException,
                NoSuchMethodException {

            mSharedFields = sharedFields;
            mTarget = target;
            mTargetMethod = target.getTargetClass()
                                  .getMethod(targetMethodName, forNames(targetParameterTypes));
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        protected void onCall(@NotNull final List<?> objects,
                @NotNull final ResultChannel<Object> result) throws Exception {

            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("such error should never happen");
            }

            callFromInvocation(mMutex, targetInstance, mTargetMethod, objects, result, mInputMode,
                               mOutputMode);
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget target = mTarget.getInvocationTarget(context);
                final Object mutexTarget =
                        (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                                : target.getTarget();
                mMutex = getSharedMutex(mutexTarget, mSharedFields);
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

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@NotNull final DefaultServiceObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTarget = builder.mTarget;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mServiceConfiguration = builder.mServiceConfiguration;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final ContextInvocationTarget<?> target = mTarget;
            final MethodInfo methodInfo = getTargetMethodInfo(target.getTargetClass(), method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Class<?>[] targetParameterTypes = targetMethod.getParameterTypes();
            final List<String> sharedFields =
                    fieldsWithShareAnnotation(mProxyConfiguration, method);
            final Object[] factoryArgs = asArgs(sharedFields, target, targetMethod.getName(),
                                                toNames(targetParameterTypes), inputMode,
                                                outputMode);
            final TargetInvocationFactory<Object, Object> factory =
                    factoryOf(ProxyInvocation.class, factoryArgs);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithAnnotations(mInvocationConfiguration, method);
            final ServiceRoutineBuilder<Object, Object> builder =
                    JRoutineService.with(mContext).on(factory);
            final Routine<Object, Object> routine = builder.withInvocations()
                                                           .with(invocationConfiguration)
                                                           .getConfigured()
                                                           .withService()
                                                           .with(mServiceConfiguration)
                                                           .getConfigured()
                                                           .buildRoutine();
            return invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
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
