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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.channel.FunctionalChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of functional channels.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 */
public interface FunctionalChannelBuilder extends ConfigurableBuilder<FunctionalChannelBuilder> {

    /**
     * Creates a new functional channel based on the specified consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in an asynchronous
     * mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> async(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional channel based on the specified command invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in an asynchronous
     * mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> async(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional channel based on the specified filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in an asynchronous
     * mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> async(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional channel based on the specified function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in an asynchronous
     * mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> async(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional channel based on the specified supplier.
     * <p/>
     * Note that the routine created on the specified supplier will be invoked in an asynchronous
     * mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> async(@NotNull Supplier<OUT> supplier);

    /**
     * {@inheritDoc}
     */
    @NotNull
    Builder<? extends FunctionalChannelBuilder> invocations();

    /**
     * Creates a new functional channel based on the specified consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a parallel mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> parallel(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional channel based on the specified command invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a parallel mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> parallel(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional channel based on the specified filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a parallel mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> parallel(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional channel based on the specified function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a parallel mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> parallel(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional channel based on the specified supplier.
     * <p/>
     * Note that the routine created on the specified supplier will be invoked in a parallel mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> parallel(@NotNull Supplier<OUT> supplier);

    /**
     * Creates a new functional channel based on the specified consumer.
     * <p/>
     * Note that the routine created on the specified consumer will be invoked in a synchronous
     * mode.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> sync(
            @NotNull BiConsumer<IN, ? super ResultChannel<OUT>> consumer);

    /**
     * Creates a new functional channel based on the specified command invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a synchronous
     * mode.
     *
     * @param invocation the command invocation instance.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> sync(@NotNull CommandInvocation<OUT> invocation);

    /**
     * Creates a new functional channel based on the specified filter invocation.
     * <p/>
     * Note that the routine created on the specified invocation will be invoked in a synchronous
     * mode.
     *
     * @param invocation the filter invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> sync(@NotNull FilterInvocation<IN, OUT> invocation);

    /**
     * Creates a new functional channel based on the specified function.
     * <p/>
     * Note that the routine created on the specified function will be invoked in a synchronous
     * mode.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <IN, OUT> FunctionalChannel<IN, OUT> sync(@NotNull Function<IN, OUT> function);

    /**
     * Creates a new functional channel based on the specified supplier.
     * <p/>
     * Note that the routine created on the specified supplier will be invoked in a synchronous
     * mode.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the functional channel.
     */
    @NotNull
    <OUT> FunctionalChannel<Void, OUT> sync(@NotNull Supplier<OUT> supplier);
}
