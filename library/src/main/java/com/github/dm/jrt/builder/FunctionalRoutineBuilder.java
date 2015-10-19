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
     * Creates a new functional routine based on the specified command invocation.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> async(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional routine based on the specified supplier.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> async(@NotNull Supplier<OUT> supplier);

    /**
     * Creates a new functional routine based on the specified bi-function.<br/>
     * The input will be accumulated as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the routine invocation completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the bi-function instance.
     * @param <IN>     the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> asyncAccumulate(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    /**
     * Creates a new functional routine based on the specified predicate.<br/>
     * The input will be filtered accordingly to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> asyncFilter(@NotNull Predicate<? super IN> predicate);

    /**
     * Creates a new functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified filter invocation.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional routine wrapping specified one.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Creates a new functional routine based on the specified bi-consumer.<br/>
     * The inputs will be reduced by applying the consumer only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified function.<br/>
     * The inputs will be reduced by applying the function only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> asyncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    /**
     * Creates a new functional routine based on the specified command invocation.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> parallel(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional routine based on the specified supplier.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> parallel(@NotNull Supplier<OUT> supplier);

    /**
     * Creates a new functional routine based on the specified predicate.<br/>
     * The input will be filtered accordingly to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> parallelFilter(@NotNull Predicate<? super IN> predicate);

    /**
     * Creates a new functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified filter invocation.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional routine wrapping specified one.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Creates a new functional routine based on the specified bi-consumer.<br/>
     * The inputs will be reduced by applying the consumer only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified function.<br/>
     * The inputs will be reduced by applying the function only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> parallelReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    /**
     * Creates a new functional routine based on the specified command invocation.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> sync(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional routine based on the specified supplier.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> sync(@NotNull Supplier<OUT> supplier);

    /**
     * Creates a new functional routine based on the specified bi-function.<br/>
     * The input will be accumulated as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the routine invocation completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the bi-function instance.
     * @param <IN>     the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> syncAccumulate(
            @NotNull BiFunction<? super IN, ? super IN, ? extends IN> function);

    /**
     * Creates a new functional routine based on the specified predicate.<br/>
     * The input will be filtered accordingly to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <IN>      the input data type.
     * @return the functional routine.
     */
    @NotNull
    <IN> FunctionalRoutine<IN, IN> syncFilter(@NotNull Predicate<? super IN> predicate);

    /**
     * Creates a new functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified filter invocation.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional routine wrapping specified one.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Creates a new functional routine based on the specified bi-consumer.<br/>
     * The inputs will be reduced by applying the consumer only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional routine based on the specified function.<br/>
     * The inputs will be reduced by applying the function only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> syncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);
}
