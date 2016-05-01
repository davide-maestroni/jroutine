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

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.Configurable;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.OperationInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerOperation;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionOperation;
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
 * @param <OUT> the output data type.
 */
public abstract class AbstractStreamChannel<OUT>
        implements StreamChannel<OUT>, Configurable<StreamChannel<OUT>> {

    private static final Binder NO_OP = new Binder() {

        public void bind() {

        }
    };

    private static final BiConsumer<? extends Collection<?>, ?> sCollectConsumer =
            new BiConsumer<Collection<Object>, Object>() {

                public void accept(final Collection<Object> outs, final Object out) {

                    outs.add(out);
                }
            };

    private final Binder mBinder;

    private final OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

    private InvocationMode mInvocationMode;

    private InvocationConfiguration mStreamConfiguration =
            InvocationConfiguration.defaultConfiguration();

    private final Configurable<StreamChannel<OUT>> mStreamConfigurable =
            new Configurable<StreamChannel<OUT>>() {

                @NotNull
                public StreamChannel<OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {

                    mStreamConfiguration = configuration;
                    return AbstractStreamChannel.this;
                }
            };

    /**
     * Constructor.
     *
     * @param channel        the wrapped output channel.
     * @param configuration  the initial invocation configuration.
     * @param invocationMode the delegation type.
     * @param binder         the binding runnable.
     */
    protected AbstractStreamChannel(@NotNull final OutputChannel<OUT> channel,
            @NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

        mStreamConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        mChannel = ConstantConditions.notNull("output channel", channel);
        mBinder = (binder != null) ? binder : NO_OP;
    }

    public boolean abort() {

        mBinder.bind();
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        mBinder.bind();
        return mChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mChannel.isEmpty();
    }

    public boolean isOpen() {

        return mChannel.isOpen();
    }

    @NotNull
    public StreamChannel<OUT> afterMax(@NotNull final UnitDuration timeout) {

        mChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        mBinder.bind();
        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {

        mBinder.bind();
        mChannel.bind(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort() {

        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyExit() {

        mChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyThrow() {

        mChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> immediately() {

        mChannel.immediately();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> skipNext(final int count) {

        mBinder.bind();
        mChannel.skipNext(count);
        return this;
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> apply(
            @NotNull final Function<? super StreamChannel<OUT>, ? extends OutputChannel<AFTER>>
                    function) {

        final OutputChannel<AFTER> outputChannel = function.apply(this);
        if (getClass().isAssignableFrom(outputChannel.getClass())) {
            return (StreamChannel<AFTER>) outputChannel;
        }

        return buildChannel(outputChannel);
    }

    @NotNull
    public StreamChannel<OUT> async() {

        mInvocationMode = InvocationMode.ASYNC;
        return this;
    }

    @NotNull
    public StreamChannel<OUT> backPressureOn(@Nullable final Runner runner, final int maxInputs,
            final long maxDelay, @NotNull final TimeUnit timeUnit) {

        return backPressureOn(runner, maxInputs, fromUnit(maxDelay, timeUnit));
    }

    @NotNull
    public StreamChannel<OUT> backPressureOn(@Nullable final Runner runner, final int maxInputs,
            @Nullable final UnitDuration maxDelay) {

        return invocationConfiguration().withRunner(runner)
                                        .withInputLimit(maxInputs)
                                        .withInputMaxDelay(maxDelay)
                                        .applyConfiguration();
    }

    @NotNull
    public StreamChannel<OUT> collect(
            @NotNull final BiConsumer<? super OUT, ? super OUT> consumer) {

        return map(AccumulateConsumerInvocation.consumerFactory(consumer));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <AFTER extends Collection<? super OUT>> StreamChannel<AFTER> collect(
            @NotNull final Supplier<? extends AFTER> supplier) {

        return collect(supplier, (BiConsumer<? super AFTER, ? super OUT>) sCollectConsumer);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> collect(@NotNull final Supplier<? extends AFTER> supplier,
            @NotNull final BiConsumer<? super AFTER, ? super OUT> consumer) {

        return map(AccumulateConsumerInvocation.consumerFactory(supplier, consumer));
    }

    @NotNull
    public StreamChannel<OUT> concat(@Nullable final OUT output) {

        return concat(JRoutineCore.io().of(output));
    }

    @NotNull
    public StreamChannel<OUT> concat(@Nullable final OUT... outputs) {

        return concat(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<OUT> concat(@Nullable final Iterable<? extends OUT> outputs) {

        return concat(JRoutineCore.io().of(outputs));
    }

    @NotNull
    public StreamChannel<OUT> concat(@NotNull final OutputChannel<? extends OUT> channel) {

        return buildChannel(Channels.<OUT>concat(this, channel).channelConfiguration()
                                                               .with(buildChannelConfiguration())
                                                               .applyConfiguration()
                                                               .buildChannels());
    }

    @NotNull
    public StreamChannel<OUT> filter(@NotNull final Predicate<? super OUT> predicate) {

        return map(predicateFilter(predicate));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends OutputChannel<? extends AFTER>>
                    function) {

        return map(new MapInvocation<OUT, AFTER>(wrap(function)));
    }

    @NotNull
    public Builder<? extends StreamChannel<OUT>> invocationConfiguration() {

        return new Builder<StreamChannel<OUT>>(this, mConfiguration);
    }

    @NotNull
    public StreamChannel<OUT> limit(final int count) {

        return map(new LimitInvocationFactory<OUT>(count));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer) {

        return map(consumerOperation(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> function) {

        return map(functionOperation(function));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return map(buildRoutine(factory));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {

        final InvocationMode invocationMode = mInvocationMode;
        final InvocationChannel<? super OUT, ? extends AFTER> channel;
        if (invocationMode == InvocationMode.ASYNC) {
            channel = routine.asyncInvoke();

        } else if (invocationMode == InvocationMode.PARALLEL) {
            channel = routine.parallelInvoke();

        } else if (invocationMode == InvocationMode.SYNC) {
            channel = routine.syncInvoke();

        } else {
            channel = routine.serialInvoke();
        }

        return concatRoutine(channel);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> mapAll(
            @NotNull final BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer) {

        return map(consumerCall(consumer));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> function) {

        return map(functionCall(function));
    }

    @NotNull
    public StreamChannel<OUT> onError(@NotNull final Consumer<? super RoutineException> consumer) {

        return tryCatch(new TryCatchBiConsumerConsumer<OUT>(consumer));
    }

    @NotNull
    public StreamChannel<Void> onOutput(@NotNull final Consumer<? super OUT> consumer) {

        return map(new ConsumerInvocation<OUT>(wrap(consumer)));
    }

    @NotNull
    public StreamChannel<OUT> orElse(@Nullable final OUT output) {

        return map(new OrElseInvocationFactory<OUT>(Collections.singletonList(output)));
    }

    @NotNull
    public StreamChannel<OUT> orElse(@Nullable final OUT... outputs) {

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
    public StreamChannel<OUT> orElse(@Nullable final Iterable<? extends OUT> outputs) {

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
    public StreamChannel<OUT> orElseGet(final long count,
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return map(new OrElseConsumerInvocationFactory<OUT>(count, wrap(consumer)));
    }

    @NotNull
    public StreamChannel<OUT> orElseGet(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return orElseGet(1, consumer);
    }

    @NotNull
    public StreamChannel<OUT> orElseGet(final long count,
            @NotNull final Supplier<? extends OUT> supplier) {

        return map(new OrElseSupplierInvocationFactory<OUT>(count, wrap(supplier)));
    }

    @NotNull
    public StreamChannel<OUT> orElseGet(@NotNull final Supplier<? extends OUT> supplier) {

        return orElseGet(1, supplier);
    }

    @NotNull
    public StreamChannel<OUT> ordered(@Nullable final OrderType orderType) {

        return streamInvocationConfiguration().withOutputOrder(orderType).applyConfiguration();
    }

    @NotNull
    public StreamChannel<OUT> parallel(final int maxInvocations) {

        return parallel().invocationConfiguration()
                         .withMaxInstances(maxInvocations)
                         .applyConfiguration();
    }

    @NotNull
    public StreamChannel<OUT> parallel() {

        mInvocationMode = InvocationMode.PARALLEL;
        return this;
    }

    @NotNull
    public StreamChannel<OUT> peek(@NotNull final Consumer<? super OUT> consumer) {

        return map(new PeekInvocation<OUT>(wrap(consumer)));
    }

    @NotNull
    public StreamChannel<OUT> reduce(
            @NotNull final BiFunction<? super OUT, ? super OUT, ? extends OUT> function) {

        return map(AccumulateFunctionInvocation.functionFactory(function));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> reduce(@NotNull Supplier<? extends AFTER> supplier,
            @NotNull final BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function) {

        return map(AccumulateFunctionInvocation.functionFactory(supplier, function));
    }

    @NotNull
    public StreamChannel<OUT> repeat() {

        return buildChannel(Channels.repeat(this)
                                    .channelConfiguration()
                                    .with(buildChannelConfiguration())
                                    .applyConfiguration()
                                    .buildChannels());
    }

    @NotNull
    public StreamChannel<OUT> runOn(@Nullable final Runner runner) {

        final InvocationMode invocationMode = mInvocationMode;
        final OperationInvocation<OUT, OUT> factory = IdentityInvocation.factoryOf();
        final StreamChannel<OUT> channel = streamInvocationConfiguration().withRunner(runner)
                                                                          .applyConfiguration()
                                                                          .async()
                                                                          .map(factory);
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
    public StreamChannel<OUT> runOnShared() {

        return runOn(null);
    }

    @NotNull
    public StreamChannel<OUT> serial() {

        mInvocationMode = InvocationMode.SERIAL;
        return this;
    }

    @NotNull
    public StreamChannel<OUT> skip(final int count) {

        return map(new SkipInvocationFactory<OUT>(count));
    }

    @NotNull
    public Builder<? extends StreamChannel<OUT>> streamInvocationConfiguration() {

        return new Builder<StreamChannel<OUT>>(mStreamConfigurable, mStreamConfiguration);
    }

    @NotNull
    public StreamChannel<OUT> sync() {

        mInvocationMode = InvocationMode.SYNC;
        return this;
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> then(@Nullable final AFTER output) {

        return map(new GenerateOutputInvocation<AFTER>(Collections.singletonList(output)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> then(@Nullable final AFTER... outputs) {

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
    public <AFTER> StreamChannel<AFTER> then(@Nullable final Iterable<? extends AFTER> outputs) {

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
    public <AFTER> StreamChannel<AFTER> thenGet(final long count,
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return map(new LoopConsumerInvocation<AFTER>(count, wrap(consumer)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> thenGet(
            @NotNull final Consumer<? super ResultChannel<AFTER>> consumer) {

        return thenGet(1, consumer);
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> thenGet(final long count,
            @NotNull final Supplier<? extends AFTER> supplier) {

        return map(new LoopSupplierInvocation<AFTER>(count, wrap(supplier)));
    }

    @NotNull
    public <AFTER> StreamChannel<AFTER> thenGet(@NotNull final Supplier<? extends AFTER> supplier) {

        return thenGet(1, supplier);
    }

    @NotNull
    public StreamChannel<? extends Selectable<OUT>> toSelectable(final int index) {

        return buildChannel(Channels.toSelectable(this, index)
                                    .channelConfiguration()
                                    .with(buildChannelConfiguration())
                                    .applyConfiguration()
                                    .buildChannels());
    }

    @NotNull
    public StreamChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .with(buildChannelConfiguration())
                                                     .applyConfiguration()
                                                     .buildChannel();
        mChannel.bind(new TryCatchOutputConsumer<OUT>(consumer, ioChannel));
        return buildChannel(ioChannel);
    }

    @NotNull
    public StreamChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return tryCatch(new TryCatchBiConsumerFunction<OUT>(function));
    }

    @NotNull
    public StreamChannel<OUT> tryFinally(@NotNull final Runnable runnable) {

        final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .with(buildChannelConfiguration())
                                                     .applyConfiguration()
                                                     .buildChannel();
        mChannel.bind(new TryFinallyOutputConsumer<OUT>(runnable, ioChannel));
        return buildChannel(ioChannel);
    }

    @NotNull
    public List<OUT> all() {

        mBinder.bind();
        return mChannel.all();
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL bind(
            @NotNull final CHANNEL channel) {

        mBinder.bind();
        return mChannel.bind(channel);
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {

        mBinder.bind();
        return mChannel.eventualIterator();
    }

    @Nullable
    public RoutineException getError() {

        mBinder.bind();
        return mChannel.getError();
    }

    public boolean hasCompleted() {

        mBinder.bind();
        return mChannel.hasCompleted();
    }

    public boolean hasNext() {

        mBinder.bind();
        return mChannel.hasNext();
    }

    public OUT next() {

        mBinder.bind();
        return mChannel.next();
    }

    public boolean isBound() {

        return mChannel.isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        mBinder.bind();
        return mChannel.next(count);
    }

    public OUT nextOrElse(final OUT output) {

        mBinder.bind();
        return mChannel.nextOrElse(output);
    }

    public void throwError() {

        mBinder.bind();
        mChannel.throwError();
    }

    public Iterator<OUT> iterator() {

        mBinder.bind();
        return mChannel.iterator();
    }

    public void remove() {

        mBinder.bind();
        mChannel.remove();
    }

    /**
     * Builds a channel configuration from the stream one.
     *
     * @return the channel configuration.
     */
    @NotNull
    protected ChannelConfiguration buildChannelConfiguration() {

        return builderFromOutputChannel(buildConfiguration()).applyConfiguration();
    }

    /**
     * Builds an invocation configuration from the stream one.
     *
     * @return the invocation configuration.
     */
    @NotNull
    protected InvocationConfiguration buildConfiguration() {

        return mStreamConfiguration.builderFrom().with(getConfiguration()).applyConfiguration();
    }

    /**
     * Returns the binder instance.
     *
     * @return the binder.
     */
    @NotNull
    protected Binder getBinder() {

        return mBinder;
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
     *
     * @param <AFTER>             the concatenation output type.
     * @param channel             the wrapped output channel.
     * @param streamConfiguration the stream configuration.
     * @param invocationMode      the invocation mode.
     * @param binder              the binder instance.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <AFTER> StreamChannel<AFTER> newChannel(
            @NotNull OutputChannel<AFTER> channel,
            @NotNull InvocationConfiguration streamConfiguration,
            @NotNull InvocationMode invocationMode, @Nullable Binder binder);

    /**
     * Creates a new routine instance based on the specified factory.
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
    private <AFTER> StreamChannel<AFTER> buildChannel(@NotNull OutputChannel<AFTER> channel) {

        return newChannel(channel, mStreamConfiguration, mInvocationMode, mBinder);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(buildConfiguration(), factory);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <AFTER> StreamChannel<AFTER> concatRoutine(
            @NotNull final InvocationChannel<? super OUT, ? extends AFTER> channel) {

        return buildChannel((OutputChannel<AFTER>) mChannel.bind(channel).result());
    }

    /**
     * Class binding two channels together.
     */
    protected static abstract class Binder {

        /**
         * Avoid instantiation.
         */
        private Binder() {

        }

        /**
         * Returns a new binder of the two specified channels.
         *
         * @param input  the channel returning the inputs.
         * @param output the channel consuming them.
         * @param <DATA> the data type.
         * @return the binder instance.
         */
        public static <DATA> Binder binderOf(@NotNull final OutputChannel<DATA> input,
                @NotNull final IOChannel<DATA> output) {

            return new InputBinder<DATA>(ConstantConditions.notNull("input channel", input),
                    ConstantConditions.notNull("output channel", output));
        }

        /**
         * Binds the two channel.
         * <br>
         * The call will have no effect if the method has been already invoked at least once.
         */
        public abstract void bind();

        /**
         * Default implementation of a binder.
         *
         * @param <DATA> the data type.
         */
        private static class InputBinder<DATA> extends Binder {

            private final OutputChannel<DATA> mInput;

            private final AtomicBoolean mIsBound = new AtomicBoolean();

            private final IOChannel<DATA> mOutput;

            /**
             * Constructor.
             *
             * @param input  the channel returning the inputs.
             * @param output the channel consuming them.
             */
            private InputBinder(@NotNull final OutputChannel<DATA> input,
                    @NotNull final IOChannel<DATA> output) {

                mInput = input;
                mOutput = output;
            }

            public void bind() {

                if (!mIsBound.getAndSet(true)) {
                    mInput.bind(mOutput).close();
                }
            }
        }
    }

    @NotNull
    public StreamChannel<OUT> apply(@NotNull final InvocationConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }
}
