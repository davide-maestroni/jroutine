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
import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

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
    DefaultResultChannel(@NonNull final AbortHandler handler, @NonNull final Runner runner,
            final boolean orderedOutput, @NonNull final Logger logger) {

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
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public ResultChannel<OUTPUT> after(@NonNull final TimeDuration delay) {

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
    @NonNull
    public ResultChannel<OUTPUT> after(final long delay, @NonNull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Override
    @NonNull
    public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<OUTPUT> channel) {

        final TimeDuration delay;

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
        }

        channel.bind(new DefaultOutputConsumer(delay));

        return this;
    }

    @Override
    @NonNull
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

            mRunner.run(new DelayedListOutputInvocation(outputQueue, outputs), delay.time,
                        delay.unit);
        }

        return this;
    }

    @Override
    @NonNull
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

            mRunner.run(new DelayedOutputInvocation(outputQueue, output), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    @NonNull
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

            mState = ChannelState.ABORT;
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

                    mState = ChannelState.DONE;
                }
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
    @NonNull
    public OutputChannel<OUTPUT> getOutput() {

        return new DefaultOutputChannel();
    }

    private boolean abort(@Nullable final Throwable throwable, final boolean isImmediate) {

        // TODO: delay abort...

        final TimeDuration delay;

        synchronized (mMutex) {

            if (!isResultOpen()) {

                mLogger.dbg(throwable, "avoiding aborting since channel is closed");

                return false;
            }

            delay = (isImmediate) ? ZERO : mResultDelay;

            mLogger.dbg(throwable, "aborting channel");

            mOutputQueue.clear();

            mAbortException = throwable;
            mState = ChannelState.EXCEPTION;
        }

        mHandler.onAbort(throwable, delay.time, delay.unit);

        return true;
    }

    @SuppressWarnings("unchecked")
    private void flushOutput() {

        final Logger logger = mLogger;

        try {

            final ArrayList<Object> outputs;
            final OutputConsumer<OUTPUT> consumer;
            final ChannelState state;

            synchronized (mMutex) {

                consumer = mOutputConsumer;

                if (consumer == null) {

                    logger.dbg("avoiding flushing output since channel is not bound");

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

            if (state == ChannelState.DONE) {

                try {

                    logger.dbg("closing consumer (%s)", consumer);

                    consumer.onClose();

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable t) {

                    logger.wrn(t, "ignoring consumer exception (%s)", consumer);
                }
            }

        } catch (final Throwable t) {

            boolean isFlush = false;
            boolean isAbort = false;

            synchronized (mMutex) {

                logger.wrn(t, "consumer exception (%s)", mOutputConsumer);

                if (isDone()) {

                    isFlush = true;

                } else if (mState != ChannelState.EXCEPTION) {

                    logger.wrn(t, "aborting on consumer exception (%s)", mOutputConsumer);

                    isAbort = true;

                    mOutputQueue.clear();

                    mAbortException = t;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else if (isAbort) {

                mHandler.onAbort(t, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    private boolean isDone() {

        return (mState == ChannelState.DONE) || (mState == ChannelState.ABORT);
    }

    private boolean isError() {

        return (mState == ChannelState.ABORT) || (mState == ChannelState.EXCEPTION);
    }

    private boolean isOutputOpen() {

        synchronized (mMutex) {

            return !mOutputQueue.isEmpty() || ((mState != ChannelState.DONE) && (mState
                    != ChannelState.ABORT));
        }
    }

    private boolean isResultOpen() {

        synchronized (mMutex) {

            return !isDone() && (mState != ChannelState.EXCEPTION);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private OUTPUT readQueue(@NonNull final TimeDuration timeout,
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

        final Throwable throwable = mAbortException;

        if (throwable != null) {

            mLogger.dbg(throwable, "abort exception");

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isResultOpen() || (mState == ChannelState.EXCEPTION)) {

            mLogger.err("invalid call on closed channel");

            throw new IllegalStateException("the channel is closed");
        }
    }

    /**
     * Enumeration identifying the channel internal state.
     */
    private static enum ChannelState {

        OUTPUT,
        RESULT,
        DONE,
        EXCEPTION,
        ABORT
    }

    /**
     * Interface defining an abort handler.
     */
    public interface AbortHandler {

        /**
         * Called on an abort.
         *
         * @param throwable the reason of the abortion.
         * @param delay     the abortion delay.
         * @param timeUnit  the delay time unit.
         */
        public void onAbort(@Nullable Throwable throwable, long delay, @NonNull TimeUnit timeUnit);
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
        private DefaultIterator(@NonNull final TimeDuration timeout,
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

                if (timeout.isZero() || isDone()) {

                    final boolean hasNext = !outputQueue.isEmpty();

                    logger.dbg("has output: %s [%s]", hasNext, timeout);

                    return hasNext;
                }

                if (mOutputHasNext == null) {

                    mOutputHasNext = new Check() {

                        @Override
                        public boolean isTrue() {

                            return !outputQueue.isEmpty() || isDone();
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
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "IT_NO_SUCH_ELEMENT",
                                                          justification = "readQueue() actually "
                                                                  + "throws it")
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
        @NonNull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> afterMax(@NonNull final TimeDuration timeout) {

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
        @NonNull
        public OutputChannel<OUTPUT> afterMax(final long timeout,
                @NonNull final TimeUnit timeUnit) {

            return afterMax(fromUnit(timeout, timeUnit));
        }

        @Override
        @NonNull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> bind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    mSubLogger.err("invalid null consumer");

                    throw new NullPointerException("the output consumer must not be null");
                }

                mOutputConsumer = new SynchronizedConsumer<OUTPUT>(consumer);
            }

            flushOutput();

            return this;
        }

        @Override
        @NonNull
        public OutputChannel<OUTPUT> eventuallyThrow(@Nullable final RuntimeException exception) {

            synchronized (mMutex) {

                verifyBound();

                mOutputTimeoutException = exception;
            }

            return this;
        }

        @Override
        @NonNull
        public OutputChannel<OUTPUT> immediately() {

            return afterMax(ZERO);
        }

        @Override
        @NonNull
        public List<OUTPUT> readAll() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            readAllInto(results);

            return results;
        }

        @Override
        @NonNull
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        public OutputChannel<OUTPUT> readAllInto(@NonNull final List<OUTPUT> results) {

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

                if (timeout.isZero() || isDone()) {

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

                            return isDone();
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

                } else {

                    while (!outputQueue.isEmpty()) {

                        final Object result = outputQueue.removeFirst();

                        logger.dbg("adding output to list: %s [%s]", result, timeout);

                        RoutineExceptionWrapper.raise(result);

                        results.add((OUTPUT) result);
                    }
                }
            }

            return this;
        }

        @Override
        @Nullable
        public OUTPUT readFirst() {

            return readQueue(mOutputTimeout, mOutputTimeoutException);
        }

        @Override
        public boolean waitComplete() {

            boolean isDone = false;

            synchronized (mMutex) {

                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputTimeoutException;

                try {

                    isDone = timeout.waitTrue(mMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return isDone();
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
        @NonNull
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
        public boolean abort(@Nullable final Throwable throwable) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    mSubLogger.dbg("avoid aborting output since result channel is closed");

                    return false;
                }

                mSubLogger.dbg(throwable, "aborting output");

                mOutputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mHandler.onAbort(throwable, 0, TimeUnit.MILLISECONDS);

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
        private DefaultOutputConsumer(@NonNull final TimeDuration delay) {

            mDelay = delay;
            mQueue = mOutputQueue.addNested();
        }

        @Override
        public void onAbort(@Nullable final Throwable throwable) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    mSubLogger.dbg("avoid aborting output since result channel is closed");

                    return;
                }

                mSubLogger.dbg(throwable, "aborting output");

                mOutputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            final TimeDuration delay = mDelay;
            mHandler.onAbort(throwable, delay.time, delay.unit);
        }

        @Override
        public void onClose() {

            boolean isFlush = false;

            synchronized (mMutex) {

                verifyOutput();

                mSubLogger.dbg("closing output [#%d]", mPendingOutputCount - 1);

                mQueue.close();

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;

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
        public void onOutput(@Nullable final OUTPUT output) {

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

                mRunner.run(new DelayedOutputInvocation(outputQueue, output), delay.time,
                            delay.unit);
            }
        }
    }

    /**
     * Implementation of an invocation handling a delayed output of a list of data.
     */
    private class DelayedListOutputInvocation implements Invocation {

        private final ArrayList<OUTPUT> mOutputs;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue   the output queue.
         * @param outputs the iterable returning the output data.
         */
        private DelayedListOutputInvocation(@NonNull final NestedQueue<Object> queue,
                @NonNull final Iterable<? extends OUTPUT> outputs) {

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

                if (isError()) {

                    mLogger.dbg("avoiding delayed output invocation since channel is closed: %s",
                                mOutputs);

                    return;
                }

                mLogger.dbg("delayed output invocation [#%d]: %s", mPendingOutputCount - 1,
                            mOutputs);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mQueue.addAll(mOutputs).close();
            }

            flushOutput();
        }
    }

    /**
     * Implementation of an invocation handling a delayed output.
     */
    private class DelayedOutputInvocation implements Invocation {

        private final OUTPUT mOutput;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the output queue.
         * @param output the output.
         */
        private DelayedOutputInvocation(@NonNull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mQueue = queue;
            mOutput = output;
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isError()) {

                    mLogger.dbg("avoiding delayed output invocation since channel is closed: %s",
                                mOutput);

                    return;
                }

                mLogger.dbg("delayed output invocation [#%d]: %s", mPendingOutputCount - 1,
                            mOutput);

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mQueue.add(mOutput).close();
            }

            flushOutput();
        }
    }

    @Override
    public boolean abort(@Nullable final Throwable throwable) {

        return abort(throwable, false);
    }

    @Override
    public boolean isOpen() {

        return isResultOpen();
    }
}