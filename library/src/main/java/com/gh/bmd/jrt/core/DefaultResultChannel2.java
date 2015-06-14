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
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.DeadlockException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.OutputDeadlockException;
import com.gh.bmd.jrt.channel.ReadDeadlockException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.TimeDuration;
import com.gh.bmd.jrt.util.TimeDuration.Check;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

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

import static com.gh.bmd.jrt.util.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.util.TimeDuration.ZERO;
import static com.gh.bmd.jrt.util.TimeDuration.fromUnit;

/**
 * TODO
 * <p/>
 * Created by davide on 12/06/15.
 *
 * @param <OUTPUT> the output data type.
 */
class DefaultResultChannel2<OUTPUT> implements ResultChannel<OUTPUT> {

    private static final WeakIdentityHashMap<OutputConsumer<?>, Object> sMutexMap =
            new WeakIdentityHashMap<OutputConsumer<?>, Object>();

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final Object mFlushMutex = new Object();

    private final AbortHandler mHandler;

    private final Check mHasOutputs;

    private final Logger mLogger;

    private final int mMaxOutput;

    private final Object mMutex = new Object();

    private final NestedQueue<Object> mOutputQueue;

    private final TimeDuration mOutputTimeout;

    private final TimeDuration mReadTimeout;

    private final Runner mRunner;

    private final TimeoutActionType mTimeoutActionType;

    private Throwable mAbortException;

    private Object mConsumerMutex;

    private OutputConsumer<? super OUTPUT> mOutputConsumer;

    private int mOutputCount;

    private Check mOutputHasNext;

    private Check mOutputNotEmpty;

    private int mPendingOutputCount;

    private TimeDuration mResultDelay = ZERO;

    private ResultChannelState<OUTPUT> mState = new OutputChannelState();

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param handler       the abortOutput handler.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultResultChannel2(@Nonnull final InvocationConfiguration configuration,
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
        mTimeoutActionType = configuration.getReadTimeoutActionOr(TimeoutActionType.DEADLOCK);
        mMaxOutput = configuration.getOutputMaxSizeOr(Integer.MAX_VALUE);
        mOutputTimeout = configuration.getOutputTimeoutOr(ZERO);
        mOutputQueue = (configuration.getOutputOrderTypeOr(OrderType.NONE) == OrderType.NONE)
                ? new SimpleNestedQueue<Object>() : new OrderedNestedQueue<Object>();
        final int maxOutputSize = mMaxOutput;
        mHasOutputs = new Check() {

            public boolean isTrue() {

                return (mOutputCount <= maxOutputSize);
            }
        };
    }

    @Nonnull
    private static Object getMutex(@Nonnull final OutputConsumer<?> consumer) {

        synchronized (sMutexMap) {

            final WeakIdentityHashMap<OutputConsumer<?>, Object> mutexMap = sMutexMap;
            Object mutex = mutexMap.get(consumer);

            if (mutex == null) {

                mutex = new Object();
                mutexMap.put(consumer, mutex);
            }

            return mutex;
        }
    }

    public boolean abort() {

        return abort(null);
    }

    @Nonnull
    public ResultChannel<OUTPUT> after(@Nonnull final TimeDuration delay) {

        synchronized (mMutex) {

            mState.after(delay);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

        return after(fromUnit(delay, timeUnit));
    }

    @Nonnull
    public ResultChannel<OUTPUT> now() {

        return after(ZERO);
    }

    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<? extends OUTPUT> channel) {

        final OutputConsumer<OUTPUT> consumer;

        synchronized (mMutex) {

            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {

            channel.passTo(consumer);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mResultDelay;
            execution = mState.pass(outputs);
        }

        if (delay.isZero()) {

            flushOutput(false);

        } else if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT output) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mResultDelay;
            execution = mState.pass(output);
        }

