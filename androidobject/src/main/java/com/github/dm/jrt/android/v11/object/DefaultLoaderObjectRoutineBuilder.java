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

package com.github.dm.jrt.android.v11.object;

import android.content.Context;

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.builder.AndroidBuilders;
import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.object.InvocationTarget;
import com.github.dm.jrt.object.JRoutineObject;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.builder.Builders;
import com.github.dm.jrt.object.builder.Builders.MethodInfo;
import com.github.dm.jrt.object.common.Mutex;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findMethod;
import static com.github.dm.jrt.object.builder.Builders.callFromInvocation;
import static com.github.dm.jrt.object.builder.Builders.getAnnotatedMethod;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 04/06/2015.
 */
class DefaultLoaderObjectRoutineBuilder implements LoaderObjectRoutineBuilder,
        LoaderConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        InvocationConfiguration.Configurable<LoaderObjectRoutineBuilder> {

    private final LoaderContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the routine context.
     * @param target  the invocation target.
     */
    DefaultLoaderObjectRoutineBuilder(@NotNull final LoaderContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("loader context", context);
        mTarget = ConstantConditions.notNull("context invocation target", target);
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
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
        if (targetMethod == null) {
            return method(name, Reflection.NO_PARAMS);
        }

        final ProxyConfiguration proxyConfiguration =
                Builders.withAnnotations(mProxyConfiguration, targetMethod);
        final AliasContextInvocationFactory<IN, OUT> factory =
                new AliasContextInvocationFactory<IN, OUT>(targetMethod, proxyConfiguration, target,
                        name);
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, targetMethod);
        final LoaderRoutineBuilder<IN, OUT> builder = JRoutineLoader.with(mContext).on(factory);
        return builder.getInvocationConfiguration()
                      .with(invocationConfiguration)
                      .setConfiguration()
                      .getLoaderConfiguration()
                      .with(loaderConfiguration)
                      .setConfiguration()
                      .buildRoutine();
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return method(findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        final ProxyConfiguration proxyConfiguration =
                Builders.withAnnotations(mProxyConfiguration, method);
        final MethodContextInvocationFactory<IN, OUT> factory =
                new MethodContextInvocationFactory<IN, OUT>(method, proxyConfiguration, mTarget,
                        method);
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, method);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, method);
        final LoaderRoutineBuilder<IN, OUT> builder = JRoutineLoader.with(mContext).on(factory);
        return builder.getInvocationConfiguration()
                      .with(invocationConfiguration)
                      .setConfiguration()
                      .getLoaderConfiguration()
                      .with(loaderConfiguration)
                      .setConfiguration()
                      .buildRoutine();
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder>
    getInvocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> getProxyConfiguration
            () {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder>
    getLoaderConfiguration() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final ProxyConfiguration configuration) {

        mProxyConfiguration = ConstantConditions.notNull("proxy configuration", configuration);
        return this;
    }

    @NotNull
    public LoaderObjectRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    /**
     * Alias method invocation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AliasContextInvocation<IN, OUT> implements ContextInvocation<IN, OUT> {

        private final String mAliasName;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<IN, OUT> mChannel;

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

        public void onAbort(@NotNull final RoutineException reason) throws Exception {

            mChannel.abort(reason);
        }

        public void onContext(@NotNull final Context context) throws Exception {

            final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            mRoutine = JRoutineObject.on(target)
                                     .getProxyConfiguration()
                                     .with(mProxyConfiguration)
                                     .setConfiguration()
                                     .method(mAliasName);
        }

        public void onDestroy() throws Exception {

            mRoutine = null;
            mInstance = null;
        }

        public void onInitialize() throws Exception {

            mChannel = mRoutine.syncInvoke();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws
                Exception {

            mChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

            result.pass(mChannel.result());
        }

        public void onTerminate() throws Exception {

            mChannel = null;
        }
    }

    /**
     * Factory of {@link AliasContextInvocation}s.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AliasContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

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
        public ContextInvocation<IN, OUT> newInvocation() {

            return new AliasContextInvocation<IN, OUT>(mProxyConfiguration, mTarget, mName);
        }
    }

    /**
     * Generic method invocation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodContextInvocation<IN, OUT> implements ContextInvocation<IN, OUT> {

        private final Method mMethod;

        private final ProxyConfiguration mProxyConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<IN, OUT> mChannel;

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

        public void onAbort(@NotNull final RoutineException reason) throws Exception {

            mChannel.abort(reason);
        }

        public void onDestroy() throws Exception {

            mRoutine = null;
            mInstance = null;
        }

        public void onInitialize() throws Exception {

            mChannel = mRoutine.syncInvoke();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) throws
                Exception {

            mChannel.pass(input);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {

            result.pass(mChannel.result());
        }

        public void onTerminate() throws Exception {

            mChannel = null;
        }

        public void onContext(@NotNull final Context context) throws Exception {

            final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            mRoutine = JRoutineObject.on(target)
                                     .getProxyConfiguration()
                                     .with(mProxyConfiguration)
                                     .setConfiguration()
                                     .method(mMethod);
        }
    }

    /**
     * Factory of {@link MethodContextInvocation}s.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class MethodContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

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
        public ContextInvocation<IN, OUT> newInvocation() {

            return new MethodContextInvocation<IN, OUT>(mProxyConfiguration, mTarget, mMethod);
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends CallContextInvocation<Object, Object> {

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
        public void onContext(@NotNull final Context context) throws Exception {

            super.onContext(context);
            final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
            final Object mutexTarget =
                    (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
                            : target.getTarget();
            mMutex = Builders.getSharedMutex(mutexTarget,
                    mProxyConfiguration.getSharedFieldsOr(null));
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }
        }

        @Override
        public void onDestroy() throws Exception {

            mInstance = null;
        }

        @Override
        protected void onCall(@NotNull final List<?> objects,
                @NotNull final ResultChannel<Object> result) throws Exception {

            callFromInvocation(mMutex, mInstance, mTargetMethod, objects, result, mInputMode,
                    mOutputMode);
        }
    }

    /**
     * Factory of {@link ProxyInvocation}s.
     */
    private static class ProxyInvocationFactory extends ContextInvocationFactory<Object, Object> {

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
        public ContextInvocation<Object, Object> newInvocation() {

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
                    Builders.withAnnotations(mProxyConfiguration, targetMethod);
            final InvocationConfiguration invocationConfiguration =
                    Builders.withAnnotations(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    AndroidBuilders.withAnnotations(mLoaderConfiguration, method);
            final ProxyInvocationFactory factory =
                    new ProxyInvocationFactory(targetMethod, proxyConfiguration, target, inputMode,
                            outputMode);
            final LoaderRoutineBuilder<Object, Object> builder =
                    JRoutineLoader.with(mContext).on(factory);
            final LoaderRoutine<Object, Object> routine = builder.getInvocationConfiguration()
                                                                 .with(invocationConfiguration)
                                                                 .setConfiguration()
                                                                 .getLoaderConfiguration()
                                                                 .with(loaderConfiguration)
                                                                 .setConfiguration()
                                                                 .buildRoutine();
            return Builders.invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                    inputMode, outputMode);
        }
    }
}
