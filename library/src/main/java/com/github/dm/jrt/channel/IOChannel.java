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
 * Interface defining a channel which is both an input and an output.
 * <p/>
 * An I/O channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from other sources. Note however that, in any case, the {@code close()} method
 * must be called in order to correctly terminate the invocation lifecycle.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface IOChannel<IN, OUT> extends InputChannel<IN>, OutputChannel<OUT> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> pass(@Nullable OutputChannel<? extends IN> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> pass(@Nullable Iterable<? extends IN> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> pass(@Nullable IN input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> pass(@Nullable IN... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> allInto(@NotNull Collection<? super OUT> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> passTo(@NotNull OutputConsumer<? super OUT> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<IN, OUT> skip(int count);

    /**
     * Returns this channel as an input one.
     *
     * @return this channel.
     */
    @NotNull
    InputChannel<IN> asInput();

    /**
     * Returns this channel as an output one.
     *
     * @return this channel.
     */
    @NotNull
    OutputChannel<OUT> asOutput();

    /**
     * Closes the channel input.<br/>
     * If the channel is already closed, this method has no effect.
     * <p/>
     * Note that this method must be always called when done with the channel.
     *
     * @return this channel.
     */
    @NotNull
    IOChannel<IN, OUT> close();
}
