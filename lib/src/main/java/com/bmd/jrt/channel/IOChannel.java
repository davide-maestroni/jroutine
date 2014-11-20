/**
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
package com.bmd.jrt.channel;

import com.bmd.jrt.time.TimeDuration;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining an input/output channel.
 * <p/>
 * An I/O channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from another source. Note however, that in both cases the
 * <b><code>close()</code></b> method must be called to correctly terminate the invocation
 * lifecycle.
 * <p/>
 * Created by davide on 10/25/14.
 *
 * @param <TYPE> the data type.
 */
public interface IOChannel<TYPE> {

    /**
     * Returns the input end of this channel.
     *
     * @return the input channel.
     */
    @Nonnull
    public ChannelInput<TYPE> input();

    /**
     * Returns the output end of this channel.
     *
     * @return the output channel.
     */
    @Nonnull
    public ChannelOutput<TYPE> output();

    /**
     * Interface defining an I/O channel input.
     *
     * @param <INPUT> the input type.
     */
    public interface ChannelInput<INPUT> extends InputChannel<INPUT> {

        @Nonnull
        @Override
        public ChannelInput<INPUT> after(@Nonnull TimeDuration delay);

        @Nonnull
        @Override
        public ChannelInput<INPUT> after(long delay, @Nonnull TimeUnit timeUnit);

        @Nonnull
        @Override
        public ChannelInput<INPUT> now();

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable OutputChannel<INPUT> channel);

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable Iterable<? extends INPUT> inputs);

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable INPUT input);

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable INPUT... inputs);

        /**
         * Closes the channel input.<br/>
         * If the channel is already close, this method has no effect.
         * <p/>
         * Note that this method must be always called when done with the channel.
         */
        public void close();
    }

    /**
     * Interface defining an I/O channel output.
     *
     * @param <OUTPUT> the output type.
     */
    public interface ChannelOutput<OUTPUT> extends OutputChannel<OUTPUT> {

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> afterMax(@Nonnull TimeDuration timeout);

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> bind(@Nullable OutputConsumer<OUTPUT> consumer);

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> eventuallyThrow(@Nullable RuntimeException exception);

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> immediately();

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> readAllInto(@Nonnull Collection<? super OUTPUT> results);
    }
}
