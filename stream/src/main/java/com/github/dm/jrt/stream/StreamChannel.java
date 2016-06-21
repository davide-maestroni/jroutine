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
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.annotation.StreamFlow;
import com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.CACHE;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.COLLECT;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.CONFIG;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.MAP;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.REDUCE;
import static com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType.START;

/**
 * Interface defining a stream channel, that is, a channel concatenating map and reduce functions.
 * <br>
 * Each function in the stream is backed by a routine instance, which may have its own specific
 * configuration and invocation mode.
 * <p>
 * To better document the effect of each method on the underlying stream, a {@link StreamFlow}
 * annotation indicates for each one the type of transformation applied.
 * <p>
 * Note that each {@code START} method may throw a {@link com.github.dm.jrt.stream.StreamException}
 * if an unexpected error occurs while initiating the flow of data.
 * <br>
 * Note also that, if at least one reduce function is part of the chain, the results will be
 * propagated only when the previous routine invocations complete.
 * <p>
 * Created by davide-maestroni on 12/23/2015.
 *
 * @param <OUT> the output data type.
 */
public interface StreamChannel<IN, OUT>
        extends Channel<IN, OUT>, ConfigurableBuilder<StreamChannel<IN, OUT>> {

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean abort();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean abort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> after(@NotNull UnitDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    List<OUT> all();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    <CHANNEL extends Channel<? super OUT, ?>> CHANNEL bind(@NotNull CHANNEL channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bind(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> close();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    Iterator<OUT> eventualIterator();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> eventuallyBreak();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> eventuallyFail();

    /**
     * {@inheritDoc}
     */
    @Nullable
    @StreamFlow(START)
    RoutineException getError();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean hasCompleted();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean hasNext();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    OUT next();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    int inputCount();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean isBound();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean isEmpty();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    boolean isOpen();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @StreamFlow(START)
    List<OUT> next(int count);

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    OUT nextOrElse(OUT output);

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    int outputCount();

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> pass(@Nullable Channel<?, ? extends IN> channel);

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    int size();

    /**
     * {@inheritDoc}.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> skipNext(int count);

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> sortedByCall();

    /**
     * {@inheritDoc}.
     */
    @NotNull
    StreamChannel<IN, OUT> sortedByDelay();

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    void throwError();

    /**
     * Returns a stream appending the specified output to this stream ones.
     * <br>
     * The output will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param output the output to append.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> append(@Nullable OUT output);

    /**
     * Returns a stream concatenating the specified outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs to append.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> append(@Nullable OUT... outputs);

    /**
     * Returns a stream appending the specified outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs to append.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> append(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Returns a stream appending the specified channel outputs to this stream ones.
     * <br>
     * The outputs will be appended to the ones produced by this stream.
     * <p>
     * Note that both the specified channel and this stream will be bound as a result of the call.
     *
     * @param channel the output channel.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> append(@NotNull Channel<?, ? extends OUT> channel);

    /**
     * Concatenates a stream appending the outputs returned by the specified supplier.
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
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> appendGet(long count, @NotNull Supplier<? extends OUT> supplier);

    /**
     * Concatenates a stream appending the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param supplier the supplier instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> appendGet(@NotNull Supplier<? extends OUT> supplier);

    /**
     * Concatenates a stream appending the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
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
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> appendGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> consumer);

    /**
     * Concatenates a stream appending the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param consumer the consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> appendGetMore(@NotNull Consumer<? super Channel<OUT, ?>> consumer);

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return the new stream instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> async();

    /**
     * Short for {@code async().streamInvocationConfiguration().withRunner(runner).apply()}.
     * <br>
     * This method is useful to easily set the stream runner.
     * <p>
     * Note that output data will not be automatically delivered through the runner.
     *
     * @param runner the runner instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> async(@Nullable Runner runner);

    /**
     * Short for {@code async(runner).map(IdentityInvocation.&lt;OUT&gt;factoryOf())}.
     * <br>
     * This method is useful to easily make the stream run on the specified runner.
     * <p>
     * Note that it is not necessary to explicitly concatenate a routine to have a stream delivering
     * the output data through the specified runner.
     *
     * @param runner the runner instance.
     * @return the new stream instance.
     * @see #async(Runner)
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> asyncMap(@Nullable Runner runner);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(backoff).apply()}.
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
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> backoffOn(@Nullable Runner runner, int limit, @NotNull Backoff backoff);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay, timeUnit).apply()}.
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
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified limit or the specified delay are
     *                                            negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> backoffOn(@Nullable Runner runner, int limit, long delay,
            @NotNull TimeUnit timeUnit);

    /**
     * Short for {@code invocationConfiguration().withRunner(runner).withInputLimit(maxInputs)
     * .withInputBackoff(delay).apply()}.
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
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified limit is negative.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> backoffOn(@Nullable Runner runner, int limit,
            @Nullable UnitDuration delay);

    /**
     * Initiates the flow of this stream.
     *
     * @return this stream instance.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bind();

    /**
     * Initiates the flow of this stream after the specified delay.
     *
     * @param delay the delay.
     * @return this stream instance.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bindAfter(@NotNull UnitDuration delay);

    /**
     * Initiates the flow of this stream after the specified delay.
     *
     * @param consumer the consumer instance.
     * @param delay    the delay.
     * @return this stream instance.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bindAfter(@NotNull UnitDuration delay,
            @NotNull OutputConsumer<OUT> consumer);

    /**
     * Initiates the flow of this stream after the specified delay.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @return this stream instance.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bindAfter(long delay, @NotNull TimeUnit timeUnit);

    /**
     * Initiates the flow of this stream after the specified delay.
     *
     * @param consumer the consumer instance.
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @return this stream instance.
     */
    @NotNull
    @StreamFlow(START)
    StreamChannel<IN, OUT> bindAfter(long delay, @NotNull TimeUnit timeUnit,
            @NotNull OutputConsumer<OUT> consumer);

    /**
     * Concatenates a stream accumulating data through the specified consumer.
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
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param accumulateConsumer the bi-consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamChannel<IN, OUT> collect(
            @NotNull BiConsumer<? super OUT, ? super OUT> accumulateConsumer);

    /**
     * Concatenates a stream accumulating data through the specified consumer.
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
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateConsumer the bi-consumer instance.
     * @param <AFTER>            the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamChannel<IN, AFTER> collect(@NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiConsumer<? super AFTER, ? super OUT> accumulateConsumer);

    /**
     * Concatenates a stream accumulating the outputs by adding them to the collections returned by
     * the specified supplier.
     * <br>
     * The accumulated value will be passed as result only when the outputs complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param collectionSupplier the supplier of collections.
     * @param <AFTER>            the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER extends Collection<? super OUT>> StreamChannel<IN, AFTER> collectInto(
            @NotNull Supplier<? extends AFTER> collectionSupplier);

    /**
     * Concatenates a stream filtering data based on the values returned by the specified predicate.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param filterPredicate the predicate instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> filter(@NotNull Predicate<? super OUT> filterPredicate);

    /**
     * Transforms this stream by applying the specified function.
     * <br>
     * This method provides a convenient way to apply a set of configurations and concatenations
     * without breaking the fluent chain.
     *
     * @param transformFunction the transformation function.
     * @param <BEFORE>          the concatenation input type.
     * @param <AFTER>           the concatenation output type.
     * @return the transformed stream.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <BEFORE, AFTER> StreamChannel<BEFORE, AFTER> flatLift(
            @NotNull Function<? super StreamChannel<IN, OUT>, ? extends StreamChannel<BEFORE,
                    AFTER>> transformFunction);

    /**
     * Concatenates a stream mapping this stream outputs by applying the specified function to each
     * one of them.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

    /**
     * Short for
     * {@code async().streamInvocationConfiguration().withRunner(immediateRunner).apply()}.
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
     * @return the new stream instance.
     * @see #async()
     * @see #parallel()
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> immediate();

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
    @StreamFlow(CONFIG)
    Builder<? extends StreamChannel<IN, OUT>> invocationConfiguration();

    /**
     * Makes the stream invoke concatenated routines with the specified mode.
     *
     * @param invocationMode the invocation mode.
     * @return the new stream instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> invocationMode(@NotNull InvocationMode invocationMode);

    /**
     * {@inheritDoc}
     */
    @StreamFlow(START)
    Iterator<OUT> iterator();

    /**
     * Transforms the stream by modifying the flow building function.
     * <br>
     * The returned function will be employed when the flow of input data is initiated (see
     * {@link TransformationType#START}).
     *
     * @param transformFunction the function modifying the flow one.
     * @param <AFTER>           the concatenation output type.
     * @return the new stream instance.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> lift(@NotNull Function<? extends Function<? super
            Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
            Channel<?, IN>, ? extends Channel<?, AFTER>>> transformFunction);

    /**
     * Transforms the stream by modifying the flow building function.
     * <br>
     * The current configuration of the stream will be passed as the first parameter.
     * <br>
     * The returned function will be employed when the flow of input data is initiated (see
     * {@link TransformationType#START}).
     *
     * @param transformFunction the bi-function modifying the flow one.
     * @param <AFTER>           the concatenation output type.
     * @return the new stream instance.
     * @throws com.github.dm.jrt.stream.StreamException if an unexpected error occurs.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> liftConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, AFTER>>> transformFunction);

    /**
     * Concatenates a stream limiting the maximum number of outputs to the specified count.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count the maximum number of outputs.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> limit(int count);

    /**
     * Concatenates a stream mapping this stream outputs by applying the specified function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull Function<? super OUT, ? extends AFTER> mappingFunction);

    /**
     * Concatenates a stream mapping this stream outputs through the specified invocation factory.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param factory the invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull InvocationFactory<? super OUT, ? extends AFTER> factory);

    /**
     * Concatenates a stream mapping this stream outputs through the specified routine.
     * <p>
     * Note that the stream configuration will be ignored.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param routine the routine instance.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Concatenates a stream mapping this stream outputs through the specified routine builder.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param builder the routine builder instance.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> map(
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Concatenates a stream mapping the whole collection of outputs by applying the specified
     * function.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param mappingFunction the function instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamChannel<IN, AFTER> mapAll(
            @NotNull Function<? super List<OUT>, ? extends AFTER> mappingFunction);

    /**
     * Concatenates a stream mapping the whole collection of outputs through the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param mappingConsumer the bi-consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    <AFTER> StreamChannel<IN, AFTER> mapAllMore(
            @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Concatenates a stream mapping this stream outputs through the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param mappingConsumer the bi-consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> mapMore(
            @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Concatenates a consumer handling the outputs completion.
     * <br>
     * The stream outputs will be no further propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, Void> onComplete(@NotNull Runnable action);

    /**
     * Concatenates a consumer handling invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param errorConsumer the consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> onError(@NotNull Consumer<? super RoutineException> errorConsumer);

    /**
     * Concatenates a consumer handling this stream outputs.
     * <br>
     * The stream outputs will be no further propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputConsumer the consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, Void> onOutput(@NotNull Consumer<? super OUT> outputConsumer);

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
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElse(@Nullable OUT output);

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
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElse(@Nullable OUT... outputs);

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
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElse(@Nullable Iterable<? extends OUT> outputs);

    /**
     * Concatenates a stream producing the outputs returned by the specified supplier in case this
     * one produced none.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElseGet(long count, @NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a stream producing the outputs returned by the specified supplier in case this
     * one produced none.
     * <br>
     * The supplier will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputSupplier the supplier instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElseGet(@NotNull Supplier<? extends OUT> outputSupplier);

    /**
     * Concatenates a stream producing the outputs returned by the specified consumer in case this
     * one produced none.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElseGetMore(long count,
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Concatenates a stream producing the outputs returned by the specified consumer in case this
     * one produced none.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocations complete.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputsConsumer the consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> orElseGetMore(
            @NotNull Consumer<? super Channel<OUT, ?>> outputsConsumer);

    /**
     * Short for {@code streamInvocationConfiguration().withOutputOrder(orderType).apply()}.
     * <br>
     * This method is useful to easily make the stream ordered or not.
     * <p>
     * Note that an ordered stream has a slightly increased cost in memory and computation.
     *
     * @param orderType the order type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> order(@Nullable OrderType orderType);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return the new stream instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> parallel();

    /**
     * Short for
     * {@code parallel().invocationConfiguration().withMaxInstances(maxInvocations).apply()}.
     * <br>
     * This method is useful to easily apply a configuration to the next routine concatenated to the
     * stream, which will limit the maximum number of concurrent invocations to the specified value.
     *
     * @param maxInvocations the maximum number of concurrent invocations.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified number is 0 or negative.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> parallel(int maxInvocations);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different channel instance.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available channels.
     * <p>
     * Note that the created channels will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param count          the number of groups.
     * @param streamFunction the function creating the processing stream channels.
     * @param <AFTER>        the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallel(int count,
            @NotNull Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param count   the number of groups.
     * @param factory the processing invocation factory.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallel(int count,
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
    <AFTER> StreamChannel<IN, AFTER> parallel(int count,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the load of the available
     * invocations.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param count   the number of groups.
     * @param builder the builder of processing routine instances.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallel(int count,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different channel instance.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created channels will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param keyFunction    the function assigning a key to each output.
     * @param streamFunction the function creating the processing stream channels.
     * @param <AFTER>        the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallelBy(@NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Function<? super StreamChannel<OUT, OUT>, ? extends StreamChannel<?
                    super OUT, ? extends AFTER>> streamFunction);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param factory     the processing invocation factory.
     * @param <AFTER>     the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallelBy(@NotNull Function<? super OUT, ?> keyFunction,
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
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallelBy(@NotNull Function<? super OUT, ?> keyFunction,
            @NotNull Routine<? super OUT, ? extends AFTER> routine);

    /**
     * Splits the outputs produced by this stream, so that each group will be processed by a
     * different routine invocation.
     * <br>
     * Each output will be assigned to a specific group based on the key returned by the specified
     * function.
     * <p>
     * Note that the created routine will employ the same configuration and invocation mode as this
     * stream.
     *
     * @param keyFunction the function assigning a key to each output.
     * @param builder     the builder of processing routine instances.
     * @param <AFTER>     the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    <AFTER> StreamChannel<IN, AFTER> parallelBy(@NotNull Function<? super OUT, ?> keyFunction,
            @NotNull RoutineBuilder<? super OUT, ? extends AFTER> builder);

    /**
     * Concatenates a stream based on the specified peeking consumer.
     * <br>
     * Outputs will be automatically passed on.
     * <p>
     * Note that the invocation will be aborted if an exception escapes the consumer.
     *
     * @param peekConsumer the consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> peek(@NotNull Consumer<? super OUT> peekConsumer);

    /**
     * Concatenates a stream calling the specified runnable when the previous routine invocations
     * complete.
     * <br>
     * Outputs will be automatically passed on.
     * <p>
     * Note that the invocation will be aborted if an exception escapes the consumer.
     *
     * @param peekAction the runnable instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> peekComplete(@NotNull Runnable peekAction);

    /**
     * Concatenates a stream accumulating data through the specified function.
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
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param accumulateFunction the bi-function instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    StreamChannel<IN, OUT> reduce(
            @NotNull BiFunction<? super OUT, ? super OUT, ? extends OUT> accumulateFunction);

    /**
     * Concatenates a stream accumulating data through the specified function.
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
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param seedSupplier       the supplier of initial accumulation values.
     * @param accumulateFunction the bi-function instance.
     * @param <AFTER>            the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> reduce(@NotNull Supplier<? extends AFTER> seedSupplier,
            @NotNull BiFunction<? super AFTER, ? super OUT, ? extends AFTER> accumulateFunction);

    /**
     * Returns a new stream repeating the output data to any newly bound channel or consumer, thus
     * effectively supporting multiple binding.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(CACHE)
    StreamChannel<IN, OUT> replay();

    /**
     * Returns a new stream retrying the whole flow of data at maximum for the specified number of
     * times.
     *
     * @param count the maximum number of retries.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamChannel<IN, OUT> retry(int count);

    /**
     * Returns a new stream retrying the whole flow of data at maximum for the specified number of
     * times.
     * <br>
     * For each retry the specified backoff policy will be applied before re-starting the flow.
     *
     * @param count   the maximum number of retries.
     * @param backoff the backoff policy.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamChannel<IN, OUT> retry(int count, @NotNull Backoff backoff);

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
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(COLLECT)
    StreamChannel<IN, OUT> retry(
            @NotNull BiFunction<? super Integer, ? super RoutineException, ? extends Long>
                    backoffFunction);

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
    @StreamFlow(MAP)
    StreamChannel<IN, ? extends Selectable<OUT>> selectable(int index);

    /**
     * Makes the stream sequential, that is, the concatenated routines will be invoked in sequential
     * mode.
     *
     * @return the new stream instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> sequential();

    /**
     * Concatenates a stream skipping the specified number of outputs.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count the number of outputs to skip.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> skip(int count);

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
    Builder<? extends StreamChannel<IN, OUT>> streamInvocationConfiguration();

    /**
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return the new stream instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    @StreamFlow(CONFIG)
    StreamChannel<IN, OUT> sync();

    /**
     * Concatenates a stream generating the specified output.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param output  the output.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> then(@Nullable AFTER output);

    /**
     * Concatenates a stream generating the specified outputs.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> then(@Nullable AFTER... outputs);

    /**
     * Concatenates a stream generating the output returned by the specified iterable.
     * <br>
     * The outputs will be generated only when the previous routine invocations complete.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputs the iterable returning the outputs.
     * @param <AFTER> the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> then(@Nullable Iterable<? extends AFTER> outputs);

    /**
     * Concatenates a stream generating the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count          the number of generated outputs.
     * @param outputSupplier the supplier instance.
     * @param <AFTER>        the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> thenGet(long count,
            @NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * Concatenates a stream generating the outputs returned by the specified supplier.
     * <br>
     * The supplier will be called only when the previous routine invocations complete.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputSupplier the supplier instance.
     * @param <AFTER>        the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> thenGet(@NotNull Supplier<? extends AFTER> outputSupplier);

    /**
     * Concatenates a stream generating the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called {@code count} number of times only when the previous routine
     * invocations complete. The count number must be positive.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param count           the number of generated outputs.
     * @param outputsConsumer the consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> thenGetMore(long count,
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * Concatenates a stream generating the outputs returned by the specified consumer.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The consumer will be called only when the previous routine invocations complete.
     * <br>
     * Previous results will not be propagated.
     * <p>
     * Note that the created routine will be initialized with the current configuration.
     * <br>
     * Note also that this stream will be bound as a result of the call.
     *
     * @param outputsConsumer the consumer instance.
     * @param <AFTER>         the concatenation output type.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(REDUCE)
    <AFTER> StreamChannel<IN, AFTER> thenGetMore(
            @NotNull Consumer<? super Channel<AFTER, ?>> outputsConsumer);

    /**
     * Concatenates a function handling invocation exceptions.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param catchFunction the function instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> tryCatch(
            @NotNull Function<? super RoutineException, ? extends OUT> catchFunction);

    /**
     * Concatenates a consumer handling invocation exceptions.
     * <br>
     * The result channel of the backing routine will be passed to the consumer, so that multiple
     * or no results may be generated.
     * <br>
     * The errors will not be automatically further propagated.
     * <p>
     * Note that this stream will be bound as a result of the call.
     *
     * @param catchConsumer the bi-consumer instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> tryCatchMore(
            @NotNull BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer);

    /**
     * Concatenates a runnable always called when outputs complete, even if an error occurred.
     * <br>
     * Both outputs and errors will be automatically passed on.
     *
     * @param action the runnable instance.
     * @return the new stream instance.
     */
    @NotNull
    @StreamFlow(MAP)
    StreamChannel<IN, OUT> tryFinally(@NotNull Runnable action);

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
