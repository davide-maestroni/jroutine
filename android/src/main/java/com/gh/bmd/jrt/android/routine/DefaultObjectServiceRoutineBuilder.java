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
package com.gh.bmd.jrt.android.routine;

import android.content.Context;
import android.os.Looper;

import com.gh.bmd.jrt.android.builder.ObjectServiceRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.AndroidSingleCallInvocation;
import com.gh.bmd.jrt.android.routine.JRoutine.InstanceFactory;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.ParamMode;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runner;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.NO_ARGS;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * Created by davide on 3/29/15.
 *
 * @param <CLASS> the wrapped object class.
 */
class DefaultObjectServiceRoutineBuilder<CLASS> implements ObjectServiceRoutineBuilder {

    private final Context mContext;

    private final Class<? extends InstanceFactory<CLASS>> mFactoryClass;

    private final Class<CLASS> mTargetClass;

    private Object[] mArgs = Reflection.NO_ARGS;

    private RoutineConfiguration mConfiguration;

    private Class<? extends Log> mLogClass;

    private Looper mLooper;

    private Class<? extends Runner> mRunnerClass;

    private Class<? extends RoutineService> mServiceClass;

    private String mShareGroup;

    /**
     * Constructor.
     *
     * @param context      the routine context.
     * @param classToken   the object class token.
     * @param factoryToken the object factory class token.
     * @throws java.lang.NullPointerException if any of the parameter is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultObjectServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final ClassToken<CLASS> classToken,
            @Nonnull final ClassToken<? extends InstanceFactory<CLASS>> factoryToken) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        mContext = context;
        mTargetClass = classToken.getRawClass();
        mFactoryClass = factoryToken.getRawClass();
    }

    @Nonnull
    private static ParamMode getReturnMode(@Nonnull final Pass annotation,
            @Nonnull final Class<?> returnType) {

        ParamMode paramMode = annotation.mode();

        if (paramMode == ParamMode.AUTO) {

            if (returnType.isArray() || returnType.isAssignableFrom(List.class)) {

                paramMode = ParamMode.PARALLEL;

            } else if (returnType.isAssignableFrom(OutputChannel.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnClass.isArray() || Iterable.class.isAssignableFrom(returnClass)) {

                    paramMode = ParamMode.COLLECTION;

                } else {

                    paramMode = ParamMode.OBJECT;
                }
            }
        }

        return paramMode;
    }

    @Nullable
    private static String withShareAnnotation(@Nullable final String shareGroup,
            @Nonnull final Method method) {

        final Share shareAnnotation = method.getAnnotation(Share.class);

        if (shareAnnotation != null) {

            return shareAnnotation.value();
        }

        return shareGroup;
    }

    @Nullable
    private static RoutineConfiguration withTimeoutAnnotation(
            @Nullable final RoutineConfiguration configuration, @Nonnull final Method method) {

        final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            return RoutineConfiguration.notNull(configuration)
                                       .builderFrom()
                                       .withReadTimeout(timeoutAnnotation.value(),
                                                        timeoutAnnotation.unit())
                                       .onReadTimeout(timeoutAnnotation.action())
                                       .buildConfiguration();
        }

        return configuration;
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        Method targetMethod = null;
        final Class<CLASS> targetClass = mTargetClass;

        for (final Method method : targetClass.getMethods()) {

            final Bind annotation = method.getAnnotation(Bind.class);

            if ((annotation != null) && name.equals(annotation.value())) {

                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {

            for (final Method method : targetClass.getDeclaredMethods()) {

                final Bind annotation = method.getAnnotation(Bind.class);

                if ((annotation != null) && name.equals(annotation.value())) {

                    targetMethod = method;
                    break;
                }
            }

            if (targetMethod == null) {

                throw new IllegalArgumentException(
                        "no annotated method with name '" + name + "' has been found");
            }
        }

        final ClassToken<BoundMethodInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<BoundMethodInvocation<INPUT, OUTPUT>>() {};
        final Class<? extends InstanceFactory> tokenClass = mFactoryClass;
        final Object[] args = mArgs;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(findConstructor(tokenClass, args), args,
                                 withShareAnnotation(mShareGroup, targetMethod), name)
                       .withConfiguration(withTimeoutAnnotation(mConfiguration, targetMethod))
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        Method targetMethod;
        final Class<CLASS> targetClass = mTargetClass;

        try {

            targetMethod = targetClass.getMethod(name, parameterTypes);

        } catch (final NoSuchMethodException ignored) {

            try {

                targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);

            } catch (final NoSuchMethodException e) {

                throw new IllegalArgumentException(e);
            }
        }

        final ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>>() {};
        final Class<? extends InstanceFactory> tokenClass = mFactoryClass;
        final Object[] args = mArgs;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(findConstructor(tokenClass, args), args,
                                 withShareAnnotation(mShareGroup, targetMethod), name,
                                 parameterTypes)
                       .withConfiguration(withTimeoutAnnotation(mConfiguration, targetMethod))
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler<CLASS>(this, itf));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public ObjectServiceRoutineBuilder dispatchingOn(@Nullable final Looper looper) {

        mLooper = looper;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withLogClass(@Nullable final Class<? extends Log> logClass) {

        mLogClass = logClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withRunnerClass(
            @Nullable final Class<? extends Runner> runnerClass) {

        mRunnerClass = runnerClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withServiceClass(
            @Nullable final Class<? extends RoutineService> serviceClass) {

        mServiceClass = serviceClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withArgs(@Nullable final Object... args) {

        mArgs = (args == null) ? Reflection.NO_ARGS : args.clone();
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    /**
     * Bound method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class BoundMethodInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBindingName;

        private final String mShareGroup;

        private final Constructor<? extends InstanceFactory> mTokenConstructor;

        private Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param constructor the object factory constructor.
         * @param args        the factory constructor arguments.
         * @param shareGroup  the share group name.
         * @param name        the binding name.
         */
        public BoundMethodInvocation(
                @Nonnull final Constructor<? extends InstanceFactory> constructor,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name) {

            mTokenConstructor = constructor;
            mArgs = args;
            mShareGroup = shareGroup;
            mBindingName = name;
        }

