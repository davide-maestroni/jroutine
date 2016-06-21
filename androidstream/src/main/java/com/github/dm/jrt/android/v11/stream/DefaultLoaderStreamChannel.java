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
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.ConstantConditions;
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
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannel<IN, OUT> extends AbstractStreamChannel<IN, OUT>
        implements LoaderStreamChannel<IN, OUT>, Configurable<LoaderStreamChannel<IN, OUT>> {

    private final LoaderStreamConfiguration mStreamConfiguration;

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannel<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return (LoaderStreamChannel<IN, OUT>) newChannel(
                            newConfiguration(streamConfiguration.getStreamConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final Configurable<LoaderStreamChannel<IN, OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannel<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannel<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return (LoaderStreamChannel<IN, OUT>) newChannel(newConfiguration(configuration,
                            mStreamConfiguration.getCurrentLoaderConfiguration()));
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannel<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannel<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannel<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return (LoaderStreamChannel<IN, OUT>) newChannel(newConfiguration(configuration,
                            streamConfiguration.getCurrentConfiguration(),
                            streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     *
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamChannel(@NotNull final Channel<?, IN> channel) {
        this(new DefaultLoaderStreamConfiguration(null, LoaderConfiguration.defaultConfiguration(),
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
    private DefaultLoaderStreamChannel(@NotNull final LoaderStreamConfiguration streamConfiguration,
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
    private DefaultLoaderStreamChannel(@NotNull final LoaderStreamConfiguration streamConfiguration,
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
    public LoaderStreamChannel<IN, OUT> after(@NotNull final UnitDuration delay) {
        return (LoaderStreamChannel<IN, OUT>) super.after(delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannel<IN, OUT>) super.after(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        return (LoaderStreamChannel<IN, OUT>) super.allInto(results);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {
        return (LoaderStreamChannel<IN, OUT>) super.bind(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> close() {
        return (LoaderStreamChannel<IN, OUT>) super.close();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> eventuallyAbort() {
        return (LoaderStreamChannel<IN, OUT>) super.eventuallyAbort();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        return (LoaderStreamChannel<IN, OUT>) super.eventuallyAbort(reason);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> eventuallyBreak() {
        return (LoaderStreamChannel<IN, OUT>) super.eventuallyBreak();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> eventuallyFail() {
        return (LoaderStreamChannel<IN, OUT>) super.eventuallyFail();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> immediately() {
        return (LoaderStreamChannel<IN, OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orderByCall() {
        return (LoaderStreamChannel<IN, OUT>) super.orderByCall();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orderByDelay() {
        return (LoaderStreamChannel<IN, OUT>) super.orderByDelay();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        return (LoaderStreamChannel<IN, OUT>) super.pass(channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        return (LoaderStreamChannel<IN, OUT>) super.pass(inputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> pass(@Nullable final IN input) {
        return (LoaderStreamChannel<IN, OUT>) super.pass(input);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> pass(@Nullable final IN... inputs) {
        return (LoaderStreamChannel<IN, OUT>) super.pass(inputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> skipNext(final int count) {
        return (LoaderStreamChannel<IN, OUT>) super.skipNext(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> append(@Nullable final OUT output) {
        return (LoaderStreamChannel<IN, OUT>) super.append(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> append(@Nullable final OUT... outputs) {
        return (LoaderStreamChannel<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> append(@Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamChannel<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> append(@NotNull final Channel<?, ? extends OUT> channel) {
        return (LoaderStreamChannel<IN, OUT>) super.append(channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {
        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<IN, OUT>) super.appendGet(count, supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> appendGet(@NotNull final Supplier<? extends OUT> supplier) {
        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannel<IN, OUT>) super.appendGet(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> appendGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<IN, OUT>) super.appendGetMore(count, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> appendGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannel<IN, OUT>) super.appendGetMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> async() {
        return (LoaderStreamChannel<IN, OUT>) super.async();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> async(@Nullable final Runner runner) {
        return (LoaderStreamChannel<IN, OUT>) super.async(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return (LoaderStreamChannel<IN, OUT>) super.asyncMap(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @NotNull final Backoff backoff) {
        return (LoaderStreamChannel<IN, OUT>) super.backoffOn(runner, limit, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            final long delay, @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannel<IN, OUT>) super.backoffOn(runner, limit, delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @Nullable final UnitDuration delay) {
        return (LoaderStreamChannel<IN, OUT>) super.backoffOn(runner, limit, delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bind() {
        return (LoaderStreamChannel<IN, OUT>) super.bind();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bindAfter(@NotNull final UnitDuration delay) {
        return (LoaderStreamChannel<IN, OUT>) super.bindAfter(delay);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bindAfter(@NotNull final UnitDuration delay,
            @NotNull final OutputConsumer<OUT> consumer) {
        return (LoaderStreamChannel<IN, OUT>) super.bindAfter(delay, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bindAfter(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamChannel<IN, OUT>) super.bindAfter(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> bindAfter(final long delay,
            @NotNull final TimeUnit timeUnit, @NotNull final OutputConsumer<OUT> consumer) {
        return (LoaderStreamChannel<IN, OUT>) super.bindAfter(delay, timeUnit, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        checkStatic(wrap(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamChannel<IN, OUT>) super.collect(accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        checkStatic(wrap(seedSupplier), seedSupplier);
        checkStatic(wrap(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamChannel<IN, AFTER>) super.collect(seedSupplier, accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> LoaderStreamChannel<IN, AFTER> collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        checkStatic(wrap(collectionSupplier), collectionSupplier);
        return (LoaderStreamChannel<IN, AFTER>) super.collectInto(collectionSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> filterPredicate) {
        checkStatic(wrap(filterPredicate), filterPredicate);
        return (LoaderStreamChannel<IN, OUT>) super.filter(filterPredicate);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamChannel<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamChannel<IN, OUT>, ? extends
                    StreamChannel<BEFORE, AFTER>> transformFunction) {
        return (LoaderStreamChannel<BEFORE, AFTER>) super.flatLift(transformFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.flatMap(mappingFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> immediate() {
        return (LoaderStreamChannel<IN, OUT>) super.immediate();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<IN, OUT>>
    invocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamChannel<IN, OUT>>(
                mInvocationConfigurable, mStreamConfiguration.getCurrentConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        return (LoaderStreamChannel<IN, OUT>) super.invocationMode(invocationMode);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, AFTER>>> transformFunction) {
        return (LoaderStreamChannel<IN, AFTER>) super.lift(transformFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> liftConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, AFTER>>> transformFunction) {
        return (LoaderStreamChannel<IN, AFTER>) super.liftConfig(transformFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> limit(final int count) {
        return (LoaderStreamChannel<IN, OUT>) super.limit(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.map(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamChannel<IN, AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamChannel<IN, AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamChannel<IN, AFTER>) super.map(builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        checkStatic(wrap(mappingFunction), mappingFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.mapAll(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        checkStatic(wrap(mappingConsumer), mappingConsumer);
        return (LoaderStreamChannel<IN, AFTER>) super.mapAllMore(mappingConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        checkStatic(wrap(mappingConsumer), mappingConsumer);
        return (LoaderStreamChannel<IN, AFTER>) super.mapMore(mappingConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, Void> onComplete(@NotNull final Runnable action) {
        return (LoaderStreamChannel<IN, Void>) super.onComplete(action);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return (LoaderStreamChannel<IN, OUT>) super.onError(errorConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return (LoaderStreamChannel<IN, Void>) super.onOutput(outputConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElse(@Nullable final OUT output) {
        return (LoaderStreamChannel<IN, OUT>) super.orElse(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElse(@Nullable final OUT... outputs) {
        return (LoaderStreamChannel<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElse(@Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamChannel<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannel<IN, OUT>) super.orElseGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannel<IN, OUT>) super.orElseGet(outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannel<IN, OUT>) super.orElseGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannel<IN, OUT>) super.orElseGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> order(@Nullable final OrderType orderType) {
        return (LoaderStreamChannel<IN, OUT>) super.order(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> parallel() {
        return (LoaderStreamChannel<IN, OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> parallel(final int maxInvocations) {
        return (LoaderStreamChannel<IN, OUT>) super.parallel(maxInvocations);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        checkStatic(wrap(streamFunction), streamFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.parallel(count, streamFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamChannel<IN, AFTER>) super.parallel(count, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamChannel<IN, AFTER>) super.parallel(count, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamChannel<IN, AFTER>) super.parallel(count, builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic(wrap(streamFunction), streamFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.parallelBy(keyFunction, streamFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return (LoaderStreamChannel<IN, AFTER>) super.parallelBy(keyFunction, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("routine", routine);
        return (LoaderStreamChannel<IN, AFTER>) super.parallelBy(keyFunction, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        checkStatic(wrap(keyFunction), keyFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.parallelBy(keyFunction, builder);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> peek(@NotNull final Consumer<? super OUT> peekConsumer) {
        checkStatic(wrap(peekConsumer), peekConsumer);
        return (LoaderStreamChannel<IN, OUT>) super.peek(peekConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> peekComplete(@NotNull final Runnable peekAction) {
        return (LoaderStreamChannel<IN, OUT>) super.peekComplete(peekAction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        checkStatic(wrap(accumulateFunction), accumulateFunction);
        return (LoaderStreamChannel<IN, OUT>) super.reduce(accumulateFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        checkStatic(wrap(seedSupplier), seedSupplier);
        checkStatic(wrap(accumulateFunction), accumulateFunction);
        return (LoaderStreamChannel<IN, AFTER>) super.reduce(seedSupplier, accumulateFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> replay() {
        return (LoaderStreamChannel<IN, OUT>) super.replay();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> retry(final int count) {
        return (LoaderStreamChannel<IN, OUT>) super.retry(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> retry(final int count, @NotNull final Backoff backoff) {
        return (LoaderStreamChannel<IN, OUT>) super.retry(count, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        return (LoaderStreamChannel<IN, OUT>) super.retry(backoffFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, ? extends ParcelableSelectable<OUT>> selectable(
            final int index) {
        return liftConfig(new SelectableTransform<IN, OUT>(index));
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> sequential() {
        return (LoaderStreamChannel<IN, OUT>) super.sequential();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> skip(final int count) {
        return (LoaderStreamChannel<IN, OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannel<IN, OUT>>
    streamInvocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamChannel<IN, OUT>>(
                mStreamInvocationConfigurable, mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> sync() {
        return (LoaderStreamChannel<IN, OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> then(@Nullable final AFTER output) {
        return (LoaderStreamChannel<IN, AFTER>) super.then(output);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> then(@Nullable final AFTER... outputs) {
        return (LoaderStreamChannel<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {
        return (LoaderStreamChannel<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannel<IN, AFTER>) super.thenGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(wrap(outputSupplier), outputSupplier);
        return (LoaderStreamChannel<IN, AFTER>) super.thenGet(outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannel<IN, AFTER>) super.thenGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(wrap(outputsConsumer), outputsConsumer);
        return (LoaderStreamChannel<IN, AFTER>) super.thenGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return (LoaderStreamChannel<IN, OUT>) super.tryCatch(catchFunction);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return (LoaderStreamChannel<IN, OUT>) super.tryCatchMore(catchConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> tryFinally(@NotNull final Runnable action) {
        return (LoaderStreamChannel<IN, OUT>) super.tryFinally(action);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> LoaderStreamChannel<BEFORE, AFTER> newChannel(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, BEFORE> sourceChannel,
            @NotNull final Function<Channel<?, BEFORE>, Channel<?, AFTER>> bindingFunction) {
        return new DefaultLoaderStreamChannel<BEFORE, AFTER>(
                (LoaderStreamConfiguration) streamConfiguration, sourceChannel, bindingFunction);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
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
    protected StreamConfiguration newConfiguration(
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
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfiguration loaderStreamConfiguration =
                (LoaderStreamConfiguration) streamConfiguration;
        final LoaderContext loaderContext = loaderStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            return JRoutineCore.on(factory)
                               .invocationConfiguration()
                               .with(loaderStreamConfiguration.asInvocationConfiguration())
                               .apply()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.on(factory).buildRoutine(), factory.hashCode(),
                        InvocationMode.SYNC);
        return JRoutineLoader.with(loaderContext)
                             .on(invocationFactory)
                             .invocationConfiguration()
                             .with(loaderStreamConfiguration.asInvocationConfiguration())
                             .apply()
                             .loaderConfiguration()
                             .with(loaderStreamConfiguration.asLoaderConfiguration())
                             .apply()
                             .buildRoutine();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> cache(@Nullable final CacheStrategyType strategyType) {
        return loaderConfiguration().withCacheStrategy(strategyType).apply();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> factoryId(final int factoryId) {
        return loaderConfiguration().withFactoryId(factoryId).apply();
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<IN, OUT>>
    loaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamChannel<IN, OUT>>(this,
                mStreamConfiguration.getCurrentLoaderConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> loaderId(final int loaderId) {
        return loaderConfiguration().withLoaderId(loaderId).apply();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderContext loaderContext = mStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(JRoutineLoader.with(loaderContext).on(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        final LoaderContext loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic("factory", factory);
        return parallel(count, JRoutineLoader.with(loaderContext).on(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, buildRoutine(builder));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        final LoaderContext loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return parallelBy(keyFunction, JRoutineLoader.with(loaderContext).on(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        checkStatic(wrap(keyFunction), keyFunction);
        return parallelBy(keyFunction, buildRoutine(builder));
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> staleAfter(@Nullable final UnitDuration staleTime) {
        return loaderConfiguration().withResultStaleTime(staleTime).apply();
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {
        return loaderConfiguration().withResultStaleTime(time, timeUnit).apply();
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends LoaderStreamChannel<IN, OUT>>
    streamLoaderConfiguration() {
        return new LoaderConfiguration.Builder<LoaderStreamChannel<IN, OUT>>(mStreamConfigurable,
                mStreamConfiguration.getStreamLoaderConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> with(@Nullable final LoaderContext context) {
        return (LoaderStreamChannel<IN, OUT>) newChannel(newConfiguration(context));
    }

    @NotNull
    private <AFTER> LoaderRoutine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        return builder.invocationConfiguration()
                      .with(null)
                      .with(streamConfiguration.asInvocationConfiguration())
                      .apply()
                      .loaderConfiguration()
                      .with(null)
                      .with(streamConfiguration.asLoaderConfiguration())
                      .apply()
                      .buildRoutine();
    }

    @NotNull
    private LoaderStreamConfiguration newConfiguration(@Nullable final LoaderContext context) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(context,
                loaderStreamConfiguration.getStreamLoaderConfiguration(),
                loaderStreamConfiguration.getCurrentLoaderConfiguration(),
                loaderStreamConfiguration.getStreamConfiguration(),
                loaderStreamConfiguration.getCurrentConfiguration(),
                loaderStreamConfiguration.getInvocationMode());
    }

    @NotNull
    private LoaderStreamConfiguration newConfiguration(
            @NotNull final LoaderConfiguration streamConfiguration,
            @NotNull final LoaderConfiguration configuration) {
        final LoaderStreamConfiguration loaderStreamConfiguration = mStreamConfiguration;
        return new DefaultLoaderStreamConfiguration(loaderStreamConfiguration.getLoaderContext(),
                streamConfiguration, configuration,
                loaderStreamConfiguration.getStreamConfiguration(),
                loaderStreamConfiguration.getCurrentConfiguration(),
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
                mChannelConfiguration =
                        builderFromOutputChannel(asInvocationConfiguration()).apply();
            }

            return mChannelConfiguration;
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {
            if (mInvocationConfiguration == null) {
                mInvocationConfiguration =
                        mStreamConfiguration.builderFrom().with(mCurrentConfiguration).apply();
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
                                                                 .apply();
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

    @NotNull
    @Override
    public LoaderStreamChannel<IN, OUT> apply(@NotNull final LoaderConfiguration configuration) {
        return (LoaderStreamChannel<IN, OUT>) newChannel(
                newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(),
                        configuration));
    }
}
