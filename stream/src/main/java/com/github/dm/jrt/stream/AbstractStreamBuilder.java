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
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.builder.TemplateRoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.FunctionDecorator;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuildingException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.decorate;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;

/**
 * Abstract implementation of a stream routine builder.
 * <p>
 * This class provides a default implementation of all the stream builder features. The inheriting
 * class just needs to create routine and configuration instances when required.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractStreamBuilder<IN, OUT> extends TemplateRoutineBuilder<IN, OUT>
        implements StreamBuilder<IN, OUT> {

    private static final StraightRunner sStraightRunner = new StraightRunner();

    private FunctionDecorator<? extends Channel<?, ?>, ? extends Channel<?, ?>> mBindingFunction;

    private boolean mIsStraight;

    private StreamConfiguration mStreamConfiguration;

    private final Configurable<StreamBuilder<IN, OUT>> mConfigurable =
            new Configurable<StreamBuilder<IN, OUT>>() {

                @NotNull
                public StreamBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final StreamConfiguration streamConfiguration = mStreamConfiguration;
                    return AbstractStreamBuilder.this.apply(
                            newConfiguration(streamConfiguration.getStreamInvocationConfiguration(),
                                    configuration, streamConfiguration.getInvocationMode()));
                }
            };

    private final Configurable<StreamBuilder<IN, OUT>> mStreamConfigurable =
            new Configurable<StreamBuilder<IN, OUT>>() {

                @NotNull
                public StreamBuilder<IN, OUT> apply(
                        @NotNull final InvocationConfiguration configuration) {
                    final StreamConfiguration streamConfiguration = mStreamConfiguration;
                    return AbstractStreamBuilder.this.apply(newConfiguration(configuration,
                            streamConfiguration.getCurrentInvocationConfiguration(),
                            streamConfiguration.getInvocationMode()));
                }
            };

    /**
     * Constructor.
     *
     * @param streamConfiguration the stream configuration.
     */
    @SuppressWarnings("unchecked")
    protected AbstractStreamBuilder(@NotNull final StreamConfiguration streamConfiguration) {
        mStreamConfiguration =
                ConstantConditions.notNull("stream configuration", streamConfiguration);
        mBindingFunction = Functions.identity();
    }

    public boolean abort() {
        return call().abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return call().abort(reason);
    }

    @NotNull
    public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        return call().after(delay, timeUnit);
    }

    @NotNull
    public Channel<IN, OUT> after(@NotNull final UnitDuration delay) {
        return call().after(delay);
    }

    @NotNull
    public List<OUT> all() {
        return call().all();
    }

    @NotNull
    public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        return call().allInto(results);
    }

    @NotNull
    public <CHANNEL extends Channel<? super OUT, ?>> CHANNEL bind(@NotNull final CHANNEL channel) {
        return call().bind(channel);
    }

    @NotNull
    public Channel<IN, OUT> bind(@NotNull final ChannelConsumer<? super OUT> consumer) {
        return call().bind(consumer);
    }

    @NotNull
    public Channel<IN, OUT> close() {
        return call().close();
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {
        return call().eventualIterator();
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort() {
        return call().eventuallyAbort();
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        return call().eventuallyAbort(reason);
    }

    @NotNull
    public Channel<IN, OUT> eventuallyBreak() {
        return call().eventuallyBreak();
    }

    @NotNull
    public Channel<IN, OUT> eventuallyFail() {
        return call().eventuallyFail();
    }

    @Nullable
    public RoutineException getError() {
        return call().getError();
    }

    public boolean hasCompleted() {
        return call().hasCompleted();
    }

    public boolean hasNext() {
        return call().hasNext();
    }

    public OUT next() {
        return call().next();
    }

    @NotNull
    public Channel<IN, OUT> immediately() {
        return call().immediately();
    }

    public int inputCount() {
        return call().inputCount();
    }

    public boolean isBound() {
        return call().isBound();
    }

    public boolean isEmpty() {
        return call().isEmpty();
    }

    public boolean isOpen() {
        return call().isOpen();
    }

    @NotNull
    public List<OUT> next(final int count) {
        return call().next(count);
    }

    public OUT nextOrElse(final OUT output) {
        return call().nextOrElse(output);
    }

    public int outputCount() {
        return call().outputCount();
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        return call().pass(channel);
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        return call().pass(inputs);
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN input) {
        return call().pass(input);
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
        return call().pass(inputs);
    }

    public int size() {
        return call().size();
    }

    @NotNull
    public Channel<IN, OUT> skipNext(final int count) {
        return call().skipNext(count);
    }

    @NotNull
    public Channel<IN, OUT> sortedByCall() {
        return call().sortedByCall();
    }

    @NotNull
    public Channel<IN, OUT> sortedByDelay() {
        return call().sortedByDelay();
    }

    public void throwError() {
        call().throwError();
    }

    @NotNull
    public StreamBuilder<IN, OUT> async() {
        return invocationMode(InvocationMode.ASYNC);
    }

    @NotNull
    public InvocationFactory<IN, OUT> buildFactory() {
        return new StreamInvocationFactory<IN, OUT>(getBindingFunction());
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> flatMap(
            @NotNull final Function<? super OUT, ? extends Channel<?, ? extends AFTER>>
                    mappingFunction) {
        return map(new MapInvocation<OUT, AFTER>(decorate(mappingFunction)));
    }

    @NotNull
    public StreamBuilder<IN, OUT> invocationMode(@NotNull final InvocationMode invocationMode) {
        mIsStraight = false;
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        return apply(newConfiguration(streamConfiguration.getStreamInvocationConfiguration(),
                streamConfiguration.getCurrentInvocationConfiguration(), invocationMode));
    }

    @NotNull
    public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> let(
            @NotNull final Function<? super StreamBuilder<IN, OUT>, ? extends
                    StreamBuilder<BEFORE, AFTER>> liftFunction) {
        try {
            return ConstantConditions.notNull("transformed stream builder",
                    liftFunction.apply(this));

        } catch (final Exception e) {
            throw StreamBuildingException.wrapIfNeeded(e);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> letWithConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? super StreamBuilder<IN,
                    OUT>, ? extends StreamBuilder<BEFORE, AFTER>> liftFunction) {
        try {
            return ConstantConditions.notNull("transformed stream",
                    ((BiFunction<StreamConfiguration, ? super StreamBuilder<IN, OUT>, ? extends
                            StreamBuilder<BEFORE, AFTER>>) liftFunction).apply(mStreamConfiguration,
                            this));

        } catch (final Exception e) {
            throw StreamBuildingException.wrapIfNeeded(e);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> lift(
            @NotNull final Function<? extends Function<? super Channel<?, IN>, ? extends
                    Channel<?, OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends
                    Channel<?, AFTER>>> liftFunction) {
        try {
            mBindingFunction = decorate(
                    ((Function<Function<Channel<?, IN>, Channel<?, OUT>>, Function<Channel<?,
                            BEFORE>, Channel<?, AFTER>>>) liftFunction)
                            .apply(getBindingFunction()));
            return (StreamBuilder<BEFORE, AFTER>) this;

        } catch (final Exception e) {
            throw StreamBuildingException.wrapIfNeeded(e);

        } finally {
            resetConfiguration();
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> liftWithConfig(
            @NotNull final BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction) {
        try {
            mBindingFunction = decorate(
                    ((BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>,
                            Function<Channel<?, BEFORE>, Channel<?, AFTER>>>) liftFunction)
                            .apply(mStreamConfiguration, getBindingFunction()));
            return (StreamBuilder<BEFORE, AFTER>) this;

        } catch (final Exception e) {
            throw StreamBuildingException.wrapIfNeeded(e);

        } finally {
            resetConfiguration();
        }
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> map(
            @NotNull final Function<? super OUT, ? extends AFTER> mappingFunction) {
        return map(functionMapping(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> map(
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {
        final StreamConfiguration streamConfiguration = getStraightConfiguration();
        return map(newRoutine(streamConfiguration, factory),
                streamConfiguration.getInvocationMode());
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
        return map(routine, mStreamConfiguration.getInvocationMode());
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> map(
            @NotNull final RoutineBuilder<? super OUT, ? extends AFTER> builder) {
        final StreamConfiguration streamConfiguration = getStraightConfiguration();
        return map(newRoutine(streamConfiguration, builder),
                streamConfiguration.getInvocationMode());
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> mapAccept(
            @NotNull final BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer) {
        return map(consumerMapping(mappingConsumer));
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> mapAll(
            @NotNull final Function<? super List<OUT>, ? extends AFTER> mappingFunction) {
        return map(functionCall(mappingFunction));
    }

    @NotNull
    public <AFTER> StreamBuilder<IN, AFTER> mapAllAccept(
            @NotNull final BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>>
                    mappingConsumer) {
        return map(consumerCall(mappingConsumer));
    }

    @NotNull
    public StreamBuilder<IN, OUT> mapOn(@Nullable final Runner runner) {
        return async().streamInvocationConfiguration()
                      .withRunner(runner)
                      .configured()
                      .map(IdentityInvocation.<OUT>factoryOf());
    }

    @NotNull
    public StreamBuilder<IN, OUT> parallel() {
        return invocationMode(InvocationMode.PARALLEL);
    }

    @NotNull
    public StreamBuilder<IN, OUT> sequential() {
        return invocationMode(InvocationMode.SEQUENTIAL);
    }

    @NotNull
    public StreamBuilder<IN, OUT> sorted() {
        return streamInvocationConfiguration().withOutputOrder(OrderType.BY_CALL).configured();
    }

    @NotNull
    public StreamBuilder<IN, OUT> straight() {
        sync();
        mIsStraight = true;
        return this;
    }

    @NotNull
    public Builder<? extends StreamBuilder<IN, OUT>> streamInvocationConfiguration() {
        return new Builder<StreamBuilder<IN, OUT>>(mStreamConfigurable,
                mStreamConfiguration.getStreamInvocationConfiguration());
    }

    @NotNull
    public StreamBuilder<IN, OUT> sync() {
        return invocationMode(InvocationMode.SYNC);
    }

    @NotNull
    public StreamBuilder<IN, OUT> unsorted() {
        return streamInvocationConfiguration().withOutputOrder(OrderType.BY_DELAY).configured();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Routine<IN, OUT> buildRoutine() {
        final Routine<? super IN, ? extends OUT> routine =
                ConstantConditions.notNull("routine instance",
                        newRoutine(mStreamConfiguration, buildFactory()));
        resetConfiguration();
        return (Routine<IN, OUT>) routine;
    }

    @NotNull
    @Override
    public Builder<? extends StreamBuilder<IN, OUT>> invocationConfiguration() {
        return new Builder<StreamBuilder<IN, OUT>>(mConfigurable,
                mStreamConfiguration.getCurrentInvocationConfiguration());
    }

    public Iterator<OUT> iterator() {
        return call().iterator();
    }

    public void remove() {
        call().remove();
    }

    /**
     * Applies the specified stream configuration.
     *
     * @param configuration the stream configuration.
     * @return this builder.
     */
    @NotNull
    protected StreamBuilder<IN, OUT> apply(@NotNull final StreamConfiguration configuration) {
        mStreamConfiguration = ConstantConditions.notNull("stream configuration", configuration);
        return this;
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
     * Creates a new routine instance based on the specified builder.
     *
     * @param streamConfiguration the stream configuration.
     * @param builder             the routine builder.
     * @param <AFTER>             the concatenation output type.
     * @return the newly created routine instance.
     */
    @NotNull
    protected <BEFORE, AFTER> Routine<? super BEFORE, ? extends AFTER> newRoutine(
            @NotNull StreamConfiguration streamConfiguration,
            @NotNull RoutineBuilder<? super BEFORE, ? extends AFTER> builder) {
        return builder.invocationConfiguration()
                      .with(null)
                      .with(streamConfiguration.asInvocationConfiguration())
                      .configured()
                      .buildRoutine();
    }

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
    private Channel<IN, OUT> call() {
        return mStreamConfiguration.getInvocationMode().invoke(this);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> getBindingFunction() {
        return (FunctionDecorator<Channel<?, IN>, Channel<?, OUT>>) mBindingFunction;
    }

    @NotNull
    private StreamConfiguration getStraightConfiguration() {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        if (mIsStraight) {
            return newConfiguration(streamConfiguration.getStreamInvocationConfiguration(),
                    streamConfiguration.getCurrentInvocationConfiguration()
                                       .builderFrom()
                                       .withRunner(sStraightRunner)
                                       .configured(), InvocationMode.ASYNC);
        }

        return streamConfiguration;
    }

    @SuppressWarnings("unchecked")
    private <AFTER> StreamBuilder<IN, AFTER> map(
            @NotNull final Routine<? super OUT, ? extends AFTER> routine,
            @NotNull final InvocationMode invocationMode) {
        mBindingFunction =
                getBindingFunction().andThen(new BindMap<OUT, AFTER>(routine, invocationMode));
        resetConfiguration();
        return (StreamBuilder<IN, AFTER>) this;
    }

    private void resetConfiguration() {
        final StreamConfiguration streamConfiguration = mStreamConfiguration;
        apply(resetConfiguration(streamConfiguration.getStreamInvocationConfiguration(),
                streamConfiguration.getInvocationMode()));
    }

    /**
     * Invocations building a stream of routines by applying a binding function.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocation<IN, OUT> extends ChannelInvocation<IN, OUT> {

        private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

        /**
         * Constructor.
         *
         * @param bindingFunction the binding function.
         */
        private StreamInvocation(
                @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
            mBindingFunction = bindingFunction;
        }

        @NotNull
        @Override
        protected Channel<?, OUT> onChannel(@NotNull final Channel<?, IN> channel) throws
                Exception {
            return mBindingFunction.apply(channel);
        }
    }

    /**
     * Factory of stream invocations.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class StreamInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mBindingFunction;

        /**
         * Constructor.
         *
         * @param bindingFunction the binding function.
         */
        private StreamInvocationFactory(
                @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> bindingFunction) {
            super(asArgs(bindingFunction));
            mBindingFunction = bindingFunction;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() throws Exception {
            return new StreamInvocation<IN, OUT>(mBindingFunction);
        }
    }
}
