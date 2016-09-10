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

import com.github.dm.jrt.core.InvocationExecution.ExecutionObserver;
import com.github.dm.jrt.core.InvocationExecution.InputData;
import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.BackoffBuilder;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.LocalValue;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.core.util.UnitDuration.Condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.Backoff.NO_DELAY;
import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;
import static com.github.dm.jrt.core.util.UnitDuration.zero;

/**
 * Default implementation of an invocation channel.
 * <p>
 * Created by davide-maestroni on 06/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class InvocationChannel<IN, OUT> implements Channel<IN, OUT> {

    private static final Runner sSyncRunner = Runners.syncRunner();

    private final IdentityHashMap<Channel<?, ? extends IN>, Void> mBoundChannels =
            new IdentityHashMap<Channel<?, ? extends IN>, Void>();

    private final InvocationExecution<IN, OUT> mExecution;

    private final Condition mHasInputs;

    private final Backoff mInputBackoff;

    private final LocalValue<UnitDuration> mInputDelay;

    private final LocalValue<OrderType> mInputOrder;

    private final NestedQueue<IN> mInputQueue;

    private final Logger mLogger;

    private final int mMaxInput;

    private final Object mMutex = new Object();

    private final ResultChannel<OUT> mResultChanel;

    private final Runner mRunner;

    private RoutineException mAbortException;

    private int mInputCount;

    private boolean mIsPendingExecution;

    private boolean mIsWaitingInput;

    private int mPendingExecutionCount;

    private InputChannelState mState;

    /**
     * Constructor.
     *
     * @param configuration    the invocation configuration.
     * @param manager          the invocation manager.
     * @param invocationRunner the runner instance used to execute the invocation.
     * @param resultRunner     the runner instance used to deliver the results.
     * @param logger           the logger instance.
     */
    InvocationChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationManager<IN, OUT> manager,
            @NotNull final Runner invocationRunner, @NotNull final Runner resultRunner,
            @NotNull final Logger logger) {
        mLogger = logger.subContextLogger(this);
        mRunner = ConstantConditions.notNull("invocation runner", invocationRunner);
        mInputOrder = new LocalValue<OrderType>(
                configuration.getInputOrderTypeOrElse(OrderType.UNSORTED));
        mInputBackoff = configuration.getInputBackoffOrElse(BackoffBuilder.noDelay());
        mMaxInput = configuration.getInputMaxSizeOrElse(Integer.MAX_VALUE);
        mInputDelay = new LocalValue<UnitDuration>(zero());
        mInputQueue = new NestedQueue<IN>() {

            @Override
            public void close() {
                // Preventing closing
            }
        };
        final Backoff backoff = mInputBackoff;
        mHasInputs = (configuration.getInputBackoffOrElse(null) != null) ? new Condition() {

            public boolean isTrue() {
                return (backoff.getDelay(mInputCount) == NO_DELAY) || (mAbortException != null);
            }
        } : new Condition() {

            public boolean isTrue() {
                return true;
            }
        };
        mResultChanel =
                new ResultChannel<OUT>(configuration.outputConfigurationBuilder().configured(),
                        new AbortHandler() {

                            public void onAbort(@NotNull final RoutineException reason,
                                    final long delay, @NotNull final TimeUnit timeUnit) {
                                final Execution execution;
                                synchronized (mMutex) {
                                    execution = mState.onHandlerAbort(reason, delay, timeUnit);
                                }

                                if (execution != null) {
                                    mRunner.run(execution, delay, timeUnit);

                                } else {
                                    // Make sure the invocation is properly recycled
                                    mRunner.run(new Execution() {

                                        public void run() {
                                            mExecution.recycle(reason);
                                            mResultChanel.close(reason);
                                        }
                                    }, 0, TimeUnit.MILLISECONDS);
                                }
                            }
                        }, resultRunner, logger);
        mExecution = new InvocationExecution<IN, OUT>(manager, new DefaultExecutionObserver(),
                mResultChanel, logger);
        mState = new InputChannelState();
    }

    public boolean abort() {
        return abort(null);
    }

    public boolean abort(@Nullable final Throwable reason) {
        final UnitDuration delay = mInputDelay.get();
        final boolean isAbort;
        final Execution execution;
        synchronized (mMutex) {
            final InputChannelState state = mState;
            isAbort = state.abortOutput();
            execution = state.abortInvocation(delay, reason);
        }

        final boolean needsAbort = (execution != null);
        if (needsAbort) {
            mRunner.run(execution, delay.value, delay.unit);
        }

        if (isAbort) {
            return mResultChanel.abort(reason);
        }

        return needsAbort && delay.isZero();
    }

    @NotNull
    public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        return after(fromUnit(delay, timeUnit));
    }

    @NotNull
    public Channel<IN, OUT> after(@NotNull final UnitDuration delay) {
        mInputDelay.set(ConstantConditions.notNull("input delay", delay));
        mResultChanel.after(delay);
        return this;
    }

    @NotNull
    public List<OUT> all() {
        return mResultChanel.all();
    }

    @NotNull
    public Channel<IN, OUT> allInto(@NotNull final Collection<? super OUT> results) {
        mResultChanel.allInto(results);
        return this;
    }

    @NotNull
    public <AFTER> Channel<? super OUT, AFTER> bind(
            @NotNull final Channel<? super OUT, AFTER> channel) {
        return mResultChanel.bind(channel);
    }

    @NotNull
    public Channel<IN, OUT> bind(@NotNull final ChannelConsumer<? super OUT> consumer) {
        mResultChanel.bind(consumer);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> close() {
        final UnitDuration delay = mInputDelay.get();
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.onClose(delay);
        }

        if (execution != null) {
            mRunner.run(execution, delay.value, delay.unit);
        }

        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort() {
        mResultChanel.eventuallyAbort();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyAbort(@Nullable final Throwable reason) {
        mResultChanel.eventuallyAbort(reason);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyContinue() {
        mResultChanel.eventuallyContinue();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyFail() {
        mResultChanel.eventuallyFail();
        return this;
    }

    @NotNull
    public Iterator<OUT> expiringIterator() {
        return mResultChanel.expiringIterator();
    }

    public boolean getComplete() {
        return mResultChanel.getComplete();
    }

    @Nullable
    public RoutineException getError() {
        return mResultChanel.getError();
    }

    public boolean hasNext() {
        return mResultChanel.hasNext();
    }

    public OUT next() {
        return mResultChanel.next();
    }

    public int inputCount() {
        synchronized (mMutex) {
            return mState.inputCount();
        }
    }

    public boolean isBound() {
        return mResultChanel.isBound();
    }

    public boolean isEmpty() {
        return (inputCount() == 0) && (outputCount() == 0);
    }

    public boolean isOpen() {
        synchronized (mMutex) {
            return mState.isChannelOpen();
        }
    }

    @NotNull
    public List<OUT> next(final int count) {
        return mResultChanel.next(count);
    }

    public OUT nextOrElse(final OUT output) {
        return mResultChanel.nextOrElse(output);
    }

    @NotNull
    public Channel<IN, OUT> now() {
        return after(zero());
    }

    public int outputCount() {
        return mResultChanel.outputCount();
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        final ChannelConsumer<IN> consumer;
        synchronized (mMutex) {
            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {
            try {
                channel.bind(consumer);

            } catch (final IllegalStateException e) {
                synchronized (mMutex) {
                    mState.onBindFailure();
                }

                throw e;
            }
        }

        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {
        final UnitDuration delay = mInputDelay.get();
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.pass(inputs, delay);
        }

        if (execution != null) {
            mRunner.run(execution, delay.value, delay.unit);
        }

        synchronized (mMutex) {
            if (!mHasInputs.isTrue()) {
                waitInputs();
            }
        }

        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN input) {
        final UnitDuration delay = mInputDelay.get();
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.pass(input, delay);
        }

        if (execution != null) {
            mRunner.run(execution, delay.value, delay.unit);
        }

        synchronized (mMutex) {
            if (!mHasInputs.isTrue()) {
                waitInputs();
            }
        }

        return this;
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final IN... inputs) {
        final UnitDuration delay = mInputDelay.get();
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.pass(inputs, delay);
        }

        if (execution != null) {
            mRunner.run(execution, delay.value, delay.unit);
        }

        synchronized (mMutex) {
            if (!mHasInputs.isTrue()) {
                waitInputs();
            }
        }

        return this;
    }

    public int size() {
        return inputCount() + outputCount();
    }

    @NotNull
    public Channel<IN, OUT> skipNext(final int count) {
        mResultChanel.skipNext(count);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> sorted() {
        mInputOrder.set(OrderType.SORTED);
        return this;
    }

    public void throwError() {
        mResultChanel.throwError();
    }

    @NotNull
    public Channel<IN, OUT> unsorted() {
        mInputOrder.set(OrderType.UNSORTED);
        return this;
    }

    public Iterator<OUT> iterator() {
        return mResultChanel.iterator();
    }

    public void remove() {
        mResultChanel.remove();
    }

    private void checkMaxSize() {
        if (mInputCount > mMaxInput) {
            throw new InputDeadlockException(
                    "maximum input channel size has been exceeded: " + mMaxInput);
        }
    }

    private void internalAbort(@NotNull final RoutineException abortException) {
        mAbortException = abortException;
        mRunner.cancel(mExecution);
        mInputCount = 0;
        mInputQueue.clear();
    }

    private void waitInputs() {
        final long delay = mInputBackoff.getDelay(mInputCount);
        if ((delay > 0) && mRunner.isExecutionThread()) {
            throw new InputDeadlockException(
                    "cannot wait on the invocation runner thread: " + Thread.currentThread()
                            + "\nTry employing a different runner than: " + mRunner);
        }

        try {
            mIsWaitingInput = true;
            if (!UnitDuration.waitTrue(delay, TimeUnit.MILLISECONDS, mMutex, mHasInputs)) {
                mLogger.dbg("timeout while waiting for room in the input channel [%s %s]", delay,
                        TimeUnit.MILLISECONDS);
            }

        } catch (final InterruptedException e) {
            throw new InvocationInterruptedException(e);

        } finally {
            mIsWaitingInput = false;
        }
    }

    /**
     * The invocation execution has been aborted.
     */
    private class AbortChannelState extends CompleteChannelState {

        @Override
        boolean abortOutput() {
            mLogger.dbg("avoiding aborting result channel since invocation is aborting");
            return false;
        }

        @Nullable
        @Override
        Execution delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            mLogger.dbg("avoiding closing result channel after delay since invocation is aborted");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue, final long delay,
                @NotNull final TimeUnit timeUnit) {
            throw consumerException();
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                @NotNull final OrderType orderType, final long delay,
                @NotNull final TimeUnit timeUnit) {
            throw consumerException();
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mLogger.dbg(reason, "avoiding aborting result channel since invocation is aborted");
            return null;
        }

        @NotNull
        private RoutineException consumerException() {
            final RoutineException abortException = mAbortException;
            mLogger.dbg(abortException, "consumer abort exception");
            return abortException;
        }

        @NotNull
        private RoutineException exception() {
            final RoutineException abortException = mAbortException;
            mLogger.dbg(abortException, "abort exception");
            throw abortException;
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Nullable
        @Override
        ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends IN> inputs,
                @NotNull final UnitDuration delay) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN input, @NotNull final UnitDuration delay) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN[] inputs, @NotNull final UnitDuration delay) {
            throw exception();
        }
    }

    /**
     * Implementation of an execution handling the abortion of the result channel.
     */
    private class AbortResultExecution implements Execution {

        private final Throwable mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private AbortResultExecution(@NotNull final Throwable reason) {
            mAbortException = reason;
        }

        public void run() {
            mResultChanel.close(mAbortException);
        }
    }

    /**
     * The channel is closed and no more inputs are expected.
     */
    private class ClosedChannelState extends InputChannelState {

        @Nullable
        @Override
        Execution abortInvocation(@NotNull final UnitDuration delay,
                @Nullable final Throwable reason) {
            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        boolean abortOutput() {
            mLogger.dbg("aborting result channel");
            return true;
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {
            if (mPendingExecutionCount <= 0) {
                mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
                return null;
            }

            return super.delayedAbortInvocation(reason);
        }

        @Nullable
        @Override
        Execution delayedClose() {
            mLogger.dbg("avoiding closing result channel after delay since already closed");
            return null;
        }

        @NotNull
        private IllegalStateException exception() {
            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution onClose(@NotNull final UnitDuration delay) {
            return null;
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends IN> inputs,
                @NotNull final UnitDuration delay) {
            throw exception();
        }

        @Override
        boolean isChannelOpen() {
            return false;
        }

        @Nullable
        @Override
        ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN input, @NotNull final UnitDuration delay) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN[] inputs, @NotNull final UnitDuration delay) {
            throw exception();
        }

        @Override
        public boolean onConsumeComplete() {
            return (mPendingExecutionCount <= 0);
        }
    }

    /**
     * The invocation is complete.
     */
    private class CompleteChannelState extends ClosedChannelState {

        @NotNull
        private IllegalStateException exception() {
            mLogger.err("consumer invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {
            mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
            return null;
        }

        @Nullable
        @Override
        Execution delayedConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error) {
            mLogger.dbg(error,
                    "avoiding aborting consumer after delay since invocation has completed");
            return null;
        }

        @Nullable
        @Override
        Execution delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            mLogger.dbg(
                    "avoiding closing result channel after delay since invocation has completed");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue, final long delay,
                @NotNull final TimeUnit timeUnit) {
            throw exception();
        }

        @Nullable
        @Override
        Execution onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mLogger.wrn(error, "avoiding aborting consumer since invocation has completed");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                @NotNull final OrderType orderType, final long delay,
                @NotNull final TimeUnit timeUnit) {
            throw exception();
        }

        @Override
        public boolean onConsumeComplete() {
            return false;
        }

        @Override
        public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
            return onNextInput(inputData);
        }

        @Override
        public void onInvocationComplete() {
        }

        @Override
        public boolean onNextInput(@NotNull final InputData<IN> inputData) {
            mLogger.wrn("avoiding consuming input since invocation has completed [#%d]",
                    mPendingExecutionCount);
            return false;
        }

        @Nullable
        @Override
        Execution delayedInput(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
            mLogger.wrn("avoiding delayed input execution since invocation has completed: %s",
                    input);
            return null;
        }

        @Nullable
        @Override
        Execution delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {
            mLogger.wrn("avoiding delayed input execution since invocation has completed: %s",
                    inputs);
            return null;
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mLogger.dbg(reason, "aborting result channel");
            return new AbortResultExecution(reason);
        }
    }

    /**
     * Default implementation of an channel consumer pushing the data into the input queue.
     */
    private class DefaultChannelConsumer implements ChannelConsumer<IN> {

        private final Channel<?, ? extends IN> mChannel;

        private final long mDelay;

        private final TimeUnit mDelayUnit;

        private final OrderType mOrderType;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param channel the bound channel.
         */
        private DefaultChannelConsumer(@NotNull final Channel<?, ? extends IN> channel) {
            final UnitDuration delay = mInputDelay.get();
            mDelay = delay.value;
            mDelayUnit = delay.unit;
            final OrderType order = (mOrderType = mInputOrder.get());
            mQueue = (order == OrderType.SORTED) ? mInputQueue.addNested() : mInputQueue;
            mChannel = channel;
        }

        public void onComplete() {
            final long delay = mDelay;
            final TimeUnit timeUnit = mDelayUnit;
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerComplete(mChannel, mQueue, delay, timeUnit);
            }

            if (execution != null) {
                mRunner.run(execution, delay, timeUnit);
            }
        }

        public void onError(@NotNull final RoutineException error) {
            final long delay = mDelay;
            final TimeUnit timeUnit = mDelayUnit;
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerError(mChannel, error, delay, timeUnit);
            }

            if (execution != null) {
                mRunner.run(execution, delay, timeUnit);
            }
        }

        public void onOutput(final IN output) {
            final long delay = mDelay;
            final TimeUnit timeUnit = mDelayUnit;
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerOutput(output, mQueue, mOrderType, delay, timeUnit);
            }

            if (execution != null) {
                mRunner.run(execution, delay, timeUnit);
            }

            synchronized (mMutex) {
                if (!mHasInputs.isTrue()) {
                    waitInputs();
                }
            }
        }
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultExecutionObserver implements ExecutionObserver<IN> {

        @NotNull
        public RoutineException getAbortException() {
            synchronized (mMutex) {
                return mState.getAbortException();
            }
        }

        public void onAbortComplete() {
            final Throwable abortException;
            final List<Channel<?, ? extends IN>> channels;
            synchronized (mMutex) {
                abortException = mAbortException;
                final IdentityHashMap<Channel<?, ? extends IN>, Void> boundChannels =
                        mBoundChannels;
                mLogger.dbg(abortException, "aborting bound channels [%d]", boundChannels.size());
                channels = new ArrayList<Channel<?, ? extends IN>>(boundChannels.keySet());
                boundChannels.clear();
            }

            for (final Channel<?, ? extends IN> channel : channels) {
                channel.abort(abortException);
            }
        }

        public boolean onConsumeComplete() {
            synchronized (mMutex) {
                return mState.onConsumeComplete();
            }
        }

        public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
            synchronized (mMutex) {
                return mState.onFirstInput(inputData);
            }
        }

        public void onInvocationComplete() {
            synchronized (mMutex) {
                mState.onInvocationComplete();
            }
        }

        public boolean onNextInput(@NotNull final InputData<IN> inputData) {
            synchronized (mMutex) {
                return mState.onNextInput(inputData);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed abortion.
     */
    private class DelayedAbortExecution implements Execution {

        private final RoutineException mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private DelayedAbortExecution(@NotNull final RoutineException reason) {
            mAbortException = reason;
        }

        public void run() {
            final RoutineException abortException = mAbortException;
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedAbortInvocation(abortException);
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }

            mResultChanel.abort(abortException);
        }
    }

    /**
     * Implementation of an execution handling a delayed close.
     */
    private class DelayedCloseExecution implements Execution {

        /**
         * Constructor.
         */
        private DelayedCloseExecution() {
        }

        public void run() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedClose();
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed consumer completion.
     */
    private class DelayedConsumerCompleteExecution implements Execution {

        private final Channel<?, ? extends IN> mChannel;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param channel the channel bound to this one.
         * @param queue   the output queue.
         */
        private DelayedConsumerCompleteExecution(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            mChannel = channel;
            mQueue = queue;
        }

        public void run() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedConsumerComplete(mChannel, mQueue);
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed consumer error.
     */
    private class DelayedConsumerErrorExecution implements Execution {

        private final RoutineException mAbortException;

        private final Channel<?, ? extends IN> mChannel;

        /**
         * Constructor.
         *
         * @param channel the channel bound to this one.
         * @param error   the abortion error.
         */
        private DelayedConsumerErrorExecution(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error) {
            mChannel = channel;
            mAbortException = error;
        }

        public void run() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedConsumerError(mChannel, mAbortException);
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed input.
     */
    private class DelayedInputExecution implements Execution {

        private final IN mInput;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param queue the input queue.
         * @param input the input.
         */
        private DelayedInputExecution(@NotNull final NestedQueue<IN> queue,
                @Nullable final IN input) {
            mQueue = queue;
            mInput = input;
        }

        public void run() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedInput(mQueue, mInput);
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed input of a list of data.
     */
    private class DelayedListInputExecution implements Execution {

        private final ArrayList<IN> mInputs;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the input queue.
         * @param inputs the list of input data.
         */
        private DelayedListInputExecution(@NotNull final NestedQueue<IN> queue,
                final ArrayList<IN> inputs) {
            mInputs = inputs;
            mQueue = queue;
        }

        public void run() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedInputs(mQueue, mInputs);
            }

            if (execution != null) {
                sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Invocation channel internal state (using "state" design pattern).
     */
    private class InputChannelState implements ExecutionObserver<IN> {

        /**
         * Called when the invocation is aborted.
         *
         * @param delay  the input delay.
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution abortInvocation(@NotNull final UnitDuration delay,
                @Nullable final Throwable reason) {
            final RoutineException abortException = AbortException.wrapIfNeeded(reason);
            if (delay.isZero()) {
                mLogger.dbg(reason, "aborting channel");
                internalAbort(abortException);
                mState = new AbortChannelState();
                mMutex.notifyAll();
                return mExecution.abort();
            }

            return new DelayedAbortExecution(abortException);
        }

        /**
         * Called when the invocation is aborted.
         *
         * @return whether the output channel has to be aborted.
         */
        boolean abortOutput() {
            return false;
        }

        /**
         * Called when the invocation is aborted after a delay.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {
            mLogger.dbg(reason, "aborting channel after delay");
            internalAbort(reason);
            mState = new AbortChannelState();
            mMutex.notifyAll();
            return mExecution.abort();
        }

        /**
         * Called when the channel is closed after a delay.
         *
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedClose() {
            mLogger.dbg("closing input channel after delay");
            mState = new ClosedChannelState();
            return mExecution;
        }

        /**
         * Called when the feeding consumer completes after a delay.
         *
         * @param channel the bound channel.
         * @param queue   the input queue.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            mLogger.dbg("closing consumer after delay");
            mBoundChannels.remove(channel);
            queue.close();
            if (!mIsPendingExecution) {
                mIsPendingExecution = true;
                return mExecution;
            }

            --mPendingExecutionCount;
            return null;
        }

        /**
         * Called when the feeding consumer receives an error after a delay.
         *
         * @param channel the bound channel.
         * @param error   the error.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error) {
            mLogger.dbg(error, "aborting consumer after delay");
            mBoundChannels.remove(channel);
            internalAbort(error);
            mState = new AbortChannelState();
            return mExecution.abort();
        }

        /**
         * Called when an input is passed to the invocation after a delay.
         *
         * @param queue the input queue.
         * @param input the input.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedInput(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
            mLogger.dbg("delayed input execution: %s", input);
            queue.add(input);
            queue.close();
            return mExecution;
        }

        /**
         * Called when some inputs are passed to the invocation after a delay.
         *
         * @param queue  the input queue.
         * @param inputs the inputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {
            mLogger.dbg("delayed input execution: %s", inputs);
            queue.addAll(inputs);
            queue.close();
            return mExecution;
        }

        /**
         * Returns the number of inputs still to be processed.
         *
         * @return the input count.
         */
        int inputCount() {
            return mInputCount;
        }

        /**
         * Called to know if the channel is open.
         *
         * @return whether the channel is open.
         */
        boolean isChannelOpen() {
            return true;
        }

        /**
         * Called when the binding of channel fails.
         */
        void onBindFailure() {
            --mPendingExecutionCount;
        }

        /**
         * Called when the inputs are complete.
         *
         * @param delay the input delay value.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onClose(@NotNull final UnitDuration delay) {
            if (delay.isZero()) {
                mLogger.dbg("closing input channel");
                mState = new ClosedChannelState();
                if (!mIsPendingExecution) {
                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingExecutionCount;
            return new DelayedCloseExecution();
        }

        /**
         * Called when the feeding consumer completes.
         *
         * @param channel  the bound channel.
         * @param queue    the input queue.
         * @param delay    the input delay value.
         * @param timeUnit the input delay unit.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue, final long delay,
                @NotNull final TimeUnit timeUnit) {
            if (delay == 0) {
                mLogger.dbg("closing consumer");
                mBoundChannels.remove(channel);
                queue.close();
                if (!mIsPendingExecution) {
                    mIsPendingExecution = true;
                    return mExecution;
                }

                --mPendingExecutionCount;
                return null;
            }

            return new DelayedConsumerCompleteExecution(channel, queue);
        }

        /**
         * Called when the feeding consumer receives an error.
         *
         * @param channel  the bound channel.
         * @param error    the error.
         * @param delay    the input delay value.
         * @param timeUnit the input delay unit.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error, final long delay,
                @NotNull final TimeUnit timeUnit) {
            if (delay == 0) {
                mLogger.dbg(error, "aborting consumer");
                mBoundChannels.remove(channel);
                internalAbort(error);
                mState = new AbortChannelState();
                return mExecution.abort();
            }

            return new DelayedConsumerErrorExecution(channel, error);
        }

        /**
         * Called when the feeding consumer receives an output.
         *
         * @param input     the input.
         * @param queue     the input queue.
         * @param orderType the input order type.
         * @param delay     the input delay value.
         * @param timeUnit  the input delay unit.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                @NotNull final OrderType orderType, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mLogger.dbg("consumer input [#%d+1]: %s [%d %s]", mInputCount, input, delay, timeUnit);
            ++mInputCount;
            checkMaxSize();
            if (delay == 0) {
                queue.add(input);
                if (!mIsPendingExecution) {
                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingExecutionCount;
            return new DelayedInputExecution(
                    (orderType != OrderType.UNSORTED) ? queue.addNested() : queue, input);
        }

        /**
         * Called when the invocation is aborted through the registered handler.
         *
         * @param reason   the reason of the abortion.
         * @param delay    the input delay value.
         * @param timeUnit the input delay unit.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onHandlerAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {
            final RoutineException abortException = AbortException.wrapIfNeeded(reason);
            if (delay == 0) {
                mLogger.dbg(reason, "aborting channel");
                internalAbort(abortException);
                mState = new AbortChannelState();
                mMutex.notifyAll();
                return mExecution.abort();
            }

            return new DelayedAbortExecution(abortException);
        }

        /**
         * Called when a channel is passed to the invocation.
         *
         * @param channel the channel instance.
         * @return the channel consumer to bind or null.
         */
        @Nullable
        ChannelConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            if (channel == null) {
                mLogger.wrn("passing null channel");
                return null;
            }

            ++mPendingExecutionCount;
            mBoundChannels.put(channel, null);
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultChannelConsumer(channel);
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param inputs the inputs.
         * @param delay  the input delay.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final Iterable<? extends IN> inputs,
                @NotNull final UnitDuration delay) {
            if (inputs == null) {
                mLogger.wrn("passing null iterable");
                return null;
            }

            final ArrayList<IN> list = new ArrayList<IN>();
            for (final IN input : inputs) {
                list.add(input);
            }

            final int size = list.size();
            mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mInputCount, size, inputs, delay);
            mInputCount += size;
            checkMaxSize();
            if (delay.isZero()) {
                mInputQueue.addAll(list);
                if (!mIsPendingExecution) {
                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingExecutionCount;
            return new DelayedListInputExecution(
                    (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested()
                            : mInputQueue, list);
        }

        /**
         * Called when an input is passed to the invocation.
         *
         * @param input the input.
         * @param delay the input delay.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final IN input, @NotNull final UnitDuration delay) {
            mLogger.dbg("passing input [#%d+1]: %s [%s]", mInputCount, input, delay);
            ++mInputCount;
            checkMaxSize();
            if (delay.isZero()) {
                mInputQueue.add(input);
                if (!mIsPendingExecution) {
                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingExecutionCount;
            return new DelayedInputExecution(
                    (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested()
                            : mInputQueue, input);
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param inputs the inputs.
         * @param delay  the input delay.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final IN[] inputs, @NotNull final UnitDuration delay) {
            if (inputs == null) {
                mLogger.wrn("passing null input array");
                return null;
            }

            final int size = inputs.length;
            mLogger.dbg("passing array [#%d+%d]: %s [%s]", mInputCount, size, inputs, delay);
            mInputCount += size;
            checkMaxSize();
            final ArrayList<IN> list = new ArrayList<IN>(size);
            Collections.addAll(list, inputs);
            if (delay.isZero()) {
                mInputQueue.addAll(list);
                if (!mIsPendingExecution) {
                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingExecutionCount;
            return new DelayedListInputExecution(
                    (mInputOrder.get() != OrderType.UNSORTED) ? mInputQueue.addNested()
                            : mInputQueue, list);
        }

        @NotNull
        public RoutineException getAbortException() {
            return InvocationException.wrapIfNeeded(mAbortException);
        }

        public void onAbortComplete() {
        }

        public boolean onConsumeComplete() {
            return false;
        }

        public boolean onFirstInput(@NotNull final InputData<IN> inputData) {
            --mPendingExecutionCount;
            mIsPendingExecution = false;
            return onNextInput(inputData);
        }

        public void onInvocationComplete() {
            mState = new CompleteChannelState();
        }

        public boolean onNextInput(@NotNull final InputData<IN> inputData) {
            final NestedQueue<IN> inputQueue = mInputQueue;
            if (!inputQueue.isEmpty()) {
                final IN input = inputQueue.removeFirst();
                mLogger.dbg("consuming input [#%d]: %s", mInputCount, input);
                final int inputCount = --mInputCount;
                if (mIsWaitingInput && (mInputBackoff.getDelay(inputCount) == NO_DELAY)) {
                    mMutex.notifyAll();
                }

                inputData.data = input;
                return true;
            }

            return false;
        }
    }
}
