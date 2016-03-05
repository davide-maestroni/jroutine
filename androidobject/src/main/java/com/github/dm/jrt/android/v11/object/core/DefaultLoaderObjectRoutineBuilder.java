/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.v11.object.core;

import android.content.Context;

import com.github.dm.jrt.android.core.builder.LoaderConfiguration;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.FunctionContextInvocation;
import com.github.dm.jrt.android.core.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.object.core.AndroidBuilders;
import com.github.dm.jrt.android.object.core.ContextInvocationTarget;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.builder.InvocationConfiguration;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.object.Builders;
import com.github.dm.jrt.object.Builders.MethodInfo;
import com.github.dm.jrt.object.InvocationTarget;
import com.github.dm.jrt.object.JRoutineObject;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.builder.ProxyConfiguration;
import com.github.dm.jrt.object.common.Mutex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findMethod;

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
    public <IN, OUT> LoaderRoutine<IN, OUT> alias(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = Builders.getAnnotatedMethod(target.getTargetClass(), name);
        if (targetMethod == null) {
            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final ProxyConfiguration proxyConfiguration =
                Builders.configurationWithAnnotations(mProxyConfiguration, targetMethod);
        final AliasContextInvocationFactory<IN, OUT> factory =
                new AliasContextInvocationFactory<IN, OUT>(targetMethod, proxyConfiguration, target,
                                                           name);
        final InvocationConfiguration invocationConfiguration =
                Builders.configurationWithAnnotations(mInvocationConfiguration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.configurationWithAnnotations(mLoaderConfiguration, targetMethod);
        final LoaderRoutineBuilder<IN, OUT> builder = JRoutineLoader.with(mContext).on(factory);
        return builder.withInvocations()
                      .with(invocationConfiguration)
                      .getConfigured()
                      .withLoaders()
                      .with(loaderConfiguration)
                      .getConfigured()
                      .buildRoutine();
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
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return method(findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        final ProxyConfiguration proxyConfiguration =
                Builders.configurationWithAnnotations(mProxyConfiguration, method);
        final MethodContextInvocationFactory<IN, OUT> factory =
                new MethodContextInvocationFactory<IN, OUT>(method, proxyConfiguration, mTarget,
                                                            method);
        final InvocationConfiguration invocationConfiguration =
                Builders.configurationWithAnnotations(mInvocationConfiguration, method);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.configurationWithAnnotations(mLoaderConfiguration, method);
        final LoaderRoutineBuilder<IN, OUT> builder = JRoutineLoader.with(mContext).on(factory);
        return builder.withInvocations()
                      .with(invocationConfiguration)
                      .getConfigured()
                      .withLoaders()
                      .with(loaderConfiguration)
                      .getConfigured()
                      .buildRoutine();
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withInvocations() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withProxies() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
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

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withLoaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
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
        private AliasContextInvocation(@NotNull final ProxyConfiguration proxyConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            mProxyConfiguration = proxyConfiguration;
            mTarget = target;
            mAliasName = name;
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutineObject.on(target)
                                         .withProxies()
                                         .with(mProxyConfiguration)
                                         .getConfigured()
                                         .alias(mAliasName);

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
     * Factory of {@link AliasContextInvocation}s.
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
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                mInstance = target.getTarget();
                mRoutine = JRoutineObject.on(target)
                                         .withProxies()
                                         .with(mProxyConfiguration)
                                         .getConfigured()
                                         .method(mMethod);

            } catch (final Throwable t) {
                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Factory of {@link MethodContextInvocation}s.
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
                @NotNull final ResultChannel<Object> result) throws Exception {

            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            Builders.callFromInvocation(mMutex, targetInstance, mTargetMethod, objects, result,
                                        mInputMode, mOutputMode);
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            try {
                final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
                final Object mutexTarget =
                        (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                                : target.getTarget();
                mMutex = Builders.getSharedMutex(mutexTarget,
                                                 mProxyConfiguration.getSharedFieldsOr(null));
                mInstance = target.getTarget();

            } catch (final Throwable t) {
                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }

    /**
     * Factory of {@link ProxyInvocation}s.
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
            final MethodInfo methodInfo =
                    Builders.getTargetMethodInfo(target.getTargetClass(), method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final ProxyConfiguration proxyConfiguration =
                    Builders.configurationWithAnnotations(mProxyConfiguration, targetMethod);
            final InvocationConfiguration invocationConfiguration =
                    Builders.configurationWithAnnotations(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    AndroidBuilders.configurationWithAnnotations(mLoaderConfiguration, method);
            final ProxyInvocationFactory factory =
                    new ProxyInvocationFactory(targetMethod, proxyConfiguration, target, inputMode,
                                               outputMode);
            final LoaderRoutineBuilder<Object, Object> builder =
                    JRoutineLoader.with(mContext).on(factory);
            final LoaderRoutine<Object, Object> routine = builder.withInvocations()
                                                                 .with(invocationConfiguration)
                                                                 .getConfigured()
                                                                 .withLoaders()
                                                                 .with(loaderConfiguration)
                                                                 .getConfigured()
                                                                 .buildRoutine();
            return Builders.invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                                          inputMode, outputMode);
        }
    }
}
