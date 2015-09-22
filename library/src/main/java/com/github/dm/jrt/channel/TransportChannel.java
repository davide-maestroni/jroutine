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
 * Interface defining a transport channel.
 * <p/>
 * A transport channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from other sources. Note however that, in any case, the {@code close()} method
 * must be called in order to correctly terminate the invocation lifecycle.
 * <p/>
 * Created by davide-maestroni on 10/25/2014.
 *
 * @param <DATA> the data type.
 */
public interface TransportChannel<DATA> extends InputChannel<DATA>, OutputChannel<DATA> {

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> orderByChance();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> pass(@Nullable OutputChannel<? extends DATA> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> pass(@Nullable Iterable<? extends DATA> inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> pass(@Nullable DATA input);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> pass(@Nullable DATA... inputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> afterMax(@NotNull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> afterMax(long timeout, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> allInto(@NotNull Collection<? super DATA> results);

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> eventually();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> immediately();

    /**
     * {@inheritDoc}
     */
    @NotNull
    TransportChannel<DATA> passTo(@NotNull OutputConsumer<? super DATA> consumer);

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
    TransportChannel<DATA> close();
}
