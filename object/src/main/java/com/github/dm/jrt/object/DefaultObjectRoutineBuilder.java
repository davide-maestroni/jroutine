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
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.object.Builders.MethodInfo;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.object.common.Mutex;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.object.Builders.callFromInvocation;
import static com.github.dm.jrt.object.Builders.configurationWithAnnotations;
import static com.github.dm.jrt.object.Builders.getAnnotatedMethod;
import static com.github.dm.jrt.object.Builders.getSharedMutex;
import static com.github.dm.jrt.object.Builders.getTargetMethodInfo;
import static com.github.dm.jrt.object.Builders.invokeRoutine;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p/>
 * Created by davide-maestroni on 09/21/2014.
 */
class DefaultObjectRoutineBuilder
        implements ObjectRoutineBuilder, Configurable<ObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<ObjectRoutineBuilder> {

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutines = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

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
    DefaultObjectRoutineBuilder(@NotNull final InvocationTarget<?> target) {

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

        return getRoutine(configurationWithAnnotations(mInvocationConfiguration, method),
                configurationWithAnnotations(mProxyConfiguration, method), method, null, null);
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ObjectRoutineBuilder>
    invocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> proxyConfiguration() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ObjectRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    public ObjectRoutineBuilder setConfiguration(@NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <IN, OUT> Routine<IN, OUT> getRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final ProxyConfiguration proxyConfiguration, @NotNull final Method method,
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
                    new RoutineInfo(invocationConfiguration, proxyConfiguration, method, inputMode,
                            outputMode);
            Routine<?, ?> routine = routineMap.get(routineInfo);
            if (routine == null) {
                final MethodInvocationFactory factory =
                        new MethodInvocationFactory(proxyConfiguration, target, method, inputMode,
                                outputMode);
                routine = JRoutineCore.on(factory)
                                      .invocationConfiguration()
                                      .with(invocationConfiguration)
                                      .setConfiguration()
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
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodCallInvocation(@NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final InvocationTarget<?> target, @NotNull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            final Object mutexTarget =
                    (Modifier.isStatic(method.getModifiers())) ? target.getTargetClass()
                            : target.getTarget();
            mMutex = getSharedMutex(mutexTarget, proxyConfiguration.getSharedFieldsOr(null));
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

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        private final InvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodInvocationFactory(@NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final InvocationTarget<?> target, @NotNull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            super(asArgs(proxyConfiguration, target, method, inputMode, outputMode));
            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @NotNull
        @Override
        public Invocation<Object, Object> newInvocation() {

            return new MethodCallInvocation(mProxyConfiguration, mTarget, mMethod, mInputMode,
                    mOutputMode);
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static final class RoutineInfo {

        private final InputMode mInputMode;

        private final InvocationConfiguration mInvocationConfiguration;

        private final Method mMethod;

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        /**
         * Constructor.
         *
         * @param invocationConfiguration the invocation configuration.
         * @param proxyConfiguration      the proxy configuration.
         * @param method                  the method to wrap.
         * @param inputMode               the input transfer mode.
         * @param outputMode              the output transfer mode.
         */
        private RoutineInfo(@NotNull final InvocationConfiguration invocationConfiguration,
                @NotNull final ProxyConfiguration proxyConfiguration, @NotNull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mInvocationConfiguration = invocationConfiguration;
            mProxyConfiguration = proxyConfiguration;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = mInputMode != null ? mInputMode.hashCode() : 0;
            result = 31 * result + mInvocationConfiguration.hashCode();
            result = 31 * result + mMethod.hashCode();
            result = 31 * result + (mOutputMode != null ? mOutputMode.hashCode() : 0);
            result = 31 * result + mProxyConfiguration.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof RoutineInfo)) {
                return false;
            }

            final RoutineInfo that = (RoutineInfo) o;
            return mInputMode == that.mInputMode && mInvocationConfiguration.equals(
                    that.mInvocationConfiguration) && mMethod.equals(that.mMethod)
                    && mOutputMode == that.mOutputMode && mProxyConfiguration.equals(
                    that.mProxyConfiguration);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class ProxyInvocationHandler implements InvocationHandler {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        /**
         * Constructor.
         */
        private ProxyInvocationHandler() {

            mInvocationConfiguration = DefaultObjectRoutineBuilder.this.mInvocationConfiguration;
            mProxyConfiguration = DefaultObjectRoutineBuilder.this.mProxyConfiguration;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final MethodInfo methodInfo = getTargetMethodInfo(mTarget.getTargetClass(), method);
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Routine<Object, Object> routine =
                    getRoutine(configurationWithAnnotations(mInvocationConfiguration, method),
                            configurationWithAnnotations(mProxyConfiguration, method),
                            methodInfo.method, inputMode, outputMode);
            return invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                    inputMode, outputMode);
        }
    }
}
