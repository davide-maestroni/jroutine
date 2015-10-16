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
import com.github.dm.jrt.functional.Function;
import com.github.dm.jrt.invocation.FilterInvocation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by davide-maestroni on 10/16/2015.
 */
public interface FunctionalRoutine<IN, OUT>
        extends Routine<IN, OUT>, ConfigurableBuilder<FunctionalRoutine<IN, OUT>> {

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
}
