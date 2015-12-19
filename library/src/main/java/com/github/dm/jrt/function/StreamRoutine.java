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
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface defining a stream routine, that is, a routine concatenating map and reduce functions.
 * <br/>
 * Each function in the channel is backed by a sub-routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p/>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the invocation completes.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamRoutine<IN, OUT>
        extends Routine<IN, OUT>, ConfigurableBuilder<StreamRoutine<IN, OUT>> {

    /**
     * Concatenates a stream routine based on the specified collecting consumer to this one.<br/>
     * The outputs will be collected by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncCollect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Concatenates a stream routine based on the specified collecting function to this one.<br/>
     * The outputs will be collected by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncCollect(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    /**
     * Concatenates a stream routine based on the specified predicate to this one.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> asyncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine outputs will not be further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, Void> asyncForEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The consumer will be called only when the invocation completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncGenerate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream routine based on the specified supplier to this one.<br/>
     * The supplier will be called {@code count} number of times only when the invocation completes.
     * The count number must be positived.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative..
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncGenerate(long count, @NotNull Supplier<AFTER> supplier);

    /**
     * Concatenates a stream routine based on the specified supplier to this one.<br/>
     * The supplier will be called only when the invocation completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncGenerate(@NotNull Supplier<AFTER> supplier);

    /**
     * Lifts this stream routine by applying the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted stream routine.
     */
    @NotNull
    <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> asyncLift(
            @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncMap(@NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a stream routine based on the specified factory to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncMap(
            @NotNull InvocationFactory<? super OUT, AFTER> factory);

    /**
     * Concatenates a stream routine based on the specified instance to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> asyncMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a stream routine based on the specified accumulating function to this one.
     * <br/>
     * The output will be computed as follows:
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
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> asyncReduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Lifts this stream routine by applying the specified function.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted stream routine.
     */
    @NotNull
    <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> flatLift(
            @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends
                    StreamRoutine<BEFORE, AFTER>> function);

    /**
     * Concatenates a stream routine based on the specified predicate to this one.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> parallelFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream routine based on the specified supplier to this one.<br/>
     * The supplier will be called {@code count} number of times only when the invocation completes.
     * If count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> parallelGenerate(long count,
            @NotNull Supplier<AFTER> supplier);

    /**
     * Lifts this stream routine by applying the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted stream routine.
     */
    @NotNull
    <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> parallelLift(
            @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> parallelMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> parallelMap(@NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a stream routine based on the specified factory to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> parallelMap(
            @NotNull InvocationFactory<? super OUT, AFTER> factory);

    /**
     * Concatenates a stream routine based on the specified instance to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a parallel mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> parallelMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a stream routine based on the specified collecting consumer to this one.<br/>
     * The outputs will be collected by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncCollect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Concatenates a stream routine based on the specified collecting function to this one.<br/>
     * The outputs will be collected by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncCollect(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    /**
     * Concatenates a stream routine based on the specified predicate to this one.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> syncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The routine outputs will not be further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, Void> syncForEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.<br/>
     * The consumer will be called only when the invocation completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in an asynchronous mode.
     *
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncGenerate(
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream routine based on the specified supplier to this one.<br/>
     * The supplier will be called {@code count} number of times only when the invocation completes.
     * If count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncGenerate(long count, @NotNull Supplier<AFTER> supplier);

    /**
     * Concatenates a stream routine based on the specified supplier to this one.<br/>
     * The supplier will be called only when the invocation completes.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncGenerate(@NotNull Supplier<AFTER> supplier);

    /**
     * Lifts this stream routine by applying the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted stream routine.
     */
    @NotNull
    <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> syncLift(
            @NotNull Function<? super StreamRoutine<IN, OUT>, ? extends Routine<BEFORE, AFTER>>
                    function);

    /**
     * Concatenates a stream routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncMap(@NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a stream routine based on the specified factory to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncMap(
            @NotNull InvocationFactory<? super OUT, AFTER> factory);

    /**
     * Concatenates a stream routine based on the specified instance to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream routine.
     */
    @NotNull
    <AFTER> StreamRoutine<IN, AFTER> syncMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a stream routine based on the specified accumulating function to this one.
     * <br/>
     * The output will be computed as follows:
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
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> syncReduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Concatenates a consumer handling a routine exception.<br/>
     * The error will not be automatically further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * Concatenates a consumer handling a routine exception.<br/>
     * The error will not be automatically further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a function handling a routine exception.<br/>
     * The error will not be automatically further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration and will be
     * invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @return the concatenated stream routine.
     */
    @NotNull
    StreamRoutine<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);
}
