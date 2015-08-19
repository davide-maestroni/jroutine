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
import com.gh.bmd.jrt.channel.ExecutionDeadlockException;
import com.gh.bmd.jrt.channel.ExecutionTimeoutException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.OutputDeadlockException;
import com.gh.bmd.jrt.channel.OutputTimeoutException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.invocation.InvocationDeadlockException;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.TemplateExecution;
import com.gh.bmd.jrt.util.TimeDuration;
import com.gh.bmd.jrt.util.TimeDuration.Check;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * Class handling the invocation output.
 * <p/>
 * This class centralizes the managing of data passing through the routine output and result
 * channels, since, logically, the two objects are part of the same entity. In fact, on one end the
 * result channel puts data into the output queue and, on the other end, the output channel reads
 * them from the same queue.
 * <p/>
 * Created by davide-maestroni on 12/06/15.
 *
 * @param <OUTPUT> the output data type.
 */
class DefaultResultChannel<OUTPUT> implements ResultChannel<OUTPUT> {

    private static final WeakIdentityHashMap<OutputConsumer<?>, Object> sMutexMap =
            new WeakIdentityHashMap<OutputConsumer<?>, Object>();

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final TimeDuration mExecutionTimeout;

    private final Object mFlushMutex = new Object();

    private final AbortHandler mHandler;

    private final Check mHasOutputs;

    private final Logger mLogger;

    private final int mMaxOutput;

    private final Object mMutex = new Object();

    private final NestedQueue<Object> mOutputQueue;

    private final TimeDuration mOutputTimeout;

    private final Runner mRunner;

    private final TimeoutActionType mTimeoutActionType;

    private RoutineException mAbortException;

    private Object mConsumerMutex;

    private boolean mIsWaitingInvocation;

    private OutputConsumer<? super OUTPUT> mOutputConsumer;

    private int mOutputCount;

    private Check mOutputHasNext;

    private Check mOutputNotEmpty;

    private int mPendingOutputCount;

    private TimeDuration mResultDelay = ZERO;

    private OrderType mResultOrder;

