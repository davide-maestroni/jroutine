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
    FunctionalRoutine<IN, OUT> andThenAccumulateAsync(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    @NotNull
    FunctionalRoutine<IN, OUT> andThenAccumulateSync(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    @NotNull
    FunctionalRoutine<IN, OUT> andThenFilterAsync(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    FunctionalRoutine<IN, OUT> andThenFilterParallel(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    FunctionalRoutine<IN, OUT> andThenFilterSync(@NotNull Predicate<? super OUT> predicate);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapAsync(
            @NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapParallel(
            @NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull Function<? super OUT, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenMapSync(
            @NotNull Routine<? super OUT, AFTER> routine);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceAsync(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceAsync(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceParallel(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceParallel(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceSync(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> andThenReduceSync(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    @NotNull
    FunctionalRoutine<IN, OUT> composeAccumulateAsync(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    @NotNull
    FunctionalRoutine<IN, OUT> composeAccumulateSync(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    @NotNull
    FunctionalRoutine<IN, OUT> composeFilterAsync(@NotNull Predicate<? super IN> predicate);

    @NotNull
    FunctionalRoutine<IN, OUT> composeFilterParallel(@NotNull Predicate<? super IN> predicate);

    @NotNull
    FunctionalRoutine<IN, OUT> composeFilterSync(@NotNull Predicate<? super IN> predicate);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull Function<BEFORE, ? extends IN> function);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapAsync(
            @NotNull Routine<BEFORE, ? extends IN> routine);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull Function<BEFORE, ? extends IN> function);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapParallel(
            @NotNull Routine<BEFORE, ? extends IN> routine);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull Function<BEFORE, ? extends IN> function);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeMapSync(
            @NotNull Routine<BEFORE, ? extends IN> routine);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceAsync(
            @NotNull BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceAsync(
            @NotNull Function<? super List<? extends BEFORE>, ? extends IN> function);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceParallel(
            @NotNull BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceParallel(
            @NotNull Function<? super List<? extends BEFORE>, ? extends IN> function);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceSync(
            @NotNull BiConsumer<? super List<? extends BEFORE>, ? super ResultChannel<IN>>
                    consumer);

    @NotNull
    <BEFORE> FunctionalRoutine<BEFORE, OUT> composeReduceSync(
            @NotNull Function<? super List<? extends BEFORE>, ? extends IN> function);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftAsync(
            @NotNull Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>> function);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftParallel(
            @NotNull Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>> function);

    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> liftSync(
            @NotNull Function<? super Routine<IN, OUT>, ? extends Routine<BEFORE, AFTER>> function);
}
