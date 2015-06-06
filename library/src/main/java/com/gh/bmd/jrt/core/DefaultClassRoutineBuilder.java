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
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.FunctionInvocation;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.TimeDuration;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.RoutineBuilders.callFromInvocation;
import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedStaticMethod;
import static com.gh.bmd.jrt.core.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping a class method.
 * <p/>
 * Created by davide-maestroni on 9/21/14.
 */
class DefaultClassRoutineBuilder
        implements ClassRoutineBuilder, InvocationConfiguration.Configurable<ClassRoutineBuilder>,
        ProxyConfiguration.Configurable<ClassRoutineBuilder> {

    private static final MethodInvocationFactory sMethodInvocationFactory =
            new MethodInvocationFactory();

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutineCache = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

    private final Class<?> mTargetClass;

    private final WeakReference<?> mTargetReference;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     * @throws java.lang.IllegalArgumentException if the specified class represents an interface.
     */
    DefaultClassRoutineBuilder(@Nonnull final Class<?> targetClass) {

        if (targetClass.isInterface()) {

            throw new IllegalArgumentException(
                    "the target class must not be an interface: " + targetClass.getName());
        }

        mTargetClass = targetClass;
        mTargetReference = null;
    }

    /**
     * Constructor.
     *
     * @param target the target object.
     */
    DefaultClassRoutineBuilder(@Nonnull final Object target) {

        mTargetClass = target.getClass();
        mTargetReference = new WeakReference<Object>(target);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final Method method = getAnnotatedStaticMethod(mTargetClass, name);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(mInvocationConfiguration, mProxyConfiguration, method);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(findMethod(mTargetClass, name, parameterTypes));
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ClassRoutineBuilder> withInvocation() {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ClassRoutineBuilder>(this, configuration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ClassRoutineBuilder> withProxy() {

        final ProxyConfiguration configuration = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ClassRoutineBuilder>(this, configuration);
    }

    /**
     * Returns the internal invocation configuration.
     *
     * @return the configuration.
     */
    @Nonnull
    protected InvocationConfiguration getInvocationConfiguration() {

        return mInvocationConfiguration;
    }

    /**
     * Returns the internal proxy configuration.
     *
     * @return the configuration.
     */
    @Nonnull
    protected ProxyConfiguration getProxyConfiguration() {

        return mProxyConfiguration;
    }

    /**
     * Gets or creates the routine.
     *
     * @param configuration the invocation configuration.
     * @param shareGroup    the share group name.
     * @param method        the method to wrap.
     * @param inputMode     the input transfer mode.
     * @param outputMode    the output transfer mode.
     * @return the routine instance.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getRoutine(
            @Nonnull final InvocationConfiguration configuration, @Nullable final String shareGroup,
            @Nonnull final Method method, @Nullable final InputMode inputMode,
            @Nullable final OutputMode outputMode) {

        final Object target = (mTargetReference != null) ? mTargetReference.get() : mTargetClass;

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

            final String methodShareGroup = (shareGroup != null) ? shareGroup : ShareGroup.ALL;
            final RoutineInfo routineInfo =
                    new RoutineInfo(configuration, method, methodShareGroup, inputMode, outputMode);
            Routine<?, ?> routine = routineMap.get(routineInfo);

            if (routine == null) {

                final Object mutex;

                if (!ShareGroup.NONE.equals(methodShareGroup)) {

                    mutex = getSharedMutex(target, methodShareGroup);

                } else {

                    mutex = null;
                }

                final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                        configuration.builderFrom();
                final InvocationFactory<Object, Object> factory = sMethodInvocationFactory;
                routine = new DefaultRoutine<Object, Object>(
                        builder.withFactoryArgs(target, method, mutex, inputMode, outputMode).set(),
                        factory);
                routineMap.put(routineInfo, routine);
            }

            return (Routine<INPUT, OUTPUT>) routine;
        }
    }

    /**
     * Returns the builder target class.
     *
     * @return the target class.
     */
    @Nonnull
    protected Class<?> getTargetClass() {

        return mTargetClass;
    }

    /**
     * Returns the builder reference to the target object.
     *
     * @return the target reference.
     */
    @Nullable
    protected WeakReference<?> getTargetReference() {

        return mTargetReference;
    }

    /**
     * Returns a routine used to call the specified method.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param proxyConfiguration      the share configuration.
     * @param targetMethod            the target method.
     * @return the routine.
     */
    @Nonnull
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration,
            @Nonnull final Method targetMethod) {

        String methodShareGroup = proxyConfiguration.getShareGroupOr(null);
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                invocationConfiguration.builderFrom();
        warn(invocationConfiguration);
        builder.withInputOrder(OrderType.PASS_ORDER)
               .withInputMaxSize(Integer.MAX_VALUE)
               .withInputTimeout(TimeDuration.ZERO)
               .withOutputMaxSize(Integer.MAX_VALUE)
               .withOutputTimeout(TimeDuration.ZERO);
        final Priority priorityAnnotation = targetMethod.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.withPriority(priorityAnnotation.value());
        }

        final ShareGroup shareGroupAnnotation = targetMethod.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            methodShareGroup = shareGroupAnnotation.value();
        }

        final Timeout timeoutAnnotation = targetMethod.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = targetMethod.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withReadTimeoutAction(actionAnnotation.value());
        }

        return getRoutine(builder.set(), methodShareGroup, targetMethod, null, null);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the invocation configuration.
     */
    protected void warn(@Nonnull final InvocationConfiguration configuration) {

        final Logger logger = configuration.newLogger(this);
        final Object[] args = configuration.getFactoryArgsOr(null);

        if (args != null) {

            logger.wrn("the specified factory arguments will be ignored: %s",
                       Arrays.toString(args));
        }

        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final int inputSize = configuration.getInputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (inputSize != InvocationConfiguration.DEFAULT) {

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final OrderType outputOrderType = configuration.getOutputOrderTypeOr(null);

        if (outputOrderType != null) {

            logger.wrn("the specified output order type will be ignored: %s", outputOrderType);
        }

        final int outputSize = configuration.getOutputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (outputSize != InvocationConfiguration.DEFAULT) {

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
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
         * @param target     the target object.
         * @param method     the method to wrap.
         * @param mutex      the mutex used for synchronization.
         * @param inputMode  the input transfer mode.
         * @param outputMode the output transfer mode.
         */
        public MethodFunctionInvocation(@Nullable final Object target, @Nonnull final Method method,
                @Nullable final Object mutex, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            mTargetReference = new WeakReference<Object>(target);
            mMethod = method;
            mMutex = (mutex != null) ? mutex : this;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Object target = mTargetReference.get();

            if (target == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            callFromInvocation(target, mMutex, objects, result, mMethod, mInputMode, mOutputMode);
        }
    }

    /**
     * Factory creating method invocations.
     */
    private static class MethodInvocationFactory implements InvocationFactory<Object, Object> {

        @Nonnull
        public Invocation<Object, Object> newInvocation(@Nonnull final Object... args) {

            return new MethodFunctionInvocation(args[0], (Method) args[1], args[2],
                                                (InputMode) args[3], (OutputMode) args[4]);
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static final class RoutineInfo {

        private final InvocationConfiguration mConfiguration;

        private final InputMode mInputMode;

        private final Method mMethod;

        private final OutputMode mOutputMode;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param configuration the invocation configuration.
         * @param method        the method to wrap.
         * @param shareGroup    the group name.
         * @param inputMode     the input transfer mode.
         * @param outputMode    the output transfer mode.
         */
        public RoutineInfo(@Nonnull final InvocationConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String shareGroup,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mMethod = method;
            mShareGroup = shareGroup;
            mConfiguration = configuration;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + (mInputMode != null ? mInputMode.hashCode() : 0);
            result = 31 * result + mMethod.hashCode();
            result = 31 * result + (mOutputMode != null ? mOutputMode.hashCode() : 0);
            result = 31 * result + mShareGroup.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof RoutineInfo)) {

                return false;
            }

            final RoutineInfo that = (RoutineInfo) o;
            return mConfiguration.equals(that.mConfiguration) && mInputMode == that.mInputMode
                    && mMethod.equals(that.mMethod) && mOutputMode == that.mOutputMode
                    && mShareGroup.equals(that.mShareGroup);
        }
    }
}
