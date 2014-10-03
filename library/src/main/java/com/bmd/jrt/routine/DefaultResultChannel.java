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

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.fromUnit;
import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Class handling the routine output.<br/>
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

    private final AbortHandler mHandler;

    private final Object mMutex = new Object();

    private final Runner mRunner;

    private Throwable mAbortException;

    private ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private TimeDuration mInputDelay = ZERO;

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
     * @throws NullPointerException if one of the parameters is null.
     */
    DefaultResultChannel(final AbortHandler handler, final Runner runner,
            final boolean orderedOutput) {

        if (handler == null) {

            throw new NullPointerException("the abort handler must not be null");
        }

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mHandler = handler;
        mRunner = runner;
        mOutputQueue = (orderedOutput) ? new OrderedNestedQueue<Object>()
                : new SimpleNestedQueue<Object>();
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Override
    public ResultChannel<OUTPUT> after(final TimeDuration delay) {

        synchronized (mMutex) {

            verifyOutput();

            if (delay == null) {

                throw new IllegalArgumentException("the input delay must not be null");
            }

            mResultDelay = delay;
        }

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Override
    public ResultChannel<OUTPUT> pass(final OutputChannel<OUTPUT> channel) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (channel == null) {

                return this;
            }

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingOutputCount;
        }

        channel.bind(new DefaultOutputConsumer(delay));

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> pass(final Iterable<? extends OUTPUT> outputs) {

        NestedQueue<Object> outputQueue;
        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                return this;
            }

            outputQueue = mOutputQueue;
            delay = mResultDelay;

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
    public ResultChannel<OUTPUT> pass(final OUTPUT output) {

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
        }

        if (delay.isZero()) {

            flushOutput();

        } else {

            mRunner.run(new DelayedOutputInvocation(outputQueue, output), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> pass(final OUTPUT... outputs) {

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

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
    public void close(final Throwable throwable) {

        final ArrayList<OutputChannel<?>> channels;

        synchronized (mMutex) {

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
    public OutputChannel<OUTPUT> getOutput() {

        return new DefaultOutputChannel();
    }

    @SuppressWarnings("unchecked")
    private void flushOutput() {

        try {

            final ArrayList<Object> outputs;
            final OutputConsumer<OUTPUT> consumer;
            final ChannelState state;

            synchronized (mMutex) {

                consumer = mOutputConsumer;

                if (consumer == null) {

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

                        consumer.onAbort(((RoutineExceptionWrapper) output).getCause());

                    } catch (final RoutineInterruptedException e) {

                        throw e;

                    } catch (final Throwable ignored) {

                    }

                    break;

                } else {

                    consumer.onOutput((OUTPUT) output);
                }
            }

            if (state == ChannelState.DONE) {

                try {

                    consumer.onClose();

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }
            }

        } catch (final Throwable t) {

            boolean isFlush = false;
            boolean isAbort = false;

            synchronized (mMutex) {

                if (isDone()) {

                    isFlush = true;

                } else if (mState != ChannelState.EXCEPTION) {

                    isAbort = true;

                    mOutputQueue.clear();

                    mAbortException = t;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else if (isAbort) {

                mHandler.onAbort(t);
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

    @SuppressWarnings("unchecked")
    private OUTPUT readQueue(final TimeDuration timeout, final RuntimeException timeoutException) {

        synchronized (mMutex) {

            verifyBound();

            final NestedQueue<Object> outputQueue = mOutputQueue;

            if (timeout.isZero() || !outputQueue.isEmpty()) {

                if (outputQueue.isEmpty()) {

                    throw new NoSuchElementException();
                }

                final Object result = outputQueue.removeFirst();

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

                if (timeoutException != null) {

                    throw timeoutException;
                }
            }

            final Object result = outputQueue.removeFirst();

            RoutineExceptionWrapper.raise(result);

            return (OUTPUT) result;
        }
    }

    private void verifyBound() {

        if (mOutputConsumer != null) {

            throw new IllegalStateException("the channel is already bound");
        }
    }

    private void verifyOutput() {

        final Throwable throwable = mAbortException;

        if (throwable != null) {

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isResultOpen() || (mState == ChannelState.EXCEPTION)) {

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
         */
        public void onAbort(Throwable throwable);
    }

    /**
     * Default implementation of an output channel iterator.
     */
    public class DefaultIterator implements Iterator<OUTPUT> {

        private final TimeDuration mTimeout;

        private final RuntimeException mTimeoutException;

        private boolean mRemoved = true;

        /**
         * Constructor.
         *
         * @param timeout          the output timeout.
         * @param timeoutException the timeout exception.
         */
        public DefaultIterator(final TimeDuration timeout,
                final RuntimeException timeoutException) {

            mTimeout = timeout;
            mTimeoutException = timeoutException;
        }

        @Override
        public boolean hasNext() {

            synchronized (mMutex) {

                verifyBound();

                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (timeout.isZero() || isDone()) {

                    return !outputQueue.isEmpty();
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

                    if (timeoutException != null) {

                        throw timeoutException;
                    }
                }

                return !outputQueue.isEmpty();
            }
        }

        @Override
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

        @Override
        public OutputChannel<OUTPUT> afterMax(final TimeDuration timeout) {

            synchronized (mMutex) {

                verifyBound();

                if (timeout == null) {

                    throw new IllegalArgumentException("the output timeout must not be null");
                }

                mOutputTimeout = timeout;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> afterMax(final long timeout, final TimeUnit timeUnit) {

            return afterMax(fromUnit(timeout, timeUnit));
        }

        @Override
        public OutputChannel<OUTPUT> bind(final OutputConsumer<OUTPUT> consumer) {

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    throw new IllegalArgumentException("the output consumer must not be null");
                }

                mOutputConsumer = new SynchronizedConsumer<OUTPUT>(consumer);
            }

            flushOutput();

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> eventuallyThrow(final RuntimeException exception) {

            synchronized (mMutex) {

                verifyBound();

                mOutputTimeoutException = exception;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> immediately() {

            return afterMax(ZERO);
        }

        @Override
        public List<OUTPUT> readAll() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            readAllInto(results);

            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        public OutputChannel<OUTPUT> readAllInto(final List<OUTPUT> results) {

            synchronized (mMutex) {

                verifyBound();

                if (results == null) {

                    throw new IllegalArgumentException("the result list must not be null");
                }

                final NestedQueue<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputTimeoutException;

                if (timeout.isZero() || isDone()) {

                    while (!outputQueue.isEmpty()) {

                        final Object result = outputQueue.removeFirst();

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

                    if (timeoutException != null) {

                        throw timeoutException;
                    }

                } else {

                    while (!outputQueue.isEmpty()) {

                        final Object result = outputQueue.removeFirst();

                        RoutineExceptionWrapper.raise(result);

                        results.add((OUTPUT) result);
                    }
                }
            }

            return this;
        }

        @Override
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

                    throw timeoutException;
                }
            }

            return isDone;
        }

        @Override
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
        public boolean abort(final Throwable throwable) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    return false;
                }

                if (mState != ChannelState.EXCEPTION) {

                    mOutputQueue.clear();

                    mAbortException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            mHandler.onAbort(throwable);

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

        /**
         * Constructor.
         *
         * @param delay the output delay.
         */
        public DefaultOutputConsumer(final TimeDuration delay) {

            mDelay = delay;
            mQueue = mOutputQueue.addNested();
        }

        @Override
        public void onAbort(final Throwable throwable) {

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    return;
                }

                if (mState != ChannelState.EXCEPTION) {

                    mOutputQueue.clear();

                    mAbortException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            mHandler.onAbort(throwable);
        }

        @Override
        public void onClose() {

            boolean isFlush = false;

            synchronized (mMutex) {

                verifyOutput();

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
        public void onOutput(final OUTPUT output) {

            NestedQueue<Object> outputQueue;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                verifyOutput();

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
        public DelayedListOutputInvocation(final NestedQueue<Object> queue,
                final Iterable<? extends OUTPUT> outputs) {

            final ArrayList<OUTPUT> outputList = new ArrayList<OUTPUT>();

            for (final OUTPUT output : outputs) {

                outputList.add(output);
            }

            mOutputs = outputList;
            mQueue = queue;
        }

        @Override
        public void abort() {

            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isError()) {

                    return;
                }

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
        public DelayedOutputInvocation(final NestedQueue<Object> queue, final OUTPUT output) {

            mQueue = queue;
            mOutput = output;
        }

        @Override
        public void abort() {

            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isError()) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mQueue.add(mOutput).close();
            }

            flushOutput();
        }
    }

    @Override
    public boolean abort(final Throwable throwable) {

        synchronized (mMutex) {

            if (isDone()) {

                return false;
            }

            if (mState != ChannelState.EXCEPTION) {

                mOutputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }
        }

        mHandler.onAbort(throwable);

        return false;
    }

    @Override
    public boolean isOpen() {

        return isResultOpen();
    }
}