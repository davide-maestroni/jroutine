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

import com.bmd.jrt.annotation.Bind;
import com.bmd.jrt.annotation.Pass;
import com.bmd.jrt.annotation.Pass.PassingMode;
import com.bmd.jrt.annotation.Share;
import com.bmd.jrt.annotation.Timeout;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.time.TimeDuration;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.builder.RoutineConfiguration.OrderBy;
import static com.bmd.jrt.common.Reflection.boxingClass;
import static com.bmd.jrt.common.Reflection.findConstructor;
import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see com.bmd.jrt.annotation.Bind
 * @see com.bmd.jrt.annotation.Pass
 * @see com.bmd.jrt.annotation.Share
 * @see com.bmd.jrt.annotation.Timeout
 * @see com.bmd.jrt.annotation.Wrap
 */
public class ObjectRoutineBuilder extends ClassRoutineBuilder {

    public static final CacheHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new CacheHashMap<Object, HashMap<ClassInfo, Object>>();

    private static final CacheHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new CacheHashMap<Object, HashMap<Method, Method>>();

    /**
     * Constructor.
     *
     * @param target the target object instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    ObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);
    }

    /**
     * Constructor.
     *
     * @param targetReference the reference to the target object.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    ObjectRoutineBuilder(@Nonnull final WeakReference<?> targetReference) {

        super(targetReference);
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
                            "the async input array of type " + PassingMode.PARALLEL
                                    + " does not match the bound type: "
                                    + paramClass.getCanonicalName());
                }

                if (length > 1) {

                    throw new IllegalArgumentException(
                            "an async input of type " + PassingMode.PARALLEL +
                                    " cannot be applied to a method taking " + length +
                                    " input parameter");

                }

                passingMode = PassingMode.PARALLEL;

            } else {

                throw new IllegalArgumentException(
                        "cannot automatically chose and async type for an output of type: "
                                + parameterType.getCanonicalName());
            }

        } else if (passingMode == PassingMode.OBJECT) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "an async input of type " + PassingMode.OBJECT + " must extends an "
                                + OutputChannel.class.getCanonicalName());
            }

        } else if (passingMode == PassingMode.COLLECTION) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "an async input of type " + PassingMode.COLLECTION + " must extends an "
                                + OutputChannel.class.getCanonicalName());
            }

            if (!paramClass.isArray() && !paramClass.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "an async input of type " + PassingMode.COLLECTION
                                + " must be bound to an array or a super class of "
                                + List.class.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "an async input of type " + PassingMode.COLLECTION +
                                " cannot be applied to a method taking " + length
                                + " input parameter");
            }

        } else { // AsyncType.PARALLEL

            if (!isArray && !Iterable.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException("an async input of type " + PassingMode.PARALLEL
                                                           + " must be an array or implement an "
                                                           + Iterable.class.getCanonicalName());
            }

            if (isArray && !boxingClass(paramClass).isAssignableFrom(
                    boxingClass(parameterType.getComponentType()))) {

                throw new IllegalArgumentException(
                        "the async input array of type " + PassingMode.PARALLEL
                                + " does not match the bound type: "
                                + paramClass.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "an async input of type " + PassingMode.PARALLEL +
                                " cannot be applied to a method taking " + length
                                + " input parameter");
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
                            "the async output array of type " + PassingMode.PARALLEL
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
                        "cannot automatically chose and async type for an input of type: "
                                + returnType.getCanonicalName());
            }

        } else if (passingMode == PassingMode.OBJECT) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException("an async output of type " + PassingMode.OBJECT
                                                           + " must be a super class of "
                                                           + channelClassName);
            }

        } else if (passingMode == PassingMode.COLLECTION) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException("an async output of type " + PassingMode.OBJECT
                                                           + " must be a super class of "
                                                           + channelClassName);
            }

            final Class<?> returnClass = annotation.value();

            if (!returnClass.isArray() && !Iterable.class.isAssignableFrom(returnClass)) {

                throw new IllegalArgumentException(
                        "an async output of type " + PassingMode.COLLECTION
                                + " must be bound to an array or a type implementing an "
                                + Iterable.class.getCanonicalName());
            }

        } else { // AsyncType.PARALLEL

            if (!returnType.isArray() && !returnType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException("an async output of type " + PassingMode.PARALLEL
                                                           + " must be an array or a super class " +
                                                           "of " + List.class.getCanonicalName());
            }

            final Class<?> returnClass = annotation.value();

            if (returnType.isArray() && !boxingClass(
                    returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                throw new IllegalArgumentException(
                        "the async output array of type " + PassingMode.PARALLEL
                                + " does not match the bound type: "
                                + returnClass.getCanonicalName());
            }
        }

        return passingMode;
    }

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.annotation.Bind} and {@link com.bmd.jrt.annotation.Timeout}
     * annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the annotation name
     * will be used to bind the interface method with the instance ones. If no name is assigned, the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    public <CLASS> CLASS buildProxy(@Nonnull final Class<CLASS> itf) {

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

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.annotation.Bind} and {@link com.bmd.jrt.annotation.Timeout}
     * annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the annotation name
     * will be used to bind the interface method with the instance ones. If no name is assigned, the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf     the token of the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    public <CLASS> CLASS buildProxy(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
    }

    /**
     * Returns a wrapper object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.annotation.Bind}, {@link com.bmd.jrt.annotation.Timeout} and
     * {@link com.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.Wrap}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor package
     * to the specific project dependencies.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the wrapping object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    public <CLASS> CLASS buildWrapper(@Nonnull final Class<CLASS> itf) {

        //TODO: avoid reflection...

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        synchronized (sClassMap) {

            final WeakReference<?> targetReference = getTargetReference();
            final Object target = (targetReference != null) ? targetReference.get() : getTarget();

            if (target == null) {

                throw new IllegalStateException("target object has been destroyed");
            }

            final CacheHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final String shareGroup = getShareGroup();
            final String classShareGroup = (shareGroup != null) ? shareGroup : Share.ALL;
            final RoutineConfiguration configuration =
                    RoutineConfiguration.notNull(getConfiguration());
            final ClassInfo classInfo = new ClassInfo(configuration, itf, classShareGroup);
            Object instance = classes.get(classInfo);

            if (instance != null) {

                return itf.cast(instance);
            }

            try {

                final Package classPackage = itf.getPackage();
                final String packageName =
                        (classPackage != null) ? classPackage.getName() + "." : "";
                final String className = packageName + itf.getSimpleName() + "$$Wrapper";
                final Class<?> wrapperClass = Class.forName(className);
                final Constructor<?> constructor =
                        findConstructor(wrapperClass, target, sMutexCache, classShareGroup,
                                        configuration);

                synchronized (sMutexCache) {

                    instance = constructor.newInstance(target, sMutexCache, classShareGroup,
                                                       configuration);
                }

                classes.put(classInfo, instance);
                return itf.cast(instance);

            } catch (final InstantiationException e) {

                throw new IllegalArgumentException(e.getCause());

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    /**
     * Returns a wrapper object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.annotation.Bind}, {@link com.bmd.jrt.annotation.Timeout} and
     * {@link com.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.Wrap}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor package
     * to the specific project dependencies.
     *
     * @param itf     the token of the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the wrapping object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    public <CLASS> CLASS buildWrapper(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(buildWrapper(itf.getRawClass()));
    }

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     */
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
     * Class used as key to identify a specific wrapper instance.
     */
    private static class ClassInfo {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param configuration the routine configuration.
         * @param itf           the wrapper interface.
         * @param shareGroup    the group name.
         */
        private ClassInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Class<?> itf, @Nonnull final String shareGroup) {

            mConfiguration = configuration;
            mItf = itf;
            mShareGroup = shareGroup;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mItf.hashCode();
            result = 31 * result + mShareGroup.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassInfo)) {

                return false;
            }

            final ClassInfo that = (ClassInfo) o;
            return mConfiguration.equals(that.mConfiguration) && mItf.equals(that.mItf)
                    && mShareGroup.equals(that.mShareGroup);
        }
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

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final WeakReference<?> targetReference = getTargetReference();
            final Object target = (targetReference != null) ? targetReference.get() : getTarget();

            if (target == null) {

                throw new IllegalStateException("target object has been destroyed");
            }

            final Class<?> returnType = method.getReturnType();
            final Class<?>[] targetParameterTypes = method.getParameterTypes();
            PassingMode asyncParamMode = null;
            PassingMode asyncReturnMode = null;

            Method targetMethod;

            synchronized (sMethodCache) {

                final CacheHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
                HashMap<Method, Method> methodMap = methodCache.get(target);

                if (methodMap == null) {

                    methodMap = new HashMap<Method, Method>();
                    methodCache.put(target, methodMap);
                }

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
                        asyncParamMode =
                                getParamMode(passAnnotation, targetParameterTypes[i], length);
                        targetParameterTypes[i] = passAnnotation.value();
                    }
                }

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    targetMethod = getTargetMethod(method, targetParameterTypes);

                    final Class<?> targetReturnType = targetMethod.getReturnType();
                    boolean isError = false;

                    if (methodAnnotation == null) {

                        isError = !returnType.isAssignableFrom(targetReturnType);

                    } else {

                        if ((asyncReturnMode == PassingMode.PARALLEL) && returnType.isArray()) {

                            isError = !boxingClass(returnType.getComponentType()).isAssignableFrom(
                                    boxingClass(targetReturnType));
                        }

                        isError |= !returnClass.isAssignableFrom(targetReturnType);
                    }

                    if (isError) {

                        throw new IllegalArgumentException(
                                "bound method has incompatible return type: " + targetMethod);
                    }

                    methodMap.put(method, targetMethod);
                }
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
            final Builder builder = RoutineConfiguration.builderFrom(mConfiguration);
            final Share shareAnnotation = method.getAnnotation(Share.class);

            if (shareAnnotation != null) {

                final String annotationShareGroup = shareAnnotation.value();

                if (!Share.ALL.equals(annotationShareGroup)) {

                    shareGroup = annotationShareGroup;
                }
            }

            if (paramMode == PassingMode.OBJECT) {

                builder.withInputOrder(OrderBy.PASSING);
            }

            builder.withInputSize(Integer.MAX_VALUE)
                   .withInputTimeout(TimeDuration.ZERO)
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

        @Override
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
