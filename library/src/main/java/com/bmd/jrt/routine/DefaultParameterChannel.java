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
import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultInvocation.InputIterator;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

    private final NestedQueue<INPUT> mInputQueue;

    private final DefaultInvocation<INPUT, OUTPUT> mInvocation;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChanel;

    private final Runner mRunner;

    private Throwable mAbortException;

    private ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private TimeDuration mInputDelay = ZERO;

    private int mPendingInputCount;

    private ChannelState mState = ChannelState.INPUT;

    /**
     * Constructor.
     *
     * @param provider      the execution provider.
     * @param runner        the runner instance.
     * @param orderedInput  whether the input data are forced to be delivered in insertion order.
     * @param orderedOutput whether the output data are forced to be delivered in insertion order.
     * @param logger        the logger.
     * @throws NullPointerException if one of the parameters is null.
     */
    DefaultParameterChannel(final ExecutionProvider<INPUT, OUTPUT> provider, final Runner runner,
            final boolean orderedInput, final boolean orderedOutput, final Logger logger) {

        mLogger = logger;
        mRunner = runner;
        mInputQueue =
                (orderedInput) ? new OrderedNestedQueue<INPUT>() : new SimpleNestedQueue<INPUT>();
        mResultChanel = new DefaultResultChannel<OUTPUT>(new AbortHandler() {

            @Override
            public void onAbort(final Throwable throwable) {

                synchronized (mMutex) {

                    if (mState == ChannelState.EXCEPTION) {

                        mLogger.wrn(
                                "%s - avoiding aborting result channel since invocation is aborted",
                                DefaultParameterChannel.this);

                        return;
                    }

                    mLogger.dbg("%s - aborting result channel", DefaultParameterChannel.this);

                    mInputQueue.clear();

                    mAbortException = throwable;
                    mState = ChannelState.EXCEPTION;
                }

                mRunner.runAbort(mInvocation);
            }
        }, runner, orderedOutput, logger);
        mInvocation = new DefaultInvocation<INPUT, OUTPUT>(provider, new DefaultInputIterator(),
                                                           mResultChanel, logger);
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Override
    public boolean abort(final Throwable throwable) {

        synchronized (mMutex) {

            if (!isOpen()) {

                mLogger.dbg(throwable, "%s - avoiding aborting since channel is closed", this);

                return false;
            }

            mLogger.dbg(throwable, "%s - aborting channel", this);

            mInputQueue.clear();

            mAbortException = throwable;
            mState = ChannelState.EXCEPTION;
        }

        mRunner.runAbort(mInvocation);

        return false;
    }

    @Override
    public boolean isOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> after(final TimeDuration delay) {

        synchronized (mMutex) {

            verifyInput();

            if (delay == null) {

                throw new IllegalArgumentException("the input delay must not be null");
            }

            mInputDelay = delay;
        }

        return this;
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> pass(final OutputChannel<INPUT> channel) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (channel == null) {

                mLogger.wrn("%s - passing null channel", this);

                return this;
            }

            mLogger.dbg("%s - passing channel [%d]: %s", this, mPendingInputCount + 1, channel);

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingInputCount;
        }

        channel.bind(new DefaultOutputConsumer(delay));

        return this;
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> pass(final Iterable<? extends INPUT> inputs) {

        NestedQueue<INPUT> inputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                mLogger.wrn("%s - passing null iterable", this);

                return this;
            }

            inputQueue = mInputQueue;
            delay = mInputDelay;

            mLogger.dbg("%s - passing iterable [%d][%s]: %s", this, mPendingInputCount + 1, delay,
                        inputs);

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

            mRunner.run(mInvocation, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedListInputInvocation(inputQueue, inputs), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> pass(final INPUT input) {

        NestedQueue<INPUT> inputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            inputQueue = mInputQueue;
            delay = mInputDelay;

            mLogger.dbg("%s - passing input [%d][%s]: %s", this, mPendingInputCount + 1, delay,
                        input);

            if (delay.isZero()) {

                inputQueue.add(input);

            } else {

                inputQueue = inputQueue.addNested();
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.run(mInvocation, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedInputInvocation(inputQueue, input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public ParameterChannel<INPUT, OUTPUT> pass(final INPUT... inputs) {

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                mLogger.wrn("%s - passing null input array", this);

                return this;
            }
        }

        return pass(Arrays.asList(inputs));
    }

    @Override
    public OutputChannel<OUTPUT> results() {

        synchronized (mMutex) {

            verifyInput();

            mLogger.dbg("%s - closing input channel", this);

            mState = ChannelState.OUTPUT;

            ++mPendingInputCount;
        }

        mRunner.run(mInvocation, 0, TimeUnit.MILLISECONDS);

        return mResultChanel.getOutput();
    }

    private void verifyInput() {

        final Throwable throwable = mAbortException;

        if (throwable != null) {

            mLogger.dbg(throwable, "%s - abort exception", this);

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isOpen()) {

            mLogger.err("%s - invalid call on closed channel", this);

            throw new IllegalStateException("the input channel is closed");
        }
    }

    /**
     * Enumeration identifying the channel internal state.
     */
    private static enum ChannelState {

        INPUT,
        OUTPUT,
        EXCEPTION
    }

    /**
     * Interface defining an object managing the creation and the recycling of execution instances.
     * <p/>
     * Created by davide on 9/24/14.
     *
     * @param <INPUT>  the input type.
     * @param <OUTPUT> the output type.
     */
    public interface ExecutionProvider<INPUT, OUTPUT> {

        /**
         * Creates and returns a new execution instance.
         *
         * @return the execution instance.
         */
        public Execution<INPUT, OUTPUT> create();

        /**
         * Discards the specified execution.
         *
         * @param execution the execution instance.
         */
        public void discard(Execution<INPUT, OUTPUT> execution);

        /**
         * Recycles the specified execution.
         *
         * @param execution the execution instance.
         */
        public void recycle(Execution<INPUT, OUTPUT> execution);
    }

    /**
     * Default implementation of an input iterator.
     */
    private class DefaultInputIterator implements InputIterator<INPUT> {

        @Override
        public Throwable getAbortException() {

            synchronized (mMutex) {

                return mAbortException;
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

            synchronized (mMutex) {

                return (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
            }
        }

        @Override
        public void onAbortComplete() {

            final Throwable exception;
            final ArrayList<OutputChannel<?>> channels;

            synchronized (mMutex) {

                exception = mAbortException;

                mLogger.dbg(exception, "%s - aborting bound channels [%d]",
                            DefaultParameterChannel.this, mBoundChannels.size());

                channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
                mBoundChannels.clear();
            }

            for (final OutputChannel<?> channel : channels) {

                channel.abort(exception);
            }
        }

        @Override
        public boolean onConsumeInput() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    mLogger.wrn("%s - avoiding consuming input since invocation is aborted [%d]",
                                DefaultParameterChannel.this, mPendingInputCount);

                    return false;
                }

                mLogger.dbg("%s - consuming input [%d]", DefaultParameterChannel.this,
                            mPendingInputCount);

                --mPendingInputCount;
            }

            return true;
        }

        @Override
        public boolean hasNext() {

            synchronized (mMutex) {

                return !mInputQueue.isEmpty();
            }
        }

        @Override
        public INPUT next() {

            synchronized (mMutex) {

                final INPUT input = mInputQueue.removeFirst();

                mLogger.dbg("%s - reading input: %s", DefaultParameterChannel.this, input);

                return input;
            }
        }

        @Override
        public void remove() {

            mLogger.err("%s - invalid input remove", DefaultParameterChannel.this);

            throw new UnsupportedOperationException();
        }
    }

    /**
     * Default implementation of an output consumer pushing the consume data into the input
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<INPUT> {

        private final TimeDuration mDelay;

        private final NestedQueue<INPUT> mQueue;

        /**
         * Constructor.
         *
         * @param delay the output delay.
         */
        public DefaultOutputConsumer(final TimeDuration delay) {

            mDelay = delay;
            mQueue = mInputQueue.addNested();
        }

        @Override
        public void onAbort(final Throwable throwable) {

            synchronized (mMutex) {

                if (!isOpen() && (mState != ChannelState.OUTPUT)) {

                    mLogger.wrn("%s - avoiding aborting consumer since channel is closed",
                                DefaultParameterChannel.this);

                    return;
                }

                mLogger.dbg("%s - aborting consumer", DefaultParameterChannel.this);

                mInputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mRunner.runAbort(mInvocation);
        }

        @Override
        public void onClose() {

            synchronized (mMutex) {

                mLogger.dbg("%s - closing consumer", DefaultParameterChannel.this);

                mQueue.close();
            }

            mRunner.run(mInvocation, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onOutput(final INPUT output) {

            NestedQueue<INPUT> inputQueue;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                final Throwable throwable = mAbortException;

                if (throwable != null) {

                    mLogger.dbg(throwable, "%s - consumer abort exception",
                                DefaultParameterChannel.this);

                    throw RoutineExceptionWrapper.wrap(throwable).raise();
                }

                if (!isOpen() && (mState != ChannelState.OUTPUT)) {

                    mLogger.dbg("%s - consumer invalid call on closed channel",
                                DefaultParameterChannel.this);

                    throw new IllegalStateException("the input channel is closed");
                }

                inputQueue = mQueue;

                mLogger.dbg("%s - consumer input [%s]: %s", DefaultParameterChannel.this, delay,
                            output);

                if (delay.isZero()) {

                    inputQueue.add(output);

                } else {

                    inputQueue = inputQueue.addNested();
                }

                ++mPendingInputCount;
            }

            if (delay.isZero()) {

                mRunner.run(mInvocation, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.run(new DelayedInputInvocation(inputQueue, output), delay.time, delay.unit);
            }
        }
    }

    /**
     * Implementation of an invocation handling a delayed input.
     */
    private class DelayedInputInvocation implements Invocation {

        private final INPUT mInput;

        private final NestedQueue<INPUT> mQueue;

        /**
         * Constructor.
         *
         * @param queue the input queue.
         * @param input the input.
         */
        public DelayedInputInvocation(final NestedQueue<INPUT> queue, final INPUT input) {

            mQueue = queue;
            mInput = input;
        }

        @Override
        public void abort() {

            mLogger.err("%s - invalid abort of delayed input invocation: %s",
                        DefaultParameterChannel.this, mInput);

            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    mLogger.dbg(
                            "%s - avoiding delayed input invocation since channel is closed: %s",
                            DefaultParameterChannel.this, mInput);

                    return;
                }

                mLogger.dbg("%s - delayed input invocation: %s", DefaultParameterChannel.this,
                            mInput);

                mQueue.add(mInput).close();
            }

            mInvocation.run();
        }
    }

    /**
     * Implementation of an invocation handling a delayed input of a list of data.
     */
    private class DelayedListInputInvocation implements Invocation {

        private final ArrayList<INPUT> mInputs;

        private final NestedQueue<INPUT> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the input queue.
         * @param inputs the iterable returning the input data.
         */
        public DelayedListInputInvocation(final NestedQueue<INPUT> queue,
                final Iterable<? extends INPUT> inputs) {

            final ArrayList<INPUT> inputList = new ArrayList<INPUT>();

            for (final INPUT input : inputs) {

                inputList.add(input);
            }

            mInputs = inputList;
            mQueue = queue;
        }

        @Override
        public void abort() {

            mLogger.err("%s - invalid abort of delayed input invocation: %s",
                        DefaultParameterChannel.this, mInputs);

            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    mLogger.dbg(
                            "%s - avoiding delayed input invocation since channel is closed: %s",
                            DefaultParameterChannel.this, mInputs);

                    return;
                }

                mLogger.dbg("%s - delayed input invocation: %s", DefaultParameterChannel.this,
                            mInputs);

                mQueue.addAll(mInputs).close();
            }

            mInvocation.run();
        }
    }
}