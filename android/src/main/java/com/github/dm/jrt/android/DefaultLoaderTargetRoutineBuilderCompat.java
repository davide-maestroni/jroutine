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

package com.github.dm.jrt.android;

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
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a loader target routine builder.
 * <p/>
 * Created by davide-maestroni on 03/07/2016.
 */
class DefaultLoaderTargetRoutineBuilderCompat implements LoaderTargetRoutineBuilder {

    private final LoaderContextCompat mContext;

    private final ContextInvocationTarget<?> mTarget;

    private BuilderType mBuilderType;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private final InvocationConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>() {

                @NotNull
                public DefaultLoaderTargetRoutineBuilderCompat setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    mInvocationConfiguration = configuration;
                    return DefaultLoaderTargetRoutineBuilderCompat.this;
                }
            };

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private final LoaderConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>
            mLoaderConfigurable =
            new LoaderConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>() {

                @NotNull
                public DefaultLoaderTargetRoutineBuilderCompat setConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mLoaderConfiguration = configuration;
                    return DefaultLoaderTargetRoutineBuilderCompat.this;
                }
            };

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private final ProxyConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>
            mProxyConfigurable =
            new ProxyConfiguration.Configurable<DefaultLoaderTargetRoutineBuilderCompat>() {

                @NotNull
                public DefaultLoaderTargetRoutineBuilderCompat setConfiguration(
                        @NotNull final ProxyConfiguration configuration) {

                    mProxyConfiguration = configuration;
                    return DefaultLoaderTargetRoutineBuilderCompat.this;
                }
            };

    /**
     * Constructor.
     *
     * @param context the loader context.
     * @param target  the invocation target.
     */
    DefaultLoaderTargetRoutineBuilderCompat(@NotNull final LoaderContextCompat context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("loader context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @NotNull
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
    public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name) {

        return newObjectBuilder().method(name);
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return newObjectBuilder().method(name, parameterTypes);
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        return newObjectBuilder().method(method);
    }

    @NotNull
    public Builder<? extends LoaderTargetRoutineBuilder> invocationConfiguration() {

        return new Builder<LoaderTargetRoutineBuilder>(mInvocationConfigurable,
                mInvocationConfiguration);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderTargetRoutineBuilder> proxyConfiguration() {

        return new ProxyConfiguration.Builder<LoaderTargetRoutineBuilder>(mProxyConfigurable,
                mProxyConfiguration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderTargetRoutineBuilder> loaderConfiguration() {

        return new LoaderConfiguration.Builder<LoaderTargetRoutineBuilder>(mLoaderConfigurable,
                mLoaderConfiguration);
    }

    @NotNull
    public LoaderTargetRoutineBuilder withType(@Nullable final BuilderType builderType) {

        mBuilderType = builderType;
        return this;
    }

    @NotNull
    private LoaderObjectRoutineBuilder newObjectBuilder() {

        return JRoutineLoaderObjectCompat.with(mContext)
                                         .on(mTarget)
                                         .invocationConfiguration()
                                         .with(mInvocationConfiguration)
                                         .setConfiguration()
                                         .proxyConfiguration()
                                         .with(mProxyConfiguration)
                                         .setConfiguration()
                                         .loaderConfiguration()
                                         .with(mLoaderConfiguration)
                                         .setConfiguration();
    }

    @NotNull
    private LoaderProxyRoutineBuilder newProxyBuilder() {

        return JRoutineLoaderProxyCompat.with(mContext)
                                        .on(mTarget)
                                        .invocationConfiguration()
                                        .with(mInvocationConfiguration)
                                        .setConfiguration()
                                        .proxyConfiguration()
                                        .with(mProxyConfiguration)
                                        .setConfiguration()
                                        .loaderConfiguration()
                                        .with(mLoaderConfiguration)
                                        .setConfiguration();
    }
}
