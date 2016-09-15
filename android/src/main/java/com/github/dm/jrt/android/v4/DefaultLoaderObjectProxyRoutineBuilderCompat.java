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

package com.github.dm.jrt.android.v4;

import com.github.dm.jrt.android.LoaderObjectProxyRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.object.JRoutineLoaderObjectCompat;
import com.github.dm.jrt.android.v4.proxy.JRoutineLoaderProxyCompat;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a Loader target routine builder.
 * <p>
 * Created by davide-maestroni on 03/07/2016.
 */
class DefaultLoaderObjectProxyRoutineBuilderCompat implements LoaderObjectProxyRoutineBuilder {

    private final LoaderContextCompat mContext;

    private final ContextInvocationTarget<?> mTarget;

    private BuilderType mBuilderType;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the Loader context.
     * @param target  the invocation target.
     */
    DefaultLoaderObjectProxyRoutineBuilderCompat(@NotNull final LoaderContextCompat context,
            @NotNull final ContextInvocationTarget<?> target) {
        mContext = ConstantConditions.notNull("Loader context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @NotNull
    @Override
    public LoaderObjectProxyRoutineBuilder apply(
            @NotNull final InvocationConfiguration configuration) {
        mInvocationConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public LoaderObjectProxyRoutineBuilder apply(@NotNull final ObjectConfiguration configuration) {
        mObjectConfiguration = ConstantConditions.notNull("object configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public Builder<? extends LoaderObjectProxyRoutineBuilder> applyInvocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderObjectProxyRoutineBuilder>(
                new InvocationConfiguration.Configurable<LoaderObjectProxyRoutineBuilder>() {

                    @NotNull
                    @Override
                    public LoaderObjectProxyRoutineBuilder apply(
                            @NotNull final InvocationConfiguration configuration) {
                        return DefaultLoaderObjectProxyRoutineBuilderCompat.this.apply(
                                configuration);
                    }
                }, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public ObjectConfiguration.Builder<? extends LoaderObjectProxyRoutineBuilder>
    applyObjectConfiguration() {
        return new ObjectConfiguration.Builder<LoaderObjectProxyRoutineBuilder>(
                new ObjectConfiguration.Configurable<LoaderObjectProxyRoutineBuilder>() {

                    @NotNull
                    @Override
                    public LoaderObjectProxyRoutineBuilder apply(
                            @NotNull final ObjectConfiguration configuration) {
                        return DefaultLoaderObjectProxyRoutineBuilderCompat.this.apply(
                                configuration);
                    }
                }, mObjectConfiguration);
    }

    @NotNull
    @Override
    public LoaderObjectProxyRoutineBuilder withType(@Nullable final BuilderType builderType) {
        mBuilderType = builderType;
        return this;
    }

    @NotNull
    @Override
    public LoaderObjectProxyRoutineBuilder apply(@NotNull final LoaderConfiguration configuration) {
        mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
        return this;
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderObjectProxyRoutineBuilder>
    applyLoaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderObjectProxyRoutineBuilder>(
                new LoaderConfiguration.Configurable<LoaderObjectProxyRoutineBuilder>() {

                    @NotNull
                    @Override
                    public LoaderObjectProxyRoutineBuilder apply(
                            @NotNull final LoaderConfiguration configuration) {
                        return DefaultLoaderObjectProxyRoutineBuilderCompat.this.apply(
                                configuration);
                    }
                }, mLoaderConfiguration);
    }

    @NotNull
    @Override
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
        final BuilderType builderType = mBuilderType;
        if (builderType == null) {
            final LoaderProxyCompat proxyAnnotation = itf.getAnnotation(LoaderProxyCompat.class);
            if ((proxyAnnotation != null) && mTarget.isAssignableTo(proxyAnnotation.value())) {
                return newProxyBuilder().buildProxy(itf);
            }

            return newObjectBuilder().buildProxy(itf);

        } else if (builderType == BuilderType.PROXY) {
            return newProxyBuilder().buildProxy(itf);
        }

        return newObjectBuilder().buildProxy(itf);
    }

    @NotNull
    @Override
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {
        return buildProxy(itf.getRawClass());
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name) {
        return newObjectBuilder().method(name);
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {
        return newObjectBuilder().method(name, parameterTypes);
    }

    @NotNull
    @Override
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {
        return newObjectBuilder().method(method);
    }

    @NotNull
    private LoaderObjectRoutineBuilder newObjectBuilder() {
        return JRoutineLoaderObjectCompat.on(mContext)
                                         .with(mTarget)
                                         .apply(mInvocationConfiguration)
                                         .apply(mObjectConfiguration)
                                         .apply(mLoaderConfiguration);
    }

    @NotNull
    private LoaderProxyRoutineBuilder newProxyBuilder() {
        return JRoutineLoaderProxyCompat.on(mContext)
                                        .with(mTarget)
                                        .apply(mInvocationConfiguration)
                                        .apply(mObjectConfiguration)
                                        .apply(mLoaderConfiguration);
    }
}
