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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.ChannelConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.core.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a transport channel.
 * <p/>
 * Created by davide-maestroni on 10/24/14.
 *
 * @param <DATA> the data type.
 */
class DefaultTransportChannel<DATA> implements TransportChannel<DATA> {

    private final DefaultTransportInput<DATA> mInputChannel;

    private final DefaultTransportOutput<DATA> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     */
    DefaultTransportChannel(@Nonnull final ChannelConfiguration configuration) {

        final InvocationConfiguration invocationConfiguration =
                asInvocationConfiguration(configuration);
        final Logger logger = invocationConfiguration.newLogger(this);
        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final DefaultResultChannel<DATA> inputChannel =
                new DefaultResultChannel<DATA>(invocationConfiguration, abortHandler,
                                               invocationConfiguration.getAsyncRunnerOr(
                                                       Runners.sharedRunner()), logger);
        abortHandler.setChannel(inputChannel);
        mInputChannel = new DefaultTransportInput<DATA>(inputChannel);
        mOutputChannel = new DefaultTransportOutput<DATA>(inputChannel.getOutput());
        logger.dbg("building transport channel with configuration: %s", configuration);
    }

    @Nonnull
    private static InvocationConfiguration asInvocationConfiguration(
            @Nonnull final ChannelConfiguration configuration) {

        return InvocationConfiguration.builder()
                                      .withAsyncRunner(configuration.getAsyncRunnerOr(null))
                                      .withOutputMaxSize(configuration.getChannelMaxSizeOr(
                                              InvocationConfiguration.DEFAULT))
                                      .withOutputOrder(configuration.getChannelOrderTypeOr(null))
                                      .withOutputTimeout(configuration.getChannelTimeoutOr(null))
                                      .withReadTimeout(configuration.getReadTimeoutOr(null))
                                      .withReadTimeoutAction(
                                              configuration.getReadTimeoutActionOr(null))
                                      .withLog(configuration.getLogOr(null))
                                      .withLogLevel(configuration.getLogLevelOr(null))
                                      .set();
    }

    @Nonnull
    public TransportInput<DATA> input() {

        return mInputChannel;
    }

    @Nonnull
    public TransportOutput<DATA> output() {

        return mOutputChannel;
    }

    /**
     * Abort handler used to close the input channel on abort.
     */
    private static class ChannelAbortHandler implements AbortHandler {

        private DefaultResultChannel<?> mChannel;

        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.close(reason);
        }

        private void setChannel(@Nonnull final DefaultResultChannel<?> channel) {

            mChannel = channel;
        }
    }

    /**
     * Default implementation of a transport channel input.
     *
     * @param <INPUT> the input data type.
     */
    private static class DefaultTransportInput<INPUT> implements TransportInput<INPUT> {

        private final DefaultResultChannel<INPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped result channel.
         */
        private DefaultTransportInput(@Nonnull final DefaultResultChannel<INPUT> wrapped) {

            mChannel = wrapped;
        }

        public boolean abort() {

            return mChannel.abort();
        }

        @Nonnull
        public TransportInput<INPUT> after(@Nonnull final TimeDuration delay) {

            mChannel.after(delay);
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mChannel.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> now() {

            mChannel.now();
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> orderByCall() {

            mChannel.orderByCall();
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> orderByChance() {

            mChannel.orderByChance();
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> orderByDelay() {

            mChannel.orderByDelay();
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> pass(@Nullable final OutputChannel<? extends INPUT> channel) {

            mChannel.pass(channel);
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

            mChannel.pass(inputs);
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> pass(@Nullable final INPUT input) {

            mChannel.pass(input);
            return this;
        }

        @Nonnull
        public TransportInput<INPUT> pass(@Nullable final INPUT... inputs) {

            mChannel.pass(inputs);
            return this;
        }

        public void close() {

            mChannel.close();
        }

        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Default implementation of a transport channel output.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultTransportOutput<OUTPUT> implements TransportOutput<OUTPUT> {

        private final OutputChannel<OUTPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped output channel.
         */
        private DefaultTransportOutput(@Nonnull final OutputChannel<OUTPUT> wrapped) {

            mChannel = wrapped;
        }

        @Nonnull
        public TransportOutput<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            mChannel.afterMax(timeout);
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.afterMax(timeout, timeUnit);
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> allInto(@Nonnull final Collection<? super OUTPUT> result) {

            mChannel.allInto(result);
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> eventually() {

            mChannel.eventually();
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> eventuallyAbort() {

            mChannel.eventuallyAbort();
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> eventuallyDeadlock() {

            mChannel.eventuallyDeadlock();
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> eventuallyExit() {

            mChannel.eventuallyExit();
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> immediately() {

            mChannel.immediately();
            return this;
        }

        @Nonnull
        public TransportOutput<OUTPUT> passTo(
                @Nonnull final OutputConsumer<? super OUTPUT> consumer) {

            mChannel.passTo(consumer);
            return this;
        }

        @Nonnull
        public List<OUTPUT> all() {

            return mChannel.all();
        }

        public boolean checkComplete() {

            return mChannel.checkComplete();
        }

        public boolean isBound() {

            return mChannel.isBound();
        }

        public OUTPUT next() {

            return mChannel.next();
        }

        @Nonnull
        public <INPUT extends InputChannel<? super OUTPUT>> INPUT passTo(
                @Nonnull final INPUT channel) {

            channel.pass(this);
            return channel;
        }

        public Iterator<OUTPUT> iterator() {

            return mChannel.iterator();
        }

        public boolean abort() {

            return mChannel.abort();
        }

        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }
}
