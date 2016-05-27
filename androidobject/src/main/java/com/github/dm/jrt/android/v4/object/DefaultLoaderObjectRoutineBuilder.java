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

package com.github.dm.jrt.android.v4.object;

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
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
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
import com.github.dm.jrt.object.config.ObjectConfiguration;

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
        ObjectConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        InvocationConfiguration.Configurable<LoaderObjectRoutineBuilder> {

    private final LoaderContextCompat mContext;

    private final ContextInvocationTarget<?> mTarget;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the routine context.
     * @param target  the invocation target.
     */
    DefaultLoaderObjectRoutineBuilder(@NotNull final LoaderContextCompat context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("loader context", context);
        mTarget = ConstantConditions.notNull("context invocation target", target);
    }

    @NotNull
    @Override
    public LoaderObjectRoutineBuilder apply(@NotNull final LoaderConfiguration configuration) {

        mLoaderConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }

    @Override
    @NotNull
    public LoaderObjectRoutineBuilder apply(@NotNull final ObjectConfiguration configuration) {

        mObjectConfiguration = ConstantConditions.notNull("object configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public LoaderObjectRoutineBuilder apply(@NotNull final InvocationConfiguration configuration) {

        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    @Override
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
    @Override
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name) {

        final ContextInvocationTarget<?> target = mTarget;
        final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
        if (targetMethod == null) {
            return method(name, Reflection.NO_PARAMS);
        }

        final ObjectConfiguration objectConfiguration =
                Builders.withAnnotations(mObjectConfiguration, targetMethod);
        final AliasContextInvocationFactory<IN, OUT> factory =
                new AliasContextInvocationFactory<IN, OUT>(targetMethod, objectConfiguration,
                        target, name);
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, targetMethod);
        final LoaderRoutineBuilder<IN, OUT> builder =
                JRoutineLoaderCompat.with(mContext).on(factory);
        return builder.invocationConfiguration()
                      .with(invocationConfiguration)
                      .apply()
                      .loaderConfiguration()
                      .with(loaderConfiguration)
                      .apply()
                      .buildRoutine();
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return method(findMethod(mTarget.getTargetClass(), name, parameterTypes));
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        final ObjectConfiguration objectConfiguration =
                Builders.withAnnotations(mObjectConfiguration, method);
        final MethodContextInvocationFactory<IN, OUT> factory =
                new MethodContextInvocationFactory<IN, OUT>(method, objectConfiguration, mTarget,
                        method);
        final InvocationConfiguration invocationConfiguration =
                Builders.withAnnotations(mInvocationConfiguration, method);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.withAnnotations(mLoaderConfiguration, method);
        final LoaderRoutineBuilder<IN, OUT> builder =
                JRoutineLoaderCompat.with(mContext).on(factory);
        return builder.invocationConfiguration()
                      .with(invocationConfiguration)
                      .apply()
                      .loaderConfiguration()
                      .with(loaderConfiguration)
                      .apply()
                      .buildRoutine();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder>
    invocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    @Override
    public ObjectConfiguration.Builder<? extends LoaderObjectRoutineBuilder> objectConfiguration() {

        final ObjectConfiguration config = mObjectConfiguration;
        return new ObjectConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder> loaderConfiguration() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    /**
     * Alias method invocation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AliasContextInvocation<IN, OUT> implements ContextInvocation<IN, OUT> {

        private final String mAliasName;

        private final ObjectConfiguration mObjectConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<IN, OUT> mChannel;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine = null;

        /**
         * Constructor.
         *
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param name                the alias name.
         */
        private AliasContextInvocation(@NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mAliasName = name;
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) {

            mChannel.abort(reason);
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            mRoutine = JRoutineObject.on(target)
                                     .objectConfiguration()
                                     .with(mObjectConfiguration)
                                     .apply()
                                     .method(mAliasName);
        }

        @Override
        public void onDestroy() {

            mRoutine = null;
            mInstance = null;
        }

        @Override
        public void onInitialize() {

            mChannel = mRoutine.syncInvoke();
        }

        @Override
        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            mChannel.pass(input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mChannel.result());
        }

        @Override
        public void onTerminate() {

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

        private final ObjectConfiguration mObjectConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param targetMethod        the target method.
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param name                the alias name.
         */
        private AliasContextInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {

            super(asArgs(targetMethod, objectConfiguration, target, name));
            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mName = name;
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() {

            return new AliasContextInvocation<IN, OUT>(mObjectConfiguration, mTarget, mName);
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

        private final ObjectConfiguration mObjectConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        private InvocationChannel<IN, OUT> mChannel;

        private Object mInstance;

        private Routine<IN, OUT> mRoutine = null;

        /**
         * Constructor.
         *
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param method              the method.
         */
        private MethodContextInvocation(@NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {

            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mMethod = method;
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) {

            mChannel.abort(reason);
        }

        @Override
        public void onDestroy() {

            mRoutine = null;
            mInstance = null;
        }

        @Override
        public void onInitialize() {

            mChannel = mRoutine.syncInvoke();
        }

        @Override
        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            mChannel.pass(input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mChannel.result());
        }

        @Override
        public void onTerminate() {

            mChannel = null;
        }

        @Override
        public void onContext(@NotNull final Context context) throws Exception {

            final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }

            mRoutine = JRoutineObject.on(target)
                                     .objectConfiguration()
                                     .with(mObjectConfiguration)
                                     .apply()
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

        private final ObjectConfiguration mObjectConfiguration;

        private final ContextInvocationTarget<?> mTarget;

        /**
         * Constructor.
         *
         * @param targetMethod        the target method.
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param method              the method.
         */
        private MethodContextInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {

            super(asArgs(targetMethod, objectConfiguration, target, method));
            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mMethod = method;
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() {

            return new MethodContextInvocation<IN, OUT>(mObjectConfiguration, mTarget, mMethod);
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends CallContextInvocation<Object, Object> {

        private final InputMode mInputMode;

        private final ObjectConfiguration mObjectConfiguration;

        private final OutputMode mOutputMode;

        private final ContextInvocationTarget<?> mTarget;

        private final Method mTargetMethod;

        private Object mInstance;

        private Mutex mMutex = Mutex.NO_MUTEX;

        /**
         * Constructor.
         *
         * @param targetMethod        the target method.
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param inputMode           the input transfer mode.
         * @param outputMode          the output transfer mode.
         */
        private ProxyInvocation(@NotNull final Method targetMethod,
                @NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mTargetMethod = targetMethod;
            mObjectConfiguration = objectConfiguration;
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
                    mObjectConfiguration.getSharedFieldsOrElse(null));
            mInstance = target.getTarget();
            final Object targetInstance = mInstance;
            if (targetInstance == null) {
                throw new IllegalStateException("the target object has been destroyed");
            }
        }

        @Override
        public void onDestroy() {

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

        private final ObjectConfiguration mObjectConfiguration;

        private final OutputMode mOutputMode;

        private final ContextInvocationTarget<?> mTarget;

        private final Method mTargetMethod;

        /**
         * Constructor.
         *
         * @param targetMethod        the target method.
         * @param objectConfiguration the object configuration.
         * @param target              the invocation target.
         * @param inputMode           the input transfer mode.
         * @param outputMode          the output transfer mode.
         */
        private ProxyInvocationFactory(@NotNull final Method targetMethod,
                @NotNull final ObjectConfiguration objectConfiguration,
                @NotNull final ContextInvocationTarget<?> target,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            super(asArgs(targetMethod, objectConfiguration, target, inputMode, outputMode));
            mTargetMethod = targetMethod;
            mObjectConfiguration = objectConfiguration;
            mTarget = target;
            mInputMode = inputMode;
            mOutputMode = outputMode;
        }

        @NotNull
        @Override
        public ContextInvocation<Object, Object> newInvocation() {

            return new ProxyInvocation(mTargetMethod, mObjectConfiguration, mTarget, mInputMode,
                    mOutputMode);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final LoaderContextCompat mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final LoaderConfiguration mLoaderConfiguration;

        private final ObjectConfiguration mObjectConfiguration;

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
            mObjectConfiguration = builder.mObjectConfiguration;
            mLoaderConfiguration = builder.mLoaderConfiguration;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final ContextInvocationTarget<?> target = mTarget;
            final MethodInfo methodInfo =
                    Builders.getTargetMethodInfo(target.getTargetClass(), method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final ObjectConfiguration objectConfiguration =
                    Builders.withAnnotations(mObjectConfiguration, targetMethod);
            final InvocationConfiguration invocationConfiguration =
                    Builders.withAnnotations(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    AndroidBuilders.withAnnotations(mLoaderConfiguration, method);
            final ProxyInvocationFactory factory =
                    new ProxyInvocationFactory(targetMethod, objectConfiguration, target, inputMode,
                            outputMode);
            final LoaderRoutineBuilder<Object, Object> builder =
                    JRoutineLoaderCompat.with(mContext).on(factory);
            final LoaderRoutine<Object, Object> routine = builder.invocationConfiguration()
                                                                 .with(invocationConfiguration)
                                                                 .apply()
                                                                 .loaderConfiguration()
                                                                 .with(loaderConfiguration)
                                                                 .apply()
                                                                 .buildRoutine();
            return Builders.invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode,
                    inputMode, outputMode);
        }
    }
}
