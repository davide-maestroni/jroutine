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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @Nonnull
    TransportChannel<DATA> after(@Nonnull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> now();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> orderByCall();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> orderByChance();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> pass(@Nullable OutputChannel<? extends DATA> channel);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> pass(@Nullable Iterable<? extends DATA> inputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> pass(@Nullable DATA input);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> pass(@Nullable DATA... inputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> afterMax(@Nonnull TimeDuration timeout);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> allInto(@Nonnull Collection<? super DATA> results);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> eventually();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> eventuallyAbort();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> eventuallyExit();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> eventuallyThrow();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> immediately();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    TransportChannel<DATA> passTo(@Nonnull OutputConsumer<? super DATA> consumer);

    /**
     * Returns this channel as an input one.
     *
     * @return this channel.
     */
    @Nonnull
    InputChannel<DATA> asInput();

    /**
     * Returns this channel as an output one.
     *
     * @return this channel.
     */
    @Nonnull
    OutputChannel<DATA> asOutput();

    /**
     * Closes the channel input.<br/>
     * If the channel is already closed, this method has no effect.
     * <p/>
     * Note that this method must be always called when done with the channel.
     *
     * @return this channel.
     */
    @Nonnull
    TransportChannel<DATA> close();
}
