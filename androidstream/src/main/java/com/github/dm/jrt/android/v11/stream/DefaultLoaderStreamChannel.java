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

import android.support.annotation.NonNull;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.JRoutineLoader.LoaderBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.JRoutineCore;
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
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.Wrapper;
import com.github.dm.jrt.stream.AbstractStreamChannel;
import com.github.dm.jrt.stream.StreamChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream output channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannel<IN, OUT> extends AbstractStreamChannel<IN, OUT>
        implements LoaderStreamChannel<OUT>, Configurable<LoaderStreamChannel<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannel.super.invocationConfiguration()
                                                    .with(null)
                                                    .with(configuration)
                                                    .apply();
                    return DefaultLoaderStreamChannel.this;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannel.super.streamInvocationConfiguration()
                                                    .with(null)
                                                    .with(configuration)
                                                    .apply();
                    return DefaultLoaderStreamChannel.this;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

    private LoaderBuilder mContextBuilder;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannel<OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param builder       the context builder.
     * @param sourceChannel the source output channel.
     * @param invoke        the invoke function.
     */
    DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
            @NotNull final OutputChannel<IN> sourceChannel,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> invoke) {

        this(builder, InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC, sourceChannel,
                invoke);
    }

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param invocationMode          the invocation mode.
     * @param sourceChannel           the source output channel.
     * @param invoke                  the invoke function.
     */
    private DefaultLoaderStreamChannel(@Nullable final LoaderBuilder builder,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<IN> sourceChannel,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> invoke) {

        super(invocationConfiguration, invocationMode, sourceChannel, invoke);
        mContextBuilder = builder;
        mStreamConfiguration =
                ConstantConditions.notNull("loader configuration", loaderConfiguration);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(@NotNull final UnitDuration timeout) {

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
    public LoaderStreamChannel<OUT> skipNext(final int count) {

        return (LoaderStreamChannel<OUT>) super.skipNext(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> apply(
            @NotNull final Function<? super StreamChannel<OUT>, ? extends OutputChannel<AFTER>>
                    function) {

        return (LoaderStreamChannel<AFTER>) super.apply(function);
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
            final int maxInputs, @Nullable final UnitDuration maxDelay) {

        return (LoaderStreamChannel<OUT>) super.backPressureOn(runner, maxInputs, maxDelay);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.collect(consumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> LoaderStreamChannel<AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<AFTER>) super.collect(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> consumer) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.collect(supplier, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> concat(@Nullable final OUT output) {

        return (LoaderStreamChannel<OUT>) super.concat(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> concat(@Nullable final OUT... outputs) {

        return (LoaderStreamChannel<OUT>) super.concat(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> concat(@Nullable final Iterable<? extends OUT> outputs) {

        return (LoaderStreamChannel<OUT>) super.concat(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> concat(@NotNull final OutputChannel<? extends OUT> channel) {

        return (LoaderStreamChannel<OUT>) super.concat(channel);
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
    invocationConfiguration() {

        final InvocationConfiguration config = getConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> limit(final int count) {

        return (LoaderStreamChannel<OUT>) super.limit(count);
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
    public <AFTER> LoaderStreamChannel<AFTER> mapAll(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.mapAll(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<AFTER>) super.mapAll(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> onError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.onError(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<Void> onOutput(@NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<Void>) super.onOutput(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElse(@Nullable final OUT output) {

        return (LoaderStreamChannel<OUT>) super.orElse(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElse(@Nullable final OUT... outputs) {

        return (LoaderStreamChannel<OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElse(@Nullable final Iterable<? extends OUT> outputs) {

        return (LoaderStreamChannel<OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElseGet(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.orElseGet(count, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElseGet(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.orElseGet(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<OUT>) super.orElseGet(count, supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> orElseGet(@NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<OUT>) super.orElseGet(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannel<OUT>) super.ordered(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> parallel(final int maxInvocations) {

        return (LoaderStreamChannel<OUT>) super.parallel(maxInvocations);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> parallel() {

        return (LoaderStreamChannel<OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> peek(@NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<OUT>) super.peek(consumer);
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
    public LoaderStreamChannel<OUT> skip(final int count) {

        return (LoaderStreamChannel<OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    streamInvocationConfiguration() {

        final InvocationConfiguration config = getStreamConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mStreamInvocationConfigurable, config);
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
    public <AFTER> LoaderStreamChannel<AFTER> thenGet(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.thenGet(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> thenGet(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<AFTER>) super.thenGet(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<AFTER>) super.thenGet(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<AFTER>) super.thenGet(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<? extends ParcelableSelectable<OUT>> toSelectable(final int index) {

        return newChannel(getStreamConfiguration(), getInvocationMode(), getSourceChannel(),
                getInvoke().andThen(new SelectableInvoke<OUT>(buildChannelConfiguration(), index)));
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
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> tryFinally(@NotNull final Runnable runnable) {

        if (!Reflection.hasStaticScope(runnable)) {
            throw new IllegalArgumentException(
                    "the runnable instance does not have a static scope: " + runnable.getClass()
                                                                                     .getName());
        }

        return (LoaderStreamChannel<OUT>) super.tryFinally(runnable);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> LoaderStreamChannel<AFTER> newChannel(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        return newChannel(streamConfiguration, mStreamConfiguration, invocationMode, sourceChannel,
                invoke);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(configuration, buildLoaderConfiguration(), factory);
    }

    @NotNull
    public LoaderStreamChannel<OUT> cache(@Nullable final CacheStrategyType strategyType) {

        return loaderConfiguration().withCacheStrategy(strategyType).apply();
    }

    @NotNull
    public LoaderStreamChannel<OUT> factoryId(final int factoryId) {

        return loaderConfiguration().withFactoryId(factoryId).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> loaderConfiguration() {

        final LoaderConfiguration config = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(this, config);
    }

    @NotNull
    public LoaderStreamChannel<OUT> loaderId(final int loaderId) {

        return loaderConfiguration().withLoaderId(loaderId).apply();
    }

    @NotNull
    public <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilder contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(contextBuilder.on(factory)
                                 .invocationConfiguration()
                                 .with(buildConfiguration())
                                 .apply()
                                 .loaderConfiguration()
                                 .with(buildLoaderConfiguration())
                                 .apply()
                                 .buildRoutine());
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(@Nullable final UnitDuration staleTime) {

        return loaderConfiguration().withResultStaleTime(staleTime).apply();
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(final long time, @NotNull final TimeUnit timeUnit) {

        return loaderConfiguration().withResultStaleTime(time, timeUnit).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    streamLoaderConfiguration() {

        final LoaderConfiguration config = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(mStreamConfigurable,
                config);
    }

    @NotNull
    public LoaderStreamChannel<OUT> with(@Nullable final LoaderContext context) {

        mContextBuilder = (context != null) ? JRoutineLoader.with(context) : null;
        return this;
    }

    @NonNull
    private LoaderConfiguration buildLoaderConfiguration() {

        return mStreamConfiguration.builderFrom().with(mConfiguration).apply();
    }

    private void checkStatic(@NotNull final Wrapper wrapper, @NotNull final Object function) {

        if (!wrapper.hasStaticScope()) {
            throw new IllegalArgumentException(
                    "the function instance does not have a static scope: " + function.getClass()
                                                                                     .getName());
        }
    }

    @NotNull
    private <BEFORE, AFTER> LoaderStreamChannel<AFTER> newChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        return new DefaultLoaderStreamChannel<BEFORE, AFTER>(mContextBuilder,
                invocationConfiguration, loaderConfiguration, invocationMode, sourceChannel,
                invoke);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilder contextBuilder = mContextBuilder;
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

    /**
     * Selectable invoke function.
     *
     * @param <OUT> the output data type.
     */
    private static class SelectableInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<ParcelableSelectable<OUT>>> {

        private final ChannelConfiguration mConfiguration;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param configuration the channel configuration.
         * @param index         the selectable index.
         */
        private SelectableInvoke(@NotNull final ChannelConfiguration configuration,
                final int index) {

            super(asArgs(configuration, index));
            mConfiguration = configuration;
            mIndex = index;
        }

        @SuppressWarnings("unchecked")
        public OutputChannel<ParcelableSelectable<OUT>> apply(final OutputChannel<OUT> channel) {

            final OutputChannel<? extends ParcelableSelectable<OUT>> outputChannel =
                    SparseChannels.toSelectable(channel, mIndex)
                                  .channelConfiguration()
                                  .with(mConfiguration)
                                  .apply()
                                  .buildChannels();
            return (OutputChannel<ParcelableSelectable<OUT>>) outputChannel;
        }
    }

    @NotNull
    public LoaderStreamChannel<OUT> apply(@NotNull final LoaderConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }
}
