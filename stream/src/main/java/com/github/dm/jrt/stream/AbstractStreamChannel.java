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
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiConsumerWrapper;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionWrapper;
import com.github.dm.jrt.function.Functions;
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
import static com.github.dm.jrt.core.util.Reflection.asArgs;
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
public abstract class AbstractStreamChannel<IN, OUT>
        implements StreamChannel<OUT>, Configurable<StreamChannel<OUT>> {

    private static final BiConsumer<? extends Collection<?>, ?> sCollectConsumer =
            new BiConsumer<Collection<Object>, Object>() {

                public void accept(final Collection<Object> outs, final Object out) {

                    outs.add(out);
                }
            };

    private final FunctionWrapper<OutputChannel<IN>, OutputChannel<OUT>> mInvoke;

    private final Object mMutex = new Object();

    private final OutputChannel<IN> mSourceChannel;

    private OutputChannel<OUT> mChannel;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

    private InvocationMode mInvocationMode;

    private boolean mIsBound;

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
     * @param configuration  the initial invocation configuration.
     * @param invocationMode the delegation type.
     * @param sourceChannel  the source output channel.
     * @param invoke         the invoke function.
     */
    protected AbstractStreamChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode,
            @NotNull final OutputChannel<IN> sourceChannel,
            @NotNull final Function<OutputChannel<IN>, OutputChannel<OUT>> invoke) {

        mStreamConfiguration =
                ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        mSourceChannel = ConstantConditions.notNull("source channel", sourceChannel);
        mInvoke = wrap(invoke);
    }

    public boolean abort() {

        return invoke().abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return invoke().abort(reason);
    }

    public boolean isEmpty() {

        return invoke().isEmpty();
    }

    public boolean isOpen() {

        return invoke().isOpen();
    }

    @NotNull
    public StreamChannel<OUT> afterMax(@NotNull final UnitDuration timeout) {

        invoke().afterMax(timeout);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        invoke().afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

        invoke().allInto(results);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {

        invoke().bind(consumer);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort() {

        invoke().eventuallyAbort();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

        invoke().eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyExit() {

        invoke().eventuallyExit();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> eventuallyThrow() {

        invoke().eventuallyThrow();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> immediately() {

        invoke().immediately();
        return this;
    }

    @NotNull
    public StreamChannel<OUT> skipNext(final int count) {

        invoke().skipNext(count);
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

        return buildChannel(outputChannel, Functions.<OutputChannel<AFTER>>identity());
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
                                        .apply();
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

        return buildChannel(mSourceChannel,
                mInvoke.andThen(new ConcatInvoke<OUT>(buildChannelConfiguration(), channel)));
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

        return buildChannel(mSourceChannel, mInvoke.andThen(
                new MapInvoke<OUT, AFTER>(ConstantConditions.notNull("routine instance", routine),
                        mInvocationMode)));
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

        return streamInvocationConfiguration().withOutputOrder(orderType).apply();
    }

    @NotNull
    public StreamChannel<OUT> parallel(final int maxInvocations) {

        return parallel().invocationConfiguration().withMaxInstances(maxInvocations).apply();
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

        return buildChannel(mSourceChannel,
                mInvoke.andThen(new RepeatInvoke<OUT>(buildChannelConfiguration())));
    }

    @NotNull
    public StreamChannel<OUT> runOn(@Nullable final Runner runner) {

        final InvocationMode invocationMode = mInvocationMode;
        final OperationInvocation<OUT, OUT> factory = IdentityInvocation.factoryOf();
        final StreamChannel<OUT> channel =
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

        return buildChannel(mSourceChannel,
                mInvoke.andThen(new SelectableInvoke<OUT>(buildChannelConfiguration(), index)));
    }

    @NotNull
    public StreamChannel<OUT> tryCatch(
            @NotNull final BiConsumer<? super RoutineException, ? super InputChannel<OUT>>
                    consumer) {

        return buildChannel(mSourceChannel, mInvoke.andThen(
                new TryCatchInvoke<OUT>(buildChannelConfiguration(), wrap(consumer))));
    }

    @NotNull
    public StreamChannel<OUT> tryCatch(
            @NotNull final Function<? super RoutineException, ? extends OUT> function) {

        return tryCatch(new TryCatchBiConsumerFunction<OUT>(function));
    }

    @NotNull
    public StreamChannel<OUT> tryFinally(@NotNull final Runnable runnable) {

        return buildChannel(mSourceChannel, mInvoke.andThen(
                new TryFinallyInvoke<OUT>(buildChannelConfiguration(),
                        ConstantConditions.notNull("runnable instance", runnable))));
    }

    @NotNull
    public List<OUT> all() {

        return invoke().all();
    }

    @NotNull
    public <CHANNEL extends InputChannel<? super OUT>> CHANNEL bind(
            @NotNull final CHANNEL channel) {

        return invoke().bind(channel);
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {

        return invoke().eventualIterator();
    }

    @Nullable
    public RoutineException getError() {

        return invoke().getError();
    }

    public boolean hasCompleted() {

        return invoke().hasCompleted();
    }

    public boolean hasNext() {

        return invoke().hasNext();
    }

    public OUT next() {

        return invoke().next();
    }

    public boolean isBound() {

        return invoke().isBound();
    }

    @NotNull
    public List<OUT> next(final int count) {

        return invoke().next(count);
    }

    public OUT nextOrElse(final OUT output) {

        return invoke().nextOrElse(output);
    }

    public void throwError() {

        invoke().throwError();
    }

    public Iterator<OUT> iterator() {

        return invoke().iterator();
    }

    public void remove() {

        invoke().remove();
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

    // TODO: 05/05/16 javadoc
    @NotNull
    protected FunctionWrapper<OutputChannel<IN>, OutputChannel<OUT>> getInvoke() {

        return mInvoke;
    }

    // TODO: 05/05/16 javadoc
    @NotNull
    protected OutputChannel<IN> getSourceChannel() {

        return mSourceChannel;
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
     * @param <BEFORE>
     * @param <AFTER>             the concatenation output type.
     * @param streamConfiguration the stream configuration.
     * @param invocationMode      the invocation mode.
     * @param sourceChannel       the source output channel.
     * @param invoke              the invoke function.
     * @return the newly created channel instance.
     */
    @NotNull
    protected abstract <BEFORE, AFTER> StreamChannel<AFTER> newChannel(
            @NotNull InvocationConfiguration streamConfiguration,
            @NotNull InvocationMode invocationMode, @NotNull OutputChannel<BEFORE> sourceChannel,
            @NotNull Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke);

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
    private <BEFORE, AFTER> StreamChannel<AFTER> buildChannel(
            @NotNull final OutputChannel<BEFORE> sourceChannel,
            @NotNull final Function<OutputChannel<BEFORE>, OutputChannel<AFTER>> invoke) {

        synchronized (mMutex) {
            mIsBound = true;
        }

        return newChannel(mStreamConfiguration, mInvocationMode, sourceChannel, invoke);
    }

    @NotNull
    private <AFTER> Routine<? super OUT, ? extends AFTER> buildRoutine(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        return newRoutine(buildConfiguration(), factory);
    }

    @NotNull
    private OutputChannel<OUT> invoke() {

        final boolean isInvoke;
        synchronized (mMutex) {
            if (mIsBound) {
                throw new IllegalStateException("the channel is already bound");
            }

            isInvoke = (mChannel == null);
        }

        if (isInvoke) {
            mChannel = mInvoke.apply(mSourceChannel);
        }

        return mChannel;
    }

    // TODO: 05/05/16 javadoc
    private static class ConcatInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

        private final OutputChannel<? extends OUT> mChannel;

        private final ChannelConfiguration mConfiguration;

        private ConcatInvoke(@NotNull final ChannelConfiguration configuration,
                @NotNull final OutputChannel<? extends OUT> channel) {

            super(asArgs(configuration, channel));
            mConfiguration = configuration;
            mChannel = channel;
        }

        public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {

            return Channels.<OUT>concat(channel, mChannel).channelConfiguration()
                                                          .with(mConfiguration)
                                                          .apply()
                                                          .buildChannels();
        }
    }

    // TODO: 05/05/16 javadoc
    private static class MapInvoke<OUT, AFTER> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<AFTER>> {

        private final InvocationMode mInvocationMode;

        private final Routine mRoutine;

        private MapInvoke(@NotNull final Routine<? super OUT, ? extends AFTER> routine,
                @NotNull final InvocationMode invocationMode) {

            super(asArgs(routine, invocationMode));
            mRoutine = routine;
            mInvocationMode = invocationMode;
        }

        @SuppressWarnings("unchecked")
        public OutputChannel<AFTER> apply(final OutputChannel<OUT> channel) {

            final InvocationMode invocationMode = mInvocationMode;
            if (invocationMode == InvocationMode.ASYNC) {
                return (OutputChannel<AFTER>) mRoutine.asyncCall(channel);

            } else if (invocationMode == InvocationMode.PARALLEL) {
                return (OutputChannel<AFTER>) mRoutine.parallelCall(channel);

            } else if (invocationMode == InvocationMode.SYNC) {
                return (OutputChannel<AFTER>) mRoutine.syncCall(channel);
            }

            return (OutputChannel<AFTER>) mRoutine.serialCall(channel);
        }
    }

    // TODO: 05/05/16 javadoc
    private static class RepeatInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

        private final ChannelConfiguration mConfiguration;

        private RepeatInvoke(@NotNull final ChannelConfiguration configuration) {

            super(asArgs(configuration));
            mConfiguration = configuration;
        }

        public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {

            return Channels.repeat(channel)
                           .channelConfiguration()
                           .with(mConfiguration)
                           .apply()
                           .buildChannels();
        }
    }

    // TODO: 05/05/16 javadoc
    private static class SelectableInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<Selectable<OUT>>> {

        private final ChannelConfiguration mConfiguration;

        private final int mIndex;

        private SelectableInvoke(@NotNull final ChannelConfiguration configuration,
                final int index) {

            super(asArgs(configuration, index));
            mConfiguration = configuration;
            mIndex = index;
        }

        @SuppressWarnings("unchecked")
        public OutputChannel<Selectable<OUT>> apply(final OutputChannel<OUT> channel) {

            final OutputChannel<? extends Selectable<OUT>> outputChannel =
                    Channels.toSelectable(channel, mIndex)
                            .channelConfiguration()
                            .with(mConfiguration)
                            .apply()
                            .buildChannels();
            return (OutputChannel<Selectable<OUT>>) outputChannel;
        }
    }

    // TODO: 05/05/16 javadoc
    private static class TryCatchInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

        private final ChannelConfiguration mConfiguration;

        private final BiConsumerWrapper<? super RoutineException, ? super InputChannel<OUT>>
                mConsumer;

        private TryCatchInvoke(@NotNull final ChannelConfiguration configuration,
                @NotNull final BiConsumerWrapper<? super RoutineException, ? super
                        InputChannel<OUT>> consumer) {

            super(asArgs(configuration, consumer));
            mConfiguration = configuration;
            mConsumer = consumer;
        }

        public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {

            final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                         .channelConfiguration()
                                                         .with(mConfiguration)
                                                         .apply()
                                                         .buildChannel();
            channel.bind(new TryCatchOutputConsumer<OUT>(mConsumer, ioChannel));
            return ioChannel;
        }
    }

    // TODO: 05/05/16 javadoc
    private static class TryFinallyInvoke<OUT> extends DeepEqualObject
            implements Function<OutputChannel<OUT>, OutputChannel<OUT>> {

        private final ChannelConfiguration mConfiguration;

        private final Runnable mRunnable;

        private TryFinallyInvoke(@NotNull final ChannelConfiguration configuration,
                @NotNull final Runnable runnable) {

            super(asArgs(configuration, runnable));
            mConfiguration = configuration;
            mRunnable = runnable;
        }

        public OutputChannel<OUT> apply(final OutputChannel<OUT> channel) {

            final IOChannel<OUT> ioChannel = JRoutineCore.io()
                                                         .channelConfiguration()
                                                         .with(mConfiguration)
                                                         .apply()
                                                         .buildChannel();
            channel.bind(new TryFinallyOutputConsumer<OUT>(mRunnable, ioChannel));
            return ioChannel;
        }
    }

    @NotNull
    public StreamChannel<OUT> apply(@NotNull final InvocationConfiguration configuration) {

        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }
}
