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
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.time.TimeDuration;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a transport channel.
 * <p/>
 * A transport channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from other sources. Note however, that in both cases the
 * <b><code>close()</code></b> method must be called to correctly terminate the invocation
 * lifecycle.
 * <p/>
 * Created by davide-maestroni on 10/25/14.
 *
 * @param <DATA> the data type.
 */
public interface TransportChannel<DATA> {

    /**
     * Returns the input end of this channel.
     *
     * @return the input channel.
     */
    @Nonnull
    TransportInput<DATA> input();

    /**
     * Returns the output end of this channel.
     *
     * @return the output channel.
     */
    @Nonnull
    TransportOutput<DATA> output();

    /**
     * Interface defining a transport channel input.
     *
     * @param <INPUT> the input data type.
     */
    interface TransportInput<INPUT> extends InputChannel<INPUT> {

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> after(@Nonnull TimeDuration delay);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> after(long delay, @Nonnull TimeUnit timeUnit);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> now();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> pass(@Nullable OutputChannel<? extends INPUT> channel);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> pass(@Nullable Iterable<? extends INPUT> inputs);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> pass(@Nullable INPUT input);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportInput<INPUT> pass(@Nullable INPUT... inputs);

        /**
         * Closes the channel input.<br/>
         * If the channel is already close, this method has no effect.
         * <p/>
         * Note that this method must be always called when done with the channel.
         */
        void close();
    }

    /**
     * Interface defining a transport channel output.
     *
     * @param <OUTPUT> the output data type.
     */
    interface TransportOutput<OUTPUT> extends OutputChannel<OUTPUT> {

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> afterMax(@Nonnull TimeDuration timeout);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> bind(@Nonnull OutputConsumer<? super OUTPUT> consumer);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> eventually();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> eventuallyAbort();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> eventuallyDeadlock();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> eventuallyExit();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> immediately();

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> readAllInto(@Nonnull Collection<? super OUTPUT> results);

        /**
         * {@inheritDoc}
         */
        @Nonnull
        TransportOutput<OUTPUT> unbind(@Nullable OutputConsumer<? super OUTPUT> consumer);
    }
}
