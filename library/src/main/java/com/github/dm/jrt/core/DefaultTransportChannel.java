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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.TransportChannel;
import com.github.dm.jrt.core.DefaultResultChannel.AbortHandler;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.TimeDuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a transport channel.
 * <p/>
 * Created by davide-maestroni on 10/24/2014.
 *
 * @param <DATA> the data type.
 */
class DefaultTransportChannel<DATA> implements TransportChannel<DATA> {

    private final DefaultResultChannel<DATA> mInputChannel;

    private final OutputChannel<DATA> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     */
    DefaultTransportChannel(@Nonnull final ChannelConfiguration configuration) {

        final InvocationConfiguration invocationConfiguration =
                configuration.toOutputChannelConfiguration();
        final Logger logger = invocationConfiguration.newLogger(this);
        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final DefaultResultChannel<DATA> inputChannel =
                new DefaultResultChannel<DATA>(invocationConfiguration, abortHandler,
                                               invocationConfiguration.getRunnerOr(
                                                       Runners.sharedRunner()), logger);
        abortHandler.setChannel(inputChannel);
        mInputChannel = inputChannel;
        mOutputChannel = inputChannel.getOutput();
        logger.dbg("building transport channel with configuration: %s", configuration);
    }

    public boolean abort() {

        return mInputChannel.abort() || mOutputChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return mInputChannel.abort(reason) || mOutputChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mInputChannel.isEmpty();
    }

    public boolean isOpen() {

        return mInputChannel.isOpen();
    }

    @Nonnull
    public TransportChannel<DATA> after(@Nonnull final TimeDuration delay) {

        mInputChannel.after(delay);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> after(final long delay, @Nonnull final TimeUnit timeUnit) {

        mInputChannel.after(delay, timeUnit);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> now() {

        mInputChannel.now();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> orderByCall() {

        mInputChannel.orderByCall();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> orderByChance() {

        mInputChannel.orderByChance();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> orderByDelay() {

        mInputChannel.orderByDelay();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> pass(@Nullable final OutputChannel<? extends DATA> channel) {

        mInputChannel.pass(channel);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> pass(@Nullable final Iterable<? extends DATA> inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> pass(@Nullable final DATA input) {

        mInputChannel.pass(input);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> pass(@Nullable final DATA... inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> afterMax(@Nonnull final TimeDuration timeout) {

        mOutputChannel.afterMax(timeout);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> afterMax(final long timeout, @Nonnull final TimeUnit timeUnit) {

        mOutputChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> allInto(@Nonnull final Collection<? super DATA> results) {

        mOutputChannel.allInto(results);
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> eventually() {

        mOutputChannel.eventually();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> eventuallyAbort() {

        mOutputChannel.eventuallyAbort();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> eventuallyExit() {

        mOutputChannel.eventuallyExit();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> eventuallyThrow() {

        mOutputChannel.eventuallyThrow();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> immediately() {

        mOutputChannel.immediately();
        return this;
    }

    @Nonnull
    public TransportChannel<DATA> passTo(@Nonnull final OutputConsumer<? super DATA> consumer) {

        mOutputChannel.passTo(consumer);
        return this;
    }

    @Nonnull
    public InputChannel<DATA> asInput() {

        return this;
    }

    @Nonnull
    public OutputChannel<DATA> asOutput() {

        return this;
    }

    @Nonnull
    public TransportChannel<DATA> close() {

        mInputChannel.close();
        return this;
    }

    @Nonnull
    public List<DATA> all() {

        return mOutputChannel.all();
    }

    public boolean checkComplete() {

        return mOutputChannel.checkComplete();
    }

    public boolean hasNext() {

        return mOutputChannel.hasNext();
    }

    public DATA next() {

        return mOutputChannel.next();
    }

    public boolean isBound() {

        return mOutputChannel.isBound();
    }

    @Nonnull
    public <IN extends InputChannel<? super DATA>> IN passTo(@Nonnull final IN channel) {

        return mOutputChannel.passTo(channel);
    }

    public boolean hasDelays() {

        return mInputChannel.hasDelays();
    }

    public Iterator<DATA> iterator() {

        return mOutputChannel.iterator();
    }

    public void remove() {

        mOutputChannel.remove();
    }

    /**
     * Abort handler used to close the input channel on abort.
     */
    private static class ChannelAbortHandler implements AbortHandler {

        private DefaultResultChannel<?> mChannel;

        public void onAbort(@Nullable final RoutineException reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.close(reason);
        }

        private void setChannel(@Nonnull final DefaultResultChannel<?> channel) {

            mChannel = channel;
        }
    }
}
