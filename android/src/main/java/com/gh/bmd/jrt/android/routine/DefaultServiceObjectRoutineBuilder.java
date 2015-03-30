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

import com.gh.bmd.jrt.android.builder.ServiceObjectRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.AndroidSingleCallInvocation;
import com.gh.bmd.jrt.android.routine.JRoutine.ObjectFactory;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.ParamMode;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
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
 * TODO
 * <p/>
 * Created by davide on 3/29/15.
 */
class DefaultServiceObjectRoutineBuilder implements ServiceObjectRoutineBuilder {

    private final Context mContext;

    private final Class<? extends ObjectFactory> mFactoryClass;

    private Object[] mArgs;

    private RoutineConfiguration mConfiguration;

    private Class<? extends Log> mLogClass;

    private Looper mLooper;

    private Class<? extends Runner> mRunnerClass;

    private Class<? extends RoutineService> mServiceClass;

    private String mShareGroup;

    @SuppressWarnings("ConstantConditions")
    public DefaultServiceObjectRoutineBuilder(@Nonnull final Context context,
            @Nonnull final Class<? extends ObjectFactory> factoryClass) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (factoryClass == null) {

            throw new NullPointerException("the factory class must not be null");
        }

        mContext = context;
        mFactoryClass = factoryClass;
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

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        final ClassToken<BoundMethodInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<BoundMethodInvocation<INPUT, OUTPUT>>() {};
        final Class<? extends ObjectFactory> factoryClass = mFactoryClass;
        final Object[] args = (mArgs != null) ? mArgs : NO_ARGS;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(findConstructor(factoryClass, args), args, mShareGroup, name)
                       .withConfiguration(mConfiguration)
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        final ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>>() {};
        final Class<? extends ObjectFactory> factoryClass = mFactoryClass;
        final Object[] args = (mArgs != null) ? mArgs : NO_ARGS;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(findConstructor(factoryClass, args), args, mShareGroup, name,
                                 parameterTypes)
                       .withConfiguration(mConfiguration)
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
                                                    new ProxyInvocationHandler(this, itf));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public ServiceObjectRoutineBuilder dispatchingOn(@Nullable final Looper looper) {

        mLooper = looper;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withLogClass(@Nullable final Class<? extends Log> logClass) {

        mLogClass = logClass;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withRunnerClass(
            @Nullable final Class<? extends Runner> runnerClass) {

        mRunnerClass = runnerClass;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withServiceClass(
            @Nullable final Class<? extends RoutineService> serviceClass) {

        mServiceClass = serviceClass;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withArgs(@Nullable final Object... args) {

        mArgs = (args != null) ? args.clone() : null;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ServiceObjectRoutineBuilder withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    private static class BoundMethodInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBoundName;

        private final Constructor<? extends ObjectFactory> mFactoryConstructor;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        public BoundMethodInvocation(
                @Nonnull final Constructor<? extends ObjectFactory> constructor,
                @Nonnull final Object[] args, @Nullable final String group,
                @Nonnull final String name) {

            mFactoryConstructor = constructor;
            mArgs = args;
            mShareGroup = group;
            mBoundName = name;
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

                mRoutine = JRoutine.on(mFactoryConstructor.newInstance(mArgs).newObject(context))
                                   .withShareGroup(mShareGroup)
                                   .boundMethod(mBoundName);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final Constructor<? extends ObjectFactory> mFactoryConstructor;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        public MethodSignatureInvocation(
                @Nonnull final Constructor<? extends ObjectFactory> constructor,
                @Nonnull final Object[] args, @Nullable final String group,
                @Nonnull final String name, @Nonnull final Class<?>[] parameterTypes) {

            mFactoryConstructor = constructor;
            mArgs = args;
            mShareGroup = group;
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

                mRoutine = JRoutine.on(mFactoryConstructor.newInstance(mArgs).newObject(context))
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

    private static class ProxyInvocation extends AndroidSingleCallInvocation<Object, Object> {

        private final Object[] mArgs;

        private final Constructor<? extends ObjectFactory> mFactoryConstructor;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final Class<?> mProxyClass;

        private final String mShareGroup;

        private Object mProxy;

        public ProxyInvocation(@Nonnull final Constructor<? extends ObjectFactory> constructor,
                @Nonnull final Object[] args, @Nonnull final Class<?> proxyClass,
                @Nullable final String group, @Nonnull final String name,
                @Nonnull final Class<?>[] parameterTypes) {

            mFactoryConstructor = constructor;
            mArgs = args;
            mProxyClass = proxyClass;
            mShareGroup = group;
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

                mProxy = JRoutine.on(mFactoryConstructor.newInstance(mArgs).newObject(context))
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
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final Object[] mArgs;

        private final RoutineConfiguration mConfiguration;

        private final Context mContext;

        private final Constructor<? extends ObjectFactory> mFactoryConstructor;

        private final Class<? extends Log> mLogClass;

        private final Looper mLooper;

        private final Class<?> mProxyClass;

        private final Class<? extends Runner> mRunnerClass;

        private final Class<? extends RoutineService> mServiceClass;

        private final String mShareGroup;

        private ProxyInvocationHandler(@Nonnull final DefaultServiceObjectRoutineBuilder builder,
                @Nonnull final Class<?> proxyClass) {

            final Object[] args = builder.mArgs;

            mContext = builder.mContext;
            mFactoryConstructor =
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
                            .withArgs(mFactoryConstructor, mArgs, mProxyClass, mShareGroup,
                                      method.getName(), method.getParameterTypes())
                            .withConfiguration(mConfiguration)
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
