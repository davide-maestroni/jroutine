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

import com.github.dm.jrt.android.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions, employing Android loaders to run the backing routines.<br/>
 * In fact, each function in the channel is backed by a routine instance, that can have its own
 * specific configuration and invocation mode.
 * <p/>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the invocation completes.
 * <p/>
 * Created by davide-maestroni on 01/15/2016.
 *
 * @param <OUT> the output data type.
 */
public interface LoaderStreamChannel<OUT>
        extends StreamChannel<OUT>, LoaderConfigurableBuilder<LoaderStreamChannel<OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> async();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs, long maxDelay,
            @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable TimeDuration maxDelay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> collect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> collect(
            @NotNull Function<? super List<? extends OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<Void> forEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(@Nullable AFTER output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(@Nullable AFTER... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> generate(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamChannel<AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> maxParallelInvocations(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> ordered(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> parallel();

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> LoaderStreamChannel<AFTER> range(@NotNull final AFTER start,
            @NotNull final AFTER end, @NotNull final Function<AFTER, AFTER> increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<Number> range(@NotNull final Number start, @NotNull final Number end);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<Number> range(@NotNull final Number start, @NotNull final Number end,
            @NotNull final Number increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> repeat();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> runOn(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> runOnShared();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> sync();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<? extends ParcelableSelectable<OUT>> toSelectable(int index);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamChannel<OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withInvocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withStreamInvocations();

    /**
     * Short for {@code withLoaders().withCacheStrategy(strategyType).set()}.
     *
     * @param strategyType the cache strategy type.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> cache(@Nullable CacheStrategyType strategyType);

    /**
     * Short for {@code withLoaders().withId(loaderId).set()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the loader ID.
     *
     * @param loaderId the loader ID.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> loaderId(int loaderId);

    /**
     * Short for {@code withLoaders().withRoutineId(routineId).set()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine ID.
     *
     * @param routineId the routine ID.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> routineId(int routineId);

    /**
     * Short for {@code withLoaders().withResultStaleTime(time, timeUnit).set()}.
     *
     * @param time     the time.
     * @param timeUnit the time unit.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> staleAfter(long time, @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code withLoaders().withResultStaleTime(staleTime).set()}.
     *
     * @param staleTime the stale time.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> staleAfter(@Nullable TimeDuration staleTime);

    /**
     * Sets the stream loader context.<br/>
     * The context will be used by all the concatenated routines until changed.<br/>
     * If null it will cause the next routines to employ the configured runner instead of an Android
     * loader.
     *
     * @param context the loader context.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamChannel<OUT> with(@Nullable LoaderContext context);

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
    LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withLoaders();

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
    LoaderConfiguration.Builder<? extends LoaderStreamChannel<OUT>> withStreamLoaders();
}
