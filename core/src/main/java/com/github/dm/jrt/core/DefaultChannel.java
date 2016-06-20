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

import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a channel.
 * <p>
 * Created by davide-maestroni on 10/24/2014.
 *
 * @param <DATA> the data type.
 */
class DefaultChannel<DATA> implements Channel<DATA, DATA> {

    private static final WeakIdentityHashMap<Runner, WeakReference<ChannelRunner>> sRunners =
            new WeakIdentityHashMap<Runner, WeakReference<ChannelRunner>>();

    private static final Runner sSyncRunner = Runners.syncRunner();

    private final ResultChannel<DATA> mChannel;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     */
    DefaultChannel(@NotNull final ChannelConfiguration configuration) {
        final InvocationConfiguration invocationConfiguration =
                configuration.toOutputChannelConfiguration().apply();
        final Logger logger = invocationConfiguration.newLogger(this);
        final Runner wrapped = configuration.getRunnerOrElse(Runners.sharedRunner());
        ChannelRunner channelRunner;
        synchronized (sRunners) {
            final WeakIdentityHashMap<Runner, WeakReference<ChannelRunner>> runners = sRunners;
            final WeakReference<ChannelRunner> runner = runners.get(wrapped);
            channelRunner = (runner != null) ? runner.get() : null;
            if (channelRunner == null) {
                channelRunner = new ChannelRunner(wrapped);
                runners.put(wrapped, new WeakReference<ChannelRunner>(channelRunner));
            }
        }

        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final ResultChannel<DATA> channel =
                new ResultChannel<DATA>(invocationConfiguration, abortHandler, channelRunner,
                        logger);
        abortHandler.setChannel(channel);
        mChannel = channel;
        logger.dbg("building channel with configuration: %s", configuration);
    }

    public boolean abort() {
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return mChannel.abort(reason);
    }

    @NotNull
    public Channel<DATA, DATA> after(@NotNull final UnitDuration delay) {
        mChannel.after(delay);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> after(final long delay, @NotNull final TimeUnit timeUnit) {
        mChannel.after(delay, timeUnit);
        return this;
    }

    @NotNull
    public List<DATA> all() {
        return mChannel.all();
    }

    @NotNull
    public Channel<DATA, DATA> allInto(@NotNull final Collection<? super DATA> results) {
        mChannel.allInto(results);
        return this;
    }

    @NotNull
    public <CHANNEL extends Channel<? super DATA, ?>> CHANNEL bind(@NotNull final CHANNEL channel) {
        return mChannel.bind(channel);
    }

    @NotNull
    public Channel<DATA, DATA> bind(@NotNull final OutputConsumer<? super DATA> consumer) {
        mChannel.bind(consumer);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> close() {
        mChannel.close();
        return this;
    }

    @NotNull
    public Iterator<DATA> eventualIterator() {
        return mChannel.eventualIterator();
    }

    @NotNull
    public Channel<DATA, DATA> eventuallyAbort() {
        mChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> eventuallyAbort(@Nullable final Throwable reason) {
        mChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> eventuallyBreak() {
        mChannel.eventuallyBreak();
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> eventuallyFail() {
        mChannel.eventuallyFail();
        return this;
    }

    @Nullable
    public RoutineException getError() {
        return mChannel.getError();
    }

    public boolean hasCompleted() {
        return mChannel.hasCompleted();
    }

    public boolean hasNext() {
        return mChannel.hasNext();
    }

    public DATA next() {
        return mChannel.next();
    }

    @NotNull
    public Channel<DATA, DATA> immediately() {
        mChannel.immediately();
        return this;
    }

    public int inputCount() {
        return mChannel.inputCount();
    }

    public boolean isBound() {
        return mChannel.isBound();
    }

    public boolean isEmpty() {
        return mChannel.isEmpty();
    }

    public boolean isOpen() {
        return mChannel.isOpen();
    }

    @NotNull
    public List<DATA> next(final int count) {
        return mChannel.next(count);
    }

    public DATA nextOrElse(final DATA output) {
        return mChannel.nextOrElse(output);
    }

    @NotNull
    public Channel<DATA, DATA> orderByCall() {
        mChannel.orderByCall();
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> orderByDelay() {
        mChannel.orderByDelay();
        return this;
    }

    public int outputCount() {
        return mChannel.outputCount();
    }

    @NotNull
    public Channel<DATA, DATA> pass(@Nullable final Channel<?, ? extends DATA> channel) {
        mChannel.pass(channel);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> pass(@Nullable final Iterable<? extends DATA> inputs) {
        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> pass(@Nullable final DATA input) {
        mChannel.pass(input);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> pass(@Nullable final DATA... inputs) {
        mChannel.pass(inputs);
        return this;
    }

    @NotNull
    public Channel<DATA, DATA> skipNext(final int count) {
        mChannel.skipNext(count);
        return this;
    }

    public void throwError() {
        mChannel.throwError();
    }

    public Iterator<DATA> iterator() {
        return mChannel.iterator();
    }

    public void remove() {
        mChannel.remove();
    }

    /**
     * Abort handler used to close the channel on abort.
     */
    private static class ChannelAbortHandler implements AbortHandler {

        private ResultChannel<?> mChannel;

        public void onAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mChannel.close(reason);
        }

        private void setChannel(@NotNull final ResultChannel<?> channel) {
            mChannel = channel;
        }
    }

    /**
     * Runner decorator running executions synchronously if delay is 0.
     */
    private static class ChannelRunner extends RunnerDecorator {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped instance.
         */
        public ChannelRunner(@NotNull final Runner wrapped) {
            super(wrapped);
        }

        @Override
        public boolean isExecutionThread() {
            return true;
        }

        @Override
        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {
            if (delay == 0) {
                sSyncRunner.run(execution, delay, timeUnit);

            } else {
                super.run(execution, delay, timeUnit);
            }
        }
    }
}
