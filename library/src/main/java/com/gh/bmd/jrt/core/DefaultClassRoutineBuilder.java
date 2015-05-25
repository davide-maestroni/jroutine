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

import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.ProcedureInvocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.callInvocation;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getAnnotatedStaticMethod;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.common.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping a class method.
 * <p/>
 * Created by davide-maestroni on 9/21/14.
 */
class DefaultClassRoutineBuilder
        implements ClassRoutineBuilder, RoutineConfiguration.Configurable<ClassRoutineBuilder>,
        ProxyConfiguration.Configurable<ClassRoutineBuilder> {

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutineCache = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

    private static InvocationFactory<Object, Object> sMethodInvocationFactory =
            new InvocationFactory<Object, Object>() {

                @Nonnull
                public Invocation<Object, Object> newInvocation(@Nonnull final Object... args) {

                    return new MethodProcedureInvocation(args[0], (Method) args[1], args[2],
                                                         (Boolean) args[3], (Boolean) args[4]);
                }
            };

    private final Class<?> mTargetClass;

    private final WeakReference<?> mTargetReference;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected, or the specified class represents an
     *                                            interface.
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
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
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
                    "no annotated method with name '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(mRoutineConfiguration, mProxyConfiguration, method);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(findMethod(mTargetClass, name, parameterTypes));
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ClassRoutineBuilder> withRoutine() {

        return new RoutineConfiguration.Builder<ClassRoutineBuilder>(this, mRoutineConfiguration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder setConfiguration(@Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
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

        return new ProxyConfiguration.Builder<ClassRoutineBuilder>(this, mProxyConfiguration);
    }

    /**
     * Returns the internal share configuration.
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
     * @param configuration     the routine configuration.
     * @param shareGroup        the share group name.
     * @param method            the method to wrap.
     * @param isInputCollection whether we need to collect the input parameters.
     * @param isOutputElement   whether the output is a collection.
     * @return the routine instance.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getRoutine(
            @Nonnull final RoutineConfiguration configuration, @Nullable final String shareGroup,
            @Nonnull final Method method, final boolean isInputCollection,
            final boolean isOutputElement) {

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
                    new RoutineInfo(configuration, method, methodShareGroup, isInputCollection,
                                    isOutputElement);
            Routine<?, ?> routine = routineMap.get(routineInfo);

            if (routine == null) {

                final Object mutex;

                if (!ShareGroup.NONE.equals(methodShareGroup)) {

                    mutex = getSharedMutex(target, methodShareGroup);

                } else {

                    mutex = null;
                }

                final RoutineConfiguration.Builder<RoutineConfiguration> builder =
                        configuration.builderFrom();
                final InvocationFactory<Object, Object> factory = sMethodInvocationFactory;
                routine = new DefaultRoutine<Object, Object>(
                        builder.withFactoryArgs(target, method, mutex, isInputCollection,
                                                isOutputElement).set(), factory);
                routineMap.put(routineInfo, routine);
            }

            return (Routine<INPUT, OUTPUT>) routine;
        }
    }

    /**
     * Returns the internal routine configuration.
     *
     * @return the configuration.
     */
    @Nonnull
    protected RoutineConfiguration getRoutineConfiguration() {

        return mRoutineConfiguration;
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
     * @param routineConfiguration the routine configuration.
     * @param proxyConfiguration   the share configuration.
     * @param targetMethod         the target method.
     * @return the routine.
     */
    @Nonnull
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(
            @Nonnull final RoutineConfiguration routineConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration,
            @Nonnull final Method targetMethod) {

        String methodShareGroup = proxyConfiguration.getShareGroupOr(null);
        final RoutineConfiguration.Builder<RoutineConfiguration> builder =
                routineConfiguration.builderFrom();
        final ShareGroup shareGroupAnnotation = targetMethod.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            methodShareGroup = shareGroupAnnotation.value();
        }

        warn(routineConfiguration);
        builder.withInputOrder(OrderType.PASS_ORDER)
               .withInputMaxSize(Integer.MAX_VALUE)
               .withInputTimeout(TimeDuration.ZERO)
               .withOutputMaxSize(Integer.MAX_VALUE)
               .withOutputTimeout(TimeDuration.ZERO);
        final Timeout timeoutAnnotation = targetMethod.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = targetMethod.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withReadTimeoutAction(actionAnnotation.value());
        }

        return getRoutine(builder.set(), methodShareGroup, targetMethod, false, false);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the routine configuration.
     */
    protected void warn(@Nonnull final RoutineConfiguration configuration) {

        Logger logger = null;
        final Object[] args = configuration.getFactoryArgsOr(null);

        if (args != null) {

            logger = configuration.newLogger(this);
            logger.wrn("the specified factory arguments will be ignored: %s",
                       Arrays.toString(args));
        }

        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final int inputSize = configuration.getInputMaxSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final OrderType outputOrderType = configuration.getOutputOrderTypeOr(null);

        if (outputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified output order type will be ignored: %s", outputOrderType);
        }

        final int outputSize = configuration.getOutputMaxSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    /**
     * Implementation of a simple invocation wrapping the target method.
     */
    private static class MethodProcedureInvocation extends ProcedureInvocation<Object, Object> {

        private final boolean mIsInputCollection;

        private final boolean mIsOutputElement;

        private final Method mMethod;

        private final Object mMutex;

        private final WeakReference<?> mTargetReference;

        /**
         * Constructor.
         *
         * @param target            the target object.
         * @param method            the method to wrap.
         * @param mutex             the mutex used for synchronization.
         * @param isInputCollection whether we need to collect the input parameters.
         * @param isOutputElement   whether the output is a collection.
         */
        public MethodProcedureInvocation(@Nullable final Object target,
                @Nonnull final Method method, @Nullable final Object mutex,
                final boolean isInputCollection, final boolean isOutputElement) {

            mTargetReference = new WeakReference<Object>(target);
            mMethod = method;
            mMutex = (mutex != null) ? mutex : this;
            mIsInputCollection = isInputCollection;
            mIsOutputElement = isOutputElement;
        }

        @Override
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Object target = mTargetReference.get();

            if (target == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            callInvocation(target, mMethod, mMutex, mIsInputCollection, mIsOutputElement, objects,
                           result);
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static final class RoutineInfo {

        private final RoutineConfiguration mConfiguration;

        private final boolean mIsInputCollection;

        private final boolean mIsOutputElement;

        private final Method mMethod;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param configuration     the routine configuration.
         * @param method            the method to wrap.
         * @param shareGroup        the group name.
         * @param isInputCollection whether we need to collect the input parameters.
         * @param isOutputElement   whether the output is a collection.
         */
        public RoutineInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String shareGroup,
                final boolean isInputCollection, final boolean isOutputElement) {

            mMethod = method;
            mShareGroup = shareGroup;
            mConfiguration = configuration;
            mIsInputCollection = isInputCollection;
            mIsOutputElement = isOutputElement;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + (mIsInputCollection ? 1 : 0);
            result = 31 * result + (mIsOutputElement ? 1 : 0);
            result = 31 * result + mMethod.hashCode();
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
            return mIsInputCollection == that.mIsInputCollection
                    && mIsOutputElement == that.mIsOutputElement && mConfiguration.equals(
                    that.mConfiguration) && mMethod.equals(that.mMethod) && mShareGroup.equals(
                    that.mShareGroup);
        }
    }
}
