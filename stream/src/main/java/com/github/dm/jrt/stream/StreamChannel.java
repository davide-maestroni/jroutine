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

import com.github.dm.jrt.builder.ConfigurableBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.channel.Selectable;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a stream output channel, that is, a channel concatenating map and reduce
 * functions.<br/>
 * Each function in the stream is backed by a routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p/>
 * Note that, if at least one reduce function is part of the chain, the results will be propagated
 * only when the previous routine invocations complete.
 * <p/>
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
    StreamChannel<OUT> afterMax(@NotNull TimeDuration timeout);

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
    StreamChannel<OUT> bindTo(@NotNull OutputConsumer<? super OUT> consumer);

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
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> skip(int count);

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> async();

    /**
     * Short for {@code withInvocations().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay, timeUnit).getConfigured()}.<br/>
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
     * Short for {@code withInvocations().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay).getConfigured()}.<br/>
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
            @Nullable TimeDuration maxDelay);

    /**
     * Concatenates a stream based on the specified consumer to this one.<br/>
     * All the outputs are collected and then passed to the consumer.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> collect(
            @NotNull BiConsumer<? super List<OUT>, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified accumulating function to this one.<br/>
     * The output will be computed as follows:
     * <pre>
     *     <code>
     *
     *         acc = function.apply(acc, input);
     *     </code>
     * </pre>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the bi-function instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> collect(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Concatenates a stream based on the specified function to this one.<br/>
     * All the outputs are collected and then the function will be applied to them.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> collect(
            @NotNull Function<? super List<OUT>, ? extends AFTER> function);

    /**
     * Concatenates a stream based on the specified consumer to this one.<br/>
     * The stream outputs will be no further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<Void> consume(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream based on the specified predicate to this one.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream mapping this stream outputs by applying the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
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
     * Concatenates a stream based on the specified mapping consumer to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
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
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
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
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
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
     * Concatenates a stream based on the specified instance to this one.<br/>
     * The set configuration will be ignored.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Short for {@code withInvocations().withMaxInstances(maxInvocations).getConfigured()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> maxParallelInvocations(int maxInvocations);

    /**
     * Short for {@code withStreamInvocations().withOutputOrder(orderType).getConfigured()}.<br/>
     * This method is useful to easily make the stream ordered or not.<br/>
     * Note that an ordered stream has a slightly increased cost in memory and computation.
     *
     * @return the configured stream.
     */
    @NotNull
    StreamChannel<OUT> ordered(@Nullable OrderType orderType);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> parallel();

    /**
     * Returns a new stream repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting multiple binding.<br/>
     * Note that this stream will be bound as a result of the call.
     *
     * @return the repeating stream.
     */
    @NotNull
    StreamChannel<OUT> repeat();

    /**
     * Short for {@code withStreamInvocations().withRunner(runner).getConfigured().asyncMap(
     * Function.<OUT>identity())}.<br/>
     * This method is useful to easily make the stream run on the specified runner.<br/>
     * Note that it is not necessary to explicitly concatenate a routine to have a stream delivering
     * the output data with the specified runner.
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
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return this stream.
     */
    @NotNull
    StreamChannel<OUT> sync();

    /**
     * Concatenates a stream generating the specified output.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param output  the output.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable AFTER output);

    /**
     * Concatenates a stream generating the specified outputs.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable AFTER... outputs);

    /**
     * Concatenates a stream generating the output returned by the specified iterable.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * Concatenates a stream based on the specified consumer to this one.<br/>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified consumer to this one.<br/>
     * The consumer will be called only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream based on the specified supplier to this one.<br/>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(long count, @NotNull Supplier<? extends AFTER> supplier);

    /**
     * Concatenates a stream based on the specified supplier to this one.<br/>
     * The supplier will be called only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> then(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * Returns a new stream making the this one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that this stream will be bound as a result of the call.
     *
     * @param index the stream index.
     * @return the selectable stream.
     */
    @NotNull
    StreamChannel<? extends Selectable<OUT>> toSelectable(int index);

    /**
     * Concatenates a consumer handling the invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this stream will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * Concatenates a consumer handling a invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a function handling a invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this stream will be bound as a result of the call.
     *
     * @param function the function instance.
     * @return the concatenated stream.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> function);

    /**
     * Gets the invocation configuration builder related only to the next concatenated routine
     * instance. Any further addition to the chain will retain only the stream configuration.<br/>
     * Only the options set in this configuration (that is, the ones with a value different from the
     * default) will override the stream one.
     * <p/>
     * Note that the configuration builder will be initialized with the current configuration for
     * the next routine.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends StreamChannel<OUT>> withInvocations();

    /**
     * Gets the invocation configuration builder related to the whole stream.<br/>
     * The configuration options will be applied to all the next concatenated routine unless
     * overwritten by specific ones.
     * <p/>
     * Note that the configuration builder will be initialized with the current stream
     * configuration.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    Builder<? extends StreamChannel<OUT>> withStreamInvocations();
}
