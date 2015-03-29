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
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runner;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * TODO
 * <p/>
 * Created by davide on 3/29/15.
 */
class DefaultServiceObjectRoutineBuilder implements ServiceObjectRoutineBuilder {

    private final Context mContext;

    private final Class<? extends ObjectFactory> mFactoryClass;

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

        findConstructor(factoryClass);

        mContext = context;
        mFactoryClass = factoryClass;
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        final ClassToken<BoundMethodInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<BoundMethodInvocation<INPUT, OUTPUT>>() {};
        return JRoutine.onService(mContext, classToken)
                       .withArgs(mFactoryClass, mShareGroup, name)
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
        return JRoutine.onService(mContext, classToken)
                       .withArgs(mFactoryClass, mShareGroup, name, parameterTypes)
                       .withConfiguration(mConfiguration)
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        final ClassToken<MethodInvocation<INPUT, OUTPUT>> classToken =
                new ClassToken<MethodInvocation<INPUT, OUTPUT>>() {};
        return JRoutine.onService(mContext, classToken)
                       .withArgs(mFactoryClass, mShareGroup, method)
                       .withConfiguration(mConfiguration)
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        return null;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return null;
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

        private final String mBoundName;

        private final Class<? extends ObjectFactory> mFactoryClass;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        public BoundMethodInvocation(@Nonnull final Class<? extends ObjectFactory> factoryClass,
                @Nullable final String group, @Nonnull final String name) {

            mFactoryClass = factoryClass;
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

                mRoutine = JRoutine.on(mFactoryClass.newInstance().newObject(context))
                                   .withShareGroup(mShareGroup)
                                   .boundMethod(mBoundName);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    private static class MethodInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Class<? extends ObjectFactory> mFactoryClass;

        private final Method mMethod;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        public MethodInvocation(@Nonnull final Class<? extends ObjectFactory> factoryClass,
                @Nullable final String group, @Nonnull final Method method) {

            mFactoryClass = factoryClass;
            mShareGroup = group;
            mMethod = method;
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

                mRoutine = JRoutine.on(mFactoryClass.newInstance().newObject(context))
                                   .withShareGroup(mShareGroup)
                                   .method(mMethod);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }

    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends AndroidSingleCallInvocation<INPUT, OUTPUT> {

        private final Class<? extends ObjectFactory> mFactoryClass;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private Routine<INPUT, OUTPUT> mRoutine;

        public MethodSignatureInvocation(@Nonnull final Class<? extends ObjectFactory> factoryClass,
                @Nullable final String group, @Nonnull final String name,
                @Nonnull final Class<?>[] parameterTypes) {

            mFactoryClass = factoryClass;
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

                mRoutine = JRoutine.on(mFactoryClass.newInstance().newObject(context))
                                   .withShareGroup(mShareGroup)
                                   .method(mMethodName, mParameterTypes);

            } catch (final InstantiationException e) {

                throw new InvocationException(e);

            } catch (final IllegalAccessException e) {

                throw new InvocationException(e);
            }
        }
    }
}
