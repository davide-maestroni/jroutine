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
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;

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

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.fromUnit;
import static com.bmd.jrt.time.TimeDuration.seconds;

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
 * @param <OUTPUT> the output type.
 */
class DefaultResultChannel<OUTPUT> implements ResultChannel<OUTPUT> {

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final Object mConsumerMutex = new Object();

    private final AbortHandler mHandler;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final Runner mRunner;

    private Throwable mAbortException;

    private OutputConsumer<OUTPUT> mOutputConsumer;

    private Check mOutputHasNext;

    private Check mOutputNotEmpty;

    private NestedQueue<Object> mOutputQueue;

    private TimeDuration mOutputTimeout = seconds(5);

    private RuntimeException mOutputTimeoutException;

    private int mPendingOutputCount;

    private TimeDuration mResultDelay = ZERO;

    private ChannelState mState = ChannelState.OUTPUT;

    /**
     * Constructor.
     *
     * @param handler       the abort handler.
     * @param runner        the runner instance.
     * @param orderedOutput whether the output data are forced to be delivered in insertion order.
     * @param logger        the logger instance.
     * @throws NullPointerException if one of the parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultResultChannel(@Nonnull final AbortHandler handler, @Nonnull final Runner runner,
            final boolean orderedOutput, @Nonnull final Logger logger) {

        if (handler == null) {

            throw new NullPointerException("the abort handler must not be null");
        }

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mHandler = handler;
        mRunner = runner;
        mLogger = logger.subContextLogger(this);
        mOutputQueue = (orderedOutput) ? new OrderedNestedQueue<Object>()
                : new SimpleNestedQueue<Object>();
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    /**
     * Aborts immediately the execution.
     *
     * @param throwable the reason of the abortion.
     * @see com.bmd.jrt.channel.Channel#abort(Throwable)
     */
    public void abortImmediately(@Nullable final Throwable throwable) {

        abort(throwable, true);
    }

