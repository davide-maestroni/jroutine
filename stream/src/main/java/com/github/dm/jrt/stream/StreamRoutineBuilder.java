/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.annotation.StreamFlow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.COLLECT;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.CONFIG;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.MAP;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.REDUCE;

/**
 * Interface defining a builder of routines concatenating map and reduce functions.
 * <br>
 * Each function in the stream will be backed by a routine instance, which may have its own
 * specific configuration and invocation mode.
 * <p>
 * To better document the effect of each method on the underlying stream, a {@link StreamFlow}
 * annotation indicates for each one the type of transformation applied.
 * <br>
 * Note also that, if at least one reduce function is part of the chain, the results will be
 * propagated only when the built routine invocation completes.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamRoutineBuilder<IN, OUT> extends RoutineBuilder<IN, OUT> {

    /**
     * Concatenates a channel appending the specified output.
     * <br>
     * The output will be appended to the ones produced by the previous routine.
     *
     * @param output the output to append.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> append(@Nullable OUT output);

    /**
     * Concatenates a channel appending the specified outputs.
     * <br>
     * The outputs will be appended to the ones produced by the previous routine.
     *
     * @param outputs the outputs to append.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> append(@Nullable OUT... outputs);

    /**
     * Concatenates a channel appending the specified outputs.
     * <br>
     * The outputs will be appended to the ones produced by the previous routine.
     *
     * @param outputs the iterable returning the outputs to append.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> append(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Concatenates a channel appending the specified channel outputs.
     * <br>
     * The outputs will be appended to the ones produced by the previous routine.
     *
     * @param channel the output channel.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> append(@NotNull Channel<?, ? extends OUT> channel);

    /**
     * Concatenates a routine appending the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocation completes. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> appendGet(long count,
            @NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a routine appending the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputSupplier the supplier instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> appendGet(@NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a routine appending the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations completes. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> appendGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Concatenates a routine appending the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputsConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> appendGetMore(
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> async();

    /**
     * Short for {@code async().streamInvocationConfiguration().withRunner(runner).applied()}.
     * <br>
     * This method is useful to easily set the stream runner.
     * <p>
     * Note that output data will not be automatically delivered through the runner.
     *
     * @param runner the runner instance.
     * @return the this builder.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> async(@Nullable Runner runner);

    /**
     * Short for {@code async(runner).map(IdentityInvocation.&lt;OUT&gt;factoryOf())}.
     * <br>
     * This method is useful to easily make the stream run on the specified runner.
     * <p>
     * Note that it is not necessary to explicitly concatenate a routine to have a stream delivering
     * the output data through the specified runner.
     *
     * @param runner the runner instance.
     * @return this builder.
     * @see #async(Runner)
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> asyncMap(@Nullable Runner runner);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(backoff).applied()}.
     * <br>
     * This method is useful to easily apply a configuration which will slow down the thread
     * feeding the next routine concatenated to the stream, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner  the configured runner.
     * @param limit   the maximum number of buffered inputs before starting to slow down the
     *                feeding thread.
     * @param backoff the backoff policy to apply to the feeding thread.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit,
            @NotNull Backoff backoff);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay, timeUnit).applied()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will slow down the thread feeding it, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner   the configured runner.
     * @param limit    the maximum number of buffered inputs before starting to slow down the
     *                 feeding thread.
     * @param delay    the constant delay to apply to the feeding thread.
     * @param timeUnit the delay time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified limit or the specified delay are
     *                                            negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit, long delay,
            @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay).applied()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will slow down the thread feeding it, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner the configured runner.
     * @param limit  the maximum number of buffered inputs before starting to slow down the
     *               feeding thread.
     * @param delay  the constant delay to apply to the feeding thread.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> backoffOn(@Nullable Runner runner, int limit,
            @Nullable UnitDuration delay);

    /**
     * Concatenates a routine accumulating data through the specified consumer.
     * <br>
     * The output will be computed as follows:
     * <pre>
     *     <code>
     *
     *         consumer.accept(acc, input);
     *     </code>
     * </pre>
     * where the initial accumulated value will be the the first input.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param accumulateConsumer the bi-consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamRoutineBuilder<IN, OUT> collect(
            @NotNull BiConsumer<? super OUT, ? super OUT> accumulateConsumer);

    /**
     * Concatenates a routine accumulating data through the specified consumer.
     * <br>
     * The output will be computed as follows:
     * <pre>
     *     <code>
     *
     *         consumer.accept(acc, input);
     *     </code>
     * </pre>
     * where the initial accumulated value will be the one returned by the specified supplier.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateConsumer the bi-consumer instance.
     * @param <AFTER>            the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamRoutineBuilder<IN, AFTER> collect(@NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiConsumer<? super AFTER, ? super OUT> accumulateConsumer);

    /**
     * Concatenates a routine accumulating the outputs by adding them to the collections returned by
     * the specified supplier.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param collectionSupplier the supplier of collections.
     * @param <AFTER>            the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER extends Collection<? super OUT>> StreamRoutineBuilder<IN, AFTER> collectInto(
            @NotNull Supplier<? extends AFTER> collectionSupplier);

    /**
     * Adds a delay at the end of this stream, so that any data, exception or completion
     * notification will be dispatched to the next concatenated routine after the specified time.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> delay(long delay, @NotNull TimeUnit timeUnit);

    /**
     * Adds a delay at the end of this stream, so that any data, exception or completion
     * notification will be dispatched to the next concatenated routine after the specified time.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param delay the delay.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> delay(@NotNull UnitDuration delay);

    /**
     * Concatenates a routine filtering data based on the values returned by the specified
     * predicate.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param filterPredicate the predicate instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> filter(@NotNull Predicate<? super OUT> filterPredicate);

    /**
     * Transforms this stream by applying the specified function.
     * <br>
     * This method provides a convenient way to apply a set of configurations and concatenations
     * without breaking the fluent chain.
     *
     * @param liftFunction the lift function.
     * @param <BEFORE>     the concatenation input type.
     * @param <AFTER>      the concatenation output type.
     * @return the lifted builder.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> flatLift(
            @NotNull Function<? super StreamRoutineBuilder<IN, OUT>, ? extends
                    StreamRoutineBuilder<BEFORE, AFTER>> liftFunction);

    /**
     * Concatenates a routine mapping this stream outputs by applying the specified function to each
     * one of them.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(CONFIG)
    Builder<? extends StreamRoutineBuilder<IN, OUT>> invocationConfiguration();

    /**
     * Makes the stream invoke concatenated routines with the specified mode.
     *
     * @param invocationMode the invocation mode.
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> invocationMode(@NotNull InvocationMode invocationMode);

    /**
     * Adds a delay at the beginning of this stream, so that any data, exception or completion
     * notification coming from the source will be dispatched to this stream after the specified
     * time.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> lag(long delay, @NotNull TimeUnit timeUnit);

    /**
     * Adds a delay at the beginning of this stream, so that any data, exception or completion
     * notification coming from the source will be dispatched to this stream after the specified
     * time.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param delay the delay.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> lag(@NotNull UnitDuration delay);

    /**
     * Transforms the stream by modifying the chain building function.
     * <br>
     * The returned function will be employed when the routine instance is built (see
     * {@link #buildRoutine()}).
     *
     * @param liftFunction the function modifying the flow one.
     * @param <BEFORE>     the concatenation input type.
     * @param <AFTER>      the concatenation output type.
     * @return this builder.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> lift(
            @NotNull Function<? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

    /**
     * Transforms the stream by modifying the chain building function.
     * <br>
     * The current configuration of the stream will be passed as the first parameter.
     * <br>
     * The returned function will be employed when the routine instance is built (see
     * {@link #buildRoutine()}).
     *
     * @param liftFunction the bi-function modifying the flow one.
     * @param <BEFORE>     the concatenation input type.
     * @param <AFTER>      the concatenation output type.
     * @return this builder.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamRoutineBuilder<BEFORE, AFTER> liftConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

    /**
     * Concatenates a routine limiting the maximum number of outputs to the specified count.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count the maximum number of outputs.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> limit(int count);

    /**
     * Concatenates a routine mapping this stream outputs by applying the specified function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> mappingFunction);

    /**
     * Concatenates a routine mapping this stream outputs through the specified invocation factory.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Concatenates a routine mapping this stream outputs through the specified routine.
     * <p>
     * Note that the stream configuration will be ignored.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Concatenates a routine mapping this stream outputs through the specified routine builder.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param builder the routine builder instance.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> map(
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Concatenates a routine mapping the whole collection of outputs by applying the specified
     * function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamRoutineBuilder<IN, AFTER> mapAll(
            @NotNull Function<? super List<OUT>, ? extends AFTER> mappingFunction);

    /**
     * Concatenates a routine mapping the whole collection of outputs through the specified
     * consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param mappingConsumer the bi-consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamRoutineBuilder<IN, AFTER> mapAllMore(
            @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Concatenates a routine mapping this stream outputs through the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param mappingConsumer the bi-consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> mapMore(
            @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Concatenates a consumer handling the outputs completion.
     * <br>
     * The stream outputs will be no further propagated.
     *
     * @param completeAction the action to perform.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, Void> onComplete(@NotNull Action completeAction);

    /**
     * Concatenates a consumer handling invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     *
     * @param errorConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> onError(
            @NotNull Consumer<? super RoutineException> errorConsumer);

    /**
     * Concatenates a consumer handling this stream outputs.
     * <br>
     * The stream outputs will be no further propagated.
     *
     * @param outputConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, Void> onOutput(@NotNull Consumer<? super OUT> outputConsumer);

    /**
     * Concatenates a routine producing the specified output in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param output the output to return.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElse(@Nullable OUT output);

    /**
     * Concatenates a routine producing the specified outputs in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputs the outputs to return.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElse(@Nullable OUT... outputs);

    /**
     * Concatenates a routine producing the specified outputs in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputs the outputs to return.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElse(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Concatenates a routine producing the outputs returned by the specified supplier in case this
     * one produced none.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations completes. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElseGet(long count,
            @NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a routine producing the outputs returned by the specified supplier in case this
     * one produced none.
     * <br>
     * The supplier will be called only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputSupplier the supplier instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElseGet(@NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a routine producing the outputs returned by the specified consumer in case this
     * one produced none.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations completes. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElseGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Concatenates a routine producing the outputs returned by the specified consumer in case this
     * one produced none.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocation completes.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputsConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> orElseGetMore(
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> parallel();

    /**
     * Short for
     * {@code parallel().invocationConfiguration().withMaxInstances(maxInvocations).applied()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified number is 0 or negative.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> parallel(int maxInvocations);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count   the number of groups.
     * @param factory the processing invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * The stream configuration will be ignored.
     *
     * @param count   the number of groups.
     * @param routine the processing routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count   the number of groups.
     * @param builder the builder of processing routine instances.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallel(int count,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param factory     the processing invocation factory.
     * @param <AFTER>     the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * The stream configuration will be ignored.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param routine     the processing routine instance.
     * @param <AFTER>     the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param builder     the builder of processing routine instances.
     * @param <AFTER>     the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamRoutineBuilder<IN, AFTER> parallelBy(
            @NotNull Function<? super OUT, ?> keyFunction,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Concatenates a routine performing the specified action when the previous routine invocation
     * completes.
     * <br>
     * Outputs will be automatically passed on, while the invocation will be aborted if an exception
     * escapes the consumer.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param completeAction the action instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> peekComplete(@NotNull Action completeAction);

    /**
     * Concatenates a routine peeking the stream errors as they are passed on.
     * <br>
     * Outputs will be automatically passed on, while the invocation will be aborted if an exception
     * escapes the consumer.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param errorConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> peekError(
            @NotNull Consumer<? super RoutineException> errorConsumer);

    /**
     * Concatenates a routine peeking the stream data as they are passed on.
     * <br>
     * Outputs will be automatically passed on, while the invocation will be aborted if an exception
     * escapes the consumer.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputConsumer the consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> peekOutput(@NotNull Consumer<? super OUT> outputConsumer);

    /**
     * Concatenates a routine accumulating data through the specified function.
     * <br>
     * The output will be computed as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * where the initial accumulated value will be the the first input.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param accumulateFunction the bi-function instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    StreamRoutineBuilder<IN, OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction);

    /**
     * Concatenates a routine accumulating data through the specified function.
     * <br>
     * The output will be computed as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * where the initial accumulated value will be the one returned by the specified supplier.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateFunction the bi-function instance.
     * @param <AFTER>            the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> reduce(@NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiFunction<? super AFTER, ? super OUT, ? extends AFTER> accumulateFunction);

    /**
     * Returns a new stream retrying the whole flow of data at maximum for the specified number of
     * times.
     *
     * @param count the maximum number of retries.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamRoutineBuilder<IN, OUT> retry(int count);

    /**
     * Returns a new stream retrying the whole flow of data at maximum for the specified number of
     * times.
     * <br>
     * For each retry the specified backoff policy will be applied before re-starting the flow.
     *
     * @param count   the maximum number of retries.
     * @param backoff the backoff policy.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamRoutineBuilder<IN, OUT> retry(int count, @NotNull Backoff backoff);

    /**
     * Returns a new stream retrying the whole flow of data until the specified function does not
     * return null.
     * <br>
     * For each retry the function is called passing the retry count (starting from 1) and the error
     * which caused the failure. If the function returns a non-null value, it will represent the
     * number of milliseconds to wait before a further retry. While, in case the function returns
     * null, the flow of data will be aborted with the passed error as reason.
     * <p>
     * Note that no retry will be attempted in case of an explicit abortion, that is, if the error
     * is an instance of {@link com.github.dm.jrt.core.channel.AbortException}.
     *
     * @param backoffFunction the retry function.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamRoutineBuilder<IN, OUT> retry(
            @NotNull BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction);

    /**
     * Makes the stream sequential, that is, the concatenated routines will be invoked in sequential
     * mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> sequential();

    /**
     * Concatenates a routine skipping the specified number of outputs.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count the number of outputs to skip.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> skip(int count);

    /**
     * Short for {@code streamInvocationConfiguration().withOutputOrder(orderType).applied()}.
     * <br>
     * This method is useful to easily make the stream ordered or not.
     * <p>
     * Note that a sorted channel has a slightly increased cost in memory and computation.
     *
     * @param orderType the order type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> sort(@Nullable OrderType orderType);

    /**
     * Short for
     * {@code async().streamInvocationConfiguration().withRunner(straightRunner).applied()}.
     * <br>
     * This method is useful to set the stream runner so that each input is immediately passed
     * through the whole chain as soon as it is fed to the stream.
     * <p>
     * On the contrary of the default synchronous runner, the set one makes so that each routine
     * in the chain is passed any input as soon as it is produced by the previous one. Such behavior
     * decreases memory demands at the expense of a deeper stack of calls. In fact, the default
     * synchronous runner breaks up routine calls so to perform them in a loop. The main drawback of
     * the latter approach is that all input data are accumulated before actually being processed by
     * the next routine invocation.
     * <p>
     * Note that the runner will be employed with asynchronous and parallel invocation modes, while
     * the synchronous and sequential modes will behave as before.
     *
     * @return this builder.
     * @see #async()
     * @see #parallel()
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> straight();

    /**
     * Gets the invocation configuration builder related to the whole stream.
     * <br>
     * The configuration options will be applied to all the next concatenated routine unless
     * overwritten by specific ones.
     * <p>
     * Note that the configuration builder will be initialized with the current stream
     * configuration.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    @StreamFlow(CONFIG)
    Builder<? extends StreamRoutineBuilder<IN, OUT>> streamInvocationConfiguration();

    /**
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamRoutineBuilder<IN, OUT> sync();

    /**
     * Concatenates a routine generating the specified output.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param output  the output.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> then(@Nullable AFTER output);

    /**
     * Concatenates a routine generating the specified outputs.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputs the outputs.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> then(@Nullable AFTER... outputs);

    /**
     * Concatenates a routine generating the output returned by the specified iterable.
     * <br>
     * The outputs will be generated only when the previous routine invocation completes.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputs the iterable returning the outputs.
     * @param <AFTER> the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * Concatenates a routine generating the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocation completes. The count number must be positive.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @param <AFTER>        the concatenation output type.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> thenGet(long count,
            @NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * Concatenates a routine generating the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called only when the previous routine invocation completes.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputSupplier the supplier instance.
     * @param <AFTER>        the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> thenGet(
            @NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * Concatenates a routine generating the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocation completes. The count number must be positive.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> thenGetMore(long count,
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * Concatenates a routine generating the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocation completes.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     *
     * @param outputsConsumer the consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamRoutineBuilder<IN, AFTER> thenGetMore(
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * Concatenates a consumer handling invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     *
     * @param catchFunction the function instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> catchFunction);

    /**
     * Concatenates a consumer handling invocation exceptions.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The errors will not be automatically further propagated.
     *
     * @param catchConsumer the bi-consumer instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> tryCatchMore(
            @NotNull BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer);

    /**
     * Concatenates an action always performed when outputs complete, even if an error occurred.
     * <br>
     * Both outputs and errors will be automatically passed on.
     *
     * @param finallyAction the action instance.
     * @return this builder.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamRoutineBuilder<IN, OUT> tryFinally(@NotNull Action finallyAction);

    /**
     * Interface defining a stream configuration.
     */
    interface StreamConfiguration {

        /**
         * Gets the combination of stream and current configuration as a channel one.
         *
         * @return the channel configuration.
         */
        @NotNull
        ChannelConfiguration asChannelConfiguration();

        /**
         * Gets the combination of stream and current configuration as an invocation one.
         *
         * @return the invocation configuration.
         */
        @NotNull
        InvocationConfiguration asInvocationConfiguration();

        /**
         * Gets the configuration that will override the stream one only for the next
         * concatenated routine.
         *
         * @return the invocation configuration.
         */
        @NotNull
        InvocationConfiguration getCurrentConfiguration();

        /**
         * Gets the stream invocation mode.
         *
         * @return the invocation mode.
         */
        @NotNull
        InvocationMode getInvocationMode();

        /**
         * Gets the configuration that will be applied to all the concatenated routines.
         *
         * @return the invocation configuration.
         */
        @NotNull
        InvocationConfiguration getStreamConfiguration();
    }
}