    private OutputChannelState mState;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param handler       the abort handler.
     * @param runner        the runner instance.
     * @param logger        the logger instance.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultResultChannel(@Nonnull final InvocationConfiguration configuration,
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
        mResultOrder = configuration.getOutputOrderTypeOr(OrderType.BY_CHANCE);
        mExecutionTimeout = configuration.getExecutionTimeoutOr(ZERO);
        mTimeoutActionType = configuration.getExecutionTimeoutActionOr(TimeoutActionType.THROW);
        mMaxOutput = configuration.getOutputMaxSizeOr(Integer.MAX_VALUE);
        mOutputTimeout = configuration.getOutputTimeoutOr(ZERO);
        mOutputQueue = new NestedQueue<Object>() {

            @Override
            public void close() {

                // Prevents closing
            }
        };
        final int maxOutputSize = mMaxOutput;
        mHasOutputs = new Check() {

            public boolean isTrue() {

                return (mOutputCount <= maxOutputSize) || mIsWaitingInvocation;
            }
        };
        mState = new OutputChannelState();
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
    public ResultChannel<OUTPUT> orderByCall() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_CALL);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> orderByChance() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_CHANCE);
        }

        return this;
    }

    @Nonnull
    public ResultChannel<OUTPUT> orderByDelay() {

        synchronized (mMutex) {

            mState.orderBy(OrderType.BY_DELAY);
        }

        return this;
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
     * @see com.gh.bmd.jrt.channel.Channel#abort(Throwable) abort(Throwable)
     */
    void abortImmediately(@Nullable final Throwable reason) {

        RoutineException abortException = InvocationException.wrapIfNeeded(reason);

        synchronized (mMutex) {

            abortException = mState.abortInvocation(abortException, ZERO);
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
        final RoutineException abortException = InvocationException.wrapIfNeeded(throwable);

        synchronized (mMutex) {

            mLogger.dbg(throwable, "aborting result channel");
            channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();
            mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

            if (mAbortException == null) {

                mAbortException = abortException;
            }

            mState = new AbortChannelState();
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

            needsFlush = mState.closeResultChannel();
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
                new DefaultOutputChannel().afterMax(mExecutionTimeout);

        if (action == TimeoutActionType.EXIT) {

            outputChannel.eventuallyExit();

        } else if (action == TimeoutActionType.ABORT) {

            outputChannel.eventuallyAbort();
        }

        return outputChannel;
    }

    /**
     * Tells the channel that the invocation instance is not available.
     */
    void startWaitingInvocation() {

        synchronized (mMutex) {

            mIsWaitingInvocation = true;
            mMutex.notifyAll();
        }
    }

    /**
     * Tells the channel that the invocation instance became available.
     */
    void stopWaitingInvocation() {

        synchronized (mMutex) {

            mIsWaitingInvocation = false;
        }
    }

    private void closeConsumer(@Nonnull final OutputChannelState state,
            @Nonnull final OutputConsumer<? super OUTPUT> consumer) {

        state.closeConsumer(consumer);

        synchronized (mMutex) {

            final OutputChannelState currentState = mState;

            if (currentState.isReadyToComplete()) {

                mState = currentState.toDoneState();
                mMutex.notifyAll();
            }
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField", "unchecked"})
    private void flushOutput(final boolean forceClose) {

        RoutineException abortException = null;

        synchronized (mFlushMutex) {

            final Logger logger = mLogger;
            final OutputChannelState state;
            final ArrayList<Object> outputs;
            final OutputConsumer<? super OUTPUT> consumer;
            final boolean isFinal;

            synchronized (mMutex) {

                state = mState;
                isFinal = state.isReadyToComplete();
                consumer = mOutputConsumer;

                if (consumer == null) {

                    logger.dbg("avoiding flushing output since channel is not bound");

                    if (isFinal) {

                        mState = state.toDoneState();
                    }

                    mMutex.notifyAll();
                    return;
                }

                outputs = new ArrayList<Object>();
                mOutputQueue.drainTo(outputs);
                mOutputCount = 0;
                mMutex.notifyAll();
            }

            synchronized (mConsumerMutex) {

                try {

                    for (final Object output : outputs) {

                        if (output instanceof RoutineExceptionWrapper) {

                            try {

                                logger.dbg("aborting consumer (%s): %s", consumer, output);
                                consumer.onError(((RoutineExceptionWrapper) output).raise());

                            } catch (final Throwable t) {

                                InvocationInterruptedException.ignoreIfPossible(t);
                                logger.wrn(t, "ignoring consumer exception (%s)", consumer);
                            }

                            break;

                        } else {

                            logger.dbg("output consumer (%s): %s", consumer, output);
                            consumer.onOutput((OUTPUT) output);
                        }
                    }

                    if (forceClose || isFinal) {

                        closeConsumer(state, consumer);
                    }

                } catch (final InvocationInterruptedException e) {

                    throw e;

                } catch (final Throwable t) {

                    final OutputChannelState finalState;
                    boolean isClose = false;

                    synchronized (mMutex) {

                        finalState = mState;
                        logger.wrn(t, "consumer exception (%s)", consumer);

                        if (forceClose || finalState.isReadyToComplete()) {

                            isClose = true;

                        } else {

                            abortException = finalState.abortConsumer(t);
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

    private boolean isNextAvailable(@Nonnull final TimeDuration timeout,
            @Nonnull final TimeoutActionType timeoutAction) {

        boolean isAbort = false;

        synchronized (mMutex) {

            verifyBound();
            final Logger logger = mLogger;
            final NestedQueue<Object> outputQueue = mOutputQueue;
            final boolean isDone = mState.isDone();

            if (timeout.isZero() || isDone) {

                if (outputQueue.isEmpty() && !isDone) {

                    logger.wrn("has output timeout: [%s] => [%s]", timeout, timeoutAction);

                    if (timeoutAction == TimeoutActionType.THROW) {

                        throw new ExecutionTimeoutException(
                                "timeout while waiting to know if more outputs are coming");

                    } else {

                        isAbort = (timeoutAction == TimeoutActionType.ABORT);
                    }
                }

            } else if (!outputQueue.isEmpty()) {

                logger.dbg("has output: true [%s]", timeout);
                return true;

            } else {

                if (mRunner.isExecutionThread()) {

                    throw new ExecutionDeadlockException();
                }

                if (mIsWaitingInvocation) {

                    throw new InvocationDeadlockException();
                }

                if (mOutputHasNext == null) {

                    mOutputHasNext = new Check() {

                        public boolean isTrue() {

                            return !outputQueue.isEmpty() || mState.isDone()
                                    || mIsWaitingInvocation;
                        }
                    };
                }

                final boolean isTimeout;

                try {

                    isTimeout = !timeout.waitTrue(mMutex, mOutputHasNext);

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }

                if (mIsWaitingInvocation) {

                    throw new InvocationDeadlockException();
                }

                if (isTimeout) {

                    logger.wrn("has output timeout: [%s] => [%s]", timeout, timeoutAction);

                    if (timeoutAction == TimeoutActionType.THROW) {

                        throw new ExecutionTimeoutException(
                                "timeout while waiting to know if more outputs are coming");

                    } else {

                        isAbort = (timeoutAction == TimeoutActionType.ABORT);
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
    @SuppressWarnings("unchecked")
    private OUTPUT nextOutput(@Nonnull final TimeDuration timeout) {

        final Object result = mOutputQueue.removeFirst();
        mLogger.dbg("reading output [#%d]: %s [%s]", mOutputCount, result, timeout);
        RoutineExceptionWrapper.raise(result);
        final int maxOutput = mMaxOutput;
        final int prevOutputCount = mOutputCount;

        if ((--mOutputCount <= maxOutput) && (prevOutputCount > maxOutput)) {

            mMutex.notifyAll();
        }

        return (OUTPUT) result;
    }

    @Nullable
    private OUTPUT readNext(@Nonnull final TimeDuration timeout,
            @Nonnull final TimeoutActionType timeoutAction) {

        boolean isAbort = false;

        try {

            synchronized (mMutex) {

                isAbort = (timeoutAction == TimeoutActionType.ABORT);
                return readQueue(timeout, timeoutAction);
            }

        } catch (final NoSuchElementException e) {

            if (isAbort) {

                abort();
                throw new AbortException(null);
            }

            throw e;
        }
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

                if (action == TimeoutActionType.THROW) {

                    throw new ExecutionTimeoutException("timeout while waiting for outputs");
                }
            }

            return nextOutput(timeout);
        }

        if (mRunner.isExecutionThread()) {

            throw new ExecutionDeadlockException();
        }

        if (mIsWaitingInvocation) {

            throw new InvocationDeadlockException();
        }

        if (mOutputNotEmpty == null) {

            mOutputNotEmpty = new Check() {

                public boolean isTrue() {

                    return !outputQueue.isEmpty() || mState.isDone() || mIsWaitingInvocation;
                }
            };
        }

        final boolean isTimeout;

        try {

            isTimeout = !timeout.waitTrue(mMutex, mOutputNotEmpty);

        } catch (final InterruptedException e) {

            throw new InvocationInterruptedException(e);
        }

        if (mIsWaitingInvocation) {

            throw new InvocationDeadlockException();
        }

        if (isTimeout) {

            logger.wrn("reading output timeout: [%s] => [%s]", timeout, action);

            if (action == TimeoutActionType.THROW) {

                throw new ExecutionTimeoutException("timeout while waiting for outputs");
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

    private void waitOutputs(final int count, @Nonnull final TimeDuration delay) {

        if (mOutputTimeout.isZero()) {

            mOutputCount -= count;
            throw new OutputTimeoutException(
                    "timeout while waiting for room in the output channel");
        }

        if (!delay.isZero() && mRunner.isExecutionThread()) {

            mOutputCount -= count;
            throw new OutputDeadlockException(
                    "cannot wait on the invocation runner thread: " + Thread.currentThread());
        }

        try {

            if (!mOutputTimeout.waitTrue(mMutex, mHasOutputs)) {

                mOutputCount -= count;
                throw new OutputTimeoutException(
                        "timeout while waiting for room in the output channel");
            }

        } catch (final InterruptedException e) {

            mOutputCount -= count;
            throw new InvocationInterruptedException(e);
        }
    }

    /**
     * Interface defining an abort handler.
     */
    interface AbortHandler {

        /**
         * Called on an abort.
         *
         * @param reason   the reason of the abortion.
         * @param delay    the abortion delay.
         * @param timeUnit the delay time unit.
         */
        void onAbort(@Nullable RoutineException reason, long delay, @Nonnull TimeUnit timeUnit);
    }

    /**
     * The invocation has been aborted and the exception put into the output queue.
     */
    private class AbortChannelState extends ExceptionChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nullable
        @Override
        RoutineException abortConsumer(@Nonnull final Throwable reason) {

            final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
            mSubLogger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            mOutputQueue.clear();
            mAbortException = abortException;
            mState = new ExceptionChannelState();
            return abortException;
        }

        @Override
        boolean isReadyToComplete() {

            return true;
        }

        @Override
        void closeConsumer(@Nonnull final OutputConsumer<? super OUTPUT> consumer) {

        }
    }

    /**
     * The invocation has completed with an abortion exception.
     */
    private class AbortedChannelState extends AbortChannelState {

        @Override
        boolean isReadyToComplete() {

            return false;
        }

        @Override
        boolean isDone() {

            return true;
        }

        @Nonnull
        @Override
        OutputChannelState toDoneState() {

            return this;
        }
    }

    /**
     * Default implementation of an output channel iterator.
     */
    private class DefaultIterator implements Iterator<OUTPUT> {

        private final TimeoutActionType mAction;

        private final TimeDuration mTimeout;

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

            return isNextAvailable(mTimeout, mAction);
        }

        @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT",
                justification = "readNext() method actually throws it")
        public OUTPUT next() {

            return readNext(mTimeout, mAction);
        }

        public void remove() {

            throw new UnsupportedOperationException();
        }
    }

    /**
     * Default implementation of a routine output channel.
     */
    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private TimeDuration mExecutionTimeout = ZERO;

        private TimeoutActionType mTimeoutActionType = TimeoutActionType.THROW;

        @Nonnull
        @SuppressWarnings("ConstantConditions")
        public OutputChannel<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            synchronized (mMutex) {

                if (timeout == null) {

                    mSubLogger.err("invalid null timeout");
                    throw new NullPointerException("the output timeout must not be null");
                }

                mExecutionTimeout = timeout;
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

            boolean isAbort = false;

            synchronized (mMutex) {

                verifyBound();
                final Logger logger = mSubLogger;

                if (results == null) {

                    logger.err("invalid null output list");
                    throw new NullPointerException("the result list must not be null");
                }

                final TimeDuration executionTimeout = mExecutionTimeout;
                final NestedQueue<Object> outputQueue = mOutputQueue;

                if (executionTimeout.isZero() || mState.isDone()) {

                    while (!outputQueue.isEmpty()) {

                        final OUTPUT result = nextOutput(executionTimeout);
                        logger.dbg("adding output to list: %s [%s]", result, executionTimeout);
                        results.add(result);
                    }

                    if (!mState.isDone()) {

                        final TimeoutActionType timeoutAction = mTimeoutActionType;
                        logger.wrn("list output timeout: [%s] => [%s]", executionTimeout,
                                   timeoutAction);

                        if (timeoutAction == TimeoutActionType.THROW) {

                            throw new ExecutionTimeoutException(
                                    "timeout while waiting to collect all outputs");

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

                                final OUTPUT result = nextOutput(executionTimeout);
                                logger.dbg("adding output to list: %s [%s]", result,
                                           executionTimeout);
                                results.add(result);
                            }

                            if (mState.isDone()) {

                                break;
                            }

                            if (mRunner.isExecutionThread()) {

                                throw new ExecutionDeadlockException();
                            }

                            if (mIsWaitingInvocation) {

                                throw new InvocationDeadlockException();
                            }

                        } while (executionTimeout.waitSinceMillis(mMutex, startTime));

                        isTimeout = !mState.isDone();

                    } catch (final InterruptedException e) {

                        throw new InvocationInterruptedException(e);
                    }

                    if (isTimeout) {

                        final TimeoutActionType action = mTimeoutActionType;
                        logger.wrn("list output timeout: [%s] => [%s]", executionTimeout, action);

                        if (action == TimeoutActionType.THROW) {

                            throw new ExecutionTimeoutException(
                                    "timeout while waiting to collect all outputs");

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

            synchronized (mMutex) {

                if (mState.isDone()) {

                    return true;
                }

                final TimeDuration executionTimeout = mExecutionTimeout;

                if (!executionTimeout.isZero()) {

                    if (mRunner.isExecutionThread()) {

                        throw new ExecutionDeadlockException();
                    }

                    if (mIsWaitingInvocation) {

                        throw new InvocationDeadlockException();
                    }
                }

                final boolean isDone;

                try {

                    isDone = executionTimeout.waitTrue(mMutex, new Check() {

                        public boolean isTrue() {

                            return mState.isDone() || mIsWaitingInvocation;
                        }
                    });

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }

                if (mIsWaitingInvocation) {

                    throw new InvocationDeadlockException();
                }

                if (!isDone) {

                    mSubLogger.wrn("waiting complete timeout: [%s]", executionTimeout);
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
        public OutputChannel<OUTPUT> eventuallyExit() {

            synchronized (mMutex) {

                mTimeoutActionType = TimeoutActionType.EXIT;
            }

            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> eventuallyThrow() {

            synchronized (mMutex) {

                mTimeoutActionType = TimeoutActionType.THROW;
            }

            return this;
        }

        public boolean hasNext() {

            final TimeDuration timeout;
            final TimeoutActionType timeoutAction;

            synchronized (mMutex) {

                timeout = mExecutionTimeout;
                timeoutAction = mTimeoutActionType;
            }

            return isNextAvailable(timeout, timeoutAction);
        }

        @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT",
                justification = "readNext() method actually throws it")
        public OUTPUT next() {

            final TimeDuration timeout;
            final TimeoutActionType timeoutAction;

            synchronized (mMutex) {

                timeout = mExecutionTimeout;
                timeoutAction = mTimeoutActionType;
            }

            return readNext(timeout, timeoutAction);
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
            final TimeoutActionType timeoutAction;

            synchronized (mMutex) {

                verifyBound();
                timeout = mExecutionTimeout;
                timeoutAction = mTimeoutActionType;
            }

            return new DefaultIterator(timeout, timeoutAction);
        }

        public void remove() {

            throw new UnsupportedOperationException();
        }

        public boolean abort() {

            return abort(null);
        }

        public boolean abort(@Nullable final Throwable reason) {

            final RoutineException abortException;

            synchronized (mMutex) {

                abortException = mState.abortOutputChannel(reason);
            }

            if (abortException != null) {

                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
                return true;
            }

            return false;
        }

        public boolean isOpen() {

            synchronized (mMutex) {

                return mState.isOpen();
            }
        }
    }

    /**
     * Default implementation of an output consumer pushing the data to consume into the output
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<OUTPUT> {

        private final TimeDuration mDelay;

        private final OrderType mOrderType;

        private final NestedQueue<Object> mQueue;

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Constructor.
         */
        private DefaultOutputConsumer() {

            final TimeDuration delay = (mDelay = mResultDelay);
            final OrderType order = (mOrderType = mResultOrder);
            mQueue = ((order == OrderType.BY_CALL) || ((order == OrderType.BY_DELAY)
                    && delay.isZero())) ? mOutputQueue.addNested() : mOutputQueue;
        }

        public void onComplete() {

            final boolean needsFlush;

            synchronized (mMutex) {

                needsFlush = mState.onConsumerComplete(mQueue);
                mSubLogger.dbg("closing output [%s]", needsFlush);
            }

            if (needsFlush) {

                flushOutput(false);
            }
        }

        public void onError(@Nullable final RoutineException error) {

            final boolean needsAbort;

            synchronized (mMutex) {

                needsAbort = mState.onConsumerError(error);
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

                execution = mState.onConsumerOutput(mQueue, output, delay, mOrderType);
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

            final RoutineException abortException;

            synchronized (mMutex) {

                abortException = mState.delayedAbortInvocation(mAbortException);
            }

            if (abortException != null) {

                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed output of a list of data.
     */
    private class DelayedListOutputExecution extends TemplateExecution {

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
    private class DelayedOutputExecution extends TemplateExecution {

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

    /**
     * The invocation has successfully completed.
     */
    private class DoneChannelState extends FlushChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nullable
        @Override
        RoutineException delayedAbortInvocation(@Nullable final RoutineException reason) {

            if (mOutputQueue.isEmpty()) {

                mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
                return null;
            }

            return super.delayedAbortInvocation(reason);
        }

        @Override
        boolean isDone() {

            return true;
        }

        @Override
        boolean isReadyToComplete() {

            return false;
        }

        @Nonnull
        @Override
        OutputChannelState toDoneState() {

            return this;
        }
    }

    /**
     * The invocation has been aborted with an exception.
     */
    private class ExceptionChannelState extends FlushChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nullable
        @Override
        RoutineException abortConsumer(@Nonnull final Throwable reason) {

            return null;
        }

        @Override
        void after(@Nonnull final TimeDuration delay) {

            throw abortException();
        }

        @Override
        boolean onConsumerComplete(@Nonnull final NestedQueue<Object> queue) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution onConsumerOutput(@Nonnull final NestedQueue<Object> queue, final OUTPUT output,
                @Nonnull final TimeDuration delay, final OrderType orderType) {

            throw abortException();
        }

        @Nonnull
        private RoutineException abortException() {

            final RoutineException abortException = mAbortException;
            mSubLogger.dbg(abortException, "abort exception");
            return mAbortException;
        }

        @Override
        void orderBy(@Nonnull final OrderType orderType) {

            throw abortException();
        }

        @Nullable
        @Override
        RoutineException delayedAbortInvocation(@Nullable final RoutineException reason) {

            mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Override
        boolean isReadyToComplete() {

            return false;
        }

        @Nullable
        @Override
        OutputConsumer<OUTPUT> pass(@Nullable final OutputChannel<? extends OUTPUT> channel) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUTPUT output) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUTPUT... outputs) {

            throw abortException();
        }

        @Nonnull
        @Override
        OutputChannelState toDoneState() {

            return new AbortedChannelState();
        }
    }

    /**
     * Invocation has completed and no output is pending.
     */
    private class FlushChannelState extends ResultChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nonnull
        private IllegalStateException exception() {

            mSubLogger.err("consumer invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Nullable
        @Override
        RoutineException abortInvocation(@Nullable final Throwable reason,
                @Nonnull final TimeDuration delay) {

            mSubLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Nullable
        @Override
        RoutineException abortOutputChannel(@Nullable final Throwable reason) {

            mSubLogger.dbg("avoiding aborting output since result channel is closed");
            return null;
        }

        @Override
        boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mSubLogger.dbg("avoiding delayed output execution since channel is closed: %s", output);
            return false;
        }

        @Override
        boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mSubLogger.dbg("avoiding delayed output execution since channel is closed: %s",
                           outputs);
            return false;
        }

        @Override
        boolean onConsumerComplete(@Nonnull final NestedQueue<Object> queue) {

            throw exception();
        }

        @Override
        boolean onConsumerError(@Nullable final RoutineException error) {

            mSubLogger.dbg("avoiding aborting output since result channel is closed");
            return false;
        }

        @Nullable
        @Override
        Execution onConsumerOutput(@Nonnull final NestedQueue<Object> queue, final OUTPUT output,
                @Nonnull final TimeDuration delay, final OrderType orderType) {

            throw exception();
        }

        @Override
        boolean isReadyToComplete() {

            return true;
        }
    }

    /**
     * Result channel internal state (using "state" design pattern).
     */
    private class OutputChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Called when a consumer cause the invocation to abort by throwing an exception.
         *
         * @param reason the reason of the abortion.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException abortConsumer(@Nonnull final Throwable reason) {

            final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
            mSubLogger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            mOutputQueue.clear();
            mAbortException = abortException;
            mState = new ExceptionChannelState();
            return abortException;
        }

        /**
         * Called when the invocation is aborted.
         *
         * @param reason the reason of the abortion.
         * @param delay  the abortion delay.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException abortInvocation(@Nullable final Throwable reason,
                @Nonnull final TimeDuration delay) {

            final RoutineException abortException = AbortException.wrapIfNeeded(reason);

            if (delay.isZero()) {

                mSubLogger.dbg(reason, "aborting channel");
                mOutputQueue.clear();
                mAbortException = abortException;
                mState = new ExceptionChannelState();
            }

            return abortException;
        }

        /**
         * Called when the output channel is closed with an exception.
         *
         * @param reason the reason of the abortion.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException abortOutputChannel(@Nullable final Throwable reason) {

            final RoutineException abortException = AbortException.wrapIfNeeded(reason);
            mSubLogger.dbg(reason, "aborting output");
            mOutputQueue.clear();
            mAbortException = abortException;
            mState = new ExceptionChannelState();
            return abortException;
        }

        /**
         * Called when a delay is set to the result channel.
         *
         * @param delay the delay.
         */
        @SuppressWarnings("ConstantConditions")
        void after(@Nonnull final TimeDuration delay) {

            if (delay == null) {

                mSubLogger.err("invalid null delay");
                throw new NullPointerException("the input delay must not be null");
            }

            mResultDelay = delay;
        }

        /**
         * Called when the specified consumer is closed.
         *
         * @param consumer the consumer instance.
         */
        void closeConsumer(@Nonnull final OutputConsumer<? super OUTPUT> consumer) {

            final Logger logger = mSubLogger;

            try {

                logger.dbg("closing consumer (%s)", consumer);
                consumer.onComplete();

            } catch (final Throwable t) {

                InvocationInterruptedException.ignoreIfPossible(t);
                logger.wrn(t, "ignoring consumer exception (%s)", consumer);
            }
        }

        /**
         * Called when the result channel is closed.
         *
         * @return whether the internal state has changed.
         */
        boolean closeResultChannel() {

            mSubLogger.dbg("closing result channel [#%d]", mPendingOutputCount);

            if (mPendingOutputCount > 0) {

                mState = new ResultChannelState();

            } else {

                mState = new FlushChannelState();
            }

            return true;
        }

        /**
         * Called when the invocation is aborted after a delay.
         *
         * @param reason the reason of the abortion.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException delayedAbortInvocation(@Nullable final RoutineException reason) {

            mSubLogger.dbg(reason, "aborting channel");
            mOutputQueue.clear();
            mAbortException = reason;
            mState = new ExceptionChannelState();
            return reason;
        }

        /**
         * Called when an output is passed to the invocation after a delay.
         *
         * @param queue  the output queue.
         * @param output the output.
         * @return whether the queue content has changed.
         */
        boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mSubLogger.dbg("delayed output execution: %s", output);
            --mPendingOutputCount;
            queue.add(output);
            queue.close();
            return true;
        }

        /**
         * Called when some outputs are passed to the invocation after a delay.
         *
         * @param queue   the output queue.
         * @param outputs the outputs.
         * @return whether the queue content has changed.
         */
        boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mSubLogger.dbg("delayed output execution: %s", outputs);
            --mPendingOutputCount;
            queue.addAll(outputs);
            queue.close();
            return true;
        }

        /**
         * Called to know if the invocation has completed.
         *
         * @return whether the invocation is complete.
         */
        boolean isDone() {

            return false;
        }

        /**
         * Called to know if the channel is open.
         *
         * @return whether the channel is open.
         */
        boolean isOpen() {

            return true;
        }

        /**
         * Called to know if this state is ready to complete.
         *
         * @return whether the state is ready to complete.
         */
        boolean isReadyToComplete() {

            return false;
        }

        /**
         * Called when the feeding consumer completes.
         *
         * @param queue the output queue.
         * @return whether the queue content has changed.
         */
        boolean onConsumerComplete(@Nonnull final NestedQueue<Object> queue) {

            queue.close();
            --mPendingOutputCount;
            mMutex.notifyAll();
            return false;
        }

        /**
         * Called when the feeding consumer receives an error.
         *
         * @param error the error.
         * @return whether the queue content has changed.
         */
        boolean onConsumerError(@Nullable final RoutineException error) {

            mSubLogger.dbg(error, "aborting output");
            mOutputQueue.clear();
            mAbortException = error;
            mState = new ExceptionChannelState();
            return true;
        }

        /**
         * Called when the feeding consumer receives an output.
         *
         * @param queue     the output queue.
         * @param output    the output.
         * @param delay     the output delay.
         * @param orderType the result order type.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerOutput(@Nonnull final NestedQueue<Object> queue, final OUTPUT output,
                @Nonnull final TimeDuration delay, final OrderType orderType) {

            mSubLogger.dbg("consumer output [#%d+1]: %s [%s]", mOutputCount, output, delay);
            ++mOutputCount;

            if (!mHasOutputs.isTrue()) {

                waitOutputs(1, delay);

                if (mState != this) {

                    --mOutputCount;
                    return mState.onConsumerOutput(queue, output, delay, orderType);
                }
            }

            if (delay.isZero()) {

                queue.add(output);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedOutputExecution(
                    (orderType != OrderType.BY_CHANCE) ? queue.addNested() : queue, output);
        }

        /**
         * Called to set the result delivery order.
         *
         * @param orderType the result order type.
         */
        void orderBy(@Nonnull final OrderType orderType) {

            mResultOrder = orderType;
        }

        /**
         * Called when an output channel is passed to the result channel.
         *
         * @param channel the channel instance.
         * @return the output consumer to bind or null.
         */
        @Nullable
        OutputConsumer<OUTPUT> pass(@Nullable final OutputChannel<? extends OUTPUT> channel) {

            if (channel == null) {

                mSubLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingOutputCount;
            mSubLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer();
        }

        /**
         * Called when some outputs are passed to the result channel.
         *
         * @param outputs the outputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            if (outputs == null) {

                mSubLogger.wrn("passing null iterable");
                return null;
            }

            final ArrayList<OUTPUT> list = new ArrayList<OUTPUT>();

            for (final OUTPUT output : outputs) {

                list.add(output);
            }

            final int size = list.size();

            if (size > mMaxOutput) {

                throw new OutputTimeoutException(
                        "outputs exceed maximum channel size [" + size + "/" + mMaxOutput + "]");
            }

            final TimeDuration delay = mResultDelay;
            mSubLogger.dbg("passing iterable [#%d+%d]: %s [%s]", mOutputCount, size, outputs,
                           delay);
            mOutputCount += size;

            if (!mHasOutputs.isTrue()) {

                waitOutputs(size, delay);

                if (mState != this) {

                    mOutputCount -= size;
                    return mState.pass(outputs);
                }
            }

            if (delay.isZero()) {

                mOutputQueue.addAll(list);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedListOutputExecution(
                    (mResultOrder != OrderType.BY_CHANCE) ? mOutputQueue.addNested() : mOutputQueue,
                    list);
        }

        /**
         * Called when an output is passed to the result channel.
         *
         * @param output the output.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final OUTPUT output) {

            final TimeDuration delay = mResultDelay;
            mSubLogger.dbg("passing output [#%d+1]: %s [%s]", mOutputCount, output, delay);
            ++mOutputCount;

            if (!mHasOutputs.isTrue()) {

                waitOutputs(1, delay);

                if (mState != this) {

                    --mOutputCount;
                    return mState.pass(output);
                }
            }

            if (delay.isZero()) {

                mOutputQueue.add(output);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedOutputExecution(
                    (mResultOrder != OrderType.BY_CHANCE) ? mOutputQueue.addNested() : mOutputQueue,
                    output);
        }

        /**
         * Called when some outputs are passed to the result channel.
         *
         * @param outputs the outputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final OUTPUT... outputs) {

            if (outputs == null) {

                mSubLogger.wrn("passing null output array");
                return null;
            }

            final int size = outputs.length;

            if (size > mMaxOutput) {

                throw new OutputTimeoutException(
                        "outputs exceed maximum channel size [" + size + "/" + mMaxOutput + "]");
            }

            final TimeDuration delay = mResultDelay;
            mSubLogger.dbg("passing array [#%d+%d]: %s [%s]", mOutputCount, size, outputs, delay);
            mOutputCount += size;

            if (!mHasOutputs.isTrue()) {

                waitOutputs(size, delay);

                if (mState != this) {

                    mOutputCount -= size;
                    return mState.pass(outputs);
                }
            }

            final ArrayList<OUTPUT> list = new ArrayList<OUTPUT>(size);
            Collections.addAll(list, outputs);

            if (delay.isZero()) {

                mOutputQueue.addAll(list);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedListOutputExecution(
                    (mResultOrder != OrderType.BY_CHANCE) ? mOutputQueue.addNested() : mOutputQueue,
                    list);
        }

        /**
         * Converts this state to done.
         *
         * @return the done state.
         */
        @Nonnull
        OutputChannelState toDoneState() {

            return new DoneChannelState();
        }
    }

    /**
     * Invocation has completed but some outputs are still pending.
     */
    private class ResultChannelState extends OutputChannelState {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        @Nonnull
        private IllegalStateException exception() {

            mSubLogger.err("invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Override
        void after(@Nonnull final TimeDuration delay) {

            throw exception();
        }

        @Override
        boolean closeResultChannel() {

            mSubLogger.dbg("avoiding closing result channel since already closed");
            return false;
        }

        @Override
        boolean delayedOutput(@Nonnull final NestedQueue<Object> queue,
                @Nullable final OUTPUT output) {

            mSubLogger.dbg("delayed output execution: %s", output);

            if (--mPendingOutputCount == 0) {

                mState = new FlushChannelState();
            }

            queue.add(output);
            queue.close();
            return true;
        }

        @Override
        boolean delayedOutputs(@Nonnull final NestedQueue<Object> queue,
                final List<OUTPUT> outputs) {

            mSubLogger.dbg("delayed output execution: %s", outputs);

            if (--mPendingOutputCount == 0) {

                mState = new FlushChannelState();
            }

            queue.addAll(outputs);
            queue.close();
            return true;
        }

        @Override
        void orderBy(@Nonnull final OrderType orderType) {

            throw exception();
        }

        @Override
        boolean isOpen() {

            return false;
        }

        @Override
        boolean onConsumerComplete(@Nonnull final NestedQueue<Object> queue) {

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
        OutputConsumer<OUTPUT> pass(@Nullable final OutputChannel<? extends OUTPUT> channel) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUTPUT output) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUTPUT... outputs) {

            throw exception();
        }
    }

    public boolean abort(@Nullable final Throwable reason) {

        final TimeDuration delay;
        final RoutineException abortException;

        synchronized (mMutex) {

            delay = mResultDelay;
            abortException = mState.abortInvocation(reason, delay);
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

    public boolean isOpen() {

        synchronized (mMutex) {

            return mState.isOpen();
        }
    }
}
