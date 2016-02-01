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
import com.github.dm.jrt.core.Channels.Selectable;
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
 * Each function in the channel is backed by a routine instance, that can have its own specific
 * configuration and invocation mode.
 * <p/>
 * Note that, if at least one reduce function is part of the concatenation, the results will be
 * propagated only when the previous routine invocations complete.
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
    StreamChannel<OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamChannel<OUT> skip(int count);

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return this channel.
     */
    @NotNull
    StreamChannel<OUT> async();

    /**
     * Short for {@code withInvocations().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay, timeUnit).set()}.<br/>
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
     * @return the configured stream channel.
     */
    @NotNull
    StreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs, long maxDelay,
            @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code withInvocations().withRunner(runner).withInputLimit(maxInputs)
     * .withInputMaxDelay(maxDelay).set()}.<br/>
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
     * @return the configured stream channel.
     */
    @NotNull
    StreamChannel<OUT> backPressureOn(@Nullable Runner runner, int maxInputs,
            @Nullable TimeDuration maxDelay);

    /**
     * Binds the inputs to this stream channel.<br/>
     * The method will have no effect unless the stream is "lazy" and inputs have not already been
     * bound.
     *
     * @return this channel.
     */
    @NotNull
    StreamChannel<OUT> bind();

    /**
     * Concatenates a stream channel based on the specified collecting consumer to this one.<br/>
     * The outputs will be collected by applying the function, only when the previous routine
     * invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> collect(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Concatenates a stream channel based on the specified collecting function to this one.<br/>
     * The outputs will be collected by applying the function, only when the previous routine
     * invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> collect(
            @NotNull Function<? super List<? extends OUT>, ? extends AFTER> function);

    /**
     * Concatenates a stream channel based on the specified predicate to this one.<br/>
     * The output will be filtered according to the result returned by the predicate.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param predicate the predicate instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<OUT> filter(@NotNull Predicate<? super OUT> predicate);

    /**
     * Concatenates a stream mapping this stream outputs by applying the specified function.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the lifting output type.
     * @return the lifted stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends OutputChannel<? extends AFTER>> function);

    /**
     * Concatenates a stream channel based on the specified consumer to this one.<br/>
     * The channel outputs will be no further propagated.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<Void> forEach(@NotNull Consumer<? super OUT> consumer);

    /**
     * Concatenates a stream channel generating the specified output.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param output  the output.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(@Nullable AFTER output);

    /**
     * Concatenates a stream channel generating the specified outputs.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param outputs the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(@Nullable AFTER... outputs);

    /**
     * Concatenates a stream channel generating the output returned by the specified iterable.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * Concatenates a stream channel based on the specified consumer to this one.<br/>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(long count,
            @NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream channel based on the specified consumer to this one.<br/>
     * The consumer will be called only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(@NotNull Consumer<? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream channel based on the specified supplier to this one.<br/>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param count    the number of generated outputs.
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative..
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(long count, @NotNull Supplier<? extends AFTER> supplier);

    /**
     * Concatenates a stream channel based on the specified supplier to this one.<br/>
     * The supplier will be called only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param supplier the supplier instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> generate(@NotNull Supplier<? extends AFTER> supplier);

    /**
     * Concatenates a stream channel based on the specified mapping consumer to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Concatenates a stream channel based on the specified mapping function to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param function the function instance.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(@NotNull Function<? super OUT, ? extends AFTER> function);

    /**
     * Concatenates a stream channel based on the specified mapping invocation factory to this one.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Concatenates a stream channel based on the specified instance to this one.<br/>
     * The set configuration will be ignored.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER> StreamChannel<AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Short for {@code withInvocations().withMaxInstances(maxInvocations).set()}.<br/>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @return the configured stream channel.
     */
    @NotNull
    StreamChannel<OUT> maxParallelInvocations(int maxInvocations);

    /**
     * Short for {@code withStreamInvocations().withOutputOrder(orderType).set()}.<br/>
     * This method is useful to easily make the stream ordered or not.<br/>
     * Note that an ordered channel has a slightly increased cost in memory and computation.
     *
     * @return the configured stream channel.
     */
    @NotNull
    StreamChannel<OUT> ordered(@Nullable OrderType orderType);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return this channel.
     */
    @NotNull
    StreamChannel<OUT> parallel();

    /**
     * Concatenates a stream channel generating the specified range of data.<br/>
     * The generated data will start from the specified first one up to and including the specified
     * last one, by computing each next element through the specified function.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param start     the first element of the range.
     * @param end       the last element of the range.
     * @param increment the function incrementing the current element.
     * @param <AFTER>   the concatenation output type.
     * @return the concatenated stream channel.
     */
    @NotNull
    <AFTER extends Comparable<AFTER>> StreamChannel<AFTER> range(@NotNull AFTER start,
            @NotNull AFTER end, @NotNull Function<AFTER, AFTER> increment);

    /**
     * Concatenates a stream channel generating the specified range of data.<br/>
     * The channel will generate a range of numbers up to and including the {@code end} element, by
     * applying a default increment of {@code +1} or {@code -1} depending on the comparison between
     * the first and the last element. That is, if the first element is less than the last, the
     * increment will be {@code +1}. On the contrary, if the former is greater than the latter, the
     * increment will be {@code -1}.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param start the first element of the range.
     * @param end   the last element of the range.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<Number> range(@NotNull Number start, @NotNull Number end);

    /**
     * Concatenates a stream channel generating the specified range of data.<br/>
     * The channel will generate a range of numbers by applying the specified increment up to and
     * including the {@code end} element.<br/>
     * The outputs will be generated only when the previous routine invocations complete.
     * <p/>
     * Note that the created routine will be initialized with the current configuration.<br/>
     * Note also that this channel will be bound as a result of the call.
     *
     * @param start     the first element of the range.
     * @param end       the last element of the range.
     * @param increment the increment to apply to the current element.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<Number> range(@NotNull Number start, @NotNull Number end,
            @NotNull Number increment);

    /**
     * Concatenates a stream channel based on the specified accumulating function to this one.<br/>
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
     * Note also that this channel will be bound as a result of the call.
     *
     * @param function the bi-function instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> function);

    /**
     * Returns a new channel repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting multiple binding.<br/>
     * Note that this channel will be bound as a result of the call.
     *
     * @return the repeating channel.
     */
    @NotNull
    StreamChannel<OUT> repeat();

    /**
     * Short for {@code withStreamInvocations().withRunner(runner).set().asyncMap(Functions
     * .<OUT>identity())}.<br/>
     * This method is useful to easily make the stream run on the specified runner.<br/>
     * Note that it is not necessary to explicitly concatenate a routine to have a channel
     * delivering the output data with the specified runner.
     *
     * @param runner the runner instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<OUT> runOn(@Nullable Runner runner);

    /**
     * Short for {@code runOn(null)}.
     *
     * @return the concatenated stream channel.
     * @see #runOn(Runner)
     */
    @NotNull
    StreamChannel<OUT> runOnShared();

    /**
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return this channel.
     */
    @NotNull
    StreamChannel<OUT> sync();

    /**
     * Returns a new channel making the this one selectable.<br/>
     * Each output will be passed along unchanged.<br/>
     * Note that this channel will be bound as a result of the call.
     *
     * @param index the channel index.
     * @return the selectable output channel.
     */
    @NotNull
    StreamChannel<? extends Selectable<OUT>> toSelectable(int index);

    /**
     * Concatenates a consumer handling the invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this channel will be bound as a result of the call.
     *
     * @param consumer the bi-consumer instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(
            @NotNull BiConsumer<? super RoutineException, ? super InputChannel<OUT>> consumer);

    /**
     * Concatenates a consumer handling a invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this channel will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the concatenated stream channel.
     */
    @NotNull
    StreamChannel<OUT> tryCatch(@NotNull Consumer<? super RoutineException> consumer);

    /**
     * Concatenates a function handling a invocation exceptions.<br/>
     * The errors will not be automatically further propagated.
     * <p/>
     * Note that this channel will be bound as a result of the call.
     *
     * @param function the function instance.
     * @return the concatenated stream channel.
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
