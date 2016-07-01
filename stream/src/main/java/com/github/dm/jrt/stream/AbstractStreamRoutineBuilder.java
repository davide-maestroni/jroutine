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

import com.github.dm.jrt.core.ChannelInvocation;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.builder.TemplateRoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.operator.Operators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Created by davide-maestroni on 07/01/2016.
 */
public abstract class AbstractStreamRoutineBuilder<IN, OUT> extends TemplateRoutineBuilder<IN, OUT>
        implements StreamRoutineBuilder<IN, OUT> {

    private static final BiConsumer<? extends Collection<?>, ?> sCollectConsumer =
            new BiConsumer<Collection<Object>, Object>() {

                public void accept(final Collection<Object> outs, final Object out) {
                    outs.add(out);
                }
            };

    private static final StraightRunner sSequentialRunner = new StraightRunner();

    private FunctionWrapper<? extends Channel<?, ?>, ? extends Channel<?, ?>> mBindingFunction;

    private StreamConfiguration mStreamConfiguration;

    private final Configurable<StreamRoutineBuilder<IN, OUT>> mConfigurable =
            new Configurable<StreamRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamRoutineBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final StreamConfiguration streamConfiguration = mStreamConfiguration;
                    mStreamConfiguration =
                            newConfiguration(streamConfiguration.getStreamConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode());
                    return AbstractStreamRoutineBuilder.this;
                }
            };

    private final Configurable<StreamRoutineBuilder<IN, OUT>> mStreamConfigurable =
            new Configurable<StreamRoutineBuilder<IN, OUT>>() {

                @NotNull
                public StreamRoutineBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final StreamConfiguration streamConfiguration = mStreamConfiguration;
                    mStreamConfiguration = newConfiguration(configuration,
                            streamConfiguration.getCurrentConfiguration(),
                            streamConfiguration.getInvocationMode());
                    return AbstractStreamRoutineBuilder.this;
                }
            };

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     */
    protected AbstractStreamRoutineBuilder(@NotNull final StreamConfiguration streamConfiguration) {
        mStreamConfiguration =
                ConstantConditions.notNull("stream configuration", streamConfiguration);
        mBindingFunction = null;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> append(@Nullable final OUT output) {
        return append(JRoutineCore.io().of(output));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> append(@Nullable final OUT... outputs) {
        return append(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> append(@Nullable final Iterable<? extends OUT> outputs) {
        return append(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> append(@NotNull final Channel<?, ? extends OUT> channel) {
        mBindingFunction = getBindingFunction().andThen(
                new BindConcat<OUT>(mStreamConfiguration.asChannelConfiguration(), channel));
        resetConfiguration();
        return this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return map(new ConcatLoopSupplierInvocation<OUT>(count, wrap(outputSupplier)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> appendGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return appendGet(1, outputSupplier);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> appendGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return map(new ConcatLoopConsumerInvocation<OUT>(count, wrap(outputsConsumer)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> appendGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return appendGetMore(1, outputsConsumer);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> async() {
        return invocationMode(InvocationMode.ASYNC);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> async(@Nullable final Runner runner) {
        return async().streamInvocationConfiguration().withRunner(runner).applied();
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return async(runner).map(IdentityInvocation.<OUT>factoryOf());
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @NotNull final Backoff backoff) {
        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(backoff)
                                        .applied();
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            final long delay, @NotNull final TimeUnit timeUnit) {
        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(delay, timeUnit)
                                        .applied();
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable final Runner runner, final int limit,
            @Nullable final UnitDuration delay) {
        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(limit)
                                        .withInputBackoff(delay)
                                        .applied();
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        return map(AccumulateConsumerInvocation.consumerFactory(accumulateConsumer));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        return map(AccumulateConsumerInvocation.consumerFactory(seedSupplier, accumulateConsumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER extends Collection<? super OUT>> StreamRoutineBuilder<IN, AFTER> collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        return collect(collectionSupplier,
                (BiConsumer<? super AFTER, ? super OUT>) sCollectConsumer);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> delay(final long delay, @NotNull final TimeUnit timeUnit) {
        mBindingFunction = getBindingFunction().andThen(
                new BindDelay<OUT>(mStreamConfiguration.asChannelConfiguration(), delay, timeUnit));
        resetConfiguration();
        return this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> delay(@NotNull final UnitDuration delay) {
        return delay(delay.value, delay.unit);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> filter(
            @NotNull final Predicate<? super OUT> filterPredicate) {
        return map(predicateFilter(filterPredicate));
    }

    @NotNull
    public <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamRoutineBuilder<IN, OUT>, ? extends
                    StreamRoutineBuilder<BEFORE, AFTER>> liftFunction) {
        try {
            return ConstantConditions.notNull("transformed stream", liftFunction.apply(this));

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);
        }
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        return map(new MapInvocation<OUT, AFTER>(wrap(mappingFunction)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> invocationMode(
            @NotNull final InvocationMode invocationMode) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        mStreamConfiguration = ConstantConditions.notNull("stream configuration",
                newConfiguration(streamConfiguration.getStreamConfiguration(),
                        streamConfiguration.getCurrentConfiguration(), invocationMode));
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public StreamRoutineBuilder<IN, OUT> lag(final long delay, @NotNull final TimeUnit timeUnit) {
        mBindingFunction = getBindingFunction().<Channel<?, IN>>compose(
                new BindDelay<IN>(mStreamConfiguration.asChannelConfiguration(), delay, timeUnit));
        resetConfiguration();
        return this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> lag(@NotNull final UnitDuration delay) {
        return lag(delay.value, delay.unit);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        try {
            mBindingFunction =
                    wrap(((Function<Function<Channel<?, IN>, Channel<?, OUT>>,
                            Function<Channel<?, BEFORE>, Channel<?, AFTER>>>) liftFunction)
                            .apply(getBindingFunction()));
            return (StreamRoutineBuilder<BEFORE, AFTER>) this;

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);

        } finally {
            resetConfiguration();
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> liftConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
        try {
            mBindingFunction =
                    wrap(((BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?,
                            OUT>>, Function<Channel<?, BEFORE>, Channel<?, AFTER>>>) liftFunction)
                            .apply(mStreamConfiguration, getBindingFunction()));
            return (StreamRoutineBuilder<BEFORE, AFTER>) this;

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);

        } finally {
            resetConfiguration();
        }
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> limit(final int count) {
        return map(Operators.<OUT>limit(count));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        return map(functionMapping(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return map(buildRoutine(factory));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        mBindingFunction = getBindingFunction().andThen(
                new BindMap<OUT, AFTER>(routine, mStreamConfiguration.getInvocationMode()));
        resetConfiguration();
        return (StreamRoutineBuilder<IN, AFTER>) this;
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        return map(functionCall(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        return map(consumerCall(mappingConsumer));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        return map(consumerMapping(mappingConsumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public StreamRoutineBuilder<IN, Void> onComplete(@NotNull final Action completeAction) {
        mBindingFunction = getBindingFunction().andThen(
                new BindCompleteConsumer<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        completeAction));
        resetConfiguration();
        return (StreamRoutineBuilder<IN, Void>) this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return tryCatchMore(new TryCatchBiConsumerConsumer<OUT>(errorConsumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public StreamRoutineBuilder<IN, Void> onOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        mBindingFunction = getBindingFunction().andThen(
                new BindOutputConsumer<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        outputConsumer));
        resetConfiguration();
        return (StreamRoutineBuilder<IN, Void>) this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> orElse(@Nullable final OUT output) {
        return map(new OrElseInvocationFactory<OUT>(Collections.singletonList(output)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> orElse(@Nullable final OUT... outputs) {
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
    public StreamRoutineBuilder<IN, OUT> orElse(@Nullable final Iterable<? extends OUT> outputs) {
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
    public StreamRoutineBuilder<IN, OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return map(new OrElseSupplierInvocationFactory<OUT>(count, wrap(outputSupplier)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> orElseGet(
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return orElseGet(1, outputSupplier);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return map(new OrElseConsumerInvocationFactory<OUT>(count, wrap(outputsConsumer)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return orElseGetMore(1, outputsConsumer);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> parallel() {
        return invocationMode(InvocationMode.PARALLEL);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> parallel(final int maxInvocations) {
        return parallel().invocationConfiguration().withMaxInstances(maxInvocations).applied();
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return parallel(count, buildRoutine(factory));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        mBindingFunction = getBindingFunction().andThen(
                new BindParallelCount<OUT, AFTER>(streamConfiguration.asChannelConfiguration(),
                        count, routine, streamConfiguration.getInvocationMode()));
        resetConfiguration();
        return (StreamRoutineBuilder<IN, AFTER>) this;
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, buildRoutine(builder));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return parallelBy(keyFunction, buildRoutine(factory));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        mBindingFunction = getBindingFunction().andThen(
                new BindParallelKey<OUT, AFTER>(streamConfiguration.asChannelConfiguration(),
                        keyFunction, routine, streamConfiguration.getInvocationMode()));
        resetConfiguration();
        return (StreamRoutineBuilder<IN, AFTER>) this;
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, buildRoutine(builder));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> peekComplete(@NotNull final Action completeAction) {
        return map(new PeekCompleteInvocation<OUT>(wrap(completeAction)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> peekError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return map(new PeekErrorInvocationFactory<OUT>(wrap(errorConsumer)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> peekOutput(
            @NotNull final Consumer<? super OUT> outputConsumer) {
        return map(new PeekOutputInvocation<OUT>(wrap(outputConsumer)));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        return map(AccumulateFunctionInvocation.functionFactory(accumulateFunction));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> reduce(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        return map(AccumulateFunctionInvocation.functionFactory(seedSupplier, accumulateFunction));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> retry(final int count) {
        return retry(count, Backoffs.zeroDelay());
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> retry(final int count, @NotNull final Backoff backoff) {
        return retry(new RetryBackoff(count, backoff));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> retry(
            @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction) {
        mBindingFunction =
                wrap(new BindRetry<IN, OUT>(mStreamConfiguration.asChannelConfiguration(),
                        getBindingFunction(), backoffFunction));
        resetConfiguration();
        return this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> sequential() {
        return invocationMode(InvocationMode.SEQUENTIAL);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> skip(final int count) {
        return map(Operators.<OUT>skip(count));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> sort(@Nullable final OrderType orderType) {
        return streamInvocationConfiguration().withOutputOrder(orderType).applied();
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> straight() {
        return async().streamInvocationConfiguration().withRunner(sSequentialRunner).applied();
    }

    @NotNull
    public Builder<? extends StreamRoutineBuilder<IN, OUT>> streamInvocationConfiguration() {
        return new Builder<StreamRoutineBuilder<IN, OUT>>(mStreamConfigurable,
                mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> sync() {
        return invocationMode(InvocationMode.SYNC);
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> then(@Nullable final AFTER output) {
        return map(new GenerateOutputInvocation<AFTER>(Collections.singletonList(output)));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> then(@Nullable final AFTER... outputs) {
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
    public <AFTER> StreamRoutineBuilder<IN, AFTER> then(
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
    public <AFTER> StreamRoutineBuilder<IN, AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        return map(new LoopSupplierInvocation<AFTER>(count, wrap(outputSupplier)));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        return thenGet(1, outputSupplier);
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        return map(new LoopConsumerInvocation<AFTER>(count, wrap(outputsConsumer)));
    }

    @NotNull
    public <AFTER> StreamRoutineBuilder<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        return thenGetMore(1, outputsConsumer);
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return tryCatchMore(new TryCatchBiConsumerFunction<OUT>(catchFunction));
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        mBindingFunction = getBindingFunction().andThen(
                new BindTryCatch<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        catchConsumer));
        resetConfiguration();
        return this;
    }

    @NotNull
    public StreamRoutineBuilder<IN, OUT> tryFinally(@NotNull final Action finallyAction) {
        mBindingFunction = getBindingFunction().andThen(
                new BindTryFinally<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        finallyAction));
        resetConfiguration();
        return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Routine<IN, OUT> buildRoutine() {
        final Routine<? super IN, ? extends OUT> routine =
                ConstantConditions.notNull("routine instance", newRoutine(mStreamConfiguration,
                        new StreamInvocationFactory<IN, OUT>(getBindingFunction())));
        resetConfiguration();
        return (Routine<IN, OUT>) routine;
    }

    @NotNull
    @Override
    public Builder<? extends StreamRoutineBuilder<IN, OUT>> invocationConfiguration() {
        return new Builder<StreamRoutineBuilder<IN, OUT>>(mConfigurable,
                mStreamConfiguration.getCurrentConfiguration());
    }

    /**
     * Creates a new stream configuration instance.
     *
     * @param streamConfiguration  the stream invocation configuration.
     * @param currentConfiguration the current invocation configuration.
     * @param invocationMode       the invocation mode.
     * @return the newly created configuration instance.
     */
    @NotNull
    protected abstract StreamConfiguration newConfiguration(
            @NotNull InvocationConfiguration streamConfiguration,
            @NotNull InvocationConfiguration currentConfiguration,
            @NotNull InvocationMode invocationMode);

    /**
     * Creates a new routine instance based on the specified factory.
     * <p>
     * Note that this method should be never directly called by the implementing class.
     *
     * @param streamConfiguration the stream configuration.
     * @param factory             the invocation factory.
     * @param <AFTER>             the concatenation output type.
     * @return the newly created routine instance.
     */
    @NotNull
    protected abstract <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
            @NotNull StreamConfiguration streamConfiguration,
            @NotNull InvocationFactory<? super BEFORE, ? extends AFTER> factory);

    /**
     * Creates a new stream configuration instance where the current one is reset to the its
     * default options.
     *
     * @param streamConfiguration the stream invocation configuration.
     * @param invocationMode      the invocation mode.
     * @return the newly created configuration instance.
     */
    @NotNull
    protected abstract StreamConfiguration resetConfiguration(
            @NotNull InvocationConfiguration streamConfiguration,
            @NotNull InvocationMode invocationMode);

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return builder.invocationConfiguration()
                      .with(null)
                      .with(mStreamConfiguration.asInvocationConfiguration())
                      .applied()
                      .buildRoutine();
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return ConstantConditions.notNull("routine instance",
                newRoutine(mStreamConfiguration, factory));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> getBindingFunction() {
        final FunctionWrapper<? extends Channel<?, ?>, ? extends Channel<?, ?>> bindingFunction =
                mBindingFunction;
        return (bindingFunction != null)
                ? (FunctionWrapper<Channel<?, IN>, Channel<?, OUT>>) bindingFunction
                : Functions.<Channel<?, IN>, Channel<?, OUT>>castTo(
                        new ClassToken<Channel<?, OUT>>() {});
    }

    private void resetConfiguration() {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        mStreamConfiguration = resetConfiguration(streamConfiguration.getStreamConfiguration(),
                streamConfiguration.getInvocationMode());
    }

    // TODO: 01/07/16 javadoc
    private static class StreamInvocation<IN, OUT> extends ChannelInvocation<IN, OUT> {

        private final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

        private StreamInvocation(
                @NotNull final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
            mBindingFunction = bindingFunction;
        }

        @NotNull
        @Override
        protected Channel<?, OUT> onChannel(@NotNull final Channel<?, IN> channel) throws
                Exception {
            return mBindingFunction.apply(channel);
        }
    }

    // TODO: 01/07/16 javadoc
    private static class StreamInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final StreamInvocation<IN, OUT> mInvocation;

        private StreamInvocationFactory(
                @NotNull final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
            super(asArgs(bindingFunction));
            mInvocation = new StreamInvocation<IN, OUT>(bindingFunction);
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() throws Exception {
            return mInvocation;
        }
    }
}
