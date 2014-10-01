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

import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.execution.ExecutionBody;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Class implementing a builder of a routine wrapping a class method.
 * <p/>
 * TODO: static, annotation, etc.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see Async
 */
public class ClassRoutineBuilder {

    private static final WeakHashMap<Object, Object> sMutexMap = new WeakHashMap<Object, Object>();

    private static final WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>
            sRoutineCache =
            new WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>();

    private final boolean mIsClass;

    private final HashMap<String, Method> mMethodMap = new HashMap<String, Method>();

    private final Object mTarget;

    private final Class<?> mTargetClass;

    private Boolean mIsSequential;

    private Runner mRunner;

    //TODO: onException

    /**
     * Constructor.
     *
     * @param target the target class or object.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ClassRoutineBuilder(final Object target) {

        if (target == null) {

            throw new NullPointerException("the target object must not be null");
        }

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
     * Returns a routine used for calling the specified method.
     * <p/>
     * The method is searched via reflection ignoring an optional name specified in a
     * {@link Async} annotation. Though, the other annotation attributes
     * will be honored.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws NullPointerException     if one of the parameter is null.
     * @throws IllegalArgumentException if no matching method is found.
     * @throws RoutineException         if an error occurred while instantiating the optional
     *                                  runner or the routine.
     */
    public Routine<Object, Object> classMethod(final String name,
            final Class<?>... parameterTypes) {

        if (name == null) {

            throw new NullPointerException("the method name must not be null");
        }

        if (parameterTypes == null) {

            throw new NullPointerException("the list of parameter types must not be null");
        }

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

        return classMethod(targetMethod);
    }

    /**
     * Returns a routine used for calling the specified method.
     * <p/>
     * The method is invoked ignoring an optional name specified in a
     * {@link Async} annotation. Though, the other annotation attributes
     * will be honored.
     *
     * @param method the method instance.
     * @return the routine.
     * @throws NullPointerException if the specified method is null.
     * @throws RoutineException     if an error occurred while instantiating the optional runner
     *                              or the routine.
     */
    public Routine<Object, Object> classMethod(final Method method) {

        if (method == null) {

            throw new NullPointerException("the method must not be null");
        }

        if (!method.isAccessible()) {

            method.setAccessible(true);
        }

        Runner runner = mRunner;
        Boolean isSequential = mIsSequential;

        final Async annotation = method.getAnnotation(Async.class);

        if (annotation != null) {

            if (runner == null) {

                final Class<? extends Runner> runnerClass = annotation.runner();

                if (runnerClass != DefaultRunner.class) {

                    try {

                        runner = runnerClass.newInstance();

                    } catch (final InstantiationException e) {

                        throw new RoutineException(e);

                    } catch (IllegalAccessException e) {

                        throw new RoutineException(e);
                    }
                }
            }

            if (isSequential == null) {

                isSequential = annotation.sequential();
            }
        }

        return getRoutine(method, runner, isSequential, false);
    }

    /**
     * Returns a routine used for calling the method whose identifying name is specified in a
     * {@link Async} annotation.
     *
     * @param name the name specified in the annotation.
     * @return the routine.
     * @throws IllegalArgumentException if the specified method is not found.
     * @throws RoutineException         if an error occurred while instantiating the optional
     *                                  runner or the routine.
     */
    public Routine<Object, Object> method(final String name) {

        final Method method = mMethodMap.get(name);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        return classMethod(method);
    }

    /**
     * Tells the builder to create a routine using a queued runner for synchronous invocations.
     *
     * @return this builder.
     */
    public ClassRoutineBuilder queued() {

        mIsSequential = false;

        return this;
    }

    /**
     * Tells the builder to create a routine using the specified runner instance for asynchronous
     * invocations.
     *
     * @param runner the runner instance.
     * @return this builder.
     * @throws NullPointerException if the specified runner is null.
     */
    public ClassRoutineBuilder runBy(final Runner runner) {

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mRunner = runner;

        return this;
    }

    /**
     * Tells the builder to create a routine using a sequential runner for synchronous invocations.
     *
     * @return this builder.
     */
    public ClassRoutineBuilder sequential() {

        mIsSequential = true;

        return this;
    }

