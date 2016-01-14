/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.android.stream;

import com.github.dm.jrt.android.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.android.builder.LoaderConfiguration;
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
import com.github.dm.jrt.stream.StreamOutputChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TODO ...
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions.
 * <br/>
 * Each function in the channel is backed by a sub-routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p/>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the invocation completes.
 * <p/>
 * Created by davide-maestroni on 01/12/2016.
 *
 * @param <OUT> the output data type.
 */
public interface LoaderStreamOutputChannel<OUT> extends StreamOutputChannel<OUT>,
        LoaderConfigurableBuilder<LoaderStreamOutputChannel<OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncCollect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncCollect(
            @NotNull Function<? super List<? extends OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> asyncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Void> asyncForEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncGenerate(
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncLift(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> asyncMap(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> asyncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> asyncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> asyncReduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            long maxDelay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable TimeDuration maxDelay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> maxParallelInvocations(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> ordered(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> parallelFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelGenerate(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelGenerate(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelLift(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> parallelMap(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> parallelRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> parallelRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> runOn(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> runOnShared();

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncCollect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncCollect(
            @NotNull Function<? super List<? extends OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> syncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Void> syncForEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncGenerate(
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncLift(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> LoaderStreamOutputChannel<AFTER> syncMap(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> LoaderStreamOutputChannel<AFTER> syncRange(
            @NotNull final AFTER start, @NotNull final AFTER end,
            @NotNull final Function<AFTER, AFTER> increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<Number> syncRange(@NotNull final Number start,
            @NotNull final Number end, @NotNull final Number increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> syncReduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>> withInvocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>>
    withStreamInvocations();

    /**
     * Short for {@code withLoaders().withId(loaderId).set()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will force the routine loader ID.
     *
     * @param loaderId the loader ID.
     * @return the configured stream channel.
     */
    @NotNull
    LoaderStreamOutputChannel<OUT> loadersId(int loaderId);

    // TODO: 1/12/16 javadoc
    @NotNull
    LoaderConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>> withLoaders();

    // TODO: 1/12/16 javadoc
    @NotNull
    LoaderConfiguration.Builder<? extends LoaderStreamOutputChannel<OUT>> withStreamLoaders();
}
