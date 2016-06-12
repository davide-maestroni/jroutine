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
import com.github.dm.jrt.core.InvocationExecution.InputIterator;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.fromUnit;
import static com.github.dm.jrt.core.util.UnitDuration.zero;

/**
 * Default implementation of an invocation input channel.
 * <p>
 * Created by davide-maestroni on 06/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultInvocationChannel<IN, OUT> implements InvocationChannel<IN, OUT> {

    private static final Runner sSyncRunner = Runners.syncRunner();

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

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

    private final DefaultResultChannel<OUT> mResultChanel;

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
    DefaultInvocationChannel(@NotNull final InvocationConfiguration configuration,
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
        mResultChanel = new DefaultResultChannel<OUT>(configuration, new AbortHandler() {

            public void onAbort(@NotNull final RoutineException reason, final long delay,
                    @NotNull final TimeUnit timeUnit) {

                final Execution execution;
                synchronized (mMutex) {
                    execution = mState.onHandlerAbort(reason);
                }

                if (execution != null) {
                    runExecution(execution, delay, timeUnit);

                } else {
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

        return false;
    }

    public boolean isEmpty() {

        return size() == 0;
    }

    public boolean isOpen() {

        synchronized (mMutex) {
            return mState.isChannelOpen();
        }
    }

    public int size() {

        synchronized (mMutex) {
            return mInputCount;
        }
    }

    @NotNull
    public InvocationChannel<IN, OUT> after(@NotNull final UnitDuration delay) {

        synchronized (mMutex) {
            mState.after(delay);
        }

        return this;
    }

    @NotNull
    public InvocationChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @NotNull
    public InvocationChannel<IN, OUT> now() {

        return after(zero());
    }

    @NotNull
    public InvocationChannel<IN, OUT> orderByCall() {

        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_CALL);
        }

        return this;
    }

    @NotNull
    public InvocationChannel<IN, OUT> orderByDelay() {

        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_DELAY);
        }

        return this;
    }

    @NotNull
    public InvocationChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        final OutputConsumer<IN> consumer;
        synchronized (mMutex) {
            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {
            channel.bind(consumer);
        }

        return this;
    }

    @NotNull
    public InvocationChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

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
    public InvocationChannel<IN, OUT> pass(@Nullable final IN input) {

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
    public InvocationChannel<IN, OUT> pass(@Nullable final IN... inputs) {

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
    public OutputChannel<OUT> result() {

        final Execution execution;
        final OutputChannel<OUT> result;
        synchronized (mMutex) {
            final InputChannelState state = mState;
            execution = state.onResult();
            result = state.getOutputChannel();
        }

        final UnitDuration delay = mInputDelay.get();
        if (execution != null) {
            runExecution(execution, delay.value, delay.unit);
        }

        return result;
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

        mInputCount = 0;
        mInputQueue.clear();
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
     * The invocation has been explicitly aborted.
     */
    private class AbortedChannelState extends ExceptionChannelState {

        @NotNull
        @Override
        OutputChannel<OUT> getOutputChannel() {

            throw super.exception();
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
            final List<OutputChannel<?>> channels;
            synchronized (mMutex) {
                abortException = mAbortException;
                mLogger.dbg(abortException, "aborting bound channels [%d]", mBoundChannels.size());
                channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
                mBoundChannels.clear();
            }

            for (final OutputChannel<?> channel : channels) {
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
     * Default implementation of an output consumer pushing the data to consume into the input
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<IN> {

        private final long mDelay;

        private final TimeUnit mDelayUnit;

        private final OrderType mOrderType;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         */
        private DefaultOutputConsumer() {

            final UnitDuration delay = mInputDelay.get();
            mDelay = delay.value;
            mDelayUnit = delay.unit;
            final OrderType order = (mOrderType = mInputOrder.get());
            mQueue = (order == OrderType.BY_CALL) ? mInputQueue.addNested() : mInputQueue;
        }

        public void onComplete() {

            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerComplete(mQueue);
            }

            if (execution != null) {
                runExecution(execution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void onError(@NotNull final RoutineException error) {

            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerError(error);
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
     * Exception thrown during invocation execution.
     */
    private class ExceptionChannelState extends ResultChannelState {

        @Override
        void after(@NotNull final UnitDuration delay) {

            throw exception();
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

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final NestedQueue<IN> queue) {

            throw consumerException();
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
        void orderBy(@NotNull final OrderType orderType) {

            throw exception();
        }

        @Nullable
        @Override
        OutputConsumer<IN> pass(@Nullable final OutputChannel<? extends IN> channel) {

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

        @NotNull
        @Override
        OutputChannel<OUT> getOutputChannel() {

            mState = new AbortedChannelState();
            final OutputChannel<OUT> outputChannel = mResultChanel.getOutput();
            outputChannel.abort(mAbortException);
            return outputChannel;
        }
    }

    /**
     * Invocation channel internal state (using "state" design pattern).
     */
    private class InputChannelState implements InputIterator<IN> {

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
                mState = new AbortedChannelState();
                mMutex.notifyAll();
                return mExecution.abort();
            }

            return new DelayedAbortExecution(abortException);
        }

        /**
         * Called to set the specified input delay.
         *
         * @param delay the input delay.
         */
        void after(@NotNull final UnitDuration delay) {

            mInputDelay.set(ConstantConditions.notNull("input delay", delay));
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
            mState = new AbortedChannelState();
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
         * Called to get the output channel.
         *
         * @return the output channel.
         */
        @NotNull
        OutputChannel<OUT> getOutputChannel() {

            mState = new OutputChannelState();
            return mResultChanel.getOutput();
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
         * Called when the feeding consumer completes.
         *
         * @param queue the input queue.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerComplete(@NotNull final NestedQueue<IN> queue) {

            mLogger.dbg("closing consumer");
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
         * @param error the error.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerError(@NotNull final RoutineException error) {

            mLogger.dbg(error, "aborting consumer");
            mAbortException = error;
            mState = new ExceptionChannelState();
            mRunner.cancel(mExecution);
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
            mState = new ExceptionChannelState();
            mMutex.notifyAll();
            return mExecution.abort();
        }

        /**
         * Called when the inputs are complete.
         *
         * @return the execution to run or null.
         */
        @Nullable
        Execution onResult() {

            mLogger.dbg("closing input channel");
            if (!mIsPendingExecution && !mIsConsuming) {
                ++mPendingExecutionCount;
                mIsPendingExecution = true;
                return mExecution;
            }

            return null;
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
         * Called when an output channel is passed to the invocation.
         *
         * @param channel the channel instance.
         * @return the output consumer to bind or null.
         */
        @Nullable
        OutputConsumer<IN> pass(@Nullable final OutputChannel<? extends IN> channel) {

            if (channel == null) {
                mLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingExecutionCount;
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer();
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

            mState = new ResultChannelState();
        }
    }

    /**
     * The channel is closed and no more inputs are expected.
     */
    private class OutputChannelState extends InputChannelState {

        @Nullable
        @Override
        Execution abortInvocation(@NotNull final UnitDuration delay,
                @Nullable final Throwable reason) {

            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        void after(@NotNull final UnitDuration delay) {

            throw exception();
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

        @NotNull
        private IllegalStateException exception() {

            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution pass(@NotNull final UnitDuration delay,
                @Nullable final Iterable<? extends IN> inputs) {

            throw exception();
        }

        @Override
        void orderBy(@NotNull final OrderType orderType) {

            throw exception();
        }

        @Override
        boolean isChannelOpen() {

            return false;
        }

        @Nullable
        @Override
        OutputConsumer<IN> pass(@Nullable final OutputChannel<? extends IN> channel) {

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
        Execution onResult() {

            return null;
        }

        @NotNull
        @Override
        OutputChannel<OUT> getOutputChannel() {

            throw exception();
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
    private class ResultChannelState extends OutputChannelState {

        @NotNull
        private IllegalStateException exception() {

            mLogger.dbg("consumer invalid call on closed channel");
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

            mLogger.dbg(reason, "avoiding aborting result channel since invocation is complete");
            return new AbortResultExecution(reason);
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@NotNull final NestedQueue<IN> queue) {

            throw exception();
        }

        @Nullable
        @Override
        Execution onConsumerError(@NotNull final RoutineException error) {

            mLogger.wrn(error, "avoiding aborting consumer since channel is closed");
            return null;
        }
    }
}
