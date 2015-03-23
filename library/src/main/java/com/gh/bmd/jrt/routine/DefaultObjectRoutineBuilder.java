/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.PassingMode;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.RoutineBuilders.Initializer;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.getProxiedMethod;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Created by davide on 9/21/14.
 */
class DefaultObjectRoutineBuilder extends DefaultClassRoutineBuilder
        implements ObjectRoutineBuilder {

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
            @Nullable final PassingMode paramType) {

        final Class<?> returnType = method.getReturnType();
        final OutputChannel<Object> outputChannel;

        if (paramType == PassingMode.PARALLEL) {

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

        } else if (paramType == PassingMode.OBJECT) {

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

        } else if (paramType == PassingMode.COLLECTION) {

            final ParameterChannel<Object, Object> parameterChannel = routine.invokeAsync();
            outputChannel = parameterChannel.pass((OutputChannel<Object>) args[0]).result();

        } else {

            outputChannel = routine.callAsync(args);
        }

        if (!Void.class.equals(boxingClass(returnType))) {

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

            return outputChannel.readNext();
        }

        return null;
    }

    @Nonnull
    private static PassingMode getParamMode(@Nonnull final Pass passAnnotation,
            @Nonnull final Class<?> parameterType, final int length) {

        PassingMode passingMode = passAnnotation.mode();
        final Class<?> paramClass = passAnnotation.value();
        final boolean isArray = parameterType.isArray();

        if (passingMode == PassingMode.AUTO) {

            if (OutputChannel.class.isAssignableFrom(parameterType)) {

                if ((length == 1) && (paramClass.isArray() || paramClass.isAssignableFrom(
                        List.class))) {

                    passingMode = PassingMode.COLLECTION;

                } else {

                    passingMode = PassingMode.OBJECT;
                }

            } else if (isArray || Iterable.class.isAssignableFrom(parameterType)) {

                if (isArray && !boxingClass(paramClass).isAssignableFrom(
                        boxingClass(parameterType.getComponentType()))) {

                    throw new IllegalArgumentException(
                            "the async input array with passing mode " + PassingMode.PARALLEL
                                    + " does not match the bound type: "
                                    + paramClass.getCanonicalName());
                }

                if (length > 1) {

                    throw new IllegalArgumentException(
                            "an async input with passing mode " + PassingMode.PARALLEL +
                                    " cannot be applied to a method taking " + length +
                                    " input parameters");

                }

                passingMode = PassingMode.PARALLEL;

            } else {

                throw new IllegalArgumentException(
                        "cannot automatically choose a passing mode for an output of type: "
                                + parameterType.getCanonicalName());
            }

        } else if (passingMode == PassingMode.OBJECT) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.OBJECT
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

        } else if (passingMode == PassingMode.COLLECTION) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.COLLECTION
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

            if (!paramClass.isArray() && !paramClass.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.COLLECTION +
                                " cannot be applied to a method taking " + length
                                + " input parameters");
            }

        } else { // AsyncType.PARALLEL

            if (!isArray && !Iterable.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.PARALLEL
                                + " must be an array or implement an "
                                + Iterable.class.getCanonicalName());
            }

            if (isArray && !boxingClass(paramClass).isAssignableFrom(
                    boxingClass(parameterType.getComponentType()))) {

                throw new IllegalArgumentException(
                        "the async input array with passing mode " + PassingMode.PARALLEL
                                + " does not match the bound type: "
                                + paramClass.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "an async input with passing mode " + PassingMode.PARALLEL +
                                " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return passingMode;
    }

    @Nonnull
    private static PassingMode getReturnMode(@Nonnull final Pass annotation,
            @Nonnull final Class<?> returnType) {

        PassingMode passingMode = annotation.mode();

        if (passingMode == PassingMode.AUTO) {

            if (returnType.isArray() || returnType.isAssignableFrom(List.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnType.isArray() && !boxingClass(
                        returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                    throw new IllegalArgumentException(
                            "the async output array with passing mode " + PassingMode.PARALLEL
                                    + " does not match the bound type: "
                                    + returnClass.getCanonicalName());
                }

                passingMode = PassingMode.PARALLEL;

            } else if (returnType.isAssignableFrom(OutputChannel.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnClass.isArray() || Iterable.class.isAssignableFrom(returnClass)) {

                    passingMode = PassingMode.COLLECTION;

                } else {

                    passingMode = PassingMode.OBJECT;
                }

            } else {

                throw new IllegalArgumentException(
                        "cannot automatically choose a passing mode for an input of type: "
                                + returnType.getCanonicalName());
            }

        } else if (passingMode == PassingMode.OBJECT) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "an async output with passing mode " + PassingMode.OBJECT
                                + " must be a superclass of " + channelClassName);
            }

        } else if (passingMode == PassingMode.COLLECTION) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "an async output with passing mode " + PassingMode.OBJECT
                                + " must be a superclass of " + channelClassName);
            }

            final Class<?> returnClass = annotation.value();

            if (!returnClass.isArray() && !Iterable.class.isAssignableFrom(returnClass)) {

                throw new IllegalArgumentException(
                        "an async output with passing mode " + PassingMode.COLLECTION
                                + " must be bound to an array or a type implementing an "
                                + Iterable.class.getCanonicalName());
            }

        } else { // AsyncType.PARALLEL

            if (!returnType.isArray() && !returnType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "an async output with passing mode " + PassingMode.PARALLEL
                                + " must be an array or a superclass " +
                                "of " + List.class.getCanonicalName());
            }

            final Class<?> returnClass = annotation.value();

            if (returnType.isArray() && !boxingClass(
                    returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                throw new IllegalArgumentException(
                        "the async output array with passing mode " + PassingMode.PARALLEL
                                + " does not match the bound type: "
                                + returnClass.getCanonicalName());
            }
        }

        return passingMode;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        final InvocationHandler handler;

        if (itf.isAssignableFrom(getTargetClass())) {

            handler = new ObjectInvocationHandler();

        } else {

            handler = new InterfaceInvocationHandler();
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
    public ObjectRoutineBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        super.withConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder withShareGroup(@Nullable final String group) {

        super.withShareGroup(group);
        return this;
    }

    @Nonnull
    private Method getTargetMethod(@Nonnull final Method method,
            @Nonnull final Class<?>[] targetParameterTypes) throws NoSuchMethodException {

        final Class<?> targetClass = getTargetClass();
        final Bind annotation = method.getAnnotation(Bind.class);

        String name = null;
        Method targetMethod = null;

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
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        private final RoutineConfiguration mConfiguration;

        private final String mShareGroup;

        /**
         * Constructor.
         */
        private InterfaceInvocationHandler() {

            mShareGroup = getShareGroup();
            mConfiguration = RoutineConfiguration.notNull(getConfiguration());
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final WeakReference<?> targetReference = getTargetReference();

            if (targetReference == null) {

                throw new IllegalStateException("target reference must not be null");
            }

            final Object target = targetReference.get();

            if (target == null) {

                throw new IllegalStateException("target object has been destroyed");
            }

            final Class<?> returnType = method.getReturnType();
            final Class<?>[] targetParameterTypes = method.getParameterTypes();
            PassingMode asyncParamMode = null;
            PassingMode asyncReturnMode = null;

            Method targetMethod;

            Class<?> returnClass = null;
            final Pass methodAnnotation = method.getAnnotation(Pass.class);

            if (methodAnnotation != null) {

                returnClass = methodAnnotation.value();
                asyncReturnMode = getReturnMode(methodAnnotation, returnType);
            }

            final Annotation[][] annotations = method.getParameterAnnotations();
            final int length = annotations.length;

            for (int i = 0; i < length; ++i) {

                final Annotation[] paramAnnotations = annotations[i];

                for (final Annotation paramAnnotation : paramAnnotations) {

                    if (paramAnnotation.annotationType() != Pass.class) {

                        continue;
                    }

                    final Pass passAnnotation = (Pass) paramAnnotation;
                    asyncParamMode = getParamMode(passAnnotation, targetParameterTypes[i], length);
                    targetParameterTypes[i] = passAnnotation.value();
                }
            }

            targetMethod = getProxiedMethod(target, method);

            if (targetMethod == null) {

                final PassingMode returnMode = asyncReturnMode;
                final Class<?> annotationReturnType = returnClass;
                targetMethod = getProxiedMethod(target, method, new Initializer<Method>() {

                    @Nonnull
                    public Method initialValue() {

                        final Method targetMethod;

                        try {

                            targetMethod = getTargetMethod(method, targetParameterTypes);

                        } catch (final NoSuchMethodException e) {

                            throw new IllegalArgumentException(e);
                        }

                        final Class<?> targetReturnType = targetMethod.getReturnType();
                        boolean isError = false;

                        if (methodAnnotation == null) {

                            isError = !returnType.isAssignableFrom(targetReturnType);

                        } else {

                            if ((returnMode == PassingMode.PARALLEL) && returnType.isArray()) {

                                isError = !boxingClass(
                                        returnType.getComponentType()).isAssignableFrom(
                                        boxingClass(targetReturnType));
                            }

                            isError |= !annotationReturnType.isAssignableFrom(targetReturnType);
                        }

                        if (isError) {

                            throw new IllegalArgumentException(
                                    "bound method has incompatible return type: " + targetMethod);
                        }

                        return targetMethod;
                    }
                });
            }

            final Routine<Object, Object> routine =
                    buildRoutine(method, targetMethod, asyncParamMode, asyncReturnMode);
            return callRoutine(routine, method, args, asyncParamMode);
        }

        @Nonnull
        private Routine<Object, Object> buildRoutine(@Nonnull final Method method,
                @Nonnull final Method targetMethod, @Nullable final PassingMode paramMode,
                @Nullable final PassingMode returnMode) {

            String shareGroup = mShareGroup;
            final RoutineConfiguration configuration = mConfiguration;
            final Builder builder = RoutineConfiguration.builderFrom(configuration);
            final Share shareAnnotation = method.getAnnotation(Share.class);

            if (shareAnnotation != null) {

                final String annotationShareGroup = shareAnnotation.value();

                if (!Share.ALL.equals(annotationShareGroup)) {

                    shareGroup = annotationShareGroup;
                }
            }

            warn(configuration);

            builder.withInputOrder(
                    (paramMode == PassingMode.PARALLEL) ? OrderType.DELIVERY : OrderType.PASSING)
                   .withInputSize(Integer.MAX_VALUE)
                   .withInputTimeout(TimeDuration.ZERO)
                   .withOutputOrder(OrderType.PASSING)
                   .withOutputSize(Integer.MAX_VALUE)
                   .withOutputTimeout(TimeDuration.ZERO);

            final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

            if (timeoutAnnotation != null) {

                builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit())
                       .onReadTimeout(timeoutAnnotation.action());
            }

            return getRoutine(builder.buildConfiguration(), shareGroup, targetMethod,
                              (paramMode == PassingMode.COLLECTION),
                              (returnMode == PassingMode.COLLECTION));
        }
    }

    /**
     * Invocation handler wrapping the target object instance.
     */
    private class ObjectInvocationHandler implements InvocationHandler {

        private final RoutineConfiguration mConfiguration;

        private final String mShareGroup;

        /**
         * Constructor.
         */
        private ObjectInvocationHandler() {

            mShareGroup = getShareGroup();
            mConfiguration = RoutineConfiguration.notNull(getConfiguration());
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final OutputChannel<Object> outputChannel =
                    method(mConfiguration, mShareGroup, method).callAsync(args);
            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                final Timeout methodAnnotation = method.getAnnotation(Timeout.class);
                TimeDuration outputTimeout = null;
                TimeoutAction outputAction = null;

                if (methodAnnotation != null) {

                    outputTimeout = fromUnit(methodAnnotation.value(), methodAnnotation.unit());
                    outputAction = methodAnnotation.action();
                }

                if (outputTimeout != null) {

                    outputChannel.afterMax(outputTimeout);
                }

                if (outputAction == TimeoutAction.DEADLOCK) {

                    outputChannel.eventuallyDeadlock();

                } else if (outputAction == TimeoutAction.EXIT) {

                    outputChannel.eventuallyExit();

                } else if (outputAction == TimeoutAction.ABORT) {

                    outputChannel.eventuallyAbort();
                }

                return outputChannel.readNext();
            }

            return null;
        }
    }
}
