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
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.Action;
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
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannelCompat<IN, OUT> extends AbstractStreamChannel<IN, OUT>
        implements LoaderStreamChannelCompat<IN, OUT>,
        Configurable<LoaderStreamChannelCompat<IN, OUT>> {

    private final LoaderStreamConfigurationCompat mStreamConfiguration;

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfigurationCompat streamConfiguration =
                            mStreamConfiguration;
                    return (LoaderStreamChannelCompat<IN, OUT>) newChannel(
                            newConfiguration(streamConfiguration.getStreamConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final Configurable<LoaderStreamChannelCompat<IN, OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return (LoaderStreamChannelCompat<IN, OUT>) newChannel(
                            newConfiguration(configuration,
                                    mStreamConfiguration.getCurrentLoaderConfiguration()));
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfigurationCompat streamConfiguration =
                            mStreamConfiguration;
                    return (LoaderStreamChannelCompat<IN, OUT>) newChannel(
                            newConfiguration(configuration,
                                    streamConfiguration.getCurrentConfiguration(),
                                    streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamChannelCompat(@NotNull final Channel<?, IN> channel) {
        this(new DefaultLoaderStreamConfigurationCompat(null,
                LoaderConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC), channel);
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     */
    private DefaultLoaderStreamChannelCompat(
            @NotNull final LoaderStreamConfigurationCompat streamConfiguration,
            @NotNull final Channel<?, IN> sourceChannel) {
        super(streamConfiguration, sourceChannel);
        mStreamConfiguration = streamConfiguration;
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     * @param bindingFunction     if null the stream will act as a wrapper of the source output
     *                            channel.
     */
    private DefaultLoaderStreamChannelCompat(
            @NotNull final LoaderStreamConfigurationCompat streamConfiguration,
            @NotNull final Channel<?, IN> sourceChannel,
            @Nullable final Function<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
        super(streamConfiguration, sourceChannel, bindingFunction);
        mStreamConfiguration =
                ConstantConditions.notNull("loader stream configuration", streamConfiguration);
    }

    private static void checkStatic(@NotNull final String name, @NotNull final Object obj) {
        if (!Reflection.hasStaticScope(obj)) {
            throw new IllegalArgumentException(
                    "the " + name + " instance does not have a static scope: " + obj.getClass()
                                                                                    .getName());
        }
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
    public LoaderStreamChannelCompat<IN, OUT> after(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.after(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> after(@NotNull final UnitDuration delay) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.after(delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> allInto(
            @NotNull final Collection<? super OUT> results) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> bind(
            @NotNull final ChannelConsumer<? super OUT> consumer) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.bind(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> close() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.close();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> eventuallyAbort() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> eventuallyBreak() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyBreak();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> eventuallyFail() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyFail();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> immediately() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> pass(
            @Nullable final Channel<?, ? extends IN> channel) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.pass(channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.pass(inputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> pass(@Nullable final IN input) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.pass(input);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> pass(@Nullable final IN... inputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.pass(inputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> skipNext(final int count) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.skipNext(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> sortedByCall() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.sortedByCall();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> sortedByDelay() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.sortedByDelay();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> append(@Nullable final OUT output) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.append(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> append(@Nullable final OUT... outputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> append(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> append(
            @NotNull final Channel<?, ? extends OUT> channel) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.append(channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.appendGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> appendGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.appendGet(outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> appendGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.appendGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> appendGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.appendGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> async() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.async();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> async(@Nullable final Runner runner) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.async(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.asyncMap(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @NotNull final Backoff backoff) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.backoffOn(runner, limit, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, final long delay, @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.backoffOn(runner, limit, delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @Nullable final UnitDuration delay) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.backoffOn(runner, limit, delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        checkStatic(wrap(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.collect(accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        checkStatic(wrap(seedSupplier), seedSupplier);
        checkStatic(wrap(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.collect(seedSupplier,
                accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> LoaderStreamChannelCompat<IN, AFTER> collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        checkStatic(wrap(collectionSupplier), collectionSupplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.collectInto(collectionSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> delay(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.delay(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> delay(@NotNull final UnitDuration delay) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.delay(delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> filterPredicate) {
        checkStatic(wrap(filterPredicate), filterPredicate);
        return (LoaderStreamChannelCompat<IN, OUT>) super.filter(filterPredicate);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamChannel<IN, OUT>, ? extends
                    StreamChannel<BEFORE, AFTER>> liftFunction) {
        return (LoaderStreamChannelCompat<BEFORE, AFTER>) super.flatLift(liftFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.flatMap(mappingFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> flow() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.flow();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> flow(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.flow(outputConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> flow(
            @NotNull final Consumer<? super OUT> outputConsumer,
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.flow(outputConsumer, errorConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> flow(
            @NotNull final Consumer<? super OUT> outputConsumer,
            @NotNull final Consumer<? super RoutineException> errorConsumer,
            @NotNull final Action completeAction) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.flow(outputConsumer, errorConsumer,
                completeAction);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    invocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mInvocationConfigurable, mStreamConfiguration.getCurrentConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.invocationMode(invocationMode);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> lag(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.lag(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> lag(@NotNull final UnitDuration delay) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.lag(delay);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.lift(liftFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> liftConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, AFTER>>> liftFunction) {
        checkStatic(wrap(liftFunction), liftFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.liftConfig(liftFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> limit(final int count) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.limit(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.map(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.map(builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapAll(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        checkStatic(wrap(mappingConsumer), mappingConsumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapAllMore(mappingConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        checkStatic(wrap(mappingConsumer), mappingConsumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapMore(mappingConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, Void> onComplete(@NotNull final Action completeAction) {
        return (LoaderStreamChannelCompat<IN, Void>) super.onComplete(completeAction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.onError(errorConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return (LoaderStreamChannelCompat<IN, Void>) super.onOutput(outputConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElse(@Nullable final OUT output) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElse(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElse(@Nullable final OUT... outputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElse(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGet(outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> order(@Nullable final OrderType orderType) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.order(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> parallel() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> parallel(final int maxInvocations) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.parallel(maxInvocations);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        checkStatic(wrap(streamFunction), streamFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallel(count, streamFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallel(count, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallel(count, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallel(count, builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic(wrap(streamFunction), streamFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallelBy(keyFunction, streamFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallelBy(keyFunction, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("routine", routine);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallelBy(keyFunction, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        checkStatic(wrap(keyFunction), keyFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.parallelBy(keyFunction, builder);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> peek(
            @NotNull final Consumer<? super OUT> peekConsumer) {
        checkStatic(wrap(peekConsumer), peekConsumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.peek(peekConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> peekComplete(@NotNull final Action peekAction) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.peekComplete(peekAction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        checkStatic(wrap(accumulateFunction), accumulateFunction);
        return (LoaderStreamChannelCompat<IN, OUT>) super.reduce(accumulateFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        checkStatic(wrap(seedSupplier), seedSupplier);
        checkStatic(wrap(accumulateFunction), accumulateFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.reduce(seedSupplier,
                accumulateFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> replay() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.replay();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> retry(final int count) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.retry(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> retry(final int count,
            @NotNull final Backoff backoff) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.retry(count, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.retry(backoffFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, ? extends ParcelableSelectable<OUT>> selectable(
            final int index) {
        return liftConfig(new SelectableLift<IN, OUT>(index));
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> sequential() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.sequential();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> skip(final int count) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> straight() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.straight();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamInvocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mStreamInvocationConfigurable, mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> sync() {
        return (LoaderStreamChannelCompat<IN, OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(@Nullable final AFTER output) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.then(output);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(@Nullable final AFTER... outputs) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {
        return (LoaderStreamChannelCompat<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGet(outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryCatch(catchFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryCatchMore(catchConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryFinally(@NotNull final Action finallyAction) {
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryFinally(finallyAction);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> newChannel(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, BEFORE> sourceChannel,
            @NotNull final Function<Channel<?, BEFORE>, Channel<?, AFTER>> bindingFunction) {
        return new DefaultLoaderStreamChannelCompat<BEFORE, AFTER>(
                (LoaderStreamConfigurationCompat) streamConfiguration, sourceChannel,
                bindingFunction);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration currentConfiguration,
            @NotNull final InvocationMode invocationMode) {
        final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfigurationCompat(
                loaderStreamConfiguration.getLoaderContext(),
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                loaderStreamConfiguration.getCurrentLoaderConfiguration(), streamConfiguration,
                currentConfiguration, invocationMode);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode) {
        final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfigurationCompat(
                loaderStreamConfiguration.getLoaderContext(),
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                LoaderConfiguration.defaultConfiguration(), streamConfiguration,
                InvocationConfiguration.defaultConfiguration(), invocationMode);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfigurationCompat loaderStreamConfiguration =
                (LoaderStreamConfigurationCompat) streamConfiguration;
        final LoaderContextCompat loaderContext = loaderStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            return JRoutineCore.with(factory)
                               .invocationConfiguration()
                               .with(loaderStreamConfiguration.asInvocationConfiguration())
                               .applied()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.with(factory).buildRoutine(), factory.hashCode(),
                        InvocationMode.SYNC);
        return JRoutineLoaderCompat.on(loaderContext)
                                   .with(invocationFactory)
                                   .invocationConfiguration()
                                   .with(loaderStreamConfiguration.asInvocationConfiguration())
                                   .applied()
                                   .loaderConfiguration()
                                   .with(loaderStreamConfiguration.asLoaderConfiguration())
                                   .applied()
                                   .buildRoutine();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> cache(
            @Nullable final CacheStrategyType strategyType) {
        return loaderConfiguration().withCacheStrategy(strategyType).applied();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> factoryId(final int factoryId) {
        return loaderConfiguration().withFactoryId(factoryId).applied();
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    loaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(this,
                mStreamConfiguration.getCurrentLoaderConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> loaderId(final int loaderId) {
        return loaderConfiguration().withLoaderId(loaderId).applied();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderContextCompat loaderContext = mStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(JRoutineLoaderCompat.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> on(@Nullable final LoaderContextCompat context) {
        return (LoaderStreamChannelCompat<IN, OUT>) newChannel(newConfiguration(context));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
        final LoaderContextCompat loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic("factory", factory);
        return parallel(count, JRoutineLoaderCompat.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallel(final int count,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, buildRoutine(builder));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
        final LoaderContextCompat loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return parallelBy(keyFunction, JRoutineLoaderCompat.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        checkStatic(wrap(keyFunction), keyFunction);
        return parallelBy(keyFunction, buildRoutine(builder));
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> staleAfter(@Nullable final UnitDuration staleTime) {
        return loaderConfiguration().withResultStaleTime(staleTime).applied();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {
        return loaderConfiguration().withResultStaleTime(time, timeUnit).applied();
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamLoaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mStreamConfigurable, mStreamConfiguration.getStreamLoaderConfiguration());
    }

    @NotNull
    private <AFTER> LoaderRoutine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
        return builder.invocationConfiguration()
                      .with(null)
                      .with(streamConfiguration.asInvocationConfiguration())
                      .applied()
                      .loaderConfiguration()
                      .with(null)
                      .with(streamConfiguration.asLoaderConfiguration())
                      .applied()
                      .buildRoutine();
    }

    @NotNull
    private LoaderStreamConfigurationCompat newConfiguration(
            @Nullable final LoaderContextCompat context) {
        final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfigurationCompat(context,
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                loaderStreamConfiguration.getCurrentLoaderConfiguration(),
                loaderStreamConfiguration.getStreamConfiguration(),
                loaderStreamConfiguration.getCurrentConfiguration(),
                loaderStreamConfiguration.getInvocationMode());
    }

    @NotNull
    private LoaderStreamConfigurationCompat newConfiguration(
            @NotNull final LoaderConfiguration streamConfiguration,
            @NotNull final LoaderConfiguration configuration) {
        final LoaderStreamConfigurationCompat loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfigurationCompat(
                loaderStreamConfiguration.getLoaderContext(), streamConfiguration, configuration,
                loaderStreamConfiguration.getStreamConfiguration(),
                loaderStreamConfiguration.getCurrentConfiguration(),
                loaderStreamConfiguration.getInvocationMode());
    }

    /**
     * Default implementation of a loader stream configuration.
     */
    protected static class DefaultLoaderStreamConfigurationCompat
            implements LoaderStreamConfigurationCompat {

        private final InvocationConfiguration mCurrentConfiguration;

        private final LoaderConfiguration mCurrentLoaderConfiguration;

        private final InvocationMode mInvocationMode;

        private final LoaderContextCompat mLoaderContext;

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
        private DefaultLoaderStreamConfigurationCompat(@Nullable final LoaderContextCompat context,
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
                mChannelConfiguration =
                        asInvocationConfiguration().outputConfigurationBuilder().applied();
            }

            return mChannelConfiguration;
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {
            if (mInvocationConfiguration == null) {
                mInvocationConfiguration =
                        mStreamConfiguration.builderFrom().with(mCurrentConfiguration).applied();
            }

            return mInvocationConfiguration;
        }

        @NotNull
        @Override
        public InvocationConfiguration getCurrentConfiguration() {
            return mCurrentConfiguration;
        }

        @NotNull
        @Override
        public InvocationMode getInvocationMode() {
            return mInvocationMode;
        }

        @NotNull
        @Override
        public InvocationConfiguration getStreamConfiguration() {
            return mStreamConfiguration;
        }

        @NotNull
        @Override
        public LoaderConfiguration asLoaderConfiguration() {
            if (mLoaderConfiguration == null) {
                mLoaderConfiguration = mStreamLoaderConfiguration.builderFrom()
                                                                 .with(mCurrentLoaderConfiguration)
                                                                 .applied();
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
        public LoaderContextCompat getLoaderContext() {
            return mLoaderContext;
        }

        @NotNull
        @Override
        public LoaderConfiguration getStreamLoaderConfiguration() {
            return mStreamLoaderConfiguration;
        }
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> apply(
            @NotNull final LoaderConfiguration configuration) {
        return (LoaderStreamChannelCompat<IN, OUT>) newChannel(
                newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(),
                        configuration));
    }
}
