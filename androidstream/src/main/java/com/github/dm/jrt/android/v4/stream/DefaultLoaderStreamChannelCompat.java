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

import android.support.annotation.NonNull;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Configurable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat.LoaderBuilderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
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
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Default implementation of a loader stream output channel.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderStreamChannelCompat<IN, OUT> extends AbstractStreamChannel<IN, OUT>
        implements LoaderStreamChannelCompat<IN, OUT>,
        Configurable<LoaderStreamChannelCompat<IN, OUT>> {

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>
            mInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {

                    DefaultLoaderStreamChannelCompat.super.invocationConfiguration()
                                                          .with(null)
                                                          .with(configuration)
                                                          .apply();
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    private final InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>
            mStreamInvocationConfigurable =
            new InvocationConfiguration.Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
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

    private LoaderContextCompat mLoaderContext;

    private LoaderConfiguration mStreamConfiguration;

    private final Configurable<LoaderStreamChannelCompat<IN, OUT>> mStreamConfigurable =
            new Configurable<LoaderStreamChannelCompat<IN, OUT>>() {

                @NotNull
                public LoaderStreamChannelCompat<IN, OUT> apply(
                        @NotNull final LoaderConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return DefaultLoaderStreamChannelCompat.this;
                }
            };

    /**
     * Constructor.
     *
     * @param sourceChannel the source output channel.
     */
    DefaultLoaderStreamChannelCompat(@NotNull final OutputChannel<IN> sourceChannel) {

        this(null, InvocationConfiguration.defaultConfiguration(),
                LoaderConfiguration.defaultConfiguration(), InvocationMode.ASYNC, sourceChannel,
                null);
    }

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param invocationMode          the invocation mode.
     * @param sourceChannel           the source output channel.
     * @param bindFunction            if null the stream will act as a wrapper of the source output
     *                                channel.
     */
    private DefaultLoaderStreamChannelCompat(@Nullable final LoaderBuilderCompat builder,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<IN> sourceChannel,
            @Nullable final Function<OutputChannel<IN>, OutputChannel<OUT>> bindFunction) {

        super(invocationConfiguration, invocationMode, sourceChannel, bindFunction);
        mContextBuilder = builder;
        mStreamConfiguration =
                ConstantConditions.notNull("loader configuration", loaderConfiguration);
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
    public LoaderStreamChannelCompat<IN, OUT> afterMax(@NotNull final UnitDuration timeout) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.afterMax(timeout);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> afterMax(final long timeout,
            @NotNull final TimeUnit timeUnit) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.afterMax(timeout, timeUnit);
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
            @NotNull final OutputConsumer<? super OUT> consumer) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.bind(consumer);
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
    public LoaderStreamChannelCompat<IN, OUT> eventuallyExit() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyExit();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> eventuallyThrow() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.eventuallyThrow();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> immediately() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.immediately();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> skipNext(final int count) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.skipNext(count);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> async() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.async();
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
            @NotNull final BiConsumer<? super OUT, ? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.collect(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> consumer) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.collect(supplier, consumer);
    }

    @NotNull
    @Override
    public <AFTER extends Collection<? super OUT>> LoaderStreamChannelCompat<IN, AFTER> collectIn(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.collectIn(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concat(@Nullable final OUT output) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.concat(output);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concat(@Nullable final OUT... outputs) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.concat(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concat(
            @Nullable final Iterable<? extends OUT> outputs) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.concat(outputs);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concat(
            @NotNull final OutputChannel<? extends OUT> channel) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.concat(channel);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concatGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.concatGet(count, supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concatGet(
            @NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.concatGet(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concatGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.concatGetMore(count, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> concatGetMore(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.concatGetMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> predicate) {

        checkStatic(wrap(predicate), predicate);
        return (LoaderStreamChannelCompat<IN, OUT>) super.filter(predicate);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.flatMap(function);
    }

    @NotNull
    @Override
    public <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> flatTransform(
            @NotNull final Function<? super StreamChannel<IN, OUT>, ? extends
                    StreamChannel<BEFORE, AFTER>> function) {

        return (LoaderStreamChannelCompat<BEFORE, AFTER>) super.flatTransform(function);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    invocationConfiguration() {

        final InvocationConfiguration config = getConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mInvocationConfigurable, config);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> limit(final int count) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.limit(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.map(function);
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
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapAll(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapAllMore(consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.mapMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.onError(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, Void>) super.onOutput(consumer);
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
            @NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGet(count, supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGet(supplier);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGetMore(count, consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.orElseGetMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> ordered(@Nullable final OrderType orderType) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.ordered(orderType);
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
    public LoaderStreamChannelCompat<IN, OUT> peek(@NotNull final Consumer<? super OUT> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.peek(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, OUT>) super.reduce(function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function) {

        checkStatic(wrap(supplier), supplier);
        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.reduce(supplier, function);
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
                    function) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.retry(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> runOn(@Nullable final Runner runner) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.runOn(runner);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> runOnShared() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.runOnShared();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> runSequentially() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.runSequentially();
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> serial() {

        return (LoaderStreamChannelCompat<IN, OUT>) super.serial();
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> simpleTransform(
            @NotNull final Function<? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<OUT>>, ? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<AFTER>>> function) {

        return (LoaderStreamChannelCompat<IN, AFTER>) super.simpleTransform(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> skip(final int count) {

        return (LoaderStreamChannelCompat<IN, OUT>) super.skip(count);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> function) {

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(keyFunction, function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(keyFunction, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("routine", routine);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(keyFunction, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {

        checkStatic(wrap(keyFunction), keyFunction);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(keyFunction, builder);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(count, function);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        checkStatic("factory", factory);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(count, factory);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        checkStatic("routine", routine);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(count, routine);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return (LoaderStreamChannelCompat<IN, AFTER>) super.splitBy(count, builder);
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamInvocationConfiguration() {

        final InvocationConfiguration config = getStreamConfiguration();
        return new InvocationConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mStreamInvocationConfigurable, config);
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
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGet(count, supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> supplier) {

        checkStatic(wrap(supplier), supplier);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGet(supplier);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGetMore(count, consumer);
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.thenGetMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, ? extends ParcelableSelectable<OUT>> toSelectable(
            final int index) {

        return transform(new SelectableTransform<IN, OUT>(index));
    }

    @NotNull
    @Override
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> transform(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    OutputChannel<IN>, ? extends OutputChannel<OUT>>, ? extends Function<? super
                    OutputChannel<IN>, ? extends OutputChannel<AFTER>>> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, AFTER>) super.transform(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        checkStatic(wrap(function), function);
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryCatch(function);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        checkStatic(wrap(consumer), consumer);
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryCatchMore(consumer);
    }

    @NotNull
    @Override
    public LoaderStreamChannelCompat<IN, OUT> tryFinally(@NotNull final Runnable runnable) {

        checkStatic("runnable", runnable);
        return (LoaderStreamChannelCompat<IN, OUT>) super.tryFinally(runnable);
    }

    @NotNull
    @Override
    protected <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> newChannel(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        return newChannel(streamConfiguration, mStreamConfiguration, invocationMode, sourceChannel,
                invoke);
    }

    @NotNull
    @Override
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {

        return new DefaultLoaderStreamConfigurationCompat(mLoaderContext, mStreamConfiguration,
                mConfiguration,
                super.newConfiguration(streamConfiguration, configuration, invocationMode));
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(configuration, buildLoaderConfiguration(), factory);
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> cache(
            @Nullable final CacheStrategyType strategyType) {

        return loaderConfiguration().withCacheStrategy(strategyType).apply();
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> factoryId(final int factoryId) {

        return loaderConfiguration().withFactoryId(factoryId).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    loaderConfiguration() {

        final LoaderConfiguration config = mConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(this, config);
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> loaderId(final int loaderId) {

        return loaderConfiguration().withLoaderId(loaderId).apply();
    }

    @NotNull
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilderCompat contextBuilder = mContextBuilder;
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
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return map(builder.invocationConfiguration()
                          .with(null)
                          .with(buildConfiguration())
                          .apply()
                          .loaderConfiguration()
                          .with(null)
                          .with(buildLoaderConfiguration())
                          .apply()
                          .buildRoutine());
    }

    @NotNull
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilderCompat contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic(wrap(keyFunction), keyFunction);
        checkStatic("factory", factory);
        return splitBy(keyFunction, contextBuilder.on(factory)
                                                  .invocationConfiguration()
                                                  .with(buildConfiguration())
                                                  .apply()
                                                  .loaderConfiguration()
                                                  .with(buildLoaderConfiguration())
                                                  .apply()
                                                  .buildRoutine());
    }

    @NotNull
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {

        checkStatic(wrap(keyFunction), keyFunction);
        return splitBy(keyFunction, builder.invocationConfiguration()
                                           .with(null)
                                           .with(buildConfiguration())
                                           .apply()
                                           .loaderConfiguration()
                                           .with(null)
                                           .with(buildLoaderConfiguration())
                                           .apply()
                                           .buildRoutine());
    }

    @NotNull
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final ContextInvocationFactory<? super OUT, ? extends AFTER> factory) {

        final LoaderBuilderCompat contextBuilder = mContextBuilder;
        if (contextBuilder == null) {
            throw new IllegalStateException("the loader context is null");
        }

        checkStatic("factory", factory);
        return splitBy(count, contextBuilder.on(factory)
                                            .invocationConfiguration()
                                            .with(buildConfiguration())
                                            .apply()
                                            .loaderConfiguration()
                                            .with(buildLoaderConfiguration())
                                            .apply()
                                            .buildRoutine());
    }

    @NotNull
    public <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(final int count,
            @NotNull final LoaderRoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return splitBy(count, builder.invocationConfiguration()
                                     .with(null)
                                     .with(buildConfiguration())
                                     .apply()
                                     .loaderConfiguration()
                                     .with(null)
                                     .with(buildLoaderConfiguration())
                                     .apply()
                                     .buildRoutine());
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> staleAfter(@Nullable final UnitDuration staleTime) {

        return loaderConfiguration().withResultStaleTime(staleTime).apply();
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> staleAfter(final long time,
            @NotNull final TimeUnit timeUnit) {

        return loaderConfiguration().withResultStaleTime(time, timeUnit).apply();
    }

    @NotNull
    public LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamLoaderConfiguration() {

        final LoaderConfiguration config = mStreamConfiguration;
        return new LoaderConfiguration.Builder<LoaderStreamChannelCompat<IN, OUT>>(
                mStreamConfigurable, config);
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> with(@Nullable final LoaderContextCompat context) {

        mLoaderContext = context;
        mContextBuilder = (context != null) ? JRoutineLoaderCompat.with(context) : null;
        return this;
    }

    @NonNull
    private LoaderConfiguration buildLoaderConfiguration() {

        return mStreamConfiguration.builderFrom().with(mConfiguration).apply();
    }

    @NotNull
    private <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> newChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        return new DefaultLoaderStreamChannelCompat<BEFORE, AFTER>(mContextBuilder,
                invocationConfiguration, loaderConfiguration, invocationMode, sourceChannel,
                invoke);
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

    /**
     * Default implementation of a loader stream configuration.
     */
    protected static class DefaultLoaderStreamConfigurationCompat
            implements LoaderStreamConfigurationCompat {

        private final LoaderConfiguration mConfiguration;

        private final LoaderContextCompat mLoaderContext;

        private final LoaderConfiguration mStreamConfiguration;

        private final StreamConfiguration mWrappedConfiguration;

        private LoaderConfiguration mLoaderConfiguration;

        /**
         * Constructor.
         *
         * @param context                   the loader context.
         * @param streamLoaderConfiguration the stream loader configuration.
         * @param loaderConfiguration       the loader configuration.
         * @param streamConfiguration       the stream invocation configuration.
         */
        protected DefaultLoaderStreamConfigurationCompat(
                @Nullable final LoaderContextCompat context,
                @NotNull final LoaderConfiguration streamLoaderConfiguration,
                @NotNull final LoaderConfiguration loaderConfiguration,
                @NotNull final StreamConfiguration streamConfiguration) {

            mLoaderContext = context;
            mStreamConfiguration = streamLoaderConfiguration;
            mConfiguration = loaderConfiguration;
            mWrappedConfiguration = streamConfiguration;
        }

        @NotNull
        public ChannelConfiguration asChannelConfiguration() {

            return mWrappedConfiguration.asChannelConfiguration();
        }

        @NotNull
        public InvocationConfiguration asInvocationConfiguration() {

            return mWrappedConfiguration.asInvocationConfiguration();
        }

        @NotNull
        public InvocationMode getInvocationMode() {

            return mWrappedConfiguration.getInvocationMode();
        }

        @NotNull
        public LoaderConfiguration asLoaderConfiguration() {

            if (mLoaderConfiguration == null) {
                mLoaderConfiguration =
                        mStreamConfiguration.builderFrom().with(mConfiguration).apply();
            }

            return mLoaderConfiguration;
        }

        @Nullable
        public LoaderContextCompat getLoaderContext() {

            return mLoaderContext;
        }
    }

    @NotNull
    public LoaderStreamChannelCompat<IN, OUT> apply(
            @NotNull final LoaderConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("loader configuration", configuration);
        return this;
    }
}
