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
package com.bmd.jrt.routine;

import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.Tunnel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a tunnel.
 * <p/>
 * Created by davide on 10/24/14.
 *
 * @param <TYPE> the data type.
 */
class DefaultTunnel<TYPE> implements Tunnel<TYPE> {

    private final DefaultTunnelInput<TYPE> mInputChannel;

    private final DefaultTunnelOutput<TYPE> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     */
    DefaultTunnel(@Nonnull final RoutineConfiguration configuration) {

        final Logger logger = Logger.createLogger(configuration.getLogOr(Logger.getGlobalLog()),
                                                  configuration.getLogLevelOr(
                                                          Logger.getGlobalLogLevel()), this);
        final TunnelAbortHandler abortHandler = new TunnelAbortHandler();
        final DefaultResultChannel<TYPE> inputChannel =
                new DefaultResultChannel<TYPE>(configuration, abortHandler,
                                               configuration.getRunnerOr(Runners.sharedRunner()),
                                               logger);
        abortHandler.setChannel(inputChannel);
        mInputChannel = new DefaultTunnelInput<TYPE>(inputChannel);
        mOutputChannel = new DefaultTunnelOutput<TYPE>(inputChannel.getOutput());
        logger.dbg("building tunnel with configuration: %s", configuration);
    }

    @Nonnull
    @Override
    public TunnelInput<TYPE> input() {

        return mInputChannel;
    }

    @Nonnull
    @Override
    public TunnelOutput<TYPE> output() {

        return mOutputChannel;
    }

    /**
     * Default implementation of a tunnel input.
     *
     * @param <INPUT> the input data type.
     */
    private static class DefaultTunnelInput<INPUT> implements TunnelInput<INPUT> {

        private final DefaultResultChannel<INPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped result channel.
         */
        private DefaultTunnelInput(@Nonnull final DefaultResultChannel<INPUT> wrapped) {

            mChannel = wrapped;
        }

        @Override
        public boolean abort() {

            return mChannel.abort();
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> after(@Nonnull final TimeDuration delay) {

            mChannel.after(delay);
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mChannel.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> now() {

            mChannel.now();
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

            mChannel.pass(channel);
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

            mChannel.pass(inputs);
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable final INPUT input) {

            mChannel.pass(input);
            return this;
        }

        @Nonnull
        @Override
        public TunnelInput<INPUT> pass(@Nullable final INPUT... inputs) {

            mChannel.pass(inputs);
            return this;
        }

        @Override
        public void close() {

            mChannel.close();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Default implementation of a tunnel output.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultTunnelOutput<OUTPUT> implements TunnelOutput<OUTPUT> {

        private final OutputChannel<OUTPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped output channel.
         */
        private DefaultTunnelOutput(@Nonnull final OutputChannel<OUTPUT> wrapped) {

            mChannel = wrapped;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            mChannel.afterMax(timeout);
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> afterMax(final long timeout, @Nonnull final TimeUnit timeUnit) {

            mChannel.afterMax(timeout, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> bind(@Nonnull final OutputConsumer<OUTPUT> consumer) {

            mChannel.bind(consumer);
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventually() {

            mChannel.eventually();
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyAbort() {

            mChannel.eventuallyAbort();
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyDeadlock() {

            mChannel.eventuallyDeadlock();
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> eventuallyExit() {

            mChannel.eventuallyExit();
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> immediately() {

            mChannel.immediately();
            return this;
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> readAllInto(@Nonnull final Collection<? super OUTPUT> result) {

            mChannel.readAllInto(result);
            return this;
        }

        @Override
        public boolean checkComplete() {

            return mChannel.checkComplete();
        }

        @Override
        public boolean isBound() {

            return mChannel.isBound();
        }

        @Nonnull
        @Override
        public List<OUTPUT> readAll() {

            return mChannel.readAll();
        }

        @Override
        public OUTPUT readNext() {

            return mChannel.readNext();
        }

        @Nonnull
        @Override
        public TunnelOutput<OUTPUT> unbind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            mChannel.unbind(consumer);
            return this;
        }

        @Override
        public Iterator<OUTPUT> iterator() {

            return mChannel.iterator();
        }

        @Override
        public boolean abort() {

            return mChannel.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Abort handler used to close the input channel on abort.
     */
    private static class TunnelAbortHandler implements AbortHandler {

        private DefaultResultChannel<?> mChannel;

        @Override
        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.close(reason);
        }

        public void setChannel(@Nonnull final DefaultResultChannel<?> channel) {

            mChannel = channel;
        }
    }
}