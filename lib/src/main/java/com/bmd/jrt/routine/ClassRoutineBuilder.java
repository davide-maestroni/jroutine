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
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.SimpleInvocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
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

    private static final ClassToken<MethodSimpleInvocation> ASYNC_INVOCATION_TOKEN =
            ClassToken.tokenOf(MethodSimpleInvocation.class);

    private static final WeakHashMap<Object, HashMap<String, Object>> sMutexCache =
            new WeakHashMap<Object, HashMap<String, Object>>();

    private static final WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>
            sRoutineCache =
            new WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>>();

    private final boolean mIsClass;

    private final HashMap<String, Method> mMethodMap = new HashMap<String, Method>();

    private final Object mTarget;

    private final Class<?> mTargetClass;

    private TimeDuration mAvailTimeout;

    private TimeDuration mInputTimeout;

    private Boolean mIsSequential;

    private String mLockId;

    private Log mLog;

    private LogLevel mLogLevel;

    private int mMaxInputSize = Async.DEFAULT_NUMBER;

    private int mMaxOutputSize = Async.DEFAULT_NUMBER;

    private int mMaxRetained = Async.DEFAULT_NUMBER;

    private int mMaxRunning = Async.DEFAULT_NUMBER;

    private Boolean mOrderedInput;

    private Boolean mOrderedOutput;

    private TimeDuration mOutputTimeout;

    private Runner mRunner;

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

    @Nonnull
    @Override
    public ClassRoutineBuilder availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return availableTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder availableTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mAvailTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder inputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        return inputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder inputTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mInputTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder logLevel(@Nonnull final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        mLogLevel = level;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxInputSize(final int maxInputSize) {

        if (maxInputSize <= 0) {

            throw new IllegalArgumentException("the buffer size cannot be 0 or negative");
        }

        mMaxInputSize = maxInputSize;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxOutputSize(final int maxOutputSize) {

        if (maxOutputSize <= 0) {

            throw new IllegalArgumentException("the buffer size cannot be 0 or negative");
        }

        mMaxOutputSize = maxOutputSize;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxRetained(final int maxRetainedInstances) {

        if (maxRetainedInstances < 0) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mMaxRetained = maxRetainedInstances;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder maxRunning(final int maxRunningInstances) {

        if (maxRunningInstances < 1) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxRunning = maxRunningInstances;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder orderedInput() {

        mOrderedInput = true;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder orderedOutput() {

        mOrderedOutput = true;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder outputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        return outputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder outputTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mOutputTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder queued() {

        mIsSequential = false;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ClassRoutineBuilder runBy(@Nonnull final Runner runner) {

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mRunner = runner;

        return this;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder sequential() {

        mIsSequential = true;

        return this;
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
        Runner runner = mRunner;
        Boolean isSequential = mIsSequential;
        int maxRunning = mMaxRunning;
        int maxRetained = mMaxRetained;
        TimeDuration availTimeout = mAvailTimeout;
        int maxInputSize = mMaxInputSize;
        TimeDuration inputTimeout = mInputTimeout;
        Boolean orderedInput = mOrderedInput;
        int maxOutputSize = mMaxOutputSize;
        TimeDuration outputTimeout = mOutputTimeout;
        Boolean orderedOutput = mOrderedOutput;
        Log log = mLog;
        LogLevel logLevel = mLogLevel;

        final Async annotation = method.getAnnotation(Async.class);

        if (annotation != null) {

            if (lockId == null) {

                lockId = annotation.lockId();
            }

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

            if (maxRunning == Async.DEFAULT_NUMBER) {

                maxRunning = annotation.maxRunning();
            }

            if (maxRetained == Async.DEFAULT_NUMBER) {

                maxRetained = annotation.maxRetained();
            }

            if (availTimeout == null) {

                final long timeout = annotation.availTimeout();

                if (timeout != Async.DEFAULT_NUMBER) {

                    availTimeout = fromUnit(timeout, annotation.availTimeUnit());
                }
            }

            if (maxInputSize == Async.DEFAULT_NUMBER) {

                maxInputSize = annotation.maxInput();
            }

            if (inputTimeout == null) {

                final long timeout = annotation.inputTimeout();

                if (timeout != Async.DEFAULT_NUMBER) {

                    inputTimeout = fromUnit(timeout, annotation.inputTimeUnit());
                }
            }

            if (orderedInput == null) {

                orderedInput = annotation.orderedInput();
            }

            if (maxOutputSize == Async.DEFAULT_NUMBER) {

                maxOutputSize = annotation.maxOutput();
            }

            if (outputTimeout == null) {

                final long timeout = annotation.outputTimeout();

                if (timeout != Async.DEFAULT_NUMBER) {

                    outputTimeout = fromUnit(timeout, annotation.outputTimeUnit());
                }
            }

            if (orderedOutput == null) {

                orderedOutput = annotation.orderedOutput();
            }

            if (log == null) {

                final Class<? extends Log> logClass = annotation.log();

                if (logClass != DefaultLog.class) {

                    try {

                        log = logClass.newInstance();

                    } catch (final InstantiationException e) {

                        throw new RoutineException(e);

                    } catch (IllegalAccessException e) {

                        throw new RoutineException(e);
                    }
                }
            }

            if (logLevel == null) {

                logLevel = annotation.logLevel();
            }
        }

        return getRoutine(method, lockId, runner, isSequential, maxRunning, maxRetained,
                          availTimeout, maxInputSize, inputTimeout, orderedInput, maxOutputSize,
                          outputTimeout, orderedOutput, log, logLevel);
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
     * Returns the maximum timeout while waiting for an invocation instance to be available.
     *
     * @return the invocation instance availability timeout.
     */
    protected TimeDuration getAvailTimeout() {

        return mAvailTimeout;
    }

    /**
     * Returns the maximum timeout while waiting for an output to be passed to the result channel.
     *
     * @return the input timeout.
     */
    protected TimeDuration getInputTimeout() {

        return mInputTimeout;
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
     * Returns the log instance set.
     *
     * @return the log instance.
     */
    protected Log getLog() {

        return mLog;
    }

    /**
     * Returns the log level set.
     *
     * @return the log level.
     */
    protected LogLevel getLogLevel() {

        return mLogLevel;
    }

    /**
     * Returns the maximum number of buffered input data.
     *
     * @return the maximum input channel size.
     */
    protected int getMaxInputSize() {

        return mMaxInputSize;
    }

    /**
     * the maximum number of buffered output data.
     *
     * @return the maximum output channel size.
     */
    protected int getMaxOutputSize() {

        return mMaxOutputSize;
    }

    /**
     * Returns the max number of retained instances.
     *
     * @return the max retained instances.
     */
    protected int getMaxRetained() {

        return mMaxRetained;
    }

    /**
     * Returns the max number of concurrently running instances.
     *
     * @return the max concurrently running instances.
     */
    protected int getMaxRunning() {

        return mMaxRunning;
    }

    /**
     * Returns the maximum timeout while waiting for an output to be passed to the result channel.
     *
     * @return the output timeout.
     */
    protected TimeDuration getOutputTimeout() {

        return mOutputTimeout;
    }

    /**
     * Creates the routine.
     *
     * @param method        the method to wrap.
     * @param lockId        the lock ID.
     * @param runner        the asynchronous runner instance.
     * @param isSequential  whether a sequential runner must be used for synchronous invocations.
     * @param maxRunning    the max number of concurrently running instances.
     * @param maxRetained   the max number of retained instances.
     * @param availTimeout  the maximum timeout while waiting for an invocation instance to be
     *                      available.
     * @param maxInputSize  the maximum number of buffered input data. Must be positive.
     * @param inputTimeout  the maximum timeout while waiting for an input to be passed to the
     *                      input channel.
     * @param orderedInput  whether the input data are forced to be delivered in insertion order.
     * @param maxOutputSize the maximum number of buffered output data. Must be positive.
     * @param outputTimeout the maximum timeout while waiting for an output to be passed to the
     *                      result channel.
     * @param orderedOutput whether the output data are forced to be delivered in insertion order.
     * @param log           the log instance.
     * @param level         the log level.
     * @return the routine instance.
     */
    @Nonnull
    protected Routine<Object, Object> getRoutine(@Nonnull final Method method,
            @Nullable final String lockId, @Nullable final Runner runner,
            @Nullable final Boolean isSequential, final int maxRunning, final int maxRetained,
            @Nullable final TimeDuration availTimeout, final int maxInputSize,
            @Nullable final TimeDuration inputTimeout, @Nullable final Boolean orderedInput,
            final int maxOutputSize, @Nullable final TimeDuration outputTimeout,
            @Nullable final Boolean orderedOutput, @Nullable final Log log,
            @Nullable final LogLevel level) {

        Routine<Object, Object> routine;

        synchronized (sMutexCache) {

            final Object target = mTarget;

            final WeakHashMap<Object, HashMap<RoutineInfo, Routine<Object, Object>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<Object, Object>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<Object, Object>>();
                routineCache.put(target, routineMap);
            }

            final Log routineLog = (log != null) ? log : Logger.getDefaultLog();
            final LogLevel routineLogLevel = (level != null) ? level : Logger.getDefaultLogLevel();
            final String routineLockId = (lockId != null) ? lockId : "";

            final RoutineInfo routineInfo =
                    new RoutineInfo(method, routineLockId, runner, isSequential, maxRunning,
                                    maxRetained, availTimeout, maxInputSize, inputTimeout,
                                    orderedInput, maxOutputSize, outputTimeout, orderedOutput,
                                    routineLog, routineLogLevel);
            routine = routineMap.get(routineInfo);

            if (routine != null) {

                return routine;
            }

            Object mutex = null;

            if (!Async.UNLOCKED.equals(routineLockId)) {

                final WeakHashMap<Object, HashMap<String, Object>> mutexCache = sMutexCache;
                HashMap<String, Object> mutexMap = mutexCache.get(target);

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

            final Class<?> targetClass = mTargetClass;
            final InvocationRoutineBuilder<Object, Object> builder =
                    new InvocationRoutineBuilder<Object, Object>(ASYNC_INVOCATION_TOKEN);

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

            if (maxRunning != Async.DEFAULT_NUMBER) {

                builder.maxRunning(maxRunning);
            }

            if (maxRetained != Async.DEFAULT_NUMBER) {

                builder.maxRetained(maxRetained);
            }

            if (availTimeout != null) {

                builder.availableTimeout(availTimeout);
            }

            if (maxInputSize != Async.DEFAULT_NUMBER) {

                builder.maxInputSize(maxInputSize);
            }

            if (inputTimeout != null) {

                builder.inputTimeout(inputTimeout);
            }

            if ((orderedInput != null) && orderedInput) {

                builder.orderedInput();
            }

            if (maxOutputSize != Async.DEFAULT_NUMBER) {

                builder.maxOutputSize(maxOutputSize);
            }

            if (outputTimeout != null) {

                builder.outputTimeout(outputTimeout);
            }

            if ((orderedOutput != null) && orderedOutput) {

                builder.orderedOutput();
            }

            routine = builder.loggedWith(routineLog)
                             .logLevel(routineLogLevel)
                             .withArgs(target, targetClass, method, mutex)
                             .buildRoutine();
            routineMap.put(routineInfo, routine);
        }

        return routine;
    }

    /**
     * Returns the asynchronous runner instance.
     *
     * @return the runner instance.
     */
    protected Runner getRunner() {

        return mRunner;
    }

    /**
     * Returns whether the sequential runner is set to be used as the synchronous one.
     *
     * @return whether the sequential runner is set.
     */
    protected Boolean getSequential() {

        return mIsSequential;
    }

    /**
     * Returns whether the input data are forced to be delivered in insertion order.
     *
     * @return whether the input data are ordered.
     */
    protected Boolean isOrderedInput() {

        return mOrderedInput;
    }

    /**
     * Returns whether the output data are forced to be delivered in insertion order.
     *
     * @return whether the output data are ordered.
     */
    protected Boolean isOrderedOutput() {

        return mOrderedOutput;
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

        private final TimeDuration mAvailTimeout;

        private final TimeDuration mInputTimeout;

        private final Boolean mIsSequential;

        private final String mLockId;

        private final Log mLog;

        private final LogLevel mLogLevel;

        private final int mMaxInputSize;

        private final int mMaxOutputSize;

        private final int mMaxRetained;

        private final int mMaxRunning;

        private final Method mMethod;

        private final Boolean mOrderedInput;

        private final Boolean mOrderedOutput;

        private final TimeDuration mOutputTimeout;

        private final Runner mRunner;

        /**
         * Constructor.
         *
         * @param method        the method to wrap.
         * @param lockId        the lock ID.
         * @param runner        the runner instance.
         * @param isSequential  whether a sequential runner must be used for synchronous
         * @param maxRunning    the max number of concurrently running instances.
         * @param maxRetained   the max number of retained instances.
         * @param availTimeout  the maximum timeout while waiting for an invocation instance to be
         *                      available.
         * @param maxInputSize  the maximum number of buffered input data. Must be positive.
         * @param inputTimeout  the maximum timeout while waiting for an input to be passed to the
         *                      input channel.
         * @param orderedInput  whether the input data are forced to be delivered in insertion
         *                      order.
         * @param maxOutputSize the maximum number of buffered output data. Must be positive.
         * @param outputTimeout the maximum timeout while waiting for an output to be passed to the
         *                      result channel.
         * @param orderedOutput whether the output data are forced to be delivered in insertion
         *                      order.
         * @param log           the log instance.
         * @param level         the log level.
         */
        private RoutineInfo(@Nonnull final Method method, @Nonnull final String lockId,
                @Nullable final Runner runner, @Nullable final Boolean isSequential,
                final int maxRunning, final int maxRetained,
                @Nullable final TimeDuration availTimeout, final int maxInputSize,
                @Nullable final TimeDuration inputTimeout, @Nullable final Boolean orderedInput,
                final int maxOutputSize, @Nullable final TimeDuration outputTimeout,
                @Nullable final Boolean orderedOutput, @Nonnull final Log log,
                @Nonnull final LogLevel level) {

            mMethod = method;
            mLockId = lockId;
            mRunner = runner;
            mIsSequential = isSequential;
            mMaxRunning = maxRunning;
            mMaxRetained = maxRetained;
            mAvailTimeout = availTimeout;
            mMaxInputSize = maxInputSize;
            mInputTimeout = inputTimeout;
            mOrderedInput = orderedInput;
            mMaxOutputSize = maxOutputSize;
            mOutputTimeout = outputTimeout;
            mOrderedOutput = orderedOutput;
            mLog = log;
            mLogLevel = level;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mAvailTimeout != null ? mAvailTimeout.hashCode() : 0;
            result = 31 * result + (mInputTimeout != null ? mInputTimeout.hashCode() : 0);
            result = 31 * result + (mIsSequential != null ? mIsSequential.hashCode() : 0);
            result = 31 * result + mLockId.hashCode();
            result = 31 * result + mLog.hashCode();
            result = 31 * result + mLogLevel.hashCode();
            result = 31 * result + mMaxInputSize;
            result = 31 * result + mMaxOutputSize;
            result = 31 * result + mMaxRetained;
            result = 31 * result + mMaxRunning;
            result = 31 * result + mMethod.hashCode();
            result = 31 * result + (mOrderedInput != null ? mOrderedInput.hashCode() : 0);
            result = 31 * result + (mOrderedOutput != null ? mOrderedOutput.hashCode() : 0);
            result = 31 * result + (mOutputTimeout != null ? mOutputTimeout.hashCode() : 0);
            result = 31 * result + (mRunner != null ? mRunner.hashCode() : 0);
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

            return mMaxInputSize == that.mMaxInputSize && mMaxOutputSize == that.mMaxOutputSize
                    && mMaxRetained == that.mMaxRetained && mMaxRunning == that.mMaxRunning && !(
                    mAvailTimeout != null ? !mAvailTimeout.equals(that.mAvailTimeout)
                            : that.mAvailTimeout != null) && !(mInputTimeout != null
                    ? !mInputTimeout.equals(that.mInputTimeout) : that.mInputTimeout != null) && !(
                    mIsSequential != null ? !mIsSequential.equals(that.mIsSequential)
                            : that.mIsSequential != null) && mLockId.equals(that.mLockId)
                    && mLog.equals(that.mLog) && mLogLevel == that.mLogLevel && mMethod.equals(
                    that.mMethod) && !(mOrderedInput != null ? !mOrderedInput.equals(
                    that.mOrderedInput) : that.mOrderedInput != null) && !(mOrderedOutput != null
                    ? !mOrderedOutput.equals(that.mOrderedOutput) : that.mOrderedOutput != null)
                    && !(mOutputTimeout != null ? !mOutputTimeout.equals(that.mOutputTimeout)
                    : that.mOutputTimeout != null) && !(mRunner != null ? !mRunner.equals(
                    that.mRunner) : that.mRunner != null);
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
