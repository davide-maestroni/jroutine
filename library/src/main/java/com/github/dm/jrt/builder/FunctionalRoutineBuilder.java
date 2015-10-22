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
import com.github.dm.jrt.functional.Consumer;
import com.github.dm.jrt.functional.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of functional routines.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 */
public interface FunctionalRoutineBuilder extends ConfigurableBuilder<FunctionalRoutineBuilder> {

    /**
     * Builds and returns a new functional routine generating outputs from the specified command
     * invocation.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> buildFrom(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Builds and returns a new functional routine generating outputs from the specified consumer.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Builds and returns a new functional routine generating outputs from the specified supplier.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> buildFrom(@NotNull Supplier<OUT> supplier);

    /**
     * Builds and returns a functional routine.
     *
     * @param <DATA> the data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> buildRoutine();
}
