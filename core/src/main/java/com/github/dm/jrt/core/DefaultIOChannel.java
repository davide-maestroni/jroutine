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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.DefaultResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of an I/O channel.
 * <p/>
 * Created by davide-maestroni on 10/24/2014.
 *
 * @param <DATA> the data type.
 */
class DefaultIOChannel<DATA> implements IOChannel<DATA> {

    private final DefaultResultChannel<DATA> mInputChannel;

    private final OutputChannel<DATA> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     */
    DefaultIOChannel(@NotNull final ChannelConfiguration configuration) {

        final InvocationConfiguration invocationConfiguration =
                configuration.toOutputChannelConfiguration();
        final Logger logger = invocationConfiguration.newLogger(this);
        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final DefaultResultChannel<DATA> inputChannel =
                new DefaultResultChannel<DATA>(invocationConfiguration, abortHandler,
                        invocationConfiguration.getRunnerOr(Runners.sharedRunner()), logger);
        abortHandler.setChannel(inputChannel);
        mInputChannel = inputChannel;
        mOutputChannel = inputChannel.getOutput();
        logger.dbg("building I/O channel with configuration: %s", configuration);
    }

    public boolean abort() {

        return mInputChannel.abort() || mOutputChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {

        return mInputChannel.abort(reason) || mOutputChannel.abort(reason);
    }

    public boolean isEmpty() {

        return mInputChannel.isEmpty() && mOutputChannel.isEmpty();
    }

    public boolean isOpen() {

        return mInputChannel.isOpen();
    }

    @NotNull
    public IOChannel<DATA> after(@NotNull final TimeDuration delay) {

        mInputChannel.after(delay);
        return this;
    }

    @NotNull
    public IOChannel<DATA> after(final long delay, @NotNull final TimeUnit timeUnit) {

        mInputChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public IOChannel<DATA> now() {

        mInputChannel.now();
        return this;
    }

    @NotNull
    public IOChannel<DATA> orderByCall() {

        mInputChannel.orderByCall();
        return this;
    }

    @NotNull
    public IOChannel<DATA> orderByDelay() {

        mInputChannel.orderByDelay();
        return this;
    }

    @NotNull
    public IOChannel<DATA> pass(@Nullable final OutputChannel<? extends DATA> channel) {

        mInputChannel.pass(channel);
        return this;
    }

    @NotNull
    public IOChannel<DATA> pass(@Nullable final Iterable<? extends DATA> inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public IOChannel<DATA> pass(@Nullable final DATA input) {

        mInputChannel.pass(input);
        return this;
    }

    @NotNull
    public IOChannel<DATA> pass(@Nullable final DATA... inputs) {

        mInputChannel.pass(inputs);
        return this;
    }

    @NotNull
    public IOChannel<DATA> afterMax(@NotNull final TimeDuration timeout) {

        mOutputChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public IOChannel<DATA> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

        mOutputChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public IOChannel<DATA> allInto(@NotNull final Collection<? super DATA> results) {

        mOutputChannel.allInto(results);
        return this;
    }

    @NotNull
    public IOChannel<DATA> bind(@NotNull final OutputConsumer<? super DATA> consumer) {

        mOutputChannel.bind(consumer);
        return this;
    }

    @NotNull
    public IOChannel<DATA> eventuallyAbort() {

        mOutputChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public IOChannel<DATA> eventuallyAbort(@Nullable final Throwable reason) {

        mOutputChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public IOChannel<DATA> eventuallyExit() {

        mOutputChannel.eventuallyExit();
        return this;
    }

    @NotNull
    public IOChannel<DATA> eventuallyThrow() {

        mOutputChannel.eventuallyThrow();
        return this;
    }

    @NotNull
    public IOChannel<DATA> immediately() {

        mOutputChannel.immediately();
        return this;
    }

    @NotNull
    public IOChannel<DATA> skip(final int count) {

        mOutputChannel.skip(count);
        return this;
    }

    @NotNull
    public InputChannel<DATA> asInput() {

        return this;
    }

    @NotNull
    public OutputChannel<DATA> asOutput() {

        return this;
    }

    @NotNull
    public IOChannel<DATA> close() {

        mInputChannel.close();
        return this;
    }

    @NotNull
    public List<DATA> all() {

        return mOutputChannel.all();
    }

    @NotNull
    public <IN extends InputChannel<? super DATA>> IN bind(@NotNull final IN channel) {

        return mOutputChannel.bind(channel);
    }

    @Nullable
    public RoutineException getError() {

        return mOutputChannel.getError();
    }

    public boolean hasCompleted() {

        return mOutputChannel.hasCompleted();
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

    @NotNull
    public List<DATA> next(final int count) {

        return mOutputChannel.next(count);
    }

    public DATA nextOr(final DATA output) {

        return mOutputChannel.nextOr(output);
    }

    public void throwError() {

        mOutputChannel.throwError();
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

        public void onAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {

            mChannel.close(reason);
        }

        private void setChannel(@NotNull final DefaultResultChannel<?> channel) {

            mChannel = channel;
        }
    }
}
