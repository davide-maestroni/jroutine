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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.JRoutineLoader.LoaderBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
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

import static com.github.dm.jrt.android.core.invocation.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream output channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannel<OUT> extends AbstractStreamChannel<OUT>
        implements LoaderStreamChannel<OUT>, Configurable<LoaderStreamChannel<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannel.super.getInvocationConfiguration()
                                                    .with(null)
                                                    .with(configuration)
                                                    .setConfiguration();
                    return DefaultLoaderStreamChannel.this;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannel.super.getStreamInvocationConfiguration()
                                                    .with(null)
                                                    .with(configuration)
                                                    .setConfiguration();
                    return DefaultLoaderStreamChannel.this;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

    private LoaderBuilder mContextBuilder;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannel<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
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
    DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
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
    private DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
            @NotNull final OutputChannel<OUT> channel, @Nullable final Binder binder) {

        this(builder, channel, InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), DelegationType.ASYNC, binder);
    }

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param channel                 the wrapped output channel.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param delegationType          the delegation type.
     * @param binder                  the binder instance.
     */
    private DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
            @NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        super(channel, invocationConfiguration, delegationType, binder);
        mContextBuilder = builder;
        mStreamConfiguration =
                ConstantConditions.notNull("loader configuration", loaderConfiguration);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        return (LoaderStreamChannel<OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannel<OUT>) super.afterMax(timeout, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        return (LoaderStreamChannel<OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamChannel<OUT>) super.bind(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort() {

        return (LoaderStreamChannel<OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        return (LoaderStreamChannel<OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyExit() {

        return (LoaderStreamChannel<OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyThrow() {

        return (LoaderStreamChannel<OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> immediately() {

        return (LoaderStreamChannel<OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> skip(final int count) {

        return (LoaderStreamChannel<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> async() {

        return (LoaderStreamChannel<OUT>) super.async();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay,
                timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        return (LoaderStreamChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> collect(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.collect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> collect(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<AFTER>) super.collect(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Void> consume(@NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<Void>) super.consume(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        checkStatic(wrap(predicate), predicate);
        return (LoaderStreamChannel<OUT>) super.filter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<AFTER>) super.flatMap(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    getInvocationConfiguration() {

        final InvocationConfiguration config = getConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    getStreamInvocationConfiguration() {

        final InvocationConfiguration config = getStreamConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mStreamInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.map(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<AFTER>) super.map(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        if (!Reflection.hasStaticScope(factory)) {
            throw new IllegalArgumentException(
                    "the factory instance does not have a static scope: " + factory.getClass()
                                                                                   .getName());
        }

        return (LoaderStreamChannel<AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return (LoaderStreamChannel<AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> maxParallelInvocations(final int maxInvocations) {

        return (LoaderStreamChannel<OUT>) super.maxParallelInvocations(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannel<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> parallel() {

        return (LoaderStreamChannel<OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<OUT>) super.reduce(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<AFTER>) super.reduce(supplier, function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> repeat() {

        return (LoaderStreamChannel<OUT>) super.repeat();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamChannel<OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> runOnShared() {

        return (LoaderStreamChannel<OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> serial() {

        return (LoaderStreamChannel<OUT>) super.serial();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> sync() {

        return (LoaderStreamChannel<OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(@Nullable final AFTER output) {

        return (LoaderStreamChannel<AFTER>) super.then(output);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(@Nullable final AFTER... outputs) {

        return (LoaderStreamChannel<AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {

        return (LoaderStreamChannel<AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.then(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.then(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<AFTER>) super.then(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> then(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<AFTER>) super.then(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<? extends ParcelableSelectable<OUT>> toSelectable(final int index) {

        final ChannelConfiguration configuration = buildChannelConfiguration();
        final OutputChannel<? extends ParcelableSelectable<OUT>> channel =
                SparseChannels.toSelectable(this, index)
                              .getChannelConfiguration()
                              .with(configuration)
                              .setConfiguration()
                              .buildChannels();
        return newChannel(channel, getStreamConfiguration(), getDelegationType(), getBinder());
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final Consumer<? super RoutineException> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.tryCatch(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamChannel<AFTER> newChannel(
            @NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        return newChannel(channel, configuration, mConfiguration, delegationType, binder);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(configuration,
                mStreamConfiguration.builderFrom().with(mConfiguration).setConfiguration(),
                factory);
    }

    @NotNull
    public LoaderStreamChannel<OUT> cache(@Nullable final CacheStrategyType strategyType) {

        return getLoaderConfiguration().withCacheStrategy(strategyType).setConfiguration();
    }

    @NotNull
    public LoaderStreamChannel<OUT> factoryId(final int factoryId) {

        return getLoaderConfiguration().withFactoryId(factoryId).setConfiguration();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> getLoaderConfiguration
            () {

        final LoaderConfiguration config = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(this, config);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    getStreamLoaderConfiguration() {

        final LoaderConfiguration config = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(mStreamConfigurable,
                config);
    }

    @NotNull
    public LoaderStreamChannel<OUT> loaderId(final int loaderId) {

        return getLoaderConfiguration().withLoaderId(loaderId).setConfiguration();
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(@Nullable final TimeDuration staleTime) {

        return getLoaderConfiguration().withResultStaleTime(staleTime).setConfiguration();
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(final long time, @NotNull final TimeUnit timeUnit) {

        return getLoaderConfiguration().withResultStaleTime(time, timeUnit).setConfiguration();
    }

    @NotNull
    public LoaderStreamChannel<OUT> with(@Nullable final LoaderContext context) {

        mContextBuilder = (context != null) ? JRoutineLoader.with(context) : null;
        return this;
    }

    private void checkStatic(@NotNull final Wrapper wrapper, @NotNull final Object function) {

        if (!wrapper.hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }
    }

    @NotNull
    private <AFTER> LoaderStreamChannel<AFTER> newChannel(
            @NotNull final OutputChannel<AFTER> channel,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        return new DefaultLoaderStreamChannel<AFTER>(mContextBuilder, channel,
                invocationConfiguration, loaderConfiguration, delegationType, binder);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilder contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            return JRoutineCore.on(factory)
                               .getInvocationConfiguration()
                               .with(invocationConfiguration)
                               .setConfiguration()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.on(factory).buildRoutine(), factory.hashCode(),
                        DelegationType.SYNC);
        return contextBuilder.on(invocationFactory)
                             .getInvocationConfiguration()
                             .with(invocationConfiguration)
                             .setConfiguration()
                             .getLoaderConfiguration()
                             .with(loaderConfiguration)
                             .setConfiguration()
                             .buildRoutine();
    }

    @NotNull
    public LoaderStreamChannel<OUT> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }
}
