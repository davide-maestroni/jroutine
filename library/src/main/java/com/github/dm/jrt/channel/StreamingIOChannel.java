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
public interface StreamingIOChannel<IN, OUT> extends IOChannel<IN, OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> skip(int count);

    /**
     * {@inheritDoc}
     */
    @NotNull
    StreamingIOChannel<IN, OUT> close();

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
    <BEFORE> StreamingIOChannel<BEFORE, OUT> combine(
            @NotNull IOChannel<BEFORE, ? extends IN> channel);

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
    <AFTER> StreamingIOChannel<IN, AFTER> concat(@NotNull IOChannel<? super OUT, AFTER> channel);
}
