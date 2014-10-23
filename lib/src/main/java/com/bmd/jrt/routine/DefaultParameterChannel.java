/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultExecution.InputIterator;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Default implementation of a parameter input channel.
 * <p/>
 * Created by davide on 9/24/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
class DefaultParameterChannel<INPUT, OUTPUT> implements ParameterChannel<INPUT, OUTPUT> {

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final DefaultExecution<INPUT, OUTPUT> mExecution;

    private final NestedQueue<INPUT> mInputQueue;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChanel;

    private final Runner mRunner;

    private Throwable mAbortException;

    private TimeDuration mInputDelay = ZERO;

    private int mPendingInputCount;

    private ChannelState mState = ChannelState.INPUT;

    /**
     * Constructor.
     *
     * @param manager       the invocation manager.
     * @param runner        the runner instance.
     * @param orderedInput  whether the input data are forced to be delivered in insertion order.
     * @param orderedOutput whether the output data are forced to be delivered in insertion order.
     * @param logger        the logger instance.
     * @throws NullPointerException if one of the parameters is null.
     */
    DefaultParameterChannel(@Nonnull final InvocationManager<INPUT, OUTPUT> manager,
            @Nonnull final Runner runner, final boolean orderedInput, final boolean orderedOutput,
            @Nonnull final Logger logger) {

        mLogger = logger.subContextLogger(this);
        mRunner = runner;
        mInputQueue =
                (orderedInput) ? new OrderedNestedQueue<INPUT>() : new SimpleNestedQueue<INPUT>();
        mResultChanel = new DefaultResultChannel<OUTPUT>(new AbortHandler() {

            @Override
            public void onAbort(final Throwable throwable, final long delay,
                    @Nonnull final TimeUnit timeUnit) {

                synchronized (mMutex) {

                    if (mState == ChannelState.EXCEPTION) {

                        mLogger.wrn("avoiding aborting result channel since invocation is aborted");

                        return;
                    }

                    mLogger.dbg("aborting result channel");

                    mInputQueue.clear();

                    mAbortException = throwable;
                    mState = ChannelState.EXCEPTION;
                }

                mRunner.run(mExecution.abort(), delay, timeUnit);
            }
        }, runner, orderedOutput, logger);
        mExecution = new DefaultExecution<INPUT, OUTPUT>(manager, new DefaultInputIterator(),
                                                         mResultChanel, logger);
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Override
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

                mInputQueue.clear();

                mAbortException = reason;
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

    @Override
    public boolean isOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ParameterChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            verifyInput();

            if (delay == null) {

                mLogger.err("invalid null delay");

                throw new NullPointerException("the input delay must not be null");
            }

            mInputDelay = delay;
        }

