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

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.builder.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.channel.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.JRoutineLoader.ContextBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.core.channel.SparseChannels;
import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.function.Wrapper;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.stream.AbstractStreamChannel;
import com.github.dm.jrt.util.Reflection;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream output channel.
 * <p/>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
public class DefaultLoaderStreamChannel<OUT> extends AbstractStreamChannel<OUT>
        implements LoaderStreamChannel<OUT>, Configurable<LoaderStreamChannel<OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannel<OUT> outer = DefaultLoaderStreamChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<OUT>>() {

                @NotNull
                public LoaderStreamChannel<OUT> setConfiguration(
                        @NotNull final InvocationConfiguration configuration) {

                    final DefaultLoaderStreamChannel<OUT> outer = DefaultLoaderStreamChannel.this;
                    outer.setConfiguration(configuration);
                    return outer;
                }
            };

    private LoaderConfiguration mConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ContextBuilder mContextBuilder;

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
    DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
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
    DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
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
    private DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
            @NotNull final OutputChannel<OUT> channel, @Nullable final Binder binder) {

        this(builder, channel, InvocationConfiguration.DEFAULT_CONFIGURATION,
             LoaderConfiguration.DEFAULT_CONFIGURATION, DelegationType.ASYNC, binder);
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
    @SuppressWarnings("ConstantConditions")
    private DefaultLoaderStreamChannel(@Nullable final ContextBuilder builder,
            @NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final DelegationType delegationType, @Nullable final Binder binder) {

        super(channel, invocationConfiguration, delegationType, binder);
        if (loaderConfiguration == null) {
            throw new NullPointerException("the loader configuration must not be null");
        }

        mContextBuilder = builder;
        mStreamConfiguration = loaderConfiguration;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(@NotNull final TimeDuration timeout) {

        super.afterMax(timeout);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        super.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        super.allInto(results);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> bindTo(@NotNull final OutputConsumer<? super OUT> consumer) {

        super.bindTo(consumer);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort() {

        super.eventuallyAbort();
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        super.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyExit() {

        super.eventuallyExit();
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> eventuallyThrow() {

        super.eventuallyThrow();
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> immediately() {

        super.immediately();
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> skip(final int count) {

        super.skip(count);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> async() {

        super.async();
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, final long maxDelay, @NotNull final TimeUnit timeUnit) {

        super.backPressureOn(runner, maxInputs, maxDelay, timeUnit);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> backPressureOn(@Nullable final Runner runner,
            final int maxInputs, @Nullable final TimeDuration maxDelay) {

        super.backPressureOn(runner, maxInputs, maxDelay);
        return this;
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
    public LoaderStreamChannel<OUT> collect(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannel<OUT>) super.collect(function);
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

        super.maxParallelInvocations(maxInvocations);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        super.ordered(orderType);
        return this;
    }

    @NotNull
    @Override
    public LoaderStreamChannel<OUT> parallel() {

        super.parallel();
        return this;
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
    public LoaderStreamChannel<OUT> sync() {

        super.sync();
        return this;
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
        return newChannel(SparseChannels.toSelectable(this, index)
                                        .withChannels()
                                        .with(configuration)
                                        .getConfigured()
                                        .build(), getStreamConfiguration(), getDelegationType(),
                          getBinder());
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
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mInvocationConfigurable, getConfiguration());
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>>
    withStreamInvocations() {

        return new InvocationConfiguration.Builder<LoaderStreamChannel<OUT>>(
                mStreamInvocationConfigurable, getStreamConfiguration());
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
                          mStreamConfiguration.builderFrom().with(mConfiguration).getConfigured(),
                          factory);
    }

    @NotNull
    public LoaderStreamChannel<OUT> cache(@Nullable final CacheStrategyType strategyType) {

        return withLoaders().withCacheStrategy(strategyType).getConfigured();
    }

    @NotNull
    public LoaderStreamChannel<OUT> loaderId(final int loaderId) {

        return withLoaders().withLoaderId(loaderId).getConfigured();
    }

    @NotNull
    public LoaderStreamChannel<OUT> routineId(final int routineId) {

        return withLoaders().withRoutineId(routineId).getConfigured();
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(final long time, @NotNull final TimeUnit timeUnit) {

        return withLoaders().withResultStaleTime(time, timeUnit).getConfigured();
    }

    @NotNull
    public LoaderStreamChannel<OUT> staleAfter(@Nullable final TimeDuration staleTime) {

        return withLoaders().withResultStaleTime(staleTime).getConfigured();
    }

    @NotNull
    public LoaderStreamChannel<OUT> with(@Nullable final LoaderContext context) {

        mContextBuilder = (context != null) ? JRoutineLoader.with(context) : null;
        return this;
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withLoaders() {

        final LoaderConfiguration configuration = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(this, configuration);
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withStreamLoaders() {

        final LoaderConfiguration configuration = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannel<OUT>>(mStreamConfigurable,
                                                                         configuration);
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
                                                     invocationConfiguration, loaderConfiguration,
                                                     delegationType, binder);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final ContextBuilder contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            return JRoutineCore.on(factory)
                               .withInvocations()
                               .with(invocationConfiguration)
                               .getConfigured()
                               .buildRoutine();
        }

        final FunctionContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.on(factory).buildRoutine(), factory.hashCode(),
                            DelegationType.SYNC);
        return contextBuilder.on(invocationFactory)
                             .withInvocations()
                             .with(invocationConfiguration)
                             .getConfigured()
                             .withLoaders()
                             .with(loaderConfiguration)
                             .getConfigured()
                             .buildRoutine();
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public LoaderStreamChannel<OUT> setConfiguration(
            @NotNull final LoaderConfiguration configuration) {

        if (configuration == null) {
            throw new NullPointerException("the loader configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}
