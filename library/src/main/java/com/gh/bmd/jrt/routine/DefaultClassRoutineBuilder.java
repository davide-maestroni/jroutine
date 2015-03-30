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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.invocation.SingleCallInvocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;

/**
 * Class implementing a builder of routines wrapping a class method.
 * <p/>
 * Created by davide on 9/21/14.
 */
class DefaultClassRoutineBuilder implements ClassRoutineBuilder {

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutineCache = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

    private final HashMap<String, Method> mMethodMap = new HashMap<String, Method>();

    private final Class<?> mTargetClass;

    private final WeakReference<?> mTargetReference;

    private RoutineConfiguration mConfiguration;

    private String mShareGroup;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected, or the specified class represents an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    DefaultClassRoutineBuilder(@Nonnull final Class<?> targetClass) {

        if (targetClass.isInterface()) {

            throw new IllegalArgumentException(
                    "the target class must not be an interface: " + targetClass.getCanonicalName());
        }

        mTargetClass = targetClass;
        mTargetReference = null;
        fillMethodMap(true);
    }

    /**
     * Constructor.
     *
     * @param target the target object.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    DefaultClassRoutineBuilder(@Nonnull final Object target) {

        mTargetClass = target.getClass();
        mTargetReference = new WeakReference<Object>(target);
        fillMethodMap(false);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        final Method method = mMethodMap.get(name);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        final Class<?> targetClass = mTargetClass;
        Method targetMethod = null;

        try {

            targetMethod = targetClass.getMethod(name, parameterTypes);

        } catch (final NoSuchMethodException ignored) {

        }

        if (targetMethod == null) {

            try {

                targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);

            } catch (final NoSuchMethodException e) {

                throw new IllegalArgumentException(e);
            }
        }

        return method(targetMethod);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(RoutineConfiguration.notNull(mConfiguration), mShareGroup, method);
    }

    @Nonnull
    public ClassRoutineBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ClassRoutineBuilder withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    /**
     * Gets the annotated method associated to the specified name.
     *
     * @param name the name specified in the annotation.
     * @return the method or null.
     * @see com.gh.bmd.jrt.annotation.Bind
     */
    @Nullable
    protected Method getAnnotatedMethod(final String name) {

        return mMethodMap.get(name);
    }

    /**
     * Returns the internal configuration.
     *
     * @return the configuration.
     */
    @Nullable
    protected RoutineConfiguration getConfiguration() {

        return mConfiguration;
    }

    /**
     * Gets or creates the routine.
     *
     * @param configuration   the routine configuration.
     * @param shareGroup      the share group name.
     * @param method          the method to wrap.
     * @param isCollectParam  whether we need to collect the input parameters.
     * @param isCollectResult whether the output is a collection.
     * @return the routine instance.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getRoutine(
            @Nonnull final RoutineConfiguration configuration, @Nullable final String shareGroup,
            @Nonnull final Method method, final boolean isCollectParam,
            final boolean isCollectResult) {

        if (!method.isAccessible()) {

            AccessController.doPrivileged(new SetAccessibleAction(method));
        }

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

            final String methodShareGroup = (shareGroup != null) ? shareGroup : Share.ALL;
            final RoutineInfo routineInfo =
                    new RoutineInfo(configuration, method, methodShareGroup, isCollectParam,
                                    isCollectResult);
            Routine<?, ?> routine = routineMap.get(routineInfo);

            if (routine == null) {

                final Object mutex;

                if (!Share.NONE.equals(methodShareGroup)) {

                    mutex = getSharedMutex(target, methodShareGroup);

                } else {

                    mutex = null;
                }

                final InvocationFactory<Object, Object> factory =
                        Invocations.withArgs(target, method, mutex, isCollectParam, isCollectResult)
                                   .factoryOf(MethodSingleCallInvocation.class);
                routine = new DefaultRoutine<Object, Object>(configuration, factory);
                routineMap.put(routineInfo, routine);
            }

            return (Routine<INPUT, OUTPUT>) routine;
        }
    }

    /**
     * Returns the share group name.
     *
     * @return the share group name.
     */
    @Nullable
    protected String getShareGroup() {

        return mShareGroup;
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
     * @param configuration the routine configuration.
     * @param shareGroup    the group name.
     * @param targetMethod  the target method.
     * @return the routine.
     * @throws java.lang.NullPointerException if the specified configuration or method are null.
     */
    @Nonnull
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(
            @Nonnull final RoutineConfiguration configuration, @Nullable final String shareGroup,
            @Nonnull final Method targetMethod) {

        String methodShareGroup = shareGroup;
        final Builder builder = RoutineConfiguration.builderFrom(configuration);
        final Share shareAnnotation = targetMethod.getAnnotation(Share.class);

        if (shareAnnotation != null) {

            methodShareGroup = shareAnnotation.value();
        }

        warn(configuration);

        builder.withInputOrder(OrderType.PASSING_ORDER)
               .withInputSize(Integer.MAX_VALUE)
               .withInputTimeout(TimeDuration.ZERO)
               .withOutputSize(Integer.MAX_VALUE)
               .withOutputTimeout(TimeDuration.ZERO);

        final Timeout timeoutAnnotation = targetMethod.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit())
                   .onReadTimeout(timeoutAnnotation.action());
        }

