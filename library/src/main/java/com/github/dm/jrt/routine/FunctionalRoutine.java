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
package com.github.dm.jrt.routine;

import com.github.dm.jrt.builder.ConfigurableBuilder;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.functional.BiConsumer;
import com.github.dm.jrt.functional.BiFunction;
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.functional.Predicate;
import com.github.dm.jrt.invocation.FilterInvocation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by davide-maestroni on 10/16/2015.
 */
public interface FunctionalRoutine<IN, OUT>
        extends Routine<IN, OUT>, ConfigurableBuilder<FunctionalRoutine<IN, OUT>> {

    @NotNull
    FunctionalRoutine<IN, OUT> asyncAccumulate(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    @NotNull
    FunctionalRoutine<IN, OUT> asyncFilter(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> asyncLift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(@NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(@NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends
                    FunctionalRoutine<BEFORE, AFTER>> function);

    @NotNull
    FunctionalRoutine<IN, OUT> parallelFilter(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> parallelLift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(@NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelReduce(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelReduce(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    @NotNull
    FunctionalRoutine<IN, OUT> syncAccumulate(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    @NotNull
    FunctionalRoutine<IN, OUT> syncFilter(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> syncLift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(@NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(@NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);
}
