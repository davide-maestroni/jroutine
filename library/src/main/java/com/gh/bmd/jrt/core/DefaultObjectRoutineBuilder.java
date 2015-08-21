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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.core.RoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.invocation.FunctionInvocation;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.RoutineBuilders.callFromInvocation;
import static com.gh.bmd.jrt.core.RoutineBuilders.configurationWithAnnotations;
import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.core.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.core.RoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.util.Reflection.NO_ARGS;
import static com.gh.bmd.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p/>
 * Created by davide-maestroni on 9/21/14.
 */
class DefaultObjectRoutineBuilder
        implements ObjectRoutineBuilder, InvocationConfiguration.Configurable<ObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<ObjectRoutineBuilder> {

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutineCache = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

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
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

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
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

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
    private <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getRoutine(
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration, @Nonnull final Method method,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        final Object target = mTarget.getTarget();

        if (target == null) {

            throw new IllegalStateException("the target object has been destroyed");
        }

        synchronized (sRoutineCache) {

            final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<?, ?>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<?, ?>>();
                routineCache.put(target, routineMap);
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

            return (Routine<INPUT, OUTPUT>) routine;
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

        private final WeakReference<?> mTargetReference;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetReference    the reference to target object.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodFunctionInvocation(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final WeakReference<?> targetReference, @Nonnull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            final Object target = targetReference.get();
            final String shareGroup = proxyConfiguration.getShareGroupOr(null);

            if ((target != null) && !ShareGroup.NONE.equals(shareGroup)) {

                mMutex = getSharedMutex(target, shareGroup);

            } else {

                mMutex = this;
            }

            mTargetReference = targetReference;
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        protected void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Object target = mTargetReference.get();

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

        private final WeakReference<?> mTargetReference;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the target object.
         * @param method             the method to wrap.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private MethodInvocationFactory(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Object target, @Nonnull final Method method,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mProxyConfiguration = proxyConfiguration;
            mTargetReference = new WeakReference<Object>(target);
            mMethod = method;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Nonnull
        @Override
        public Invocation<Object, Object> newInvocation() {

            return new MethodFunctionInvocation(mProxyConfiguration, mTargetReference, mMethod,
                                                mInputMode, mOutputMode);
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
                    buildRoutine(method, methodInfo.method, inputMode, outputMode);
            return invokeRoutine(routine, method, (args != null) ? args : NO_ARGS, inputMode,
                                 outputMode);
        }

        @Nonnull
        private Routine<Object, Object> buildRoutine(@Nonnull final Method method,
                @Nonnull final Method targetMethod, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            return getRoutine(configurationWithAnnotations(mInvocationConfiguration, method),
                              configurationWithAnnotations(mProxyConfiguration, method),
                              targetMethod, inputMode, outputMode);
        }
    }
}
