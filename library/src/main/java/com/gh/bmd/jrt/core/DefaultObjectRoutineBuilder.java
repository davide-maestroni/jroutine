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

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration.Configurable;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ParameterChannel;
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

import static com.gh.bmd.jrt.builder.RoutineBuilders.getParamMode;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getReturnMode;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Created by davide on 9/21/14.
 */
class DefaultObjectRoutineBuilder extends DefaultClassRoutineBuilder
        implements ObjectRoutineBuilder {

    private static final WeakIdentityHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new WeakIdentityHashMap<Object, HashMap<Method, Method>>();

    private Configurable<ObjectRoutineBuilder> mConfigurable =
            new Configurable<ObjectRoutineBuilder>() {

                @Nonnull
                public ObjectRoutineBuilder configureWith(
                        @Nonnull final ProxyConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.configureWith(configuration);
                }

                @Nonnull
                public ObjectRoutineBuilder configureWith(
                        @Nonnull final RoutineConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.configureWith(configuration);
                }
            };

    /**
     * Constructor.
     *
     * @param target the target object instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    DefaultObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Object callRoutine(@Nonnull final Routine<Object, Object> routine,
            @Nonnull final Method method, @Nonnull final Object[] args,
            @Nullable final PassMode paramMode, @Nullable final PassMode returnMode) {

        final Class<?> returnType = method.getReturnType();
        final OutputChannel<Object> outputChannel;

        if (paramMode == PassMode.PARALLEL) {

            final ParameterChannel<Object, Object> parameterChannel = routine.invokeParallel();
            final Class<?> parameterType = method.getParameterTypes()[0];
            final Object arg = args[0];

            if (arg == null) {

                parameterChannel.pass((Iterable<Object>) null);

            } else if (OutputChannel.class.isAssignableFrom(parameterType)) {

                parameterChannel.pass((OutputChannel<Object>) arg);

            } else if (parameterType.isArray()) {

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

            outputChannel = parameterChannel.result();

        } else if (paramMode == PassMode.OBJECT) {

            final ParameterChannel<Object, Object> parameterChannel = routine.invokeAsync();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final int length = args.length;

            for (int i = 0; i < length; ++i) {

                final Object arg = args[i];

                if (OutputChannel.class.isAssignableFrom(parameterTypes[i])) {

                    parameterChannel.pass((OutputChannel<Object>) arg);

                } else {

                    parameterChannel.pass(arg);
                }
            }

            outputChannel = parameterChannel.result();

        } else if (paramMode == PassMode.COLLECTION) {

            final ParameterChannel<Object, Object> parameterChannel = routine.invokeAsync();
            outputChannel = parameterChannel.pass((OutputChannel<Object>) args[0]).result();

        } else {

            outputChannel = routine.callAsync(args);
        }

        if (!Void.class.equals(boxingClass(returnType))) {

            if (returnMode != null) {

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

            return outputChannel.readNext();
        }

        return null;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
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
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> configure() {

        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(mConfigurable,
                                                                    getRoutineConfiguration(),
                                                                    getProxyConfiguration());
    }

    @Nonnull
    @Override
    protected ObjectRoutineBuilder configureWith(@Nonnull final ProxyConfiguration configuration) {

        super.configureWith(configuration);
        return this;
    }

    @Nonnull
    @Override
    protected ObjectRoutineBuilder configureWith(
            @Nonnull final RoutineConfiguration configuration) {

        super.configureWith(configuration);
        return this;
    }

    @Nonnull
    private Method getTargetMethod(@Nonnull final Method method,
            @Nonnull final Class<?>[] targetParameterTypes) throws NoSuchMethodException {

        String name = null;
        Method targetMethod = null;
        final Class<?> targetClass = getTargetClass();
        final Bind annotation = method.getAnnotation(Bind.class);

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
                @Nonnull final Method targetMethod, @Nullable final PassMode paramMode,
                @Nullable final PassMode returnMode) {

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
                    (paramMode == PassMode.PARALLEL) ? OrderType.NONE : OrderType.PASSING_ORDER)
                   .withInputSize(Integer.MAX_VALUE)
                   .withInputTimeout(TimeDuration.ZERO)
                   .withOutputOrder((returnMode == PassMode.COLLECTION) ? OrderType.PASSING_ORDER
                                            : OrderType.NONE)
                   .withOutputSize(Integer.MAX_VALUE)
                   .withOutputTimeout(TimeDuration.ZERO);
            final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

            if (timeoutAnnotation != null) {

                builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
            }

            final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

            if (actionAnnotation != null) {

                builder.onReadTimeout(actionAnnotation.value());
            }

            return getRoutine(builder.then(), shareGroup, targetMethod,
                              (paramMode == PassMode.COLLECTION),
                              (returnMode == PassMode.COLLECTION));
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

            PassMode asyncParamMode = null;
            PassMode asyncReturnMode = null;
            Class<?> returnClass = null;
            final Pass methodAnnotation = method.getAnnotation(Pass.class);

            if (methodAnnotation != null) {

                returnClass = methodAnnotation.value();
                asyncReturnMode = getReturnMode(method);
            }

            final Class<?>[] targetParameterTypes = method.getParameterTypes();
            final Annotation[][] annotations = method.getParameterAnnotations();
            final int length = annotations.length;

            for (int i = 0; i < length; ++i) {

                final PassMode paramMode = getParamMode(method, i);

                if (paramMode != null) {

                    asyncParamMode = paramMode;

                    for (final Annotation paramAnnotation : annotations[i]) {

                        if (paramAnnotation.annotationType() == Pass.class) {

                            final Pass passAnnotation = (Pass) paramAnnotation;
                            targetParameterTypes[i] = passAnnotation.value();
                            break;
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

                    if (methodAnnotation == null) {

                        isError = !returnType.isAssignableFrom(targetReturnType);

                    } else {

                        if ((asyncReturnMode == PassMode.PARALLEL) && returnType.isArray()) {

                            isError = !boxingClass(returnType.getComponentType()).isAssignableFrom(
                                    boxingClass(targetReturnType));
                        }

                        isError |= !returnClass.isAssignableFrom(targetReturnType);
                    }

                    if (isError) {

                        throw new IllegalArgumentException(
                                "the proxy method has incompatible return type: " + method);
                    }
                }
            }

            final Routine<Object, Object> routine =
                    buildRoutine(method, targetMethod, asyncParamMode, asyncReturnMode);
            return callRoutine(routine, method, args, asyncParamMode, asyncReturnMode);
        }
    }
}
