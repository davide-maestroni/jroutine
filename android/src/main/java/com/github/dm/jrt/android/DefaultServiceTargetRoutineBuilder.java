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

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.JRoutineServiceObject;
import com.github.dm.jrt.android.object.builder.ServiceObjectRoutineBuilder;
import com.github.dm.jrt.android.proxy.JRoutineServiceProxy;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Default implementation of a service target routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
class DefaultServiceTargetRoutineBuilder implements ServiceTargetRoutineBuilder {

    private final ServiceContext mContext;

    private final ContextInvocationTarget<?> mTarget;

    private BuilderType mBuilderType;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private final InvocationConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>() {

                @NotNull
                public DefaultServiceTargetRoutineBuilder setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    mInvocationConfiguration = configuration;
                    return DefaultServiceTargetRoutineBuilder.this;
                }
            };

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.defaultConfiguration();

    private final ProxyConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>
            mProxyConfigurable =
            new ProxyConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>() {

                @NotNull
                public DefaultServiceTargetRoutineBuilder setConfiguration(
                        @NotNull final ProxyConfiguration configuration) {

                    mProxyConfiguration = configuration;
                    return DefaultServiceTargetRoutineBuilder.this;
                }
            };

    private ServiceConfiguration mServiceConfiguration =
            ServiceConfiguration.defaultConfiguration();

    private final ServiceConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>
            mServiceConfigurable =
            new ServiceConfiguration.Configurable<DefaultServiceTargetRoutineBuilder>() {

                @NotNull
                public DefaultServiceTargetRoutineBuilder setConfiguration(
                        @NotNull final ServiceConfiguration configuration) {

                    mServiceConfiguration = configuration;
                    return DefaultServiceTargetRoutineBuilder.this;
                }
            };

    /**
     * Constructor.
     *
     * @param context the service context.
     * @param target  the invocation target.
     */
    DefaultServiceTargetRoutineBuilder(@NotNull final ServiceContext context,
            @NotNull final ContextInvocationTarget<?> target) {

        mContext = ConstantConditions.notNull("service context", context);
        mTarget = ConstantConditions.notNull("invocation target", target);
    }

    @NotNull
    public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {

        final BuilderType builderType = mBuilderType;
        if (builderType == null) {
            final ServiceProxy proxyAnnotation = itf.getAnnotation(ServiceProxy.class);
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
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {

        return newObjectBuilder().method(name);
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return newObjectBuilder().method(name, parameterTypes);
    }

    @NotNull
    public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {

        return newObjectBuilder().method(method);
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends ServiceTargetRoutineBuilder>
    getInvocationConfiguration() {

        return new InvocationConfiguration.Builder<DefaultServiceTargetRoutineBuilder>(
                mInvocationConfigurable, mInvocationConfiguration);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends ServiceTargetRoutineBuilder>
    getProxyConfiguration() {

        return new ProxyConfiguration.Builder<DefaultServiceTargetRoutineBuilder>(
                mProxyConfigurable, mProxyConfiguration);
    }

    @NotNull
    public ServiceTargetRoutineBuilder withType(@Nullable final BuilderType builderType) {

        mBuilderType = builderType;
        return this;
    }

    @NotNull
    public ServiceConfiguration.Builder<? extends ServiceTargetRoutineBuilder>
    getServiceConfiguration() {

        return new ServiceConfiguration.Builder<DefaultServiceTargetRoutineBuilder>(
                mServiceConfigurable, mServiceConfiguration);
    }

    @NotNull
    private ServiceObjectRoutineBuilder newObjectBuilder() {

        return JRoutineServiceObject.with(mContext)
                                    .on(mTarget)
                                    .getInvocationConfiguration()
                                    .with(mInvocationConfiguration)
                                    .setConfiguration()
                                    .getProxyConfiguration()
                                    .with(mProxyConfiguration)
                                    .setConfiguration()
                                    .getServiceConfiguration()
                                    .with(mServiceConfiguration)
                                    .setConfiguration();
    }

    @NotNull
    private ServiceProxyRoutineBuilder newProxyBuilder() {

        return JRoutineServiceProxy.with(mContext)
                                   .on(mTarget)
                                   .getInvocationConfiguration()
                                   .with(mInvocationConfiguration)
                                   .setConfiguration()
                                   .getProxyConfiguration()
                                   .with(mProxyConfiguration)
                                   .setConfiguration()
                                   .getServiceConfiguration()
                                   .with(mServiceConfiguration)
                                   .setConfiguration();
    }
}
