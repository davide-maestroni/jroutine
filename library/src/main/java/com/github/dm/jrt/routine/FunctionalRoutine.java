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
 * Interface defining a functional routine, that is, a routine concatenating map and reduce
 * functions.<br/>
 * Each function in the channel is backed by a sub-routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p/>
 * Note that, when at least one reduce function is part of the concatenation, the results will be
 * propagated only when the invocation completes.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface FunctionalRoutine<IN, OUT>
        extends Routine<IN, OUT>, ConfigurableBuilder<FunctionalRoutine<IN, OUT>> {

    /**
     * Concatenates a functional routine based on the specified accumulate function to this one.
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
     * @return the concatenated functional routine.
     */
    @NotNull
    FunctionalRoutine<IN, OUT> asyncAccumulate(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Concatenates a functional routine based on the specified predicate to this ones.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated functional routine.
     */
    @NotNull
    FunctionalRoutine<IN, OUT> asyncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a functional routine based on the specified invocation to this one.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param invocation the filter invocation instance.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Concatenates a functional routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(@NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a functional routine based on the specified instance to this one.
     * <p/>
     * Note that the passed routine will be invoked in an asynchronous mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a functional routine based on the specified reducing consumer to this one.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Concatenates a functional routine based on the specified reducing function to this one.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in an asynchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> asyncReduce(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    /**
     * Lifts this functional routine by applying the specified function.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted functional routine.
     */
    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> flatLift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends
                    FunctionalRoutine<BEFORE, AFTER>> function);

    /**
     * Lifts this functional routine by applying the specified function.
     *
     * @param function the function instance.
     * @param <BEFORE> the lifting input type.
     * @param <AFTER>  the lifting output type.
     * @return the lifted functional routine.
     */
    @NotNull
    <BEFORE, AFTER> FunctionalRoutine<BEFORE, AFTER> lift(
            @NotNull Function<? super FunctionalRoutine<IN, OUT>, ? extends Routine<BEFORE,
                    AFTER>> function);

    /**
     * Concatenates a functional routine based on the specified predicate to this ones.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated functional routine.
     */
    @NotNull
    FunctionalRoutine<IN, OUT> parallelFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);


    /**
     * Concatenates a functional routine based on the specified invocation to this one.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param invocation the filter invocation instance.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Concatenates a functional routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(
            @NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a functional routine based on the specified instance to this one.
     * <p/>
     * Note that the passed routine will be invoked in a parallel mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> parallelMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a functional routine based on the specified accumulate function to this one.
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
     * @return the concatenated functional routine.
     */
    @NotNull
    FunctionalRoutine<IN, OUT> syncAccumulate(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Concatenates a functional routine based on the specified predicate to this ones.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param predicate the predicate instance.
     * @return the concatenated functional routine.
     */
    @NotNull
    FunctionalRoutine<IN, OUT> syncFilter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a functional routine based on the specified consumer to this one.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);


    /**
     * Concatenates a functional routine based on the specified invocation to this one.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param invocation the filter invocation instance.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Concatenates a functional routine based on the specified function to this one.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(@NotNull Function<? super OUT, AFTER> function);

    /**
     * Concatenates a functional routine based on the specified instance to this one.
     * <p/>
     * Note that the passed routine will be invoked in a synchronous mode.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncMap(@NotNull Routine<? super OUT, AFTER> routine);

    /**
     * Concatenates a functional routine based on the specified reducing consumer to this one.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Concatenates a functional routine based on the specified reducing function to this one.<br/>
     * The outputs will be reduced by applying the function, only when the routine invocation
     * completes.
     * <p/>
     * Note that the created routine will be invoked in a synchronous mode.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated functional routine.
     */
    @NotNull
    <AFTER> FunctionalRoutine<IN, AFTER> syncReduce(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);
}