    @Override
    @Nonnull
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

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    @Override
    public ResultChannel<OUTPUT> now() {

        return after(ZERO);
    }

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<OUTPUT> channel) {

        final TimeDuration delay;
        final DefaultOutputConsumer consumer;

        synchronized (mMutex) {

            verifyOutput();

            if (channel == null) {

                mLogger.wrn("passing null channel");

                return this;
            }

            mLogger.dbg("passing channel [#%d]: %s", mPendingOutputCount + 1, channel);

            mBoundChannels.add(channel);

            delay = mResultDelay;

            ++mPendingOutputCount;

            consumer = new DefaultOutputConsumer(delay);
        }

        channel.bind(consumer);

        return this;
    }

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

        NestedQueue<Object> outputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                mLogger.wrn("passing null iterable");

                return this;
            }

            outputQueue = mOutputQueue;
            delay = mResultDelay;

            mLogger.dbg("passing iterable [#%d]: %s [%s]", mPendingOutputCount + 1, outputs, delay);

            if (delay.isZero()) {

                for (final OUTPUT output : outputs) {

                    outputQueue.add(output);
                }

            } else {

                outputQueue = outputQueue.addNested();

                ++mPendingOutputCount;
            }
        }

        if (delay.isZero()) {

            flushOutput();

        } else {

            mRunner.run(new DelayedListOutputExecution(outputQueue, outputs), delay.time,
                        delay.unit);
        }

        return this;
    }

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT output) {

        NestedQueue<Object> outputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            outputQueue = mOutputQueue;
            delay = mResultDelay;

            mLogger.dbg("passing output [#%d]: %s [%s]", mPendingOutputCount + 1, output, delay);

            if (delay.isZero()) {

                outputQueue.add(output);

            } else {

                outputQueue = outputQueue.addNested();

                ++mPendingOutputCount;
            }
        }

        if (delay.isZero()) {

            flushOutput();

        } else {

            mRunner.run(new DelayedOutputExecution(outputQueue, output), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    @Nonnull
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
     * Closes this channel with the specified exception.
     *
     * @param throwable the exception.
     */
    public void close(@Nullable final Throwable throwable) {

        final ArrayList<OutputChannel<?>> channels;

        synchronized (mMutex) {

            mLogger.dbg(throwable, "aborting result channel");

            channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();

            mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

            mAbortException = throwable;
            mState = ChannelState.ABORTED;
            mMutex.notifyAll();
        }

        for (final OutputChannel<?> channel : channels) {

            channel.abort(throwable);
        }

        flushOutput();
    }

    /**
     * Closes this channel successfully.
     */
    public void close() {

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

            flushOutput();
        }
    }

    /**
     * Returns the output channel reading the data pushed into this channel.
     *
     * @return the output channel.
     */
    @Nonnull
    public OutputChannel<OUTPUT> getOutput() {

        return new DefaultOutputChannel();
    }

    private boolean abort(@Nullable final Throwable throwable, final boolean isImmediate) {

        final TimeDuration delay;

        synchronized (mMutex) {

            if (!isResultOpen()) {

                mLogger.dbg(throwable, "avoiding aborting since channel is closed");

                return false;
            }

            delay = (isImmediate) ? ZERO : mResultDelay;

            if (delay.isZero()) {

                mLogger.dbg(throwable, "aborting channel");

                mOutputQueue.clear();

                mAbortException = throwable;
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

    private void closeConsumer() {

        synchronized (mConsumerMutex) {

            final Logger logger = mLogger;
            final OutputConsumer<OUTPUT> consumer = mOutputConsumer;

            try {

                logger.dbg("closing consumer (%s)", consumer);

                consumer.onClose();

            } catch (final RoutineInterruptedException e) {

                throw e;

            } catch (final Throwable t) {

                logger.wrn(t, "ignoring consumer exception (%s)", consumer);
            }

            synchronized (mMutex) {

                if ((mState == ChannelState.FLUSH) || (mState == ChannelState.ABORTED)) {

                    mState = ChannelState.DONE;

                    mMutex.notifyAll();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void flushOutput() {

        Throwable abortException = null;

        synchronized (mConsumerMutex) {

            final Logger logger = mLogger;

            try {

                final ArrayList<Object> outputs;
                final OutputConsumer<OUTPUT> consumer;
                final ChannelState state;

                synchronized (mMutex) {

                    consumer = mOutputConsumer;

                    if (consumer == null) {

                        logger.dbg("avoiding flushing output since channel is not bound");

                        if ((mState == ChannelState.FLUSH) || (mState == ChannelState.ABORTED)) {

                            mState = ChannelState.DONE;
                        }

                        mMutex.notifyAll();

                        return;
                    }

                    outputs = new ArrayList<Object>();
                    mOutputQueue.moveTo(outputs);
                    state = mState;

                    mMutex.notifyAll();
                }

                for (final Object output : outputs) {

                    if (output instanceof RoutineExceptionWrapper) {

                        try {

                            logger.dbg("aborting consumer (%s): %s", consumer, output);

                            consumer.onAbort(((RoutineExceptionWrapper) output).getCause());

                        } catch (final RoutineInterruptedException e) {

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

                if ((state == ChannelState.FLUSH) || (state == ChannelState.ABORTED)) {

                    closeConsumer();
                }

            } catch (final RoutineInterruptedException e) {

                throw e;

            } catch (final Throwable t) {

                boolean isClose = false;

                synchronized (mMutex) {

                    logger.wrn(t, "consumer exception (%s)", mOutputConsumer);

                    if ((mState == ChannelState.FLUSH) || (mState == ChannelState.ABORTED)) {

                        isClose = true;

                    } else if (mState != ChannelState.EXCEPTION) {

                        logger.wrn(t, "aborting on consumer exception (%s)", mOutputConsumer);

                        abortException = t;

                        mOutputQueue.clear();

                        mAbortException = t;
                        mState = ChannelState.EXCEPTION;
                    }
                }

                if (isClose) {

                    closeConsumer();
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

    private boolean isResultOpen() {

        synchronized (mMutex) {

            return (mState != ChannelState.DONE) && (mState != ChannelState.EXCEPTION);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private OUTPUT readQueue(@Nonnull final TimeDuration timeout,
            @Nullable final RuntimeException timeoutException) {

        synchronized (mMutex) {

            verifyBound();

            final Logger logger = mLogger;
            final NestedQueue<Object> outputQueue = mOutputQueue;

            if (timeout.isZero() || !outputQueue.isEmpty()) {

                if (outputQueue.isEmpty()) {

                    throw new NoSuchElementException();
                }

                final Object result = outputQueue.removeFirst();

                logger.dbg("reading output: %s [%s]", result, timeout);

                RoutineExceptionWrapper.raise(result);

                return (OUTPUT) result;
            }

            if (mOutputNotEmpty == null) {

                mOutputNotEmpty = new Check() {

                    @Override
                    public boolean isTrue() {

                        return !outputQueue.isEmpty();
                    }
                };
            }

            boolean isTimeout = false;

            try {

                isTimeout = !timeout.waitTrue(mMutex, mOutputNotEmpty);

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            if (isTimeout) {

                logger.wrn("reading output timeout: %s [%s]", timeoutException, timeout);

                if (timeoutException != null) {

                    throw timeoutException;
                }
            }

            final Object result = outputQueue.removeFirst();

            logger.dbg("reading output: %s [%s]", result, timeout);

            RoutineExceptionWrapper.raise(result);

            return (OUTPUT) result;
        }
    }

    private void verifyBound() {

        if (mOutputConsumer != null) {

            mLogger.err("invalid call on bound channel");

            throw new IllegalStateException("the channel is already bound");
        }
    }

    private void verifyOutput() {

        if (mState == ChannelState.EXCEPTION) {

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

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private final TimeDuration mTimeout;

        private final RuntimeException mTimeoutException;

        private boolean mRemoved = true;

        /**
         * Constructor.
         *
         * @param timeout          the output timeout.
         * @param timeoutException the timeout exception.
         */
        private DefaultIterator(@Nonnull final TimeDuration timeout,
                @Nullable final RuntimeException timeoutException) {

            mTimeout = timeout;
            mTimeoutException = timeoutException;
        }

        @Override
        public boolean hasNext() {

            synchronized (mMutex) {

                verifyBound();

                final Logger logger = mSubLogger;
                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (timeout.isZero() || (mState == ChannelState.DONE)) {

                    final boolean hasNext = !outputQueue.isEmpty();

                    logger.dbg("has output: %s [%s]", hasNext, timeout);

                    return hasNext;
                }

                if (mOutputHasNext == null) {

                    mOutputHasNext = new Check() {

                        @Override
                        public boolean isTrue() {

                            return !outputQueue.isEmpty() || (mState == ChannelState.DONE);
                        }
                    };
                }

                boolean isTimeout = false;

                try {

                    isTimeout = !timeout.waitTrue(mMutex, mOutputHasNext);

                } catch (final InterruptedException e) {

                    RoutineInterruptedException.interrupt(e);
                }

                if (isTimeout) {

                    logger.wrn("has output timeout: %s [%s]", timeoutException, timeout);

                    if (timeoutException != null) {

                        throw timeoutException;
                    }
                }

                final boolean hasNext = !outputQueue.isEmpty();

                logger.dbg("has output: %s [%s]", hasNext, timeout);

                return hasNext;
            }
        }

        @Override
        @Nullable
        @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT",
                            justification = "NestedQueue.removeFirst() actually throws it")
        public OUTPUT next() {

            synchronized (mMutex) {

                final OUTPUT next = readQueue(mTimeout, mTimeoutException);

                mRemoved = false;

                return next;
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

        @Override
        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            synchronized (mMutex) {

                verifyBound();

                if (timeout == null) {

                    mSubLogger.err("invalid null timeout");

                    throw new NullPointerException("the output timeout must not be null");
                }

                mOutputTimeout = timeout;
            }

            return this;
        }

        @Override
        @Nonnull
        public OutputChannel<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return afterMax(fromUnit(timeout, timeUnit));
        }

        @Override
        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> bind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            final boolean isClose;

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    mSubLogger.err("invalid null consumer");

                    throw new NullPointerException("the output consumer must not be null");
                }

                isClose = (mState == ChannelState.DONE);

                mOutputConsumer = consumer;
            }

            flushOutput();

            if (isClose) {

                closeConsumer();
            }

            return this;
        }

        @Override
        @Nonnull
        public OutputChannel<OUTPUT> eventuallyThrow(@Nullable final RuntimeException exception) {

            synchronized (mMutex) {

                verifyBound();

                mOutputTimeoutException = exception;
            }

            return this;
        }

        @Override
        @Nonnull
        public OutputChannel<OUTPUT> immediately() {

            return afterMax(ZERO);
        }

        @Override
        public boolean isComplete() {

            boolean isDone = false;

            synchronized (mMutex) {

                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputTimeoutException;

                try {

                    isDone = timeout.waitTrue(mMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return (mState == ChannelState.DONE);
                        }
                    });

                } catch (final InterruptedException e) {

                    RoutineInterruptedException.interrupt(e);
                }

                if (!isDone && (timeoutException != null)) {

                    mSubLogger.wrn("waiting complete timeout: %s [%s]", timeoutException, timeout);

                    throw timeoutException;
                }
            }

            return isDone;
        }

        @Override
        @Nonnull
        public List<OUTPUT> readAll() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            readAllInto(results);

            return results;
        }

        @Override
        @Nonnull
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        public OutputChannel<OUTPUT> readAllInto(
                @Nonnull final Collection<? super OUTPUT> results) {

            synchronized (mMutex) {

                verifyBound();

                final Logger logger = mSubLogger;

                if (results == null) {

                    logger.err("invalid null output list");

                    throw new NullPointerException("the result list must not be null");
                }

                final NestedQueue<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputTimeoutException;

                if (timeout.isZero() || (mState == ChannelState.DONE)) {

                    while (!outputQueue.isEmpty()) {

                        final Object result = outputQueue.removeFirst();

                        logger.dbg("adding output to list: %s [%s]", result, timeout);

                        RoutineExceptionWrapper.raise(result);

                        results.add((OUTPUT) result);
                    }

                    return this;
                }

                boolean isTimeout = false;

                try {

                    isTimeout = !timeout.waitTrue(mMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return (mState == ChannelState.DONE);
                        }
                    });

                } catch (final InterruptedException e) {

                    RoutineInterruptedException.interrupt(e);
                }

                if (isTimeout) {

                    logger.wrn("list output timeout: %s [%s]", timeoutException, timeout);

                    if (timeoutException != null) {

                        throw timeoutException;
                    }
                }

                while (!outputQueue.isEmpty()) {

                    final Object result = outputQueue.removeFirst();

                    logger.dbg("adding output to list: %s [%s]", result, timeout);

                    RoutineExceptionWrapper.raise(result);

                    results.add((OUTPUT) result);
                }
            }

            return this;
        }

        @Override
        public OUTPUT readFirst() {

            return readQueue(mOutputTimeout, mOutputTimeoutException);
        }

        @Override
        @Nonnull
        public Iterator<OUTPUT> iterator() {

            final TimeDuration timeout;
            final RuntimeException exception;

            synchronized (mMutex) {

                verifyBound();

                timeout = mOutputTimeout;
                exception = mOutputTimeoutException;
            }

            return new DefaultIterator(timeout, exception);
        }

        @Override
        public boolean abort() {

            return abort(null);
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    mSubLogger.dbg("avoiding aborting output since result channel is closed");

                    return false;
                }

                mSubLogger.dbg(reason, "aborting output");

                mOutputQueue.clear();

                mAbortException = reason;
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
        public void onAbort(@Nullable final Throwable reason) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    mSubLogger.dbg("avoiding aborting output since result channel is closed");

                    return;
                }

                mSubLogger.dbg(reason, "aborting output");

                mOutputQueue.clear();

                mAbortException = reason;
                mState = ChannelState.EXCEPTION;
            }

            final TimeDuration delay = mDelay;
            mHandler.onAbort(reason, delay.time, delay.unit);
        }

        @Override
        public void onClose() {

            boolean isFlush = false;

            synchronized (mMutex) {

                verifyOutput();

                mSubLogger.dbg("closing output [#%d]", mPendingOutputCount - 1);

                mQueue.close();

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;

                    isFlush = true;

                } else {

                    mMutex.notifyAll();
                }
            }

            if (isFlush) {

                flushOutput();
            }
        }

        @Override
        public void onOutput(final OUTPUT output) {

            NestedQueue<Object> outputQueue;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                verifyOutput();

                mSubLogger.dbg("adding output to queue [#%d]: %s [%s]", mPendingOutputCount + 1,
                               output, delay);

                outputQueue = mQueue;

                if (delay.isZero()) {

                    outputQueue.add(output);

                } else {

                    outputQueue = outputQueue.addNested();

                    ++mPendingOutputCount;
                }
            }

            if (delay.isZero()) {

                flushOutput();

            } else {

                mRunner.run(new DelayedOutputExecution(outputQueue, output), delay.time,
                            delay.unit);
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

                if (!isOutputOpen()) {

                    mLogger.dbg(throwable, "avoiding aborting since channel is closed");

                    return;
                }

                mLogger.dbg(throwable, "aborting channel");

                mOutputQueue.clear();

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
         * @param outputs the iterable returning the output data.
         */
        private DelayedListOutputExecution(@Nonnull final NestedQueue<Object> queue,
                @Nonnull final Iterable<? extends OUTPUT> outputs) {

            final ArrayList<OUTPUT> outputList = new ArrayList<OUTPUT>();

            for (final OUTPUT output : outputs) {

                outputList.add(output);
            }

            mOutputs = outputList;
            mQueue = queue;
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (mState == ChannelState.EXCEPTION) {

                    mLogger.dbg("avoiding delayed output execution since channel is closed: %s",
                                mOutputs);

                    return;
                }

                mLogger.dbg("delayed output execution [#%d]: %s", mPendingOutputCount - 1,
                            mOutputs);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;
                }

                mQueue.addAll(mOutputs).close();
            }

            flushOutput();
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

                if (mState == ChannelState.EXCEPTION) {

                    mLogger.dbg("avoiding delayed output execution since channel is closed: %s",
                                mOutput);

                    return;
                }

                mLogger.dbg("delayed output execution [#%d]: %s", mPendingOutputCount - 1, mOutput);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.FLUSH;
                }

                mQueue.add(mOutput).close();
            }

            flushOutput();
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