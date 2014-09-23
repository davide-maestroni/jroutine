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
 * Created by davide on 9/21/14.
 */
public class InstanceRoutineBuilder extends MethodRoutineBuilder {

    private static final HashMap<Method, Method> mMethodMap = new HashMap<Method, Method>();

    private final Class<?> mTargetClass;

    InstanceRoutineBuilder(final Object target) {

        super(target);

        mTargetClass = target.getClass();
    }

    public <CLASS> CLASS asyn(final Class<CLASS> itf) {

        if (itf == null) {

            throw new IllegalArgumentException();
        }

        if (!itf.isInterface()) {

            throw new IllegalArgumentException();
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
    public InstanceRoutineBuilder queued() {

        super.queued();

        return this;
    }

    @Override
    public InstanceRoutineBuilder runningOn(final Runner runner) {

        super.runningOn(runner);

        return this;
    }

    @Override
    public InstanceRoutineBuilder sequential() {

        super.sequential();

        return this;
    }

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

                        if (runnerClass != NoRunner.class) {

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