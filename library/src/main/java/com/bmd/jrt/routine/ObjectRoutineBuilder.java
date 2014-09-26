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
import com.bmd.jrt.runner.Runner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Class implementing a builder of a routine wrapping an object instance.
 * <p/>
 * Created by davide on 9/21/14.
 */
public class ObjectRoutineBuilder extends ClassRoutineBuilder {

    private static final HashMap<Method, Method> mMethodMap = new HashMap<Method, Method>();

    private final Class<?> mTargetClass;

    /**
     * Constructor.
     *
     * @param target the target object instance.
     */
    ObjectRoutineBuilder(final Object target) {

        super(target);

        mTargetClass = target.getClass();
    }

    /**
     * Returns a proxy object enable the asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.routine.AsynMethod} annotation. In case the target instance does
     * not implement the specified interface, the name attribute will be used to bind the interface
     * method with the instance ones. If no name is assigned the one of the interface method will
     * be used instead.
     *
     * @param itf     the interface implemented by the return object.
     * @param <CLASS> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class is null or does not
     *                                            represent an interface.
     */
    public <CLASS> CLASS asyn(final Class<CLASS> itf) {

        if (itf == null) {

            throw new IllegalArgumentException("the interface type must be null");
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

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final Class<?> targetClass = mTargetClass;
            final Class<?> returnType = method.getReturnType();

            Method targetMethod;
            Runner runner = null;
            Boolean isSequential = null;

            synchronized (mMethodMap) {

                final HashMap<Method, Method> methodMap = mMethodMap;

                targetMethod = methodMap.get(method);

                if (targetMethod == null) {

                    String name = null;
                    final AsynMethod annotation = method.getAnnotation(AsynMethod.class);

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

                    final Class<?>[] parameterTypes = method.getParameterTypes();

                    try {

                        targetMethod = targetClass.getMethod(name, parameterTypes);

                    } catch (final NoSuchMethodException ignored) {

                    }

                    if ((targetMethod == null) || !targetMethod.getReturnType()
                                                               .equals(returnType)) {

                        targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);
                    }

                    if (!targetMethod.getReturnType().equals(returnType)) {

                        throw new NoSuchMethodException();
                    }

                    methodMap.put(method, targetMethod);
                }
            }

            final OutputChannel<Object> outputChannel =
                    getRoutine(targetMethod, runner, isSequential).invokeAsyn(args);

            if (!ReflectionUtils.boxingClass(returnType).equals(Void.class)) {

                return outputChannel.iterator().next();
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

            final OutputChannel<Object> outputChannel = classMethod(method).invokeAsyn(args);

            final Class<?> returnType = method.getReturnType();

            if (!ReflectionUtils.boxingClass(returnType).equals(Void.class)) {

                return outputChannel.iterator().next();
            }

            return null;
        }
    }
}