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

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
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
import com.github.dm.jrt.stream.AbstractStreamBuilder;
import com.github.dm.jrt.stream.StreamBuilder;

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
 * Created by davide-maestroni on 07/04/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamBuilderCompat<IN, OUT> extends AbstractStreamBuilder<IN, OUT>
        implements LoaderStreamBuilderCompat<IN, OUT> {

    private LoaderStreamConfigurationCompat mStreamConfiguration;

    private final InvocationConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilderCompat<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfigurationCompat streamConfiguration =
                            mStreamConfiguration;
                    return DefaultLoaderStreamBuilderCompat.this.apply(
                            newConfiguration(streamConfiguration.getStreamConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>
            mLoaderConfigurable =
            new LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilderCompat<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultLoaderStreamBuilderCompat.this.apply(
                            newConfiguration(mStreamConfiguration.getStreamLoaderConfiguration(),
                                    configuration));
                }
            };

    private final LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>
            mStreamLoaderConfigurable =
            new LoaderConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilderCompat<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {
                    return DefaultLoaderStreamBuilderCompat.this.apply(
                            newConfiguration(configuration,
                                    mStreamConfiguration.getCurrentLoaderConfiguration()));
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamBuilderCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamBuilderCompat<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final LoaderStreamConfigurationCompat streamConfiguration =
                            mStreamConfiguration;
                    return DefaultLoaderStreamBuilderCompat.this.apply(
                            newConfiguration(configuration,
                                    streamConfiguration.getCurrentConfiguration(),
                                    streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     */
    DefaultLoaderStreamBuilderCompat() {
        this(new DefaultLoaderStreamConfigurationCompat(null,
                LoaderConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(),
                InvocationConfiguration.defaultConfiguration(), InvocationMode.ASYNC));
    }

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     */
    DefaultLoaderStreamBuilderCompat(
            @NotNull final LoaderStreamConfigurationCompat streamConfiguration) {
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
    public LoaderStreamBuilderCompat<IN, OUT> append(@Nullable final OUT output) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.append(output);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> append(@Nullable final OUT... outputs) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> append(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.append(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> append(
            @NotNull final Channel<?, ? extends OUT> channel) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.append(channel);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.appendGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> appendGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.appendGet(outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> appendMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.appendMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> appendMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.appendMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> async() {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.async();
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> async(@Nullable final Runner runner) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.async(runner);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.asyncMap(runner);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @NotNull final Backoff backoff) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.backoffOn(runner, limit, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, final long delay, @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.backoffOn(runner, limit, delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> backoffOn(@Nullable final Runner runner,
            final int limit, @Nullable final UnitDuration delay) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.backoffOn(runner, limit, delay);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        checkStatic(decorate(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.collect(accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        checkStatic(decorate(seedSupplier), seedSupplier);
        checkStatic(decorate(accumulateConsumer), accumulateConsumer);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.collect(seedSupplier,
                accumulateConsumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> LoaderStreamBuilderCompat<IN, AFTER> collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        checkStatic(decorate(collectionSupplier), collectionSupplier);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.collectInto(collectionSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> delay(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.delay(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> delay(@NotNull final UnitDuration delay) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.delay(delay);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> filterPredicate) {
        checkStatic(decorate(filterPredicate), filterPredicate);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.filter(filterPredicate);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamBuilder<IN, OUT>, ? extends
                    StreamBuilder<BEFORE, AFTER>> liftFunction) {
        return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.flatLift(liftFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.flatMap(mappingFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.invocationMode(invocationMode);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> lag(final long delay,
            @NotNull final TimeUnit timeUnit) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.lag(delay, timeUnit);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> lag(@NotNull final UnitDuration delay) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.lag(delay);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.lift(liftFunction);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamBuilderCompat<BEFORE, AFTER> liftConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
        return (LoaderStreamBuilderCompat<BEFORE, AFTER>) super.liftConfig(liftFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> limit(final int count) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.limit(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.map(builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        checkStatic(decorate(mappingFunction), mappingFunction);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapAll(mappingFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapAllMore(mappingConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        checkStatic(decorate(mappingConsumer), mappingConsumer);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.mapMore(mappingConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, Void> onComplete(@NotNull final Action completeAction) {
        return (LoaderStreamBuilderCompat<IN, Void>) super.onComplete(completeAction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.onError(errorConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return (LoaderStreamBuilderCompat<IN, Void>) super.onOutput(outputConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElse(@Nullable final OUT output) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElse(output);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElse(@Nullable final OUT... outputs) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElse(
            @Nullable final Iterable<? extends OUT> outputs) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElse(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElseGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElseGet(outputSupplier);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElseMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElseMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElseMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElseMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> orElseThrow(@Nullable final Throwable error) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.orElseThrow(error);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> parallel() {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.parallel();
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> parallel(final int maxInvocations) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.parallel(maxInvocations);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic("factory", factory);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.parallel(count, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic("routine", routine);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.parallel(count, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, builder.buildRoutine());
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.parallelBy(keyFunction, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("routine", routine);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.parallelBy(keyFunction, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, builder.buildRoutine());
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> peekComplete(@NotNull final Action completeAction) {
        checkStatic(decorate(completeAction), completeAction);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.peekComplete(completeAction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> peekError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        checkStatic(decorate(errorConsumer), errorConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.peekError(errorConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> peekOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        checkStatic(decorate(outputConsumer), outputConsumer);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.peekOutput(outputConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        checkStatic(decorate(accumulateFunction), accumulateFunction);
        return (LoaderStreamBuilderCompat<IN, OUT>) super.reduce(accumulateFunction);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        checkStatic(decorate(seedSupplier), seedSupplier);
        checkStatic(decorate(accumulateFunction), accumulateFunction);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.reduce(seedSupplier,
                accumulateFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> retry(final int count) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.retry(count);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> retry(final int count,
            @NotNull final Backoff backoff) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.retry(count, backoff);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.retry(backoffFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> sequential() {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.sequential();
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> skip(final int count) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> sorted(@Nullable final OrderType orderType) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.sorted(orderType);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> straight() {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.straight();
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
    streamInvocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamBuilderCompat<IN, OUT>>(
                mStreamInvocationConfigurable, mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> sync() {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.sync();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThen(@Nullable final AFTER output) {
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThen(output);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThen(@Nullable final AFTER... outputs) {
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThen(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThen(
            @Nullable final Iterable<? extends AFTER> outputs) {
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThen(outputs);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThenGet(final long count,
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThenGet(count, outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        checkStatic(decorate(outputSupplier), outputSupplier);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThenGet(outputSupplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThenMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThenMore(count, outputsConsumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> andThenMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        checkStatic(decorate(outputsConsumer), outputsConsumer);
        return (LoaderStreamBuilderCompat<IN, AFTER>) super.andThenMore(outputsConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.tryCatch(catchFunction);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.tryCatchMore(catchConsumer);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> tryFinally(@NotNull final Action finallyAction) {
        return (LoaderStreamBuilderCompat<IN, OUT>) super.tryFinally(finallyAction);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamBuilderCompat<IN, OUT>>
    invocationConfiguration() {
        return new InvocationConfiguration.Builder<LoaderStreamBuilderCompat<IN, OUT>>(
                mInvocationConfigurable, mStreamConfiguration.getCurrentConfiguration());
    }

    @NotNull
    @Override
    protected LoaderStreamConfigurationCompat newConfiguration(
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
    protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final InvocationFactory<? super BEFORE, ? extends AFTER> factory) {
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

        final ContextInvocationFactory<? super BEFORE, ? extends AFTER> invocationFactory =
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
    protected LoaderStreamConfigurationCompat resetConfiguration(
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
    public ContextInvocationFactory<IN, OUT> buildContextFactory() {
        final InvocationFactory<IN, OUT> factory = buildFactory();
        return factoryFrom(JRoutineCore.with(factory).buildRoutine(), factory.hashCode(),
                InvocationMode.SYNC);
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> cache(
            @Nullable final CacheStrategyType strategyType) {
        return loaderConfiguration().withCacheStrategy(strategyType).applied();
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> factoryId(final int factoryId) {
        return loaderConfiguration().withFactoryId(factoryId).applied();
    }

    @NotNull
    @Override
    public Builder<? extends LoaderStreamBuilderCompat<IN, OUT>> loaderConfiguration() {
        return new Builder<LoaderStreamBuilderCompat<IN, OUT>>(mLoaderConfigurable,
                mStreamConfiguration.getCurrentLoaderConfiguration());
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> loaderId(final int loaderId) {
        return loaderConfiguration().withLoaderId(loaderId).applied();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderContextCompat loaderContext = mStreamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        return map(JRoutineLoaderCompat.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> on(@Nullable final LoaderContextCompat context) {
        return apply(newConfiguration(context));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallel(final int count,
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
    public <AFTER> LoaderStreamBuilderCompat<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {
        final LoaderStreamConfigurationCompat streamConfiguration = mStreamConfiguration;
        final LoaderContextCompat loaderContext = streamConfiguration.getLoaderContext();
        if (loaderContext == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic(decorate(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return parallelBy(keyFunction, JRoutineLoaderCompat.on(loaderContext).with(factory));
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> staleAfter(@Nullable final UnitDuration staleTime) {
        return loaderConfiguration().withResultStaleTime(staleTime).applied();
    }

    @NotNull
    @Override
    public LoaderStreamBuilderCompat<IN, OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {
        return loaderConfiguration().withResultStaleTime(time, timeUnit).applied();
    }

    @NotNull
    @Override
    public Builder<? extends LoaderStreamBuilderCompat<IN, OUT>> streamLoaderConfiguration() {
        return new Builder<LoaderStreamBuilderCompat<IN, OUT>>(mStreamLoaderConfigurable,
                mStreamConfiguration.getStreamLoaderConfiguration());
    }

    /**
     * Applies the specified stream configuration.
     *
     * @param configuration the stream configuration.
     * @return this builder.
     */
    @NotNull
    protected LoaderStreamBuilderCompat<IN, OUT> apply(
            @NotNull final LoaderStreamConfigurationCompat configuration) {
        super.apply(configuration);
        mStreamConfiguration = configuration;
        return this;
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
    private static class DefaultLoaderStreamConfigurationCompat
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
}
