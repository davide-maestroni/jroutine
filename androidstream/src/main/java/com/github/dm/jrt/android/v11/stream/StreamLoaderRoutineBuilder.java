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

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.StreamRoutineBuilder;
import com.github.dm.jrt.stream.annotation.StreamFlow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.COLLECT;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.CONFIG;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.MAP;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.REDUCE;

/**
 * Interface defining a builder of routines concatenating map and reduce functions.
 * <br>
 * Each function in the stream will be backed by a routine instance, which may have its own
 * specific configuration and invocation mode.
 * <br>
 * In order to prevent undesired leaks, the class of the specified functions must have a static
 * scope.
 * <p>
 * To better document the effect of each method on the underlying stream, a {@link StreamFlow}
 * annotation indicates for each one the type of transformation applied.
 * <br>
 * Note also that, if at least one reduce function is part of the chain, the results will be
 * propagated only when the built routine invocation completes.
 * <p>
 * Created by davide-maestroni on 07/03/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamLoaderRoutineBuilder<IN, OUT> extends StreamRoutineBuilder<IN, OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> append(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> append(@Nullable OUT... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> append(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> append(@NotNull Channel<?, ? extends OUT> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> appendGet(long count,
            @NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> appendGet(@NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> appendGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> appendGetMore(
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> async();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> async(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> asyncMap(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit,
            @NotNull Backoff backoff);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit, long delay,
            @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit,
            @Nullable UnitDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    InvocationFactory<IN, OUT> buildFactory();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    StreamLoaderRoutineBuilder<IN, OUT> collect(
            @NotNull BiConsumer<? super OUT, ? super OUT> accumulateConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> collect(
            @NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiConsumer<? super AFTER, ? super OUT> accumulateConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    <AFTER extends Collection<? super OUT>> StreamLoaderRoutineBuilder<IN, AFTER> collectInto(
            @NotNull Supplier<? extends AFTER> collectionSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> delay(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> delay(@NotNull UnitDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> filter(@NotNull Predicate<? super OUT> filterPredicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> flatLift(
            @NotNull Function<? super StreamRoutineBuilder<IN, OUT>, ? extends
                    StreamRoutineBuilder<BEFORE, AFTER>> liftFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    InvocationConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    invocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> invocationMode(@NotNull InvocationMode invocationMode);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> lag(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> lag(@NotNull UnitDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> lift(
            @NotNull Function<? extends Function<? super Channel<?, IN>, ? extends Channel<?,
                    OUT>>, ? extends Function<? super Channel<?, BEFORE>, ? extends Channel<?,
                    AFTER>>> liftFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamLoaderRoutineBuilder<BEFORE, AFTER> liftConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> limit(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapAll(
            @NotNull Function<? super List<OUT>, ? extends AFTER> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapAllMore(
            @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> mapMore(
            @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, Void> onComplete(@NotNull Action completeAction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> onError(
            @NotNull Consumer<? super RoutineException> errorConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, Void> onOutput(@NotNull Consumer<? super OUT> outputConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElse(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElse(@Nullable OUT... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElse(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElseGet(long count,
            @NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElseGet(@NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElseGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElseGetMore(
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> orElseThrow(@Nullable Throwable error);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> parallel();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> parallel(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> peekComplete(@NotNull Action completeAction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> peekError(
            @NotNull Consumer<? super RoutineException> errorConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> peekOutput(@NotNull Consumer<? super OUT> outputConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    StreamLoaderRoutineBuilder<IN, OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> reduce(
            @NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiFunction<? super AFTER, ? super OUT, ? extends AFTER> accumulateFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    StreamLoaderRoutineBuilder<IN, OUT> retry(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    StreamLoaderRoutineBuilder<IN, OUT> retry(int count, @NotNull Backoff backoff);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(COLLECT)
    StreamLoaderRoutineBuilder<IN, OUT> retry(
            @NotNull BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> sequential();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> sorted(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> straight();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    InvocationConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    streamInvocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> sync();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(@Nullable AFTER output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(@Nullable AFTER... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGet(long count,
            @NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGet(
            @NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGetMore(long count,
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(REDUCE)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> thenGetMore(
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> catchFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> tryCatchMore(
            @NotNull BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @StreamFlow(MAP)
    StreamLoaderRoutineBuilder<IN, OUT> tryFinally(@NotNull Action finallyAction);

    /**
     * Builds a new context invocation factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    ContextInvocationFactory<IN, OUT> buildContextFactory();

    /**
     * Short for {@code loaderConfiguration().withCacheStrategy(strategyType).applied()}.
     *
     * @param strategyType the cache strategy type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> cache(@Nullable CacheStrategyType strategyType);

    /**
     * Short for {@code loaderConfiguration().withFactoryId(factoryId).applied()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the factory ID to the specified one.
     *
     * @param factoryId the factory ID.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> factoryId(int factoryId);

    /**
     * Gets the loader configuration builder related to the instance.
     * <br>
     * The configuration options not supported by the specific implementation might be ignored.
     * <p>
     * Note that the configuration builder must be initialized with the current configuration.
     *
     * @return the loader configuration builder.
     */
    @NotNull
    @StreamFlow(CONFIG)
    LoaderConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    loaderConfiguration();

    /**
     * Short for {@code loaderConfiguration().withLoaderId(loaderId).applied()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine loader ID.
     *
     * @param loaderId the loader ID.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> loaderId(int loaderId);

    /**
     * Concatenates a stream based on the specified mapping invocation factory to this one.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param factory the context invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalStateException if the loader context is not set.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> map(
            @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Sets the stream loader context.
     * <br>
     * The context will be used by all the concatenated routines until changed.
     * <br>
     * If null it will cause the next routines to employ the configured runner instead of an Android
     * loader.
     *
     * @param context the loader context.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> on(@Nullable LoaderContext context);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param count   the number of groups.
     * @param factory the processing invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param factory     the processing invocation factory.
     * @param <AFTER>     the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamLoaderRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Short for {@code loaderConfiguration().withResultStaleTime(staleTime).applied()}.
     *
     * @param staleTime the stale time.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> staleAfter(@Nullable UnitDuration staleTime);

    /**
     * Short for {@code loaderConfiguration().withResultStaleTime(time, timeUnit).applied()}.
     *
     * @param time     the time.
     * @param timeUnit the time unit.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamLoaderRoutineBuilder<IN, OUT> staleAfter(long time, @NotNull TimeUnit timeUnit);

    /**
     * Gets the loader configuration builder related to the whole stream.
     * <br>
     * The configuration options will be applied to all the next concatenated routines unless
     * overwritten by specific ones.
     * <p>
     * Note that the configuration builder will be initialized with the current stream
     * configuration.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    @StreamFlow(CONFIG)
    LoaderConfiguration.Builder<? extends StreamLoaderRoutineBuilder<IN, OUT>>
    streamLoaderConfiguration();

    /**
     * Interface defining a loader stream configuration.
     */
    interface LoaderStreamConfiguration extends StreamConfiguration {

        /**
         * Gets the combination of stream and current configuration as a loader one.
         *
         * @return the loader configuration.
         */
        @NotNull
        LoaderConfiguration asLoaderConfiguration();

        /**
         * Gets the configuration that will override the stream one only for the next
         * concatenated routine.
         *
         * @return the loader configuration.
         */
        @NotNull
        LoaderConfiguration getCurrentLoaderConfiguration();

        /**
         * Gets the stream loader context.
         *
         * @return the loader context.
         */
        @Nullable
        LoaderContext getLoaderContext();

        /**
         * Gets the configuration that will be applied to all the concatenated routines.
         *
         * @return the loader configuration.
         */
        @NotNull
        LoaderConfiguration getStreamLoaderConfiguration();
    }
}