        if (delay.isZero()) {

            flushOutput(false);

        } else if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT... outputs) {

        final TimeDuration delay;
        final Execution execution;

        synchronized (mMutex) {

            delay = mResultDelay;
            execution = mState.pass(outputs);
        }

        if (delay.isZero()) {

            flushOutput(false);

        } else if (execution != null) {

            mRunner.run(execution, delay.time, delay.unit);
        }

        return this;
    }

    /**
     * Aborts immediately the execution.
     *
     * @param reason the reason of the abortion.
     * @see com.gh.bmd.jrt.channel.Channel#abort(Throwable)
     */
    void abortImmediately(@Nullable final Throwable reason) {

        Throwable abortException =
                (reason instanceof RoutineException) ? reason : new InvocationException(reason);

        synchronized (mMutex) {

            abortException = mState.abort(abortException, ZERO);
        }

        if (abortException != null) {

            mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Closes this channel with the specified exception.
     *
     * @param throwable the exception.
     */
    void close(@Nullable final Throwable throwable) {

        final ArrayList<OutputChannel<?>> channels;
        final Throwable abortException = (throwable instanceof RoutineException) ? throwable
                : new InvocationException(throwable);

        synchronized (mMutex) {

            mLogger.dbg(throwable, "aborting result channel");
            channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();
            mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

            if (mAbortException == null) {

                mAbortException = abortException;
            }

            mState = new AbortedChannelState();
            mMutex.notifyAll();
        }

        for (final OutputChannel<?> channel : channels) {

            channel.abort(abortException);
        }

        flushOutput(false);
    }

    /**
     * Closes this channel successfully.
     */
    void close() {

        final boolean needsFlush;

        synchronized (mMutex) {

            needsFlush = mState.close();
        }

        if (needsFlush) {

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

        final TimeoutActionType action = mTimeoutActionType;
        final OutputChannel<OUTPUT> outputChannel =
                new DefaultOutputChannel().afterMax(mReadTimeout);

        if (action == TimeoutActionType.EXIT) {

            outputChannel.eventuallyExit();

        } else if (action == TimeoutActionType.ABORT) {

            outputChannel.eventuallyAbort();
        }

        return outputChannel;
    }

    private void addOutputs(final int count) {

        mOutputCount += count;

        try {

            if (!mOutputTimeout.waitTrue(mMutex, mHasOutputs)) {

                throw new OutputDeadlockException(
                        "deadlock while waiting for room in the output channel");
            }

        } catch (final InterruptedException e) {

            throw new InvocationInterruptedException(e);
        }
    }

    private void closeConsumer(@Nonnull final ResultChannelState<OUTPUT> state,
            @Nonnull final OutputConsumer<? super OUTPUT> consumer) {

        state.closeConsumer(consumer);

        synchronized (mMutex) {

            if (!mState.isPendingOutput()) {

                mState = new DoneChannelState();
                mMutex.notifyAll();
            }
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField", "unchecked"})
    private void flushOutput(final boolean forceClose) {

        Throwable abortException = null;

        synchronized (mFlushMutex) {

            final Logger logger = mLogger;
            final ResultChannelState<OUTPUT> state;
            final ArrayList<Object> outputs;
            final OutputConsumer<? super OUTPUT> consumer;
            final boolean isPending;

            synchronized (mMutex) {

                state = mState;
                isPending = state.isPendingOutput();
                consumer = mOutputConsumer;

                if (consumer == null) {

                    logger.dbg("avoiding flushing output since channel is not bound");

                    if (!isPending) {

                        mState = new DoneChannelState();
                    }

                    mMutex.notifyAll();
                    return;
                }

                outputs = new ArrayList<Object>();
                mOutputQueue.moveTo(outputs);
                mOutputCount = 0;
                mMutex.notifyAll();
            }

            synchronized (mConsumerMutex) {

                try {

                    for (final Object output : outputs) {

                        if (output instanceof RoutineExceptionWrapper) {

                            try {

                                logger.dbg("aborting consumer (%s): %s", consumer, output);
                                consumer.onError(((RoutineExceptionWrapper) output).getCause());

                            } catch (final InvocationInterruptedException e) {

                                throw e;

                            } catch (final Throwable ignored) {

                                logger.wrn(ignored, "ignoring consumer exception (%s)", consumer);
                            }

                            break;

                        } else {

                            logger.dbg("output consumer (%s): %s", consumer, output);
                            consumer.onOutput((OUTPUT) output);
                        }
                    }

                    if (forceClose || !isPending) {

                        closeConsumer(state, consumer);
                    }

                } catch (final InvocationInterruptedException e) {

                    throw e;

                } catch (final Throwable t) {

                    final ResultChannelState<OUTPUT> finalState;
                    boolean isClose = false;

                    synchronized (mMutex) {

                        finalState = mState;
                        logger.wrn(t, "consumer exception (%s)", consumer);

                        if (forceClose || !finalState.isPendingOutput()) {

                            isClose = true;

                        } else {

                            abortException = finalState.abortFlush(t, logger);
                        }
                    }

                    if (isClose) {

                        closeConsumer(finalState, consumer);
                    }
                }
            }
        }

        if (abortException != null) {

            mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
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
            @Nonnull final TimeoutActionType action) {

        verifyBound();
        final Logger logger = mLogger;
        final NestedQueue<Object> outputQueue = mOutputQueue;

        if (timeout.isZero() || !outputQueue.isEmpty()) {

            if (outputQueue.isEmpty()) {

                logger.wrn("reading output timeout: [%s] => [%s]", timeout, action);

                if (action == TimeoutActionType.DEADLOCK) {

                    throw new ReadDeadlockException("deadlock while waiting for outputs");
                }
            }

            return nextOutput(timeout);
        }

        if (mRunner.isRunnerThread()) {

            throw new DeadlockException("cannot wait on the same runner thread");
        }

        if (mOutputNotEmpty == null) {

            mOutputNotEmpty = new Check() {

                public boolean isTrue() {

                    return !outputQueue.isEmpty();
                }
            };
        }

        final boolean isTimeout;

        try {

            isTimeout = !timeout.waitTrue(mMutex, mOutputNotEmpty);

        } catch (final InterruptedException e) {

            throw new InvocationInterruptedException(e);
        }

        if (isTimeout) {

            logger.wrn("reading output timeout: [%s] => [%s]", timeout, action);

            if (action == TimeoutActionType.DEADLOCK) {

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

    private interface ResultChannelState<OUTPUT> {

        @Nullable
        Throwable abort(@Nullable Throwable reason, @Nonnull TimeDuration delay);

        @Nullable
        Throwable abortFlush(@Nonnull Throwable reason, @Nonnull Logger logger);

        @Nullable
        Throwable abortOutput(@Nullable Throwable reason, @Nonnull Logger logger);

        void after(@Nonnull TimeDuration delay);

        boolean close();

        void closeConsumer(@Nonnull OutputConsumer<? super OUTPUT> consumer);

        /**
         * Delayed {@link ResultChannel#abort(Throwable)}.
         */
        @Nullable
        Throwable delayedAbort(@Nullable Throwable reason);

        /**
         * Delayed {@link ResultChannel#pass(Object)}.
         */
        boolean delayedOutput(@Nonnull NestedQueue<Object> queue, @Nullable OUTPUT output);

        /**
         * Delayed {@link ResultChannel#pass(Iterable)}.
         */
        boolean delayedOutputs(@Nonnull NestedQueue<Object> queue, List<OUTPUT> outputs);

        boolean isDone();

        boolean isOpen();

        boolean isOpenOutput();

        boolean isPendingOutput();

        boolean onComplete(@Nonnull NestedQueue<Object> queue, @Nonnull TimeDuration delay,
                @Nonnull Logger logger);

        boolean onError(@Nullable Throwable error, @Nonnull Logger logger);

        @Nullable
        Execution onOutput(OUTPUT output, @Nonnull TimeDuration delay,
                @Nonnull NestedQueue<Object> queue, @Nonnull Logger logger);

        /**
         * See {@link ResultChannel#pass(OutputChannel)}.
         */
        @Nullable
        OutputConsumer<OUTPUT> pass(@Nullable OutputChannel<? extends OUTPUT> channel);

        /**
         * See {@link ResultChannel#pass(Iterable)}.
         */
        @Nullable
        Execution pass(@Nullable Iterable<? extends OUTPUT> outputs);

        /**
         * See {@link ResultChannel#pass(Object)}.
         */
        @Nullable
        Execution pass(@Nullable OUTPUT output);

        /**
         * See {@link ResultChannel#pass(Object[])}.
         */
        @Nullable
        Execution pass(@Nullable OUTPUT... outputs);
    }

    private class AbortedChannelState extends ExceptionChannelState {

        @Override
        public boolean isPendingOutput() {

            return false;
        }

        @Nullable
        @Override
        public Throwable abortFlush(@Nonnull final Throwable reason, @Nonnull final Logger logger) {

            logger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            mOutputQueue.clear();
            mAbortException = reason;
            mState = new ExceptionChannelState();
            return reason;
        }

        @Override
        public void closeConsumer(@Nonnull final OutputConsumer<? super OUTPUT> consumer) {

        }
    }

    private class CompleteChannelState extends OutputChannelState {

        private IllegalStateException exception() {

            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Override
        public void after(@Nonnull final TimeDuration delay) {

            throw exception();
        }

        @Override
        public boolean close() {

            mLogger.dbg("avoiding closing result channel since already closed");
            return false;
        }

        @Override
        public boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mLogger.dbg("delayed output execution: %s", output);

            if (--mPendingOutputCount == 0) {

                mState = new FlushChannelState();
            }

            queue.add(output);
            queue.close();
            return true;
        }

        @Override
        public boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mLogger.dbg("delayed output execution: %s", outputs);

            if (--mPendingOutputCount == 0) {

                mState = new FlushChannelState();
            }

            queue.addAll(outputs);
            queue.close();
            return true;
        }

        @Override
        public boolean isOpen() {

            return false;
        }

        @Override
        public boolean onComplete(@Nonnull final NestedQueue<Object> queue,
                @Nonnull final TimeDuration delay, @Nonnull final Logger logger) {

            queue.close();

            if (--mPendingOutputCount == 0) {

                mState = new FlushChannelState();
                return true;
            }

            mMutex.notifyAll();
            return false;
        }

        @Nullable
        @Override
        public OutputConsumer<OUTPUT> pass(
                @Nullable final OutputChannel<? extends OUTPUT> channel) {

            throw exception();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            throw exception();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final OUTPUT output) {

            throw exception();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final OUTPUT... outputs) {

            throw exception();
        }
    }

    /**
     * Default implementation of an output channel iterator.
     */
    private class DefaultIterator implements Iterator<OUTPUT> {

        private final TimeoutActionType mAction;

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
                @Nonnull final TimeoutActionType action) {

            mTimeout = timeout;
            mAction = action;
        }

        public boolean hasNext() {

            final boolean isRunnerThread = mRunner.isRunnerThread();
            boolean isAbort = false;

            synchronized (mMutex) {

                verifyBound();
                final Logger logger = mSubLogger;
                final TimeDuration timeout = mTimeout;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (timeout.isZero() || mState.isDone()) {

                    final boolean hasNext = !outputQueue.isEmpty();

                    if (!hasNext && !mState.isDone()) {

                        final TimeoutActionType action = mAction;
                        logger.wrn("has output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutActionType.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to know if more outputs are coming");

                        } else {

                            isAbort = (action == TimeoutActionType.ABORT);
                        }
                    }

                } else {

                    if (isRunnerThread) {

                        throw new DeadlockException("cannot wait on the same runner thread");
                    }

                    if (mOutputHasNext == null) {

                        mOutputHasNext = new Check() {

                            public boolean isTrue() {

                                return !outputQueue.isEmpty() || mState.isDone();
                            }
                        };
                    }

                    final boolean isTimeout;

                    try {

                        isTimeout = !timeout.waitTrue(mMutex, mOutputHasNext);

                    } catch (final InterruptedException e) {

                        throw new InvocationInterruptedException(e);
                    }

                    if (isTimeout) {

                        final TimeoutActionType action = mAction;
                        logger.wrn("has output timeout: [%s] => [%s]", timeout, action);

                        if (action == TimeoutActionType.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to know if more outputs are coming");

                        } else {

                            isAbort = (action == TimeoutActionType.ABORT);
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
        @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT",
                justification = "NestedQueue.removeFirst() actually throws it")
        public OUTPUT next() {

            boolean isAbort = false;

            try {

                synchronized (mMutex) {

                    final TimeoutActionType action = mAction;
                    isAbort = (action == TimeoutActionType.ABORT);
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
     * Default implementation of a routine output channel.
     */
    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private TimeDuration mReadTimeout = ZERO;

        private TimeoutActionType mTimeoutActionType = TimeoutActionType.DEADLOCK;

        @Nonnull
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
        public OutputChannel<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            return afterMax(fromUnit(timeout, timeUnit));
        }

        @Nonnull
        public List<OUTPUT> all() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            allInto(results);
            return results;
        }

        @Nonnull
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        public OutputChannel<OUTPUT> allInto(@Nonnull final Collection<? super OUTPUT> results) {

            final boolean isRunnerThread = mRunner.isRunnerThread();
            boolean isAbort = false;

            synchronized (mMutex) {

                verifyBound();
                final Logger logger = mSubLogger;

                if (results == null) {

                    logger.err("invalid null output list");
                    throw new NullPointerException("the result list must not be null");
                }

                final TimeDuration readTimeout = mReadTimeout;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (readTimeout.isZero() || mState.isDone()) {

                    while (!outputQueue.isEmpty()) {

                        final OUTPUT result = nextOutput(readTimeout);
                        logger.dbg("adding output to list: %s [%s]", result, readTimeout);
                        results.add(result);
                    }

                    if (!mState.isDone()) {

                        final TimeoutActionType timeoutAction = mTimeoutActionType;
                        logger.wrn("list output timeout: [%s] => [%s]", readTimeout, timeoutAction);

                        if (timeoutAction == TimeoutActionType.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to collect all outputs");

                        } else {

                            isAbort = (timeoutAction == TimeoutActionType.ABORT);
                        }
                    }

                } else {

                    final long startTime = System.currentTimeMillis();
                    final boolean isTimeout;

                    try {

                        do {

                            while (!outputQueue.isEmpty()) {

                                final OUTPUT result = nextOutput(readTimeout);
                                logger.dbg("adding output to list: %s [%s]", result, readTimeout);
                                results.add(result);
                            }

                            if (mState.isDone()) {

                                break;
                            }

                            if (isRunnerThread) {

                                throw new DeadlockException(
                                        "cannot wait on the same runner thread");
                            }

                        } while (readTimeout.waitSinceMillis(mMutex, startTime));

                        isTimeout = !mState.isDone();

                    } catch (final InterruptedException e) {

                        throw new InvocationInterruptedException(e);
                    }

                    if (isTimeout) {

                        final TimeoutActionType action = mTimeoutActionType;
                        logger.wrn("list output timeout: [%s] => [%s]", readTimeout, action);

                        if (action == TimeoutActionType.DEADLOCK) {

                            throw new ReadDeadlockException(
                                    "deadlock while waiting to collect all outputs");

                        } else {

                            isAbort = (action == TimeoutActionType.ABORT);
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

        public boolean checkComplete() {

            final boolean isRunnerThread = mRunner.isRunnerThread();

            synchronized (mMutex) {

                final TimeDuration readTimeout = mReadTimeout;

                if (!readTimeout.isZero() && isRunnerThread) {

                    throw new DeadlockException("cannot wait on the same runner thread");
                }

                final boolean isDone;

                try {

                    isDone = readTimeout.waitTrue(mMutex, new Check() {

                        public boolean isTrue() {

                            return mState.isDone();
                        }
                    });

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }

                if (!isDone) {

                    mSubLogger.wrn("waiting complete timeout: [%s]", readTimeout);
                }

                return isDone;
            }
        }

        @Nonnull
        public OutputChannel<OUTPUT> eventually() {

            return afterMax(INFINITY);
        }

        @Nonnull
        public OutputChannel<OUTPUT> eventuallyAbort() {

            synchronized (mMutex) {

                mTimeoutActionType = TimeoutActionType.ABORT;
            }

            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> eventuallyDeadlock() {

            synchronized (mMutex) {

                mTimeoutActionType = TimeoutActionType.DEADLOCK;
            }

            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> eventuallyExit() {

            synchronized (mMutex) {

                mTimeoutActionType = TimeoutActionType.EXIT;
            }

            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> immediately() {

            return afterMax(ZERO);
        }

        public boolean isBound() {

            synchronized (mMutex) {

                return (mOutputConsumer != null);
            }
        }

        public OUTPUT next() {

            boolean isAbort = false;

            try {

                synchronized (mMutex) {

                    final TimeoutActionType timeoutAction = mTimeoutActionType;
                    isAbort = (timeoutAction == TimeoutActionType.ABORT);
                    return readQueue(mReadTimeout, timeoutAction);
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
        public <INPUT extends InputChannel<? super OUTPUT>> INPUT passTo(
                @Nonnull final INPUT channel) {

            channel.pass(this);
            return channel;
        }

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> passTo(
                @Nonnull final OutputConsumer<? super OUTPUT> consumer) {

            final boolean forceClose;

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    mSubLogger.err("invalid null consumer");
                    throw new NullPointerException("the output consumer must not be null");
                }

                forceClose = mState.isDone();
                mOutputConsumer = consumer;
                mConsumerMutex = getMutex(consumer);
            }

            flushOutput(forceClose);
            return this;
        }

        @Nonnull
        public Iterator<OUTPUT> iterator() {

            final TimeDuration timeout;
            final TimeoutActionType action;

            synchronized (mMutex) {

                verifyBound();
                timeout = mReadTimeout;
                action = mTimeoutActionType;
            }

            return new DefaultIterator(timeout, action);
        }

        public boolean abort() {

            return abort(null);
        }

        public boolean abort(@Nullable final Throwable reason) {

            final Throwable abortException;

            synchronized (mMutex) {

                abortException = mState.abortOutput(reason, mSubLogger);
            }

            if (abortException != null) {

                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
                return true;
            }

            return false;
        }

        public boolean isOpen() {

            synchronized (mMutex) {

                return mState.isOpenOutput();
            }
        }
    }

    /**
     * Default implementation of an output consumer pushing the data to consume into the output
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

        public void onComplete() {

            final boolean needsFlush;

            synchronized (mMutex) {

                needsFlush = mState.onComplete(mQueue, mDelay, mSubLogger);
                mSubLogger.dbg("closing output [%s]", needsFlush);
            }

            if (needsFlush) {

                flushOutput(false);
            }
        }

        public void onError(@Nullable final Throwable error) {

            final boolean needsAbort;

            synchronized (mMutex) {

                needsAbort = mState.onError(error, mSubLogger);
            }

            if (needsAbort) {

                final TimeDuration delay = mDelay;
                mHandler.onAbort(error, delay.time, delay.unit);
            }
        }

        public void onOutput(final OUTPUT output) {

            final Execution execution;
            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                execution = mState.onOutput(output, delay, mQueue, mSubLogger);
            }

            if (delay.isZero()) {

                flushOutput(false);

            } else if (execution != null) {

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

            final Throwable abortException;

            synchronized (mMutex) {

                abortException = mState.delayedAbort(mAbortException);
            }

            if (abortException != null) {

                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
            }
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

        public void run() {

            final boolean needsFlush;

            synchronized (mMutex) {

                needsFlush = mState.delayedOutputs(mQueue, mOutputs);
            }

            if (needsFlush) {

                flushOutput(false);
            }
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

        public void run() {

            final boolean needsFlush;

            synchronized (mMutex) {

                needsFlush = mState.delayedOutput(mQueue, mOutput);
            }

            if (needsFlush) {

                flushOutput(false);
            }
        }
    }

    private class DoneChannelState extends FlushChannelState {

        @Nullable
        @Override
        public Throwable delayedAbort(@Nullable final Throwable reason) {

            if (!isOpenOutput()) {

                mLogger.dbg(reason, "avoiding aborting since channel is closed");
                return null;
            }

            return super.delayedAbort(reason);
        }

        @Override
        public boolean isDone() {

            return true;
        }

        @Override
        public boolean isOpenOutput() {

            return !mOutputQueue.isEmpty();
        }
    }

    private class ExceptionChannelState extends FlushChannelState {

        @Override
        public void after(@Nonnull final TimeDuration delay) {

            throw abortException();
        }

        @Override
        public boolean onComplete(@Nonnull final NestedQueue<Object> queue,
                @Nonnull final TimeDuration delay, @Nonnull final Logger logger) {

            throw abortException();
        }

        @Override
        public boolean isPendingOutput() {

            return true;
        }

        @Nullable
        @Override
        public Execution onOutput(final OUTPUT output, @Nonnull final TimeDuration delay,
                @Nonnull final NestedQueue<Object> queue, @Nonnull final Logger logger) {

            throw abortException();
        }

        private RoutineException abortException() {

            final Throwable abortException = mAbortException;
            mLogger.dbg(abortException, "abort exception");
            return RoutineExceptionWrapper.wrap(abortException).raise();
        }

        @Nullable
        @Override
        public OutputConsumer<OUTPUT> pass(
                @Nullable final OutputChannel<? extends OUTPUT> channel) {

            throw abortException();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            throw abortException();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final OUTPUT output) {

            throw abortException();
        }

        @Nullable
        @Override
        public Execution pass(@Nullable final OUTPUT... outputs) {

            throw abortException();
        }

        @Nullable
        @Override
        public Throwable abortFlush(@Nonnull final Throwable reason, @Nonnull final Logger logger) {

            return null;
        }
    }

    private class FlushChannelState extends CompleteChannelState {

        @Nullable
        @Override
        public Throwable abort(@Nullable final Throwable reason,
                @Nonnull final TimeDuration delay) {

            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        private IllegalStateException exception(@Nonnull final Logger logger) {

            logger.err("consumer invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Nullable
        @Override
        public Throwable abortOutput(@Nullable final Throwable reason,
                @Nonnull final Logger logger) {

            logger.dbg("avoiding aborting output since result channel is closed");
            return null;
        }

        @Override
        public boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mLogger.dbg("avoiding delayed output execution since channel is closed: %s", output);
            return false;
        }

        @Override
        public boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mLogger.dbg("avoiding delayed output execution since channel is closed: %s", outputs);
            return false;
        }

        @Override
        public boolean onComplete(@Nonnull final NestedQueue<Object> queue,
                @Nonnull final TimeDuration delay, @Nonnull final Logger logger) {

            throw exception(logger);
        }

        @Override
        public boolean isPendingOutput() {

            return false;
        }

        @Override
        public boolean onError(@Nullable final Throwable error, @Nonnull final Logger logger) {

            logger.dbg("avoiding aborting output since result channel is closed");
            return false;
        }

        @Nullable
        @Override
        public Execution onOutput(final OUTPUT output, @Nonnull final TimeDuration delay,
                @Nonnull final NestedQueue<Object> queue, @Nonnull final Logger logger) {

            throw exception(logger);
        }
    }

    private class OutputChannelState implements ResultChannelState<OUTPUT> {

        @Nullable
        public Throwable abort(@Nullable final Throwable reason,
                @Nonnull final TimeDuration delay) {

            final Throwable abortException =
                    (reason instanceof RoutineException) ? reason : new AbortException(reason);

            if (delay.isZero()) {

                mLogger.dbg(reason, "aborting channel");
                mOutputQueue.clear();
                mAbortException = abortException;
                mState = new ExceptionChannelState();
            }

            return abortException;
        }

        @Nullable
        public Throwable abortFlush(@Nonnull final Throwable reason, @Nonnull final Logger logger) {

            logger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            mOutputQueue.clear();
            mAbortException = reason;
            mState = new ExceptionChannelState();
            return reason;
        }

        @Nullable
        public Throwable abortOutput(@Nullable final Throwable reason,
                @Nonnull final Logger logger) {

            final Throwable abortException =
                    (reason instanceof RoutineException) ? reason : new AbortException(reason);
            logger.dbg(reason, "aborting output");
            mOutputQueue.clear();
            mAbortException = abortException;
            mState = new ExceptionChannelState();
            return abortException;
        }

        @SuppressWarnings("ConstantConditions")
        public void after(@Nonnull final TimeDuration delay) {

            if (delay == null) {

                mLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mResultDelay = delay;
        }

        public boolean close() {

            mLogger.dbg("closing result channel [#%d]", mPendingOutputCount);

            if (mPendingOutputCount > 0) {

                mState = new CompleteChannelState();

            } else {

                mState = new FlushChannelState();
            }

            return true;
        }

        public void closeConsumer(@Nonnull final OutputConsumer<? super OUTPUT> consumer) {

            final Logger logger = mLogger;

            try {

                logger.dbg("closing consumer (%s)", consumer);
                consumer.onComplete();

            } catch (final InvocationInterruptedException e) {

                throw e;

            } catch (final Throwable ignored) {

                logger.wrn(ignored, "ignoring consumer exception (%s)", consumer);
            }
        }

        @Nullable
        public Throwable delayedAbort(@Nullable final Throwable reason) {

            mLogger.dbg(reason, "aborting channel");
            mOutputQueue.clear();
            mAbortException = reason;
            mState = new ExceptionChannelState();
            return reason;
        }

        public boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mLogger.dbg("delayed output execution: %s", output);
            --mPendingOutputCount;
            queue.add(output);
            queue.close();
            return true;
        }

        public boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mLogger.dbg("delayed output execution: %s", outputs);
            --mPendingOutputCount;
            queue.addAll(outputs);
            queue.close();
            return true;
        }

        public boolean isDone() {

            return false;
        }

        public boolean isOpen() {

            return true;
        }

        public boolean isOpenOutput() {

            return true;
        }

        public boolean isPendingOutput() {

            return true;
        }

        public boolean onComplete(@Nonnull final NestedQueue<Object> queue,
                @Nonnull final TimeDuration delay, @Nonnull final Logger logger) {

            queue.close();
            --mPendingOutputCount;
            mMutex.notifyAll();
            return false;
        }

        public boolean onError(@Nullable final Throwable error, @Nonnull final Logger logger) {

            logger.dbg(error, "aborting output");
            mOutputQueue.clear();
            mAbortException = error;
            mState = new ExceptionChannelState();
            return true;
        }

        @Nullable
        public Execution onOutput(final OUTPUT output, @Nonnull final TimeDuration delay,
                @Nonnull final NestedQueue<Object> queue, @Nonnull final Logger logger) {

            final boolean isZero;
            final NestedQueue<Object> outputQueue;

            if (delay.isZero()) {

                isZero = true;
                outputQueue = queue;
                outputQueue.add(output);

            } else {

                isZero = false;
                outputQueue = queue.addNested();
                ++mPendingOutputCount;
            }

            logger.dbg("consumer output [#%d+1]: %s [%s]", mOutputCount, output, delay);
            addOutputs(1);
            return (!isZero) ? new DelayedOutputExecution(outputQueue, output) : null;
        }

        @Nullable
        public OutputConsumer<OUTPUT> pass(
                @Nullable final OutputChannel<? extends OUTPUT> channel) {

            if (channel == null) {

                mLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingOutputCount;
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer(mResultDelay);
        }

        @Nullable
        public Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            if (outputs == null) {

                mLogger.wrn("passing null iterable");
                return null;
            }

            final boolean isZero;
            final NestedQueue<Object> outputQueue;
            final TimeDuration delay = mResultDelay;
            ArrayList<OUTPUT> list = null;
            int count = 0;

            if (delay.isZero()) {

                isZero = true;
                outputQueue = mOutputQueue;

                for (final OUTPUT output : outputs) {

                    outputQueue.add(output);
                    ++count;
                }

            } else {

                isZero = false;
                outputQueue = mOutputQueue.addNested();
                list = new ArrayList<OUTPUT>();

                for (final OUTPUT output : outputs) {

                    list.add(output);
                }

                count = list.size();
                ++mPendingOutputCount;
            }

            mLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mOutputCount, count, outputs, delay);
            addOutputs(count);
            return (!isZero) ? new DelayedListOutputExecution(outputQueue, list) : null;
        }

        @Nullable
        public Execution pass(@Nullable final OUTPUT output) {

            final boolean isZero;
            final NestedQueue<Object> outputQueue;
            final TimeDuration delay = mResultDelay;

            if (delay.isZero()) {

                isZero = true;
                outputQueue = mOutputQueue;
                outputQueue.add(output);

            } else {

                isZero = false;
                outputQueue = mOutputQueue.addNested();
                ++mPendingOutputCount;
            }

            mLogger.dbg("passing output [#%d+1]: %s [%s]", mOutputCount, output, delay);
            addOutputs(1);
            return (!isZero) ? new DelayedOutputExecution(outputQueue, output) : null;
        }

        @Nullable
        public Execution pass(@Nullable final OUTPUT... outputs) {

            if (outputs == null) {

                mLogger.wrn("passing null output array");
                return null;
            }

            return pass(Arrays.asList(outputs));
        }
    }

    @Override
    public boolean abort(@Nullable final Throwable reason) {

        final TimeDuration delay;
        final Throwable abortException;

        synchronized (mMutex) {

            delay = mResultDelay;
            abortException = mState.abort(reason, delay);
        }

        if (abortException != null) {

            if (delay.isZero()) {

                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.run(new DelayedAbortExecution(abortException), delay.time, delay.unit);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean isOpen() {

        synchronized (mMutex) {

            return mState.isOpen();
        }
    }
}