        @Override
        public void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(mTokenConstructor.newInstance(mArgs).newInstance(context))
                                   .withShareGroup(mShareGroup).boundMethod(mBindingName);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private final Constructor<? extends InstanceFactory> mTokenConstructor;

        private Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param constructor    the object factory constructor.
         * @param args           the factory constructor arguments.
         * @param shareGroup     the share group name.
         * @param name           the method name.
         * @param parameterTypes the method parameter types.
         */
        public MethodSignatureInvocation(
                @Nonnull final Constructor<? extends InstanceFactory> constructor,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name, @Nonnull final Class<?>[] parameterTypes) {

            mTokenConstructor = constructor;
            mArgs = args;
            mShareGroup = shareGroup;
            mMethodName = name;
            mParameterTypes = parameterTypes;
        }

        @Override
        public void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(mTokenConstructor.newInstance(mArgs).newInstance(context))
                                   .withShareGroup(mShareGroup)
                                   .method(mMethodName, mParameterTypes);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends AndroidSingleCallInvocation<Object, Object> {

        private final Object[] mArgs;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final Class<?> mProxyClass;

        private final String mShareGroup;

        private final Constructor<? extends InstanceFactory> mTokenConstructor;

        private Object mProxy;

        /**
         * Constructor.
         *
         * @param proxyClass     the proxy class.
         * @param constructor    the object factory constructor.
         * @param args           the factory constructor arguments.
         * @param shareGroup     the share group name.
         * @param name           the method name.
         * @param parameterTypes the method parameter types.
         */
        public ProxyInvocation(@Nonnull final Class<?> proxyClass,
                @Nonnull final Constructor<? extends InstanceFactory> constructor,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name, @Nonnull final Class<?>[] parameterTypes) {

            mTokenConstructor = constructor;
            mArgs = args;
            mProxyClass = proxyClass;
            mShareGroup = shareGroup;
            mMethodName = name;
            mParameterTypes = parameterTypes;
        }

        @Override
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            try {

                final Object proxy = mProxy;
                final Method method = mProxyClass.getMethod(mMethodName, mParameterTypes);
                final Object methodResult = method.invoke(proxy, objects.toArray());
                final Class<?> returnType = method.getReturnType();

                if (!Void.class.equals(boxingClass(returnType))) {

                    final Pass annotation = method.getAnnotation(Pass.class);

                    if (annotation != null) {

                        final ParamMode returnMode = getReturnMode(annotation, returnType);

                        if ((returnMode == ParamMode.OBJECT) || ((returnMode == ParamMode.OBJECT)
                                && OutputChannel.class.isAssignableFrom(returnType))) {

                            result.pass((OutputChannel<?>) methodResult);

                        } else {

                            result.pass(methodResult);
                        }

                    } else {

                        result.pass(methodResult);
                    }
                }

            } catch (final NoSuchMethodException e) {

                throw new InvocationException(e);

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mProxy = JRoutine.on(mTokenConstructor.newInstance(mArgs).newInstance(context))
                                 .withShareGroup(mShareGroup)
                                 .buildProxy(mProxyClass);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler<CLASS> implements InvocationHandler {

        private final Object[] mArgs;

        private final RoutineConfiguration mConfiguration;

        private final Context mContext;

        private final Class<? extends Log> mLogClass;

        private final Looper mLooper;

        private final Class<?> mProxyClass;

        private final Class<? extends Runner> mRunnerClass;

        private final Class<? extends RoutineService> mServiceClass;

        private final String mShareGroup;

        private final Constructor<? extends InstanceFactory<CLASS>> mTokenConstructor;

        /**
         * Constructor.
         *
         * @param builder    the builder instance.
         * @param proxyClass the proxy class.
         */
        private ProxyInvocationHandler(
                @Nonnull final DefaultObjectServiceRoutineBuilder<CLASS> builder,
                @Nonnull final Class<?> proxyClass) {

            final Object[] args = builder.mArgs;

            mContext = builder.mContext;
            mTokenConstructor =
                    findConstructor(builder.mFactoryClass, (args != null) ? args : NO_ARGS);
            mArgs = args;
            mServiceClass = builder.mServiceClass;
            mConfiguration = RoutineConfiguration.notNull(builder.mConfiguration)
                                                 .builderFrom()
                                                 .buildConfiguration();
            mShareGroup = builder.mShareGroup;
            mRunnerClass = builder.mRunnerClass;
            mLogClass = builder.mLogClass;
            mLooper = builder.mLooper;
            mProxyClass = proxyClass;
        }

        public Object invoke(final Object proxy, @Nonnull final Method method,
                final Object[] args) throws Throwable {

            final OutputChannel<Object> outputChannel =
                    JRoutine.onService(mContext, ClassToken.tokenOf(ProxyInvocation.class))
                            .withArgs(mProxyClass, mTokenConstructor, mArgs,
                                      withShareAnnotation(mShareGroup, method), method.getName(),
                                      method.getParameterTypes())
                            .withConfiguration(withTimeoutAnnotation(mConfiguration, method))
                            .withServiceClass(mServiceClass)
                            .withRunnerClass(mRunnerClass)
                            .withLogClass(mLogClass)
                            .dispatchingOn(mLooper)
                            .callAsync(args);
            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                if (method.getAnnotation(Pass.class) != null) {

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
    }
}