        return this;
    }

    @Override
    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> after(final long delay,
            @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> now() {

        return after(ZERO);
    }

    @Override
    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

        final TimeDuration delay;
        final DefaultOutputConsumer consumer;

        synchronized (mMutex) {

            verifyInput();

            if (channel == null) {

                mLogger.wrn("passing null channel");

                return this;
            }

            mLogger.dbg("passing channel [#%d]: %s", mPendingInputCount + 1, channel);

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingInputCount;

            consumer = new DefaultOutputConsumer(delay);
        }

        channel.bind(consumer);

        return this;
    }

    @Override
    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

        NestedQueue<INPUT> inputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                mLogger.wrn("passing null iterable");

                return this;
            }

            inputQueue = mInputQueue;
            delay = mInputDelay;

            mLogger.dbg("passing iterable [#%d]: %s [%s]", mPendingInputCount + 1, inputs, delay);

            if (delay.isZero()) {

                for (final INPUT input : inputs) {

                    inputQueue.add(input);
                }

            } else {

                inputQueue = inputQueue.addNested();
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedListInputExecution(inputQueue, inputs), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

        NestedQueue<INPUT> inputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            inputQueue = mInputQueue;
            delay = mInputDelay;

            mLogger.dbg("passing input [#%d]: %s [%s]", mPendingInputCount + 1, input, delay);

            if (delay.isZero()) {

                inputQueue.add(input);

            } else {

                inputQueue = inputQueue.addNested();
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedInputExecution(inputQueue, input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    @Nonnull
    public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                mLogger.wrn("passing null input array");

                return this;
            }
        }

        return pass(Arrays.asList(inputs));
    }

    @Override
    @Nonnull
    public OutputChannel<OUTPUT> results() {

        synchronized (mMutex) {

            verifyInput();

            mLogger.dbg("closing input channel");

            mState = ChannelState.OUTPUT;

            ++mPendingInputCount;
        }

        mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);

        return mResultChanel.getOutput();
    }

    private boolean isInputComplete() {

        synchronized (mMutex) {

            return (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
        }
    }

    private void verifyInput() {

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
    private static enum ChannelState {

        INPUT,      // input channel is open
        OUTPUT,     // no more input
        EXCEPTION   // abort issued
    }

    /**
     * Interface defining an object managing the creation and the recycling of invocation instances.
     *
     * @param <INPUT>  the input type.
     * @param <OUTPUT> the output type.
     */
    interface InvocationManager<INPUT, OUTPUT> {

        /**
         * Creates and returns a new invocation instance.
         *
         * @return the invocation instance.
         */
        @Nonnull
        public Invocation<INPUT, OUTPUT> create();

        /**
         * Discards the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        public void discard(@Nonnull Invocation<INPUT, OUTPUT> invocation);

        /**
         * Recycles the specified invocation.
         *
         * @param invocation the invocation instance.
         */
        public void recycle(@Nonnull Invocation<INPUT, OUTPUT> invocation);
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<INPUT> {

        @Override
        @Nullable
        public Throwable getAbortException() {

            synchronized (mMutex) {

                return mAbortException;
            }
        }

        @Override
        public boolean hasInput() {

            synchronized (mMutex) {

                return isInput() && !mInputQueue.isEmpty();
            }
        }

        @Override
        public boolean isAborting() {

            synchronized (mMutex) {

                return (mState == ChannelState.EXCEPTION);
            }
        }

        @Override
        public boolean isComplete() {

            return isInputComplete();
        }

        @Override
        public INPUT nextInput() throws NoSuchElementException {

            synchronized (mMutex) {

                if (!isInput()) {

                    mLogger.err("invalid read from close input");

                    throw new NoSuchElementException();
                }

                final INPUT input = mInputQueue.removeFirst();

                mLogger.dbg("reading input: %s", input);

                return input;
            }
        }

        @Override
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

        @Override
        public void onConsumeInput() {

            synchronized (mMutex) {

                if (!isInput()) {

                    mLogger.wrn("avoiding consuming input since invocation is aborted [#%d]",
                                mPendingInputCount);

                    return;
                }

                mLogger.dbg("consuming input [#%d]", mPendingInputCount);

                --mPendingInputCount;
            }
        }

        private boolean isInput() {

            return ((mState == ChannelState.INPUT) || (mState == ChannelState.OUTPUT));
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

        @Override
        public void onAbort(@Nullable final Throwable reason) {

            synchronized (mMutex) {

                if (!isOpen() && (mState != ChannelState.OUTPUT)) {

                    mSubLogger.wrn("avoiding aborting consumer since channel is closed");

                    return;
                }

                mSubLogger.dbg("aborting consumer");

                mInputQueue.clear();

                mAbortException = reason;
                mState = ChannelState.EXCEPTION;
            }

            final TimeDuration delay = mDelay;
            mRunner.run(mExecution.abort(), delay.time, delay.unit);
        }

        @Override
        public void onClose() {

            synchronized (mMutex) {

                mSubLogger.dbg("closing consumer");

                mQueue.close();
            }

            mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onOutput(final INPUT output) {

            NestedQueue<INPUT> inputQueue;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                final Throwable throwable = mAbortException;

                if (throwable != null) {

                    mSubLogger.dbg(throwable, "consumer abort exception");

                    throw RoutineExceptionWrapper.wrap(throwable).raise();
                }

                if (!isOpen() && (mState != ChannelState.OUTPUT)) {

                    mSubLogger.dbg("consumer invalid call on closed channel");

                    throw new IllegalStateException("the input channel is closed");
                }

                inputQueue = mQueue;

                mSubLogger.dbg("consumer input: %s [%s]", output, delay);

                if (delay.isZero()) {

                    inputQueue.add(output);

                } else {

                    inputQueue = inputQueue.addNested();
                }

                ++mPendingInputCount;
            }

            if (delay.isZero()) {

                mRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.run(new DelayedInputExecution(inputQueue, output), delay.time, delay.unit);
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
        private DelayedAbortExecution(final Throwable throwable) {

            mThrowable = throwable;
        }

        @Override
        public void run() {

            final Throwable throwable = mThrowable;

            synchronized (mMutex) {

                if (isInputComplete()) {

                    mLogger.dbg(throwable, "avoiding aborting since channel is closed");

                    return;
                }

                mLogger.dbg(throwable, "aborting channel");

                mInputQueue.clear();

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

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

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
         * @param inputs the iterable returning the input data.
         */
        private DelayedListInputExecution(@Nonnull final NestedQueue<INPUT> queue,
                @Nonnull final Iterable<? extends INPUT> inputs) {

            final ArrayList<INPUT> inputList = new ArrayList<INPUT>();

            for (final INPUT input : inputs) {

                inputList.add(input);
            }

            mInputs = inputList;
            mQueue = queue;
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

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