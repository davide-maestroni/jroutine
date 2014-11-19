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
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.CacheHashMap;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ReflectionUtils.boxingClass;

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

    public static final CacheHashMap<Object, HashMap<WrapperInfo, Object>> sWrapperMap =
            new CacheHashMap<Object, HashMap<WrapperInfo, Object>>();

    private static final CacheHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new CacheHashMap<Object, HashMap<Method, Method>>();

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
    public ObjectRoutineBuilder availableTimeout(@Nonnull final TimeDuration timeout) {

        super.availableTimeout(timeout);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder inputMaxSize(final int inputMaxSize) {

        super.inputMaxSize(inputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder inputOrder(@Nonnull final ChannelDataOrder order) {

        super.inputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder inputTimeout(final long timeout, @Nonnull final TimeUnit timeUnit) {

        super.inputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder inputTimeout(@Nonnull final TimeDuration timeout) {

        super.inputTimeout(timeout);

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
    public ObjectRoutineBuilder outputMaxSize(final int outputMaxSize) {

        super.outputMaxSize(outputMaxSize);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder outputOrder(final ChannelDataOrder order) {

        super.outputOrder(order);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder outputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        super.outputTimeout(timeout, timeUnit);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder outputTimeout(@Nonnull final TimeDuration timeout) {

        super.outputTimeout(timeout);

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
    public ObjectRoutineBuilder syncRunner(@Nonnull final SyncRunnerType type) {

        super.syncRunner(type);

        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder lockId(@Nullable final String id) {

        super.lockId(id);

        return this;
    }

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the value attribute
     * will be used to bind the interface method with the instance ones. If no tag is assigned the
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

    /**
     * Returns a proxy object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the value attribute
     * will be used to bind the interface method with the instance ones. If no tag is assigned the
     * method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link AsyncOverride} annotation will be honored.
     *
     * @param itf     the token of the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    @Nonnull
    public <CLASS> CLASS as(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(as(itf.getRawClass()));
    }

    /**
     * Returns an object enabling asynchronous calls to the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} and {@link AsyncOverride} annotation.<br/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.AsyncWrapper}.<br/>
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
    public <CLASS> CLASS wrappedAs(@Nonnull final Class<CLASS> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        synchronized (sWrapperMap) {

            final Object target = mTarget;
            final Class<?> targetClass = mTargetClass;
            final CacheHashMap<Object, HashMap<WrapperInfo, Object>> wrapperMap = sWrapperMap;

            HashMap<WrapperInfo, Object> wrappers = wrapperMap.get(target);

            if (wrappers == null) {

                wrappers = new HashMap<WrapperInfo, Object>();
                wrapperMap.put(target, wrappers);
            }

            final String lockId = getLockId();
            final String wrapperLockId = (lockId != null) ? lockId : "";
            final RoutineConfiguration configuration = getBuilder().buildConfiguration();
            final WrapperInfo wrapperInfo = new WrapperInfo(configuration, itf, wrapperLockId);

            Object wrapper = wrappers.get(wrapperInfo);

            if (wrapper != null) {

                return itf.cast(wrapper);
            }

            try {

                final String wrapperClassName =
                        itf.getPackage().getName() + "." + itf.getSimpleName()
                                + targetClass.getSimpleName();

                final Class<?> wrapperClass = Class.forName(wrapperClassName);
                final Constructor<?> constructor =
                        wrapperClass.getConstructor(target.getClass(), Map.class, String.class,
                                                    RoutineConfiguration.class);

                synchronized (sMutexCache) {

                    wrapper = constructor.newInstance(target, sMutexCache, wrapperLockId,
                                                      configuration);
                }

                wrappers.put(wrapperInfo, wrapper);

                return itf.cast(wrapper);

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
     * optional {@link Async} and {@link AsyncOverride} annotation.<br/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.AsyncWrapper}.<br/>
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
    public <CLASS> CLASS wrappedAs(@Nonnull final ClassToken<CLASS> itf) {

        return itf.cast(wrappedAs(itf.getRawClass()));
    }

    /**
     * Class used as key to identify a specific wrapper instance.
     */
    private static class WrapperInfo {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mLockId;

        /**
         * Constructor.
         *
         * @param configuration the routine configuration.
         * @param itf           the wrapper interface.
         * @param lockId        the lock ID.
         */
        private WrapperInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Class<?> itf, @Nonnull final String lockId) {

            mConfiguration = configuration;
            mItf = itf;
            mLockId = lockId;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mItf.hashCode();
            result = 31 * result + mLockId.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof WrapperInfo)) {

                return false;
            }

            final WrapperInfo that = (WrapperInfo) o;

            return mConfiguration.equals(that.mConfiguration) && mItf.equals(that.mItf)
                    && mLockId.equals(that.mLockId);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        private final RoutineConfiguration mConfiguration;

        private final String mLockId;

        private final Object mTarget;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         */
        private InterfaceInvocationHandler() {

            mTarget = ObjectRoutineBuilder.this.mTarget;
            mTargetClass = ObjectRoutineBuilder.this.mTargetClass;
            mLockId = getLockId();
            mConfiguration = getBuilder().buildConfiguration();
        }

        @Override
        @SuppressWarnings("unchecked")
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
            RoutineConfiguration configuration = mConfiguration;

            synchronized (sMethodCache) {

                final CacheHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
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

                    if ((parameterTypes.length > 0) && (parameterTypes.length
                            != methodParameterTypes.length)) {

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

                        name = annotation.value();
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

                    methodMap.put(method, targetMethod);
                }

                if (annotation != null) {

                    if (lockId == null) {

                        lockId = annotation.lockId();
                    }

                    configuration = overrideConfiguration(configuration, annotation);
                }
            }

            if (isOverrideParameters) {

                configuration = new RoutineConfigurationBuilder(configuration).inputOrder(
                        ChannelDataOrder.INSERTION).buildConfiguration();
            }

            final Routine<Object, Object> routine = getRoutine(configuration, targetMethod, lockId);
            final OutputChannel<Object> outputChannel;

            if (isParallel) {

                final ParameterChannel<Object, Object> parameterChannel = routine.invokeParallel();
                final Object arg = args[0];

                if (isOverrideParameters) {

                    parameterChannel.pass((OutputChannel<Object>) arg);

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

                        parameterChannel.pass((OutputChannel<Object>) arg);

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
