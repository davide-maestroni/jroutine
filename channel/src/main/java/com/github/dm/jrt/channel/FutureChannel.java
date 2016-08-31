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

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.LocalValue;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;
import static com.github.dm.jrt.core.util.UnitDuration.zero;

/**
 * Implementation of a channel backed by a Future instance.
 * <p>
 * Created by davide-maestroni on 08/30/2016.
 */
class FutureChannel<OUT> implements Channel<OUT, OUT> {

    private final AtomicReference<Throwable> mAbortException = new AtomicReference<Throwable>(null);

    private final Future<OUT> mFuture;

    private final AtomicBoolean mIsBound = new AtomicBoolean();

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final LocalValue<UnitDuration> mOutputTimeout;

    private final Runner mRunner;

    private final LocalValue<TimeoutActionType> mTimeoutActionType;

    private final LocalValue<Throwable> mTimeoutException = new LocalValue<Throwable>(null);

    private boolean mIsOutput;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param future        the Future instance.
     */
    FutureChannel(@NotNull final ChannelConfiguration configuration,
            @NotNull final Future<OUT> future) {
        mLogger = configuration.newLogger(this);
        mFuture = ConstantConditions.notNull("future instance", future);
        mRunner = configuration.getRunnerOrElse(Runners.sharedRunner());
        mOutputTimeout = new LocalValue<UnitDuration>(configuration.getOutputTimeoutOrElse(zero()));
        mTimeoutActionType = new LocalValue<TimeoutActionType>(
                configuration.getOutputTimeoutActionOrElse(TimeoutActionType.FAIL));
    }

    public boolean abort() {
        return abort(null);
    }

    public boolean abort(@Nullable final Throwable reason) {
        final boolean isCancelled = mFuture.cancel(reason != null);
        if (isCancelled) {
            mAbortException.set(reason);
        }

        return isCancelled;
    }

