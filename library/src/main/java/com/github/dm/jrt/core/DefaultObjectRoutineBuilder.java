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
package com.github.dm.jrt.core;

import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.annotation.ShareGroup;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.RoutineBuilders.MethodInfo;
import com.github.dm.jrt.invocation.FunctionInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;
import com.github.dm.jrt.util.WeakIdentityHashMap;

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

    private final InvocationTarget mTarget;

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
    DefaultObjectRoutineBuilder(@Nonnull final InvocationTarget target) {

        final Class<?> targetClass = target.getTargetClass();

        if (targetClass.isInterface()) {

            throw new IllegalArgumentException(
                    "the target class must not be an interface: " + targetClass.getName());
        }

        mTarget = target;
    }

    @Nonnull
    public <IN, OUT> Routine<IN, OUT> aliasMethod(@Nonnull final String name) {

        final Method method = getAnnotatedMethod(name, mTarget.getTargetClass());

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler());
        return itf.cast(proxy);
    }

    @Nonnull
    public <IN, OUT> Routine<IN, OUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(Reflection.findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @Nonnull
    public <IN, OUT> Routine<IN, OUT> method(@Nonnull final Method method) {

        return getRoutine(configurationWithAnnotations(mInvocationConfiguration, method),
                          configurationWithAnnotations(mProxyConfiguration, method), method, null,
                          null);
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ObjectRoutineBuilder> invocations() {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ObjectRoutineBuilder>(this, configuration);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> proxies() {

        final ProxyConfiguration configuration = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(this, configuration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ObjectRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <IN, OUT> Routine<IN, OUT> getRoutine(
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration, @Nonnull final Method method,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        final InvocationTarget target = mTarget;
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
                routine = new DefaultRoutine<Object, Object>(invocationConfiguration, factory);
                routineMap.put(routineInfo, routine);
            }

            return (Routine<IN, OUT>) routine;
        }
    }

    /**
     * Implementation of a simple invocation wrapping the target method.
     */
    private static class MethodFunctionInvocation extends FunctionInvocation<Object, Object> {

        private final InputMode mInputMode;

        private final Method mMethod;

        private final Object mMutex;

        private final OutputMode mOutputMode;

        private final InvocationTarget mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodFunctionInvocation(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final InvocationTarget target, @Nonnull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            final Object mutexTarget =
                    (Modifier.isStatic(method.getModifiers())) ? target.getTargetClass()
                            : target.getTarget();
            final String shareGroup = proxyConfiguration.getShareGroupOr(null);

            if ((mutexTarget != null) && !ShareGroup.NONE.equals(shareGroup)) {

                mMutex = getSharedMutex(mutexTarget, shareGroup);

            } else {

                mMutex = this;
            }

            mTarget = target;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        protected void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Object target = mTarget.getTarget();

            if (target == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            callFromInvocation(mMethod, mMutex, target, objects, result, mInputMode, mOutputMode);
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

        private final InvocationTarget mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodInvocationFactory(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final InvocationTarget target, @Nonnull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Nonnull
        @Override
        public Invocation<Object, Object> newInvocation() {

            return new MethodFunctionInvocation(mProxyConfiguration, mTarget, mMethod, mInputMode,
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
        private RoutineInfo(@Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration, @Nonnull final Method method,
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

            final MethodInfo methodInfo = getTargetMethodInfo(method, mTarget.getTargetClass());
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Routine<Object, Object> routine =
                    getRoutine(configurationWithAnnotations(mInvocationConfiguration, method),
                               configurationWithAnnotations(mProxyConfiguration, method),
                               methodInfo.method, inputMode, outputMode);
            return invokeRoutine(routine, method, (args != null) ? args : Reflection.NO_ARGS,
                                 inputMode, outputMode);
        }
    }
}
