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
package com.gh.bmd.jrt.android.v11.core;

import android.content.Context;

import com.gh.bmd.jrt.android.annotation.CacheStrategy;
import com.gh.bmd.jrt.android.annotation.ClashResolution;
import com.gh.bmd.jrt.android.annotation.InputClashResolution;
import com.gh.bmd.jrt.android.annotation.LoaderId;
import com.gh.bmd.jrt.android.annotation.StaleTime;
import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderObjectRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.AbstractContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.FunctionContextInvocation;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.RoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.MethodInvocation;
import com.gh.bmd.jrt.invocation.MethodInvocationDecorator;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.RoutineBuilders.callFromInvocation;
import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.core.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.core.RoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.util.Reflection.findConstructor;
import static com.gh.bmd.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Created by davide-maestroni on 4/6/2015.
 */
class DefaultLoaderObjectRoutineBuilder implements LoaderObjectRoutineBuilder,
        LoaderConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        InvocationConfiguration.Configurable<LoaderObjectRoutineBuilder> {

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final RoutineContext mContext;

    private final Object[] mFactoryArgs;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param targetClass the target object class.
     * @param factoryArgs the object factory arguments.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderObjectRoutineBuilder(@Nonnull final RoutineContext context,
            @Nonnull final Class<?> targetClass, @Nullable final Object[] factoryArgs) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mContext = context;
        mTargetClass = targetClass;
        mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
    }

    @Nonnull
    private static InvocationConfiguration configurationWithAnnotations(
            @Nonnull final InvocationConfiguration configuration, @Nonnull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                configuration.builderFrom();
        final Priority priorityAnnotation = method.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.withPriority(priorityAnnotation.value());
        }

        final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withExecutionTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withExecutionTimeoutAction(actionAnnotation.value());
        }

        return builder.set();
    }

    @Nonnull
    private static LoaderConfiguration configurationWithAnnotations(
            @Nonnull final LoaderConfiguration configuration, @Nonnull final Method method) {

        final LoaderConfiguration.Builder<LoaderConfiguration> builder =
                configuration.builderFrom();

        final LoaderId idAnnotation = method.getAnnotation(LoaderId.class);

        if (idAnnotation != null) {

            builder.withId(idAnnotation.value());
        }

        final ClashResolution clashAnnotation = method.getAnnotation(ClashResolution.class);

        if (clashAnnotation != null) {

            builder.withClashResolution(clashAnnotation.value());
        }

        final InputClashResolution inputClashAnnotation =
                method.getAnnotation(InputClashResolution.class);

        if (inputClashAnnotation != null) {

            builder.withInputClashResolution(inputClashAnnotation.value());
        }

        final CacheStrategy cacheAnnotation = method.getAnnotation(CacheStrategy.class);

        if (cacheAnnotation != null) {

            builder.withCacheStrategy(cacheAnnotation.value());
        }

        final StaleTime staleTimeAnnotation = method.getAnnotation(StaleTime.class);

        if (staleTimeAnnotation != null) {

            builder.withResultStaleTime(staleTimeAnnotation.value(), staleTimeAnnotation.unit());
        }

        return builder.set();
    }

    @Nonnull
    private static ProxyConfiguration configurationWithAnnotations(
            @Nonnull final ProxyConfiguration configuration, @Nonnull final Method method) {

        final ProxyConfiguration.Builder<ProxyConfiguration> builder = configuration.builderFrom();

        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            builder.withShareGroup(shareGroupAnnotation.value());
        }

        return builder.set();
    }

    @Nonnull
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Object getInstance(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass, @Nonnull final Object[] args) throws
            IllegalAccessException, InvocationTargetException, InstantiationException {

        Object target = null;

        if (context instanceof FactoryContext) {

            // the only safe way is to synchronize the factory using the very same instance
            synchronized (context) {

                target = ((FactoryContext) context).geInstance(targetClass, args);
            }
        }

        if (target == null) {

            target = findConstructor(targetClass, args).newInstance(args);

        } else if (!targetClass.isInstance(target)) {

            throw new InstantiationException();
        }

        return target;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = getAnnotatedMethod(name, targetClass);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final ProxyConfiguration proxyConfiguration =
                configurationWithAnnotations(mProxyConfiguration, targetMethod);
        final AliasContextInvocationFactory<INPUT, OUTPUT> factory =
                new AliasContextInvocationFactory<INPUT, OUTPUT>(proxyConfiguration, targetMethod,
                                                                 targetClass, mFactoryArgs, name);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                configurationWithAnnotations(mLoaderConfiguration, targetMethod);
        return JRoutine.on(mContext, factory)
                       .invocations()
                       .with(invocationConfiguration)
                       .set()
                       .loaders()
                       .with(loaderConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(findMethod(mTargetClass, name, parameterTypes));
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        final ProxyConfiguration proxyConfiguration =
                configurationWithAnnotations(mProxyConfiguration, method);
        final MethodContextInvocationFactory<INPUT, OUTPUT> factory =
                new MethodContextInvocationFactory<INPUT, OUTPUT>(proxyConfiguration, method,
                                                                  mTargetClass, mFactoryArgs,
                                                                  method);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, method);
        final LoaderConfiguration loaderConfiguration =
                configurationWithAnnotations(mLoaderConfiguration, method);
        return JRoutine.on(mContext, factory)
                       .invocations()
                       .with(invocationConfiguration)
                       .set()
                       .loaders()
                       .with(loaderConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    /**
     * Alias method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasContextInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final String mAliasName;

        private final Object[] mFactoryArgs;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine = null;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetClass        the target object class.
         * @param factoryArgs        the object factor arguments.
         * @param name               the alias name.
         */
        @SuppressWarnings("unchecked")
        public AliasContextInvocation(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Class<?> targetClass, @Nonnull final Object[] factoryArgs,
                @Nonnull final String name) {

            mProxyConfiguration = proxyConfiguration;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mAliasName = name;
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mFactoryArgs);
                mRoutine = JRoutine.on(target)
                                   .proxies()
                                   .with(mProxyConfiguration)
                                   .set()
                                   .aliasMethod(mAliasName);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }

        @Override
        protected void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mTarget == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.syncCall(inputs));
        }
    }

    /**
     * Factory of {@link AliasContextInvocation AliasContextInvocation}s.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasContextInvocationFactory<INPUT, OUTPUT>
            extends AbstractContextInvocationFactory<INPUT, OUTPUT> {

        private final Object[] mFactoryArgs;

        private final String mName;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetMethod       the target method.
         * @param targetClass        the target object class.
         * @param factoryArgs        the object factor arguments.
         * @param name               the alias name.
         */
        private AliasContextInvocationFactory(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Method targetMethod, @Nonnull final Class<?> targetClass,
                @Nonnull final Object[] factoryArgs, @Nonnull final String name) {

            super(proxyConfiguration, targetMethod, targetMethod, factoryArgs, name);
            mProxyConfiguration = proxyConfiguration;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mName = name;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation() {

            return new AliasContextInvocation<INPUT, OUTPUT>(mProxyConfiguration, mTargetClass,
                                                             mFactoryArgs, mName);
        }
    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodContextInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final Object[] mFactoryArgs;

        private final Method mMethod;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine = null;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetClass        the target object class.
         * @param factoryArgs        the object factor arguments.
         * @param method             the method.
         */
        @SuppressWarnings("unchecked")
        public MethodContextInvocation(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Class<?> targetClass, @Nonnull final Object[] factoryArgs,
                @Nonnull final Method method) {

            mProxyConfiguration = proxyConfiguration;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mMethod = method;
        }

        @Override
        protected void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mTarget == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.syncCall(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mFactoryArgs);
                mRoutine = JRoutine.on(target)
                                   .proxies()
                                   .with(mProxyConfiguration)
                                   .set()
                                   .method(mMethod);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link MethodContextInvocation MethodContextInvocation}s.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodContextInvocationFactory<INPUT, OUTPUT>
            extends AbstractContextInvocationFactory<INPUT, OUTPUT> {

        private final Object[] mFactoryArgs;

        private final Method mMethod;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetMethod       the target method.
         * @param targetClass        the target object class.
         * @param factoryArgs        the object factor arguments.
         * @param method             the method.
         */
        private MethodContextInvocationFactory(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Method targetMethod, @Nonnull final Class<?> targetClass,
                @Nonnull final Object[] factoryArgs, @Nonnull final Method method) {

            super(proxyConfiguration, targetMethod, targetClass, factoryArgs, method);
            mProxyConfiguration = proxyConfiguration;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mMethod = method;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation() {

            return new MethodContextInvocation<INPUT, OUTPUT>(mProxyConfiguration, mTargetClass,
                                                              mFactoryArgs, mMethod);
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends FunctionContextInvocation<Object, Object>
            implements MethodInvocation<Object, Object> {

        private final Object[] mArgs;

        private final MethodInvocationDecorator mDecorator;

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        private final Method mTargetMethod;

        private Object mMutex;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param args               the factory constructor arguments.
         * @param targetClass        the target object class.
         * @param targetMethod       the target method.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        public ProxyInvocation(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Object[] args, @Nonnull final Class<?> targetClass,
                @Nonnull final Method targetMethod, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            mDecorator = proxyConfiguration.getMethodDecoratorOr(
                    MethodInvocationDecorator.NO_DECORATION);
            mProxyConfiguration = proxyConfiguration;
            mArgs = args;
            mTargetClass = targetClass;
            mTargetMethod = targetMethod;
            mInputMode = inputMode;
            mOutputMode = outputMode;
            mMutex = this;
        }

        public void onInvocation(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            callFromInvocation(mTargetMethod, mMutex, mTarget, objects, result, mInputMode,
                               mOutputMode);
        }

        @Override
        protected void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            final Method method = mTargetMethod;
            mDecorator.decorate(this, method.getName(), method.getParameterTypes())
                      .onInvocation(objects, result);
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mArgs);
                final String shareGroup = mProxyConfiguration.getShareGroupOr(null);

                if (!ShareGroup.NONE.equals(shareGroup)) {

                    mMutex = getSharedMutex(target, shareGroup);
                }

                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link ProxyInvocation ProxyInvocation}s.
     */
    private static class ProxyInvocationFactory
            extends AbstractContextInvocationFactory<Object, Object> {

        private final Object[] mFactoryArgs;

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        private final Method mTargetMethod;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param targetMethod       the target method.
         * @param targetClass        the target object class.
         * @param factoryArgs        the object factor arguments.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private ProxyInvocationFactory(@Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final Method targetMethod, @Nonnull final Class<?> targetClass,
                @Nonnull final Object[] factoryArgs, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            super(proxyConfiguration, targetMethod, targetClass, factoryArgs, inputMode,
                  outputMode);
            mProxyConfiguration = proxyConfiguration;
            mTargetClass = targetClass;
            mFactoryArgs = factoryArgs;
            mTargetMethod = targetMethod;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Nonnull
        public ContextInvocation<Object, Object> newInvocation() {

            return new ProxyInvocation(mProxyConfiguration, mFactoryArgs, mTargetClass,
                                       mTargetMethod, mInputMode, mOutputMode);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final RoutineContext mContext;

        private final Object[] mFactoryArgs;

        private final InvocationConfiguration mInvocationConfiguration;

        private final LoaderConfiguration mLoaderConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultLoaderObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTargetClass = builder.mTargetClass;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mLoaderConfiguration = builder.mLoaderConfiguration;
            mFactoryArgs = builder.mFactoryArgs;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final MethodInfo methodInfo = getTargetMethodInfo(method, mTargetClass);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final ProxyConfiguration proxyConfiguration =
                    configurationWithAnnotations(mProxyConfiguration, targetMethod);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithAnnotations(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    configurationWithAnnotations(mLoaderConfiguration, method);
            final ProxyInvocationFactory factory =
                    new ProxyInvocationFactory(proxyConfiguration, targetMethod, mTargetClass,
                                               mFactoryArgs, inputMode, outputMode);
            final Routine<Object, Object> routine = JRoutine.on(mContext, factory)
                                                            .invocations()
                                                            .with(invocationConfiguration)
                                                            .set()
                                                            .loaders()
                                                            .with(loaderConfiguration)
                                                            .set()
                                                            .buildRoutine();
            return invokeRoutine(routine, method, (args == null) ? Reflection.NO_ARGS : args,
                                 inputMode, outputMode);
        }
    }

    static {

        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;
        classMap.put(byte.class.getName(), byte.class);
        classMap.put(char.class.getName(), char.class);
        classMap.put(int.class.getName(), int.class);
        classMap.put(long.class.getName(), long.class);
        classMap.put(float.class.getName(), float.class);
        classMap.put(double.class.getName(), double.class);
        classMap.put(short.class.getName(), short.class);
        classMap.put(void.class.getName(), void.class);
    }
}
