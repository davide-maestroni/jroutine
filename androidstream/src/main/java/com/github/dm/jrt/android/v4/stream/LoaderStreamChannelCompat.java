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

import com.github.dm.jrt.android.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.builder.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.channel.ParcelableSelectable;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.builder.InvocationConfiguration;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.TimeDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.stream.StreamChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions, employing Android loaders to run the backing routines.<br/>
 * In fact, each function in the channel is backed by a routine instance, that can have its own
 * specific configuration and invocation mode.<br/>
 * In order to prevent undesired leaks, the class of the specified functions must have a static
 * scope.
 * <p/>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the previous routine invocations complete.
 * <p/>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
public interface LoaderStreamChannelCompat<OUT>
        extends StreamChannel<OUT>, LoaderConfigurableBuilder<LoaderStreamChannelCompat<OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> bindTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> async();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            long maxDelay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable TimeDuration maxDelay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> collect(
            @NotNull BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> collect(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> collect(
            @NotNull Function<? super List<OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<Void> consume(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> map(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> maxParallelInvocations(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> ordered(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> parallel();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> repeat();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> runOn(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> runOnShared();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> sync();

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(@Nullable AFTER output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(@Nullable AFTER... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannelCompat<AFTER> then(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<? extends ParcelableSelectable<OUT>> toSelectable(int index);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>> withInvocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannelCompat<OUT>>
    withStreamInvocations();

    /**
     * Short for {@code withLoaders().withCacheStrategy(strategyType).getConfigured()}.
     *
     * @param strategyType the cache strategy type.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> cache(@Nullable CacheStrategyType strategyType);

    /**
     * Short for {@code withLoaders().withLoaderId(loaderId).getConfigured()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine loader ID.
     *
     * @param loaderId the loader ID.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> loaderId(int loaderId);

    /**
     * Short for {@code withLoaders().withRoutineId(routineId).getConfigured()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine ID.
     *
     * @param routineId the routine ID.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> routineId(int routineId);

    /**
     * Short for {@code withLoaders().withResultStaleTime(time, timeUnit).getConfigured()}.
     *
     * @param time     the time.
     * @param timeUnit the time unit.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> staleAfter(long time, @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code withLoaders().withResultStaleTime(staleTime).getConfigured()}.
     *
     * @param staleTime the stale time.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> staleAfter(@Nullable TimeDuration staleTime);

    /**
     * Sets the stream loader context.<br/>
     * The context will be used by all the concatenated routines until changed.<br/>
     * If null it will cause the next routines to employ the configured runner instead of an Android
     * loader.
     *
     * @param context the loader context.
     * @return the configured stream.
     */
    @NotNull
    LoaderStreamChannelCompat<OUT> with(@Nullable LoaderContextCompat context);

    /**
     * Gets the loader configuration builder related only to the next concatenated routine instance.
     * Any further addition to the chain will retain only the stream configuration.<br/>
     * Only the options set in this configuration (that is, the ones with a value different from the
     * default) will override the stream one.
     * <p/>
     * Note that the configuration builder will be initialized with the current configuration for
     * the next routine.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends LoaderStreamChannelCompat<OUT>> withLoaders();

    /**
     * Gets the loader configuration builder related to the whole stream.<br/>
     * The configuration options will be applied to all the next concatenated routine unless
     * overwritten by specific ones.
     * <p/>
     * Note that the configuration builder will be initialized with the current stream
     * configuration.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends LoaderStreamChannelCompat<OUT>> withStreamLoaders();
}
