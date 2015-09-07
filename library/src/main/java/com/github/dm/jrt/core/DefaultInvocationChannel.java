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

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.InputDeadlockException;
import com.github.dm.jrt.channel.InputTimeoutException;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.DefaultExecution.InputIterator;
import com.github.dm.jrt.core.DefaultResultChannel.AbortHandler;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.runner.Execution;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.TemplateExecution;
import com.github.dm.jrt.util.TimeDuration;
import com.github.dm.jrt.util.TimeDuration.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.github.dm.jrt.util.TimeDuration.ZERO;
import static com.github.dm.jrt.util.TimeDuration.fromUnit;

/**
 * Default implementation of a invocation input channel.
 * <p/>
 * Created by davide-maestroni on 06/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultInvocationChannel<IN, OUT> implements InvocationChannel<IN, OUT> {

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final DefaultExecution<IN, OUT> mExecution;

    private final Check mHasInputs;

    private final NestedQueue<IN> mInputQueue;

    private final TimeDuration mInputTimeout;

    private final Logger mLogger;

    private final int mMaxInput;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUT> mResultChanel;

    private final Runner mRunner;

    private RoutineException mAbortException;

    private int mInputCount;

    private TimeDuration mInputDelay = ZERO;

    private OrderType mInputOrder;

    private boolean mIsConsuming;

    private boolean mIsPendingExecution;

    private int mPendingExecutionCount;

    private int mPendingInputCount;

    private InputChannelState mState;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param manager       the invocation manager.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     */
    DefaultInvocationChannel(@Nonnull final InvocationConfiguration configuration,
            @Nonnull final InvocationManager<IN, OUT> manager, @Nonnull final Runner runner,
            @Nonnull final Logger logger) {

        mLogger = logger.subContextLogger(this);
        mRunner = runner;
        mInputOrder = configuration.getInputOrderTypeOr(OrderType.BY_CHANCE);
        mMaxInput = configuration.getInputMaxSizeOr(Integer.MAX_VALUE);
        mInputTimeout = configuration.getInputTimeoutOr(ZERO);
        mInputQueue = new NestedQueue<IN>() {

            @Override
            public void close() {

                // Prevents closing
            }
        };
        final int maxInputSize = mMaxInput;
        mHasInputs = new Check() {

            public boolean isTrue() {

                return (mInputCount <= maxInputSize);
            }
        };
        mResultChanel = new DefaultResultChannel<OUT>(configuration, new AbortHandler() {

            public void onAbort(@Nullable final RoutineException reason, final long delay,
                    @Nonnull final TimeUnit timeUnit) {

                final Execution execution;

                synchronized (mMutex) {

                    execution = mState.onHandlerAbort(reason);
                }

                if (execution != null) {

                    mRunner.run(execution, delay, timeUnit);
                }
            }
        }, runner, logger);
        mExecution =
                new DefaultExecution<IN, OUT>(manager, new DefaultInputIterator(), mResultChanel,
                                              logger);
        mState = new InputChannelState();
    }

    public boolean abort() {

        return abort(null);
    }

    public boolean abort(@Nullable final Throwable reason) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mInputDelay;
            execution = mState.abortInvocation(reason);
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
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

    @Nonnull
    public InvocationChannel<IN, OUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            mState.after(delay);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    public InvocationChannel<IN, OUT> now() {

        return after(ZERO);
    }

    @Nonnull
    public InvocationChannel<IN, OUT> orderByCall() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_CALL);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> orderByChance() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_CHANCE);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> orderByDelay() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_DELAY);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> pass(@Nullable final OutputChannel<? extends IN> channel) {

        final OutputConsumer<IN> consumer;

        synchronized (mMutex) {

            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {

            channel.passTo(consumer);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

        final Execution execution;
        final TimeDuration delay;

        synchronized (mMutex) {

            delay = mInputDelay;
            execution = mState.pass(inputs);
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> pass(@Nullable final IN input) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mInputDelay;
            execution = mState.pass(input);
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<IN, OUT> pass(@Nullable final IN... inputs) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mInputDelay;
            execution = mState.pass(inputs);
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public OutputChannel<OUT> result() {

        final TimeDuration delay;
        final Execution execution;
        final OutputChannel<OUT> result;

        synchronized (mMutex) {

            final InputChannelState state = mState;
            delay = mInputDelay;
            execution = state.onResult();
            result = state.getOutputChannel();
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return result;
    }

    public boolean hasDelays() {

        synchronized (mMutex) {

            return (mPendingInputCount > 0);
        }
    }

    private void internalAbort(@Nullable final RoutineException abortException) {

        mInputQueue.clear();
        mPendingInputCount = 0;
        mAbortException = abortException;
        mRunner.cancel(mExecution);
    }

    private void waitInputs(final int count) {

        if (mInputTimeout.isZero()) {

            mInputCount -= count;
            throw new InputTimeoutException("timeout while waiting for room in the input channel");
        }

        if (mRunner.isExecutionThread()) {

            mInputCount -= count;
            throw new InputDeadlockException(
                    "cannot wait on the invocation runner thread: " + Thread.currentThread());
        }

        try {

            if (!mInputTimeout.waitTrue(mMutex, mHasInputs)) {

                mInputCount -= count;
                throw new InputTimeoutException(
                        "timeout while waiting for room in the input channel");
            }

        } catch (final InterruptedException e) {

            mInputCount -= count;
            throw new InvocationInterruptedException(e);
        }
    }

    /**
     * Interface defining an object managing the creation and the recycling of invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    interface InvocationManager<IN, OUT> {

        /**
         * Creates a new invocation instance.
         *
         * @param observer the invocation observer.
         */
        void create(@Nonnull InvocationObserver<IN, OUT> observer);

        /**
         * Discards the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        void discard(@Nonnull Invocation<IN, OUT> invocation);

        /**
         * Recycles the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        void recycle(@Nonnull Invocation<IN, OUT> invocation);
    }

    /**
     * Interface defining an observer of invocation instances.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    interface InvocationObserver<IN, OUT> {

        /**
         * Called when a new invocation instances is available.
         *
         * @param invocation the invocation.
         */
        void onCreate(@Nonnull Invocation<IN, OUT> invocation);

        /**
         * Called when an error occurs during the invocation instantiation.
         *
         * @param error the error.
         */
        void onError(@Nonnull Throwable error);
    }

    /**
     * Implementation of an execution handling the abortion of the result channel.
     */
    private class AbortResultExecution extends TemplateExecution {

        private final Throwable mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private AbortResultExecution(@Nullable final Throwable reason) {

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

        @Nonnull
        @Override
        OutputChannel<OUT> getOutputChannel() {

            throw super.exception();
        }
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<IN> {

        @Nullable
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

        public boolean isAborting() {

            synchronized (mMutex) {

                return mState.isAborting();
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

            synchronized (mMutex) {

                return mState.onConsumeComplete();
            }
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

        private final TimeDuration mDelay;

        private final OrderType mOrderType;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         */
        private DefaultOutputConsumer() {

            final TimeDuration delay = (mDelay = mInputDelay);
            final OrderType order = (mOrderType = mInputOrder);
            mQueue = ((order == OrderType.BY_CALL) || ((order == OrderType.BY_DELAY)
                    && delay.isZero())) ? mInputQueue.addNested() : mInputQueue;
        }

        public void onComplete() {

            final Execution execution;

            synchronized (mMutex) {

                execution = mState.onConsumerComplete(mQueue);
            }

            if (execution != null) {

                mRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void onError(@Nullable final RoutineException error) {

            final Execution execution;

            synchronized (mMutex) {

                execution = mState.onConsumerError(error);
            }

            if (execution != null) {

                final TimeDuration delay = mDelay;
                mRunner.run(execution, delay.time, delay.unit);
            }
        }

        public void onOutput(final IN output) {

            final Execution execution;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                execution = mState.onConsumerOutput(output, mQueue, delay, mOrderType);
            }

            if (execution != null) {

                mRunner.run(execution, delay.time, delay.unit);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed abortion.
     */
    private class DelayedAbortExecution extends TemplateExecution {

        private final RoutineException mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private DelayedAbortExecution(@Nullable final RoutineException reason) {

            mAbortException = reason;
        }

        public void run() {

            final Execution execution;

            synchronized (mMutex) {

                execution = mState.delayedAbortInvocation(mAbortException);
            }

            if (execution != null) {

                execution.run();
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed input.
     */
    private class DelayedInputExecution extends TemplateExecution {

        private final IN mInput;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param queue the input queue.
         * @param input the input.
         */
        private DelayedInputExecution(@Nonnull final NestedQueue<IN> queue,
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

                execution.run();
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed input of a list of data.
     */
    private class DelayedListInputExecution extends TemplateExecution {

        private final ArrayList<IN> mInputs;

        private final NestedQueue<IN> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the input queue.
         * @param inputs the list of input data.
         */
        private DelayedListInputExecution(@Nonnull final NestedQueue<IN> queue,
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

                execution.run();
            }
        }
    }

    /**
     * Exception thrown during invocation execution.
     */
    private class ExceptionChannelState extends ResultChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Override
        void after(@Nonnull final TimeDuration delay) {

            throw exception();
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @Nonnull final NestedQueue<IN> queue,
                @Nonnull final TimeDuration delay, @Nonnull final OrderType orderType) {

            throw consumerException();
        }

        @Nullable
        @Override
        Execution onHandlerAbort(@Nullable final RoutineException reason) {

            mSubLogger.wrn("avoiding aborting result channel since invocation is aborted");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@Nonnull final NestedQueue<IN> queue) {

            throw consumerException();
        }

        @Nonnull
        private RoutineException consumerException() {

            final RoutineException abortException = mAbortException;
            mSubLogger.dbg(abortException, "consumer abort exception");
            return abortException;
        }

        @Nonnull
        private RoutineException exception() {

            final RoutineException abortException = mAbortException;
            mSubLogger.dbg(abortException, "abort exception");
            throw abortException;
        }

        @Override
        void orderBy(@Nonnull final OrderType orderType) {

            throw exception();
        }

        @Override
        public boolean isAborting() {

            return true;
        }

        @Override
        public void onInvocationComplete() {

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

        @Nonnull
        @Override
        OutputChannel<OUT> getOutputChannel() {

            mPendingInputCount = 0;
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

            if (mInputDelay.isZero()) {

                mSubLogger.dbg(reason, "aborting channel");
                internalAbort(abortException);
                mState = new AbortedChannelState();
                return mExecution.abort();
            }

            return new DelayedAbortExecution(abortException);
        }

        /**
         * Called to set the specified input delay.
         *
         * @param delay the delay.
         */
        @SuppressWarnings("ConstantConditions")
        void after(@Nonnull final TimeDuration delay) {

            if (delay == null) {

                mLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mInputDelay = delay;
        }

        /**
         * Called when the invocation is aborted after a delay.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution delayedAbortInvocation(@Nullable final RoutineException reason) {

            mSubLogger.dbg(reason, "aborting channel");
            internalAbort(reason);
            mState = new AbortedChannelState();
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
        Execution delayedInput(@Nonnull final NestedQueue<IN> queue, @Nullable final IN input) {

            mSubLogger.dbg("delayed input execution: %s", input);
            --mPendingInputCount;
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
        Execution delayedInputs(@Nonnull final NestedQueue<IN> queue, final List<IN> inputs) {

            mSubLogger.dbg("delayed input execution: %s", inputs);
            --mPendingInputCount;
            queue.addAll(inputs);
            queue.close();
            return mExecution;
        }

        /**
         * Called to get the output channel.
         *
         * @return the output channel.
         */
        @Nonnull
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
        Execution onConsumerComplete(@Nonnull final NestedQueue<IN> queue) {

            mSubLogger.dbg("closing consumer");
            --mPendingInputCount;
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
        Execution onConsumerError(@Nullable final RoutineException error) {

            mSubLogger.dbg("aborting consumer");
            mPendingInputCount = 0;
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
         * @param delay     the input delay.
         * @param orderType the input order type.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerOutput(final IN input, @Nonnull final NestedQueue<IN> queue,
                @Nonnull final TimeDuration delay, @Nonnull final OrderType orderType) {

            mSubLogger.dbg("consumer input [#%d+1]: %s [%s]", mInputCount, input, delay);
            ++mInputCount;

            if (!mHasInputs.isTrue()) {

                waitInputs(1);

                if (mState != this) {

                    --mInputCount;
                    return mState.onConsumerOutput(input, queue, delay, orderType);
                }
            }

            if (delay.isZero()) {

                queue.add(input);

                if (!mIsPendingExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingInputCount;
            ++mPendingExecutionCount;
            return new DelayedInputExecution(
                    (orderType != OrderType.BY_CHANCE) ? queue.addNested() : queue, input);
        }

        /**
         * Called when the invocation is aborted through the registered handler.
         *
         * @param reason the reason of the abortion.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onHandlerAbort(@Nullable final RoutineException reason) {

            mSubLogger.dbg("aborting result channel");
            internalAbort(reason);
            mState = new ExceptionChannelState();
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
        void orderBy(@Nonnull final OrderType orderType) {

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
            ++mPendingInputCount;
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

            if (size > mMaxInput) {

                throw new InputTimeoutException(
                        "inputs exceed maximum channel size [" + size + "/" + mMaxInput + "]");
            }

            final TimeDuration delay = mInputDelay;
            mSubLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mInputCount, size, inputs, delay);
            mInputCount += size;

            if (!mHasInputs.isTrue()) {

                waitInputs(size);

                if (mState != this) {

                    mInputCount -= size;
                    return mState.pass(inputs);
                }
            }

            if (delay.isZero()) {

                mInputQueue.addAll(list);

                if (!mIsPendingExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingInputCount;
            ++mPendingExecutionCount;
            return new DelayedListInputExecution(
                    (mInputOrder != OrderType.BY_CHANCE) ? mInputQueue.addNested() : mInputQueue,
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

            final TimeDuration delay = mInputDelay;
            mSubLogger.dbg("passing input [#%d+1]: %s [%s]", mInputCount, input, delay);
            ++mInputCount;

            if (!mHasInputs.isTrue()) {

                waitInputs(1);

                if (mState != this) {

                    --mInputCount;
                    return mState.pass(input);
                }
            }

            if (delay.isZero()) {

                mInputQueue.add(input);

                if (!mIsPendingExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                    return mExecution;
                }

                return null;
            }

            ++mPendingInputCount;
            ++mPendingExecutionCount;
            return new DelayedInputExecution(
                    (mInputOrder != OrderType.BY_CHANCE) ? mInputQueue.addNested() : mInputQueue,
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

            if (size > mMaxInput) {

                throw new InputTimeoutException(
                        "inputs exceed maximum channel size [" + size + "/" + mMaxInput + "]");
            }

            final TimeDuration delay = mInputDelay;
            mSubLogger.dbg("passing array [#%d+%d]: %s [%s]", mInputCount, size, inputs, delay);
            mInputCount += size;

            if (!mHasInputs.isTrue()) {

                waitInputs(size);

                if (mState != this) {

                    mInputCount -= size;
                    return mState.pass(inputs);
                }
            }

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

            ++mPendingInputCount;
            ++mPendingExecutionCount;
            return new DelayedListInputExecution(
                    (mInputOrder != OrderType.BY_CHANCE) ? mInputQueue.addNested() : mInputQueue,
                    list);
        }

        @Nullable
        public RoutineException getAbortException() {

            return mAbortException;
        }

        public boolean hasInput() {

            return !mInputQueue.isEmpty();
        }

        public boolean isAborting() {

            return false;
        }

        @Nullable
        @SuppressFBWarnings(value = "NO_NOTIFY_NOT_NOTIFYALL",
                justification = "only one input is released")
        public IN nextInput() {

            final IN input = mInputQueue.removeFirst();
            mSubLogger.dbg("reading input [#%d]: %s", mInputCount, input);
            final int maxInput = mMaxInput;
            final int prevInputCount = mInputCount;

            if ((--mInputCount <= maxInput) && (prevInputCount > maxInput)) {

                mMutex.notify();
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
        void after(@Nonnull final TimeDuration delay) {

            throw exception();
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@Nullable final RoutineException reason) {

            if ((mPendingExecutionCount <= 0) && !mIsConsuming) {

                mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
                return null;
            }

            return super.delayedAbortInvocation(reason);
        }

        @Nonnull
        private IllegalStateException exception() {

            mSubLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Override
        void orderBy(@Nonnull final OrderType orderType) {

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

        @Nonnull
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

        @Nonnull
        private IllegalStateException exception() {

            mSubLogger.dbg("consumer invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Nullable
        @Override
        Execution onConsumerOutput(final IN input, @Nonnull final NestedQueue<IN> queue,
                @Nonnull final TimeDuration delay, @Nonnull final OrderType orderType) {

            throw exception();
        }

        @Nullable
        @Override
        Execution delayedAbortInvocation(@Nullable final RoutineException reason) {

            mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        public boolean onConsumeComplete() {

            mIsConsuming = false;
            return false;
        }

        @Nullable
        @Override
        Execution delayedInput(@Nonnull final NestedQueue<IN> queue, @Nullable final IN input) {

            mSubLogger.dbg("avoiding delayed input execution since channel is closed: %s", input);
            return null;
        }

        @Nullable
        @Override
        Execution delayedInputs(@Nonnull final NestedQueue<IN> queue, final List<IN> inputs) {

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
        Execution onHandlerAbort(@Nullable final RoutineException reason) {

            mSubLogger.dbg("avoiding aborting result channel since invocation is complete");
            return new AbortResultExecution(reason);
        }

        @Nullable
        @Override
        Execution onConsumerComplete(@Nonnull final NestedQueue<IN> queue) {

            throw exception();
        }

        @Nullable
        @Override
        Execution onConsumerError(@Nullable final RoutineException error) {

            mSubLogger.wrn("avoiding aborting consumer since channel is closed");
            return null;
        }
    }
}
