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

package com.github.dm.jrt.android.v11;

import com.github.dm.jrt.android.LoaderAutoProxyRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxy;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.object.JRoutineLoaderObject;
import com.github.dm.jrt.android.v11.proxy.JRoutineLoaderProxy;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a loader target routine builder.
 * <p>
 * Created by davide-maestroni on 03/07/2016.
 */
class DefaultLoaderAutoProxyRoutineBuilder implements LoaderAutoProxyRoutineBuilder {

    private final LoaderContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private BuilderType mBuilderType;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private final InvocationConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>() {

                @NotNull
                public DefaultLoaderAutoProxyRoutineBuilder apply(
                        @NotNull final InvocationConfiguration configuration) {
                    mInvocationConfiguration = configuration;
                    return DefaultLoaderAutoProxyRoutineBuilder.this;
                }
            };

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

    private final LoaderConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>
            mLoaderConfigurable =
            new LoaderConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>() {

                @NotNull
                public DefaultLoaderAutoProxyRoutineBuilder apply(
                        @NotNull final LoaderConfiguration configuration) {
                    mLoaderConfiguration = configuration;
                    return DefaultLoaderAutoProxyRoutineBuilder.this;
                }
            };

    private ObjectConfiguration mObjectConfiguration = ObjectConfiguration.defaultConfiguration();

    private final ObjectConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>
            mProxyConfigurable =
            new ObjectConfiguration.Configurable<DefaultLoaderAutoProxyRoutineBuilder>() {

                @NotNull
                public DefaultLoaderAutoProxyRoutineBuilder apply(
                        @NotNull final ObjectConfiguration configuration) {
                    mObjectConfiguration = configuration;
                    return DefaultLoaderAutoProxyRoutineBuilder.this;
                }
            };

    /**
     * Constructor.
     *
     * @param context the loader context.
     * @param target  the invocation target.
     */
    DefaultLoaderAutoProxyRoutineBuilder(@NotNull final LoaderContext context,
            @NotNull final ContextInvocationTarget<?> target) {
        mContext = ConstantConditions.notNull("loader context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @NotNull
    @Override
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
        final BuilderType builderType = mBuilderType;
        if (builderType == null) {
            final LoaderProxy proxyAnnotation = itf.getAnnotation(LoaderProxy.class);
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
    @Override
    public Builder<? extends LoaderAutoProxyRoutineBuilder> invocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderAutoProxyRoutineBuilder>(
                mInvocationConfigurable, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public ObjectConfiguration.Builder<? extends LoaderAutoProxyRoutineBuilder>
    objectConfiguration() {
        return new ObjectConfiguration.Builder<LoaderAutoProxyRoutineBuilder>(mProxyConfigurable,
                mObjectConfiguration);
    }

    @NotNull
    @Override
    public LoaderAutoProxyRoutineBuilder withType(@Nullable final BuilderType builderType) {
        mBuilderType = builderType;
        return this;
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderAutoProxyRoutineBuilder>
    loaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderAutoProxyRoutineBuilder>(mLoaderConfigurable,
                mLoaderConfiguration);
    }

    @NotNull
    private LoaderObjectRoutineBuilder newObjectBuilder() {
        return JRoutineLoaderObject.on(mContext)
                                   .with(mTarget)
                                   .invocationConfiguration()
                                   .with(mInvocationConfiguration)
                                   .configured()
                                   .objectConfiguration()
                                   .with(mObjectConfiguration)
                                   .configured()
                                   .loaderConfiguration()
                                   .with(mLoaderConfiguration)
                                   .configured();
    }

    @NotNull
    private LoaderProxyRoutineBuilder newProxyBuilder() {
        return JRoutineLoaderProxy.on(mContext)
                                  .with(mTarget)
                                  .invocationConfiguration()
                                  .with(mInvocationConfiguration)
                                  .configured()
                                  .objectConfiguration()
                                  .with(mObjectConfiguration)
                                  .configured()
                                  .loaderConfiguration()
                                  .with(mLoaderConfiguration)
                                  .configured();
    }
}
