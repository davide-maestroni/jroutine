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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.LocalFence;
import com.github.dm.jrt.core.util.UnitDuration;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Abstract implementation of a stream channel.
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

    private static final LocalFence sNewChannelFence = new LocalFence();

    private static final ImmediateRunner sSequentialRunner = new ImmediateRunner();

    private final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

    private final Object mMutex = new Object();

    private final Channel<?, IN> mSourceChannel;

    private final StreamConfiguration mStreamConfiguration;

    private Channel<?, OUT> mChannel;

    private boolean mIsConcat;

    private final Configurable<StreamChannel<IN, OUT>> mStreamConfigurable =
            new Configurable<StreamChannel<IN, OUT>>() {

                @NotNull
                public StreamChannel<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final StreamConfiguration streamConfiguration = mStreamConfiguration;
                    return newChannel(newConfiguration(configuration,
                            streamConfiguration.getCurrentConfiguration(),
                            streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     */
    protected AbstractStreamChannel(@NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, IN> sourceChannel) {
        mStreamConfiguration =
                ConstantConditions.notNull("stream configuration", streamConfiguration);
        mSourceChannel = ConstantConditions.notNull("source channel", sourceChannel);
        mBindingFunction = null;
    }

    /**
     * Constructor.
     * <p>
     * Note that this constructor can be called only to produce the result of a
     * {@link #newChannel(StreamConfiguration, Channel, Function)} method invocation.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     * @param bindingFunction     if null the stream will act as a wrapper of the source output
     *                            channel.
     * @throws java.lang.IllegalStateException if the constructor is invoked outside the
     *                                         {@code newChannel()} method.
     */
    protected AbstractStreamChannel(@NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, IN> sourceChannel,
            @Nullable final Function<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
        if (!sNewChannelFence.isInside()) {
            throw new IllegalStateException(
                    "the constructor can be called only inside the invocation of the "
                            + "'newChannel()' method");
        }

        mStreamConfiguration =
                ConstantConditions.notNull("stream configuration", streamConfiguration);
        mSourceChannel = ConstantConditions.notNull("source channel", sourceChannel);
        mBindingFunction = (bindingFunction != null) ? wrap(bindingFunction) : null;
    }

    public boolean abort() {
        return bindChannel().abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return bindChannel().abort(reason);
    }

    @NotNull
    public StreamChannel<IN, OUT> after(@NotNull final UnitDuration delay) {
        bindChannel().after(delay);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        bindChannel().after(delay, timeUnit);
        return this;
    }

    @NotNull
    public List<OUT> all() {
        return bindChannel().all();
    }

    @NotNull
    public StreamChannel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        bindChannel().allInto(results);
        return this;
    }

    @NotNull
    public <CHANNEL extends Channel<? super OUT, ?>> CHANNEL bind(@NotNull final CHANNEL channel) {
        return bindChannel().bind(channel);
    }

    @NotNull
    public StreamChannel<IN, OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {
        bindChannel().bind(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> close() {
        return this;
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {
        return bindChannel().eventualIterator();
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyAbort() {
        bindChannel().eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        bindChannel().eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyBreak() {
        bindChannel().eventuallyBreak();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> eventuallyFail() {
        bindChannel().eventuallyFail();
        return this;
    }

    @Nullable
    public RoutineException getError() {
        return bindChannel().getError();
    }

    public boolean hasCompleted() {
        return bindChannel().hasCompleted();
    }

    public boolean hasNext() {
        return bindChannel().hasNext();
    }

    public OUT next() {
        return bindChannel().next();
    }

    @NotNull
    public StreamChannel<IN, OUT> immediately() {
        bindChannel().immediately();
        return this;
    }

    public int inputCount() {
        bindChannel();
        return mSourceChannel.inputCount();
    }

    public boolean isBound() {
        return bindChannel().isBound();
    }

    public boolean isEmpty() {
        return (inputCount() == 0) && (outputCount() == 0);
    }

    public boolean isOpen() {
        bindChannel();
        return mSourceChannel.isOpen();
    }

    @NotNull
    public List<OUT> next(final int count) {
        return bindChannel().next(count);
    }

    public OUT nextOrElse(final OUT output) {
        return bindChannel().nextOrElse(output);
    }

    @NotNull
    public StreamChannel<IN, OUT> orderByCall() {
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> orderByDelay() {
        return this;
    }

    public int outputCount() {
        return bindChannel().outputCount();
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        throw new IllegalStateException("cannot pass data to a stream channel");
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        throw new IllegalStateException("cannot pass data to a stream channel");
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final IN input) {
        throw new IllegalStateException("cannot pass data to a stream channel");
    }

    @NotNull
    public StreamChannel<IN, OUT> pass(@Nullable final IN... inputs) {
        throw new IllegalStateException("cannot pass data to a stream channel");
    }

    public int size() {
        return bindChannel().size();
    }

    @NotNull
    public StreamChannel<IN, OUT> skipNext(final int count) {
        bindChannel().skipNext(count);
        return this;
    }

    public void throwError() {
        bindChannel().throwError();
    }

    @NotNull
    public StreamChannel<IN, OUT> append(@Nullable final OUT output) {
        return append(JRoutineCore.io().of(output));
    }

    @NotNull
    public StreamChannel<IN, OUT> append(@Nullable final OUT... outputs) {
        return append(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<IN, OUT> append(@Nullable final Iterable<? extends OUT> outputs) {
        return append(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<IN, OUT> append(@NotNull final Channel<?, ? extends OUT> channel) {
        return newChannel(getBindingFunction().andThen(
                new BindConcat<OUT>(mStreamConfiguration.asChannelConfiguration(), channel)));
    }

    @NotNull
    public StreamChannel<IN, OUT> appendGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {
        return map(new ConcatLoopSupplierInvocation<OUT>(count, wrap(supplier)));
    }

    @NotNull
    public StreamChannel<IN, OUT> appendGet(@NotNull final Supplier<? extends OUT> supplier) {
        return appendGet(1, supplier);
    }

    @NotNull
    public StreamChannel<IN, OUT> appendGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
        return map(new ConcatLoopConsumerInvocation<OUT>(count, wrap(consumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> appendGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
        return appendGetMore(1, consumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> async() {
        return invocationMode(InvocationMode.ASYNC);
    }

    @NotNull
    public StreamChannel<IN, OUT> async(@Nullable final Runner runner) {
        return async().streamInvocationConfiguration().withRunner(runner).apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> asyncMap(@Nullable final Runner runner) {
        return async(runner).map(IdentityInvocation.<OUT>factoryOf());
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
    public StreamChannel<IN, OUT> bind() {
        bindChannel();
        return this;
    }

    @NotNull
    public StreamChannel<IN, OUT> bindAfter(@NotNull final UnitDuration delay) {
        return bindAfter(delay.value, delay.unit);
    }

    @NotNull
    public StreamChannel<IN, OUT> bindAfter(@NotNull final UnitDuration delay,
            @NotNull final OutputConsumer<OUT> consumer) {
        return bindAfter(delay.value, delay.unit, consumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> bindAfter(final long delay, @NotNull final TimeUnit timeUnit) {
        final Runner runner =
                mStreamConfiguration.asInvocationConfiguration().getRunnerOrElse(null);
        return newChannel(
                new BindDelayed<IN, OUT>(runner, delay, timeUnit, getBindingFunction())).bind();
    }

    @NotNull
    public StreamChannel<IN, OUT> bindAfter(final long delay, @NotNull final TimeUnit timeUnit,
            @NotNull final OutputConsumer<OUT> consumer) {
        final Runner runner =
                mStreamConfiguration.asInvocationConfiguration().getRunnerOrElse(null);
        return newChannel(
                new BindDelayed<IN, OUT>(runner, delay, timeUnit, getBindingFunction())).bind(
                consumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> accumulateConsumer) {
        return map(AccumulateConsumerInvocation.consumerFactory(accumulateConsumer));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> collect(
            @NotNull final Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> accumulateConsumer) {
        return map(AccumulateConsumerInvocation.consumerFactory(seedSupplier, accumulateConsumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER extends Collection<? super OUT>> StreamChannel<IN, AFTER> collectInto(
            @NotNull final Supplier<? extends AFTER> collectionSupplier) {
        return collect(collectionSupplier,
                (BiConsumer<? super AFTER, ? super OUT>) sCollectConsumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> filter(@NotNull final Predicate<? super OUT> filterPredicate) {
        return map(predicateFilter(filterPredicate));
    }

    @NotNull
    public <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> flatLift(
            @NotNull final Function<? super StreamChannel<IN, OUT>, ? extends
                    StreamChannel<BEFORE, AFTER>> transformFunction) {
        try {
            return ConstantConditions.notNull("transformed stream", transformFunction.apply(this));

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);
        }
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        return map(new MapInvocation<OUT, AFTER>(wrap(mappingFunction)));
    }

    @NotNull
    public StreamChannel<IN, OUT> immediate() {
        return async().streamInvocationConfiguration().withRunner(sSequentialRunner).apply();
    }

    @NotNull
    public Builder<? extends StreamChannel<IN, OUT>> invocationConfiguration() {
        return new Builder<StreamChannel<IN, OUT>>(this,
                mStreamConfiguration.getCurrentConfiguration());
    }

    @NotNull
    public StreamChannel<IN, OUT> invocationMode(@NotNull final InvocationMode invocationMode) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        return newChannel(newConfiguration(streamConfiguration.getStreamConfiguration(),
                streamConfiguration.getCurrentConfiguration(), invocationMode));
    }

    public Iterator<OUT> iterator() {
        return bindChannel().iterator();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamChannel<IN, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, AFTER>>> transformFunction) {
        try {
            return newChannel(ConstantConditions.notNull("binding function",
                    ((Function<Function<Channel<?, IN>, Channel<?, OUT>>, Function<Channel<?,
                            IN>, Channel<?, AFTER>>>) transformFunction)
                            .apply(getBindingFunction())));

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER> StreamChannel<IN, AFTER> liftConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, AFTER>>> transformFunction) {
        try {
            return newChannel(ConstantConditions.notNull("binding function",
                    ((BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>,
                            Function<Channel<?, IN>, Channel<?, AFTER>>>) transformFunction)
                            .apply(mStreamConfiguration, getBindingFunction())));

        } catch (final Exception e) {
            throw StreamException.wrapIfNeeded(e);
        }
    }

    @NotNull
    public StreamChannel<IN, OUT> limit(final int count) {
        return map(Operators.<OUT>limit(count));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        return map(functionMapping(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return map(buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        return newChannel(getBindingFunction().andThen(
                new BindMap<OUT, AFTER>(ConstantConditions.notNull("routine instance", routine),
                        mStreamConfiguration.getInvocationMode())));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return map(buildRoutine(builder));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        return map(functionCall(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapAllMore(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        return map(consumerCall(mappingConsumer));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> mapMore(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        return map(consumerMapping(mappingConsumer));
    }

    @NotNull
    public StreamChannel<IN, Void> onComplete(@NotNull final Runnable action) {
        return newChannel(getBindingFunction().andThen(
                new BindCompleteConsumer<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        action)));
    }

    @NotNull
    public StreamChannel<IN, OUT> onError(
            @NotNull final Consumer<? super RoutineException> errorConsumer) {
        return tryCatchMore(new TryCatchBiConsumerConsumer<OUT>(errorConsumer));
    }

    @NotNull
    public StreamChannel<IN, Void> onOutput(@NotNull final Consumer<? super OUT> outputConsumer) {
        return newChannel(getBindingFunction().andThen(
                new BindOutputConsumer<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        wrap(outputConsumer))));
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
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        return map(new OrElseSupplierInvocationFactory<OUT>(count, wrap(outputSupplier)));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGet(@NotNull final Supplier<? extends OUT> outputSupplier) {
        return orElseGet(1, outputSupplier);
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGetMore(final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return map(new OrElseConsumerInvocationFactory<OUT>(count, wrap(outputsConsumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> orElseGetMore(
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        return orElseGetMore(1, outputsConsumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> order(@Nullable final OrderType orderType) {
        return streamInvocationConfiguration().withOutputOrder(orderType).apply();
    }

    @NotNull
    public StreamChannel<IN, OUT> parallel() {
        return invocationMode(InvocationMode.PARALLEL);
    }

    @NotNull
    public StreamChannel<IN, OUT> parallel(final int maxInvocations) {
        return parallel().invocationConfiguration().withMaxInstances(maxInvocations).apply();
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        return parallel(count, new StreamInvocationFactory<OUT, AFTER>(wrap(streamFunction)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return parallel(count, buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        return newChannel(getBindingFunction().andThen(
                new BindParallelCount<OUT, AFTER>(streamConfiguration.asChannelConfiguration(),
                        count, routine, streamConfiguration.getInvocationMode())));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallel(final int count,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallel(count, buildRoutine(builder));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction) {
        return parallelBy(keyFunction,
                new StreamInvocationFactory<OUT, AFTER>(wrap(streamFunction)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return parallelBy(keyFunction, buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        return newChannel(getBindingFunction().andThen(
                new BindParallelKey<OUT, AFTER>(streamConfiguration.asChannelConfiguration(),
                        keyFunction, routine, streamConfiguration.getInvocationMode())));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> parallelBy(
            @NotNull final Function<? super OUT, ?> keyFunction,
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return parallelBy(keyFunction, buildRoutine(builder));
    }

    @NotNull
    public StreamChannel<IN, OUT> peek(@NotNull final Consumer<? super OUT> peekConsumer) {
        return map(new PeekInvocation<OUT>(wrap(peekConsumer)));
    }

    @NotNull
    public StreamChannel<IN, OUT> peekComplete(@NotNull final Runnable peekAction) {
        return map(new PeekCompleteInvocation<OUT>(peekAction));
    }

    @NotNull
    public StreamChannel<IN, OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction) {
        return map(AccumulateFunctionInvocation.functionFactory(accumulateFunction));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> reduce(@NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER>
                    accumulateFunction) {
        return map(AccumulateFunctionInvocation.functionFactory(seedSupplier, accumulateFunction));
    }

    @NotNull
    public StreamChannel<IN, OUT> replay() {
        return newChannel(getBindingFunction().andThen(
                new BindReplay<OUT>(mStreamConfiguration.asChannelConfiguration())));
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
                    backoffFunction) {
        return newChannel(new BindRetry<IN, OUT>(mStreamConfiguration.asChannelConfiguration(),
                getBindingFunction(), backoffFunction));
    }

    @NotNull
    public StreamChannel<IN, ? extends Selectable<OUT>> selectable(final int index) {
        return newChannel(getBindingFunction().andThen(
                new BindSelectable<OUT>(mStreamConfiguration.asChannelConfiguration(), index)));
    }

    @NotNull
    public StreamChannel<IN, OUT> sequential() {
        return invocationMode(InvocationMode.SEQUENTIAL);
    }

    @NotNull
    public StreamChannel<IN, OUT> skip(final int count) {
        return map(Operators.<OUT>skip(count));
    }

    @NotNull
    public Builder<? extends StreamChannel<IN, OUT>> streamInvocationConfiguration() {
        return new Builder<StreamChannel<IN, OUT>>(mStreamConfigurable,
                mStreamConfiguration.getStreamConfiguration());
    }

    @NotNull
    public StreamChannel<IN, OUT> sync() {
        return invocationMode(InvocationMode.SYNC);
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
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        return map(new LoopSupplierInvocation<AFTER>(count, wrap(outputSupplier)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGet(
            @NotNull final Supplier<? extends AFTER> outputSupplier) {
        return thenGet(1, outputSupplier);
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGetMore(final long count,
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        return map(new LoopConsumerInvocation<AFTER>(count, wrap(outputsConsumer)));
    }

    @NotNull
    public <AFTER> StreamChannel<IN, AFTER> thenGetMore(
            @NotNull final Consumer<? super Channel<AFTER, ?>> outputsConsumer) {
        return thenGetMore(1, outputsConsumer);
    }

    @NotNull
    public StreamChannel<IN, OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
        return tryCatchMore(new TryCatchBiConsumerFunction<OUT>(catchFunction));
    }

    @NotNull
    public StreamChannel<IN, OUT> tryCatchMore(
            @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>>
                    catchConsumer) {
        return newChannel(getBindingFunction().andThen(
                new BindTryCatch<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        wrap(catchConsumer))));
    }

    @NotNull
    public StreamChannel<IN, OUT> tryFinally(@NotNull final Runnable action) {
        return newChannel(getBindingFunction().andThen(
                new BindTryFinally<OUT>(mStreamConfiguration.asChannelConfiguration(),
                        ConstantConditions.notNull("runnable instance", action))));
    }

    public void remove() {
        bindChannel().remove();
    }

    /**
     * Creates a new stream channel instance.
     * <p>
     * Inheriting class must always employ this method to create new instances.
     *
     * @param streamConfiguration the stream configuration.
     * @return the new channel instance.
     */
    @NotNull
    protected StreamChannel<IN, OUT> newChannel(
            @NotNull final StreamConfiguration streamConfiguration) {
        synchronized (mMutex) {
            if (mIsConcat) {
                throw new IllegalStateException("the channel has already been concatenated");
            }

            mIsConcat = true;
        }

        final LocalFence fence = sNewChannelFence;
        fence.enter();
        try {
            return ConstantConditions.notNull("new stream channel instance",
                    newChannel(streamConfiguration, mSourceChannel, getBindingFunction()));

        } finally {
            fence.exit();
        }
    }

    /**
     * Creates a new channel instance.
     * <p>
     * Note that this method should be never directly called by the implementing class.
     *
     * @param streamConfiguration the stream configuration.
     * @param sourceChannel       the source channel.
     * @param bindingFunction     the binding function.
     * @param <BEFORE>            the concatenation input type.
     * @param <AFTER>             the concatenation output type.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> newChannel(
            @NotNull final StreamConfiguration streamConfiguration,
            @NotNull final Channel<?, BEFORE> sourceChannel,
            @NotNull final Function<Channel<?, BEFORE>, Channel<?, AFTER>> bindingFunction);

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
     * Creates a new stream configuration instance where the current configuration is reset to its
     * defaults.
     *
     * @param streamConfiguration the stream invocation configuration.
     * @param invocationMode      the invocation mode.
     * @return the newly created configuration instance.
     */
    @NotNull
    protected abstract StreamConfiguration newConfiguration(
            @NotNull InvocationConfiguration streamConfiguration,
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
    protected abstract <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull StreamConfiguration streamConfiguration,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    @NotNull
    @SuppressWarnings("unchecked")
    private Channel<?, OUT> bindChannel() {
        final boolean isBind;
        synchronized (mMutex) {
            if (mIsConcat) {
                throw new IllegalStateException("the channel has already been concatenated");
            }

            isBind = (mChannel == null);
        }

        if (isBind) {
            if (mBindingFunction == null) {
                mChannel = (Channel<?, OUT>) mSourceChannel;

            } else {
                final Channel<IN, IN> inputChannel = JRoutineCore.io().buildChannel();
                try {
                    mChannel = mBindingFunction.apply(inputChannel);

                } catch (final Exception e) {
                    inputChannel.abort(InvocationException.wrapIfNeeded(e));
                    throw StreamException.wrapIfNeeded(e);
                }

                inputChannel.pass(mSourceChannel).close();
            }
        }

        return mChannel;
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        return builder.invocationConfiguration()
                      .with(null)
                      .with(mStreamConfiguration.asInvocationConfiguration())
                      .apply()
                      .buildRoutine();
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        return ConstantConditions.notNull("routine instance",
                newRoutine(mStreamConfiguration, factory));
    }

    @NotNull
    private FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> getBindingFunction() {
        final FunctionWrapper<Channel<?, IN>, Channel<?, OUT>> bindingFunction = mBindingFunction;
        return (bindingFunction != null) ? bindingFunction
                : wrap(Functions.<Channel<?, IN>, Channel<?, OUT>>castTo(
                        new ClassToken<Channel<?, OUT>>() {}));
    }

    @NotNull
    private <AFTER> StreamChannel<IN, AFTER> newChannel(
            @NotNull final Function<Channel<?, IN>, Channel<?, AFTER>> bindingFunction) {
        synchronized (mMutex) {
            if (mIsConcat) {
                throw new IllegalStateException("the channel has already been concatenated");
            }

            mIsConcat = true;
        }

        final LocalFence fence = sNewChannelFence;
        fence.enter();
        try {
            final StreamConfiguration streamConfiguration = mStreamConfiguration;
            return ConstantConditions.notNull("new stream channel instance", newChannel(
                    newConfiguration(streamConfiguration.getStreamConfiguration(),
                            streamConfiguration.getInvocationMode()), mSourceChannel,
                    bindingFunction));

        } finally {
            fence.exit();
        }
    }

    @NotNull
    public StreamChannel<IN, OUT> apply(@NotNull final InvocationConfiguration configuration) {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        return newChannel(
                newConfiguration(streamConfiguration.getStreamConfiguration(), configuration,
                        streamConfiguration.getInvocationMode()));
    }
}
