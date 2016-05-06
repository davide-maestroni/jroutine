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

package com.github.dm.jrt.android.object;

import android.content.Context;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.object.builder.ServiceObjectRoutineBuilder;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.object.InvocationTarget;
import com.github.dm.jrt.object.JRoutineObject;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.annotation.SharedFields;
import com.github.dm.jrt.object.builder.Builders.MethodInfo;
import com.github.dm.jrt.object.common.Mutex;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findMethod;
import static com.github.dm.jrt.object.builder.Builders.callFromInvocation;
import static com.github.dm.jrt.object.builder.Builders.getAnnotatedMethod;
import static com.github.dm.jrt.object.builder.Builders.getSharedMutex;
import static com.github.dm.jrt.object.builder.Builders.getTargetMethodInfo;
import static com.github.dm.jrt.object.builder.Builders.invokeRoutine;
import static com.github.dm.jrt.object.builder.Builders.withAnnotations;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 03/29/2015.
 */
class DefaultServiceObjectRoutineBuilder implements ServiceObjectRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ObjectConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceObjectRoutineBuilder> {

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final ServiceContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

    private ServiceConfiguration mServiceConfiguration =
            ServiceConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the service context.
     * @param target  the invocation target.
     */
    DefaultServiceObjectRoutineBuilder(@NotNull final ServiceContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("service context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @Nullable
    private static List<String> fieldsWithShareAnnotation(
            @NotNull final ObjectConfiguration configuration, @NotNull final Method method) {

        final SharedFields sharedFieldsAnnotation = method.getAnnotation(SharedFields.class);
        if (sharedFieldsAnnotation != null) {
            return Arrays.asList(sharedFieldsAnnotation.value());
        }

        return configuration.getSharedFieldsOrElse(null);
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
    public ServiceObjectRoutineBuilder apply(@NotNull final ServiceConfiguration configuration) {

        mServiceConfiguration = ConstantConditions.notNull("service configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceObjectRoutineBuilder apply(@NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    public ServiceObjectRoutineBuilder apply(@NotNull final ObjectConfiguration configuration) {

        mObjectConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
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
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
        if (targetMethod == null) {
            return method(name, Reflection.NO_PARAMS);
        }

        final List<String> sharedFields =
                fieldsWithShareAnnotation(mObjectConfiguration, targetMethod);
        final Object[] args = asArgs(sharedFields, target, name);
        final TargetInvocationFactory<Object, Object> factory =
                factoryOf(MethodAliasInvocation.class, args);
        final ServiceRoutineBuilder<Object, Object> builder =
                JRoutineService.with(mContext).on(factory);
        return (Routine<IN, OUT>) builder.invocationConfiguration()
                                         .with(withAnnotations(mInvocationConfiguration,
                                                 targetMethod))
                                         .apply()
                                         .serviceConfiguration()
                                         .with(mServiceConfiguration)
                                         .apply()
                                         .buildRoutine();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = findMethod(target.getTargetClass(), name, parameterTypes);
        final List<String> sharedFields =
                fieldsWithShareAnnotation(mObjectConfiguration, targetMethod);
        final Object[] args = asArgs(sharedFields, target, name, toNames(parameterTypes));
        final TargetInvocationFactory<Object, Object> factory =
                factoryOf(MethodSignatureInvocation.class, args);
        final ServiceRoutineBuilder<Object, Object> builder =
                JRoutineService.with(mContext).on(factory);
        return (Routine<IN, OUT>) builder.invocationConfiguration()
                                         .with(withAnnotations(mInvocationConfiguration,
                                                 targetMethod))
                                         .apply()
                                         .serviceConfiguration()
                                         .with(mServiceConfiguration)
                                         .apply()
                                         .buildRoutine();
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ServiceObjectRoutineBuilder>
    invocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ObjectConfiguration.Builder<? extends ServiceObjectRoutineBuilder> objectConfiguration() {

        final ObjectConfiguration config = mObjectConfiguration;
        return new ObjectConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceObjectRoutineBuilder>
    serviceConfiguration() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    /**
     * Alias method invocation.
     */
    private static class MethodAliasInvocation extends TemplateContextInvocation<Object, Object> {

        private final String mAliasName;

        private final List<String> mSharedFields;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<Object, Object> mChannel;

        @SuppressWarnings("unused")
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
            final InvocationTarget target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            mRoutine = JRoutineObject.on(target)
                                     .objectConfiguration()
                                     .withSharedFields(mSharedFields)
                                     .apply()
                                     .method(mAliasName);
        }

        @Override
        public void onInitialize() throws Exception {

            mChannel = mRoutine.syncInvoke();
        }

        @Override
        public void onInput(final Object input, @NotNull final ResultChannel<Object> result) throws
                Exception {

            mChannel.pass(input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<Object> result) throws Exception {

            result.pass(mChannel.result());
        }

        @Override
        public void onTerminate() throws Exception {

            mChannel = null;
        }
    }

    /**
     * Invocation based on method signature.
     */
    private static class MethodSignatureInvocation
            extends TemplateContextInvocation<Object, Object> {

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final List<String> mSharedFields;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<Object, Object> mChannel;

        @SuppressWarnings("unused")
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
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            final InvocationTarget target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            mRoutine = JRoutineObject.on(target)
                                     .objectConfiguration()
                                     .withSharedFields(mSharedFields)
                                     .apply()
                                     .method(mMethodName, mParameterTypes);
        }

        @Override
        public void onInitialize() throws Exception {

            mChannel = mRoutine.syncInvoke();
        }

        @Override
        public void onInput(final Object input, @NotNull final ResultChannel<Object> result) throws
                Exception {

            mChannel.pass(input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<Object> result) throws Exception {

            result.pass(mChannel.result());
        }

        @Override
        public void onTerminate() throws Exception {

            mChannel = null;
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends CallContextInvocation<Object, Object> {

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
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            final InvocationTarget target = mTarget.getInvocationTarget(context);
            final Object mutexTarget =
                    (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                            : target.getTarget();
            mMutex = getSharedMutex(mutexTarget, mSharedFields);
            mInstance = target.getTarget();
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
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final ServiceContext mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final ObjectConfiguration mObjectConfiguration;

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
            mObjectConfiguration = builder.mObjectConfiguration;
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
                    fieldsWithShareAnnotation(mObjectConfiguration, method);
            final Object[] factoryArgs = asArgs(sharedFields, target, targetMethod.getName(),
                    toNames(targetParameterTypes), inputMode, outputMode);
            final TargetInvocationFactory<Object, Object> factory =
                    factoryOf(ProxyInvocation.class, factoryArgs);
            final InvocationConfiguration invocationConfiguration =
                    withAnnotations(mInvocationConfiguration, method);
            final ServiceRoutineBuilder<Object, Object> builder =
                    JRoutineService.with(mContext).on(factory);
            final Routine<Object, Object> routine = builder.invocationConfiguration()
                                                           .with(invocationConfiguration)
                                                           .apply()
                                                           .serviceConfiguration()
                                                           .with(mServiceConfiguration)
                                                           .apply()
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
