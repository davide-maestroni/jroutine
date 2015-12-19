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

import com.github.dm.jrt.annotation.Alias;
import com.github.dm.jrt.annotation.CoreInstances;
import com.github.dm.jrt.annotation.Input;
import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.InputMaxSize;
import com.github.dm.jrt.annotation.InputOrder;
import com.github.dm.jrt.annotation.InputTimeout;
import com.github.dm.jrt.annotation.Inputs;
import com.github.dm.jrt.annotation.Invoke;
import com.github.dm.jrt.annotation.Invoke.InvocationMode;
import com.github.dm.jrt.annotation.LogLevel;
import com.github.dm.jrt.annotation.MaxInstances;
import com.github.dm.jrt.annotation.Output;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.annotation.OutputMaxSize;
import com.github.dm.jrt.annotation.OutputOrder;
import com.github.dm.jrt.annotation.OutputTimeout;
import com.github.dm.jrt.annotation.Priority;
import com.github.dm.jrt.annotation.ReadTimeout;
import com.github.dm.jrt.annotation.ReadTimeoutAction;
import com.github.dm.jrt.annotation.SharedFields;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.Mutex;
import com.github.dm.jrt.util.Reflection;
import com.github.dm.jrt.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.dm.jrt.util.Reflection.asArgs;

/**
 * Utility class used to manage cached objects shared by routine builders.
 * <p/>
 * Created by davide-maestroni on 03/23/2015.
 */
public class RoutineBuilders {

    private static final WeakIdentityHashMap<Class<?>, Map<String, Method>> sAliasMethods =
            new WeakIdentityHashMap<Class<?>, Map<String, Method>>();

    private static final WeakIdentityHashMap<Object, Map<String, ReentrantLock>> sLocks =
            new WeakIdentityHashMap<Object, Map<String, ReentrantLock>>();

    private static final WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>> sMethods =
            new WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>>();

    private static final WeakIdentityHashMap<Object, ExchangeMutex> sMutexes =
            new WeakIdentityHashMap<Object, ExchangeMutex>();

    /**
     * Avoid direct instantiation.
     */
    protected RoutineBuilders() {

    }

