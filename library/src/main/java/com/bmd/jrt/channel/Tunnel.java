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
 * Interface defining a tunnel.
 * <p/>
 * An tunnel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The tunnel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from another source. Note however, that in both cases the
 * <b><code>close()</code></b> method must be called to correctly terminate the invocation
 * lifecycle.
 * <p/>
 * Created by davide on 10/25/14.
 *
 * @param <TYPE> the data type.
 */
public interface Tunnel<TYPE> {

    /**
     * Returns the input end of this tunnel.
     *
     * @return the input channel.
     */
    @Nonnull
    public TunnelInput<TYPE> input();

    /**
     * Returns the output end of this tunnel.
     *
     * @return the output channel.
     */
    @Nonnull
    public TunnelOutput<TYPE> output();

    /**
     * Interface defining a tunnel input.
     *
     * @param <INPUT> the input data type.
     */
    public interface TunnelInput<INPUT> extends InputChannel<INPUT> {

        @Nonnull
        @Override
        public TunnelInput<INPUT> after(@Nonnull TimeDuration delay);

        @Nonnull
        @Override
        public TunnelInput<INPUT> after(long delay, @Nonnull TimeUnit timeUnit);

        @Nonnull
        @Override
        public TunnelInput<INPUT> now();

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable OutputChannel<INPUT> channel);

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable Iterable<? extends INPUT> inputs);

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable INPUT input);

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable INPUT... inputs);

        /**
         * Closes the tunnel input.<br/>
         * If the channel is already close, this method has no effect.
         * <p/>
         * Note that this method must be always called when done with the tunnel.
         */
        public void close();
    }

    /**
     * Interface defining a tunnel output.
     *
     * @param <OUTPUT> the output data type.
     */
    public interface TunnelOutput<OUTPUT> extends OutputChannel<OUTPUT> {

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> afterMax(@Nonnull TimeDuration timeout);

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> bind(@Nonnull OutputConsumer<OUTPUT> consumer);

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventually();

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyAbort();

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyDeadlock();

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyExit();

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> immediately();

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> readAllInto(@Nonnull Collection<? super OUTPUT> results);
    }
}
