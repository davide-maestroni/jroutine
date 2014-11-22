/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.annotation.Async;
import com.bmd.jrt.annotation.DefaultLog;
import com.bmd.jrt.annotation.DefaultRunner;
import com.bmd.jrt.builder.RoutineBuilder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.SimpleInvocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.boxingClass;
import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping a class method.
 * <p/>
 * Note that only static methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see Async
 */
public class ClassRoutineBuilder implements RoutineBuilder {

    protected static final CacheHashMap<Object, Map<String, Object>> sMutexCache =
            new CacheHashMap<Object, Map<String, Object>>();

    private static final CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>
            sRoutineCache =
            new CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>();

    private final RoutineConfigurationBuilder mBuilder = new RoutineConfigurationBuilder();

    private final boolean mIsClass;

    private final HashMap<String, Method> mMethodMap = new HashMap<String, Method>();

    private final Object mTarget;

    private final Class<?> mTargetClass;

    private String mLockId;

    /**
     * Constructor.
     *
     * @param target the target class or object.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate tag in the annotations is detected.
     */
    ClassRoutineBuilder(@Nonnull final Object target) {

        final Class<?> targetClass;

        if (target instanceof Class) {

            mTarget = null;
            mIsClass = true;

            targetClass = ((Class<?>) target);

        } else {

            mTarget = target;
            mIsClass = false;

            targetClass = target.getClass();
        }

        mTargetClass = targetClass;

        final HashMap<String, Method> methodMap = mMethodMap;
        fillMap(methodMap, targetClass.getMethods());

        final HashMap<String, Method> declaredMethodMap = new HashMap<String, Method>();

        fillMap(declaredMethodMap, targetClass.getDeclaredMethods());

        for (final Entry<String, Method> methodEntry : declaredMethodMap.entrySet()) {

            final String name = methodEntry.getKey();

            if (!methodMap.containsKey(name)) {

                methodMap.put(name, methodEntry.getValue());
            }
        }
    }

