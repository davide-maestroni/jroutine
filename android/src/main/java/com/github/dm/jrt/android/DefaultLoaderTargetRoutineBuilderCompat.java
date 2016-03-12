package com.github.dm.jrt.android;

import com.github.dm.jrt.TargetRoutineBuilder.BuilderType;
import com.github.dm.jrt.android.core.builder.LoaderConfiguration;
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
import com.github.dm.jrt.object.builder.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
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
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderTargetRoutineBuilderCompat(@NotNull final LoaderContextCompat context,
            @NotNull final ContextInvocationTarget<?> target) {

        if (context == null) {
            throw new NullPointerException("the context must not be null");
        }

        if (target == null) {
            throw new NullPointerException("the invocation target must not be null");
        }

        mContext = context;
        mTarget = target;
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> alias(@NotNull final String name) {

        return newObjectBuilder().alias(name);
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
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {

        return newObjectBuilder().method(name, parameterTypes);
    }

    @NotNull
    public <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull final Method method) {

        return newObjectBuilder().method(method);
    }

    @NotNull
    public LoaderTargetRoutineBuilder withBuilder(@Nullable final BuilderType builderType) {

        mBuilderType = builderType;
        return this;
    }

    @NotNull
    public Builder<? extends LoaderTargetRoutineBuilder> withInvocations() {

        return new Builder<LoaderTargetRoutineBuilder>(mInvocationConfigurable,
                                                       mInvocationConfiguration);
    }

    @NotNull
    public ProxyConfiguration.Builder<? extends LoaderTargetRoutineBuilder> withProxies() {

        return new ProxyConfiguration.Builder<LoaderTargetRoutineBuilder>(mProxyConfigurable,
                                                                          mProxyConfiguration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderTargetRoutineBuilder> withLoaders() {

        return new LoaderConfiguration.Builder<LoaderTargetRoutineBuilder>(mLoaderConfigurable,
                                                                           mLoaderConfiguration);
    }

    @NotNull
    private LoaderObjectRoutineBuilder newObjectBuilder() {

        return JRoutineLoaderObjectCompat.with(mContext)
                                         .on(mTarget)
                                         .withInvocations()
                                         .with(mInvocationConfiguration)
                                         .getConfigured()
                                         .withProxies()
                                         .with(mProxyConfiguration)
                                         .getConfigured()
                                         .withLoaders()
                                         .with(mLoaderConfiguration)
                                         .getConfigured();
    }

    @NotNull
    private LoaderProxyRoutineBuilder newProxyBuilder() {

        return JRoutineLoaderProxyCompat.with(mContext)
                                        .on(mTarget)
                                        .withInvocations()
                                        .with(mInvocationConfiguration)
                                        .getConfigured()
                                        .withProxies()
                                        .with(mProxyConfiguration)
                                        .getConfigured()
                                        .withLoaders()
                                        .with(mLoaderConfiguration)
                                        .getConfigured();
    }
}
