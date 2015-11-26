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
package com.github.dm.jrt.android.v11.core;

import android.content.Context;

import com.github.dm.jrt.android.annotation.CacheStrategy;
import com.github.dm.jrt.android.annotation.ClashResolution;
import com.github.dm.jrt.android.annotation.InputClashResolution;
import com.github.dm.jrt.android.annotation.LoaderId;
import com.github.dm.jrt.android.annotation.ResultStaleTime;
import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.InvocationTarget;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.core.RoutineBuilders.MethodInfo;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Mutex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

import static com.github.dm.jrt.core.RoutineBuilders.callFromInvocation;
import static com.github.dm.jrt.core.RoutineBuilders.configurationWithAnnotations;
import static com.github.dm.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.github.dm.jrt.core.RoutineBuilders.getSharedMutex;
import static com.github.dm.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.github.dm.jrt.core.RoutineBuilders.invokeRoutine;
import static com.github.dm.jrt.util.Reflection.asArgs;
import static com.github.dm.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p/>
 * Created by davide-maestroni on 04/06/2015.
 */
class DefaultLoaderObjectRoutineBuilder implements LoaderObjectRoutineBuilder,
        LoaderConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        InvocationConfiguration.Configurable<LoaderObjectRoutineBuilder> {

    private final LoaderContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the routine context.
     * @param target  the invocation target.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderObjectRoutineBuilder(@NotNull final LoaderContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (target == null) {

            throw new NullPointerException("the invocation target must not be null");
        }

        mContext = context;
        mTarget = target;
    }

    @NotNull
    private static LoaderConfiguration loaderConfigurationWithAnnotations(
            @NotNull final LoaderConfiguration configuration, @NotNull final Method method) {

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

        final ResultStaleTime staleTimeAnnotation = method.getAnnotation(ResultStaleTime.class);

        if (staleTimeAnnotation != null) {

            builder.withResultStaleTime(staleTimeAnnotation.value(), staleTimeAnnotation.unit());
        }

        return builder.set();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <IN, OUT> LoaderRoutine<IN, OUT> aliasMethod(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final ProxyConfiguration proxyConfiguration =
                configurationWithAnnotations(mProxyConfiguration, targetMethod);
        final AliasContextInvocationFactory<IN, OUT> factory =
                new AliasContextInvocationFactory<IN, OUT>(targetMethod, proxyConfiguration, target,
                                                           name);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                loaderConfigurationWithAnnotations(mLoaderConfiguration, targetMethod);
        final DefaultLoaderRoutineBuilder<IN, OUT> builder =
                new DefaultLoaderRoutineBuilder<IN, OUT>(mContext, factory);
        return builder.invocations()
                      .with(invocationConfiguration)
                      .set()
                      .loaders()
                      .with(loaderConfiguration)
                      .set()
                      .buildRoutine();
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return method(findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        final ProxyConfiguration proxyConfiguration =
                configurationWithAnnotations(mProxyConfiguration, method);
        final MethodContextInvocationFactory<IN, OUT> factory =
                new MethodContextInvocationFactory<IN, OUT>(method, proxyConfiguration, mTarget,
                                                            method);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, method);
        final LoaderConfiguration loaderConfiguration =
                loaderConfigurationWithAnnotations(mLoaderConfiguration, method);
        final DefaultLoaderRoutineBuilder<IN, OUT> builder =
                new DefaultLoaderRoutineBuilder<IN, OUT>(mContext, factory);
        return builder.invocations()
                      .with(invocationConfiguration)
                      .set()
                      .loaders()
                      .with(loaderConfiguration)
                      .set()
                      .buildRoutine();
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder> invocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> proxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    /**
     * Alias method invocation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AliasContextInvocation<IN, OUT>
            extends FunctionContextInvocation<IN, OUT> {

        private final String mAliasName;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine = null;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param name               the alias name.
         */
        @SuppressWarnings("unchecked")
        private AliasContextInvocation(@NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mAliasName = name;
        }

        @Override
        public void onContext(@NotNull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutine.on(target)
                                   .proxies()
                                   .with(mProxyConfiguration)
                                   .set()
                                   .aliasMethod(mAliasName);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }

        @Override
        protected void onCall(@NotNull final List<? extends IN> inputs,
                @NotNull final ResultChannel<OUT> result) {

            final Routine<IN, OUT> routine = mRoutine;

            if ((routine == null) || (mInstance == null)) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }
    }

    /**
     * Factory of {@link AliasContextInvocation AliasContextInvocation}s.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AliasContextInvocationFactory<IN, OUT>
            extends FunctionContextInvocationFactory<IN, OUT> {

        private final String mName;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param targetMethod       the target method.
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param name               the alias name.
         */
        private AliasContextInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            super(asArgs(targetMethod, proxyConfiguration, target, name));
            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mName = name;
        }

        @NotNull
        public FunctionContextInvocation<IN, OUT> newInvocation() {

            return new AliasContextInvocation<IN, OUT>(mProxyConfiguration, mTarget, mName);
        }
    }

    /**
     * Generic method invocation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodContextInvocation<IN, OUT>
            extends FunctionContextInvocation<IN, OUT> {

        private final Method mMethod;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine = null;

        /**
         * Constructor.
         *
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method.
         */
        @SuppressWarnings("unchecked")
        private MethodContextInvocation(@NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {

            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mMethod = method;
        }

        @Override
        protected void onCall(@NotNull final List<? extends IN> inputs,
                @NotNull final ResultChannel<OUT> result) {

            final Routine<IN, OUT> routine = mRoutine;

            if ((routine == null) || (mInstance == null)) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(routine.syncCall(inputs));
        }

        @Override
        public void onContext(@NotNull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutine.on(target)
                                   .proxies()
                                   .with(mProxyConfiguration)
                                   .set()
                                   .method(mMethod);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Factory of {@link MethodContextInvocation MethodContextInvocation}s.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodContextInvocationFactory<IN, OUT>
            extends FunctionContextInvocationFactory<IN, OUT> {

        private final Method mMethod;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param targetMethod       the target method.
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param method             the method.
         */
        private MethodContextInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {

            super(asArgs(targetMethod, proxyConfiguration, target, method));
            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mMethod = method;
        }

        @NotNull
        public FunctionContextInvocation<IN, OUT> newInvocation() {

            return new MethodContextInvocation<IN, OUT>(mProxyConfiguration, mTarget, mMethod);
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends FunctionContextInvocation<Object, Object> {

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private final Method mTargetMethod;

        private Object mInstance;

        private Mutex mMutex = Mutex.NO_MUTEX;

        /**
         * Constructor.
         *
         * @param targetMethod       the target method.
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private ProxyInvocation(@NotNull final Method targetMethod,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mTargetMethod = targetMethod;
            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @Override
        protected void onCall(@NotNull final List<?> objects,
                @NotNull final ResultChannel<Object> result) {

            final Object targetInstance = mInstance;

            if (targetInstance == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            callFromInvocation(mMutex, targetInstance, mTargetMethod, objects, result, mInputMode,
                               mOutputMode);
        }

        @Override
        public void onContext(@NotNull final Context context) {

            super.onContext(context);

            try {

                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                final Object mutexTarget =
                        (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                                : target.getTarget();
                mMutex = getSharedMutex(mutexTarget, mProxyConfiguration.getSharedFieldsOr(null));
                mInstance = target.getTarget();

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Factory of {@link ProxyInvocation ProxyInvocation}s.
     */
    private static class ProxyInvocationFactory
            extends FunctionContextInvocationFactory<Object, Object> {

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private final Method mTargetMethod;

        /**
         * Constructor.
         *
         * @param targetMethod       the target method.
         * @param proxyConfiguration the proxy configuration.
         * @param target             the invocation target.
         * @param inputMode          the input transfer mode.
         * @param outputMode         the output transfer mode.
         */
        private ProxyInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            super(asArgs(targetMethod, proxyConfiguration, target, inputMode, outputMode));
            mTargetMethod = targetMethod;
            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @NotNull
        public FunctionContextInvocation<Object, Object> newInvocation() {

            return new ProxyInvocation(mTargetMethod, mProxyConfiguration, mTarget, mInputMode,
                                       mOutputMode);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final LoaderContext mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final LoaderConfiguration mLoaderConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@NotNull final DefaultLoaderObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTarget = builder.mTarget;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mLoaderConfiguration = builder.mLoaderConfiguration;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final ContextInvocationTarget<?> target = mTarget;
            final MethodInfo methodInfo = getTargetMethodInfo(target.getTargetClass(), method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final ProxyConfiguration proxyConfiguration =
                    configurationWithAnnotations(mProxyConfiguration, targetMethod);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithAnnotations(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    loaderConfigurationWithAnnotations(mLoaderConfiguration, method);
            final ProxyInvocationFactory factory =
                    new ProxyInvocationFactory(targetMethod, proxyConfiguration, target, inputMode,
                                               outputMode);
            final DefaultLoaderRoutineBuilder<Object, Object> builder =
                    new DefaultLoaderRoutineBuilder<Object, Object>(mContext, factory);
            final LoaderRoutine<Object, Object> routine = builder.invocations()
                                                                 .with(invocationConfiguration)
                                                                 .set()
                                                                 .loaders()
                                                                 .with(loaderConfiguration)
                                                                 .set()
                                                                 .buildRoutine();
            return invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                                 inputMode, outputMode);
        }
    }
}