    /**
     * Applies the configuration inferred from the specified annotation attributes to the passed
     * builder.
     *
     * @param builder    the builder.
     * @param annotation the annotation.
     * @return the passed builder.
     * @throws NullPointerException if any of the passed parameter is null.
     */
    protected static RoutineConfigurationBuilder applyConfiguration(
            final RoutineConfigurationBuilder builder, final Async annotation) {

        final Class<? extends Runner> runnerClass = annotation.runnerClass();

        if (runnerClass != DefaultRunner.class) {

            try {

                builder.runBy(runnerClass.newInstance());

            } catch (final InstantiationException e) {

                throw new RoutineException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RoutineException(e);
            }
        }

        builder.syncRunner(annotation.runnerType());
        builder.maxRunning(annotation.maxRunning());
        builder.maxRetained(annotation.maxRetained());

        final long availTimeout = annotation.availTimeout();

        if (availTimeout != DEFAULT) {

            builder.availableTimeout(fromUnit(availTimeout, annotation.availTimeUnit()));
        }

        builder.inputMaxSize(annotation.maxInput());

        final long inputTimeout = annotation.inputTimeout();

        if (inputTimeout != DEFAULT) {

            builder.inputTimeout(fromUnit(inputTimeout, annotation.inputTimeUnit()));
        }

        builder.inputOrder(annotation.inputOrder());

        builder.outputMaxSize(annotation.maxOutput());

        final long outputTimeout = annotation.outputTimeout();

        if (outputTimeout != DEFAULT) {

            builder.outputTimeout(fromUnit(outputTimeout, annotation.outputTimeUnit()));
        }

        builder.outputOrder(annotation.outputOrder());

        final Class<? extends Log> logClass = annotation.log();

        if (logClass != DefaultLog.class) {

            try {

                builder.loggedWith(logClass.newInstance());

            } catch (final InstantiationException e) {

                throw new RoutineException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RoutineException(e);
            }
        }

        builder.logLevel(annotation.logLevel());

        return builder;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        mBuilder.apply(configuration);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        mBuilder.availableTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder availableTimeout(@Nullable final TimeDuration timeout) {

        mBuilder.availableTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder inputMaxSize(final int inputMaxSize) {

        mBuilder.inputMaxSize(inputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder inputOrder(@Nonnull final DataOrder order) {

        mBuilder.inputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder inputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        mBuilder.inputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder inputTimeout(@Nullable final TimeDuration timeout) {

        mBuilder.inputTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder logLevel(@Nonnull final LogLevel level) {

        mBuilder.logLevel(level);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder loggedWith(@Nullable final Log log) {

        mBuilder.loggedWith(log);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxRetained(final int maxRetainedInstances) {

        mBuilder.maxRetained(maxRetainedInstances);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxRunning(final int maxRunningInstances) {

        mBuilder.maxRunning(maxRunningInstances);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder outputMaxSize(final int outputMaxSize) {

        mBuilder.outputMaxSize(outputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder outputOrder(@Nonnull final DataOrder order) {

        mBuilder.outputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder outputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        mBuilder.outputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder outputTimeout(@Nullable final TimeDuration timeout) {

        mBuilder.outputTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder runBy(@Nullable final Runner runner) {

        mBuilder.runBy(runner);

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder syncRunner(@Nonnull final RunnerType type) {

        mBuilder.syncRunner(type);

        return this;
    }

    /**
     * Returns a routine used for calling the method whose identifying tag is specified in a
     * {@link Async} annotation.
     *
     * @param tag the tag specified in the annotation.
     * @return the routine.
     * @throws IllegalArgumentException if the specified method is not found.
     * @throws RoutineException         if an error occurred while instantiating the optional
     *                                  runner or the routine.
     */
    @Nonnull
    public Routine<Object, Object> asyncMethod(@Nonnull final String tag) {

        final Method method = mMethodMap.get(tag);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with tag '" + tag + "' has been found");
        }

        return method(method);
    }

    /**
     * Tells the builder to create a routine using the specified lock ID.
     *
     * @param id the lock ID.
     * @return this builder.
     * @see Async
     */
    @Nonnull
    public ClassRoutineBuilder lockId(@Nullable final String id) {

        mLockId = id;

        return this;
    }

    /**
     * Returns a routine used for calling the specified method.
     * <p/>
     * The method is searched via reflection ignoring an optional tag specified in a {@link Async}
     * annotation. Though, the other annotation attributes will be honored.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws NullPointerException     if one of the parameter is null.
     * @throws IllegalArgumentException if no matching method is found.
     * @throws RoutineException         if an error occurred while instantiating the optional
     *                                  runner or the routine.
     */
    @Nonnull
    public Routine<Object, Object> method(@Nonnull final String name,
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

    /**
     * Returns a routine used for calling the specified method.
     * <p/>
     * The method is invoked ignoring an optional tag specified in a {@link Async} annotation.
     * Though, the other annotation attributes will be honored.
     *
     * @param method the method instance.
     * @return the routine.
     * @throws NullPointerException if the specified method is null.
     * @throws RoutineException     if an error occurred while instantiating the optional runner
     *                              or the routine.
     */
    @Nonnull
    public Routine<Object, Object> method(@Nonnull final Method method) {

        if (!method.isAccessible()) {

            AccessController.doPrivileged(new SetAccessibleAction(method));
        }

        String lockId = mLockId;
        final DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        final Async annotation = method.getAnnotation(Async.class);

        if (annotation != null) {

            if (lockId == null) {

                lockId = annotation.lockId();
            }

            applyConfiguration(builder, annotation);
        }

        return getRoutine(builder.apply(mBuilder.buildConfiguration()).buildConfiguration(), method,
                          lockId);
    }

    /**
     * Gets the annotated method associated to the specified tag.
     *
     * @param tag the tag specified in the annotation.
     * @return the method or null.
     */
    @Nullable
    protected Method getAnnotatedMethod(@Nonnull final String tag) {

        return mMethodMap.get(tag);
    }

    /**
     * Returns the internal configurator builder.
     *
     * @return the configurator builder.
     */
    protected RoutineConfigurationBuilder getBuilder() {

        return mBuilder;
    }

    /**
     * Returns the ID of the lock.
     *
     * @return the lock ID.
     */
    protected String getLockId() {

        return mLockId;
    }

    /**
     * Creates the routine.
     *
     * @param configuration the routine configuration.
     * @param method        the method to wrap.
     * @param lockId        the lock ID.
     * @return the routine instance.
     */
    @Nonnull
    protected Routine<Object, Object> getRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final Method method, @Nullable final String lockId) {

        Routine<Object, Object> routine;

        synchronized (sRoutineCache) {

            final Object target = (mTarget != null) ? mTarget : mTargetClass;

            final CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<Object, Object>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<Object, Object>>();
                routineCache.put(target, routineMap);
            }

            final String routineLockId = (lockId != null) ? lockId : "";
            final RoutineInfo routineInfo = new RoutineInfo(configuration, method, routineLockId);
            routine = routineMap.get(routineInfo);

            if (routine != null) {

                return routine;
            }

            Object mutex = null;

            if (!Async.UNLOCKED.equals(routineLockId)) {

                synchronized (sMutexCache) {

                    final CacheHashMap<Object, Map<String, Object>> mutexCache = sMutexCache;
                    Map<String, Object> mutexMap = mutexCache.get(target);

                    if (mutexMap == null) {

                        mutexMap = new HashMap<String, Object>();
                        mutexCache.put(target, mutexMap);
                    }

                    mutex = mutexMap.get(routineLockId);

                    if (mutex == null) {

                        mutex = new Object();
                        mutexMap.put(routineLockId, mutex);
                    }
                }
            }

            final Class<?> targetClass = mTargetClass;
            final Runner syncRunner = (configuration.getSyncRunner(null) == RunnerType.SEQUENTIAL)
                    ? Runners.sequentialRunner() : Runners.queuedRunner();

            routine = new DefaultRoutine<Object, Object>(configuration, syncRunner,
                                                         MethodSimpleInvocation.class, target,
                                                         targetClass, method, mutex);
            routineMap.put(routineInfo, routine);
        }

        return routine;
    }

    private void fillMap(@Nonnull final HashMap<String, Method> map,
            @Nonnull final Method[] methods) {

        final boolean isClass = mIsClass;

        for (final Method method : methods) {

            final int staticFlag = method.getModifiers() & Modifier.STATIC;

            if (isClass) {

                if (staticFlag == 0) {

                    continue;
                }

            } else if (staticFlag != 0) {

                continue;
            }

            final Async annotation = method.getAnnotation(Async.class);

            if (annotation != null) {

                String tag = annotation.value();

                if ((tag == null) || (tag.length() == 0)) {

                    tag = method.getName();
                }

                if (map.containsKey(tag)) {

                    throw new IllegalArgumentException(
                            "the tag '" + tag + "' has already been used to identify a different"
                                    + " method");
                }

                map.put(tag, method);
            }
        }
    }

    /**
     * Implementation of a simple invocation wrapping the target method.
     */
    private static class MethodSimpleInvocation extends SimpleInvocation<Object, Object> {

        private final boolean mHasResult;

        private final Method mMethod;

        private final Object mMutex;

        private final Object mTarget;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param target      the target object.
         * @param targetClass the target class.
         * @param method      the method to wrap.
         * @param mutex       the mutex used for synchronization.
         */
        public MethodSimpleInvocation(@Nullable final Object target,
                @Nonnull final Class<?> targetClass, @Nonnull final Method method,
                @Nullable final Object mutex) {

            mTarget = target;
            mTargetClass = targetClass;
            mMethod = method;
            mMutex = (mutex != null) ? mutex : this;

            final Class<?> returnType = method.getReturnType();
            mHasResult = !Void.class.equals(boxingClass(returnType));
        }

        @Override
        public void onExec(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> results) {

            synchronized (mMutex) {

                final Object target = mTarget;
                final Method method = mMethod;

                try {

                    final Object result =
                            method.invoke(target, objects.toArray(new Object[objects.size()]));

                    if (mHasResult) {

                        results.pass(result);
                    }

                } catch (final InvocationTargetException e) {

                    throw new RoutineInvocationException(e.getCause(), target, mTargetClass,
                                                         method.getName(),
                                                         method.getParameterTypes());

                } catch (final RoutineException e) {

                    throw e;

                } catch (final Throwable t) {

                    throw new RoutineException(t);
                }
            }
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static class RoutineInfo {

        private final RoutineConfiguration mConfiguration;

        private final String mLockId;

        private final Method mMethod;

        /**
         * Constructor.
         *
         * @param configuration the routine configuration.
         * @param method        the method to wrap.
         * @param lockId        the lock ID.
         */
        private RoutineInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String lockId) {

            mMethod = method;
            mLockId = lockId;
            mConfiguration = configuration;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mLockId.hashCode();
            result = 31 * result + mMethod.hashCode();
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

            return mConfiguration.equals(that.mConfiguration) && mLockId.equals(that.mLockId)
                    && mMethod.equals(that.mMethod);
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

        @Override
        public Void run() {

            mMethod.setAccessible(true);

            return null;
        }
    }
}
