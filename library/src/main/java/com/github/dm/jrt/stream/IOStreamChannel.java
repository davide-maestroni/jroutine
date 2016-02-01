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

import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 01/31/2016.
 */
public interface IOStreamChannel<IN, OUT> extends InputChannel<IN>, StreamChannel<OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> async();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> backPressureOn(@Nullable Runner runner, int maxInputs, long maxDelay,
            @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable TimeDuration maxDelay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> bind();

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> collect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> collect(
            @NotNull Function<? super List<? extends OUT>, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, Void> forEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(@Nullable AFTER output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(@Nullable AFTER... outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(long count,
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> generate(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER> IOStreamChannel<IN, AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> maxParallelInvocations(int maxInvocations);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> ordered(@Nullable OrderType orderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> parallel();

    /**
     * {@inheritDoc}
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> StreamChannel<AFTER> range(@NotNull AFTER start,
            @NotNull AFTER end, @NotNull Function<AFTER, AFTER> increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, Number> range(@NotNull Number start, @NotNull Number end);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, Number> range(@NotNull Number start, @NotNull Number end,
            @NotNull Number increment);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> repeat();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> runOn(@Nullable Runner runner);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> runOnShared();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> sync();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, ? extends Selectable<OUT>> toSelectable(int index);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOStreamChannel<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    Builder<? extends IOStreamChannel<IN, OUT>> withInvocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    Builder<? extends IOStreamChannel<IN, OUT>> withStreamInvocations();

    /**
     * Returns this channel as an input one.
     *
     * @return this channel.
     */
    @NotNull
    InputChannel<IN> asInput();

    /**
     * Returns this channel as an output one.
     *
     * @return this channel.
     */
    @NotNull
    OutputChannel<OUT> asOutput();

    /**
     * Closes the channel input.<br/>
     * If the channel is already closed, this method has no effect.
     * <p/>
     * Note that this method must be always called when done with the channel.
     *
     * @return this channel.
     */
    @NotNull
    IOStreamChannel<IN, OUT> close();
}
