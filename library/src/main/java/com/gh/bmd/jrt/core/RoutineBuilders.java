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

import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Inputs;
import com.gh.bmd.jrt.annotation.Output;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.gh.bmd.jrt.util.Reflection.boxingClass;
import static com.gh.bmd.jrt.util.Reflection.findMethod;
import static com.gh.bmd.jrt.util.Reflection.makeAccessible;

/**
 * Utility class used to manage cached objects shared by routine builders.
 * <p/>
 * Created by davide-maestroni on 3/23/15.
 */
public class RoutineBuilders {

    private static final WeakIdentityHashMap<Class<?>, Map<String, Method>> sAliasCache =
            new WeakIdentityHashMap<Class<?>, Map<String, Method>>();

    private static final WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>> sMethodCache =
            new WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>>();

    private static final WeakIdentityHashMap<Object, Map<String, Object>> sMutexCache =
            new WeakIdentityHashMap<Object, Map<String, Object>>();

    /**
     * Avoid direct instantiation.
     */
    protected RoutineBuilders() {

    }

    /**
     * Calls the specified target method from inside a routine invocation.
     *
     * @param targetMethod the target method.
     * @param mutex        the method mutex.
     * @param target       the target instance.
     * @param objects      the input objects.
     * @param result       the invocation result channel.
     * @param inputMode    the input transfer mode.
     * @param outputMode   the output transfer mode.
     * @throws com.gh.bmd.jrt.channel.RoutineException in case of errors.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void callFromInvocation(@Nonnull final Method targetMethod,
            @Nonnull final Object mutex, @Nonnull final Object target,
            @Nonnull final List<?> objects, @Nonnull final ResultChannel<Object> result,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        makeAccessible(targetMethod);

        try {

            final Object methodResult;

            synchronized (mutex) {

                final Object[] args;

                if (inputMode == InputMode.COLLECTION) {

                    final Class<?> paramClass = targetMethod.getParameterTypes()[0];

                    if (paramClass.isArray()) {

                        final int size = objects.size();
                        final Object array = Array.newInstance(paramClass.getComponentType(), size);

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

                methodResult = targetMethod.invoke(target, args);
            }

            final Class<?> returnType = targetMethod.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

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
     * @see com.gh.bmd.jrt.annotation.Priority Priority
     * @see com.gh.bmd.jrt.annotation.Timeout Timeout
     * @see com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction
     */
    @Nonnull
    public static InvocationConfiguration configurationWithAnnotations(
            @Nullable final InvocationConfiguration configuration, @Nonnull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builderFrom(configuration);
        final Priority priorityAnnotation = method.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.withPriority(priorityAnnotation.value());
        }

        final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withExecutionTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withExecutionTimeoutAction(actionAnnotation.value());
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
     * @see com.gh.bmd.jrt.annotation.ShareGroup ShareGroup
     */
    @Nonnull
    public static ProxyConfiguration configurationWithAnnotations(
            @Nullable final ProxyConfiguration configuration, @Nonnull final Method method) {

        final ProxyConfiguration.Builder<ProxyConfiguration> builder =
                ProxyConfiguration.builderFrom(configuration);
        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            builder.withShareGroup(shareGroupAnnotation.value());
        }

