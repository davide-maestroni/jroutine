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

import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.builder.ConfigurableBuilder;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions.
 * <br>
 * Each function in the stream is backed by a routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p>
 * Note that, if at least one reduce function is part of the chain, the results will be propagated
 * only when the previous routine invocations complete.
 * <p>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
public interface StreamChannel<OUT>
        extends OutputChannel<OUT>, ConfigurableBuilder<StreamChannel<OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> afterMax(@NotNull UnitDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> bind(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> immediately();

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<OUT> skipNext(int count);

    /**
     * Transforms this stream by applying the specified function.
     *
     * @param function the transformation function.
     * @param <AFTER>  the concatenation output type.
     * @return the transformed stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> apply(
            @NotNull Function<? super StreamChannel<OUT>, ? extends OutputChannel<AFTER>> function);

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> async();

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay, timeUnit).applyConfiguration()}.
     * <br>
     * This method is useful to easily apply a configuration which will slow down the thread
     * feeding the next routine concatenated to the stream, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner    the configured runner.
     * @param maxInputs the maximum number of buffered inputs before starting to slow down the
     *                  feeding thread.
     * @param maxDelay  the maximum delay to apply to the feeding thread.
     * @param timeUnit  the delay time unit.
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs, long maxDelay,
            @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay).applyConfiguration()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will slow down the thread feeding it, when the number of buffered inputs
     * exceeds the specified limit. Since waiting on the same runner thread is not allowed, it is
     * advisable to employ a runner instance different from the feeding one, so to avoid deadlock
     * exceptions.
     *
     * @param runner    the configured runner.
     * @param maxInputs the maximum number of buffered inputs before starting to slow down the
     *                  feeding thread.
     * @param maxDelay  the maximum delay to apply to the feeding thread.
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable UnitDuration maxDelay);

    /**
     * Concatenates a stream based on the specified accumulating consumer to this one.
     * <br>
     * The output will be computed as follows, where the initial accumulated value will be the
     * the first input:
     * <pre>
     *     <code>
     *
     *         consumer.accept(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> collect(@NotNull BiConsumer<? super OUT, ? super OUT> consumer);

    /**
     * Concatenates a stream accumulating the outputs by adding them to the collections returned by
     * the specified supplier to this one.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier of collections.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER extends Collection<? super OUT>> StreamChannel<AFTER> collect(
            @NotNull Supplier<? extends AFTER> supplier);

    /**
     * Concatenates a stream based on the specified accumulating consumer to this one.
     * <br>
     * The output will be computed as follows, where the initial accumulated value will be the
     * one returned by the specified supplier:
     * <pre>
     *     <code>
     *
     *         consumer.accept(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier of initial accumulation values.
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> collect(@NotNull Supplier<? extends AFTER> supplier,
            @NotNull BiConsumer<? super AFTER, ? super OUT> consumer);

    /**
     * Returns a stream concatenating the specified output to this stream ones.
     * <br>
     * The output will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param output the output to append.
     * @return the new stream.
     */
    @NotNull
    StreamChannel<OUT> concat(@Nullable OUT output);

    /**
     * Returns a stream concatenating the specified outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs to append.
     * @return the new stream.
     */
    @NotNull
    StreamChannel<OUT> concat(@Nullable OUT... outputs);

    /**
     * Returns a stream concatenating the specified outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs to append.
     * @return the new stream.
     */
    @NotNull
    StreamChannel<OUT> concat(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Returns a stream concatenating the specified channel outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that both the specified channel and this stream will be bound as a result of the call.
     *
     * @param channel the output channel.
     * @return the new stream.
     */
    @NotNull
    StreamChannel<OUT> concat(@NotNull OutputChannel<? extends OUT> channel);

    /**
     * Concatenates a stream based on the specified predicate to this one.
     * <br>
     * The output will be filtered according to the result returned by the predicate.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream mapping this stream outputs by applying the specified function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * Gets the invocation configuration builder related only to the next concatenated routine
     * instance. Any further addition to the chain will retain only the stream configuration.
     * <br>
     * Only the options set in this configuration (that is, the ones with a value different from the
     * default) will override the stream ones.
     * <p>
     * Note that the configuration builder will be initialized with the current configuration for
     * the next routine.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends StreamChannel<OUT>> invocationConfiguration();

    /**
     * Concatenates a stream limiting the maximum number of outputs to the specified count.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count the maximum number of outputs.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    StreamChannel<OUT> limit(int count);

    /**
     * Concatenates a stream based on the specified mapping consumer to this one.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified mapping function to this one.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(@NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * Concatenates a stream based on the specified mapping invocation factory to this one.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Concatenates a stream based on the specified instance to this one.
     * <br>
     * The set configuration will be ignored.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Concatenates a stream based on the specified consumer to this one.
     * <br>
     * All the outputs are collected and then passed to the consumer.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> mapAll(
            @NotNull BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified function to this one.
     * <br>
     * All the outputs are collected and then the function will be applied to them.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> mapAll(
            @NotNull Function<? super List<OUT>, ? extends AFTER> function);

    /**
     * Concatenates a consumer handling an invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> onError(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a stream based on the specified consumer to this one.
     * <br>
     * The stream outputs will be no further propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<Void> onOutput(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream producing the specified output in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param output the output to return.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> orElse(@Nullable OUT output);

    /**
     * Concatenates a stream producing the specified outputs in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs to return.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> orElse(@Nullable OUT... outputs);

    /**
     * Concatenates a stream producing the specified outputs in case this one produced none.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs to return.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> orElse(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Concatenates a stream calling the specified consumer to this one.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    StreamChannel<OUT> orElseGet(long count,
            @NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream calling the specified consumer to this one.
     * <br>
     * The consumer will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> orElseGet(@NotNull Consumer<? super ResultChannel<OUT>> consumer);

    /**
     * Concatenates a stream calling the specified supplier to this one.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    StreamChannel<OUT> orElseGet(long count, @NotNull Supplier<? extends OUT> supplier);

    /**
     * Concatenates a stream calling the specified supplier to this one.
     * <br>
     * The supplier will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> orElseGet(@NotNull Supplier<? extends OUT> supplier);

    /**
     * Short for
     * {@code streamInvocationConfiguration().withOutputOrder(orderType).applyConfiguration()}.
     * <br>
     * This method is useful to easily make the stream ordered or not.
     * <p>
     * Note that an ordered stream has a slightly increased cost in memory and computation.
     *
     * @param orderType the order type.
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> ordered(@Nullable OrderType orderType);

    /**
     * Short for {@code parallel().invocationConfiguration().withMaxInstances(maxInvocations)
     * .applyConfiguration()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> parallel(int maxInvocations);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> parallel();

    /**
     * Concatenates a stream based on the specified peeking consumer.
     * <br>
     * Outputs will be automatically passed on.
     * <p>
     * Note that the invocation will be aborted if an exception escapes the consumer.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> peek(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream based on the specified accumulating function to this one.
     * <br>
     * The output will be computed as follows, where the initial accumulated value will be the
     * the first input:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the bi-function instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Concatenates a stream based on the specified accumulating function to this one.
     * <br>
     * The output will be computed as follows, where the initial accumulated value will be the
     * one returned by the specified supplier:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier of initial accumulation values.
     * @param function the bi-function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> reduce(@NotNull Supplier<? extends AFTER> supplier,
            @NotNull BiFunction<? super AFTER, ? super OUT, ? extends AFTER> function);

    /**
     * Returns a new stream repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting multiple binding.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @return the repeating stream.
     */
    @NotNull
    StreamChannel<OUT> repeat();

    /**
     * Short for {@code streamInvocationConfiguration().withRunner(runner).applyConfiguration()
     * .asyncMap(IdentityInvocation.&lt;OUT&gt;factoryOf())}.
     * <br>
     * This method is useful to easily make the stream run on the specified runner.
     * <p>
     * Note that it is not necessary to explicitly concatenate a routine to have a stream delivering
     * the output data through the specified runner.
     *
     * @param runner the runner instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> runOn(@Nullable Runner runner);

    /**
     * Short for {@code runOn(null)}.
     *
     * @return the concatenated stream.
     * @see #runOn(Runner)
     */
    @NotNull
    StreamChannel<OUT> runOnShared();

    /**
     * Makes the stream serial, that is, the concatenated routines will be invoked in serial mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> serial();

    /**
     * Concatenates a stream skipping the specified number of outputs.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count the number of outputs to skip.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    StreamChannel<OUT> skip(int count);

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
    Builder<? extends StreamChannel<OUT>> streamInvocationConfiguration();

    /**
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> sync();

    /**
     * Concatenates a stream generating the specified output.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param output  the output.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable AFTER output);

    /**
     * Concatenates a stream generating the specified outputs.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable AFTER... outputs);

    /**
     * Concatenates a stream generating the output returned by the specified iterable.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * Concatenates a stream based on the specified consumer to this one.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> thenGet(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified consumer to this one.
     * <br>
     * The consumer will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> thenGet(@NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified supplier to this one.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> thenGet(long count, @NotNull Supplier<? extends AFTER> supplier);

    /**
     * Concatenates a stream based on the specified supplier to this one.
     * <br>
     * The supplier will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> thenGet(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * Returns a new stream making this one selectable.
     * <br>
     * Each output will be passed along unchanged.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param index the stream index.
     * @return the selectable stream.
     */
    @NotNull
    StreamChannel<? extends Selectable<OUT>> toSelectable(int index);

    /**
     * Concatenates a consumer handling the invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * Concatenates a function handling an invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * Concatenates a runnable always called when outputs complete, even if an error occurred.
     * <br>
     * Both outputs and errors will be automatically passed on.
     *
     * @param runnable the runnable instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryFinally(@NotNull Runnable runnable);
}
