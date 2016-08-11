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

package com.github.dm.jrt.android.v11.stream;

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Decorator;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.AbstractStreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Default implementation of a stream loader routine builder.
 * <p>
 * Created by davide-maestroni on 07/03/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamBuilder<IN, OUT> extends AbstractStreamBuilder<IN, OUT>
        implements LoaderStreamBuilder<IN, OUT> {

    private LoaderStreamConfiguration mStreamConfiguration;

    private final InvocationConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return DefaultLoaderStreamBuilder.this.apply(
                            newConfiguration(streamConfiguration.getStreamInvocationConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final LoaderConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>
            mLoaderConfigurable =
            new LoaderConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilder<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultLoaderStreamBuilder.this.apply(mStreamConfiguration =
                            newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(),
                                    configuration));
                }
            };

    private final LoaderConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>
            mStreamLoaderConfigurable =
            new LoaderConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilder<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultLoaderStreamBuilder.this.apply(newConfiguration(configuration,
                            mStreamConfiguration.getCurrentLoaderConfiguration()));
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamBuilder<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return DefaultLoaderStreamBuilder.this.apply(newConfiguration(configuration,
                            streamConfiguration.getCurrentInvocationConfiguration(),
                            streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     */
    DefaultLoaderStreamBuilder() {
        this(new DefaultLoaderStreamConfiguration(null, LoaderConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC));
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     */
    DefaultLoaderStreamBuilder(@NotNull final LoaderStreamConfiguration streamConfiguration) {
        super(streamConfiguration);
        mStreamConfiguration = streamConfiguration;
    }

    private static void checkStatic(@NotNull final String name, @NotNull final Object obj) {
        if (!Reflection.hasStaticScope(obj)) {
            throw new IllegalArgumentException(
                    "the " + name + " instance does not have a static scope: " + obj.getClass()
                                                                                    .getName());
        }
    }

    private static void checkStatic(@NotNull final Decorator decorator,
            @NotNull final Object function) {
        if (!decorator.hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> async() {
        return (LoaderStreamBuilder<IN, OUT>) super.async();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilder<IN, AFTER>) super.flatMap(mappingFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        return (LoaderStreamBuilder<IN, OUT>) super.invocationMode(invocationMode);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilder<BEFORE, AFTER> let(
            @NotNull final Function<? super StreamBuilder<IN, OUT>, ? extends
                    StreamBuilder<BEFORE, AFTER>> liftFunction) {
        return (LoaderStreamBuilder<BEFORE, AFTER>) super.let(liftFunction);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilder<BEFORE, AFTER> letWithConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? super StreamBuilder<IN,
                    OUT>, ? extends StreamBuilder<BEFORE, AFTER>> liftFunction) {
        return (LoaderStreamBuilder<BEFORE, AFTER>) super.letWithConfig(liftFunction);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilder<BEFORE, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        return (LoaderStreamBuilder<BEFORE, AFTER>) super.lift(liftFunction);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilder<BEFORE, AFTER> liftWithConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
        return (LoaderStreamBuilder<BEFORE, AFTER>) super.liftWithConfig(liftFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilder<IN, AFTER>) super.map(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamBuilder<IN, AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamBuilder<IN, AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamBuilder<IN, AFTER>) super.map(builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> mapAccept(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (LoaderStreamBuilder<IN, AFTER>) super.mapAccept(mappingConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilder<IN, AFTER>) super.mapAll(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> mapAllAccept(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (LoaderStreamBuilder<IN, AFTER>) super.mapAllAccept(mappingConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> mapOn(@Nullable final Runner runner) {
        return (LoaderStreamBuilder<IN, OUT>) super.mapOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> parallel() {
        return (LoaderStreamBuilder<IN, OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> sequential() {
        return (LoaderStreamBuilder<IN, OUT>) super.sequential();
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> sorted() {
        return (LoaderStreamBuilder<IN, OUT>) super.sorted();
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> straight() {
        return (LoaderStreamBuilder<IN, OUT>) super.straight();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamBuilder<IN, OUT>>
    streamInvocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamBuilder<IN, OUT>>(
                mStreamInvocationConfigurable,
                mStreamConfiguration.getStreamInvocationConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> sync() {
        return (LoaderStreamBuilder<IN, OUT>) super.sync();
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> unsorted() {
        return (LoaderStreamBuilder<IN, OUT>) super.unsorted();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamBuilder<IN, OUT>>
    invocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamBuilder<IN, OUT>>(
                mInvocationConfigurable, mStreamConfiguration.getCurrentInvocationConfiguration());
    }

    @NotNull
    @Override
    protected LoaderStreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration currentConfiguration,
            @NotNull final InvocationMode invocationMode) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(loaderStreamConfiguration.getLoaderContext(),
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                loaderStreamConfiguration.getCurrentLoaderConfiguration(), streamConfiguration,
                currentConfiguration, invocationMode);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super BEFORE, ? extends AFTER> factory) {
        final LoaderStreamConfiguration loaderStreamConfiguration =
                (LoaderStreamConfiguration) streamConfiguration;
        final LoaderContext loaderContext = loaderStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            return JRoutineCore.with(factory)
                               .invocationConfiguration()
                               .with(loaderStreamConfiguration.asInvocationConfiguration())
                               .configured()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super BEFORE, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.with(factory).buildRoutine(), factory.hashCode(),
                        InvocationMode.SYNC);
        return JRoutineLoader.on(loaderContext)
                             .with(invocationFactory)
                             .invocationConfiguration()
                             .with(loaderStreamConfiguration.asInvocationConfiguration())
                             .configured()
                             .loaderConfiguration()
                             .with(loaderStreamConfiguration.asLoaderConfiguration())
                             .configured()
                             .buildRoutine();
    }

    @NotNull
    @Override
    protected LoaderStreamConfiguration resetConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(loaderStreamConfiguration.getLoaderContext(),
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                LoaderConfiguration.defaultConfiguration(), streamConfiguration,
                InvocationConfiguration.defaultConfiguration(), invocationMode);
    }

    @NotNull
    @Override
    public ContextInvocationFactory<IN, OUT> buildContextFactory() {
        final InvocationFactory<IN, OUT> factory = buildFactory();
        return factoryFrom(JRoutineCore.with(factory).buildRoutine(), factory.hashCode(),
                InvocationMode.SYNC);
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderStreamBuilder<IN, OUT>>
    loaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamBuilder<IN, OUT>>(mLoaderConfigurable,
                mStreamConfiguration.getCurrentLoaderConfiguration());
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderContext loaderContext = mStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(JRoutineLoader.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilder<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    @Override
    public LoaderStreamBuilder<IN, OUT> on(@Nullable final LoaderContext context) {
        return apply(newConfiguration(context));
    }

    @NotNull
    @Override
    public Builder<? extends LoaderStreamBuilder<IN, OUT>> streamLoaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamBuilder<IN, OUT>>(
                mStreamLoaderConfigurable, mStreamConfiguration.getStreamLoaderConfiguration());
    }

    /**
     * Applies the specified stream configuration.
     *
     * @param configuration the stream configuration.
     * @return this builder.
     */
    @NotNull
    protected LoaderStreamBuilder<IN, OUT> apply(
            @NotNull final LoaderStreamConfiguration configuration) {
        super.apply(configuration);
        mStreamConfiguration = configuration;
        return this;
    }

    @NotNull
    private <AFTER> LoaderRoutine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        return builder.invocationConfiguration()
                      .with(null)
                      .with(streamConfiguration.asInvocationConfiguration())
                      .configured()
                      .loaderConfiguration()
                      .with(null)
                      .with(streamConfiguration.asLoaderConfiguration())
                      .configured()
                      .buildRoutine();
    }

    @NotNull
    private LoaderStreamConfiguration newConfiguration(@Nullable final LoaderContext context) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(context,
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                loaderStreamConfiguration.getCurrentLoaderConfiguration(),
                loaderStreamConfiguration.getStreamInvocationConfiguration(),
                loaderStreamConfiguration.getCurrentInvocationConfiguration(),
                loaderStreamConfiguration.getInvocationMode());
    }

    @NotNull
    private LoaderStreamConfiguration newConfiguration(
            @NotNull final LoaderConfiguration streamConfiguration,
            @NotNull final LoaderConfiguration configuration) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(loaderStreamConfiguration.getLoaderContext(),
                streamConfiguration, configuration,
                loaderStreamConfiguration.getStreamInvocationConfiguration(),
                loaderStreamConfiguration.getCurrentInvocationConfiguration(),
                loaderStreamConfiguration.getInvocationMode());
    }

    /**
     * Default implementation of a loader stream configuration.
     */
    private static class DefaultLoaderStreamConfiguration implements LoaderStreamConfiguration {

        private final InvocationConfiguration mCurrentConfiguration;

        private final LoaderConfiguration mCurrentLoaderConfiguration;

        private final InvocationMode mInvocationMode;

        private final LoaderContext mLoaderContext;

        private final InvocationConfiguration mStreamConfiguration;

        private final LoaderConfiguration mStreamLoaderConfiguration;

        private volatile ChannelConfiguration mChannelConfiguration;

        private volatile InvocationConfiguration mInvocationConfiguration;

        private volatile LoaderConfiguration mLoaderConfiguration;

        /**
         * Constructor.
         *
         * @param context                    the loader context.
         * @param streamLoaderConfiguration  the stream loader configuration.
         * @param currentLoaderConfiguration the current loader configuration.
         * @param streamConfiguration        the stream invocation configuration.
         * @param currentConfiguration       the current invocation configuration.
         * @param invocationMode             the invocation mode.
         */
        private DefaultLoaderStreamConfiguration(@Nullable final LoaderContext context,
                @NotNull final LoaderConfiguration streamLoaderConfiguration,
                @NotNull final LoaderConfiguration currentLoaderConfiguration,
                @NotNull final InvocationConfiguration streamConfiguration,
                @NotNull final InvocationConfiguration currentConfiguration,
                @NotNull final InvocationMode invocationMode) {
            mLoaderContext = context;
            mStreamLoaderConfiguration = ConstantConditions.notNull("stream loader configuration",
                    streamLoaderConfiguration);
            mCurrentLoaderConfiguration = ConstantConditions.notNull("current loader configuration",
                    currentLoaderConfiguration);
            mStreamConfiguration = ConstantConditions.notNull("stream invocation configuration",
                    streamConfiguration);
            mCurrentConfiguration = ConstantConditions.notNull("current invocation configuration",
                    currentConfiguration);
            mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        }

        @NotNull
        public ChannelConfiguration asChannelConfiguration() {
            if (mChannelConfiguration == null) {
                mChannelConfiguration = asInvocationConfiguration().outputConfigurationBuilder()
                                                                   .buildConfiguration();
            }

            return mChannelConfiguration;
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {
            if (mInvocationConfiguration == null) {
                mInvocationConfiguration =
                        mStreamConfiguration.builderFrom().with(mCurrentConfiguration).configured();
            }

            return mInvocationConfiguration;
        }

        @NotNull
        @Override
        public InvocationConfiguration getCurrentInvocationConfiguration() {
            return mCurrentConfiguration;
        }

        @NotNull
        @Override
        public InvocationMode getInvocationMode() {
            return mInvocationMode;
        }

        @NotNull
        @Override
        public InvocationConfiguration getStreamInvocationConfiguration() {
            return mStreamConfiguration;
        }

        @NotNull
        @Override
        public LoaderConfiguration asLoaderConfiguration() {
            if (mLoaderConfiguration == null) {
                mLoaderConfiguration = mStreamLoaderConfiguration.builderFrom()
                                                                 .with(mCurrentLoaderConfiguration)
                                                                 .configured();
            }

            return mLoaderConfiguration;
        }

        @NotNull
        @Override
        public LoaderConfiguration getCurrentLoaderConfiguration() {
            return mCurrentLoaderConfiguration;
        }

        @Nullable
        @Override
        public LoaderContext getLoaderContext() {
            return mLoaderContext;
        }

        @NotNull
        @Override
        public LoaderConfiguration getStreamLoaderConfiguration() {
            return mStreamLoaderConfiguration;
        }
    }
}
