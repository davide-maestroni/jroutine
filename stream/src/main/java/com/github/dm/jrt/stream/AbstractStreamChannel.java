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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Abstract implementation of a stream output channel.
 * <p>
 * This class provides a default implementation of all the stream channel features. The inheriting
 * class just needs to create routine and channel instances when required.
 * <p>
 * Created by davide-maestroni on 01/12/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractStreamChannel<IN, OUT>
        implements StreamChannel<IN, OUT>, Configurable<StreamChannel<IN, OUT>> {

    private static final BiConsumer<? extends Collection<?>, ?> sCollectConsumer =
            new BiConsumer<Collection<Object>, Object>() {

                public void accept(final Collection<Object> outs, final Object out) {

                    outs.add(out);
                }
            };

    private static final SequentialRunner sSequentialRunner = new SequentialRunner();

    private static FunctionWrapper<? extends OutputChannel<?>, ? extends OutputChannel<?>>
            sIdentity = wrap(new Function<OutputChannel<?>, OutputChannel<?>>() {

        public OutputChannel<?> apply(final OutputChannel<?> channel) {

            return channel;
        }
    });

    private final FunctionWrapper<OutputChannel<IN>, OutputChannel<OUT>> mBind;

    private final Object mMutex = new Object();

    private final OutputChannel<IN> mSourceChannel;

    private OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

    private InvocationMode mInvocationMode;

    private boolean mIsBound;

    private InvocationConfiguration mStreamConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private final Configurable<StreamChannel<IN, OUT>> mStreamConfigurable =
            new Configurable<StreamChannel<IN, OUT>>() {

                @NotNull
                public StreamChannel<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return AbstractStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param configuration  the initial invocation configuration.
     * @param invocationMode the delegation type.
     * @param sourceChannel  the source output channel.
     * @param bindFunction   if null the stream will act as a wrapper of the source output channel.
     */
    @SuppressWarnings("unchecked")
    protected AbstractStreamChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<IN> sourceChannel,
            @Nullable final Function<OutputChannel<IN>, OutputChannel<OUT>> bindFunction) {

        mStreamConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        mSourceChannel = ConstantConditions.notNull("source channel", sourceChannel);
        mBind = (bindFunction != null) ? wrap(bindFunction)
                : (FunctionWrapper<OutputChannel<IN>, OutputChannel<OUT>>) sIdentity;
    }

    public boolean abort() {

        return bind().abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return bind().abort(reason);
    }

    public boolean isEmpty() {

        return bind().isEmpty();
    }

    public boolean isOpen() {

        return bind().isOpen();
    }

    public int size() {

        return bind().size();
    }

    @NotNull
    public StreamChannel<IN, OUT> afterMax(@NotNull final UnitDuration timeout) {

        bind().afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        bind().afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {

        bind().allInto(results);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {

        bind().bind(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyAbort() {

        bind().eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {

        bind().eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyExit() {

        bind().eventuallyExit();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyThrow() {

        bind().eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> immediately() {

        bind().immediately();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> skipNext(final int count) {

        bind().skipNext(count);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> async() {

        mInvocationMode = InvocationMode.ASYNC;
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @NotNull final Backoff backoff) {

        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(backoff)
                                        .apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            final long delay, @NotNull final TimeUnit timeUnit) {

        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(delay, timeUnit)
                                        .apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @Nullable final UnitDuration delay) {

        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(delay)
                                        .apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> consumer) {

        return map(AccumulateConsumerInvocation.consumerFactory(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> consumer) {

        return map(AccumulateConsumerInvocation.consumerFactory(supplier, consumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER extends Collection<? super OUT>> StreamChannel<IN, AFTER> collectIn(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return collect(supplier, (BiConsumer<? super AFTER, ? super OUT>) sCollectConsumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> concat(@Nullable final OUT output) {

        return concat(JRoutineCore.io().of(output));
    }

    @NotNull
    public StreamChannel<IN, OUT> concat(@Nullable final OUT... outputs) {

        return concat(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<IN, OUT> concat(@Nullable final Iterable<? extends OUT> outputs) {

        return concat(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<IN, OUT> concat(@NotNull final OutputChannel<? extends OUT> channel) {

        return buildChannel(
                mBind.andThen(new BindConcat<OUT>(buildChannelConfiguration(), channel)));
    }

    @NotNull
    public StreamChannel<IN, OUT> concatGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {

        return map(new ConcatLoopSupplierInvocation<OUT>(count, wrap(supplier)));
    }

    @NotNull
    public StreamChannel<IN, OUT> concatGet(@NotNull final Supplier<? extends OUT> supplier) {

        return concatGet(1, supplier);
    }

    @NotNull
    public StreamChannel<IN, OUT> concatGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return map(new ConcatLoopConsumerInvocation<OUT>(count, wrap(consumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> concatGetMore(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return concatGetMore(1, consumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        return map(predicateFilter(predicate));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return map(new MapInvocation<OUT, AFTER>(wrap(function)));
    }

    @NotNull
    public <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> flatTransform(
            @NotNull final Function<? super StreamChannel<IN, OUT>, ? extends
                    StreamChannel<BEFORE, AFTER>> function) {

        try {
            return ConstantConditions.notNull("transformed stream", function.apply(this));

        } catch (final Exception e) {
            throw StreamException.wrap(e);
        }
    }

    @NotNull
    public Builder<? extends StreamChannel<IN, OUT>> invocationConfiguration() {

        return new Builder<StreamChannel<IN, OUT>>(this, mConfiguration);
    }

    @NotNull
    public StreamChannel<IN, OUT> invocationMode(@NotNull final InvocationMode invocationMode) {

        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> limit(final int count) {

        return map(new LimitInvocationFactory<OUT>(count));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return map(functionMapping(function));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return map(buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return buildChannel(mBind.andThen(
                new BindMap<OUT, AFTER>(ConstantConditions.notNull("routine instance", routine),
                        mInvocationMode)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return map(builder.invocationConfiguration()
                          .with(null)
                          .with(buildConfiguration())
                          .apply()
                          .buildRoutine());
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        return map(functionCall(function));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        return map(consumerCall(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return map(consumerMapping(consumer));
    }

    @NotNull
    public StreamChannel<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> consumer) {

        return tryCatchMore(new TryCatchBiConsumerConsumer<OUT>(consumer));
    }

    @NotNull
    public StreamChannel<IN, Void> onOutput(@NotNull final Consumer<? super OUT> consumer) {

        return buildChannel(mBind.andThen(
                new BindOutputConsumer<OUT>(buildChannelConfiguration(), wrap(consumer))));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElse(@Nullable final OUT output) {

        return map(new OrElseInvocationFactory<OUT>(Collections.singletonList(output)));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElse(@Nullable final OUT... outputs) {

        final List<OUT> list;
        if (outputs != null) {
            list = new ArrayList<OUT>();
            Collections.addAll(list, outputs);

        } else {
            list = Collections.emptyList();
        }

        return map(new OrElseInvocationFactory<OUT>(list));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElse(@Nullable final Iterable<? extends OUT> outputs) {

        final List<OUT> list;
        if (outputs != null) {
            list = new ArrayList<OUT>();
            for (final OUT output : outputs) {
                list.add(output);
            }

        } else {
            list = Collections.emptyList();
        }

        return map(new OrElseInvocationFactory<OUT>(list));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {

        return map(new OrElseSupplierInvocationFactory<OUT>(count, wrap(supplier)));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGet(@NotNull final Supplier<? extends OUT> supplier) {

        return orElseGet(1, supplier);
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return map(new OrElseConsumerInvocationFactory<OUT>(count, wrap(consumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return orElseGetMore(1, consumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> ordered(@Nullable final OrderType orderType) {

        return streamInvocationConfiguration().withOutputOrder(orderType).apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> parallel() {

        mInvocationMode = InvocationMode.PARALLEL;
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> parallel(final int maxInvocations) {

        return parallel().invocationConfiguration().withMaxInstances(maxInvocations).apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> peek(@NotNull final Consumer<? super OUT> consumer) {

        return map(new PeekInvocation<OUT>(wrap(consumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return map(AccumulateFunctionInvocation.functionFactory(function));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> reduce(@NotNull Supplier<? extends AFTER> supplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function) {

        return map(AccumulateFunctionInvocation.functionFactory(supplier, function));
    }

    @NotNull
    public StreamChannel<IN, OUT> replay() {

        return buildChannel(mBind.andThen(new BindReplay<OUT>(buildChannelConfiguration())));
    }

    @NotNull
    public StreamChannel<IN, OUT> retry(final int count) {

        return retry(count, Backoffs.zeroDelay());
    }

    @NotNull
    public StreamChannel<IN, OUT> retry(final int count, @NotNull final Backoff backoff) {

        return retry(new RetryBackoff(count, backoff));
    }

    @NotNull
    public StreamChannel<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    function) {

        return buildChannel(new BindRetry<IN, OUT>(buildChannelConfiguration(), mBind, function));
    }

    @NotNull
    public StreamChannel<IN, OUT> runOn(@Nullable final Runner runner) {

        final InvocationMode invocationMode = mInvocationMode;
        final MappingInvocation<OUT, OUT> factory = IdentityInvocation.factoryOf();
        final StreamChannel<IN, OUT> channel =
                streamInvocationConfiguration().withRunner(runner).apply().async().map(factory);
        if (invocationMode == InvocationMode.ASYNC) {
            return channel.async();
        }

        if (invocationMode == InvocationMode.PARALLEL) {
            return channel.parallel();
        }

        if (invocationMode == InvocationMode.SYNC) {
            return channel.sync();
        }

        return channel.serial();
    }

    @NotNull
    public StreamChannel<IN, OUT> runOnShared() {

        return runOn(null);
    }

    @NotNull
    public StreamChannel<IN, OUT> runSequentially() {

        return streamInvocationConfiguration().withRunner(sSequentialRunner).apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> serial() {

        mInvocationMode = InvocationMode.SERIAL;
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamChannel<IN, AFTER> simpleTransform(
            @NotNull final Function<? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<OUT>>, ? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<AFTER>>> function) {

        try {
            return buildChannel(ConstantConditions.notNull("binding function",
                    ((Function<Function<OutputChannel<IN>, OutputChannel<OUT>>,
                            Function<OutputChannel<IN>, OutputChannel<AFTER>>>) function)
                            .apply(mBind)));

        } catch (final Exception e) {
            throw StreamException.wrap(e);
        }
    }

    @NotNull
    public StreamChannel<IN, OUT> skip(final int count) {

        return map(new SkipInvocationFactory<OUT>(count));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> function) {

        return splitBy(keyFunction, new StreamInvocationFactory<OUT, AFTER>(wrap(function)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return splitBy(keyFunction, buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return buildChannel(mBind.andThen(
                new BindSplitKey<OUT, AFTER>(buildChannelConfiguration(), keyFunction, routine,
                        mInvocationMode)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return splitBy(keyFunction, builder.invocationConfiguration()
                                           .with(null)
                                           .with(buildConfiguration())
                                           .apply()
                                           .buildRoutine());
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(final int count,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> function) {

        return splitBy(count, new StreamInvocationFactory<OUT, AFTER>(wrap(function)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return splitBy(count, buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        return buildChannel(mBind.andThen(
                new BindSplitCount<OUT, AFTER>(buildChannelConfiguration(), count, routine,
                        mInvocationMode)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> splitBy(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {

        return splitBy(count, builder.invocationConfiguration()
                                     .with(null)
                                     .with(buildConfiguration())
                                     .apply()
                                     .buildRoutine());
    }

    @NotNull
    public StreamChannel<IN, OUT> start() {

        bind();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> startAfter(@NotNull final UnitDuration delay) {

        return startAfter(delay.value, delay.unit);
    }

    @NotNull
    public StreamChannel<IN, OUT> startAfter(final long delay, @NotNull final TimeUnit timeUnit) {

        final Runner runner = buildConfiguration().getRunnerOrElse(Runners.sharedRunner());
        runner.run(new BindExecution(this), delay, timeUnit);
        return this;
    }

    @NotNull
    public Builder<? extends StreamChannel<IN, OUT>> streamInvocationConfiguration() {

        return new Builder<StreamChannel<IN, OUT>>(mStreamConfigurable, mStreamConfiguration);
    }

    @NotNull
    public StreamChannel<IN, OUT> sync() {

        mInvocationMode = InvocationMode.SYNC;
        return this;
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> then(@Nullable final AFTER output) {

        return map(new GenerateOutputInvocation<AFTER>(Collections.singletonList(output)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> then(@Nullable final AFTER... outputs) {

        final List<AFTER> list;
        if (outputs != null) {
            list = new ArrayList<AFTER>();
            Collections.addAll(list, outputs);

        } else {
            list = Collections.emptyList();
        }

        return map(new GenerateOutputInvocation<AFTER>(list));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> then(
            @Nullable final Iterable<? extends AFTER> outputs) {

        final List<AFTER> list;
        if (outputs != null) {
            list = new ArrayList<AFTER>();
            for (final AFTER output : outputs) {
                list.add(output);
            }

        } else {
            list = Collections.emptyList();
        }

        return map(new GenerateOutputInvocation<AFTER>(list));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return map(new LoopSupplierInvocation<AFTER>(count, wrap(supplier)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return thenGet(1, supplier);
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return map(new LoopConsumerInvocation<AFTER>(count, wrap(consumer)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return thenGetMore(1, consumer);
    }

    @NotNull
    public StreamChannel<IN, ? extends Selectable<OUT>> toSelectable(final int index) {

        return buildChannel(
                mBind.andThen(new BindSelectable<OUT>(buildChannelConfiguration(), index)));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamChannel<IN, AFTER> transform(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    OutputChannel<IN>, ? extends OutputChannel<OUT>>, ? extends Function<? super
                    OutputChannel<IN>, ? extends OutputChannel<AFTER>>> function) {

        try {
            return buildChannel(ConstantConditions.notNull("binding function",
                    ((BiFunction<StreamConfiguration, Function<OutputChannel<IN>,
                            OutputChannel<OUT>>, Function<OutputChannel<IN>,
                            OutputChannel<AFTER>>>) function)
                            .apply(newConfiguration(mStreamConfiguration, mConfiguration,
                                    mInvocationMode), mBind)));

        } catch (final Exception e) {
            throw StreamException.wrap(e);
        }
    }

    @NotNull
    public StreamChannel<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return tryCatchMore(new TryCatchBiConsumerFunction<OUT>(function));
    }

    @NotNull
    public StreamChannel<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return buildChannel(
                mBind.andThen(new BindTryCatch<OUT>(buildChannelConfiguration(), wrap(consumer))));
    }

    @NotNull
    public StreamChannel<IN, OUT> tryFinally(@NotNull final Runnable runnable) {

        return buildChannel(mBind.andThen(new BindTryFinally<OUT>(buildChannelConfiguration(),
                ConstantConditions.notNull("runnable instance", runnable))));
    }

    @NotNull
    public List<OUT> all() {

        return bind().all();
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL bind(
            @NotNull final CHANNEL channel) {

        return bind().bind(channel);
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {

        return bind().eventualIterator();
    }

    @Nullable
    public RoutineException getError() {

        return bind().getError();
    }

    public boolean hasCompleted() {

        return bind().hasCompleted();
    }

    public boolean hasNext() {

        return bind().hasNext();
    }

    public OUT next() {

        return bind().next();
    }

    public boolean isBound() {

        return bind().isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        return bind().next(count);
    }

    public OUT nextOrElse(final OUT output) {

        return bind().nextOrElse(output);
    }

    public void throwError() {

        bind().throwError();
    }

    public Iterator<OUT> iterator() {

        return bind().iterator();
    }

    public void remove() {

        bind().remove();
    }

    /**
     * Builds a channel configuration from the stream one.
     *
     * @return the channel configuration.
     */
    @NotNull
    protected ChannelConfiguration buildChannelConfiguration() {

        return builderFromOutputChannel(buildConfiguration()).apply();
    }

    /**
     * Builds an invocation configuration from the stream one.
     *
     * @return the invocation configuration.
     */
    @NotNull
    protected InvocationConfiguration buildConfiguration() {

        return mStreamConfiguration.builderFrom().with(getConfiguration()).apply();
    }

    /**
     * Returns the configuration which will be used by the next routine concatenated to the stream.
     *
     * @return the configuration.
     */
    @NotNull
    protected InvocationConfiguration getConfiguration() {

        return mConfiguration;
    }

    /**
     * Returns the invocation mode used by all the routines concatenated to the stream.
     *
     * @return the invocation mode.
     */
    @NotNull
    protected InvocationMode getInvocationMode() {

        return mInvocationMode;
    }

    /**
     * Returns the configuration used by all the routines concatenated to the stream.
     *
     * @return the configuration.
     */
    @NotNull
    protected InvocationConfiguration getStreamConfiguration() {

        return mStreamConfiguration;
    }

    /**
     * Creates a new channel instance.
     * <p>
     * Note that this method should be never directly called by the implementing class.
     *
     * @param <BEFORE>            the concatenation input type.
     * @param <AFTER>             the concatenation output type.
     * @param streamConfiguration the stream configuration.
     * @param invocationMode      the invocation mode.
     * @param sourceChannel       the source output channel.
     * @param bind                the binding function.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> newChannel(
            @NotNull InvocationConfiguration streamConfiguration,
            @NotNull InvocationMode invocationMode, @NotNull OutputChannel<BEFORE> sourceChannel,
            @NotNull Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> bind);

    /**
     * Creates a new stream configuration instance.
     * <p>
     * Note that this method should be never directly called by the implementing class.
     *
     * @param streamConfiguration the stream invocation configuration.
     * @param configuration       the invocation configuration.
     * @param invocationMode      the invocation mode.
     * @return the newly created configuration instance.
     */
    @NotNull
    protected StreamConfiguration newConfiguration(
            @NotNull final InvocationConfiguration streamConfiguration,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode) {

        return new DefaultStreamConfiguration(streamConfiguration, configuration, invocationMode);
    }

    /**
     * Creates a new routine instance based on the specified factory.
     * <p>
     * Note that this method should be never directly called by the implementing class.
     *
     * @param <AFTER>       the concatenation output type.
     * @param configuration the routine configuration.
     * @param factory       the invocation factory.
     * @return the newly created routine instance.
     */
    @NotNull
    protected abstract <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull InvocationConfiguration configuration,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    @NotNull
    @SuppressWarnings("unchecked")
    private OutputChannel<OUT> bind() {

        final boolean isBind;
        synchronized (mMutex) {
            if (mIsBound) {
                throw new IllegalStateException("the channel is already bound");
            }

            isBind = (mChannel == null);
        }

        if (isBind) {
            if (mBind == sIdentity) {
                mChannel = (OutputChannel<OUT>) mSourceChannel;

            } else {
                final IOChannel<IN> inputChannel = JRoutineCore.io().buildChannel();
                try {
                    mChannel = mBind.apply(inputChannel);

                } catch (final Exception e) {
                    inputChannel.abort(InvocationException.wrapIfNeeded(e));
                    throw StreamException.wrap(e);
                }

                inputChannel.pass(mSourceChannel).close();
            }
        }

        return mChannel;
    }

    @NotNull
    private <AFTER> StreamChannel<IN, AFTER> buildChannel(
            @NotNull final Function<OutputChannel<IN>, OutputChannel<AFTER>> bind) {

        synchronized (mMutex) {
            mIsBound = true;
        }

        return newChannel(mStreamConfiguration, mInvocationMode, mSourceChannel, bind);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(buildConfiguration(), factory);
    }

    private static class BindExecution implements Execution {

        private final AbstractStreamChannel<?, ?> mChannel;

        private BindExecution(@NotNull final AbstractStreamChannel<?, ?> channel) {

            mChannel = channel;
        }

        public void run() {

            try {
                mChannel.bind();

            } catch (final Throwable t) {
                InvocationInterruptedException.throwIfInterrupt(t);
            }
        }
    }

    /**
     * Default implementation of a stream configuration.
     */
    private static class DefaultStreamConfiguration implements StreamConfiguration {

        private final InvocationConfiguration mConfiguration;

        private final InvocationMode mInvocationMode;

        private final InvocationConfiguration mStreamConfiguration;

        private ChannelConfiguration mChannelConfiguration;

        private InvocationConfiguration mInvocationConfiguration;

        /**
         * Constructor.
         *
         * @param streamConfiguration the stream invocation configuration.
         * @param configuration       the invocation configuration.
         * @param invocationMode      the invocation mode.
         */
        private DefaultStreamConfiguration(
                @NotNull final InvocationConfiguration streamConfiguration,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationMode invocationMode) {

            mStreamConfiguration = streamConfiguration;
            mConfiguration = configuration;
            mInvocationMode = invocationMode;
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
                        mStreamConfiguration.builderFrom().with(mConfiguration).apply();
            }

            return mInvocationConfiguration;
        }

        @NotNull
        public InvocationMode getInvocationMode() {

            return mInvocationMode;
        }
    }

    @NotNull
    public StreamChannel<IN, OUT> apply(@NotNull final InvocationConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }
}