        return getRoutine(builder.buildConfiguration(), methodShareGroup, targetMethod, false,
                          false);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the routine configuration.
     */
    protected void warn(@Nonnull final RoutineConfiguration configuration) {

        Logger logger = null;

        final OrderType inputOrder = configuration.getInputOrderOr(null);

        if (inputOrder != null) {

            logger = Logger.newLogger(configuration, this);
            logger.wrn("the specified input order will be ignored: %s", inputOrder);
        }

        final int inputSize = configuration.getInputSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final OrderType outputOrder = configuration.getOutputOrderOr(null);

        if (outputOrder != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified output order will be ignored: %s", outputOrder);
        }

        final int outputSize = configuration.getOutputSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    private void fillMap(@Nonnull final HashMap<String, Method> map,
            @Nonnull final Method[] methods, final boolean isClass) {

        for (final Method method : methods) {

            final boolean isStatic = Modifier.isStatic(method.getModifiers());

            if (isClass) {

                if (!isStatic) {

                    continue;
                }

            } else if (isStatic) {

                continue;
            }

            final Bind annotation = method.getAnnotation(Bind.class);

            if (annotation != null) {

                final String name = annotation.value();

                if (map.containsKey(name)) {

                    throw new IllegalArgumentException(
                            "the name '" + name + "' has already been used to identify a different"
                                    + " method");
                }

                map.put(name, method);
            }
        }
    }

    private void fillMethodMap(final boolean isClass) {

        final Class<?> targetClass = mTargetClass;
        final HashMap<String, Method> methodMap = mMethodMap;
        fillMap(methodMap, targetClass.getMethods(), isClass);

        final HashMap<String, Method> declaredMethodMap = new HashMap<String, Method>();
        fillMap(declaredMethodMap, targetClass.getDeclaredMethods(), isClass);

        for (final Entry<String, Method> methodEntry : declaredMethodMap.entrySet()) {

            final String name = methodEntry.getKey();

            if (!methodMap.containsKey(name)) {

                methodMap.put(name, methodEntry.getValue());
            }
        }
    }

    /**
     * Implementation of a simple invocation wrapping the target method.
     */
    private static class MethodSingleCallInvocation extends SingleCallInvocation<Object, Object> {

        private final boolean mHasResult;

        private final boolean mIsArrayResult;

        private final boolean mIsCollectParam;

        private final boolean mIsCollectResult;

        private final Method mMethod;

        private final Object mMutex;

        private final WeakReference<?> mTargetReference;

        /**
         * Constructor.
         *
         * @param target          the target object.
         * @param method          the method to wrap.
         * @param mutex           the mutex used for synchronization.
         * @param isCollectParam  whether we need to collect the input parameters.
         * @param isCollectResult whether the output is a collection.
         */
        public MethodSingleCallInvocation(@Nullable final Object target,
                @Nonnull final Method method, @Nullable final Object mutex,
                final boolean isCollectParam, final boolean isCollectResult) {

            mTargetReference = new WeakReference<Object>(target);
            mMethod = method;
            mMutex = (mutex != null) ? mutex : this;
            mIsCollectParam = isCollectParam;
            mIsCollectResult = isCollectResult;

            final Class<?> returnClass = method.getReturnType();
            mHasResult = !Void.class.equals(boxingClass(returnClass));
            mIsArrayResult = returnClass.isArray();
        }

        @Override
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            synchronized (mMutex) {

                final Object target = mTargetReference.get();

                if (target == null) {

                    throw new IllegalStateException("the target object has been destroyed");
                }

                final Method method = mMethod;

                try {

                    final Object[] args;

                    if (mIsCollectParam) {

                        final Class<?> paramClass = method.getParameterTypes()[0];

                        if (paramClass.isArray()) {

                            final int size = objects.size();
                            final Object array =
                                    Array.newInstance(paramClass.getComponentType(), size);

                            for (int i = 0; i < size; ++i) {

                                Array.set(array, i, objects.get(i));
                            }

                            args = new Object[]{array};

                        } else {

                            args = new Object[]{objects};
                        }

                    } else {

                        args = objects.toArray(new Object[objects.size()]);
                    }

                    final Object methodResult = method.invoke(target, args);

                    if (mHasResult) {

                        if (mIsCollectResult) {

                            if (mIsArrayResult) {

                                if (methodResult != null) {

                                    final int length = Array.getLength(methodResult);

                                    for (int i = 0; i < length; ++i) {

                                        result.pass(Array.get(methodResult, i));
                                    }
                                }

                            } else {

                                result.pass((Iterable<?>) methodResult);
                            }

                        } else {

                            result.pass(methodResult);
                        }
                    }

                } catch (final InvocationTargetException e) {

                    throw new InvocationException(e.getCause());

                } catch (final RoutineException e) {

                    throw e;

                } catch (final Throwable t) {

                    throw new InvocationException(t);
                }
            }
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static final class RoutineInfo {

        private final RoutineConfiguration mConfiguration;

        private final boolean mIsCollectParam;

        private final boolean mIsCollectResult;

        private final Method mMethod;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param configuration   the routine configuration.
         * @param method          the method to wrap.
         * @param shareGroup      the group name.
         * @param isCollectParam  whether we need to collect the input parameters.
         * @param isCollectResult whether the output is a collection.
         */
        public RoutineInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String shareGroup,
                final boolean isCollectParam, final boolean isCollectResult) {

            mMethod = method;
            mShareGroup = shareGroup;
            mConfiguration = configuration;
            mIsCollectParam = isCollectParam;
            mIsCollectResult = isCollectResult;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + (mIsCollectParam ? 1 : 0);
            result = 31 * result + (mIsCollectResult ? 1 : 0);
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

            return mIsCollectParam == that.mIsCollectParam
                    && mIsCollectResult == that.mIsCollectResult && mConfiguration.equals(
                    that.mConfiguration) && mMethod.equals(that.mMethod) && mShareGroup.equals(
                    that.mShareGroup);
        }
    }

    /**
     * Privileged action used to grant accessibility to a method.
     */
    private static class SetAccessibleAction implements PrivilegedAction<Void> {

        private final Method mMethod;

        /**
         * Constructor.
         *
         * @param method the method instance.
         */
        private SetAccessibleAction(@Nonnull final Method method) {

            mMethod = method;
        }

        public Void run() {

            mMethod.setAccessible(true);
            return null;
        }
    }
}
