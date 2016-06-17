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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * I/O channel caching the output data and passing them to newly bound consumer, thus effectively
 * supporting binding of several output consumers.
 * <p>
 * The {@link #isBound()} method will always return false and the bound methods will never fail.
 * <br>
 * Note, however, that the implementation will silently prevent the same consumer or channel
 * instance to be bound twice.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <OUT> the output data type.
 */
class ReplayChannel<OUT> implements OutputChannel<OUT>, OutputConsumer<OUT> {

    private final ArrayList<OUT> mCached = new ArrayList<OUT>();

    private final OutputChannel<OUT> mChannel;

    private final IdentityHashMap<InputChannel<? super OUT>, Void> mChannels =
            new IdentityHashMap<InputChannel<? super OUT>, Void>();

    private final ChannelConfiguration mConfiguration;

    private final IdentityHashMap<OutputConsumer<? super OUT>, IOChannel<OUT>> mConsumers =
            new IdentityHashMap<OutputConsumer<? super OUT>, IOChannel<OUT>>();

    private final Object mMutex = new Object();

    private RoutineException mAbortException;

    private boolean mIsComplete;

    private volatile IOChannel<OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param channel       the channel to replay.
     */
    ReplayChannel(@Nullable final ChannelConfiguration configuration,
            @NotNull final OutputChannel<OUT> channel) {
        mConfiguration = (configuration != null) ? configuration
                : ChannelConfiguration.defaultConfiguration();
        mOutputChannel = createOutputChannel();
        mChannel = channel;
        channel.bind(this);
    }

    public boolean abort() {
        return mChannel.abort();
    }

    public boolean abort(@Nullable final Throwable reason) {
        return mChannel.abort(reason);
    }

    public boolean isEmpty() {
        if (mChannel.isEmpty()) {
            synchronized (mMutex) {
                return mCached.isEmpty();
            }
        }

        return false;
    }

    public boolean isOpen() {
        return mChannel.isOpen();
    }

    public int size() {
        return mOutputChannel.size();
    }

    @NotNull
    public OutputChannel<OUT> afterMax(@NotNull final UnitDuration timeout) {
        mOutputChannel.afterMax(timeout);
        return this;
    }

    @NotNull
    public OutputChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {
        mOutputChannel.afterMax(timeout, timeUnit);
        return this;
    }

    @NotNull
    public List<OUT> all() {
        return mOutputChannel.all();
    }

    @NotNull
    public OutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {
        mOutputChannel.allInto(results);
        return this;
    }

    @NotNull
    public <IN extends InputChannel<? super OUT>> IN bind(@NotNull final IN channel) {
        synchronized (mMutex) {
            final IdentityHashMap<InputChannel<? super OUT>, Void> channels = mChannels;
            if (channels.containsKey(channel)) {
                return channel;
            }

            channels.put(channel, null);
        }

        channel.pass(this);
        return channel;
    }

    @NotNull
    public OutputChannel<OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {
        final boolean isComplete;
        final RoutineException abortException;
        final IOChannel<OUT> outputChannel;
        final IOChannel<OUT> inputChannel;
        final IOChannel<OUT> newChannel;
        final ArrayList<OUT> cachedOutputs;
        synchronized (mMutex) {
            final IdentityHashMap<OutputConsumer<? super OUT>, IOChannel<OUT>> consumers =
                    mConsumers;
            if (consumers.containsKey(consumer)) {
                return this;
            }

            isComplete = mIsComplete;
            abortException = mAbortException;
            outputChannel = mOutputChannel;
            if ((abortException == null) && !isComplete) {
                consumers.put(consumer, outputChannel);
            }

            mOutputChannel = (newChannel = createOutputChannel());
            inputChannel = JRoutineCore.io().buildChannel();
            newChannel.pass(inputChannel);
            cachedOutputs = new ArrayList<OUT>(mCached);
        }

        inputChannel.pass(cachedOutputs).close();
        if (abortException != null) {
            newChannel.abort(abortException);

        } else if (isComplete) {
            newChannel.close();
        }

        outputChannel.bind(consumer);
        return this;
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {
        return mOutputChannel.eventualIterator();
    }

    @NotNull
    public OutputChannel<OUT> eventuallyAbort() {
        mOutputChannel.eventuallyAbort();
        return this;
    }

    @NotNull
    public OutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {
        mOutputChannel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public OutputChannel<OUT> eventuallyBreak() {
        mOutputChannel.eventuallyBreak();
        return this;
    }

    @NotNull
    public OutputChannel<OUT> eventuallyThrow() {
        mOutputChannel.eventuallyThrow();
        return this;
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

    public OUT next() {
        return mOutputChannel.next();
    }

    @NotNull
    public OutputChannel<OUT> immediately() {
        mOutputChannel.immediately();
        return this;
    }

    public boolean isBound() {
        return false;
    }

    @NotNull
    public List<OUT> next(final int count) {
        return mOutputChannel.next(count);
    }

    public OUT nextOrElse(final OUT output) {
        return mOutputChannel.nextOrElse(output);
    }

    @NotNull
    public OutputChannel<OUT> skipNext(final int count) {
        mOutputChannel.skipNext(count);
        return this;
    }

    public void throwError() {
        mOutputChannel.throwError();
    }

    public Iterator<OUT> iterator() {
        return mOutputChannel.iterator();
    }

    public void onComplete() {
        final ArrayList<IOChannel<OUT>> channels;
        synchronized (mMutex) {
            mIsComplete = true;
            channels = new ArrayList<IOChannel<OUT>>(mConsumers.values());
        }

        mOutputChannel.close();
        for (final IOChannel<OUT> channel : channels) {
            channel.close();
        }
    }

    public void onError(@NotNull final RoutineException error) {
        final ArrayList<IOChannel<OUT>> channels;
        synchronized (mMutex) {
            mAbortException = error;
            channels = new ArrayList<IOChannel<OUT>>(mConsumers.values());
        }

        mOutputChannel.abort(error);
        for (final IOChannel<OUT> channel : channels) {
            channel.abort(error);
        }
    }

    public void onOutput(final OUT output) {
        final ArrayList<IOChannel<OUT>> channels;
        synchronized (mMutex) {
            mCached.add(output);
            channels = new ArrayList<IOChannel<OUT>>(mConsumers.values());
        }

        mOutputChannel.pass(output);
        for (final IOChannel<OUT> channel : channels) {
            channel.pass(output);
        }
    }

    public void remove() {
        mOutputChannel.remove();
    }

    @NotNull
    private IOChannel<OUT> createOutputChannel() {
        return JRoutineCore.io()
                           .channelConfiguration()
                           .with(mConfiguration)
                           .withOrder(OrderType.BY_CALL)
                           .apply()
                           .buildChannel();
    }
}