    /**
     * Creates the routine.
     *
     * @param method       the method to wrap.
     * @param runner       the asynchronous runner instance.
     * @param isSequential whether a sequential runner must be used for synchronous invocations.
     * @param orderedInput whether the input data are forced to be delivered in insertion order.
     * @return the routine instance.
     */
    protected Routine<Object, Object> getRoutine(final Method method, final Runner runner,
            final Boolean isSequential, final boolean orderedInput) {

        final Object target = mTarget;
        Routine<Object, Object> routine;

        synchronized (sMutexMap) {

            final WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<Object, Object>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<Object, Object>>();
                routineCache.put(target, routineMap);
            }

            final RoutineInfo routineInfo =
                    new RoutineInfo(method, runner, isSequential, orderedInput);
            routine = routineMap.get(routineInfo);

            if (routine != null) {

                return routine;
            }

            final WeakHashMap<Object, Object> mutexMap = sMutexMap;
            Object mutex = mutexMap.get(target);

            if (mutex == null) {

                mutex = new Object();
                mutexMap.put(target, mutex);
            }

            final RoutineBuilder<Object, Object> builder =
                    new RoutineBuilder<Object, Object>(ClassToken.token(MethodExecutionBody.class));

            if (runner != null) {

                builder.runBy(runner);
            }

            if (isSequential != null) {

                if (isSequential) {

                    builder.sequential();

                } else {

                    builder.queued();
                }
            }

            if (orderedInput) {

                builder.orderedInput();
            }

            routine = builder.withArgs(target, method, mutex).routine();
            routineMap.put(routineInfo, routine);
        }

        return routine;
    }

    private void fillMap(final HashMap<String, Method> map, final Method[] methods) {

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

                String name = annotation.name();

                if ((name == null) || (name.length() == 0)) {

                    name = method.getName();
                }

                if (map.containsKey(name)) {

                    throw new IllegalArgumentException(
                            "the name '" + name + "' has already been used to identify a different"
                                    + " method");
                }

                map.put(name, method);
            }
        }
    }

    /**
     * Implementation of an execution body wrapping the target method.
     */
    private static class MethodExecutionBody extends ExecutionBody<Object, Object> {

        private final boolean mHasResult;

        private final Method mMethod;

        private final Object mMutex;

        private final Object mTarget;

        /**
         * Constructor.
         *
         * @param target the target class or object.
         * @param method the method to wrap.
         * @param mutex  the mutex used for synchronization.
         */
        public MethodExecutionBody(final Object target, final Method method, final Object mutex) {

            mTarget = target;
            mMethod = method;
            mMutex = mutex;

            final Class<?> returnType = method.getReturnType();
            mHasResult = !ReflectionUtils.boxingClass(returnType).equals(Void.class);
        }

        @Override
        public void onExec(final List<?> objects, final ResultChannel<Object> results) {

            synchronized (mMutex) {

                try {

                    final Object result =
                            mMethod.invoke(mTarget, objects.toArray(new Object[objects.size()]));

                    if (mHasResult) {

                        results.pass(result);
                    }

                } catch (final IllegalAccessException e) {

                    throw new RoutineException(e);

                } catch (final InvocationTargetException e) {

                    throw new RoutineException(e);
                }
            }
        }
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    private static class RoutineInfo {

        private final Boolean mIsSequential;

        private final Method mMethod;

        private final boolean mOrderedInput;

        private final Runner mRunner;

        /**
         * Constructor.
         *
         * @param method       the method to wrap.
         * @param runner       the runner instance.
         * @param isSequential whether a sequential runner must be used for synchronous
         * @param orderedInput whether the input data are forced to be delivered in insertion order.
         */
        private RoutineInfo(final Method method, final Runner runner, final Boolean isSequential,
                final boolean orderedInput) {

            mMethod = method;
            mRunner = runner;
            mIsSequential = isSequential;
            mOrderedInput = orderedInput;
        }

        @Override
        public int hashCode() {

            int result = mIsSequential != null ? mIsSequential.hashCode() : 0;
            result = 31 * result + mMethod.hashCode();
            result = 31 * result + (mOrderedInput ? 1 : 0);
            result = 31 * result + (mRunner != null ? mRunner.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof RoutineInfo)) {

                return false;
            }

            final RoutineInfo that = (RoutineInfo) o;

            return mOrderedInput == that.mOrderedInput && !(mIsSequential != null
                    ? !mIsSequential.equals(that.mIsSequential) : that.mIsSequential != null)
                    && mMethod.equals(that.mMethod) && !(mRunner != null ? !mRunner.equals(
                    that.mRunner) : that.mRunner != null);
        }
    }
}