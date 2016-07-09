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

package com.github.dm.jrt.stream.builder;

import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining a builder of routines concatenating map and reduce functions.
 * <br>
 * Each function in the stream will be backed by a routine instance, which may have its own
 * specific configuration and invocation mode.
 * <p>
 * Note that, if at least one reduce function is part of the chain, the results will be propagated
 * only when the built routine invocation completes.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamBuilder<IN, OUT> extends RoutineBuilder<IN, OUT>, Channel<IN, OUT> {

    /**
     * {@inheritDoc}
     * <p>
     * Note that the stream configuration will be employed to build the routine instance.
     */
    @NotNull
    Routine<IN, OUT> buildRoutine();

    /**
     * Makes the stream asynchronous, that is, the concatenated routines will be invoked in
     * asynchronous mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> async();

    /**
     * Builds a new invocation factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    InvocationFactory<IN, OUT> buildFactory();

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
    <AFTER> StreamBuilder<IN, AFTER> flatMap(
            @NotNull Function<? super OUT, ? extends Channel<?, ? extends AFTER>> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @NotNull
    Builder<? extends StreamBuilder<IN, OUT>> invocationConfiguration();

    /**
     * Makes the stream invoke concatenated routines with the specified mode.
     *
     * @param invocationMode the invocation mode.
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> invocationMode(@NotNull InvocationMode invocationMode);

    /**
     * Transforms this stream by applying the specified function.
     * <p>
     * This method provides a convenient way to apply a set of configurations and concatenations
     * without breaking the fluent chain.
     *
     * @param liftFunction the lift function.
     * @param <BEFORE>     the concatenation input type.
     * @param <AFTER>      the concatenation output type.
     * @return the lifted builder.
     * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
     *                                                                  occurs.
     */
    @NotNull
    <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> let(
            @NotNull Function<? super StreamBuilder<IN, OUT>, ? extends
                    StreamBuilder<BEFORE, AFTER>> liftFunction);

    /**
     * Transforms this stream by applying the specified function.
     * <br>
     * The current configuration of the stream will be passed as the first parameter.
     * <p>
     * This method provides a convenient way to apply a set of configurations and concatenations
     * without breaking the fluent chain.
     *
     * @param liftFunction the lift function.
     * @param <BEFORE>     the concatenation input type.
     * @param <AFTER>      the concatenation output type.
     * @return the lifted builder.
     * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
     *                                                                  occurs.
     */
    @NotNull
    <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> letWithConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? super StreamBuilder<IN, OUT>, ?
                    extends StreamBuilder<BEFORE, AFTER>> liftFunction);

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
     * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
     *                                                                  occurs.
     */
    @NotNull
    <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> lift(@NotNull Function<? extends Function<? super
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
     * @throws com.github.dm.jrt.stream.builder.StreamBuildingException if an unexpected error
     *                                                                  occurs.
     */
    @NotNull
    <BEFORE, AFTER> StreamBuilder<BEFORE, AFTER> liftWithConfig(
            @NotNull BiFunction<? extends StreamConfiguration, ? extends Function<? super
                    Channel<?, IN>, ? extends Channel<?, OUT>>, ? extends Function<? super
                    Channel<?, BEFORE>, ? extends Channel<?, AFTER>>> liftFunction);

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
    <AFTER> StreamBuilder<IN, AFTER> map(
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
    <AFTER> StreamBuilder<IN, AFTER> map(
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
    <AFTER> StreamBuilder<IN, AFTER> map(@NotNull Routine<? super OUT, ? extends AFTER> routine);

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
    <AFTER> StreamBuilder<IN, AFTER> map(
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
    <AFTER> StreamBuilder<IN, AFTER> mapAll(
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
    <AFTER> StreamBuilder<IN, AFTER> mapAllWith(
            @NotNull BiConsumer<? super List<OUT>, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Short for {@code async().streamInvocationConfiguration().withRunner(runner).applied()
     * .map(IdentityInvocation.&lt;OUT&gt;factoryOf())}.
     * <br>
     * This method is useful to easily make the stream run on the specified runner.
     * <p>
     * Note that it is not necessary to explicitly concatenate a routine to have a stream delivering
     * the output data through the specified runner.
     *
     * @param runner the runner instance.
     * @return this builder.
     */
    @NotNull
    StreamBuilder<IN, OUT> mapOn(@Nullable Runner runner);

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
    <AFTER> StreamBuilder<IN, AFTER> mapWith(
            @NotNull BiConsumer<? super OUT, ? super Channel<AFTER, ?>> mappingConsumer);

    /**
     * Makes the stream parallel, that is, the concatenated routines will be invoked in parallel
     * mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> parallel();

    /**
     * Makes the stream sequential, that is, the concatenated routines will be invoked in sequential
     * mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> sequential();

    /**
     * Makes the outputs of this stream sorted by the order they are passed to the result channel.
     *
     * @return this builder.
     */
    @NotNull
    StreamBuilder<IN, OUT> sorted();

    /**
     * Makes so the stream straight so that each input is immediately passed through the following
     * chain of routines.
     * <p>
     * Unlike the default synchronous runner, the employed one makes so that each routine in the
     * chain is passed any input as soon as it is produced by the previous one. Such behavior
     * decreases memory demands at the expense of a deeper stack of calls. In fact, the default
     * synchronous runner breaks up routine calls so to perform them in a loop. The main drawback of
     * the latter approach is that all input data might be accumulated before actually being
     * processed by the next routine invocation.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> straight();

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
    Builder<? extends StreamBuilder<IN, OUT>> streamInvocationConfiguration();

    /**
     * Makes the stream synchronous, that is, the concatenated routines will be invoked in
     * synchronous mode.
     *
     * @return this builder.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    StreamBuilder<IN, OUT> sync();

    /**
     * Makes the outputs of this stream unsorted.
     *
     * @return this builder.
     */
    @NotNull
    StreamBuilder<IN, OUT> unsorted();

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
        InvocationConfiguration getCurrentInvocationConfiguration();

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
        InvocationConfiguration getStreamInvocationConfiguration();
    }
}
