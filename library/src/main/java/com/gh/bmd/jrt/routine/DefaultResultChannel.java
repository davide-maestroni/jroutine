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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.OutputDeadlockException;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.ReadDeadlockException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;
import com.gh.bmd.jrt.time.TimeDuration.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.ZERO;
import static com.gh.bmd.jrt.time.TimeDuration.fromUnit;

/**
 * Class handling the routine output.
 * <p/>
 * This class centralizes the managing of data passing through the routine output and result
 * channels, since, logically, the two objects are part of the same entity. In fact, on one end the
 * result channel puts data into the output queue and, on the other end, the output channel reads
 * them from the same queue.
 * <p/>
 * Created by davide on 9/24/14.
 *
 * @param <OUTPUT> the output data type.
 */
class DefaultResultChannel<OUTPUT> implements ResultChannel<OUTPUT> {

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final Object mConsumerMutex = new Object();

    private final AbortHandler mHandler;

    private final Check mHasOutputs;

    private final Logger mLogger;

    private final int mMaxOutput;

    private final Object mMutex = new Object();

    private final TimeDuration mOutputTimeout;

    private final TimeDuration mReadTimeout;

    private final Runner mRunner;

    private final TimeoutAction mTimeoutAction;

    private Throwable mAbortException;

    private boolean mIsException;

    private OutputConsumer<OUTPUT> mOutputConsumer;

    private int mOutputCount;

    private Check mOutputHasNext;

    private Check mOutputNotEmpty;

    private NestedQueue<Object> mOutputQueue;

    private int mPendingOutputCount;

    private TimeDuration mResultDelay = ZERO;

