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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.annotation.StreamTransform;
import com.github.dm.jrt.stream.annotation.StreamTransform.TransformType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions, employing Android loaders to run the backing routines.
 * <br>
 * In fact, each function in the channel is backed by a routine instance, that can have its own
 * specific configuration and invocation mode.
 * <br>
 * In order to prevent undesired leaks, the class of the specified functions must have a static
 * scope.
 * <p>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the previous routine invocations complete.
 * <p>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
public interface LoaderStreamChannelCompat<IN, OUT> extends StreamChannel<IN, OUT>,
        LoaderConfigurableBuilder<LoaderStreamChannelCompat<IN, OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> afterMax(@NotNull UnitDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> bind(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.FLOW)
    LoaderStreamChannelCompat<IN, OUT> skipNext(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> async();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            long maxDelay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable UnitDuration maxDelay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    LoaderStreamChannelCompat<IN, OUT> collect(
            @NotNull BiConsumer<? super OUT, ? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER extends Collection<? super OUT>> LoaderStreamChannelCompat<IN, AFTER> collect(
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> collect(
            @NotNull Supplier<? extends AFTER> supplier,
            @NotNull BiConsumer<? super AFTER, ? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> concat(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> concat(@Nullable OUT... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> concat(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> concat(@NotNull OutputChannel<? extends OUT> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    invocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> limit(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapAll(
            @NotNull BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> mapAll(
            @NotNull Function<? super List<OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> onError(
            @NotNull Consumer<? super RoutineException> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, Void> onOutput(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElse(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElse(@Nullable OUT... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElse(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElseGet(long count,
            @NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElseGet(
            @NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElseGet(long count,
            @NotNull Supplier<? extends OUT> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> orElseGet(@NotNull Supplier<? extends OUT> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> ordered(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> parallel();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> parallel(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> peek(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    LoaderStreamChannelCompat<IN, OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> reduce(@NotNull Supplier<? extends AFTER> supplier,
            @NotNull BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> repeat();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> runOn(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> runOnShared();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> serial();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<? super
                    OUT, ? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(int count,
            @NotNull Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<? super
                    OUT, ? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(int count,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(int count,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamInvocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> sync();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(@Nullable AFTER output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(@Nullable AFTER... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.STOP)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> thenGet(
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, ? extends ParcelableSelectable<OUT>> toSelectable(int index);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> transform(
            @NotNull Function<? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<OUT>>, ? extends Function<? super OutputChannel<IN>, ? extends
                    OutputChannel<AFTER>>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    LoaderStreamChannelCompat<IN, OUT> tryFinally(@NotNull Runnable runnable);

    /**
     * Transforms this stream by applying the specified function.
     *
     * @param function the transformation function.
     * @param <BEFORE> the concatenation input type.
     * @param <AFTER>  the concatenation output type.
     * @return the transformed stream.
     */
    @NotNull
    <BEFORE, AFTER> LoaderStreamChannelCompat<BEFORE, AFTER> applyLoader(
            @NotNull Function<? super LoaderStreamChannelCompat<IN, OUT>, ? extends
                    LoaderStreamChannelCompat<BEFORE, AFTER>> function);

    /**
     * Short for {@code loaderConfiguration().withCacheStrategy(strategyType).apply()}.
     *
     * @param strategyType the cache strategy type.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> cache(@Nullable CacheStrategyType strategyType);

    /**
     * Short for {@code loaderConfiguration().withFactoryId(factoryId).apply()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the factory ID to the specified one.
     *
     * @param factoryId the factory ID.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> factoryId(int factoryId);

    /**
     * Gets the loader configuration builder related only to the next concatenated routine instance.
     * Any further addition to the chain will retain only the stream configuration.
     * <br>
     * Only the options set in this configuration (that is, the ones with a value different from the
     * default) will override the stream ones.
     * <p>
     * Note that the configuration builder will be initialized with the current configuration for
     * the next routine.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>> loaderConfiguration();

    /**
     * Short for {@code loaderConfiguration().withLoaderId(loaderId).apply()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine loader ID.
     *
     * @param loaderId the loader ID.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> loaderId(int loaderId);

    /**
     * Concatenates a stream based on the specified mapping invocation factory to this one.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param factory the context invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream instance.
     * @throws java.lang.IllegalStateException if the loader context is not set.
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> map(
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
     * @return the concatenated stream instance.
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

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
     * @return the concatenated stream instance.
     */
    @NotNull
    @StreamTransform(TransformType.BIND)
    <AFTER> LoaderStreamChannelCompat<IN, AFTER> splitBy(int count,
            @NotNull ContextInvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Short for {@code loaderConfiguration().withResultStaleTime(staleTime).apply()}.
     *
     * @param staleTime the stale time.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> staleAfter(@Nullable UnitDuration staleTime);

    /**
     * Short for {@code loaderConfiguration().withResultStaleTime(time, timeUnit).apply()}.
     *
     * @param time     the time.
     * @param timeUnit the time unit.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> staleAfter(long time, @NotNull TimeUnit timeUnit);

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
    LoaderConfiguration.Builder<? extends LoaderStreamChannelCompat<IN, OUT>>
    streamLoaderConfiguration();

    /**
     * Sets the stream loader context.
     * <br>
     * The context will be used by all the concatenated routines until changed.
     * <br>
     * If null it will cause the next routines to employ the configured runner instead of an Android
     * loader.
     *
     * @param context the loader context.
     * @return the configured stream.
     */
    @NotNull
    @StreamTransform(TransformType.CONFIG)
    LoaderStreamChannelCompat<IN, OUT> with(@Nullable LoaderContextCompat context);
}
