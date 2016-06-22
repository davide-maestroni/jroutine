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

import com.github.dm.jrt.core.InvocationExecution.InputIterator;
import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputConsumer;
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
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
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

    private final SimpleQueue<Execution> mExecutionQueue = new SimpleQueue<Execution>();

    private final Condition mHasInputs;

    private final Backoff mInputBackoff;

    private final LocalValue<UnitDuration> mInputDelay;

    private final int mInputLimit;

    private final LocalValue<OrderType> mInputOrder;

    private final NestedQueue<IN> mInputQueue;

    private final Logger mLogger;

    private final int mMaxInput;

    private final Object mMutex = new Object();

    private final ResultChannel<OUT> mResultChanel;

    private final Runner mRunner;

    private RoutineException mAbortException;

    private int mInputCount;

    private boolean mIsConsuming;

    private boolean mIsPendingExecution;

    private int mPendingExecutionCount;

    private InputChannelState mState;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param manager       the invocation manager.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     */
    InvocationChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationManager<IN, OUT> manager, @NotNull final Runner runner,
            @NotNull final Logger logger) {
        mLogger = logger.subContextLogger(this);
        mRunner = runner;
        mInputOrder = new LocalValue<OrderType>(
                configuration.getInputOrderTypeOrElse(OrderType.BY_DELAY));
        mInputLimit = configuration.getInputLimitOrElse(Integer.MAX_VALUE);
        mInputBackoff = configuration.getInputBackoffOrElse(Backoffs.zeroDelay());
        mMaxInput = configuration.getInputMaxSizeOrElse(Integer.MAX_VALUE);
        mInputDelay = new LocalValue<UnitDuration>(zero());
        mInputQueue = new NestedQueue<IN>() {

            @Override
            public void close() {
                // Preventing closing
            }
        };
        final int inputLimit = mInputLimit;
        mHasInputs = new Condition() {

            public boolean isTrue() {
                return (mInputCount <= inputLimit) || (mAbortException != null);
            }
        };
        mResultChanel = new ResultChannel<OUT>(configuration.outputConfigurationBuilder().apply(),
                new AbortHandler() {

                    public void onAbort(@NotNull final RoutineException reason, final long delay,
                            @NotNull final TimeUnit timeUnit) {
                        final Execution execution;
                        synchronized (mMutex) {
                            execution = mState.onHandlerAbort(reason);
                        }

                        if (execution != null) {
                            runExecution(execution, delay, timeUnit);

                        } else {
                            // Make sure the invocation is properly recycled
                            mExecution.recycle();
                            mResultChanel.close(reason);
                        }
                    }
                }, runner, logger);
        mExecution =
                new InvocationExecution<IN, OUT>(manager, new DefaultInputIterator(), mResultChanel,
                        logger);
        mState = new InputChannelState();
    }

    public boolean abort() {
        return abort(null);
    }

    public boolean abort(@Nullable final Throwable reason) {
        final UnitDuration delay = mInputDelay.get();
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.abortInvocation(delay, reason);
        }

        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
            return true;
        }

        synchronized (mMutex) {
            return mState.abortOutput(reason);
        }
    }

    @NotNull
    public Channel<IN, OUT> after(@NotNull final UnitDuration delay) {
        mInputDelay.set(ConstantConditions.notNull("input delay", delay));
        mResultChanel.after(delay);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {
        return after(fromUnit(delay, timeUnit));
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
    public <CHANNEL extends Channel<? super OUT, ?>> CHANNEL bind(@NotNull final CHANNEL channel) {
        return mResultChanel.bind(channel);
    }

    @NotNull
    public Channel<IN, OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {
        mResultChanel.bind(consumer);
        return this;
    }

    @NotNull
    public Channel<IN, OUT> close() {
        final Execution execution;
        synchronized (mMutex) {
            execution = mState.onClose();
        }

        final UnitDuration delay = mInputDelay.get();
        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
        }

        return this;
    }

    @NotNull
    public Iterator<OUT> eventualIterator() {
        return mResultChanel.eventualIterator();
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
    public Channel<IN, OUT> eventuallyBreak() {
        mResultChanel.eventuallyBreak();
        return this;
    }

    @NotNull
    public Channel<IN, OUT> eventuallyFail() {
        mResultChanel.eventuallyFail();
        return this;
    }

    @Nullable
    public RoutineException getError() {
        return mResultChanel.getError();
    }

    public boolean hasCompleted() {
        return mResultChanel.hasCompleted();
    }

    public boolean hasNext() {
        return mResultChanel.hasNext();
    }

    public OUT next() {
        return mResultChanel.next();
    }

    @NotNull
    public Channel<IN, OUT> immediately() {
        return after(zero());
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

    public int outputCount() {
        return mResultChanel.outputCount();
    }

    @NotNull
    public Channel<IN, OUT> pass(@Nullable final Channel<?, ? extends IN> channel) {
        final OutputConsumer<IN> consumer;
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
            execution = mState.pass(delay, inputs);
        }

        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
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
            execution = mState.pass(delay, input);
        }

        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
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
            execution = mState.pass(delay, inputs);
        }

        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
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
    public Channel<IN, OUT> sortedByCall() {
        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_CALL);
        }

        return this;
    }

    @NotNull
    public Channel<IN, OUT> sortedByDelay() {
        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_DELAY);
        }

        return this;
    }

    public void throwError() {
        mResultChanel.throwError();
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

    private void forceExecution(@NotNull final Execution execution) {
        synchronized (mMutex) {
            if (mIsConsuming) {
                mExecutionQueue.add(execution);
                return;
            }
        }

        sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
    }

    private void internalAbort(@NotNull final RoutineException abortException) {
        mAbortException = abortException;
        mRunner.cancel(mExecution);
    }

    private void runExecution(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {
        if (delay > 0) {
            mRunner.run(execution, delay, timeUnit);

        } else {
            synchronized (mMutex) {
                if (mIsConsuming) {
                    mExecutionQueue.add(execution);
                    return;
                }
            }

            mRunner.run(execution, delay, timeUnit);
        }
    }

    private void waitInputs() {
        final long delay = mInputBackoff.getDelay(mInputCount - mInputLimit);
        if ((delay > 0) && mRunner.isExecutionThread()) {
            throw new InputDeadlockException(
                    "cannot wait on the invocation runner thread: " + Thread.currentThread()
                            + "\nTry employing a different runner than: " + mRunner);
        }

        try {
            if (!UnitDuration.waitTrue(delay, TimeUnit.MILLISECONDS, mMutex, mHasInputs)) {
                mLogger.dbg("timeout while waiting for room in the input channel [%s %s]", delay,
                        TimeUnit.MILLISECONDS);
            }

        } catch (final InterruptedException e) {
            throw new InvocationInterruptedException(e);
        }
    }

    /**
     * The invocation execution has been aborted.
     */
    private class AbortChannelState extends CompleteChannelState {

        @Override
        public int inputCount() {
            return 0;
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            throw consumerException();
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {
            throw consumerException();
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@NotNull final RoutineException reason) {
            mLogger.wrn(reason, "avoiding aborting result channel since invocation is aborted");
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
        boolean abortOutput(@Nullable final Throwable reason) {
            mLogger.wrn("avoiding aborting result channel since invocation is aborting");
            return false;
        }

        @Nullable
        @Override
        OutputConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay,
                @Nullable final Iterable<? extends IN> inputs) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN input) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN... inputs) {
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

        @NotNull
        private IllegalStateException exception() {
            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution abortInvocation(@NotNull final UnitDuration delay,
                @Nullable final Throwable reason) {
            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {
            if ((mPendingExecutionCount <= 0) && !mIsConsuming) {
                mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
                return null;
            }

            return super.delayedAbortInvocation(reason);
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay,
                @Nullable final Iterable<? extends IN> inputs) {
            throw exception();
        }

        @Override
        boolean isChannelOpen() {
            return false;
        }

        @Nullable
        @Override
        OutputConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN input) {
            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN... inputs) {
            throw exception();
        }

        @Nullable
        @Override
        Execution onClose() {
            return null;
        }

        @Override
        public boolean onConsumeComplete() {
            mIsConsuming = false;
            return (mPendingExecutionCount <= 0);
        }
    }

    /**
     * The invocation is complete.
     */
    private class CompleteChannelState extends ClosedChannelState {

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {
            mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
            return null;
        }

        @NotNull
        private IllegalStateException exception() {
            mLogger.dbg("consumer invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            throw exception();
        }

        @Nullable
        @Override
        Execution onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error) {
            mLogger.wrn(error, "avoiding aborting consumer since channel is closed");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {
            throw exception();
        }

        @Override
        public boolean onConsumeComplete() {
            mIsConsuming = false;
            return false;
        }

        @Override
        public void onInvocationComplete() {
        }

        @Nullable
        @Override
        Execution delayedInput(@NotNull final NestedQueue<IN> queue, @Nullable final IN input) {
            mLogger.dbg("avoiding delayed input execution since channel is closed: %s", input);
            return null;
        }

        @Nullable
        @Override
        Execution delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {
            mLogger.dbg("avoiding delayed input execution since channel is closed: %s", inputs);
            return null;
        }

        @Override
        public boolean hasInput() {
            return false;
        }

        @Override
        public void onConsumeStart() {
            mLogger.wrn("avoiding consuming input since invocation is complete [#%d]",
                    mPendingExecutionCount);
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@NotNull final RoutineException reason) {
            mLogger.dbg(reason, "aborting result channel");
            return new AbortResultExecution(reason);
        }
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<IN> {

        @NotNull
        public RoutineException getAbortException() {
            synchronized (mMutex) {
                return mState.getAbortException();
            }
        }

        public boolean hasInput() {
            synchronized (mMutex) {
                return mState.hasInput();
            }
        }

        @Nullable
        public IN nextInput() {
            synchronized (mMutex) {
                return mState.nextInput();
            }
        }

        public void onAbortComplete() {
            final Throwable abortException;
            final List<Channel<?, ? extends IN>> channels;
            synchronized (mMutex) {
                mInputCount = 0;
                mInputQueue.clear();
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
            Execution execution = null;
            final boolean isComplete;
            synchronized (mMutex) {
                final SimpleQueue<Execution> queue = mExecutionQueue;
                if (!queue.isEmpty()) {
                    execution = queue.removeFirst();
                }

                isComplete = mState.onConsumeComplete();
            }

            if (execution != null) {
                mRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }

            return isComplete;
        }

        public void onConsumeStart() {
            synchronized (mMutex) {
                mState.onConsumeStart();
            }
        }

        public void onInvocationComplete() {
            synchronized (mMutex) {
                mState.onInvocationComplete();
            }
        }
    }

    /**
     * Default implementation of an output consumer pushing the data into the input queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<IN> {

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
        private DefaultOutputConsumer(@NotNull final Channel<?, ? extends IN> channel) {
            final UnitDuration delay = mInputDelay.get();
            mDelay = delay.value;
            mDelayUnit = delay.unit;
            final OrderType order = (mOrderType = mInputOrder.get());
            mQueue = (order == OrderType.BY_CALL) ? mInputQueue.addNested() : mInputQueue;
            mChannel = channel;
        }

        public void onComplete() {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerComplete(mChannel, mQueue);
            }

            if (execution != null) {
                runExecution(execution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void onError(@NotNull final RoutineException error) {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerError(mChannel, error);
            }

            if (execution != null) {
                runExecution(execution, mDelay, mDelayUnit);
            }
        }

        public void onOutput(final IN output) {
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerOutput(output, mQueue, mDelay, mDelayUnit, mOrderType);
            }

            if (execution != null) {
                runExecution(execution, mDelay, mDelayUnit);
            }

            synchronized (mMutex) {
                if (!mHasInputs.isTrue()) {
                    waitInputs();
                }
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
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.delayedAbortInvocation(mAbortException);
            }

            if (execution != null) {
                forceExecution(execution);
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
                forceExecution(execution);
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
                forceExecution(execution);
            }
        }
    }

    /**
     * Invocation channel internal state (using "state" design pattern).
     */
    private class InputChannelState implements InputIterator<IN> {

        /**
         * Returns the number of inputs still to be processed.
         *
         * @return the input count.
         */
        public int inputCount() {
            return mInputCount;
        }

        /**
         * Called when the binding of channel fails.
         */
        public void onBindFailure() {
            --mPendingExecutionCount;
        }

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
         * Called when the invocation is aborted but the channel has been already closed.
         *
         * @param reason the reason of the abortion.
         * @return whether the output channel was aborted.
         */
        boolean abortOutput(@Nullable final Throwable reason) {
            mLogger.dbg("aborting result channel");
            return mResultChanel.abort(reason);
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
         * Called to know if the channel is open.
         *
         * @return whether the channel is open.
         */
        boolean isChannelOpen() {
            return true;
        }

        /**
         * Called when the inputs are complete.
         *
         * @return the execution to run or null.
         */
        @Nullable
        Execution onClose() {
            mLogger.dbg("closing input channel");
            mState = new ClosedChannelState();
            if (!mIsPendingExecution && !mIsConsuming) {
                ++mPendingExecutionCount;
                mIsPendingExecution = true;
                return mExecution;
            }

            return null;
        }

        /**
         * Called when the feeding consumer completes.
         *
         * @param channel the bound channel.
         * @param queue   the input queue.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerComplete(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final NestedQueue<IN> queue) {
            mLogger.dbg("closing consumer");
            mBoundChannels.remove(channel);
            queue.close();
            if (!mIsPendingExecution && !mIsConsuming) {
                mIsPendingExecution = true;
                return mExecution;

            } else {
                --mPendingExecutionCount;
            }

            return null;
        }

        /**
         * Called when the feeding consumer receives an error.
         *
         * @param channel the bound channel.
         * @param error   the error.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerError(@NotNull final Channel<?, ? extends IN> channel,
                @NotNull final RoutineException error) {
            mLogger.dbg(error, "aborting consumer");
            mBoundChannels.remove(channel);
            internalAbort(error);
            mState = new AbortChannelState();
            return mExecution.abort();
        }

        /**
         * Called when the feeding consumer receives an output.
         *
         * @param input     the input.
         * @param queue     the input queue.
         * @param delay     the input delay value.
         * @param timeUnit  the input delay unit.
         * @param orderType the input order type.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {
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
                    (orderType != OrderType.BY_DELAY) ? queue.addNested() : queue, input);
        }

        /**
         * Called when the invocation is aborted through the registered handler.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onHandlerAbort(@NotNull final RoutineException reason) {
            mLogger.dbg(reason, "aborting result channel");
            internalAbort(reason);
            mState = new AbortChannelState();
            mMutex.notifyAll();
            return mExecution.abort();
        }

        /**
         * Called to set the input delivery order.
         *
         * @param orderType the input order type.
         */
        void orderBy(@NotNull final OrderType orderType) {
            mInputOrder.set(ConstantConditions.notNull("order type", orderType));
        }

        /**
         * Called when a channel is passed to the invocation.
         *
         * @param channel the channel instance.
         * @return the output consumer to bind or null.
         */
        @Nullable
        OutputConsumer<IN> pass(@Nullable final Channel<?, ? extends IN> channel) {
            if (channel == null) {
                mLogger.wrn("passing null channel");
                return null;
            }

            ++mPendingExecutionCount;
            mBoundChannels.put(channel, null);
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer(channel);
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param delay  the input delay.
         * @param inputs the inputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@NotNull final UnitDuration delay,
                @Nullable final Iterable<? extends IN> inputs) {
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
                    (mInputOrder.get() != OrderType.BY_DELAY) ? mInputQueue.addNested()
                            : mInputQueue, list);
        }

        /**
         * Called when an input is passed to the invocation.
         *
         * @param delay the input delay.
         * @param input the input.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN input) {
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
                    (mInputOrder.get() != OrderType.BY_DELAY) ? mInputQueue.addNested()
                            : mInputQueue, input);
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param delay  the input delay.
         * @param inputs the inputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@NotNull final UnitDuration delay, @Nullable final IN... inputs) {
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
                    (mInputOrder.get() != OrderType.BY_DELAY) ? mInputQueue.addNested()
                            : mInputQueue, list);
        }

        @NotNull
        public RoutineException getAbortException() {
            return InvocationException.wrapIfNeeded(mAbortException);
        }

        public boolean hasInput() {
            return !mInputQueue.isEmpty();
        }

        @Nullable
        public IN nextInput() {
            final IN input = mInputQueue.removeFirst();
            mLogger.dbg("reading input [#%d]: %s", mInputCount, input);
            final int inputLimit = mInputLimit;
            final int prevInputCount = mInputCount;
            if ((--mInputCount <= inputLimit) && (prevInputCount > inputLimit)) {
                mMutex.notifyAll();
            }

            return input;
        }

        public void onAbortComplete() {
        }

        public boolean onConsumeComplete() {
            mIsConsuming = false;
            return false;
        }

        public void onConsumeStart() {
            mLogger.dbg("consuming input [#%d]", mPendingExecutionCount);
            --mPendingExecutionCount;
            mIsPendingExecution = false;
            mIsConsuming = true;
        }

        public void onInvocationComplete() {
            mState = new CompleteChannelState();
        }
    }
}
