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

    private final int mInputLimit;

    private final UnitDuration mInputMaxDelay;

    private final NestedQueue<IN> mInputQueue;

    private final Logger mLogger;

    private final int mMaxInput;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUT> mResultChanel;

    private final Runner mRunner;

    private RoutineException mAbortException;

    private int mInputCount;

    private long mInputDelay = 0;

    private TimeUnit mInputDelayUnit = TimeUnit.MILLISECONDS;

    private OrderType mInputOrder;

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
        mInputOrder = configuration.getInputOrderTypeOrElse(OrderType.BY_DELAY);
        mInputLimit = configuration.getInputLimitOrElse(Integer.MAX_VALUE);
        mInputMaxDelay = configuration.getInputMaxDelayOrElse(zero());
        mMaxInput = configuration.getInputMaxSizeOrElse(Integer.MAX_VALUE);
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

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mInputDelay;
            timeUnit = mInputDelayUnit;
            execution = mState.abortInvocation(reason);
        }

        if (execution != null) {
            runExecution(execution, delay, timeUnit);
            return true;
        }

        return false;
    }

    public boolean isEmpty() {

        synchronized (mMutex) {
            return mInputQueue.isEmpty();
        }
    }

    public boolean isOpen() {

        synchronized (mMutex) {
            return mState.isChannelOpen();
        }
    }

    @NotNull
    public InvocationChannel<IN, OUT> after(@NotNull final UnitDuration delay) {

        return after(delay.value, delay.unit);
    }

    @NotNull
    public InvocationChannel<IN, OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        synchronized (mMutex) {
            mState.after(delay, timeUnit);
        }

        return this;
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

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mInputDelay;
            timeUnit = mInputDelayUnit;
            execution = mState.pass(inputs);
        }

        if (execution != null) {
            runExecution(execution, delay, timeUnit);
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

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mInputDelay;
            timeUnit = mInputDelayUnit;
            execution = mState.pass(input);
        }

        if (execution != null) {
            runExecution(execution, delay, timeUnit);
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

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mInputDelay;
            timeUnit = mInputDelayUnit;
            execution = mState.pass(inputs);
        }

        if (execution != null) {
            runExecution(execution, delay, timeUnit);
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

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        final OutputChannel<OUT> result;
        synchronized (mMutex) {
            delay = mInputDelay;
            timeUnit = mInputDelayUnit;
            final InputChannelState state = mState;
            execution = state.onResult();
            result = state.getOutputChannel();
        }

        if (execution != null) {
            runExecution(execution, delay, timeUnit);
        }

        return result;
    }

    private void checkMaxSize() {

        if (mInputCount > mMaxInput) {
            throw new InputDeadlockException(
                    "maximum input channel size has been reached: " + mMaxInput);
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

        final UnitDuration delay = mInputMaxDelay;
        if (!delay.isZero() && mRunner.isExecutionThread()) {
            throw new InputDeadlockException(
                    "cannot wait on the invocation runner thread: " + Thread.currentThread()
                            + "\nTry employing a different runner than: " + mRunner);
        }

        try {
            if (!delay.waitTrue(mMutex, mHasInputs)) {
                mLogger.dbg("timeout while waiting for room in the input channel [%s]", delay);
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

            mDelay = mInputDelay;
            mDelayUnit = mInputDelayUnit;
            final OrderType order = (mOrderType = mInputOrder);
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

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Override
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

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

            mSubLogger.wrn("avoiding aborting result channel since invocation is aborted");
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
            mSubLogger.dbg(abortException, "consumer abort exception");
            return abortException;
        }

        @NotNull
        private RoutineException exception() {

            final RoutineException abortException = mAbortException;
            mSubLogger.dbg(abortException, "abort exception");
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
        Execution pass(@Nullable final Iterable<? extends IN> inputs) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN input) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN... inputs) {

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

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Called when the invocation is aborted.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution abortInvocation(@Nullable final Throwable reason) {

            final RoutineException abortException = AbortException.wrapIfNeeded(reason);
            if (mInputDelay == 0) {
                mSubLogger.dbg(reason, "aborting channel");
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
         * @param delay    the delay value.
         * @param timeUnit the delay unit.
         */
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

            mInputDelay = ConstantConditions.notNegative("input delay", delay);
            mInputDelayUnit = ConstantConditions.notNull("input delay unit", timeUnit);
        }

        /**
         * Called when the invocation is aborted after a delay.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {

            mSubLogger.dbg(reason, "aborting channel");
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

            mSubLogger.dbg("delayed input execution: %s", input);
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

            mSubLogger.dbg("delayed input execution: %s", inputs);
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

            mSubLogger.dbg("closing consumer");
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

            mSubLogger.dbg("aborting consumer");
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

            mSubLogger.dbg("consumer input [#%d+1]: %s [%d %s]", mInputCount, input, delay,
                    timeUnit);
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

            mSubLogger.dbg("aborting result channel");
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

            mSubLogger.dbg("closing input channel");
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

            mInputOrder = orderType;
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
                mSubLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingExecutionCount;
            mSubLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer();
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param inputs the inputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final Iterable<? extends IN> inputs) {

            if (inputs == null) {
                mSubLogger.wrn("passing null iterable");
                return null;
            }

            final ArrayList<IN> list = new ArrayList<IN>();
            for (final IN input : inputs) {
                list.add(input);
            }

            final int size = list.size();
            final long delay = mInputDelay;
            mSubLogger.dbg("passing iterable [#%d+%d]: %s [%d %s]", mInputCount, size, inputs,
                    delay, mInputDelayUnit);
            mInputCount += size;
            checkMaxSize();
            if (delay == 0) {
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
                    (mInputOrder != OrderType.BY_DELAY) ? mInputQueue.addNested() : mInputQueue,
                    list);
        }

        /**
         * Called when an input is passed to the invocation.
         *
         * @param input the input.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final IN input) {

            final long delay = mInputDelay;
            mSubLogger.dbg("passing input [#%d+1]: %s [%d %s]", mInputCount, input, delay,
                    mInputDelayUnit);
            ++mInputCount;
            checkMaxSize();
            if (delay == 0) {
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
                    (mInputOrder != OrderType.BY_DELAY) ? mInputQueue.addNested() : mInputQueue,
                    input);
        }

        /**
         * Called when some inputs are passed to the invocation.
         *
         * @param inputs the inputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final IN... inputs) {

            if (inputs == null) {
                mSubLogger.wrn("passing null input array");
                return null;
            }

            final int size = inputs.length;
            final long delay = mInputDelay;
            mSubLogger.dbg("passing array [#%d+%d]: %s [%d %s]", mInputCount, size, inputs, delay,
                    mInputDelayUnit);
            mInputCount += size;
            checkMaxSize();
            final ArrayList<IN> list = new ArrayList<IN>(size);
            Collections.addAll(list, inputs);
            if (delay == 0) {
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
                    (mInputOrder != OrderType.BY_DELAY) ? mInputQueue.addNested() : mInputQueue,
                    list);
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
            mSubLogger.dbg("reading input [#%d]: %s", mInputCount, input);
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

            mSubLogger.dbg("consuming input [#%d]", mPendingExecutionCount);
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

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nullable
        @Override
        Execution abortInvocation(@Nullable final Throwable reason) {

            mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

            throw exception();
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {

            if ((mPendingExecutionCount <= 0) && !mIsConsuming) {
                mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
                return null;
            }

            return super.delayedAbortInvocation(reason);
        }

        @NotNull
        private IllegalStateException exception() {

            mSubLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
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
        Execution pass(@Nullable final Iterable<? extends IN> inputs) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN input) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final IN... inputs) {

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

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @NotNull
        private IllegalStateException exception() {

            mSubLogger.dbg("consumer invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @NotNull final NestedQueue<IN> queue,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {

            throw exception();
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@NotNull final RoutineException reason) {

            mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
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

            mSubLogger.dbg("avoiding delayed input execution since channel is closed: %s", input);
            return null;
        }

        @Nullable
        @Override
        Execution delayedInputs(@NotNull final NestedQueue<IN> queue, final List<IN> inputs) {

            mSubLogger.dbg("avoiding delayed input execution since channel is closed: %s", inputs);
            return null;
        }

        @Override
        public boolean hasInput() {

            return false;
        }

        @Override
        public void onConsumeStart() {

            mSubLogger.wrn("avoiding consuming input since invocation is complete [#%d]",
                    mPendingExecutionCount);
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@NotNull final RoutineException reason) {

            mSubLogger.dbg("avoiding aborting result channel since invocation is complete");
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

            mSubLogger.wrn("avoiding aborting consumer since channel is closed");
            return null;
        }
    }
}