    @NotNull
    public Channel<OUT, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        return after(fromUnit(delay, timeUnit));
    }

    @NotNull
    public Channel<OUT, OUT> after(@NotNull final UnitDuration delay) {
        mOutputTimeout.set(ConstantConditions.notNull("delay", delay));
        return this;
    }

    @NotNull
    public List<OUT> all() {
        final ArrayList<OUT> outputs = new ArrayList<OUT>();
        allInto(outputs);
        return outputs;
    }

    @NotNull
    public Channel<OUT, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        ConstantConditions.notNull("results collection", results);
        final Iterator<OUT> iterator = expiringIterator();
        while (iterator.hasNext()) {
            results.add(iterator.next());
        }

        return this;
    }

    @NotNull
    public <AFTER> Channel<? super OUT, AFTER> bind(
            @NotNull final Channel<? super OUT, AFTER> channel) {
        return channel.pass(this);
    }

    @NotNull
    public Channel<OUT, OUT> bind(@NotNull final ChannelConsumer<? super OUT> consumer) {
        if (mIsBound.getAndSet(true)) {
            mLogger.err("invalid call on bound channel");
            throw new IllegalStateException("the channel is already bound");
        }

        final UnitDuration delay = mOutputTimeout.get();
        mRunner.run(new Execution() {

            public void run() {
                try {
                    try {
                        consumer.onOutput(mFuture.get());

                    } catch (final CancellationException e) {
                        consumer.onError(AbortException.wrapIfNeeded(mAbortException.get()));
                        return;

                    } catch (final InterruptedException e) {
                        consumer.onError(new InvocationInterruptedException(e));
                        return;

                    } catch (final Exception e) {
                        consumer.onError(InvocationException.wrapIfNeeded(e));
                        return;
                    }

                    consumer.onComplete();

                } catch (final Throwable t) {
                    mLogger.wrn(t, "consumer exception (%s)", consumer);
                }
            }
        }, delay.value, delay.unit);
        return this;
    }

    @NotNull
    public Channel<OUT, OUT> close() {
        return this;
    }

    @NotNull
    public Channel<OUT, OUT> eventuallyAbort() {
        return eventuallyAbort(null);
    }

    @NotNull
    public Channel<OUT, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        mTimeoutActionType.set(TimeoutActionType.ABORT);
        mTimeoutException.set(reason);
        return this;
    }

    @NotNull
    public Channel<OUT, OUT> eventuallyContinue() {
        mTimeoutActionType.set(TimeoutActionType.CONTINUE);
        mTimeoutException.set(null);
        return this;
    }

    @NotNull
    public Channel<OUT, OUT> eventuallyFail() {
        mTimeoutActionType.set(TimeoutActionType.FAIL);
        mTimeoutException.set(null);
        return this;
    }

    @NotNull
    public Iterator<OUT> expiringIterator() {
        verifyBound();
        return new ExpiringFutureIterator(mOutputTimeout.get(), mTimeoutActionType.get(),
                mTimeoutException.get());
    }

    public boolean getComplete() {
        final UnitDuration delay = mOutputTimeout.get();
        try {
            mFuture.get(delay.value, delay.unit);
            return true;

        } catch (final Exception ignored) {
        }

        return false;
    }

    @Nullable
    public RoutineException getError() {
        final UnitDuration delay = mOutputTimeout.get();
        try {
            mFuture.get(delay.value, delay.unit);

        } catch (final TimeoutException ignored) {

        } catch (final CancellationException e) {
            return AbortException.wrapIfNeeded(mAbortException.get());

        } catch (final InterruptedException e) {
            return new InvocationInterruptedException(e);

        } catch (final Exception e) {
            return InvocationException.wrapIfNeeded(e);
        }

        return null;
    }

    public boolean hasNext() {
        final UnitDuration timeout = mOutputTimeout.get();
        return isNextAvailable(timeout.value, timeout.unit, mTimeoutActionType.get(),
                mTimeoutException.get());
    }

    public OUT next() {
        final UnitDuration timeout = mOutputTimeout.get();
        return readNext(timeout.value, timeout.unit, mTimeoutActionType.get(),
                mTimeoutException.get());
    }

    @NotNull
    public Channel<OUT, OUT> immediately() {
        return after(zero());
    }

    public int inputCount() {
        return outputCount();
    }

    public boolean isBound() {
        return mIsBound.get();
    }

    public boolean isEmpty() {
        return (outputCount() == 0);
    }

    public boolean isOpen() {
        return false;
    }

    @NotNull
    public List<OUT> next(final int count) {
        if (count <= 0) {
            return new ArrayList<OUT>(0);
        }

        return all();
    }

    public OUT nextOrElse(final OUT output) {
        try {
            return next();

        } catch (final NoSuchElementException ignored) {
        }

        return output;
    }

    public int outputCount() {
        if (mFuture.isDone()) {
            synchronized (mMutex) {
                return mIsOutput ? 0 : 1;
            }
        }

        return 0;
    }

    @NotNull
    public Channel<OUT, OUT> pass(@Nullable final Channel<?, ? extends OUT> channel) {
        return failPass();
    }

    @NotNull
    public Channel<OUT, OUT> pass(@Nullable final Iterable<? extends OUT> inputs) {
        return failPass();
    }

    @NotNull
    public Channel<OUT, OUT> pass(@Nullable final OUT input) {
        return failPass();
    }

    @NotNull
    public Channel<OUT, OUT> pass(@Nullable final OUT... inputs) {
        return failPass();
    }

    public int size() {
        return outputCount();
    }

    @NotNull
    public Channel<OUT, OUT> skipNext(final int count) {
        if (count > 0) {
            final Iterator<OUT> iterator = expiringIterator();
            try {
                for (int i = 0; i < count; ++i) {
                    iterator.next();
                }

            } catch (final NoSuchElementException ignored) {
                final UnitDuration timeout = mOutputTimeout.get();
                final TimeoutActionType timeoutAction = mTimeoutActionType.get();
                mLogger.wrn("skipping output timeout: %s => [%s]", timeout, timeoutAction);
                if (timeoutAction == TimeoutActionType.FAIL) {
                    throw new OutputTimeoutException(
                            "timeout while waiting to know if more outputs are coming " + timeout);

                } else if (timeoutAction == TimeoutActionType.ABORT) {
                    final Throwable timeoutException = mTimeoutException.get();
                    abort(timeoutException);
                    throw AbortException.wrapIfNeeded(timeoutException);
                }
            }
        }

        return this;
    }

    @NotNull
    public Channel<OUT, OUT> sorted() {
        return this;
    }

    public void throwError() {
        final RoutineException error = getError();
        if (error != null) {
            throw error;
        }
    }

    @NotNull
    public Channel<OUT, OUT> unsorted() {
        return this;
    }

    @NotNull
    public Iterator<OUT> iterator() {
        verifyBound();
        return new FutureIterator(mOutputTimeout.get(), mTimeoutActionType.get(),
                mTimeoutException.get());
    }

    @NotNull
    private Channel<OUT, OUT> failPass() {
        if (mFuture.isCancelled()) {
            throw AbortException.wrapIfNeeded(mAbortException.get());
        }

        throw new IllegalStateException("the channel is closed");
    }

    private boolean isNextAvailable(final long timeout, @NotNull final TimeUnit timeUnit,
            @NotNull final TimeoutActionType timeoutAction,
            @Nullable final Throwable timeoutException) {
        verifyBound();
        final Logger logger = mLogger;
        try {
            synchronized (mMutex) {
                if (mIsOutput) {
                    return false;
                }

                mFuture.get(timeout, timeUnit);
                logger.dbg("has output: %s [%d %s]", true, timeout, timeUnit);
                return true;
            }

        } catch (final TimeoutException e) {
            logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
            if (timeoutAction == TimeoutActionType.FAIL) {
                throw new OutputTimeoutException(
                        "timeout while waiting to know if more outputs are coming [" + timeout + " "
                                + timeUnit + "]");

            } else if (timeoutAction == TimeoutActionType.ABORT) {
                abort(timeoutException);
                throw AbortException.wrapIfNeeded(timeoutException);
            }

        } catch (final CancellationException e) {
            throw AbortException.wrapIfNeeded(mAbortException.get());

        } catch (final Exception e) {
            InvocationInterruptedException.throwIfInterrupt(e);
            throw InvocationException.wrapIfNeeded(e);
        }

        return false;
    }

    @Nullable
    private OUT readNext(final long timeout, @NotNull final TimeUnit timeUnit,
            @NotNull final TimeoutActionType timeoutAction,
            @Nullable final Throwable timeoutException) {
        verifyBound();
        try {
            synchronized (mMutex) {
                if (mIsOutput) {
                    throw new NoSuchElementException();
                }

                final OUT output = mFuture.get(timeout, timeUnit);
                mIsOutput = true;
                return output;
            }

        } catch (final NoSuchElementException e) {
            throw e;

        } catch (final TimeoutException e) {
            mLogger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit,
                    timeoutAction);
            if (timeoutAction == TimeoutActionType.FAIL) {
                throw new OutputTimeoutException(
                        "timeout while waiting to know if more outputs are coming [" + timeout + " "
                                + timeUnit + "]");

            } else if (timeoutAction == TimeoutActionType.ABORT) {
                abort(timeoutException);
                throw AbortException.wrapIfNeeded(timeoutException);
            }

            throw new NoSuchElementException();

        } catch (final CancellationException e) {
            throw AbortException.wrapIfNeeded(mAbortException.get());

        } catch (final Exception e) {
            InvocationInterruptedException.throwIfInterrupt(e);
            throw InvocationException.wrapIfNeeded(e);
        }
    }

    private void verifyBound() {
        if (mIsBound.get()) {
            mLogger.err("invalid call on bound channel");
            throw new IllegalStateException("the channel is already bound");
        }
    }

    /**
     * Default implementation of a Future channel expiring iterator.
     */
    private class ExpiringFutureIterator implements Iterator<OUT> {

        private final TimeoutActionType mAction;

        private final long mDelayMillis;

        private final Throwable mException;

        private long mEndTime = Long.MIN_VALUE;

        /**
         * Constructor.
         *
         * @param delay     the output delay.
         * @param action    the timeout action.
         * @param exception the timeout exception.
         */
        public ExpiringFutureIterator(@NotNull final UnitDuration delay,
                @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
            mDelayMillis = delay.toMillis();
            mAction = action;
            mException = exception;
        }

        public boolean hasNext() {
            return isNextAvailable(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
        }

        private long getTimeoutMillis() {
            synchronized (mMutex) {
                if (mEndTime == Long.MIN_VALUE) {
                    mEndTime = System.currentTimeMillis() + mDelayMillis;
                }

                return Math.max(0, mEndTime - System.currentTimeMillis());
            }
        }

        public OUT next() {
            return readNext(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
        }

        public void remove() {
            ConstantConditions.unsupported();
        }
    }

    /**
     * Default implementation of a Future channel iterator.
     */
    private class FutureIterator implements Iterator<OUT> {

        private final TimeoutActionType mAction;

        private final Throwable mException;

        private final TimeUnit mTimeUnit;

        private final long mTimeout;

        /**
         * Constructor.
         *
         * @param delay     the output delay.
         * @param action    the timeout action.
         * @param exception the timeout exception.
         */
        public FutureIterator(@NotNull final UnitDuration delay,
                @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {
            mTimeout = delay.value;
            mTimeUnit = delay.unit;
            mAction = action;
            mException = exception;
        }

        public boolean hasNext() {
            return isNextAvailable(mTimeout, mTimeUnit, mAction, mException);
        }

        public OUT next() {
            return readNext(mTimeout, mTimeUnit, mAction, mException);
        }

        public void remove() {
            ConstantConditions.unsupported();
        }
    }

    public void remove() {
        ConstantConditions.unsupported();
    }
}
