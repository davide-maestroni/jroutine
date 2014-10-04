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

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.WeakHashMap;

import static com.bmd.jrt.routine.ReflectionUtils.boxingClass;

/**
 * Class implementing a builder of a routine wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @see Async
 * @see AsyncParameters
 * @see AsyncResult
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
     * @throws IllegalArgumentException if a duplicate name in the annotations is detected.
     */
    ObjectRoutineBuilder(final Object target) {

        super(target);

        mTarget = target;
        mTargetClass = target.getClass();
    }

    /**
     * Returns a proxy object enable the asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link Async} annotation. If no name is assigned the one of the interface method
     * will be used instead.<br/>
     * In case the wrapped object does not implement the specified interface, the name attribute
     * will be used to bind the interface method with the instance ones. The interface will be
     * interpreted as a mirror of the target object methods, and the optional
     * {@link AsyncParameters} and {@link AsyncResult} annotations will be honored.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws NullPointerException     if the specified class is null.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     */
    public <CLASS> CLASS asAsyn(final Class<CLASS> itf) {

        if (itf == null) {

            throw new NullPointerException("the interface type must be null");
        }

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

    @Override
    public ObjectRoutineBuilder logLevel(final LogLevel level) {

        super.logLevel(level);

        return this;
    }

    @Override
    public ObjectRoutineBuilder logWith(final Log log) {

        super.logWith(log);

        return this;
    }

    @Override
    public ObjectRoutineBuilder queued() {

        super.queued();

        return this;
    }

    @Override
    public ObjectRoutineBuilder runBy(final Runner runner) {

        super.runBy(runner);

        return this;
    }

    @Override
    public ObjectRoutineBuilder sequential() {

        super.sequential();

        return this;
    }

    @Override
    public ObjectRoutineBuilder withinTry(final Catch catchClause) {

        super.withinTry(catchClause);

        return this;
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final Object target = mTarget;
            final Class<?> targetClass = mTargetClass;
            final Class<?> returnType = method.getReturnType();
            final boolean isOverrideParameters;
            final boolean isOverrideReturn;

            Method targetMethod;
            Runner runner = null;
            Boolean isSequential = null;

            synchronized (sMethodCache) {

                final WeakHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
                HashMap<Method, Method> methodMap = methodCache.get(target);

                if (methodMap == null) {

                    methodMap = new HashMap<Method, Method>();
                    methodCache.put(target, methodMap);
                }

                final AsyncParameters paramAnnotation = method.getAnnotation(AsyncParameters.class);

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    String name = null;
                    Class<?>[] parameterTypes = null;

                    final Async annotation = method.getAnnotation(Async.class);

                    if (annotation != null) {

                        name = annotation.name();

                        final Class<? extends Runner> runnerClass = annotation.runner();

                        if (runnerClass != DefaultRunner.class) {

                            runner = runnerClass.newInstance();
                        }

                        isSequential = annotation.sequential();
                    }

                    if ((name == null) || (name.length() == 0)) {

                        name = method.getName();
                    }

                    if (paramAnnotation != null) {

                        parameterTypes = paramAnnotation.value();
                        final Class<?>[] methodParameterTypes = method.getParameterTypes();

                        if (parameterTypes.length != methodParameterTypes.length) {

                            throw new IllegalArgumentException(
                                    "the async parameters are not compatible");
                        }

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

                    if ((parameterTypes == null) || (parameterTypes.length == 0)) {

                        parameterTypes = method.getParameterTypes();
                    }

                    try {

                        targetMethod = targetClass.getMethod(name, parameterTypes);

                    } catch (final NoSuchMethodException ignored) {

                    }

                    if (targetMethod == null) {

                        targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);
                    }
                }

                isOverrideReturn = (method.getAnnotation(AsyncResult.class) != null)
                        && OutputChannel.class.isAssignableFrom(returnType);

                if (!isOverrideReturn && !returnType.isAssignableFrom(
                        targetMethod.getReturnType())) {

                    throw new IllegalArgumentException("the async return type is not compatible");
                }

                methodMap.put(method, targetMethod);

                isOverrideParameters =
                        (paramAnnotation != null) && (paramAnnotation.value().length > 0);
            }

            final Routine<Object, Object> routine =
                    getRoutine(targetMethod, runner, isSequential, isOverrideParameters);
            final OutputChannel<Object> outputChannel;

            if (isOverrideParameters) {

                final ParameterChannel<Object, Object> parameterChannel = routine.invokeAsyn();

                for (final Object arg : args) {

                    if (arg instanceof OutputChannel) {

                        parameterChannel.pass((OutputChannel<?>) arg);

                    } else {

                        parameterChannel.pass(arg);
                    }
                }

                outputChannel = parameterChannel.results();

            } else {

                outputChannel = routine.runAsyn(args);
            }

            if (!boxingClass(returnType).equals(Void.class)) {

                if (isOverrideReturn) {

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

            final OutputChannel<Object> outputChannel = classMethod(method).runAsyn(args);

            final Class<?> returnType = method.getReturnType();

            if (!boxingClass(returnType).equals(Void.class)) {

                return outputChannel.readFirst();
            }

            return null;
        }
    }
}