        return builder.set();
    }

    /**
     * Gets the method annotated with the specified alias name.
     *
     * @param name        the alias name.
     * @param targetClass the target class.
     * @return the method.
     * @throws java.lang.IllegalArgumentException if no method with the specified alias name was
     *                                            found.
     */
    @Nullable
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static Method getAnnotatedMethod(@Nonnull final String name,
            @Nonnull final Class<?> targetClass) {

        final WeakIdentityHashMap<Class<?>, Map<String, Method>> aliasCache = sAliasCache;

        synchronized (aliasCache) {

            Map<String, Method> methodMap = aliasCache.get(targetClass);

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

                aliasCache.put(targetClass, methodMap);
            }

            return methodMap.get(name);
        }
    }

    /**
     * Gets the input transfer mode associated to the specified method parameter, while also
     * validating the use of the {@link com.gh.bmd.jrt.annotation.Input Input} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method the proxy method.
     * @param index  the index of the parameter.
     * @return the input mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.gh.bmd.jrt.annotation.Input Input
     */
    @Nullable
    public static InputMode getInputMode(@Nonnull final Method method, final int index) {

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

        if (inputMode == InputMode.VALUE) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.VALUE
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

        } else if (inputMode == InputMode.COLLECTION) {

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

        } else { // InputMode.PARALLEL

            final boolean isArray = parameterType.isArray();

            if (!isArray && !Iterable.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.PARALLEL
                                + " must be an array or implement an "
                                + Iterable.class.getCanonicalName());
            }

            final Class<?> paramClass = inputAnnotation.value();

            if (isArray && !boxingClass(paramClass).isAssignableFrom(
                    boxingClass(parameterType.getComponentType()))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async input array with mode " + InputMode.PARALLEL
                                + " does not match the bound type: "
                                + paramClass.getCanonicalName());
            }

            final int length = parameterTypes.length;

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.PARALLEL
                                + " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the inputs transfer mode associated to the specified method, while also
     * validating the use of the {@link com.gh.bmd.jrt.annotation.Inputs Inputs} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method the proxy method.
     * @return the input mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.gh.bmd.jrt.annotation.Inputs Inputs
     */
    @Nullable
    public static InputMode getInputsMode(@Nonnull final Method method) {

        final Inputs methodAnnotation = method.getAnnotation(Inputs.class);

        if (methodAnnotation == null) {

            return null;
        }

        if (method.getParameterTypes().length > 0) {

            throw new IllegalArgumentException(
                    "methods annotated with " + Inputs.class.getSimpleName()
                            + " must have no input parameters: " + method);
        }

        final Class<?> returnType = method.getReturnType();

        if (!returnType.isAssignableFrom(InvocationChannel.class) && !returnType.isAssignableFrom(
                Routine.class)) {

            throw new IllegalArgumentException(
                    "the proxy method has incompatible return type: " + method);
        }

        final Class<?>[] parameterTypes = methodAnnotation.value();
        InputMode inputMode = methodAnnotation.mode();

        if (inputMode == InputMode.COLLECTION) {

            final Class<?> parameterType = parameterTypes[0];

            if (!parameterType.isArray() && !parameterType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (parameterTypes.length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION +
                                " cannot be applied to a method taking " + parameterTypes.length
                                + " input parameters");
            }

        } else if (inputMode == InputMode.PARALLEL) {

            if (parameterTypes.length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.PARALLEL +
                                " cannot be applied to a method taking " + parameterTypes.length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the output transfer mode of the return type of the specified method, while also
     * validating the use of the {@link com.gh.bmd.jrt.annotation.Output Output} annotation.<br/>
     * In case no annotation is present, the function will return with null.
     *
     * @param method           the proxy method.
     * @param targetReturnType the target return type.
     * @return the output mode.
     * @throws java.lang.IllegalArgumentException if the method has been incorrectly annotated.
     * @see com.gh.bmd.jrt.annotation.Output Output
     */
    @Nullable
    public static OutputMode getOutputMode(@Nonnull final Method method,
            @Nonnull final Class<?> targetReturnType) {

        final Output outputAnnotation = method.getAnnotation(Output.class);

        if (outputAnnotation == null) {

            return null;
        }

        final Class<?> returnType = method.getReturnType();
        OutputMode outputMode = outputAnnotation.value();

        if (outputMode == OutputMode.VALUE) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.VALUE
                                + " must be a superclass of " + channelClassName);
            }

        } else if (outputMode == OutputMode.ELEMENT) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with mode " + OutputMode.VALUE
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

            if (returnType.isArray() && !boxingClass(
                    returnType.getComponentType()).isAssignableFrom(
                    boxingClass(targetReturnType))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async output array with mode " + OutputMode.COLLECTION
                                + " does not match the bound type: "
                                + targetReturnType.getCanonicalName());
            }
        }

        return outputMode;
    }

    /**
     * Returns the cached mutex associated with the specified target and share group.<br/>
     * If the cache was empty, it is filled with a new object automatically created.
     *
     * @param target     the target object instance.
     * @param shareGroup the share group name.
     * @return the cached mutex.
     */
    @Nonnull
    public static Object getSharedMutex(@Nonnull final Object target,
            @Nullable final String shareGroup) {

        synchronized (sMutexCache) {

            final WeakIdentityHashMap<Object, Map<String, Object>> mutexCache = sMutexCache;
            Map<String, Object> mutexMap = mutexCache.get(target);

            if (mutexMap == null) {

                mutexMap = new HashMap<String, Object>();
                mutexCache.put(target, mutexMap);
            }

            final String groupName = (shareGroup != null) ? shareGroup : ShareGroup.ALL;
            Object mutex = mutexMap.get(groupName);

            if (mutex == null) {

                mutex = new Object();
                mutexMap.put(groupName, mutex);
            }

            return mutex;
        }
    }

    /**
     * Gets info about the method targeted by the specified proxy one.
     *
     * @param proxyMethod the proxy method.
     * @param targetClass the target class.
     * @return the method info.
     * @throws java.lang.IllegalArgumentException if no target method was found.
     */
    @Nonnull
    public static MethodInfo getTargetMethodInfo(@Nonnull final Method proxyMethod,
            @Nonnull final Class<?> targetClass) {

        MethodInfo methodInfo;

        synchronized (sMethodCache) {

            final WeakIdentityHashMap<Class<?>, Map<Method, MethodInfo>> methodCache = sMethodCache;
            Map<Method, MethodInfo> methodMap = methodCache.get(targetClass);

            if (methodMap == null) {

                methodMap = new HashMap<Method, MethodInfo>();
                methodCache.put(targetClass, methodMap);
            }

            methodInfo = methodMap.get(proxyMethod);

            if (methodInfo == null) {

                InputMode inputMode = null;
                OutputMode outputMode = null;
                final Class<?>[] targetParameterTypes;
                final Inputs inputsAnnotation = proxyMethod.getAnnotation(Inputs.class);

                if (inputsAnnotation != null) {

                    targetParameterTypes = inputsAnnotation.value();
                    inputMode = getInputsMode(proxyMethod);
                    outputMode = OutputMode.VALUE;

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

                final Method targetMethod =
                        getTargetMethod(proxyMethod, targetClass, targetParameterTypes);
                final Class<?> returnType = proxyMethod.getReturnType();
                final Class<?> targetReturnType = targetMethod.getReturnType();
                final Output outputAnnotation = proxyMethod.getAnnotation(Output.class);
                boolean isError = false;

                if (outputAnnotation != null) {

                    outputMode = getOutputMode(proxyMethod, targetReturnType);

                    if ((outputMode == OutputMode.COLLECTION) && returnType.isArray()) {

                        isError = !boxingClass(returnType.getComponentType()).isAssignableFrom(
                                boxingClass(targetReturnType));
                    }

                } else if (inputsAnnotation == null) {

                    isError = !returnType.isAssignableFrom(targetReturnType);
                }

                if (isError) {

                    throw new IllegalArgumentException(
                            "the proxy method has incompatible return type: " + proxyMethod);
                }

                methodInfo = new MethodInfo(targetMethod, inputMode, outputMode);
                methodMap.put(proxyMethod, methodInfo);
            }
        }

        return methodInfo;
    }

    /**
     * Invokes the routine wrapping the specified method.
     *
     * @param routine    the routine to be called.
     * @param method     the target method.
     * @param args       the method arguments.
     * @param inputMode  the input transfer mode.
     * @param outputMode the output transfer mode.
     * @return the invocation output.
     * @throws com.gh.bmd.jrt.channel.RoutineException in case of errors.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object invokeRoutine(@Nonnull final Routine<Object, Object> routine,
            @Nonnull final Method method, @Nonnull final Object[] args,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        if (method.isAnnotationPresent(Inputs.class)) {

            if (method.getReturnType().isAssignableFrom(Routine.class)) {

                return routine;
            }

            return (inputMode == InputMode.PARALLEL) ? routine.parallelInvoke()
                    : routine.asyncInvoke();
        }

        final Class<?> returnType = method.getReturnType();
        final OutputChannel<Object> outputChannel;

        if (inputMode == InputMode.PARALLEL) {

            final InvocationChannel<Object, Object> invocationChannel = routine.parallelInvoke();
            final Class<?> parameterType = method.getParameterTypes()[0];
            final Object arg = args[0];

            if (arg == null) {

                invocationChannel.pass((Iterable<Object>) null);

            } else if (OutputChannel.class.isAssignableFrom(parameterType)) {

                invocationChannel.pass((OutputChannel<Object>) arg);

            } else if (parameterType.isArray()) {

                final int length = Array.getLength(arg);

                for (int i = 0; i < length; i++) {

                    invocationChannel.pass(Array.get(arg, i));
                }

            } else {

                final Iterable<?> iterable = (Iterable<?>) arg;

                for (final Object input : iterable) {

                    invocationChannel.pass(input);
                }
            }

            outputChannel = invocationChannel.result();

        } else if (inputMode == InputMode.VALUE) {

            final InvocationChannel<Object, Object> invocationChannel =
                    routine.asyncInvoke().orderByCall();
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

            outputChannel = routine.asyncInvoke()
                                   .orderByCall()
                                   .pass((OutputChannel<Object>) args[0])
                                   .result();

        } else {

            outputChannel = routine.asyncCall(args);
        }

        if (!Void.class.equals(boxingClass(returnType))) {

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

    private static void fillMap(@Nonnull final Map<String, Method> map,
            @Nonnull final Method[] methods) {

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

    @Nonnull
    private static Method getTargetMethod(@Nonnull final Method method,
            @Nonnull final Class<?> targetClass, @Nonnull final Class<?>[] targetParameterTypes) {

        String name = null;
        Method targetMethod = null;
        final Alias annotation = method.getAnnotation(Alias.class);

        if (annotation != null) {

            name = annotation.value();
            targetMethod = getAnnotatedMethod(name, targetClass);
        }

        if (targetMethod == null) {

            if (name == null) {

                name = method.getName();
            }

            targetMethod = findMethod(targetClass, name, targetParameterTypes);
        }

        return targetMethod;
    }

    /**
     * Data class storing information about the target method.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "this is an immutable data class")
    public static class MethodInfo {

        /**
         * The input transfer mode.
         */
        public final InputMode inputMode;

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
         * @param method     the target method.
         * @param inputMode  the input mode.
         * @param outputMode the output mode.
         */
        private MethodInfo(@Nonnull final Method method, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            this.method = method;
            this.inputMode = inputMode;
            this.outputMode = outputMode;
        }
    }
}
