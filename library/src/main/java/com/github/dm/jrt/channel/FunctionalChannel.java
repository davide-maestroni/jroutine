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
package com.github.dm.jrt.channel;

import com.github.dm.jrt.builder.ConfigurableBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a functional channel, that is, an I/O channel concatenating map and reduce
 * functions.<br/>
 * Each function in the channel is backed by a routine that can have its own configuration and
 * invocation mode.
 * <p/>
 * Note that, when at least one reduce function is part of the channel, the results will be
 * propagated only when the channel is closed.<br/>
 * Note also that, contrary to routines, instances of this interface cannot be successfully reused,
 * but must be re-created each time after being closed.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface FunctionalChannel<IN, OUT>
        extends IOChannel<IN, OUT>, ConfigurableBuilder<FunctionalChannel<IN, OUT>> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    FunctionalChannel<IN, OUT> close();

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * one.
     * <p/>
     * Note that the passed channel will be closed as a result of the call.
     *
     * @param channel the channel to concatenate after this one.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMap(@NotNull IOChannel<? super OUT, AFTER> channel);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in an asynchronous
     * mode.
     *
     * @param consumer the bi-consumer to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in an asynchronous
     * mode.
     *
     * @param invocation the filter invocation to concatenate.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in an asynchronous
     * mode.
     *
     * @param function the function to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapAsync(
            @NotNull Function<? super OUT, AFTER> function);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a parallel mode.
     *
     * @param invocation the filter invocation to concatenate.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a parallel mode.
     *
     * @param function the function to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapParallel(
            @NotNull Function<? super OUT, AFTER> function);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a synchronous
     * mode.
     *
     * @param consumer the bi-consumer to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull BiConsumer<? super OUT, ? super ResultChannel<AFTER>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a synchronous
     * mode.
     *
     * @param invocation the filter invocation to concatenate.
     * @param <AFTER>    the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull FilterInvocation<? super OUT, AFTER> invocation);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a synchronous
     * mode.
     *
     * @param function the function to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenMapSync(
            @NotNull Function<? super OUT, AFTER> function);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * reducing consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in an asynchronous
     * mode.
     *
     * @param consumer the reducing bi-consumer to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenReduceAsync(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * reducing function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in an asynchronous
     * mode.
     *
     * @param function the reducing function to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenReduceAsync(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * reducing consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a synchronous
     * mode.
     *
     * @param consumer the reducing bi-consumer to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenReduceSync(
            @NotNull BiConsumer<? super List<? extends OUT>, ? super ResultChannel<AFTER>>
                    consumer);

    /**
     * Creates a new functional channel which is the concatenation of this channel and the specified
     * reducing function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a synchronous
     * mode.
     *
     * @param function the reducing function to concatenate.
     * @param <AFTER>  the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> FunctionalChannel<IN, AFTER> andThenReduceSync(
            @NotNull Function<? super List<? extends OUT>, AFTER> function);

    /**
     * Creates a new functional channel which is the concatenation of the specified channel and this
     * one.
     * <p/>
     * Note that this channel will be closed as a result of the call.
     *
     * @param channel  the channel after which to concatenate this one.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> compose(
            @NotNull IOChannel<BEFORE, ? extends IN> channel);

    /**
     * Creates a new functional channel which is the concatenation of the specified filter
     * invocation and this channel.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in an asynchronous
     * mode.
     *
     * @param invocation the filter invocation after which to concatenate this channel.
     * @param <BEFORE>   the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    /**
     * Creates a new functional channel which is the concatenation of the specified consumer and
     * this channel.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in an asynchronous
     * mode.
     *
     * @param consumer the consumer after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of the specified function and
     * this channel.
     * <p/>
     * Note that the routine created on the specified function will be invoked in an asynchronous
     * mode.
     *
     * @param function the function after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeAsync(
            @NotNull Function<BEFORE, ? extends IN> function);

    /**
     * Creates a new functional channel which is the concatenation of the specified filter
     * invocation and this channel.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a parallel mode.
     *
     * @param invocation the filter invocation after which to concatenate this channel.
     * @param <BEFORE>   the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    /**
     * Creates a new functional channel which is the concatenation of the specified consumer and
     * this channel.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a parallel mode.
     *
     * @param consumer the consumer after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of the specified function and
     * this channel.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a parallel mode.
     *
     * @param function the function after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeParallel(
            @NotNull Function<BEFORE, ? extends IN> function);

    /**
     * Creates a new functional channel which is the concatenation of the specified filter
     * invocation and this channel.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a synchronous
     * mode.
     *
     * @param invocation the filter invocation after which to concatenate this channel.
     * @param <BEFORE>   the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull FilterInvocation<BEFORE, ? extends IN> invocation);

    /**
     * Creates a new functional channel which is the concatenation of the specified consumer and
     * this channel.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a synchronous
     * mode.
     *
     * @param consumer the consumer after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull BiConsumer<BEFORE, ? super ResultChannel<IN>> consumer);

    /**
     * Creates a new functional channel which is the concatenation of the specified function and
     * this channel.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a synchronous
     * mode.
     *
     * @param function the function after which to concatenate this channel.
     * @param <BEFORE> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> FunctionalChannel<BEFORE, OUT> composeSync(
            @NotNull Function<BEFORE, ? extends IN> function);

    /**
     * {@inheritDoc}
     */
    @NotNull
    Builder<? extends FunctionalChannel<IN, OUT>> invocations();
}
