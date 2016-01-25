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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining a channel which is both an input and an output.<br/>
 * The channel synchronously delivers the passed inputs the the bound output consumer or input
 * channel, unless a specific delay is set by calling the proper methods.
 * <p/>
 * An I/O channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from other sources. Note however that, in any case, the {@code close()} method
 * must be called in order to correctly terminate the invocation lifecycle.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 *
 * @param <DATA> the data type.
 */
public interface IOChannel<DATA> extends InputChannel<DATA>, OutputChannel<DATA> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> pass(@Nullable OutputChannel<? extends DATA> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> pass(@Nullable Iterable<? extends DATA> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> pass(@Nullable DATA input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> pass(@Nullable DATA... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> allInto(@NotNull Collection<? super DATA> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> eventuallyAbort(@Nullable Throwable reason);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> passTo(@NotNull OutputConsumer<? super DATA> consumer);

    /**
     * {@inheritDoc}
     */
    @NotNull
    IOChannel<DATA> skip(int count);

    /**
     * Returns this channel as an input one.
     *
     * @return this channel.
     */
    @NotNull
    InputChannel<DATA> asInput();

    /**
     * Returns this channel as an output one.
     *
     * @return this channel.
     */
    @NotNull
    OutputChannel<DATA> asOutput();

    /**
     * Closes the channel input.<br/>
     * If the channel is already closed, this method has no effect.
     * <p/>
     * Note that this method must be always called when done with the channel.
     *
     * @return this channel.
     */
    @NotNull
    IOChannel<DATA> close();
}
