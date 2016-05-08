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

package com.github.dm.jrt.object;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.builder.Builders.MethodInfo;
import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.object.common.Mutex;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.object.builder.Builders.callFromInvocation;
import static com.github.dm.jrt.object.builder.Builders.getAnnotatedMethod;
import static com.github.dm.jrt.object.builder.Builders.getSharedMutex;
import static com.github.dm.jrt.object.builder.Builders.getTargetMethodInfo;
import static com.github.dm.jrt.object.builder.Builders.invokeRoutine;
import static com.github.dm.jrt.object.builder.Builders.withAnnotations;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 09/21/2014.
 */
class DefaultObjectRoutineBuilder
        implements ObjectRoutineBuilder, Configurable<ObjectRoutineBuilder>,
        ObjectConfiguration.Configurable<ObjectRoutineBuilder> {

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutines = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

    private final InvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param target the invocation target.
     * @throws java.lang.IllegalArgumentException if the class of specified target represents an
     *                                            interface.
     */
    DefaultObjectRoutineBuilder(@NotNull final InvocationTarget<?> target) {

        final Class<?> targetClass = target.getTargetClass();
        if (targetClass.isInterface()) {
            throw new IllegalArgumentException(
                    "the target class must not be an interface: " + targetClass.getName());
        }

        mTarget = target;
    }

    @NotNull
    public ObjectRoutineBuilder apply(@NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    public ObjectRoutineBuilder apply(@NotNull final ObjectConfiguration configuration) {

        mObjectConfiguration = ConstantConditions.notNull("object configuration", configuration);
        return this;
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        if (!itf.isInterface()) {
            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                new ProxyInvocationHandler());
        return itf.cast(proxy);
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {

        final Method method = getAnnotatedMethod(mTarget.getTargetClass(), name);
        if (method == null) {
            return method(name, Reflection.NO_PARAMS);
        }

        return method(method);
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return method(Reflection.findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {

        return getRoutine(withAnnotations(mInvocationConfiguration, method),
                withAnnotations(mObjectConfiguration, method), method, null, null);
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ObjectRoutineBuilder>
    invocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ObjectConfiguration.Builder<? extends ObjectRoutineBuilder> objectConfiguration() {

        final ObjectConfiguration config = mObjectConfiguration;
        return new ObjectConfiguration.Builder<ObjectRoutineBuilder>(this, config);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <IN, OUT> Routine<IN, OUT> getRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final ObjectConfiguration objectConfiguration, @NotNull final Method method,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        final InvocationTarget<?> target = mTarget;
        final Object targetInstance = target.getTarget();
        if (targetInstance == null) {
            throw new IllegalStateException("the target object has been destroyed");
        }

        synchronized (sRoutines) {
            final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>> routines =
                    sRoutines;
            HashMap<RoutineInfo, Routine<?, ?>> routineMap = routines.get(targetInstance);
            if (routineMap == null) {
                routineMap = new HashMap<RoutineInfo, Routine<?, ?>>();
                routines.put(targetInstance, routineMap);
            }

            final RoutineInfo routineInfo =
                    new RoutineInfo(invocationConfiguration, objectConfiguration, method, inputMode,
                            outputMode);
            Routine<?, ?> routine = routineMap.get(routineInfo);
            if (routine == null) {
                final MethodInvocationFactory factory =
                        new MethodInvocationFactory(objectConfiguration, target, method, inputMode,
                                outputMode);
                routine = JRoutineCore.on(factory)
                                      .invocationConfiguration()
                                      .with(invocationConfiguration)
                                      .apply()
                                      .buildRoutine();
                routineMap.put(routineInfo, routine);
            }

            return (Routine<IN, OUT>) routine;
        }
    }

    /**
     * Implementation of a simple invocation wrapping the target method.
     */
    private static class MethodCallInvocation extends CallInvocation<Object, Object> {

        private final InputMode mInputMode;

        private final Method mMethod;

        private final Mutex mMutex;

        private final OutputMode mOutputMode;

        private final InvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param method              the method to wrap.
         * @param inputMode           the input transfer mode.
         * @param outputMode          the output transfer mode.
         */
        private MethodCallInvocation(@NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final InvocationTarget<?> target, @NotNull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            final Object mutexTarget =
                    (Modifier.isStatic(method.getModifiers())) ? target.getTargetClass()
                            : target.getTarget();
            mMutex = getSharedMutex(mutexTarget, objectConfiguration.getSharedFieldsOrElse(null));
            mTarget = target;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        protected void onCall(@NotNull final List<?> objects,
                @NotNull final ResultChannel<Object> result) throws Exception {

            final Object target = mTarget.getTarget();
            if (target == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            callFromInvocation(mMutex, target, mMethod, objects, result, mInputMode, mOutputMode);
        }
    }

    /**
     * Factory creating method invocations.
     */
    private static class MethodInvocationFactory extends InvocationFactory<Object, Object> {

        private final InputMode mInputMode;

        private final Method mMethod;

        private final ObjectConfiguration mObjectConfiguration;

        private final OutputMode mOutputMode;

        private final InvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param method              the method to wrap.
         * @param inputMode           the input transfer mode.
         * @param outputMode          the output transfer mode.
         */
        private MethodInvocationFactory(@NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final InvocationTarget<?> target, @NotNull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            super(asArgs(objectConfiguration, target, method, inputMode, outputMode));
            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @NotNull
        @Override
        public Invocation<Object, Object> newInvocation() {

            return new MethodCallInvocation(mObjectConfiguration, mTarget, mMethod, mInputMode,
                    mOutputMode);
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static final class RoutineInfo extends DeepEqualObject {

        /**
         * Constructor.
         *
         * @param invocationConfiguration the invocation configuration.
         * @param objectConfiguration     the object configuration.
         * @param method                  the method to wrap.
         * @param inputMode               the input transfer mode.
         * @param outputMode              the output transfer mode.
         */
        private RoutineInfo(@NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final Method method, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            super(asArgs(invocationConfiguration, objectConfiguration, method, inputMode,
                    outputMode));
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class ProxyInvocationHandler implements InvocationHandler {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ObjectConfiguration mObjectConfiguration;

        /**
         * Constructor.
         */
        private ProxyInvocationHandler() {

            mInvocationConfiguration = DefaultObjectRoutineBuilder.this.mInvocationConfiguration;
            mObjectConfiguration = DefaultObjectRoutineBuilder.this.mObjectConfiguration;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final MethodInfo methodInfo = getTargetMethodInfo(mTarget.getTargetClass(), method);
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Routine<Object, Object> routine =
                    getRoutine(withAnnotations(mInvocationConfiguration, method),
                            withAnnotations(mObjectConfiguration, method), methodInfo.method,
                            inputMode, outputMode);
            return invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                    inputMode, outputMode);
        }
    }
}