    /**
     * Calls the specified target method from inside a routine invocation.
     *
     * @param mutex        the method mutex.
     * @param target       the target instance.
     * @param targetMethod the target method.
     * @param objects      the input objects.
     * @param result       the invocation result channel.
     * @param inputMode    the input transfer mode.
     * @param outputMode   the output transfer mode.
     * @throws com.github.dm.jrt.channel.RoutineException in case of errors.
     */
    public static void callFromInvocation(@NotNull final Mutex mutex, @NotNull final Object target,
            @NotNull final Method targetMethod, @NotNull final List<?> objects,
            @NotNull final ResultChannel<Object> result, @Nullable final InputMode inputMode,
            @Nullable final OutputMode outputMode) {

        Reflection.makeAccessible(targetMethod);

        try {

            final Object methodResult;
            mutex.acquire();

            try {

                final Object[] args;

                if (inputMode == InputMode.COLLECTION) {

                    final Class<?> paramClass = targetMethod.getParameterTypes()[0];

                    if (paramClass.isArray()) {

                        final int size = objects.size();
                        final Object array = Array.newInstance(paramClass.getComponentType(), size);

                        for (int i = 0; i < size; ++i) {

                            Array.set(array, i, objects.get(i));
                        }

                        args = asArgs(array);

                    } else {

                        args = asArgs(objects);
                    }

                } else {

                    args = objects.toArray(new Object[objects.size()]);
                }

                methodResult = targetMethod.invoke(target, args);

            } finally {

                mutex.release();
            }

            final Class<?> returnType = targetMethod.getReturnType();

            if (!Void.class.equals(Reflection.boxingClass(returnType))) {

                if (outputMode == OutputMode.ELEMENT) {

                    if (returnType.isArray()) {

                        if (methodResult != null) {

                            result.orderByCall();
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

        } catch (final RoutineException e) {

            throw e;

        } catch (final InvocationTargetException e) {

            throw new InvocationException(e.getCause());

        } catch (final Throwable t) {

            throw new InvocationException(t);
        }
    }

    /**
     * Returns a configuration properly modified by taking into account the annotations added to the
     * specified method.
     *
     * @param configuration the initial configuration.
     * @param method        the target method.
     * @return the modified configuration.
     * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
     * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
     * @see com.github.dm.jrt.annotation.InputOrder InputOrder
     * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
     * @see com.github.dm.jrt.annotation.LogLevel LogLevel
     * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
     * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
     * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
     * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
     * @see com.github.dm.jrt.annotation.Priority Priority
     * @see com.github.dm.jrt.annotation.ReadTimeout ReadTimeout
     * @see com.github.dm.jrt.annotation.ReadTimeoutAction ReadTimeoutAction
     */
    @NotNull
    public static InvocationConfiguration configurationWithAnnotations(
            @Nullable final InvocationConfiguration configuration, @NotNull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builderFrom(configuration);
        final CoreInstances coreInstancesAnnotation = method.getAnnotation(CoreInstances.class);

        if (coreInstancesAnnotation != null) {

            builder.withCoreInstances(coreInstancesAnnotation.value());
        }

        final InputMaxSize inputSizeAnnotation = method.getAnnotation(InputMaxSize.class);

        if (inputSizeAnnotation != null) {

            builder.withInputMaxSize(inputSizeAnnotation.value());
        }

        final InputOrder inputOrderAnnotation = method.getAnnotation(InputOrder.class);

        if (inputOrderAnnotation != null) {

            builder.withInputOrder(inputOrderAnnotation.value());
        }

        final InputTimeout inputTimeoutAnnotation = method.getAnnotation(InputTimeout.class);

        if (inputTimeoutAnnotation != null) {

            builder.withInputTimeout(inputTimeoutAnnotation.value(), inputTimeoutAnnotation.unit());
        }

        final LogLevel logLevelAnnotation = method.getAnnotation(LogLevel.class);

        if (logLevelAnnotation != null) {

            builder.withLogLevel(logLevelAnnotation.value());
        }

        final MaxInstances maxInstancesAnnotation = method.getAnnotation(MaxInstances.class);

        if (maxInstancesAnnotation != null) {

            builder.withMaxInstances(maxInstancesAnnotation.value());
        }

        final OutputMaxSize outputSizeAnnotation = method.getAnnotation(OutputMaxSize.class);

        if (outputSizeAnnotation != null) {

            builder.withOutputMaxSize(outputSizeAnnotation.value());
        }

        final OutputOrder outputOrderAnnotation = method.getAnnotation(OutputOrder.class);

        if (outputOrderAnnotation != null) {

            builder.withOutputOrder(outputOrderAnnotation.value());
        }

        final OutputTimeout outputTimeoutAnnotation = method.getAnnotation(OutputTimeout.class);

        if (outputTimeoutAnnotation != null) {

            builder.withOutputTimeout(outputTimeoutAnnotation.value(),
                                      outputTimeoutAnnotation.unit());
        }

        final Priority priorityAnnotation = method.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.withPriority(priorityAnnotation.value());
        }

        final ReadTimeout readTimeoutAnnotation = method.getAnnotation(ReadTimeout.class);

        if (readTimeoutAnnotation != null) {

            builder.withReadTimeout(readTimeoutAnnotation.value(), readTimeoutAnnotation.unit());
        }

        final ReadTimeoutAction actionAnnotation = method.getAnnotation(ReadTimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withReadTimeoutAction(actionAnnotation.value());
        }

        return builder.set();
    }

    /**
     * Returns a configuration properly modified by taking into account the annotations added to the
     * specified method.
     *
     * @param configuration the initial configuration.
     * @param method        the target method.
     * @return the modified configuration.
     * @see com.github.dm.jrt.annotation.SharedFields SharedFields
     */
    @NotNull
    public static ProxyConfiguration configurationWithAnnotations(
            @Nullable final ProxyConfiguration configuration, @NotNull final Method method) {

        final ProxyConfiguration.Builder<ProxyConfiguration> builder =
                ProxyConfiguration.builderFrom(configuration);
        final SharedFields sharedFieldsAnnotation = method.getAnnotation(SharedFields.class);

        if (sharedFieldsAnnotation != null) {

            builder.withSharedFields(sharedFieldsAnnotation.value());
        }

        return builder.set();
    }

    /**
     * Gets the method annotated with the specified alias name.
     *
     * @param targetClass the target class.
     * @param name        the alias name.
     * @return the method.
     * @throws java.lang.IllegalArgumentException if no method with the specified alias name was
     *                                            found.
     */
    @Nullable
    public static Method getAnnotatedMethod(@NotNull final Class<?> targetClass,
            @NotNull final String name) {

        synchronized (sAliasMethods) {

            final WeakIdentityHashMap<Class<?>, Map<String, Method>> aliasMethods = sAliasMethods;
            Map<String, Method> methodMap = aliasMethods.get(targetClass);

            if (methodMap == null) {

                methodMap = new HashMap<String, Method>();
                fillMap(methodMap, targetClass.getMethods());
                final HashMap<String, Method> declaredMethodMap = new HashMap<String, Method>();
                fillMap(declaredMethodMap, targetClass.getDeclaredMethods());

                for (final Entry<String, Method> methodEntry : declaredMethodMap.entrySet()) {

                    final String methodName = methodEntry.getKey();

                    if (!methodMap.containsKey(methodName)) {

                        methodMap.put(methodName, methodEntry.getValue());
                    }
                }

                aliasMethods.put(targetClass, methodMap);
            }

            return methodMap.get(name);
        }
    }

    /**
     * Gets the input transfer mode associated to the specified method parameter, while also
     * validating the use of the {@link com.github.dm.jrt.annotation.Input Input} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method the proxy method.
     * @param index  the index of the parameter.
     * @return the input mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.github.dm.jrt.annotation.Input Input
     */
    @Nullable
    public static InputMode getInputMode(@NotNull final Method method, final int index) {

        Input inputAnnotation = null;
        final Annotation[][] annotations = method.getParameterAnnotations();

        for (final Annotation annotation : annotations[index]) {

            if (annotation.annotationType() == Input.class) {

                inputAnnotation = (Input) annotation;
                break;
            }
        }

        if (inputAnnotation == null) {

            return null;
        }

        InputMode inputMode = inputAnnotation.mode();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?> parameterType = parameterTypes[index];

        if (inputMode == InputMode.CHANNEL) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.CHANNEL
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

        } else { // InputMode.COLLECTION

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

            final Class<?> paramClass = inputAnnotation.value();

            if (!paramClass.isArray() && !paramClass.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            final int length = parameterTypes.length;

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION +
                                " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the routine invocation mode associated to the specified method, while also validating
     * the use of the {@link com.github.dm.jrt.annotation.Invoke Invoke} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method the proxy method.
     * @return the input mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.github.dm.jrt.annotation.Invoke Invoke
     */
    @Nullable
    public static InvocationMode getInvocationMode(@NotNull final Method method) {

        final Invoke methodAnnotation = method.getAnnotation(Invoke.class);

        if (methodAnnotation == null) {

            return null;
        }

        final InvocationMode invocationMode = methodAnnotation.value();

        if ((invocationMode == InvocationMode.PARALLEL) && (method.getParameterTypes().length
                > 1)) {

            throw new IllegalArgumentException(
                    "methods annotated with invocation mode " + InvocationMode.PARALLEL
                            + " must have at maximum one input parameter: " + method);
        }

        return invocationMode;
    }

    /**
     * Gets the output transfer mode of the return type of the specified method, while also
     * validating the use of the {@link com.github.dm.jrt.annotation.Output Output} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method           the proxy method.
     * @param targetReturnType the target return type.
     * @return the output mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.github.dm.jrt.annotation.Output Output
     */
    @Nullable
    public static OutputMode getOutputMode(@NotNull final Method method,
            @NotNull final Class<?> targetReturnType) {

        final Output outputAnnotation = method.getAnnotation(Output.class);

        if (outputAnnotation == null) {

            return null;
        }

        final Class<?> returnType = method.getReturnType();
        OutputMode outputMode = outputAnnotation.value();

        if (outputMode == OutputMode.CHANNEL) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.CHANNEL
                                + " must be a superclass of " + channelClassName);
            }

        } else if (outputMode == OutputMode.ELEMENT) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.CHANNEL
                                + " must be a superclass of " + channelClassName);
            }

            if (!targetReturnType.isArray() && !Iterable.class.isAssignableFrom(targetReturnType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.ELEMENT
                                + " must be bound to an array or a type implementing an "
                                + Iterable.class.getCanonicalName());
            }

        } else { // OutputMode.COLLECTION

            if (!returnType.isArray() && !returnType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.COLLECTION
                                + " must be an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (returnType.isArray() && !Reflection.boxingClass(returnType.getComponentType())
                                                   .isAssignableFrom(Reflection.boxingClass(
                                                           targetReturnType))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async output array with mode " + OutputMode.COLLECTION
                                + " does not match the bound type: "
                                + targetReturnType.getCanonicalName());
            }
        }

        return outputMode;
    }

    /**
     * Returns the cached mutex associated with the specified target and shared fields.<br/>
     * If the cache was empty, it is filled with a new object automatically created.<br/>
     * If the target is null {@link Mutex#NO_MUTEX} will be returned.
     *
     * @param target       the target object instance.
     * @param sharedFields the shared field names.
     * @return the cached mutex.
     */
    @NotNull
    public static Mutex getSharedMutex(@Nullable final Object target,
            @Nullable final List<String> sharedFields) {

        if ((target == null) || ((sharedFields != null) && sharedFields.isEmpty())) {

            return Mutex.NO_MUTEX;
        }

        ExchangeMutex exchangeMutex;

        synchronized (sMutexes) {

            final WeakIdentityHashMap<Object, ExchangeMutex> mutexes = sMutexes;
            exchangeMutex = mutexes.get(target);

            if (exchangeMutex == null) {

                exchangeMutex = new ExchangeMutex();
                mutexes.put(target, exchangeMutex);
            }
        }

        if (sharedFields == null) {

            return exchangeMutex;
        }

        synchronized (sLocks) {

            final WeakIdentityHashMap<Object, Map<String, ReentrantLock>> locksCache = sLocks;
            Map<String, ReentrantLock> lockMap = locksCache.get(target);

            if (lockMap == null) {

                lockMap = new HashMap<String, ReentrantLock>();
                locksCache.put(target, lockMap);
            }

            final TreeSet<String> nameSet = new TreeSet<String>(sharedFields);
            final int size = nameSet.size();
            final ReentrantLock[] locks = new ReentrantLock[size];
            int i = 0;

            for (final String name : nameSet) {

                ReentrantLock lock = lockMap.get(name);

                if (lock == null) {

                    lock = new ReentrantLock();
                    lockMap.put(name, lock);
                }

                locks[i++] = lock;
            }

            return new BuilderMutex(exchangeMutex, locks);
        }
    }

    /**
     * Gets info about the method targeted by the specified proxy one.
     *
     * @param targetClass the target class.
     * @param proxyMethod the proxy method.
     * @return the method info.
     * @throws java.lang.IllegalArgumentException if no target method was found.
     */
    @NotNull
    public static MethodInfo getTargetMethodInfo(@NotNull final Class<?> targetClass,
            @NotNull final Method proxyMethod) {

        MethodInfo methodInfo;

        synchronized (sMethods) {

            final WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>> methodCache = sMethods;
            Map<Method, MethodInfo> methodMap = methodCache.get(targetClass);

            if (methodMap == null) {

                methodMap = new HashMap<Method, MethodInfo>();
                methodCache.put(targetClass, methodMap);
            }

            methodInfo = methodMap.get(proxyMethod);

            if (methodInfo == null) {

                final InvocationMode invocationMode = getInvocationMode(proxyMethod);
                InputMode inputMode = null;
                OutputMode outputMode = null;
                final Class<?>[] targetParameterTypes;
                final Inputs inputsAnnotation = proxyMethod.getAnnotation(Inputs.class);

                if (inputsAnnotation != null) {

                    if (proxyMethod.getParameterTypes().length > 0) {

                        throw new IllegalArgumentException(
                                "methods annotated with " + Inputs.class.getSimpleName()
                                        + " must have no input parameters: " + proxyMethod);
                    }

                    final Class<?> returnType = proxyMethod.getReturnType();

                    if (!returnType.isAssignableFrom(InvocationChannel.class)
                            && !returnType.isAssignableFrom(Routine.class)) {

                        throw new IllegalArgumentException(
                                "the proxy method has incompatible return type: " + proxyMethod);
                    }

                    targetParameterTypes = inputsAnnotation.value();
                    inputMode = InputMode.CHANNEL;
                    outputMode = OutputMode.CHANNEL;

                } else {

                    targetParameterTypes = proxyMethod.getParameterTypes();
                    final Annotation[][] annotations = proxyMethod.getParameterAnnotations();
                    final int length = annotations.length;

                    for (int i = 0; i < length; ++i) {

                        final InputMode paramMode = getInputMode(proxyMethod, i);

                        if (paramMode != null) {

                            inputMode = paramMode;

                            for (final Annotation paramAnnotation : annotations[i]) {

                                if (paramAnnotation.annotationType() == Input.class) {

                                    targetParameterTypes[i] = ((Input) paramAnnotation).value();
                                    break;
                                }
                            }
                        }
                    }
                }

                if ((invocationMode == InvocationMode.PARALLEL) && (targetParameterTypes.length
                        > 1)) {

                    throw new IllegalArgumentException(
                            "methods annotated with invocation mode " + InvocationMode.PARALLEL
                                    + " must have no input parameters: " + proxyMethod);
                }

                final Method targetMethod =
                        getTargetMethod(proxyMethod, targetClass, targetParameterTypes);
                final Class<?> returnType = proxyMethod.getReturnType();
                final Class<?> targetReturnType = targetMethod.getReturnType();
                final Output outputAnnotation = proxyMethod.getAnnotation(Output.class);
                boolean isError = false;

                if (outputAnnotation != null) {

                    outputMode = getOutputMode(proxyMethod, targetReturnType);

                    if ((outputMode == OutputMode.COLLECTION) && returnType.isArray()) {

                        isError = !Reflection.boxingClass(returnType.getComponentType())
                                             .isAssignableFrom(
                                                     Reflection.boxingClass(targetReturnType));
                    }

                } else if (inputsAnnotation == null) {

                    isError = !returnType.isAssignableFrom(targetReturnType);
                }

                if (isError) {

                    throw new IllegalArgumentException(
                            "the proxy method has incompatible return type: " + proxyMethod);
                }

                methodInfo = new MethodInfo(targetMethod, invocationMode, inputMode, outputMode);
                methodMap.put(proxyMethod, methodInfo);
            }
        }

        return methodInfo;
    }

    /**
     * Invokes the routine wrapping the specified method.
     *
     * @param routine        the routine to be called.
     * @param method         the target method.
     * @param args           the method arguments.
     * @param invocationMode the routine invocation mode.
     * @param inputMode      the input transfer mode.
     * @param outputMode     the output transfer mode.
     * @return the invocation output.
     * @throws com.github.dm.jrt.channel.RoutineException in case of errors.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object invokeRoutine(@NotNull final Routine<Object, Object> routine,
            @NotNull final Method method, @NotNull final Object[] args,
            @Nullable final InvocationMode invocationMode, @Nullable final InputMode inputMode,
            @Nullable final OutputMode outputMode) {

        final Class<?> returnType = method.getReturnType();

        if (method.isAnnotationPresent(Inputs.class)) {

            if (returnType.isAssignableFrom(InvocationChannel.class)) {

                return (invocationMode == InvocationMode.SYNC) ? routine.syncInvoke()
                        : (invocationMode == InvocationMode.PARALLEL) ? routine.parallelInvoke()
                                : routine.asyncInvoke();
            }

            return routine;
        }

        final OutputChannel<Object> outputChannel;
        final InvocationChannel<Object, Object> invocationChannel =
                (invocationMode == InvocationMode.SYNC) ? routine.syncInvoke()
                        : (invocationMode == InvocationMode.PARALLEL) ? routine.parallelInvoke()
                                : routine.asyncInvoke();

        // TODO: 12/14/15 remove InputMode.ELEMENT and OutputMode.COLLECTION
        if (inputMode == InputMode.CHANNEL) {

            invocationChannel.orderByCall();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final int length = args.length;

            for (int i = 0; i < length; ++i) {

                final Object arg = args[i];

                if (OutputChannel.class.isAssignableFrom(parameterTypes[i])) {

                    invocationChannel.pass((OutputChannel<Object>) arg);

                } else {

                    invocationChannel.pass(arg);
                }
            }

            outputChannel = invocationChannel.result();

        } else if (inputMode == InputMode.COLLECTION) {

            outputChannel =
                    invocationChannel.orderByCall().pass((OutputChannel<Object>) args[0]).result();

        } else {

            outputChannel = invocationChannel.pass(args).result();
        }

        if (!Void.class.equals(Reflection.boxingClass(returnType))) {

            if (outputMode != null) {

                if (OutputChannel.class.isAssignableFrom(returnType)) {

                    return outputChannel;
                }

                if (returnType.isAssignableFrom(List.class)) {

                    return outputChannel.all();
                }

                if (returnType.isArray()) {

                    final List<Object> results = outputChannel.all();
                    final int size = results.size();
                    final Object array = Array.newInstance(returnType.getComponentType(), size);

                    for (int i = 0; i < size; ++i) {

                        Array.set(array, i, results.get(i));
                    }

                    return array;
                }
            }

            return outputChannel.all().iterator().next();
        }

        outputChannel.checkComplete();
        return null;
    }

    private static void fillMap(@NotNull final Map<String, Method> map,
            @NotNull final Method[] methods) {

        for (final Method method : methods) {

            final Alias annotation = method.getAnnotation(Alias.class);

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

    @NotNull
    private static Method getTargetMethod(@NotNull final Method method,
            @NotNull final Class<?> targetClass, @NotNull final Class<?>[] targetParameterTypes) {

        String name = null;
        Method targetMethod = null;
        final Alias annotation = method.getAnnotation(Alias.class);

        if (annotation != null) {

            name = annotation.value();
            targetMethod = getAnnotatedMethod(targetClass, name);
        }

        if (targetMethod == null) {

            if (name == null) {

                name = method.getName();
            }

            targetMethod = Reflection.findMethod(targetClass, name, targetParameterTypes);

        } else {

            // Validate method parameters
            Reflection.findMethod(targetClass, targetMethod.getName(), targetParameterTypes);
        }

        return targetMethod;
    }

    /**
     * Data class storing information about the target method.
     */
    public static class MethodInfo {

        /**
         * The input transfer mode.
         */
        public final InputMode inputMode;

        /**
         * The routine invocation mode.
         */
        public final InvocationMode invocationMode;

        /**
         * The target method.
         */
        public final Method method;

        /**
         * The output transfer mode.
         */
        public final OutputMode outputMode;

        /**
         * Constructor.
         *
         * @param method         the target method.
         * @param invocationMode the invocation mode.
         * @param inputMode      the input mode.
         * @param outputMode     the output mode.
         */
        private MethodInfo(@NotNull final Method method,
                @Nullable final InvocationMode invocationMode, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            this.method = method;
            this.invocationMode = invocationMode;
            this.inputMode = inputMode;
            this.outputMode = outputMode;
        }
    }

    /**
     * Mutex implementation.
     */
    private static class BuilderMutex implements Mutex {

        private final ReentrantLock[] mLocks;

        private final ExchangeMutex mMutex;

        /**
         * Constructor.
         *
         * @param mutex the exchange mutex.
         * @param locks the locks.
         */
        private BuilderMutex(@NotNull final ExchangeMutex mutex,
                @NotNull final ReentrantLock[] locks) {

            mMutex = mutex;
            mLocks = locks;
        }

        public void acquire() {

            mMutex.acquirePartialMutex();

            for (final ReentrantLock lock : mLocks) {

                lock.lock();
            }
        }

        public void release() {

            final ReentrantLock[] locks = mLocks;
            final int length = locks.length;

            for (int i = length - 1; i >= 0; --i) {

                locks[i].unlock();
            }

            mMutex.releasePartialMutex();
        }
    }

    /**
     * Class used to synchronize between partial and full mutexes.
     */
    private static class ExchangeMutex implements Mutex {

        private final ReentrantLock mLock = new ReentrantLock();

        private final Object mMutex = new Object();

        private int mFullMutexCount;

        private int mPartialMutexCount;

        /**
         * Acquires a partial mutex making sure that no full one is already taken.
         */
        public void acquirePartialMutex() {

            try {

                synchronized (mMutex) {

                    while (mFullMutexCount > 0) {

                        mMutex.wait();
                    }

                    ++mPartialMutexCount;
                }

            } catch (final InterruptedException e) {

                InvocationInterruptedException.throwIfInterrupt(e);
            }
        }

        /**
         * Releases a partial mutex.
         */
        public void releasePartialMutex() {

            synchronized (mMutex) {

                --mPartialMutexCount;
                mMutex.notifyAll();
            }
        }

        public void acquire() {

            try {

                synchronized (mMutex) {

                    while (mPartialMutexCount > 0) {

                        mMutex.wait();
                    }

                    ++mFullMutexCount;
                }

                mLock.lock();

            } catch (final InterruptedException e) {

                InvocationInterruptedException.throwIfInterrupt(e);
            }
        }

        public void release() {

            mLock.unlock();

            synchronized (mMutex) {

                --mFullMutexCount;
                mMutex.notifyAll();
            }
        }
    }
}
