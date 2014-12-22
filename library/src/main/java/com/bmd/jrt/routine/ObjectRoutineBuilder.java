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
import com.bmd.jrt.annotation.AsyncType;
import com.bmd.jrt.annotation.ParallelType;
import com.bmd.jrt.builder.DefaultConfigurationBuilder;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.common.Reflection.boxingClass;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see Async
 * @see AsyncType
 * @see ParallelType
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
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);
    }

    /**
     * Constructor.
     *
     * @param targetReference the reference to the target object.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ObjectRoutineBuilder(@Nonnull final WeakReference<?> targetReference) {

        super(targetReference);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Object callRoutine(@Nonnull final Routine<Object, Object> routine,
            @Nonnull final Method method, @Nonnull final ParamType paramType,
            @Nonnull final ResultType resultType, @Nonnull final Object[] args) {

        final Class<?> returnType = method.getReturnType();
        final OutputChannel<Object> outputChannel;

        if (paramType == ParamType.PARALLEL) {

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

        } else if (paramType == ParamType.ASYNC) {

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

        } else {

            outputChannel = routine.callAsync(args);
        }

        if (!Void.class.equals(boxingClass(returnType))) {

            if (resultType == ResultType.CHANNEL) {

                return outputChannel;
            }

            if (resultType == ResultType.LIST) {

                return outputChannel.readAll();
            }

            if (resultType == ResultType.ARRAY) {

                final List<Object> results = outputChannel.readAll();
                final int size = results.size();
                final Object array = Array.newInstance(returnType.getComponentType(), size);

                for (int i = 0; i < size; ++i) {

                    Array.set(array, i, results.get(i));
                }

                return array;
            }

            return outputChannel.readAll().iterator().next();
        }

        return null;
    }

    @Nonnull
    @Override
    public ClassRoutineBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        super.apply(configuration);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        super.availableTimeout(timeout, timeUnit);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder availableTimeout(@Nullable final TimeDuration timeout) {

        super.availableTimeout(timeout);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder logLevel(@Nonnull final LogLevel level) {

        super.logLevel(level);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder loggedWith(@Nullable final Log log) {

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
    public ObjectRoutineBuilder runBy(@Nullable final Runner runner) {

        super.runBy(runner);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder syncRunner(@Nonnull final RunnerType type) {

        super.syncRunner(type);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder lockName(@Nullable final String lockName) {

        super.lockName(lockName);
        return this;
    }

    /**
     * Returns an object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async}, {@link AsyncType} and {@link ParallelType} annotations.<br/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.AsyncClass}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor package
     * to the specific project dependencies.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the wrapping object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS implementationOf(@Nonnull final Class<CLASS> itf) {

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

            final Class<?> targetClass = getTargetClass();
            final CacheHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;

            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final String lockName = getLockName();
            final String classLockName = (lockName != null) ? lockName : Async.DEFAULT_NAME;
            final RoutineConfiguration configuration = getBuilder().buildConfiguration();
            final ClassInfo classInfo = new ClassInfo(configuration, itf, classLockName);

            Object instance = classes.get(classInfo);

            if (instance != null) {

                return itf.cast(instance);
            }

            try {

                final Package classPackage = itf.getPackage();
                final String className =
                        ((classPackage != null) ? classPackage.getName() + "." : "")
                                + itf.getSimpleName() + targetClass.getSimpleName();
                final Class<?> wrapperClass = Class.forName(className);
                final Constructor<?> constructor =
                        wrapperClass.getConstructor(targetClass, CacheHashMap.class, String.class,
                                                    RoutineConfiguration.class);

                synchronized (sMutexCache) {

                    instance = constructor.newInstance(target, sMutexCache, classLockName,
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
     * Returns an object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async}, {@link AsyncType} and {@link ParallelType} annotations.<br/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.AsyncClass}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor package
     * to the specific project dependencies.
     *
     * @param itf     the token of the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the wrapping object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS implementationOf(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(implementationOf(itf.getRawClass()));
    }

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the value attribute
     * will be used to bind the interface method with the instance ones. If no name is assigned the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link AsyncType} and {@link ParallelType} annotations will be honored.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS proxy(@Nonnull final Class<CLASS> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        final InvocationHandler handler;

        if (itf.isAssignableFrom(getTargetClass())) {

            handler = new ObjectInvocationHandler(itf);

        } else {

            handler = new InterfaceInvocationHandler(itf);
        }

        final Object proxy =
                Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf}, handler);
        return itf.cast(proxy);
    }

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the value attribute
     * will be used to bind the interface method with the instance ones. If no name is assigned the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link AsyncType} and {@link ParallelType} annotations will be honored.
     *
     * @param itf     the token of the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS proxy(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(proxy(itf.getRawClass()));
    }

    @Nonnull
    private Method getTargetMethod(@Nonnull final Method method,
            @Nonnull final Class<?>[] targetParameterTypes) throws NoSuchMethodException {

        final Class<?> targetClass = getTargetClass();
        final Async annotation = method.getAnnotation(Async.class);

        String name = null;

        if (annotation != null) {

            name = annotation.value();
        }

        if ((name == null) || (name.length() == 0)) {

            name = method.getName();
        }

        Method targetMethod = getAnnotatedMethod(name);

        if (targetMethod == null) {

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
     * Enumeration defining how parameters are passed to the proxy method.
     */
    private enum ParamType {

        ASYNC,      // through an output channel
        PARALLEL,   // through a list or array
        DEFAULT     // the normal way
    }

    /**
     * Enumeration defining how results are returned by the proxy method.
     */
    private enum ResultType {

        CHANNEL,    // through an output channel
        LIST,       // through a list
        ARRAY,      // through an array
        DEFAULT     // the normal way
    }

    /**
     * Class used as key to identify a specific wrapper instance.
     */
    private static class ClassInfo {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mLockName;

        /**
         * Constructor.
         *
         * @param configuration the routine configuration.
         * @param itf           the wrapper interface.
         * @param lockName      the lock name.
         */
        private ClassInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Class<?> itf, @Nonnull final String lockName) {

            mConfiguration = configuration;
            mItf = itf;
            mLockName = lockName;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mItf.hashCode();
            result = 31 * result + mLockName.hashCode();
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
            return mConfiguration.equals(that.mConfiguration) && mItf.equals(that.mItf) && mLockName
                    .equals(that.mLockName);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mLockName;

        /**
         * Constructor.
         *
         * @param itf the proxy interface.
         */
        private InterfaceInvocationHandler(@Nonnull final Class<?> itf) {

            final String lockName = getLockName();

            mItf = itf;
            mLockName = (lockName != null) ? lockName : Async.DEFAULT_NAME;
            mConfiguration = getBuilder().buildConfiguration();
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
            ParamType paramType = ParamType.DEFAULT;
            ResultType resultType = ResultType.DEFAULT;

            Method targetMethod;

            synchronized (sMethodCache) {

                final CacheHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
                HashMap<Method, Method> methodMap = methodCache.get(target);

                if (methodMap == null) {

                    methodMap = new HashMap<Method, Method>();
                    methodCache.put(target, methodMap);
                }

                final AsyncType overrideAnnotation = method.getAnnotation(AsyncType.class);

                if (overrideAnnotation != null) {

                    if (returnType.isArray()) {

                        resultType = ResultType.ARRAY;

                    } else if (returnType.isAssignableFrom(List.class)) {

                        resultType = ResultType.LIST;

                    } else if (returnType.isAssignableFrom(OutputChannel.class)) {

                        resultType = ResultType.CHANNEL;

                    } else {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }
                }

                final Annotation[][] annotations = method.getParameterAnnotations();
                final int length = annotations.length;

                for (int i = 0; i < length; ++i) {

                    final Annotation[] paramAnnotations = annotations[i];

                    for (final Annotation paramAnnotation : paramAnnotations) {

                        if (paramAnnotation.annotationType() == AsyncType.class) {

                            final Class<?> parameterType = targetParameterTypes[i];

                            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                                throw new IllegalArgumentException(
                                        "the async input parameter is not compatible");
                            }

                            paramType = ParamType.ASYNC;
                            targetParameterTypes[i] = ((AsyncType) paramAnnotation).value();
                            break;
                        }

                        if (paramAnnotation.annotationType() == ParallelType.class) {

                            final Class<?> parameterType = targetParameterTypes[i];

                            if ((length > 1) || (
                                    !OutputChannel.class.isAssignableFrom(parameterType)
                                            && !Iterable.class.isAssignableFrom(parameterType)
                                            && !parameterType.isArray())) {

                                throw new IllegalArgumentException(
                                        "the async input parameter is not compatible");
                            }

                            paramType = ParamType.PARALLEL;
                            targetParameterTypes[i] = ((ParallelType) paramAnnotation).value();
                            break;
                        }
                    }
                }

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    targetMethod = getTargetMethod(method, targetParameterTypes);

                    final Class<?> targetReturnType = targetMethod.getReturnType();

                    if ((overrideAnnotation == null) && !returnType.isAssignableFrom(
                            targetReturnType)) {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }

                    if ((resultType == ResultType.ARRAY) && !returnType.getComponentType()
                                                                       .isAssignableFrom(
                                                                               targetReturnType)) {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }

                    methodMap.put(method, targetMethod);
                }

                if (overrideAnnotation != null) {

                    if (!overrideAnnotation.value()
                                           .isAssignableFrom(targetMethod.getReturnType())) {

                        throw new IllegalArgumentException(
                                "the async return type is not compatible");
                    }
                }
            }

            final Routine<Object, Object> routine = buildRoutine(method, targetMethod, paramType);
            return callRoutine(routine, method, paramType, resultType, args);
        }

        private Routine<Object, Object> buildRoutine(final Method method, final Method targetMethod,
                final ParamType paramType) {

            String lockName = mLockName;
            final DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();

            final Async classAnnotation = mItf.getAnnotation(Async.class);

            if (classAnnotation != null) {

                if (Async.DEFAULT_NAME.equals(mLockName)) {

                    lockName = classAnnotation.lockName();
                }

                applyConfiguration(builder, classAnnotation);
            }

            final Async methodAnnotation = method.getAnnotation(Async.class);

            if (methodAnnotation != null) {

                if (Async.DEFAULT_NAME.equals(mLockName)) {

                    final String annotationLockName = methodAnnotation.lockName();

                    if (!Async.DEFAULT_NAME.equals(annotationLockName)) {

                        lockName = annotationLockName;
                    }
                }

                applyConfiguration(builder, methodAnnotation);
            }

            builder.apply(mConfiguration);

            if (paramType == ParamType.ASYNC) {

                builder.inputOrder(DataOrder.INSERTION);
            }

            builder.inputSize(Integer.MAX_VALUE)
                   .inputTimeout(TimeDuration.ZERO)
                   .outputSize(Integer.MAX_VALUE)
                   .outputTimeout(TimeDuration.ZERO);
            return getRoutine(builder.buildConfiguration(), lockName, targetMethod);
        }
    }

    /**
     * Invocation handler wrapping the target object instance.
     */
    private class ObjectInvocationHandler implements InvocationHandler {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mLockName;

        /**
         * Constructor.
         *
         * @param itf the proxy interface.
         */
        private ObjectInvocationHandler(@Nonnull final Class<?> itf) {

            mItf = itf;
            mLockName = getLockName();
            mConfiguration = getBuilder().buildConfiguration();
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final OutputChannel<Object> outputChannel =
                    method(mConfiguration, mLockName, mItf, method).callAsync(args);

            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                return outputChannel.readFirst();
            }

            return null;
        }
    }
}
