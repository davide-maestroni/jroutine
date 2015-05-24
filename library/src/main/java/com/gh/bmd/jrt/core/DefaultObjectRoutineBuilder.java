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
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.RoutineChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.getInputMode;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getOutputMode;
import static com.gh.bmd.jrt.common.Reflection.NO_ARGS;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Created by davide-maestroni on 9/21/14.
 */
class DefaultObjectRoutineBuilder extends DefaultClassRoutineBuilder
        implements ObjectRoutineBuilder {

    private static final WeakIdentityHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new WeakIdentityHashMap<Object, HashMap<Method, Method>>();

    private final ProxyConfiguration.Configurable<ObjectRoutineBuilder> mProxyConfigurable =
            new ProxyConfiguration.Configurable<ObjectRoutineBuilder>() {

                @Nonnull
                public ObjectRoutineBuilder setConfiguration(
                        @Nonnull final ProxyConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    private final RoutineConfiguration.Configurable<ObjectRoutineBuilder> mRoutineConfigurable =
            new RoutineConfiguration.Configurable<ObjectRoutineBuilder>() {

                @Nonnull
                public ObjectRoutineBuilder setConfiguration(
                        @Nonnull final RoutineConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    /**
     * Constructor.
     *
     * @param target the target object instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     */
    DefaultObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Object callRoutine(@Nonnull final Routine<Object, Object> routine,
            @Nonnull final Method method, @Nonnull final Object[] args,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        final Class<?> returnType = method.getReturnType();
        final OutputChannel<Object> outputChannel;

        if (inputMode == InputMode.ELEMENT) {

            final RoutineChannel<Object, Object> routineChannel = routine.invokeParallel();
            final Class<?> parameterType = method.getParameterTypes()[0];
            final Object arg = args[0];

            if (arg == null) {

                routineChannel.pass((Iterable<Object>) null);

            } else if (OutputChannel.class.isAssignableFrom(parameterType)) {

                routineChannel.pass((OutputChannel<Object>) arg);

            } else if (parameterType.isArray()) {

                final int length = Array.getLength(arg);

                for (int i = 0; i < length; i++) {

                    routineChannel.pass(Array.get(arg, i));
                }

            } else {

                final Iterable<?> iterable = (Iterable<?>) arg;

                for (final Object input : iterable) {

                    routineChannel.pass(input);
                }
            }

            outputChannel = routineChannel.result();

        } else if (inputMode == InputMode.VALUE) {

            final RoutineChannel<Object, Object> routineChannel = routine.invokeAsync();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final int length = args.length;

            for (int i = 0; i < length; ++i) {

                final Object arg = args[i];

                if (OutputChannel.class.isAssignableFrom(parameterTypes[i])) {

                    routineChannel.pass((OutputChannel<Object>) arg);

                } else {

                    routineChannel.pass(arg);
                }
            }

            outputChannel = routineChannel.result();

        } else if (inputMode == InputMode.COLLECTION) {

            outputChannel = routine.invokeAsync().pass((OutputChannel<Object>) args[0]).result();

        } else {

            outputChannel = routine.callAsync(args);
        }

        if (!Void.class.equals(boxingClass(returnType))) {

            if (outputMode != null) {

                if (OutputChannel.class.isAssignableFrom(returnType)) {

                    return outputChannel;
                }

                if (returnType.isAssignableFrom(List.class)) {

                    return outputChannel.readAll();
                }

                if (returnType.isArray()) {

                    final List<Object> results = outputChannel.readAll();
                    final int size = results.size();
                    final Object array = Array.newInstance(returnType.getComponentType(), size);

                    for (int i = 0; i < size; ++i) {

                        Array.set(array, i, results.get(i));
                    }

                    return array;
                }
            }

            return outputChannel.readAll().iterator().next();
        }

        return null;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final InvocationHandler handler;

        if (itf.isAssignableFrom(getTargetClass())) {

            handler = new InterfaceInvocationHandler();

        } else {

            handler = new ProxyInvocationHandler();
        }

        final Object proxy =
                Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf}, handler);
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public RoutineConfiguration.Builder<? extends ObjectRoutineBuilder> withRoutine() {

        return new RoutineConfiguration.Builder<ObjectRoutineBuilder>(mRoutineConfigurable,
                                                                      getRoutineConfiguration());
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> withProxy() {

        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(mProxyConfigurable,
                                                                    getProxyConfiguration());
    }

    @Nonnull
    private Method getTargetMethod(@Nonnull final Method method,
            @Nonnull final Class<?>[] targetParameterTypes) throws NoSuchMethodException {

        String name = null;
        Method targetMethod = null;
        final Class<?> targetClass = getTargetClass();
        final Alias annotation = method.getAnnotation(Alias.class);

        if (annotation != null) {

            name = annotation.value();
            targetMethod = getAnnotatedMethod(name);
        }

        if (targetMethod == null) {

            if (name == null) {

                name = method.getName();
            }

            try {

                targetMethod = targetClass.getMethod(name, targetParameterTypes);

            } catch (final NoSuchMethodException ignored) {

            }

            if (targetMethod == null) {

                targetMethod = targetClass.getDeclaredMethod(name, targetParameterTypes);
            }
        }

        return targetMethod;
    }

    /**
     * Invocation handler wrapping the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        private final ProxyConfiguration mProxyConfiguration;

        private final RoutineConfiguration mRoutineConfiguration;

        /**
         * Constructor.
         */
        private InterfaceInvocationHandler() {

            mRoutineConfiguration = getRoutineConfiguration();
            mProxyConfiguration = getProxyConfiguration();
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final OutputChannel<Object> outputChannel =
                    method(mRoutineConfiguration, mProxyConfiguration, method).callAsync(args);
            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                TimeDuration outputTimeout = null;
                TimeoutActionType outputAction = null;
                final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

                if (timeoutAnnotation != null) {

                    outputTimeout = fromUnit(timeoutAnnotation.value(), timeoutAnnotation.unit());
                }

                final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

                if (actionAnnotation != null) {

                    outputAction = actionAnnotation.value();
                }

                if (outputTimeout != null) {

                    outputChannel.afterMax(outputTimeout);
                }

                if (outputAction == TimeoutActionType.DEADLOCK) {

                    outputChannel.eventuallyDeadlock();

                } else if (outputAction == TimeoutActionType.EXIT) {

                    outputChannel.eventuallyExit();

                } else if (outputAction == TimeoutActionType.ABORT) {

                    outputChannel.eventuallyAbort();
                }

                return outputChannel.readNext();
            }

            return null;
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class ProxyInvocationHandler implements InvocationHandler {

        private final ProxyConfiguration mProxyConfiguration;

        private final RoutineConfiguration mRoutineConfiguration;

        /**
         * Constructor.
         */
        private ProxyInvocationHandler() {

            mRoutineConfiguration = getRoutineConfiguration();
            mProxyConfiguration = getProxyConfiguration();
        }

        @Nonnull
        private Routine<Object, Object> buildRoutine(@Nonnull final Method method,
                @Nonnull final Method targetMethod, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            String shareGroup = mProxyConfiguration.getShareGroupOr(null);
            final RoutineConfiguration configuration = mRoutineConfiguration;
            final RoutineConfiguration.Builder<RoutineConfiguration> builder =
                    configuration.builderFrom();
            final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

            if (shareGroupAnnotation != null) {

                shareGroup = shareGroupAnnotation.value();
            }

            warn(configuration);
            builder.withInputOrder(
                    (inputMode == InputMode.ELEMENT) ? OrderType.NONE : OrderType.PASS_ORDER)
                   .withInputMaxSize(Integer.MAX_VALUE)
                   .withInputTimeout(TimeDuration.ZERO)
                   .withOutputOrder((outputMode == OutputMode.ELEMENT) ? OrderType.PASS_ORDER
                                            : OrderType.NONE)
                   .withOutputMaxSize(Integer.MAX_VALUE)
                   .withOutputTimeout(TimeDuration.ZERO);
            final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

            if (timeoutAnnotation != null) {

                builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
            }

            final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

            if (actionAnnotation != null) {

                builder.withReadTimeoutAction(actionAnnotation.value());
            }

            return getRoutine(builder.set(), shareGroup, targetMethod,
                              (inputMode == InputMode.COLLECTION),
                              (outputMode == OutputMode.ELEMENT));
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final WeakReference<?> targetReference = getTargetReference();

            if (targetReference == null) {

                throw new IllegalStateException("the target reference must not be null");
            }

            final Object target = targetReference.get();

            if (target == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            InputMode inputMode = null;
            OutputMode outputMode = null;
            final Class<?>[] targetParameterTypes;
            final Inputs inputsAnnotation = method.getAnnotation(Inputs.class);
            final Output outputAnnotation = method.getAnnotation(Output.class);

            if (inputsAnnotation != null) {

                if (outputAnnotation != null) {

                    throw new IllegalArgumentException(
                            "cannot have both " + Output.class.getSimpleName() + " and "
                                    + Inputs.class.getSimpleName()
                                    + " annotations on the same method: " + method);
                }

                targetParameterTypes = inputsAnnotation.value();
                inputMode = getInputMode(method);
                outputMode = OutputMode.VALUE;

                if (!method.getReturnType().isAssignableFrom(RoutineChannel.class)) {

                    throw new IllegalArgumentException(
                            "the proxy method has incompatible return type: " + method);
                }

            } else {

                targetParameterTypes = method.getParameterTypes();
                final Annotation[][] annotations = method.getParameterAnnotations();
                final int length = annotations.length;

                for (int i = 0; i < length; ++i) {

                    final InputMode paramMode = getInputMode(method, i);

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

            Method targetMethod;

            synchronized (sMethodCache) {

                final Class<?> targetClass = getTargetClass();
                final WeakIdentityHashMap<Object, HashMap<Method, Method>> methodCache =
                        sMethodCache;
                HashMap<Method, Method> methodMap = methodCache.get(targetClass);

                if (methodMap == null) {

                    methodMap = new HashMap<Method, Method>();
                    methodCache.put(targetClass, methodMap);
                }

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    try {

                        targetMethod = getTargetMethod(method, targetParameterTypes);

                    } catch (final NoSuchMethodException e) {

                        throw new IllegalArgumentException(e);
                    }

                    final Class<?> returnType = method.getReturnType();
                    final Class<?> targetReturnType = targetMethod.getReturnType();
                    boolean isError = false;

                    if (outputAnnotation != null) {

                        outputMode = getOutputMode(method, targetReturnType);

                        if ((outputMode == OutputMode.COLLECTION) && returnType.isArray()) {

                            isError = !boxingClass(returnType.getComponentType()).isAssignableFrom(
                                    boxingClass(targetReturnType));
                        }

                    } else if (inputsAnnotation == null) {

                        isError = !returnType.isAssignableFrom(targetReturnType);
                    }

                    if (isError) {

                        throw new IllegalArgumentException(
                                "the proxy method has incompatible return type: " + method);
                    }

                } else if (outputAnnotation != null) {

                    outputMode = getOutputMode(method, targetMethod.getReturnType());
                }
            }

            final Routine<Object, Object> routine =
                    buildRoutine(method, targetMethod, inputMode, outputMode);

            if (inputsAnnotation != null) {

                return (inputMode == InputMode.ELEMENT) ? routine.invokeParallel()
                        : routine.invokeAsync();
            }

            return callRoutine(routine, method, (args != null) ? args : NO_ARGS, inputMode,
                               outputMode);
        }
    }
}
