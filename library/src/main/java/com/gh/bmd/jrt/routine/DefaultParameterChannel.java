/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.InputDeadlockException;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.DefaultExecution.InputIterator;
import com.gh.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;
import com.gh.bmd.jrt.time.TimeDuration.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.gh.bmd.jrt.time.TimeDuration.ZERO;
import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Default implementation of a parameter input channel.
 * <p/>
 * Created by davide on 9/24/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultParameterChannel<INPUT, OUTPUT> implements ParameterChannel<INPUT, OUTPUT> {

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final DefaultExecution<INPUT, OUTPUT> mExecution;

    private final Check mHasInputs;

    private final NestedQueue<INPUT> mInputQueue;

    private final TimeDuration mInputTimeout;

    private final Logger mLogger;

    private final int mMaxInput;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChanel;

    private final Runner mRunner;

    private Throwable mAbortException;

    private int mInputCount;

    private TimeDuration mInputDelay = ZERO;

    private boolean mIsConsuming;

    private boolean mIsPendingExecution;

    private int mPendingExecutionCount;

    private ChannelState mState = ChannelState.INPUT;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @param manager       the invocation manager.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     * @throws java.lang.NullPointerException     if one of the parameters is null.
     */
    DefaultParameterChannel(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final InvocationManager<INPUT, OUTPUT> manager, @Nonnull final Runner runner,
            @Nonnull final Logger logger) {

        mLogger = logger.subContextLogger(this);
        mRunner = runner;
        mMaxInput = configuration.getInputSizeOr(Integer.MAX_VALUE);
        mInputTimeout = configuration.getInputTimeoutOr(ZERO);

        if (mInputTimeout == null) {

            throw new NullPointerException("the input timeout must not be null");
        }

        final int maxInputSize = mMaxInput;

        if (maxInputSize < 1) {

            throw new IllegalArgumentException("the input buffer size cannot be 0 or negative");
        }

        mInputQueue = (configuration.getInputOrderOr(OrderType.DELIVERY) == OrderType.DELIVERY)
                ? new SimpleNestedQueue<INPUT>() : new OrderedNestedQueue<INPUT>();
        mHasInputs = new Check() {

            public boolean isTrue() {

                return (mInputCount <= maxInputSize);
            }
        };
        mResultChanel = new DefaultResultChannel<OUTPUT>(configuration, new AbortHandler() {

            public void onAbort(@Nullable final Throwable reason, final long delay,
                    @Nonnull final TimeUnit timeUnit) {

                final boolean isDone;

                synchronized (mMutex) {

                    if (mState == ChannelState.EXCEPTION) {

                        mLogger.wrn("avoiding aborting result channel since invocation is aborted");
                        return;
                    }

                    isDone = (mState == ChannelState.RESULT);

                    if (isDone) {

                        mLogger.dbg(
                                "avoiding aborting result channel since invocation is complete");

                    } else {

                        mLogger.dbg("aborting result channel");
                        mAbortException = reason;
                        mState = ChannelState.EXCEPTION;
                    }
                }

                if (isDone) {

                    mRunner.run(new AbortResultExecution(reason), delay, timeUnit);

                } else {

                    mRunner.run(mExecution.abort(), delay, timeUnit);
                }
            }
        }, runner, logger);
        mExecution = new DefaultExecution<INPUT, OUTPUT>(manager, new DefaultInputIterator(),
                                                         mResultChanel, logger);
    }

    public boolean abort() {

        return abort(null);
    }

    public boolean abort(@Nullable final Throwable reason) {

        final TimeDuration delay;

        synchronized (mMutex) {

            if (!isOpen()) {

                mLogger.dbg(reason, "avoiding aborting since channel is closed");
                return false;
            }

            delay = mInputDelay;

            if (delay.isZero()) {

                mLogger.dbg(reason, "aborting channel");
                mAbortException =
                        (reason instanceof AbortException) ? reason : new AbortException(reason);
                mState = ChannelState.EXCEPTION;
            }
        }

        if (delay.isZero()) {

            mRunner.run(mExecution.abort(), 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedAbortExecution(reason), delay.time, delay.unit);
        }

        return true;
    }

    public boolean isOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ParameterChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            verifyOpen();

            if (delay == null) {

                mLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mInputDelay = delay;
        }

        return this;
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> after(final long delay,
            @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> now() {

        return after(ZERO);
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(
            @Nullable final OutputChannel<? extends INPUT> channel) {

        final TimeDuration delay;
        final DefaultOutputConsumer consumer;

        synchronized (mMutex) {

            verifyOpen();

            if (channel == null) {

                mLogger.wrn("passing null channel");
                return this;
            }

            mBoundChannels.add(channel);
            delay = mInputDelay;
            ++mPendingExecutionCount;
            mLogger.dbg("passing channel: %s", channel);

            consumer = new DefaultOutputConsumer(delay);
        }

        channel.bind(consumer);

        return this;
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

        NestedQueue<INPUT> inputQueue;
        ArrayList<INPUT> list = null;
        boolean needsExecution = false;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOpen();

            if (inputs == null) {

                mLogger.wrn("passing null iterable");
                return this;
            }

            inputQueue = mInputQueue;
            delay = mInputDelay;

            int count = 0;

            if (delay.isZero()) {

                for (final INPUT input : inputs) {

                    inputQueue.add(input);
                    ++count;
                }

            } else {

                inputQueue = inputQueue.addNested();
                list = new ArrayList<INPUT>();

                for (final INPUT input : inputs) {

                    list.add(input);
                }

                count = list.size();
            }

            mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mInputCount, count, inputs, delay);
            addInputs(count);

            if (delay.isZero()) {

                needsExecution = !mIsPendingExecution;

                if (needsExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                }

            } else {

                ++mPendingExecutionCount;
            }
        }

        if (delay.isZero()) {

            if (needsExecution) {

                mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
            }

        } else {

            mRunner.run(new DelayedListInputExecution(inputQueue, list), delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

        NestedQueue<INPUT> inputQueue;
        boolean needsExecution = false;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOpen();

            inputQueue = mInputQueue;
            delay = mInputDelay;

            if (delay.isZero()) {

                inputQueue.add(input);

            } else {

                inputQueue = inputQueue.addNested();
            }

            mLogger.dbg("passing input [#%d+1]: %s [%s]", mInputCount, input, delay);
            addInputs(1);

            if (delay.isZero()) {

                needsExecution = !mIsPendingExecution;

                if (needsExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                }

            } else {

                ++mPendingExecutionCount;
            }
        }

        if (delay.isZero()) {

            if (needsExecution) {

                mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
            }

        } else {

            mRunner.run(new DelayedInputExecution(inputQueue, input), delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

        synchronized (mMutex) {

            verifyOpen();

            if (inputs == null) {

                mLogger.wrn("passing null input array");
                return this;
            }
        }

        return pass(Arrays.asList(inputs));
    }

    @Nonnull
    public OutputChannel<OUTPUT> result() {

        final boolean needsExecution;

        synchronized (mMutex) {

            verifyOpen();

            mLogger.dbg("closing input channel");
            mState = ChannelState.OUTPUT;
            needsExecution = !mIsPendingExecution && !mIsConsuming;

            if (needsExecution) {

                ++mPendingExecutionCount;
                mIsPendingExecution = true;
            }
        }

        if (needsExecution) {

            mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
        }

        return mResultChanel.getOutput();
    }

    private void addInputs(final int count) {

        mInputCount += count;

        try {

            if (!mInputTimeout.waitTrue(mMutex, mHasInputs)) {

                throw new InputDeadlockException(
                        "deadlock while waiting for room in the input channel");
            }

        } catch (final InterruptedException e) {

            throw InvocationInterruptedException.interrupt(e);
        }
    }

    private boolean isInput() {

        return ((mState == ChannelState.INPUT) || (mState == ChannelState.OUTPUT));
    }

    private boolean isInputComplete() {

        return (mState == ChannelState.OUTPUT) && (mPendingExecutionCount <= 0) && !mIsConsuming;
    }

    private void verifyOpen() {

        if (mState == ChannelState.EXCEPTION) {

            final Throwable throwable = mAbortException;
            mLogger.dbg(throwable, "abort exception");
            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isOpen()) {

            mLogger.err("invalid call on closed channel");
            throw new IllegalStateException("the input channel is closed");
        }
    }

    /**
     * Enumeration identifying the channel internal state.
     */
    private enum ChannelState {

        INPUT,      // input channel is open
        OUTPUT,     // no more input
        RESULT,     // result called
        EXCEPTION   // abort issued
    }

    /**
     * Interface defining an object managing the creation and the recycling of invocation instances.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    interface InvocationManager<INPUT, OUTPUT> {

        /**
         * Creates and returns a new invocation instance.
         *
         * @return the invocation instance.
         */
        @Nonnull
        Invocation<INPUT, OUTPUT> create();

        /**
         * Discards the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        void discard(@Nonnull Invocation<INPUT, OUTPUT> invocation);

        /**
         * Recycles the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        void recycle(@Nonnull Invocation<INPUT, OUTPUT> invocation);
    }

    /**
     * Implementation of an execution handling the abortion of the result channel.
     */
    private class AbortResultExecution implements Execution {

        private Throwable mReason;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private AbortResultExecution(@Nullable final Throwable reason) {

            mReason = reason;
        }

        public void run() {

            mResultChanel.close(mReason);
        }
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<INPUT> {

        @Nullable
        public Throwable getAbortException() {

            synchronized (mMutex) {

                return mAbortException;
            }
        }

        public boolean hasInput() {

            synchronized (mMutex) {

                return isInput() && !mInputQueue.isEmpty();
            }
        }

        public boolean isAborting() {

            synchronized (mMutex) {

                return (mState == ChannelState.EXCEPTION);
            }
        }

        @Nullable
        @SuppressFBWarnings(value = "NO_NOTIFY_NOT_NOTIFYALL",
                justification = "only one input is released")
        public INPUT nextInput() {

            synchronized (mMutex) {

                final INPUT input = mInputQueue.removeFirst();
                mLogger.dbg("reading input [#%d]: %s", mInputCount, input);

                final int maxInput = mMaxInput;
                final int prevInputCount = mInputCount;

                if ((--mInputCount < maxInput) && (prevInputCount >= maxInput)) {

                    mMutex.notify();
                }

                return input;
            }
        }

        public void onAbortComplete() {

            final Throwable exception;
            final ArrayList<OutputChannel<?>> channels;

            synchronized (mMutex) {

                exception = mAbortException;
                mLogger.dbg(exception, "aborting bound channels [%d]", mBoundChannels.size());

                channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
                mBoundChannels.clear();
            }

            for (final OutputChannel<?> channel : channels) {

                channel.abort(exception);
            }
        }

        public boolean onConsumeComplete() {

            synchronized (mMutex) {

                mIsConsuming = false;
                return isInputComplete();
            }
        }

        public void onConsumeStart() {

            synchronized (mMutex) {

                if (!isInput()) {

                    mLogger.wrn("avoiding consuming input since invocation is complete [#%d]",
                                mPendingExecutionCount);
                    return;
                }

                mLogger.dbg("consuming input [#%d]", mPendingExecutionCount);
                --mPendingExecutionCount;
                mIsPendingExecution = false;
                mIsConsuming = true;
            }
        }

        public void onInvocationComplete() {

            synchronized (mMutex) {

                if (mState != ChannelState.EXCEPTION) {

                    mState = ChannelState.RESULT;
                }
            }
        }
    }

    /**
     * Default implementation of an output consumer pushing the consume data into the input
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<INPUT> {

        private final TimeDuration mDelay;

        private final NestedQueue<INPUT> mQueue;

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Constructor.
         *
         * @param delay the output delay.
         */
        private DefaultOutputConsumer(@Nonnull final TimeDuration delay) {

            mDelay = delay;
            mQueue = mInputQueue.addNested();
        }

        public void onComplete() {

            final boolean needsExecution;

            synchronized (mMutex) {

                verifyInput();

                mSubLogger.dbg("closing consumer");
                mQueue.close();
                needsExecution = !mIsPendingExecution && !mIsConsuming;

                if (needsExecution) {

                    mIsPendingExecution = true;

                } else {

                    --mPendingExecutionCount;
                }
            }

            if (needsExecution) {

                mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void onError(@Nullable final Throwable error) {

            synchronized (mMutex) {

                if (!isInput()) {

                    mSubLogger.wrn("avoiding aborting consumer since channel is closed");
                    return;
                }

                mSubLogger.dbg("aborting consumer");
                mAbortException = error;
                mState = ChannelState.EXCEPTION;
            }

            final TimeDuration delay = mDelay;
            mRunner.run(mExecution.abort(), delay.time, delay.unit);
        }

        public void onOutput(final INPUT output) {

            NestedQueue<INPUT> inputQueue;
            boolean needsExecution = false;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                verifyInput();

                inputQueue = mQueue;

                if (delay.isZero()) {

                    inputQueue.add(output);

                } else {

                    inputQueue = inputQueue.addNested();
                }

                mSubLogger.dbg("consumer input [#%d+1]: %s [%s]", mInputCount, output, delay);
                addInputs(1);

                if (delay.isZero()) {

                    needsExecution = !mIsPendingExecution;

                    if (needsExecution) {

                        ++mPendingExecutionCount;
                        mIsPendingExecution = true;
                    }

                } else {

                    ++mPendingExecutionCount;
                }
            }

            if (delay.isZero()) {

                if (needsExecution) {

                    mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
                }

            } else {

                mRunner.run(new DelayedInputExecution(inputQueue, output), delay.time, delay.unit);
            }
        }

        private void verifyInput() {

            if (mState == ChannelState.EXCEPTION) {

                final Throwable throwable = mAbortException;
                mSubLogger.dbg(throwable, "consumer abort exception");
                throw RoutineExceptionWrapper.wrap(throwable).raise();
            }

            if (!isInput()) {

                mSubLogger.dbg("consumer invalid call on closed channel");
                throw new IllegalStateException("the input channel is closed");
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed abort.
     */
    private class DelayedAbortExecution implements Execution {

        private final Throwable mThrowable;

        /**
         * Constructor.
         *
         * @param throwable the reason of the abort.
         */
        private DelayedAbortExecution(@Nullable final Throwable throwable) {

            mThrowable = throwable;
        }

        public void run() {

            final Throwable throwable = mThrowable;

            synchronized (mMutex) {

                if (isInputComplete() || (mState == ChannelState.RESULT)) {

                    mLogger.dbg(throwable, "avoiding aborting since channel is closed");
                    return;
                }

                mLogger.dbg(throwable, "aborting channel");
                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mRunner.run(mExecution.abort(), 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Implementation of an execution handling a delayed input.
     */
    private class DelayedInputExecution implements Execution {

        private final INPUT mInput;

        private final NestedQueue<INPUT> mQueue;

        /**
         * Constructor.
         *
         * @param queue the input queue.
         * @param input the input.
         */
        private DelayedInputExecution(@Nonnull final NestedQueue<INPUT> queue,
                @Nullable final INPUT input) {

            mQueue = queue;
            mInput = input;
        }

        public void run() {

            synchronized (mMutex) {

                if (!isInput()) {

                    mLogger.dbg("avoiding delayed input execution since channel is closed: %s",
                                mInput);
                    return;
                }

                mLogger.dbg("delayed input execution: %s", mInput);
                mQueue.add(mInput).close();
            }

            mExecution.run();
        }
    }

    /**
     * Implementation of an execution handling a delayed input of a list of data.
     */
    private class DelayedListInputExecution implements Execution {

        private final ArrayList<INPUT> mInputs;

        private final NestedQueue<INPUT> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the input queue.
         * @param inputs the list of input data.
         */
        private DelayedListInputExecution(@Nonnull final NestedQueue<INPUT> queue,
                final ArrayList<INPUT> inputs) {

            mInputs = inputs;
            mQueue = queue;
        }

        public void run() {

            synchronized (mMutex) {

                if (!isInput()) {

                    mLogger.dbg("avoiding delayed input execution since channel is closed: %s",
                                mInputs);
                    return;
                }

                mLogger.dbg("delayed input execution: %s", mInputs);
                mQueue.addAll(mInputs).close();
            }

            mExecution.run();
        }
    }
}
