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
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
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
import com.github.dm.jrt.function.Decorator;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.AbstractStreamRoutineBuilder;
import com.github.dm.jrt.stream.StreamRoutineBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
class DefaultStreamLoaderRoutineBuilder<IN, OUT> extends AbstractStreamRoutineBuilder<IN, OUT>
        implements StreamLoaderRoutineBuilder<IN, OUT> {

    private LoaderStreamConfiguration mStreamConfiguration;

    private final InvocationConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamLoaderRoutineBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return DefaultStreamLoaderRoutineBuilder.this.apply(
                            newConfiguration(streamConfiguration.getStreamConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final LoaderConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>
            mLoaderConfigurable =
            new LoaderConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamLoaderRoutineBuilder<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultStreamLoaderRoutineBuilder.this.apply(mStreamConfiguration =
                            newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(),
                                    configuration));
                }
            };

    private final LoaderConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>
            mStreamLoaderConfigurable =
            new LoaderConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamLoaderRoutineBuilder<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultStreamLoaderRoutineBuilder.this.apply(
                            newConfiguration(configuration,
                                    mStreamConfiguration.getCurrentLoaderConfiguration()));
                }
            };

    private final InvocationConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<StreamLoaderRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamLoaderRoutineBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
                    return DefaultStreamLoaderRoutineBuilder.this.apply(
                            newConfiguration(configuration,
                                    streamConfiguration.getCurrentConfiguration(),
                                    streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     */
    DefaultStreamLoaderRoutineBuilder() {
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
    DefaultStreamLoaderRoutineBuilder(
            @NotNull final LoaderStreamConfiguration streamConfiguration) {
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
    public StreamLoaderRoutineBuilder<IN, OUT> append(@Nullable final OUT output) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.append(output);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> append(@Nullable final OUT... outputs) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> append(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> append(
            @NotNull final Channel<?, ? extends OUT> channel) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.append(channel);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.appendGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> appendGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.appendGet(outputSupplier);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> appendGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.appendGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> appendGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.appendGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> async() {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.async();
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> async(@Nullable final Runner runner) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.async(runner);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.asyncMap(runner);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @NotNull final Backoff backoff) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.backoffOn(runner, limit, backoff);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, final long delay, @NotNull final TimeUnit timeUnit) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.backoffOn(runner, limit, delay,
                timeUnit);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @Nullable final UnitDuration delay) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.backoffOn(runner, limit, delay);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        checkStatic(decorate(accumulateConsumer), accumulateConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.collect(accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        checkStatic(decorate(seedSupplier), seedSupplier);
        checkStatic(decorate(accumulateConsumer), accumulateConsumer);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.collect(seedSupplier,
                accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> StreamLoaderRoutineBuilder<IN, AFTER>
    collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        checkStatic(decorate(collectionSupplier), collectionSupplier);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.collectInto(collectionSupplier);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> delay(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.delay(delay, timeUnit);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> delay(@NotNull final UnitDuration delay) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.delay(delay);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> filterPredicate) {
        checkStatic(decorate(filterPredicate), filterPredicate);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.filter(filterPredicate);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamRoutineBuilder<IN, OUT>, ? extends
                    StreamRoutineBuilder<BEFORE, AFTER>> liftFunction) {
        return (StreamLoaderRoutineBuilder<BEFORE, AFTER>) super.flatLift(liftFunction);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.flatMap(mappingFunction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.invocationMode(invocationMode);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> lag(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.lag(delay, timeUnit);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> lag(@NotNull final UnitDuration delay) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.lag(delay);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        return (StreamLoaderRoutineBuilder<BEFORE, AFTER>) super.lift(liftFunction);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> liftConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
        return (StreamLoaderRoutineBuilder<BEFORE, AFTER>) super.liftConfig(liftFunction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> limit(final int count) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.limit(count);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.map(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.map(builder);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.mapAll(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.mapAllMore(mappingConsumer);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.mapMore(mappingConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, Void> onComplete(@NotNull final Action completeAction) {
        return (StreamLoaderRoutineBuilder<IN, Void>) super.onComplete(completeAction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.onError(errorConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return (StreamLoaderRoutineBuilder<IN, Void>) super.onOutput(outputConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElse(@Nullable final OUT output) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElse(output);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElse(@Nullable final OUT... outputs) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElse(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElseGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElseGet(outputSupplier);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElseGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElseGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> orElseThrow(@Nullable final Throwable error) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.orElseThrow(error);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> parallel() {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.parallel();
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> parallel(final int maxInvocations) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.parallel(maxInvocations);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.parallel(count, factory);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.parallel(count, routine);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, builder.buildRoutine());
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.parallelBy(keyFunction, factory);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("routine", routine);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.parallelBy(keyFunction, routine);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, builder.buildRoutine());
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> peekComplete(@NotNull final Action completeAction) {
        checkStatic(decorate(completeAction), completeAction);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.peekComplete(completeAction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> peekError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        checkStatic(decorate(errorConsumer), errorConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.peekError(errorConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> peekOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        checkStatic(decorate(outputConsumer), outputConsumer);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.peekOutput(outputConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        checkStatic(decorate(accumulateFunction), accumulateFunction);
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.reduce(accumulateFunction);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        checkStatic(decorate(seedSupplier), seedSupplier);
        checkStatic(decorate(accumulateFunction), accumulateFunction);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.reduce(seedSupplier,
                accumulateFunction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> retry(final int count) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.retry(count);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> retry(final int count,
            @NotNull final Backoff backoff) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.retry(count, backoff);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.retry(backoffFunction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> sequential() {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.sequential();
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> skip(final int count) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> sorted(@Nullable final OrderType orderType) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.sorted(orderType);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> straight() {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.straight();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    streamInvocationConfiguration() {
        return new InvocationConfiguration.Builder<StreamLoaderRoutineBuilder<IN, OUT>>(
                mStreamInvocationConfigurable, mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> sync() {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(@Nullable final AFTER output) {
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.then(output);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(@Nullable final AFTER... outputs) {
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.then(outputs);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.thenGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.thenGet(outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.thenGetMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (StreamLoaderRoutineBuilder<IN, AFTER>) super.thenGetMore(outputsConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.tryCatch(catchFunction);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.tryCatchMore(catchConsumer);
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> tryFinally(@NotNull final Action finallyAction) {
        return (StreamLoaderRoutineBuilder<IN, OUT>) super.tryFinally(finallyAction);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    invocationConfiguration() {
        return new InvocationConfiguration.Builder<StreamLoaderRoutineBuilder<IN, OUT>>(
                mInvocationConfigurable, mStreamConfiguration.getCurrentConfiguration());
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
                               .applied()
                               .buildRoutine();
        }

        final ContextInvocationFactory<? super BEFORE, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutineCore.with(factory).buildRoutine(), factory.hashCode(),
                        InvocationMode.SYNC);
        return JRoutineLoader.on(loaderContext)
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
    public StreamLoaderRoutineBuilder<IN, OUT> cache(
            @Nullable final CacheStrategyType strategyType) {
        return loaderConfiguration().withCacheStrategy(strategyType).applied();
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> factoryId(final int factoryId) {
        return loaderConfiguration().withFactoryId(factoryId).applied();
    }

    @NotNull
    @Override
    public LoaderConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    loaderConfiguration() {
        return new LoaderConfiguration.Builder<StreamLoaderRoutineBuilder<IN, OUT>>(
                mLoaderConfigurable, mStreamConfiguration.getCurrentLoaderConfiguration());
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> loaderId(final int loaderId) {
        return loaderConfiguration().withLoaderId(loaderId).applied();
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderContext loaderContext = mStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(JRoutineLoader.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> on(@Nullable final LoaderContext context) {
        return apply(newConfiguration(context));
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        final LoaderContext loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic("factory", factory);
        return parallel(count, JRoutineLoader.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfiguration streamConfiguration = mStreamConfiguration;
        final LoaderContext loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return parallelBy(keyFunction, JRoutineLoader.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> staleAfter(@Nullable final UnitDuration staleTime) {
        return loaderConfiguration().withResultStaleTime(staleTime).applied();
    }

    @NotNull
    @Override
    public StreamLoaderRoutineBuilder<IN, OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {
        return loaderConfiguration().withResultStaleTime(time, timeUnit).applied();
    }

    @NotNull
    @Override
    public Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>> streamLoaderConfiguration() {
        return new LoaderConfiguration.Builder<StreamLoaderRoutineBuilder<IN, OUT>>(
                mStreamLoaderConfigurable, mStreamConfiguration.getStreamLoaderConfiguration());
    }

    /**
     * Applies the specified stream configuration.
     *
     * @param configuration the stream configuration.
     * @return this builder.
     */
    @NotNull
    protected StreamLoaderRoutineBuilder<IN, OUT> apply(
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
                      .applied()
                      .loaderConfiguration()
                      .with(null)
                      .with(streamConfiguration.asLoaderConfiguration())
                      .applied()
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
