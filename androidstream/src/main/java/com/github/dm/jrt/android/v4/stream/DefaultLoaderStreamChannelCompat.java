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

package com.github.dm.jrt.android.v4.stream;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v4.channel.SparseChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat.LoaderBuilderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.TimeDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.Wrapper;
import com.github.dm.jrt.stream.AbstractStreamChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream output channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannelCompat<OUT> extends AbstractStreamChannel<OUT>
        implements LoaderStreamChannelCompat<OUT>, Configurable<LoaderStreamChannelCompat<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> applyConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannelCompat.super.invocationConfiguration()
                                                          .with(null)
                                                          .with(configuration)
                                                          .apply();
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> applyConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannelCompat.super.streamInvocationConfiguration()
                                                          .with(null)
                                                          .with(configuration)
                                                          .apply();
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

    private LoaderBuilderCompat mContextBuilder;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannelCompat<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannelCompat<OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<OUT> applyConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamChannelCompat(@Nullable final LoaderBuilderCompat builder,
            @NotNull final OutputChannel<OUT> channel) {

        this(builder, channel, (Binder) null);
    }

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param input   the channel returning the inputs.
     * @param output  the channel consuming them.
     */
    DefaultLoaderStreamChannelCompat(@Nullable final LoaderBuilderCompat builder,
            @NotNull final OutputChannel<OUT> input, @NotNull final IOChannel<OUT> output) {

        this(builder, output, Binder.binderOf(input, output));
    }

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param channel the wrapped output channel.
     * @param binder  the binder instance.
     */
    private DefaultLoaderStreamChannelCompat(@Nullable final LoaderBuilderCompat builder,
            @NotNull final OutputChannel<OUT> channel, @Nullable final Binder binder) {

        this(builder, channel, InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC, binder);
    }

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param channel                 the wrapped output channel.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param invocationMode          the delegation type.
     * @param binder                  the binder instance.
     */
    private DefaultLoaderStreamChannelCompat(@Nullable final LoaderBuilderCompat builder,
            @NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

        super(channel, invocationConfiguration, invocationMode, binder);
        mContextBuilder = builder;
        mStreamConfiguration =
                ConstantConditions.notNull("loader configuration", loaderConfiguration);
    }

    private static void checkStatic(@NotNull final Wrapper wrapper,
            @NotNull final Object function) {

        if (!wrapper.hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return (LoaderStreamChannelCompat<OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannelCompat<OUT>) super.afterMax(timeout, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return (LoaderStreamChannelCompat<OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> bind(
            @NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamChannelCompat<OUT>) super.bind(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyAbort() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyExit() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> eventuallyThrow() {

        return (LoaderStreamChannelCompat<OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> immediately() {

        return (LoaderStreamChannelCompat<OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> skip(final int count) {

        return (LoaderStreamChannelCompat<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> async() {

        return (LoaderStreamChannelCompat<OUT>) super.async();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannelCompat<OUT>) super.backPressureOn(runner, maxInputs, maxDelay,
                timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        return (LoaderStreamChannelCompat<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        checkStatic(wrap(predicate), predicate);
        return (LoaderStreamChannelCompat<OUT>) super.filter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<AFTER>) super.flatMap(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    invocationConfiguration() {

        final InvocationConfiguration config = getConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(
                mInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<AFTER>) super.map(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<AFTER>) super.map(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        if (!Reflection.hasStaticScope(factory)) {
            throw new IllegalArgumentException(
                    "the factory instance does not have a static scope: " + factory.getClass()
                                                                                   .getName());
        }

        return (LoaderStreamChannelCompat<AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannelCompat<AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> mapAll(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<AFTER>) super.mapAll(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<AFTER>) super.mapAll(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> maxParallelInvocations(final int maxInvocations) {

        return (LoaderStreamChannelCompat<OUT>) super.maxParallelInvocations(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> onError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<OUT>) super.onError(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<Void> onOutput(@NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<Void>) super.onOutput(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannelCompat<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> parallel() {

        return (LoaderStreamChannelCompat<OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<OUT>) super.collect(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<OUT>) super.reduce(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> consumer) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<AFTER>) super.collect(supplier, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<AFTER>) super.reduce(supplier, function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> repeat() {

        return (LoaderStreamChannelCompat<OUT>) super.repeat();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamChannelCompat<OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> runOnShared() {

        return (LoaderStreamChannelCompat<OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> serial() {

        return (LoaderStreamChannelCompat<OUT>) super.serial();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    streamInvocationConfiguration() {

        final InvocationConfiguration config = getStreamConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(
                mStreamInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> sync() {

        return (LoaderStreamChannelCompat<OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(@Nullable final AFTER output) {

        return (LoaderStreamChannelCompat<AFTER>) super.then(output);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(@Nullable final AFTER... outputs) {

        return (LoaderStreamChannelCompat<AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {

        return (LoaderStreamChannelCompat<AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<AFTER>) super.then(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<AFTER>) super.then(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<AFTER>) super.then(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<AFTER> then(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<AFTER>) super.then(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<? extends ParcelableSelectable<OUT>> toSelectable(
            final int index) {

        final ChannelConfiguration configuration = buildChannelConfiguration();
        final OutputChannel<? extends ParcelableSelectable<OUT>> channel =
                SparseChannelsCompat.toSelectable(this, index)
                                    .channelConfiguration()
                                    .with(configuration)
                                    .apply()
                                    .buildChannels();
        return newChannel(channel, getStreamConfiguration(), getInvocationMode(), getBinder());
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<OUT> tryFinally(@NotNull final Runnable runnable) {

        if (!Reflection.hasStaticScope(runnable)) {
            throw new IllegalArgumentException(
                    "the runnable instance does not have a static scope: " + runnable.getClass()
                                                                                     .getName());
        }

        return (LoaderStreamChannelCompat<OUT>) super.tryFinally(runnable);
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamChannelCompat<AFTER> newChannel(
            @NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

        return newChannel(channel, configuration, mStreamConfiguration, invocationMode, binder);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(configuration,
                mStreamConfiguration.builderFrom().with(mConfiguration).apply(), factory);
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> cache(@Nullable final CacheStrategyType strategyType) {

        return loaderConfiguration().withCacheStrategy(strategyType).apply();
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> factoryId(final int factoryId) {

        return loaderConfiguration().withFactoryId(factoryId).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    loaderConfiguration() {

        final LoaderConfiguration config = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(this, config);
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> loaderId(final int loaderId) {

        return loaderConfiguration().withLoaderId(loaderId).apply();
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> staleAfter(@Nullable final TimeDuration staleTime) {

        return loaderConfiguration().withResultStaleTime(staleTime).apply();
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {

        return loaderConfiguration().withResultStaleTime(time, timeUnit).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    streamLoaderConfiguration() {

        final LoaderConfiguration config = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<OUT>>(mStreamConfigurable,
                config);
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> with(@Nullable final LoaderContextCompat context) {

        mContextBuilder = (context != null) ? JRoutineLoaderCompat.with(context) : null;
        return this;
    }

    @NotNull
    private <AFTER> LoaderStreamChannelCompat<AFTER> newChannel(
            @NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

        return new DefaultLoaderStreamChannelCompat<AFTER>(mContextBuilder, channel,
                invocationConfiguration, loaderConfiguration, invocationMode, binder);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilderCompat contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            return JRoutineCore.on(factory)
                               .invocationConfiguration()
                               .with(invocationConfiguration)
                               .apply()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.on(factory).buildRoutine(), factory.hashCode(),
                        InvocationMode.SYNC);
        return contextBuilder.on(invocationFactory)
                             .invocationConfiguration()
                             .with(invocationConfiguration)
                             .apply()
                             .loaderConfiguration()
                             .with(loaderConfiguration)
                             .apply()
                             .buildRoutine();
    }

    @NotNull
    public LoaderStreamChannelCompat<OUT> applyConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }
}
