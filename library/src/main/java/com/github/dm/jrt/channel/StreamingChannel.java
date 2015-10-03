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

import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a streaming channel, that is, an I/O channel in which data are processed by
 * one or more routine invocations.
 * <p/>
 * This type of channel is mainly meant to act as a processing pipeline in which each input produces
 * one or more results.<br/>
 * Streaming channels can be concatenated to form a new channel, or used as inputs for other
 * channels or routine invocations.<br/>
 * Note that a streaming channel must always be closed in order to correctly terminate the lifecycle
 * of the involved invocations.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StreamingChannel<IN, OUT> extends IOChannel<IN, OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> eventually();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingChannel<IN, OUT> close();

    /**
     * Creates a new streaming channel which is the concatenation of this channel and the specified
     * one.
     * <p/>
     * Note that the passed channel will be closed as a result of the call.
     *
     * @param channel the channel to concatenate after this one.
     * @param <AFTER> the concatenation output type.
     * @return the concatenated channel.
     */
    @NotNull
    <AFTER> StreamingChannel<IN, AFTER> append(@NotNull IOChannel<? super OUT, AFTER> channel);

    /**
     * Creates a new streaming channel which is the concatenation of the specified channel and this
     * one.
     * <p/>
     * Note that this channel will be closed as a result of the call.
     *
     * @param channel  the channel after which to concatenate this one.
     * @param <BEFORE> the concatenation input type.
     * @return the concatenated channel.
     */
    @NotNull
    <BEFORE> StreamingChannel<BEFORE, OUT> prepend(
            @NotNull IOChannel<BEFORE, ? extends IN> channel);
}
