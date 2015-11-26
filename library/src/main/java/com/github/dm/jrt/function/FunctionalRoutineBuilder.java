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
package com.github.dm.jrt.function;

import com.github.dm.jrt.builder.ConfigurableBuilder;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
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
     * Builds and returns a new functional routine generating outputs from the specified command
     * invocation.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> from(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Builds and returns a new functional routine generating outputs from the specified consumer.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> from(@NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Builds and returns a new functional routine generating outputs from the specified supplier.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> FunctionalRoutine<Void, OUT> from(@NotNull Supplier<OUT> supplier);

    /**
     * Concatenates a functional routine based on the specified accumulate function.
     * <br/>
     * The output will be accumulated as follows:
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
     * @param <DATA>   the data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> thenAsyncAccumulate(
            @NotNull BiFunction<? super DATA, ? super DATA, DATA> function);

    /**
     * Concatenates a functional routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> thenAsyncFilter(
            @NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(@NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a functional routine based on the specified factory.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(@NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a functional routine based on the specified instance.
     * <p/>
     * Note that the passed routine will be invoked in an asynchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Concatenates a functional routine based on the specified reducing consumer.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a functional routine based on the specified reducing function.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenAsyncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    /**
     * Concatenates a functional routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> thenParallelFilter(
            @NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
            @NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a functional routine based on the specified factory.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(
            @NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a functional routine based on the specified instance.
     * <p/>
     * Note that the passed routine will be invoked in a parallel mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenParallelMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Concatenates a functional routine based on the specified accumulate function.
     * <br/>
     * The output will be accumulated as follows:
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
     * @param <DATA>   the data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> thenSyncAccumulate(
            @NotNull BiFunction<? super DATA, ? super DATA, DATA> function);

    /**
     * Concatenates a functional routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <DATA> FunctionalRoutine<DATA, DATA> thenSyncFilter(@NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a functional routine based on the specified function.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(@NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a functional routine based on the specified factory.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(@NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a functional routine based on the specified instance.
     * <p/>
     * Note that the passed routine will be invoked in a synchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Concatenates a functional routine based on the specified reducing consumer.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a functional routine based on the specified reducing function.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <IN, OUT> FunctionalRoutine<IN, OUT> thenSyncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);
}
