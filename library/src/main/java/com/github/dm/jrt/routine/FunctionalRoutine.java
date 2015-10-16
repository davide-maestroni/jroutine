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
}
