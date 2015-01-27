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

import com.bmd.jrt.annotation.AsyncName;
import com.bmd.jrt.annotation.LockName;
import com.bmd.jrt.annotation.ResultTimeout;
import com.bmd.jrt.builder.RoutineBuilder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.common.InvocationInterruptedException;
import com.bmd.jrt.invocation.SimpleInvocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
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

import static com.bmd.jrt.common.Reflection.boxingClass;

/**
 * Class implementing a builder of routines wrapping a class method.
 * <p/>
 * Note that only static methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see com.bmd.jrt.annotation.AsyncName
 * @see com.bmd.jrt.annotation.LockName
 * @see com.bmd.jrt.annotation.ResultTimeout
 */
public class ClassRoutineBuilder implements RoutineBuilder {

    protected static final CacheHashMap<Object, Map<String, Object>> sMutexCache =
            new CacheHashMap<Object, Map<String, Object>>();

    private static final CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>
            sRoutineCache =
            new CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>();

    private final RoutineConfigurationBuilder mBuilder = new RoutineConfigurationBuilder();

    private final HashMap<String, Method> mMethodMap = new HashMap<String, Method>();

    private final Object mTarget;

    private final Class<?> mTargetClass;

    private final WeakReference<?> mTargetReference;

    private String mLockName;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected, or the
     *                                  specified class represents an interface.
     */
    ClassRoutineBuilder(@Nonnull final Class<?> targetClass) {

        if (targetClass.isInterface()) {

            throw new IllegalArgumentException("the target class must not be an interface");
        }

        mTargetClass = targetClass;
        mTarget = null;
        mTargetReference = null;
        fillMethodMap(true);
    }

