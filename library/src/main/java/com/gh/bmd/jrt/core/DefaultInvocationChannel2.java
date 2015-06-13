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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InputDeadlockException;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.DefaultExecution.InputIterator;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationManager;
import com.gh.bmd.jrt.core.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.TimeDuration;
import com.gh.bmd.jrt.util.TimeDuration.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.gh.bmd.jrt.util.TimeDuration.ZERO;
import static com.gh.bmd.jrt.util.TimeDuration.fromUnit;

/**
 * Default implementation of a invocation input channel.
 * <p/>
 * Created by davide on 11/06/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultInvocationChannel2<INPUT, OUTPUT> implements InvocationChannel<INPUT, OUTPUT> {

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

    private InvocationChannelState<INPUT, OUTPUT> mState = new InputChannelState();

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param manager       the invocation manager.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     */
    DefaultInvocationChannel2(@Nonnull final InvocationConfiguration configuration,
            @Nonnull final InvocationManager<INPUT, OUTPUT> manager, @Nonnull final Runner runner,
            @Nonnull final Logger logger) {

        mLogger = logger.subContextLogger(this);
        mRunner = runner;
        mMaxInput = configuration.getInputMaxSizeOr(Integer.MAX_VALUE);
        mInputTimeout = configuration.getInputTimeoutOr(ZERO);

        if (mInputTimeout == null) {

            throw new NullPointerException("the input timeout must not be null");
        }

        final int maxInputSize = mMaxInput;

        if (maxInputSize < 1) {

            throw new IllegalArgumentException("the input buffer size cannot be 0 or negative");
        }

        mInputQueue = (configuration.getInputOrderTypeOr(OrderType.NONE) == OrderType.NONE)
                ? new SimpleNestedQueue<INPUT>() : new OrderedNestedQueue<INPUT>();
        mHasInputs = new Check() {

            public boolean isTrue() {

                return (mInputCount <= maxInputSize);
            }
        };
        mResultChanel = new DefaultResultChannel<OUTPUT>(configuration, new AbortHandler() {

            public void onAbort(@Nullable final Throwable reason, final long delay,
                    @Nonnull final TimeUnit timeUnit) {

                final Execution execution;

                synchronized (mMutex) {

                    execution = mState.onAbort(reason);
                }

                if (execution != null) {

                    mRunner.run(execution, delay, timeUnit);
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
        final Execution execution;

        synchronized (mMutex) {

            delay = mInputDelay;
            execution = mState.abort(reason);
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
            return true;
        }

        return false;
    }

    public boolean isOpen() {

        synchronized (mMutex) {

            return mState.isOpen();
        }
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public InvocationChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            if (delay == null) {

                mLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mInputDelay = delay;
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> after(final long delay,
            @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> now() {

        return after(ZERO);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> pass(
            @Nullable final OutputChannel<? extends INPUT> channel) {

        final OutputConsumer<INPUT> consumer;

        synchronized (mMutex) {

            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {

            channel.passTo(consumer);
        }

        return this;
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

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
    public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

        final Execution execution;
        final TimeDuration delay;

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
    public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

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
    public OutputChannel<OUTPUT> result() {

        final Execution execution;
        final TimeDuration delay;
        final OutputChannel<OUTPUT> result;

        synchronized (mMutex) {

            final InvocationChannelState<INPUT, OUTPUT> state = mState;
            delay = mInputDelay;
            execution = state.onResult();
            result = state.result();
        }

        if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return result;
    }

    private void addInputs(final int count) {

        mInputCount += count;

        try {

            if (!mInputTimeout.waitTrue(mMutex, mHasInputs)) {

                throw new InputDeadlockException(
                        "deadlock while waiting for room in the input channel");
            }

        } catch (final InterruptedException e) {

            throw new InvocationInterruptedException(e);
        }
    }

    /**
     * Interface defining the channel internal state (using "state" design pattern).
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private interface InvocationChannelState<INPUT, OUTPUT> extends InputIterator<INPUT> {

        /**
         * See {@link InvocationChannel#abort(Throwable)}.
         */
        @Nullable
        Execution abort(@Nullable Throwable reason);

        /**
         * Delayed {@link InvocationChannel#abort(Throwable)}.
         */
        boolean delayedAbort(@Nullable Throwable reason);

        /**
         * Delayed {@link InvocationChannel#pass(Object)}.
         */
        boolean delayedInput(@Nonnull NestedQueue<INPUT> queue, @Nullable INPUT input);

        /**
         * Delayed {@link InvocationChannel#pass(Iterable)}.
         */
        boolean delayedInputs(@Nonnull NestedQueue<INPUT> queue, List<INPUT> inputs);

        /**
         * See {@link InvocationChannel#isOpen()}.
         */
        boolean isOpen();

        /**
         * See {@link AbortHandler#onAbort(Throwable, long, TimeUnit)}.
         */
        @Nullable
        Execution onAbort(@Nullable Throwable reason);

        @Nonnull
        List<OutputChannel<?>> onAbortComplete(@Nonnull Throwable abortException);

        /**
         * See {@link OutputConsumer#onComplete()}.
         */
        @Nullable
        Execution onComplete(@Nonnull NestedQueue<INPUT> queue, @Nonnull Logger logger);

        /**
         * See {@link OutputConsumer#onError(Throwable)}.
         */
        @Nullable
        Execution onError(@Nullable Throwable error, @Nonnull Logger logger);

        /**
         * See {@link OutputConsumer#onOutput(Object)}.
         */
        @Nullable
        Execution onOutput(INPUT output, @Nonnull TimeDuration delay,
                @Nonnull NestedQueue<INPUT> queue, @Nonnull Logger logger);

        Execution onResult();

        /**
         * See {@link InvocationChannel#pass(OutputChannel)}.
         */
        @Nullable
        OutputConsumer<INPUT> pass(@Nullable OutputChannel<? extends INPUT> channel);

        /**
         * See {@link InvocationChannel#pass(Iterable)}.
         */
        @Nullable
        Execution pass(@Nullable Iterable<? extends INPUT> inputs);

        /**
         * See {@link InvocationChannel#pass(Object)}.
         */
        @Nullable
        Execution pass(@Nullable INPUT input);

        /**
         * See {@link InvocationChannel#pass(Object[])}.
         */
        @Nullable
        Execution pass(@Nullable INPUT... inputs);

        /**
         * Delayed {@link InvocationChannel#result()}.
         */
        @Nonnull
        OutputChannel<OUTPUT> result();
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

        @Override
        public Execution onResult() {

            return null;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> result() {

            throw super.exception();
        }
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<INPUT> {

        @Nullable
        public Throwable getAbortException() {

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
        public INPUT nextInput() {

            synchronized (mMutex) {

                return mState.nextInput();
            }
        }

        public void onAbortComplete() {

            final Throwable abortException;
            final List<OutputChannel<?>> channels;

            synchronized (mMutex) {

                abortException = mAbortException;
                channels = mState.onAbortComplete(abortException);
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

            final Execution execution;

            synchronized (mMutex) {

                execution = mState.onComplete(mQueue, mSubLogger);
            }

            if (execution != null) {

                mRunner.run(execution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void onError(@Nullable final Throwable error) {

            final Execution execution;

            synchronized (mMutex) {

                execution = mState.onError(error, mSubLogger);
            }

            if (execution != null) {

                final TimeDuration delay = mDelay;
                mRunner.run(execution, delay.time, delay.unit);
            }
        }

        public void onOutput(final INPUT output) {

            final Execution execution;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                execution = mState.onOutput(output, delay, mQueue, mSubLogger);
            }

            if (execution != null) {

                mRunner.run(execution, delay.time, delay.unit);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed abortion.
     */
    private class DelayedAbortExecution implements Execution {

        private final Throwable mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private DelayedAbortExecution(@Nullable final Throwable reason) {

            mAbortException = reason;
        }

        public void run() {

            final boolean isAbort;
            final InvocationChannelState<INPUT, OUTPUT> state;

            synchronized (mMutex) {

                state = mState;
                isAbort = state.delayedAbort(mAbortException);
            }

            if (isAbort) {

                mRunner.run(mExecution.abort(), 0, TimeUnit.MILLISECONDS);
            }
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

            final boolean isInput;
            final InvocationChannelState<INPUT, OUTPUT> state;

            synchronized (mMutex) {

                state = mState;
                isInput = state.delayedInput(mQueue, mInput);
            }

            if (isInput) {

                mExecution.run();
            }
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

            final boolean isInput;
            final InvocationChannelState<INPUT, OUTPUT> state;

            synchronized (mMutex) {

                state = mState;
                isInput = state.delayedInputs(mQueue, mInputs);
            }

            if (isInput) {

                mExecution.run();
            }
        }
    }

    /**
     * Exception thrown during invocation execution.
     */
    private class ExceptionChannelState extends ResultChannelState {

        @Override
        public Execution onAbort(@Nullable final Throwable reason) {

            mLogger.wrn("avoiding aborting result channel since invocation is aborted");
            return null;
        }

        @Override
        public Execution onComplete(@Nonnull final NestedQueue<INPUT> queue,
                @Nonnull final Logger logger) {

            throw consumerException(logger);
        }

        @Override
        public Execution onOutput(final INPUT output, @Nonnull final TimeDuration delay,
                @Nonnull final NestedQueue<INPUT> queue, @Nonnull final Logger logger) {

            throw consumerException(logger);
        }

        @Nonnull
        private RoutineException consumerException(@Nonnull final Logger logger) {

            final Throwable abortException = mAbortException;
            logger.dbg(abortException, "consumer abort exception");
            return RoutineExceptionWrapper.wrap(abortException).raise();
        }

        @Nonnull
        private RoutineException exception() {

            final Throwable abortException = mAbortException;
            mLogger.dbg(abortException, "abort exception");
            throw RoutineExceptionWrapper.wrap(abortException).raise();
        }

        @Override
        public boolean isAborting() {

            return true;
        }

        @Override
        public void onInvocationComplete() {

        }

        @Override
        public OutputConsumer<INPUT> pass(@Nullable final OutputChannel<? extends INPUT> channel) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final Iterable<? extends INPUT> inputs) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final INPUT input) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final INPUT... inputs) {

            throw exception();
        }

        @Override
        public Execution onResult() {

            mState = new AbortedChannelState();
            return null;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> result() {

            final OutputChannel<OUTPUT> outputChannel = mResultChanel.getOutput();
            outputChannel.abort(mAbortException);
            return outputChannel;
        }
    }

    /**
     * The input channel is open.
     */
    private class InputChannelState implements InvocationChannelState<INPUT, OUTPUT> {

        @Nullable
        public Execution abort(@Nullable final Throwable reason) {

            final Throwable abortException =
                    (reason instanceof RoutineException) ? reason : new AbortException(reason);

            if (mInputDelay.isZero()) {

                mLogger.dbg(reason, "aborting channel");
                mAbortException = abortException;
                mState = new AbortedChannelState();
                return mExecution.abort();
            }

            return new DelayedAbortExecution(abortException);
        }

        public boolean delayedAbort(@Nullable final Throwable reason) {

            mLogger.dbg(reason, "aborting channel");
            DefaultInvocationChannel2.this.mAbortException = reason;
            mState = new AbortedChannelState();
            return true;
        }

        public boolean delayedInput(@Nonnull final NestedQueue<INPUT> queue,
                @Nullable final INPUT input) {

            mLogger.dbg("delayed input execution: %s", input);
            queue.add(input);
            queue.close();
            return true;
        }

        public boolean delayedInputs(@Nonnull final NestedQueue<INPUT> queue,
                final List<INPUT> inputs) {

            mLogger.dbg("delayed input execution: %s", inputs);
            queue.addAll(inputs);
            queue.close();
            return true;
        }

        public boolean isOpen() {

            return true;
        }

        @Nullable
        public Execution onAbort(@Nullable final Throwable reason) {

            mLogger.dbg("aborting result channel");
            mAbortException = reason;
            mState = new ExceptionChannelState();
            return mExecution.abort();
        }

        @Nonnull
        public List<OutputChannel<?>> onAbortComplete(@Nonnull final Throwable abortException) {

            mLogger.dbg(abortException, "aborting bound channels [%d]", mBoundChannels.size());
            final ArrayList<OutputChannel<?>> channels =
                    new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();
            return channels;
        }

        @Nullable
        public Execution onComplete(@Nonnull NestedQueue<INPUT> queue, @Nonnull Logger logger) {

            logger.dbg("closing consumer");
            queue.close();
            final boolean needsExecution = !mIsPendingExecution && !mIsConsuming;

            if (needsExecution) {

                mIsPendingExecution = true;

            } else {

                --mPendingExecutionCount;
            }

            return (needsExecution) ? mExecution : null;
        }

        @Nullable
        public Execution onError(@Nullable final Throwable error, @Nonnull Logger logger) {

            logger.dbg("aborting consumer");
            mAbortException = error;
            mState = new ExceptionChannelState();
            return mExecution.abort();
        }

        @Nullable
        public Execution onOutput(final INPUT output, @Nonnull final TimeDuration delay,
                @Nonnull NestedQueue<INPUT> queue, @Nonnull Logger logger) {

            final NestedQueue<INPUT> inputQueue;
            boolean needsExecution = false;

            if (delay.isZero()) {

                inputQueue = queue;
                queue.add(output);

            } else {

                inputQueue = queue.addNested();
            }

            logger.dbg("consumer input [#%d+1]: %s [%s]", mInputCount, output, delay);
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

            if (delay.isZero()) {

                if (needsExecution) {

                    return mExecution;
                }

            } else {

                return new DelayedInputExecution(inputQueue, output);
            }

            return null;
        }

        @Nullable
        public Execution onResult() {

            mLogger.dbg("closing input channel");
            mState = new OutputChannelState();
            final boolean needsExecution = !mIsPendingExecution && !mIsConsuming;

            if (needsExecution) {

                ++mPendingExecutionCount;
                mIsPendingExecution = true;
            }

            return (needsExecution) ? mExecution : null;
        }

        @Nullable
        public OutputConsumer<INPUT> pass(@Nullable final OutputChannel<? extends INPUT> channel) {

            if (channel == null) {

                mLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingExecutionCount;
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer(mInputDelay);
        }

        @Nullable
        public Execution pass(@Nullable final Iterable<? extends INPUT> inputs) {

            if (inputs == null) {

                mLogger.wrn("passing null iterable");
                return null;
            }

            ArrayList<INPUT> list = null;
            final NestedQueue<INPUT> inputQueue;
            final TimeDuration delay = mInputDelay;
            int count = 0;

            if (delay.isZero()) {

                inputQueue = mInputQueue;

                for (final INPUT input : inputs) {

                    inputQueue.add(input);
                    ++count;
                }

            } else {

                inputQueue = mInputQueue.addNested();
                list = new ArrayList<INPUT>();

                for (final INPUT input : inputs) {

                    list.add(input);
                }

                count = list.size();
            }

            mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mInputCount, count, inputs, delay);
            addInputs(count);
            boolean needsExecution = false;

            if (delay.isZero()) {

                needsExecution = !mIsPendingExecution;

                if (needsExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                }

            } else {

                ++mPendingExecutionCount;
            }

            if (delay.isZero()) {

                if (needsExecution) {

                    return mExecution;
                }

            } else {

                return new DelayedListInputExecution(inputQueue, list);
            }

            return null;
        }

        @Nullable
        public Execution pass(@Nullable final INPUT input) {

            final NestedQueue<INPUT> inputQueue;
            final TimeDuration delay = mInputDelay;

            if (delay.isZero()) {

                inputQueue = mInputQueue;
                inputQueue.add(input);

            } else {

                inputQueue = mInputQueue.addNested();
            }

            mLogger.dbg("passing input [#%d+1]: %s [%s]", mInputCount, input, delay);
            addInputs(1);
            boolean needsExecution = false;

            if (delay.isZero()) {

                needsExecution = !mIsPendingExecution;

                if (needsExecution) {

                    ++mPendingExecutionCount;
                    mIsPendingExecution = true;
                }

            } else {

                ++mPendingExecutionCount;
            }

            if (delay.isZero()) {

                if (needsExecution) {

                    return mExecution;
                }

            } else {

                return new DelayedInputExecution(inputQueue, input);
            }

            return null;
        }

        @Nullable
        public Execution pass(@Nullable final INPUT... inputs) {

            if (inputs == null) {

                mLogger.wrn("passing null input array");
                return null;
            }

            return pass(Arrays.asList(inputs));
        }

        @Nonnull
        public OutputChannel<OUTPUT> result() {

            return mResultChanel.getOutput();
        }

        @Nullable
        public Throwable getAbortException() {

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
        public INPUT nextInput() {

            final INPUT input = mInputQueue.removeFirst();
            mLogger.dbg("reading input [#%d]: %s", mInputCount, input);
            final int maxInput = mMaxInput;
            final int prevInputCount = mInputCount;

            if ((--mInputCount < maxInput) && (prevInputCount >= maxInput)) {

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
     * The channel is close and no more inputs are expected.
     */
    private class OutputChannelState extends InputChannelState {

        @Nullable
        @Override
        public Execution abort(@Nullable final Throwable reason) {

            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        public boolean delayedAbort(@Nullable final Throwable reason) {

            if ((mPendingExecutionCount <= 0) && !mIsConsuming) {

                mLogger.dbg(reason, "avoiding aborting since channel is closed");
                return false;
            }

            return super.delayedAbort(reason);
        }

        @Nonnull
        private IllegalStateException exception() {

            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Override
        public boolean isOpen() {

            return false;
        }

        @Override
        public OutputConsumer<INPUT> pass(@Nullable final OutputChannel<? extends INPUT> channel) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final Iterable<? extends INPUT> inputs) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final INPUT input) {

            throw exception();
        }

        @Override
        public Execution pass(@Nullable final INPUT... inputs) {

            throw exception();
        }

        @Override
        public Execution onResult() {

            return null;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> result() {

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

        @Override
        public boolean delayedAbort(@Nullable final Throwable reason) {

            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return false;
        }

        private IllegalStateException exception(@Nonnull final Logger logger) {

            logger.dbg("consumer invalid call on closed channel");
            return new IllegalStateException("the input channel is closed");
        }

        @Override
        public boolean onConsumeComplete() {

            return false;
        }

        @Override
        public boolean delayedInput(@Nonnull final NestedQueue<INPUT> queue,
                @Nullable final INPUT input) {

            mLogger.dbg("avoiding delayed input execution since channel is closed: %s", input);
            return false;
        }

        @Override
        public boolean delayedInputs(@Nonnull final NestedQueue<INPUT> queue,
                final List<INPUT> inputs) {

            mLogger.dbg("avoiding delayed input execution since channel is closed: %s", inputs);
            return false;
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

        @Override
        public Execution onAbort(@Nullable final Throwable reason) {

            mLogger.dbg("avoiding aborting result channel since invocation is complete");
            return new AbortResultExecution(reason);
        }

        @Override
        public Execution onComplete(@Nonnull final NestedQueue<INPUT> queue,
                @Nonnull final Logger logger) {

            throw exception(logger);
        }

        @Override
        public Execution onError(@Nullable final Throwable error, @Nonnull final Logger logger) {

            return null;
        }

        @Override
        public Execution onOutput(final INPUT output, @Nonnull final TimeDuration delay,
                @Nonnull final NestedQueue<INPUT> queue, @Nonnull final Logger logger) {

            throw exception(logger);
        }
    }
}
