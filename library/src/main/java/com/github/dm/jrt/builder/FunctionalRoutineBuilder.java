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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.functional.BiConsumer;
import com.github.dm.jrt.functional.BiFunction;
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.functional.Predicate;
import com.github.dm.jrt.functional.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by davide-maestroni on 10/18/2015.
 */
public interface FunctionalRoutineBuilder extends ConfigurableBuilder<FunctionalRoutineBuilder> {

    @NotNull
    <IN> FunctionalRoutine<IN, IN> asyncAccumulate(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    @NotNull
    <IN> FunctionalRoutine<IN, IN> asyncFilter(@NotNull Predicate<? super IN> predicate);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> asyncMap(@NotNull CommandInvocation<OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull FilterInvocation<IN, OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull Function<IN, OUT> function);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull Routine<IN, OUT> routine);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> asyncMap(@NotNull Supplier<OUT> supplier);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    @NotNull
    <IN> FunctionalRoutine<IN, IN> parallelFilter(@NotNull Predicate<? super IN> predicate);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> parallelMap(@NotNull CommandInvocation<OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull FilterInvocation<IN, OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull Function<IN, OUT> function);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull Routine<IN, OUT> routine);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> parallelMap(@NotNull Supplier<OUT> supplier);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    @NotNull
    <IN> FunctionalRoutine<IN, IN> syncAccumulate(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    @NotNull
    <IN> FunctionalRoutine<IN, IN> syncFilter(@NotNull Predicate<? super IN> predicate);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> syncMap(@NotNull CommandInvocation<OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull FilterInvocation<IN, OUT> invocation);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull Function<IN, OUT> function);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull Routine<IN, OUT> routine);

    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> syncMap(@NotNull Supplier<OUT> supplier);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);
}