    /**
     * Constructor.
     *
     * @param target the target object.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ClassRoutineBuilder(@Nonnull final Object target) {

        mTargetClass = target.getClass();
        mTarget = target;
        mTargetReference = null;
        fillMethodMap(false);
    }

    /**
     * Constructor.
     *
     * @param targetReference the reference to the target object.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ClassRoutineBuilder(@Nonnull final WeakReference<?> targetReference) {

        final Object target = targetReference.get();

        if (target == null) {

            throw new IllegalStateException("target object has been destroyed");
        }

        mTargetClass = target.getClass();
        mTarget = null;
        mTargetReference = targetReference;
        fillMethodMap(false);
    }

    /**
     * Returns a routine used for calling the method whose identifying name is specified in a
     * {@link AsyncName} annotation.
     *
     * @param name     the name specified in the annotation.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     * @throws IllegalArgumentException if the specified method is not found.
     */
    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> annotatedMethod(@Nonnull final String name) {

        final Method method = mMethodMap.get(name);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        return method(method);
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
    public ClassRoutineBuilder onResultTimeout(@Nonnull final TimeoutAction action) {

        mBuilder.onResultTimeout(action);
        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder resultTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        mBuilder.resultTimeout(timeout, timeUnit);
        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder resultTimeout(@Nullable final TimeDuration timeout) {

        mBuilder.resultTimeout(timeout);
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
     * Tells the builder to create a routine using the specified lock name.
     *
     * @param lockName the lock name.
     * @return this builder.
     * @see LockName
     */
    @Nonnull
    public ClassRoutineBuilder lockName(@Nullable final String lockName) {

        mLockName = lockName;
        return this;
    }

    //TODO: annotation documentation

    /**
     * Returns a routine used for calling the specified method.
     * <p/>
     * The method is searched via reflection ignoring an optional name specified in a
     * {@link AsyncName} annotation. Though, the other annotation attributes will be honored.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws NullPointerException     if one of the parameter is null.
     * @throws IllegalArgumentException if no matching method is found.
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
     * The method is invoked ignoring an optional name specified in a {@link AsyncName} annotation.
     * Though, the other annotation attributes will be honored.
     *
     * @param method   the method instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     * @throws NullPointerException if the specified method is null.
     */
    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(mBuilder.buildConfiguration(), mLockName, method);
    }

    /**
     * Gets the annotated method associated to the specified name.
     *
     * @param name the name specified in the annotation.
     * @return the method or null.
     */
    @Nullable
    protected Method getAnnotatedMethod(final String name) {

        return mMethodMap.get(name);
    }

    /**
     * Returns the internal configurator builder.
     *
     * @return the configurator builder.
     */
    @Nonnull
    protected RoutineConfigurationBuilder getBuilder() {

        return mBuilder;
    }

    /**
     * Returns the name of the lock.
     *
     * @return the lock name.
     */
    @Nullable
    protected String getLockName() {

        return mLockName;
    }

    /**
     * Creates the routine.
     *
     * @param configuration the routine configuration.
     * @param lockName      the lock name.
     * @param method        the method to wrap.
     * @param <INPUT>       the input data type.
     * @param <OUTPUT>      the output data type.
     * @return the routine instance.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getRoutine(
            @Nonnull final RoutineConfiguration configuration, @Nullable final String lockName,
            @Nonnull final Method method) {

        if (!method.isAccessible()) {

            AccessController.doPrivileged(new SetAccessibleAction(method));
        }

        Routine<Object, Object> routine;

        synchronized (sRoutineCache) {

            final Object target = (mTargetReference != null) ? mTargetReference.get()
                    : (mTarget != null) ? mTarget : mTargetClass;

            if (target == null) {

                throw new IllegalStateException("target object has been destroyed");
            }

            final CacheHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<Object, Object>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<Object, Object>>();
                routineCache.put(target, routineMap);
            }

            final String methodLockName = (lockName != null) ? lockName : LockName.DEFAULT_LOCK;
            final RoutineInfo routineInfo = new RoutineInfo(configuration, method, methodLockName);
            routine = routineMap.get(routineInfo);

            if (routine != null) {

                return (Routine<INPUT, OUTPUT>) routine;
            }

            Object mutex = null;

            if (!LockName.NULL_LOCK.equals(methodLockName)) {

                synchronized (sMutexCache) {

                    final CacheHashMap<Object, Map<String, Object>> mutexCache = sMutexCache;
                    Map<String, Object> mutexMap = mutexCache.get(target);

                    if (mutexMap == null) {

                        mutexMap = new HashMap<String, Object>();
                        mutexCache.put(target, mutexMap);
                    }

                    mutex = mutexMap.get(methodLockName);

                    if (mutex == null) {

                        mutex = new Object();
                        mutexMap.put(methodLockName, mutex);
                    }
                }
            }

            routine =
                    new DefaultRoutine<Object, Object>(configuration, MethodSimpleInvocation.class,
                                                       mTargetReference, mTarget, method, mutex);
            routineMap.put(routineInfo, routine);
        }

        return (Routine<INPUT, OUTPUT>) routine;
    }

    /**
     * Returns the builder target object.
     *
     * @return the target object.
     */
    @Nullable
    protected Object getTarget() {

        return mTarget;
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
     * Returns a routine used for calling the specified method.
     *
     * @param configuration the routine configuration.
     * @param lockName      the lock name.
     * @param targetMethod  the target method.
     * @return the routine.
     * @throws NullPointerException if the specified configuration, class or method are null.
     */
    @Nonnull
    protected <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(
            @Nonnull final RoutineConfiguration configuration, @Nullable final String lockName,
            @Nonnull final Method targetMethod) {

        String methodLockName = lockName;
        final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
        final LockName lockAnnotation = targetMethod.getAnnotation(LockName.class);

        if (lockAnnotation != null) {

            final String annotationLockName = lockAnnotation.value();

            if (!LockName.DEFAULT_LOCK.equals(annotationLockName)) {

                methodLockName = annotationLockName;
            }
        }

        builder.apply(configuration)
               .inputSize(Integer.MAX_VALUE)
               .inputTimeout(TimeDuration.ZERO)
               .outputSize(Integer.MAX_VALUE)
               .outputTimeout(TimeDuration.ZERO);

        final ResultTimeout timeoutAnnotation = targetMethod.getAnnotation(ResultTimeout.class);

        if (timeoutAnnotation != null) {

            builder.resultTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit())
                   .onResultTimeout(timeoutAnnotation.action());
        }

        return getRoutine(builder.buildConfiguration(), methodLockName, targetMethod);
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

            final AsyncName annotation = method.getAnnotation(AsyncName.class);

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
    private static class MethodSimpleInvocation extends SimpleInvocation<Object, Object> {

        private final boolean mHasResult;

        private final Method mMethod;

        private final Object mMutex;

        private final Object mTarget;

        private final WeakReference<?> mTargetReference;

        /**
         * Constructor.
         *
         * @param targetReference the reference to the target object.
         * @param target          the target object.
         * @param method          the method to wrap.
         * @param mutex           the mutex used for synchronization.
         */
        public MethodSimpleInvocation(final WeakReference<?> targetReference,
                @Nullable final Object target, @Nonnull final Method method,
                @Nullable final Object mutex) {

            mTargetReference = targetReference;
            mTarget = target;
            mMethod = method;
            mMutex = (mutex != null) ? mutex : this;

            final Class<?> returnType = method.getReturnType();
            mHasResult = !Void.class.equals(boxingClass(returnType));
        }

        @Override
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            synchronized (mMutex) {

                final Object target;
                final WeakReference<?> targetReference = mTargetReference;

                if (targetReference != null) {

                    target = targetReference.get();

                    if (target == null) {

                        throw new IllegalStateException("target object has been destroyed");
                    }

                } else {

                    target = mTarget;
                }

                final Method method = mMethod;

                try {

                    final Object methodResult =
                            method.invoke(target, objects.toArray(new Object[objects.size()]));

                    if (mHasResult) {

                        result.pass(methodResult);
                    }

                } catch (final InvocationTargetException e) {

                    throw new InvocationException(e.getCause());

                } catch (final InvocationInterruptedException e) {

                    throw e.interrupt();

                } catch (final InvocationException e) {

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
    private static class RoutineInfo {

        private final RoutineConfiguration mConfiguration;

        private final String mLockName;

        private final Method mMethod;

        /**
         * Constructor.
         *
         * @param configuration the routine configuration.
         * @param method        the method to wrap.
         * @param lockName      the lock name.
         */
        private RoutineInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String lockName) {

            mMethod = method;
            mLockName = lockName;
            mConfiguration = configuration;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mLockName.hashCode();
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
            return mConfiguration.equals(that.mConfiguration) && mLockName.equals(that.mLockName)
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
