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
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining a builder of stream routines.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 */
public interface StreamRoutineBuilder extends ConfigurableBuilder<StreamRoutineBuilder> {

    /**
     * Concatenates a stream routine based on the specified accumulate function.
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
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the bi-function instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> asyncAccumulate(
            @NotNull BiFunction<? super DATA, ? super DATA, DATA> function);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine exception will be further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> asyncError(
            @NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a stream routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> asyncFilter(@NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine outputs will be not further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, Void> asyncForEach(@NotNull Consumer<? super DATA> consumer);

    /**
     * Builds and returns a new stream routine generating outputs from the specified command
     * invocation.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncFrom(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Builds and returns a new stream routine generating outputs from the specified consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncFrom(
            @NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Builds and returns a new stream routine generating outputs from the specified supplier.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncFrom(@NotNull Supplier<OUT> supplier);

    /**
     * Concatenates a stream routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream routine based on the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncMap(@NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a stream routine based on the specified factory.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncMap(@NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a stream routine based on the specified instance.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Builds and returns a new stream routine generating the specified outputs.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable Iterable<OUT> outputs);

    /**
     * Builds and returns a new stream routine generating the specified output.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable OUT output);

    /**
     * Builds and returns a new stream routine generating the specified outputs.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> asyncOf(@Nullable OUT... outputs);

    /**
     * Concatenates a stream routine based on the specified reducing consumer.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream routine based on the specified reducing function.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> asyncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);

    /**
     * Concatenates a stream routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> parallelFilter(@NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a stream routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> parallelMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream routine based on the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> parallelMap(@NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a stream routine based on the specified factory.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> parallelMap(@NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a stream routine based on the specified instance.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> parallelMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Concatenates a stream routine based on the specified accumulate function.
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
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the bi-function instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> syncAccumulate(
            @NotNull BiFunction<? super DATA, ? super DATA, DATA> function);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine exception will be further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> syncError(
            @NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a stream routine based on the specified predicate.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param predicate the predicate instance.
     * @param <DATA>    the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, DATA> syncFilter(@NotNull Predicate<? super DATA> predicate);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine outputs will be not further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <DATA>   the data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <DATA> StreamRoutine<DATA, Void> syncForEach(@NotNull Consumer<? super DATA> consumer);

    /**
     * Builds and returns a new stream routine generating outputs from the specified command
     * invocation.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncFrom(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Builds and returns a new stream routine generating outputs from the specified consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncFrom(@NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Builds and returns a new stream routine generating outputs from the specified supplier.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncFrom(@NotNull Supplier<OUT> supplier);

    /**
     * Concatenates a stream routine based on the specified consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncMap(
            @NotNull BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream routine based on the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncMap(@NotNull Function<? super IN, OUT> function);

    /**
     * Concatenates a stream routine based on the specified factory.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncMap(@NotNull InvocationFactory<IN, OUT> factory);

    /**
     * Concatenates a stream routine based on the specified instance.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param routine the routine instance.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncMap(@NotNull Routine<IN, OUT> routine);

    /**
     * Builds and returns a new stream routine generating the specified outputs.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param outputs the iterable returning the output data.
     * @param <OUT>   the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable Iterable<OUT> outputs);

    /**
     * Builds and returns a new stream routine generating the specified output.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param output the output.
     * @param <OUT>  the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable OUT output);

    /**
     * Builds and returns a new stream routine generating the specified outputs.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param outputs the output data.
     * @param <OUT>   the output data type.
     * @return the newly created routine instance.
     */
    @NotNull
    <OUT> StreamRoutine<Void, OUT> syncOf(@Nullable OUT... outputs);

    /**
     * Concatenates a stream routine based on the specified reducing consumer.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncReduce(
            @NotNull BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream routine based on the specified reducing function.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <IN, OUT> StreamRoutine<IN, OUT> syncReduce(
            @NotNull Function<? super List<? extends IN>, OUT> function);
}
