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
import com.bmd.jrt.annotation.AsyncOverride;
import com.bmd.jrt.annotation.DefaultLog;
import com.bmd.jrt.annotation.DefaultRunner;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.boxingClass;
import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see Async
 * @see AsyncOverride
 */
public class ObjectRoutineBuilder extends ClassRoutineBuilder {

    private static final WeakHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new WeakHashMap<Object, HashMap<Method, Method>>();

    private final Object mTarget;

    private final Class<?> mTargetClass;

    /**
     * Constructor.
     *
     * @param target the target object instance.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate tag in the annotations is detected.
     */
    ObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);

        mTarget = target;
        mTargetClass = target.getClass();
    }

    /**
     * Returns a proxy object enabling the asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the tag attribute
     * will be used to bind the interface method with the instance ones.  If no tag is assigned the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link AsyncOverride} annotation will be honored.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS as(@Nonnull final Class<CLASS> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        final InvocationHandler handler;

        if (itf.isAssignableFrom(mTargetClass)) {

            handler = new ObjectInvocationHandler();

        } else {

            handler = new InterfaceInvocationHandler();
        }

        final Object proxy =
                Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf}, handler);

        return itf.cast(proxy);
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder logLevel(@Nonnull final LogLevel level) {

        super.logLevel(level);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder loggedWith(@Nonnull final Log log) {

        super.loggedWith(log);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder maxRetained(final int maxRetainedInstances) {

        super.maxRetained(maxRetainedInstances);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder maxRunning(final int maxRunningInstances) {

        super.maxRunning(maxRunningInstances);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder queued() {

        super.queued();

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder runBy(@Nonnull final Runner runner) {

        super.runBy(runner);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder sequential() {

        super.sequential();

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder lockId(@Nullable final String id) {

        super.lockId(id);

        return this;
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

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

        private final Boolean mOrderedInput;

        private final Boolean mOrderedOutput;

        private final TimeDuration mOutputTimeout;

        private final Runner mRunner;

        private final Object mTarget;

        private final Class<?> mTargetClass;

        private InterfaceInvocationHandler() {

            mTarget = ObjectRoutineBuilder.this.mTarget;
            mTargetClass = ObjectRoutineBuilder.this.mTargetClass;
            mLockId = getLockId();
            mRunner = getRunner();
            mIsSequential = getSequential();
            mMaxRunning = getMaxRunning();
            mMaxRetained = getMaxRetained();
            mAvailTimeout = getAvailTimeout();
            mMaxInputSize = getMaxInputBufferSize();
            mInputTimeout = getInputTimeout();
            mOrderedInput = isOrderedInput();
            mMaxOutputSize = getMaxOutputBufferSize();
            mOutputTimeout = getOutputTimeout();
            mOrderedOutput = isOrderedOutput();
            mLog = getLog();
            mLogLevel = getLogLevel();
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final Object target = mTarget;
            final Class<?> targetClass = mTargetClass;
            final Class<?> returnType = method.getReturnType();
            boolean isOverrideParameters = false;
            boolean isOverrideResult = false;
            boolean isParallel = false;

            Method targetMethod;
            String lockId = mLockId;
            Runner runner = mRunner;
            Boolean isSequential = mIsSequential;
            int maxRunning = mMaxRunning;
            int maxRetained = mMaxRetained;
            TimeDuration availTimeout = mAvailTimeout;
            int maxInputBufferSize = mMaxInputSize;
            TimeDuration inputTimeout = mInputTimeout;
            Boolean orderedInput = mOrderedInput;
            int maxOutputBufferSize = mMaxOutputSize;
            TimeDuration outputTimeout = mOutputTimeout;
            Boolean orderedOutput = mOrderedOutput;
            Log log = mLog;
            LogLevel level = mLogLevel;

            synchronized (sMethodCache) {

                final WeakHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
                HashMap<Method, Method> methodMap = methodCache.get(target);

                if (methodMap == null) {

                    methodMap = new HashMap<Method, Method>();
                    methodCache.put(target, methodMap);
                }

                Class<?>[] parameterTypes = null;

                final AsyncOverride overrideAnnotation = method.getAnnotation(AsyncOverride.class);

                if (overrideAnnotation != null) {

                    parameterTypes = overrideAnnotation.value();
                    final Class<?>[] methodParameterTypes = method.getParameterTypes();

                    if (parameterTypes.length != methodParameterTypes.length) {

                        throw new IllegalArgumentException(
                                "the async parameters are not compatible");
                    }

                    isParallel = overrideAnnotation.parallel();

                    if (isParallel) {

                        if (methodParameterTypes.length != 1) {

                            throw new IllegalArgumentException(
                                    "the parallel parameter is not compatible");
                        }

                        final Class<?> parameterType = methodParameterTypes[0];

                        if (OutputChannel.class.equals(parameterType)) {

                            isOverrideParameters = true;

                        } else if (!parameterType.isArray() && !Iterable.class.isAssignableFrom(
                                parameterType)) {

                            throw new IllegalArgumentException(
                                    "the parallel parameter is not compatible");
                        }

                    } else if (parameterTypes.length > 0) {

                        isOverrideParameters = true;

                        final int length = parameterTypes.length;

                        for (int i = 0; i < length; i++) {

                            final Class<?> parameterType = methodParameterTypes[i];

                            if (!OutputChannel.class.equals(parameterType)
                                    && !parameterTypes[i].isAssignableFrom(parameterType)) {

                                throw new IllegalArgumentException(
                                        "the async parameters are not compatible");
                            }
                        }
                    }

                    isOverrideResult = overrideAnnotation.result();

                    if (isOverrideResult && !OutputChannel
                            .class.isAssignableFrom(returnType)) {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }
                }

                final Async annotation = method.getAnnotation(Async.class);

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    String name = null;

                    if (annotation != null) {

                        name = annotation.tag();
                    }

                    if ((name == null) || (name.length() == 0)) {

                        name = method.getName();
                    }

                    if ((parameterTypes == null) || (parameterTypes.length == 0)) {

                        parameterTypes = method.getParameterTypes();
                    }

                    targetMethod = getAnnotatedMethod(name);

                    if (targetMethod == null) {

                        try {

                            targetMethod = targetClass.getMethod(name, parameterTypes);

                        } catch (final NoSuchMethodException ignored) {

                        }

                        if (targetMethod == null) {

                            targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);
                        }
                    }

                    if (!isOverrideResult && !returnType.isAssignableFrom(
                            targetMethod.getReturnType())) {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }
                }

                if (annotation != null) {

                    if (lockId == null) {

                        lockId = annotation.lockId();
                    }

                    if (runner == null) {

                        final Class<? extends Runner> runnerClass = annotation.runner();

                        if (runnerClass != DefaultRunner.class) {

                            runner = runnerClass.newInstance();
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

                    if (maxInputBufferSize == Async.DEFAULT_NUMBER) {

                        maxInputBufferSize = annotation.maxInput();
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

                    if (maxOutputBufferSize == Async.DEFAULT_NUMBER) {

                        maxOutputBufferSize = annotation.maxOutput();
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

                            log = logClass.newInstance();
                        }
                    }

                    if (level == null) {

                        level = annotation.logLevel();
                    }
                }

                methodMap.put(method, targetMethod);
            }

            if (isOverrideParameters) {

                orderedInput = true;
            }

            final Routine<Object, Object> routine =
                    getRoutine(targetMethod, lockId, runner, isSequential, maxRunning, maxRetained,
                               availTimeout, maxInputBufferSize, inputTimeout, orderedInput,
                               maxOutputBufferSize, outputTimeout, orderedOutput, log, level);
            final OutputChannel<Object> outputChannel;

            if (isParallel) {

                final ParameterChannel<Object, Object> parameterChannel = routine.invokeParallel();
                final Object arg = args[0];

                if (isOverrideParameters) {

                    parameterChannel.pass((OutputChannel<?>) arg);

                } else if (arg == null) {

                    parameterChannel.pass((Object) null);

                } else if (arg.getClass().isArray()) {

                    final int length = Array.getLength(arg);

                    for (int i = 0; i < length; i++) {

                        parameterChannel.pass(Array.get(arg, i));
                    }

                } else {

                    final Iterable<?> iterable = (Iterable<?>) arg;

                    for (final Object input : iterable) {

                        parameterChannel.pass(input);
                    }
                }

                outputChannel = parameterChannel.results();

            } else if (isOverrideParameters) {

                final ParameterChannel<Object, Object> parameterChannel = routine.invokeAsync();

                for (final Object arg : args) {

                    if (arg instanceof OutputChannel) {

                        parameterChannel.pass((OutputChannel<?>) arg);

                    } else {

                        parameterChannel.pass(arg);
                    }
                }

                outputChannel = parameterChannel.results();

            } else {

                outputChannel = routine.runAsync(args);
            }

            if (!Void.class.equals(boxingClass(returnType))) {

                if (isOverrideResult) {

                    return outputChannel;
                }

                return outputChannel.readFirst();
            }

            return null;
        }
    }

    /**
     * Invocation handler wrapping the target object instance.
     */
    private class ObjectInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final OutputChannel<Object> outputChannel = method(method).runAsync(args);

            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                return outputChannel.readFirst();
            }

            return null;
        }
    }
}