    private ChannelState mState = ChannelState.OUTPUT;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @param handler       the abort handler.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     * @throws java.lang.NullPointerException if one of the parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultResultChannel(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final AbortHandler handler, @Nonnull final Runner runner,
            @Nonnull final Logger logger) {

        if (handler == null) {

            throw new NullPointerException("the abort handler must not be null");
        }

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mLogger = logger.subContextLogger(this);
        mHandler = handler;
        mRunner = runner;
        mReadTimeout = configuration.getReadTimeoutOr(ZERO);
        mTimeoutAction = configuration.getReadTimeoutActionOr(TimeoutAction.DEADLOCK);
        mMaxOutput = configuration.getOutputSizeOr(Integer.MAX_VALUE);
        mOutputTimeout = configuration.getOutputTimeoutOr(ZERO);
        mOutputQueue = (configuration.getOutputOrderOr(OrderType.DELIVERY) == OrderType.DELIVERY)
                ? new SimpleNestedQueue<Object>() : new OrderedNestedQueue<Object>();

        final int maxOutputSize = mMaxOutput;
        mHasOutputs = new Check() {

            @Override
            public boolean isTrue() {

                return (mOutputCount <= maxOutputSize);
            }
        };
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ResultChannel<OUTPUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            verifyOutput();

            if (delay == null) {

                mLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mResultDelay = delay;
        }

        return this;
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> now() {

        return after(ZERO);
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<OUTPUT> channel) {

        final TimeDuration delay;
        final DefaultOutputConsumer consumer;

        synchronized (mMutex) {

            verifyOutput();

            if (channel == null) {

                mLogger.wrn("passing null channel");
                return this;
            }

            mBoundChannels.add(channel);
            delay = mResultDelay;
            ++mPendingOutputCount;
            mLogger.dbg("passing channel: %s", channel);

            consumer = new DefaultOutputConsumer(delay);
        }

        channel.bind(consumer);

        return this;
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

        NestedQueue<Object> outputQueue;
        ArrayList<OUTPUT> list = null;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                mLogger.wrn("passing null iterable");
                return this;
            }

            outputQueue = mOutputQueue;
            delay = mResultDelay;

            int count = 0;

            if (delay.isZero()) {

                for (final OUTPUT output : outputs) {

                    outputQueue.add(output);
                    ++count;
                }

            } else {

                outputQueue = outputQueue.addNested();
                list = new ArrayList<OUTPUT>();

                for (final OUTPUT output : outputs) {

                    list.add(output);
                }

                count = list.size();
                ++mPendingOutputCount;
            }

            mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mOutputCount, count, outputs, delay);
            addOutputs(count);
        }

        if (delay.isZero()) {

            flushOutput(false);

        } else {

            mRunner.run(new DelayedListOutputExecution(outputQueue, list), delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT output) {

        NestedQueue<Object> outputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            outputQueue = mOutputQueue;
            delay = mResultDelay;

            if (delay.isZero()) {

                outputQueue.add(output);

            } else {

                outputQueue = outputQueue.addNested();
                ++mPendingOutputCount;
            }

            mLogger.dbg("passing output [#%d+1]: %s [%s]", mOutputCount, output, delay);
            addOutputs(1);
        }

        if (delay.isZero()) {

            flushOutput(false);

        } else {

            mRunner.run(new DelayedOutputExecution(outputQueue, output), delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT... outputs) {

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                mLogger.wrn("passing null output array");
                return this;
            }
        }

        return pass(Arrays.asList(outputs));
    }

    /**
     * Aborts immediately the execution.
     *
     * @param throwable the reason of the abortion.
     * @see com.gh.bmd.jrt.channel.Channel#abort(Throwable)
     */
    void abortImmediately(@Nullable final Throwable throwable) {

        abort(throwable, true);
    }

    /**
     * Closes this channel with the specified exception.
     *
     * @param throwable the exception.
     */
    void close(@Nullable final Throwable throwable) {

        final ArrayList<OutputChannel<?>> channels;

        synchronized (mMutex) {

            mLogger.dbg(throwable, "aborting result channel");

            channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();
            mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));
            mIsException = true;

            if (mAbortException == null) {

                mAbortException = throwable;
            }

            mState = ChannelState.ABORTED;
            mMutex.notifyAll();
        }

        for (final OutputChannel<?> channel : channels) {

            channel.abort(throwable);
        }

        flushOutput(false);
    }

    /**
     * Closes this channel successfully.
     */
    void close() {

        boolean isFlush = false;

        synchronized (mMutex) {

            mLogger.dbg("closing result channel [#%d]", mPendingOutputCount);

            if (mState == ChannelState.OUTPUT) {

                isFlush = true;

                if (mPendingOutputCount > 0) {

                    mState = ChannelState.RESULT;

                } else {

                    mState = ChannelState.FLUSH;
                }

            } else {

                mLogger.dbg("avoiding closing result channel since already closed");
            }
        }

        if (isFlush) {

            flushOutput(false);
        }
    }

    /**
     * Returns the output channel reading the data pushed into this channel.
     *
     * @return the output channel.
     */
    @Nonnull
    OutputChannel<OUTPUT> getOutput() {

        final TimeoutAction action = mTimeoutAction;
        final OutputChannel<OUTPUT> outputChannel =
                new DefaultOutputChannel().afterMax(mReadTimeout);

        if (action == TimeoutAction.EXIT) {

            outputChannel.eventuallyExit();

        } else if (action == TimeoutAction.ABORT) {

            outputChannel.eventuallyAbort();
        }

        return outputChannel;
    }

    private boolean abort(@Nullable final Throwable throwable, final boolean isImmediate) {

        final TimeDuration delay;

        synchronized (mMutex) {

            if (isResultComplete()) {

                mLogger.dbg(throwable, "avoiding aborting since channel is closed");
                return false;
            }

            delay = (isImmediate) ? ZERO : mResultDelay;

            if (delay.isZero()) {

                mLogger.dbg(throwable, "aborting channel");
                mOutputQueue.clear();
                mIsException = true;
                mAbortException = (isImmediate || (throwable instanceof AbortException)) ? throwable
                        : new AbortException(throwable);
                mState = ChannelState.EXCEPTION;
            }
        }

        if (delay.isZero()) {

            mHandler.onAbort(throwable, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedAbortExecution(throwable), delay.time, delay.unit);
        }

        return true;
    }

    private void addOutputs(final int count) {

        mOutputCount += count;

        try {

            if (!mOutputTimeout.waitTrue(mMutex, mHasOutputs)) {

                throw new OutputDeadlockException(
                        "deadlock while waiting for room in the output channel");
            }

        } catch (final InterruptedException e) {

            throw InvocationInterruptedException.interrupt(e);
        }
    }

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
                        justification = "cannot be called if output consumer is null")
    private void closeConsumer(final ChannelState state) {

        if (state != ChannelState.ABORTED) {

            final Logger logger = mLogger;
            final OutputConsumer<OUTPUT> consumer = mOutputConsumer;

            try {

                logger.dbg("closing consumer (%s)", consumer);
                consumer.onComplete();

            } catch (final InvocationInterruptedException e) {

                throw e;

            } catch (final Throwable t) {

                logger.wrn(t, "ignoring consumer exception (%s)", consumer);
            }
        }

        synchronized (mMutex) {

            if (!isOutputPending(mState)) {

                mState = ChannelState.DONE;
                mMutex.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void flushOutput(final boolean forceClose) {

        Throwable abortException = null;

        synchronized (mConsumerMutex) {

            final Logger logger = mLogger;
            final ArrayList<Object> outputs;
            final OutputConsumer<OUTPUT> consumer;
            final ChannelState state;

            synchronized (mMutex) {

                consumer = mOutputConsumer;

                if (consumer == null) {

                    logger.dbg("avoiding flushing output since channel is not bound");

                    if (!isOutputPending(mState)) {

                        mState = ChannelState.DONE;
                    }

                    mMutex.notifyAll();
                    return;
                }

                outputs = new ArrayList<Object>();
                mOutputQueue.moveTo(outputs);
                state = mState;
                mOutputCount = 0;
                mMutex.notifyAll();
            }

            try {

                for (final Object output : outputs) {

                    if (output instanceof RoutineExceptionWrapper) {

                        try {

                            logger.dbg("aborting consumer (%s): %s", consumer, output);
                            consumer.onError(((RoutineExceptionWrapper) output).getCause());

                        } catch (final InvocationInterruptedException e) {

                            throw e;

                        } catch (final Throwable t) {

                            logger.wrn(t, "ignoring consumer exception (%s)", consumer);
                        }

                        break;

                    } else {

                        logger.dbg("output consumer (%s): %s", consumer, output);
                        consumer.onOutput((OUTPUT) output);
                    }
                }

                if (forceClose || !isOutputPending(state)) {

                    closeConsumer(state);
                }

            } catch (final InvocationInterruptedException e) {

                throw e;

            } catch (final Throwable t) {

                boolean isClose = false;
                final ChannelState finalState;

                synchronized (mMutex) {

                    logger.wrn(t, "consumer exception (%s)", mOutputConsumer);
                    finalState = mState;

                    if (forceClose || !isOutputPending(finalState)) {

                        isClose = true;

                    } else if (finalState != ChannelState.EXCEPTION) {

                        logger.wrn(t, "aborting on consumer exception (%s)", mOutputConsumer);
                        abortException = t;

                        mOutputQueue.clear();
                        mIsException = true;
                        mAbortException = t;
                        mState = ChannelState.EXCEPTION;
                    }
                }

                if (isClose) {

                    closeConsumer(finalState);
                }
            }
        }

        if (abortException != null) {

            mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isOutputOpen() {

        synchronized (mMutex) {

            return !mOutputQueue.isEmpty() || (mState != ChannelState.DONE);
        }
    }

    private boolean isOutputPending(@Nonnull final ChannelState state) {

        return (state != ChannelState.FLUSH) && (state != ChannelState.ABORTED);
    }

    private boolean isResultComplete() {

        return (mState.ordinal() >= ChannelState.FLUSH.ordinal());
    }

    private boolean isResultOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.OUTPUT);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private OUTPUT nextOutput(@Nonnull final TimeDuration timeout) {

        final Object result = mOutputQueue.removeFirst();
        mLogger.dbg("reading output [#%d]: %s [%s]", mOutputCount, result, timeout);

        RoutineExceptionWrapper.raise(result);

        final int maxOutput = mMaxOutput;
        final int prevOutputCount = mOutputCount;

        if ((--mOutputCount < maxOutput) && (prevOutputCount >= maxOutput)) {

            mMutex.notifyAll();
        }

        return (OUTPUT) result;
    }

    @Nullable
    private OUTPUT readQueue(@Nonnull final TimeDuration timeout,
            @Nonnull final TimeoutAction action) {

        verifyBound();

        final Logger logger = mLogger;
        final NestedQueue<Object> outputQueue = mOutputQueue;

        if (timeout.isZero() || !outputQueue.isEmpty()) {

            if (outputQueue.isEmpty()) {

                logger.wrn("reading output timeout: [%s] => [%s]", timeout, action);

                if (action == TimeoutAction.DEADLOCK) {

                    throw new ReadDeadlockException("deadlock while waiting for outputs");
                }
            }

            return nextOutput(timeout);
        }

        if (mOutputNotEmpty == null) {

            mOutputNotEmpty = new Check() {

                @Override
                public boolean isTrue() {

                    return !outputQueue.isEmpty();
                }
            };
        }

        final boolean isTimeout;

        try {

            isTimeout = !timeout.waitTrue(mMutex, mOutputNotEmpty);

        } catch (final InterruptedException e) {

            throw InvocationInterruptedException.interrupt(e);
        }

        if (isTimeout) {

            logger.wrn("reading output timeout: [%s] => [%s]", timeout, action);

            if (action == TimeoutAction.DEADLOCK) {

                throw new ReadDeadlockException("deadlock while waiting for outputs");
            }
        }

        return nextOutput(timeout);
    }

    private void verifyBound() {

        if (mOutputConsumer != null) {

            mLogger.err("invalid call on bound channel");
            throw new IllegalStateException("the channel is already bound");
        }
    }

    private void verifyOutput() {

        if (mIsException) {

            final Throwable throwable = mAbortException;
            mLogger.dbg(throwable, "abort exception");
            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isResultOpen()) {

            mLogger.err("invalid call on closed channel");
            throw new IllegalStateException("the channel is closed");
        }
    }

    /**
     * Enumeration identifying the channel internal state.
     */
    private static enum ChannelState {

        OUTPUT,     // result channel is open
        RESULT,     // result channel is closed
        FLUSH,      // no more pending outputs
        EXCEPTION,  // abort issued
        ABORTED,    // invocation aborted
        DONE        // output is closed
    }

    /**
     * Interface defining an abort handler.
     */
    public interface AbortHandler {

        /**
         * Called on an abort.
         *
         * @param reason   the reason of the abortion.
         * @param delay    the abortion delay.
         * @param timeUnit the delay time unit.
         */
        public void onAbort(@Nullable Throwable reason, long delay, @Nonnull TimeUnit timeUnit);
    }

    /**
     * Default implementation of an output channel iterator.
     */
    private class DefaultIterator implements Iterator<OUTPUT> {

        private final TimeoutAction mAction;

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private final TimeDuration mTimeout;

        private boolean mRemoved = true;

        /**
         * Constructor.
         *
         * @param timeout the output timeout.
         * @param action  the timeout action.
         */
        private DefaultIterator(@Nonnull final TimeDuration timeout,
                @Nonnull final TimeoutAction action) {

            mTimeout = timeout;
            mAction = action;
        }

        @Override
        public boolean hasNext() {

            boolean isAbort = false;

            synchronized (mMutex) {

                verifyBound();

                final Logger logger = mSubLogger;
                final TimeDuration timeout = mTimeout;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (timeout.isZero() || (mState == ChannelState.DONE)) {

                    final boolean hasNext = !outputQueue.isEmpty();

                    if (!hasNext && (mState != ChannelState.DONE)) {

                        final TimeoutAction action = mAction;
                        logger.wrn("has output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutAction.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to know if more outputs are coming");

                        } else {

                            isAbort = (action == TimeoutAction.ABORT);
                        }
                    }

                } else {

                    if (mOutputHasNext == null) {

                        mOutputHasNext = new Check() {

                            @Override
                            public boolean isTrue() {

                                return !outputQueue.isEmpty() || (mState == ChannelState.DONE);
                            }
                        };
                    }

                    final boolean isTimeout;

                    try {

                        isTimeout = !timeout.waitTrue(mMutex, mOutputHasNext);

                    } catch (final InterruptedException e) {

                        throw InvocationInterruptedException.interrupt(e);
                    }

                    if (isTimeout) {

                        final TimeoutAction action = mAction;
                        logger.wrn("has output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutAction.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to know if more outputs are coming");

                        } else {

                            isAbort = (action == TimeoutAction.ABORT);
                        }
                    }
                }

                if (!isAbort) {

                    final boolean hasNext = !outputQueue.isEmpty();
                    logger.dbg("has output: %s [%s]", hasNext, timeout);
                    return hasNext;
                }
            }

            abort();
            throw new AbortException(null);
        }

        @Nullable
        @Override
        @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT",
                            justification = "NestedQueue.removeFirst() actually throws it")
        public OUTPUT next() {

            boolean isAbort = false;

            try {

                synchronized (mMutex) {

                    final TimeoutAction action = mAction;
                    isAbort = (action == TimeoutAction.ABORT);

                    final OUTPUT next = readQueue(mTimeout, action);
                    mRemoved = false;
                    return next;
                }

            } catch (final NoSuchElementException e) {

                if (isAbort) {

                    abort();
                    throw new AbortException(null);
                }

                throw e;
            }
        }

        @Override
        public void remove() {

            synchronized (mMutex) {

                verifyBound();

                if (mRemoved) {

                    mSubLogger.err("invalid output remove");
                    throw new IllegalStateException("the element has been already removed");
                }

                mRemoved = true;
            }
        }
    }

    /**
     * Default implementation of an routine output channel.
     */
    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private TimeDuration mReadTimeout = ZERO;

        private TimeoutAction mTimeoutAction = TimeoutAction.DEADLOCK;

        @Nonnull
        @Override
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            synchronized (mMutex) {

                if (timeout == null) {

                    mSubLogger.err("invalid null timeout");
                    throw new NullPointerException("the output timeout must not be null");
                }

                mReadTimeout = timeout;
            }

            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return afterMax(fromUnit(timeout, timeUnit));
        }

        @Nonnull
        @Override
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> bind(@Nonnull final OutputConsumer<OUTPUT> consumer) {

            final boolean forceClose;
            final ChannelState state;

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    mSubLogger.err("invalid null consumer");
                    throw new NullPointerException("the output consumer must not be null");
                }

                state = mState;
                forceClose = (state == ChannelState.DONE);
                mOutputConsumer = consumer;
            }

            flushOutput(forceClose);

            return this;
        }

        @Override
        public boolean checkComplete() {

            final boolean isDone;

            synchronized (mMutex) {

                final TimeDuration timeout = mReadTimeout;

                try {

                    isDone = timeout.waitTrue(mMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return (mState == ChannelState.DONE);
                        }
                    });

                } catch (final InterruptedException e) {

                    throw InvocationInterruptedException.interrupt(e);
                }

                if (!isDone) {

                    mSubLogger.wrn("waiting complete timeout: [%s]", timeout);
                }
            }

            return isDone;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> eventually() {

            return afterMax(INFINITY);
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> eventuallyAbort() {

            synchronized (mMutex) {

                mTimeoutAction = TimeoutAction.ABORT;
            }

            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> eventuallyDeadlock() {

            synchronized (mMutex) {

                mTimeoutAction = TimeoutAction.DEADLOCK;
            }

            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> eventuallyExit() {

            synchronized (mMutex) {

                mTimeoutAction = TimeoutAction.EXIT;
            }

            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> immediately() {

            return afterMax(ZERO);
        }

        @Override
        public boolean isBound() {

            synchronized (mMutex) {

                return (mOutputConsumer != null);
            }
        }

        @Nonnull
        @Override
        public List<OUTPUT> readAll() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            readAllInto(results);
            return results;
        }

        @Nonnull
        @Override
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        public OutputChannel<OUTPUT> readAllInto(
                @Nonnull final Collection<? super OUTPUT> results) {

            boolean isAbort = false;

            synchronized (mMutex) {

                verifyBound();

                final Logger logger = mSubLogger;

                if (results == null) {

                    logger.err("invalid null output list");
                    throw new NullPointerException("the result list must not be null");
                }

                final NestedQueue<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mReadTimeout;

                if (timeout.isZero() || (mState == ChannelState.DONE)) {

                    while (!outputQueue.isEmpty()) {

                        final OUTPUT result = nextOutput(timeout);
                        logger.dbg("adding output to list: %s [%s]", result, timeout);
                        results.add(result);
                    }

                    if (mState != ChannelState.DONE) {

                        final TimeoutAction action = mTimeoutAction;
                        logger.wrn("list output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutAction.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to collect all outputs");

                        } else {

                            isAbort = (action == TimeoutAction.ABORT);
                        }
                    }

                } else {

                    final long startTime = System.currentTimeMillis();
                    final boolean isTimeout;

                    try {

                        while (timeout.waitSinceMillis(mMutex, startTime)) {

                            while (!outputQueue.isEmpty()) {

                                final OUTPUT result = nextOutput(timeout);
                                logger.dbg("adding output to list: %s [%s]", result, timeout);
                                results.add(result);
                            }

                            if (mState == ChannelState.DONE) {

                                break;
                            }
                        }

                        isTimeout = (mState != ChannelState.DONE);

                    } catch (final InterruptedException e) {

                        throw InvocationInterruptedException.interrupt(e);
                    }

                    if (isTimeout) {

                        final TimeoutAction action = mTimeoutAction;
                        logger.wrn("list output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutAction.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to collect all outputs");

                        } else {

                            isAbort = (action == TimeoutAction.ABORT);
                        }
                    }
                }
            }

            if (isAbort) {

                abort();
                throw new AbortException(null);
            }

            return this;
        }

        @Override
        public OUTPUT readNext() {

            boolean isAbort = false;

            try {

                synchronized (mMutex) {

                    final TimeoutAction action = mTimeoutAction;
                    isAbort = (action == TimeoutAction.ABORT);

                    return readQueue(mReadTimeout, action);
                }

            } catch (final NoSuchElementException e) {

                if (isAbort) {

                    abort();
                    throw new AbortException(null);
                }

                throw e;
            }
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> unbind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            synchronized (mMutex) {

                if (mOutputConsumer == consumer) {

                    mOutputConsumer = null;
                }
            }

            return this;
        }

        @Nonnull
        @Override
        public Iterator<OUTPUT> iterator() {

            final TimeDuration timeout;
            final TimeoutAction action;

            synchronized (mMutex) {

                verifyBound();

                timeout = mReadTimeout;
                action = mTimeoutAction;
            }

            return new DefaultIterator(timeout, action);
        }

        @Override
        public boolean abort() {

            return abort(null);
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            synchronized (mMutex) {

                if (isResultComplete()) {

                    mSubLogger.dbg("avoiding aborting output since result channel is closed");
                    return false;
                }

                mSubLogger.dbg(reason, "aborting output");
                mOutputQueue.clear();
                mIsException = true;
                mAbortException =
                        (reason instanceof AbortException) ? reason : new AbortException(reason);
                mState = ChannelState.EXCEPTION;
            }

            mHandler.onAbort(reason, 0, TimeUnit.MILLISECONDS);

            return true;
        }

        @Override
        public boolean isOpen() {

            return isOutputOpen();
        }
    }

    /**
     * Default implementation of an output consumer pushing the consume data into the output
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<OUTPUT> {

        private final TimeDuration mDelay;

        private final NestedQueue<Object> mQueue;

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Constructor.
         *
         * @param delay the output delay.
         */
        private DefaultOutputConsumer(@Nonnull final TimeDuration delay) {

            mDelay = delay;
            mQueue = mOutputQueue.addNested();
        }

        @Override
        public void onComplete() {

            boolean isFlush = false;

            synchronized (mMutex) {

                verifyComplete();

                mQueue.close();

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;
                    isFlush = true;

                } else {

                    mMutex.notifyAll();
                }

                mSubLogger.dbg("closing output [%s]", isFlush);
            }

            if (isFlush) {

                flushOutput(false);
            }
        }

        @Override
        public void onError(@Nullable final Throwable error) {

            synchronized (mMutex) {

                if (isResultComplete()) {

                    mSubLogger.dbg("avoiding aborting output since result channel is closed");
                    return;
                }

                mSubLogger.dbg(error, "aborting output");
                mOutputQueue.clear();
                mIsException = true;
                mAbortException = error;
                mState = ChannelState.EXCEPTION;
            }

            final TimeDuration delay = mDelay;
            mHandler.onAbort(error, delay.time, delay.unit);
        }

        @Override
        public void onOutput(final OUTPUT output) {

            NestedQueue<Object> outputQueue;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                verifyComplete();

                outputQueue = mQueue;

                if (delay.isZero()) {

                    outputQueue.add(output);

                } else {

                    outputQueue = outputQueue.addNested();
                    ++mPendingOutputCount;
                }

                mSubLogger.dbg("consumer output [#%d+1]: %s [%s]", mOutputCount, output, delay);
                addOutputs(1);
            }

            if (delay.isZero()) {

                flushOutput(false);

            } else {

                mRunner.run(new DelayedOutputExecution(outputQueue, output), delay.time,
                            delay.unit);
            }
        }

        private void verifyComplete() {

            if (mIsException) {

                final Throwable throwable = mAbortException;
                mSubLogger.dbg(throwable, "consumer abort exception");
                throw RoutineExceptionWrapper.wrap(throwable).raise();
            }

            if (isResultComplete()) {

                mSubLogger.err("consumer invalid call on closed channel");
                throw new IllegalStateException("the channel is closed");
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

        @Override
        public void run() {

            final Throwable throwable = mThrowable;

            synchronized (mMutex) {

                if (!isOutputOpen()) {

                    mLogger.dbg(throwable, "avoiding aborting since channel is closed");
                    return;
                }

                mLogger.dbg(throwable, "aborting channel");
                mOutputQueue.clear();
                mIsException = true;
                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mHandler.onAbort(throwable, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Implementation of an execution handling a delayed output of a list of data.
     */
    private class DelayedListOutputExecution implements Execution {

        private final ArrayList<OUTPUT> mOutputs;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue   the output queue.
         * @param outputs the list of output data.
         */
        private DelayedListOutputExecution(@Nonnull final NestedQueue<Object> queue,
                final ArrayList<OUTPUT> outputs) {

            mOutputs = outputs;
            mQueue = queue;
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isResultComplete()) {

                    mLogger.dbg("avoiding delayed output execution since channel is closed: %s",
                                mOutputs);
                    return;
                }

                mLogger.dbg("delayed output execution: %s", mOutputs);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;
                }

                mQueue.addAll(mOutputs).close();
            }

            flushOutput(false);
        }
    }

    /**
     * Implementation of an execution handling a delayed output.
     */
    private class DelayedOutputExecution implements Execution {

        private final OUTPUT mOutput;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the output queue.
         * @param output the output.
         */
        private DelayedOutputExecution(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mQueue = queue;
            mOutput = output;
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isResultComplete()) {

                    mLogger.dbg("avoiding delayed output execution since channel is closed: %s",
                                mOutput);
                    return;
                }

                mLogger.dbg("delayed output execution: %s", mOutput);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;
                }

                mQueue.add(mOutput).close();
            }

            flushOutput(false);
        }
    }

    @Override
    public boolean abort(@Nullable final Throwable reason) {

        return abort(reason, false);
    }

    @Override
    public boolean isOpen() {

        return isResultOpen();
    }
}
