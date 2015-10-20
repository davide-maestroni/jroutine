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
 * Interface defining a builder of functional routines.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 */
public interface FunctionalRoutineBuilder extends ConfigurableBuilder<FunctionalRoutineBuilder> {

    /**
     * Creates a new functional routine based on the specified accumulate function.<br/>
     * The input will be accumulated as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the routine invocation completes.
     *
     * @param function the bi-function instance.
     * @param <IN>     the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> accumulate(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    /**
     * Creates a new functional routine based on the specified predicate.<br/>
     * The input will be filtered accordingly to the result returned by the predicate.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> filter(@NotNull Predicate<? super IN> predicate);

    /**
     * Creates a new functional routine based on the specified command invocation.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> from(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional routine based on the specified supplier.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> from(@NotNull Supplier<OUT> supplier);

    /**
     * Creates a new functional routine based on the specified consumer.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> map(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified filter invocation.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional routine based on the specified function.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional routine wrapping specified one.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> map(@NotNull Routine<IN, OUT> routine);

    /**
     * Creates a new functional routine based on the specified bi-consumer.<br/>
     * The inputs will be reduced by applying the consumer only when the routine invocation
     * completes.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> reduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified function.<br/>
     * The inputs will be reduced by applying the function only when the routine invocation
     * completes.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> reduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);
}
