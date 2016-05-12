/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.ExecutionTimeoutException;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.core.util.UnitDuration.Condition;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.zero;

/**
 * Class handling the invocation output.
 * <p>
 * This class centralizes the managing of data passing through the routine output and result
 * channels, since, logically, the two objects are part of the same entity. In fact, on one end the
 * result channel puts data into the output queue and, on the other end, the output channel reads
 * them from the same queue.
 * <p>
 * Created by davide-maestroni on 06/12/2015.
 *
 * @param <OUT> the output data type.
 */
class DefaultResultChannel<OUT> implements ResultChannel<OUT> {

    private static final String INVOCATION_DEADLOCK_MESSAGE =
            "cannot wait while no invocation instance is available"
                    + "\nTry increasing the max number of instances";

    private static final WeakIdentityHashMap<OutputConsumer<?>, Object> sConsumerMutexes =
            new WeakIdentityHashMap<OutputConsumer<?>, Object>();

    private static final LocalMutableBoolean sInsideInvocation = new LocalMutableBoolean();

    private static final Runner sSyncRunner = Runners.syncRunner();

    private final ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private final SimpleQueue<WrappedExecution> mExecutionQueue =
            new SimpleQueue<WrappedExecution>();

    private final UnitDuration mExecutionTimeout;

    private final Object mFlushMutex = new Object();

    private final AbortHandler mHandler;

    private final Condition mHasOutputs;

    private final Logger mLogger;

    private final int mMaxOutput;

    private final Object mMutex = new Object();

    private final Backoff mOutputBackoff;

    private final int mOutputLimit;

    private final NestedQueue<Object> mOutputQueue;

    private final Runner mRunner;

    private final TimeoutActionType mTimeoutActionType;

    private RoutineException mAbortException;

    private Object mConsumerMutex;

    private FlushExecution mFlushExecution;

    private FlushExecution mForcedFlushExecution;

    private boolean mIsWaitingExecution;

    private boolean mIsWaitingInvocation;

    private OutputConsumer<? super OUT> mOutputConsumer;

    private int mOutputCount;

    private Condition mOutputHasNext;

    private Condition mOutputNotEmpty;

    private int mPendingOutputCount;

    private long mResultDelay = 0;

    private TimeUnit mResultDelayUnit = TimeUnit.MILLISECONDS;

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
    DefaultResultChannel(@NotNull final InvocationConfiguration configuration,
            @NotNull final AbortHandler handler, @NotNull final Runner runner,
            @NotNull final Logger logger) {

        mLogger = logger.subContextLogger(this);
        mHandler = ConstantConditions.notNull("abort handler", handler);
        mRunner = ConstantConditions.notNull("runner instance", runner);
        mResultOrder = configuration.getOutputOrderTypeOrElse(OrderType.BY_DELAY);
        mExecutionTimeout = configuration.getReadTimeoutOrElse(zero());
        mTimeoutActionType = configuration.getReadTimeoutActionOrElse(TimeoutActionType.THROW);
        mOutputLimit = configuration.getOutputLimitOrElse(Integer.MAX_VALUE);
        mOutputBackoff = configuration.getOutputBackoffOrElse(Backoffs.zeroDelay());
        mMaxOutput = configuration.getOutputMaxSizeOrElse(Integer.MAX_VALUE);
        mOutputQueue = new NestedQueue<Object>() {

            @Override
            public void close() {

                // Preventing closing
            }
        };
        final int outputLimit = mOutputLimit;
        mHasOutputs = new Condition() {

            public boolean isTrue() {

                return (mOutputCount <= outputLimit) || mIsWaitingInvocation || (mAbortException
                        != null);
            }
        };
        mState = new OutputChannelState();
    }

    @NotNull
    private static Object getMutex(@NotNull final OutputConsumer<?> consumer) {

        synchronized (sConsumerMutexes) {
            final WeakIdentityHashMap<OutputConsumer<?>, Object> consumerMutexes = sConsumerMutexes;
            Object mutex = consumerMutexes.get(consumer);
            if (mutex == null) {
                mutex = new Object();
                consumerMutexes.put(consumer, mutex);
            }

            return mutex;
        }
    }

    public boolean abort() {

        return abort(null);
    }

    @NotNull
    public ResultChannel<OUT> after(@NotNull final UnitDuration delay) {

        return after(delay.value, delay.unit);
    }

    @NotNull
    public ResultChannel<OUT> after(final long delay, @NotNull final TimeUnit timeUnit) {

        synchronized (mMutex) {
            mState.after(delay, timeUnit);
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> now() {

        return after(zero());
    }

    @NotNull
    public ResultChannel<OUT> orderByCall() {

        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_CALL);
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> orderByDelay() {

        synchronized (mMutex) {
            mState.orderBy(OrderType.BY_DELAY);
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> pass(@Nullable final OutputChannel<? extends OUT> channel) {

        final OutputConsumer<OUT> consumer;
        synchronized (mMutex) {
            consumer = mState.pass(channel);
        }

        if ((consumer != null) && (channel != null)) {
            channel.bind(consumer);
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> pass(@Nullable final Iterable<? extends OUT> outputs) {

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mResultDelay;
            timeUnit = mResultDelayUnit;
            execution = mState.pass(outputs);
        }

        if (delay == 0) {
            runFlush(false);

        } else if (execution != null) {
            runExecution(execution, delay, timeUnit);
        }

        synchronized (mMutex) {
            if (!mHasOutputs.isTrue()) {
                waitOutputs();
            }
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> pass(@Nullable final OUT output) {

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mResultDelay;
            timeUnit = mResultDelayUnit;
            execution = mState.pass(output);
        }

        if (delay == 0) {
            runFlush(false);

        } else if (execution != null) {
            runExecution(execution, delay, timeUnit);
        }

        synchronized (mMutex) {
            if (!mHasOutputs.isTrue()) {
                waitOutputs();
            }
        }

        return this;
    }

    @NotNull
    public ResultChannel<OUT> pass(@Nullable final OUT... outputs) {

        final long delay;
        final TimeUnit timeUnit;
        final Execution execution;
        synchronized (mMutex) {
            delay = mResultDelay;
            timeUnit = mResultDelayUnit;
            execution = mState.pass(outputs);
        }

        if (delay == 0) {
            runFlush(false);

        } else if (execution != null) {
            runExecution(execution, delay, timeUnit);
        }

        synchronized (mMutex) {
            if (!mHasOutputs.isTrue()) {
                waitOutputs();
            }
        }

        return this;
    }

    /**
     * Aborts immediately the execution.
     *
     * @param reason the reason of the abortion.
     * @see com.github.dm.jrt.core.channel.Channel#abort(Throwable) Channel.abort(Throwable)
     */
    void abortImmediately(@Nullable final Throwable reason) {

        RoutineException abortException = InvocationException.wrapIfNeeded(reason);
        synchronized (mMutex) {
            abortException = mState.abortInvocation(abortException, 0, TimeUnit.MILLISECONDS);
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
            mPendingOutputCount = 0;
            if (mAbortException == null) {
                mAbortException = abortException;
            }

            mState = new AbortChannelState();
            mMutex.notifyAll();
        }

        for (final OutputChannel<?> channel : channels) {
            channel.abort(abortException);
        }

        runFlush(false);
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
            runFlush(false);
        }
    }

    /**
     * Notifies to this channel that the invocation execution is entering.
     */
    void enterInvocation() {

        sInsideInvocation.get().mIsTrue = true;
    }

    /**
     * Notifies to this channel that the invocation execution is exiting.
     */
    void exitInvocation() {

        sInsideInvocation.get().mIsTrue = false;
    }

    /**
     * Returns the output channel reading the data pushed into this channel.
     *
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> getOutput() {

        final TimeoutActionType action = mTimeoutActionType;
        final OutputChannel<OUT> outputChannel =
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

    private void checkCanWait() {

        if (sInsideInvocation.get().mIsTrue) {
            throw new ExecutionDeadlockException(
                    "cannot wait inside an invocation execution: " + Thread.currentThread()
                            + "\nTry binding the output channel");
        }

        if (mRunner.isExecutionThread()) {
            throw new ExecutionDeadlockException(
                    "cannot wait on the channel runner thread: " + Thread.currentThread()
                            + "\nTry binding the output channel or employing a different runner "
                            + "than: " + mRunner);
        }

        if (mIsWaitingInvocation) {
            throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
        }
    }

    private void checkMaxSize() {

        if (mOutputCount > mMaxOutput) {
            throw new OutputDeadlockException(
                    "maximum output channel size has been reached: " + mMaxOutput);
        }
    }

    private void closeConsumer(@NotNull final OutputChannelState state,
            @NotNull final OutputConsumer<? super OUT> consumer) {

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
            final OutputConsumer<? super OUT> consumer;
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

                            } catch (final RoutineException e) {
                                InvocationInterruptedException.throwIfInterrupt(e);
                                logger.wrn(e, "ignoring consumer exception (%s)", consumer);

                            } catch (final Throwable t) {
                                InvocationInterruptedException.throwIfInterrupt(t);
                                logger.err(t, "ignoring consumer exception (%s)", consumer);
                            }

                            break;

                        } else {
                            logger.dbg("output consumer (%s): %s", consumer, output);
                            consumer.onOutput((OUT) output);
                        }
                    }

                    if (forceClose || isFinal) {
                        closeConsumer(state, consumer);
                    }

                } catch (final InvocationInterruptedException e) {
                    throw e;

                } catch (final Throwable t) {
                    synchronized (mMutex) {
                        logger.wrn(t, "consumer exception (%s)", consumer);
                        abortException = mState.abortConsumer(t);
                    }
                }
            }
        }

        if (abortException != null) {
            mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);
        }
    }

    private void forceExecution(@NotNull final Execution execution) {

        final SimpleQueue<WrappedExecution> queue = mExecutionQueue;
        synchronized (mMutex) {
            if (mIsWaitingExecution) {
                queue.add(new WrappedExecution(execution));
                return;
            }

            mIsWaitingExecution = true;
        }

        sSyncRunner.run(execution, 0, TimeUnit.MILLISECONDS);
        final WrappedExecution nextExecution;
        synchronized (mMutex) {
            if (!queue.isEmpty()) {
                nextExecution = queue.removeFirst();

            } else {
                mIsWaitingExecution = false;
                nextExecution = null;
            }
        }

        if (nextExecution != null) {
            mRunner.run(nextExecution, 0, TimeUnit.MILLISECONDS);
        }
    }

    private void internalAbort(@NotNull final RoutineException abortException) {

        mOutputQueue.clear();
        mPendingOutputCount = 0;
        mAbortException = abortException;
        mState = new ExceptionChannelState();
        mMutex.notifyAll();
    }

    private boolean isNextAvailable(final long timeout, @NotNull final TimeUnit timeUnit,
            @NotNull final TimeoutActionType timeoutAction,
            @Nullable final Throwable timeoutException) {

        boolean isAbort = false;
        synchronized (mMutex) {
            verifyBound();
            final Logger logger = mLogger;
            final NestedQueue<Object> outputQueue = mOutputQueue;
            final boolean isDone = mState.isDone();
            final boolean hasOutputs = !outputQueue.isEmpty();
            if (isDone || hasOutputs) {
                logger.dbg("has output: %s [%d %s]", hasOutputs, timeout, timeUnit);
                return hasOutputs;

            } else if (timeout == 0) {
                logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit, timeoutAction);
                if (timeoutAction == TimeoutActionType.THROW) {
                    throw new ExecutionTimeoutException(
                            "timeout while waiting to know if more outputs are coming [" + timeout
                                    + "]");

                } else {
                    isAbort = (timeoutAction == TimeoutActionType.ABORT);
                }

            } else {
                checkCanWait();
                if (mOutputHasNext == null) {
                    mOutputHasNext = new Condition() {

                        public boolean isTrue() {

                            return !outputQueue.isEmpty() || mState.isDone()
                                    || mIsWaitingInvocation;
                        }
                    };
                }

                final boolean isTimeout;
                try {
                    isTimeout = !UnitDuration.waitTrue(timeout, timeUnit, mMutex, mOutputHasNext);
                    verifyBound();

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }

                if (outputQueue.isEmpty() && !mState.isDone() && mIsWaitingInvocation) {
                    throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
                }

                if (isTimeout) {
                    logger.wrn("has output timeout: [%d %s] => [%s]", timeout, timeUnit,
                            timeoutAction);
                    if (timeoutAction == TimeoutActionType.THROW) {
                        throw new ExecutionTimeoutException(
                                "timeout while waiting to know if more outputs are coming ["
                                        + timeout + "]");

                    } else {
                        isAbort = (timeoutAction == TimeoutActionType.ABORT);
                    }
                }
            }

            if (!isAbort) {
                final boolean hasNext = !outputQueue.isEmpty();
                logger.dbg("has output: %s [%d %s]", hasNext, timeout, timeUnit);
                return hasNext;
            }
        }

        abort(timeoutException);
        throw AbortException.wrapIfNeeded(timeoutException);
    }

    private void nextExecution() {

        final SimpleQueue<WrappedExecution> queue = mExecutionQueue;
        final WrappedExecution nextExecution;
        synchronized (mMutex) {
            if (!queue.isEmpty()) {
                nextExecution = queue.removeFirst();

            } else {
                mIsWaitingExecution = false;
                nextExecution = null;
            }
        }

        if (nextExecution != null) {
            mRunner.run(nextExecution, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private OUT nextOutput(final long timeout, @NotNull final TimeUnit timeUnit) {

        final Object result = mOutputQueue.removeFirst();
        mLogger.dbg("reading output [#%d]: %s [%d %s]", mOutputCount, result, timeout, timeUnit);
        if (result instanceof RoutineExceptionWrapper) {
            mOutputQueue.add(result);
            throw ((RoutineExceptionWrapper) result).raise();
        }

        final int outputLimit = mOutputLimit;
        final int prevOutputCount = mOutputCount;
        if ((--mOutputCount <= outputLimit) && (prevOutputCount > outputLimit)) {
            mMutex.notifyAll();
        }

        return (OUT) result;
    }

    @Nullable
    private OUT readNext(final long timeout, @NotNull final TimeUnit timeUnit,
            @NotNull final TimeoutActionType timeoutAction,
            @Nullable final Throwable timeoutException) {

        final boolean isTimeout;
        synchronized (mMutex) {
            verifyBound();
            final Logger logger = mLogger;
            final NestedQueue<Object> outputQueue = mOutputQueue;
            boolean isAbort = false;
            if ((timeout == 0) || !outputQueue.isEmpty()) {
                if (outputQueue.isEmpty()) {
                    logger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit,
                            timeoutAction);
                    if (timeoutAction == TimeoutActionType.THROW) {
                        throw new ExecutionTimeoutException(
                                "timeout while waiting for outputs [" + timeout + "]");
                    }

                    isAbort = (timeoutAction == TimeoutActionType.ABORT);
                }

            } else {
                checkCanWait();
                if (mOutputNotEmpty == null) {
                    mOutputNotEmpty = new Condition() {

                        public boolean isTrue() {

                            return !outputQueue.isEmpty() || mState.isDone() ||
                                    mIsWaitingInvocation;
                        }
                    };
                }

                try {
                    isTimeout = !UnitDuration.waitTrue(timeout, timeUnit, mMutex, mOutputNotEmpty);
                    verifyBound();

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }

                if (outputQueue.isEmpty() && !mState.isDone() && mIsWaitingInvocation) {
                    throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
                }

                if (isTimeout) {
                    logger.wrn("reading output timeout: [%d %s] => [%s]", timeout, timeUnit,
                            timeoutAction);
                    if (timeoutAction == TimeoutActionType.THROW) {
                        throw new ExecutionTimeoutException(
                                "timeout while waiting for outputs [" + timeout + "]");
                    }

                    isAbort = (timeoutAction == TimeoutActionType.ABORT);
                }
            }

            if (!isAbort) {
                return nextOutput(timeout, timeUnit);
            }
        }

        abort(timeoutException);
        throw AbortException.wrapIfNeeded(timeoutException);
    }

    private void runExecution(@NotNull final Execution execution, final long delay,
            @NotNull final TimeUnit timeUnit) {

        if (delay > 0) {
            mRunner.run(new DelayedWrappedExecution(execution), delay, timeUnit);

        } else {
            final WrappedExecution wrappedExecution = new WrappedExecution(execution);
            synchronized (mMutex) {
                if (mIsWaitingExecution) {
                    mExecutionQueue.add(wrappedExecution);
                    return;
                }

                mIsWaitingExecution = true;
            }

            mRunner.run(wrappedExecution, delay, timeUnit);
        }
    }

    private void runFlush(final boolean forceClose) {

        // Need to make sure to pass the outputs to the consumer in the runner thread, so to avoid
        // deadlock issues
        if (mRunner.isExecutionThread()) {
            flushOutput(forceClose);

        } else {
            final FlushExecution execution;
            if (forceClose) {
                if (mForcedFlushExecution == null) {
                    mForcedFlushExecution = new FlushExecution(true);
                }

                execution = mForcedFlushExecution;

            } else {
                if (mFlushExecution == null) {
                    mFlushExecution = new FlushExecution(false);
                }

                execution = mFlushExecution;
            }

            runExecution(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    private void verifyBound() {

        if (mOutputConsumer != null) {
            mLogger.err("invalid call on bound channel");
            throw new IllegalStateException("the channel is already bound");
        }
    }

    private void waitOutputs() {

        try {
            final long delay = mOutputBackoff.getDelay(mOutputCount - mOutputLimit);
            if (!UnitDuration.waitTrue(delay, TimeUnit.MILLISECONDS, mMutex, mHasOutputs)) {
                mLogger.dbg("timeout while waiting for room in the output channel [%s]", delay);
            }

        } catch (final InterruptedException e) {
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
        void onAbort(@NotNull RoutineException reason, long delay, @NotNull TimeUnit timeUnit);
    }

    /**
     * Thread local implementation storing a mutable boolean.
     */
    private static class LocalMutableBoolean extends ThreadLocal<MutableBoolean> {

        @Override
        protected MutableBoolean initialValue() {

            return new MutableBoolean();
        }
    }

    /**
     * Simple data class storing a boolean.
     */
    private static class MutableBoolean {

        private boolean mIsTrue;
    }

    /**
     * The invocation has been aborted and the exception put into the output queue.
     */
    private class AbortChannelState extends ExceptionChannelState {

        @Nullable
        @Override
        RoutineException abortConsumer(@NotNull final Throwable reason) {

            final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
            mLogger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            internalAbort(abortException);
            return abortException;
        }

        @Override
        boolean isReadyToComplete() {

            return true;
        }

        @Override
        void closeConsumer(@NotNull final OutputConsumer<? super OUT> consumer) {

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

        @NotNull
        @Override
        OutputChannelState toDoneState() {

            return this;
        }
    }

    /**
     * Default implementation of an output channel iterator.
     */
    private class DefaultIterator implements Iterator<OUT> {

        private final TimeoutActionType mAction;

        private final Throwable mException;

        private final long mTimeout;

        private final TimeUnit mTimeoutUnit;

        /**
         * Constructor.
         *
         * @param timeout   the output timeout.
         * @param timeUnit  the output timeout unit.
         * @param action    the timeout action.
         * @param exception the timeout exception.
         */
        private DefaultIterator(final long timeout, @NotNull final TimeUnit timeUnit,
                @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {

            mTimeout = timeout;
            mTimeoutUnit = timeUnit;
            mAction = action;
            mException = exception;
        }

        public boolean hasNext() {

            return isNextAvailable(mTimeout, mTimeoutUnit, mAction, mException);
        }

        public OUT next() {

            return readNext(mTimeout, mTimeoutUnit, mAction, mException);
        }

        public void remove() {

            ConstantConditions.unsupported();
        }
    }

    /**
     * Default implementation of a routine output channel.
     */
    private class DefaultOutputChannel implements OutputChannel<OUT> {

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        private long mExecutionTimeout = 0;

        private TimeUnit mExecutionTimeoutUnit = TimeUnit.MILLISECONDS;

        private TimeoutActionType mTimeoutActionType = TimeoutActionType.THROW;

        private Throwable mTimeoutException;

        @NotNull
        public OutputChannel<OUT> afterMax(@NotNull final UnitDuration timeout) {

            return afterMax(timeout.value, timeout.unit);
        }

        @NotNull
        public OutputChannel<OUT> afterMax(final long timeout, @NotNull final TimeUnit timeUnit) {

            synchronized (mMutex) {
                mExecutionTimeout = ConstantConditions.notNegative("output timeout", timeout);
                mExecutionTimeoutUnit = ConstantConditions.notNull("output timeout unit", timeUnit);
            }

            return this;
        }

        @NotNull
        public List<OUT> all() {

            final ArrayList<OUT> results = new ArrayList<OUT>();
            allInto(results);
            return results;
        }

        @NotNull
        public OutputChannel<OUT> allInto(@NotNull final Collection<? super OUT> results) {

            ConstantConditions.notNull("results collection", results);
            final Iterator<OUT> iterator = eventualIterator();
            while (iterator.hasNext()) {
                results.add(iterator.next());
            }

            return this;
        }

        @NotNull
        public <IN extends InputChannel<? super OUT>> IN bind(@NotNull final IN channel) {

            channel.pass(this);
            return channel;
        }

        @NotNull
        public OutputChannel<OUT> bind(@NotNull final OutputConsumer<? super OUT> consumer) {

            final boolean forceClose;
            synchronized (mMutex) {
                verifyBound();
                forceClose = mState.isDone();
                mOutputConsumer = ConstantConditions.notNull("output consumer", consumer);
                mConsumerMutex = getMutex(consumer);
            }

            runFlush(forceClose);
            return this;
        }

        @NotNull
        public Iterator<OUT> eventualIterator() {

            final long timeout;
            final TimeUnit timeUnit;
            final TimeoutActionType timeoutAction;
            final Throwable timeoutException;
            synchronized (mMutex) {
                verifyBound();
                timeout = mExecutionTimeout;
                timeUnit = mExecutionTimeoutUnit;
                timeoutAction = mTimeoutActionType;
                timeoutException = mTimeoutException;
            }

            return new EventualIterator(timeout, timeUnit, timeoutAction, timeoutException);
        }

        @NotNull
        public OutputChannel<OUT> eventuallyAbort() {

            synchronized (mMutex) {
                mTimeoutActionType = TimeoutActionType.ABORT;
                mTimeoutException = null;
            }

            return this;
        }

        @NotNull
        public OutputChannel<OUT> eventuallyAbort(@Nullable final Throwable reason) {

            synchronized (mMutex) {
                mTimeoutActionType = TimeoutActionType.ABORT;
                mTimeoutException = reason;
            }

            return this;
        }

        @NotNull
        public OutputChannel<OUT> eventuallyExit() {

            synchronized (mMutex) {
                mTimeoutActionType = TimeoutActionType.EXIT;
                mTimeoutException = null;
            }

            return this;
        }

        @NotNull
        public OutputChannel<OUT> eventuallyThrow() {

            synchronized (mMutex) {
                mTimeoutActionType = TimeoutActionType.THROW;
                mTimeoutException = null;
            }

            return this;
        }

        @Nullable
        public RoutineException getError() {

            synchronized (mMutex) {
                if (mState.isDone()) {
                    return mAbortException;
                }

                final long executionTimeout = mExecutionTimeout;
                if (executionTimeout > 0) {
                    checkCanWait();
                }

                final TimeUnit executionTimeoutUnit = mExecutionTimeoutUnit;
                final boolean isDone;
                try {
                    isDone = UnitDuration.waitTrue(executionTimeout, executionTimeoutUnit, mMutex,
                            new Condition() {

                                public boolean isTrue() {

                                    return mState.isDone() || (mAbortException != null)
                                            || mIsWaitingInvocation;
                                }
                            });

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }

                if (!mState.isDone() && (mAbortException == null) && mIsWaitingInvocation) {
                    throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
                }

                if (!isDone) {
                    mSubLogger.wrn("waiting error timeout: [%d %s]", executionTimeout,
                            executionTimeoutUnit);
                }

                return mAbortException;
            }
        }

        public boolean hasCompleted() {

            synchronized (mMutex) {
                if (mState.isDone()) {
                    return true;
                }

                final long executionTimeout = mExecutionTimeout;
                if (executionTimeout > 0) {
                    checkCanWait();
                }

                final TimeUnit executionTimeoutUnit = mExecutionTimeoutUnit;
                final boolean isDone;
                try {
                    isDone = UnitDuration.waitTrue(executionTimeout, executionTimeoutUnit, mMutex,
                            new Condition() {

                                public boolean isTrue() {

                                    return mState.isDone() || mIsWaitingInvocation;
                                }
                            });

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }

                if (!mState.isDone() && mIsWaitingInvocation) {
                    throw new InvocationDeadlockException(INVOCATION_DEADLOCK_MESSAGE);
                }

                if (!isDone) {
                    mSubLogger.wrn("waiting done timeout: [%d %s]", executionTimeout,
                            executionTimeoutUnit);
                }

                return isDone;
            }
        }

        public boolean hasNext() {

            final long timeout;
            final TimeUnit timeUnit;
            final TimeoutActionType timeoutAction;
            final Throwable timeoutException;
            synchronized (mMutex) {
                timeout = mExecutionTimeout;
                timeUnit = mExecutionTimeoutUnit;
                timeoutAction = mTimeoutActionType;
                timeoutException = mTimeoutException;
            }

            return isNextAvailable(timeout, timeUnit, timeoutAction, timeoutException);
        }

        public OUT next() {

            final long timeout;
            final TimeUnit timeUnit;
            final TimeoutActionType timeoutAction;
            final Throwable timeoutException;
            synchronized (mMutex) {
                timeout = mExecutionTimeout;
                timeUnit = mExecutionTimeoutUnit;
                timeoutAction = mTimeoutActionType;
                timeoutException = mTimeoutException;
            }

            return readNext(timeout, timeUnit, timeoutAction, timeoutException);
        }

        @NotNull
        public OutputChannel<OUT> immediately() {

            return afterMax(zero());
        }

        public boolean isBound() {

            synchronized (mMutex) {
                return (mOutputConsumer != null);
            }
        }

        @NotNull
        public List<OUT> next(final int count) {

            if (count <= 0) {
                return Collections.emptyList();
            }

            final ArrayList<OUT> results = new ArrayList<OUT>(count);
            final Iterator<OUT> iterator = eventualIterator();
            for (int i = 0; i < count && iterator.hasNext(); ++i) {
                results.add(iterator.next());
            }

            return results;
        }

        public OUT nextOrElse(final OUT output) {

            final long timeout;
            final TimeUnit timeUnit;
            final TimeoutActionType timeoutAction;
            final Throwable timeoutException;
            synchronized (mMutex) {
                timeout = mExecutionTimeout;
                timeUnit = mExecutionTimeoutUnit;
                timeoutAction = mTimeoutActionType;
                timeoutException = mTimeoutException;
            }

            try {
                return readNext(timeout, timeUnit, timeoutAction, timeoutException);

            } catch (final NoSuchElementException ignored) {

            }

            return output;
        }

        @NotNull
        public OutputChannel<OUT> skipNext(final int count) {

            if (count > 0) {
                final long timeout;
                final TimeUnit timeUnit;
                final TimeoutActionType timeoutAction;
                final Throwable timeoutException;
                synchronized (mMutex) {
                    timeout = mExecutionTimeout;
                    timeUnit = mExecutionTimeoutUnit;
                    timeoutAction = mTimeoutActionType;
                    timeoutException = mTimeoutException;
                }

                final Iterator<OUT> iterator = eventualIterator();
                try {
                    for (int i = 0; i < count; ++i) {
                        iterator.next();
                    }

                } catch (final NoSuchElementException ignored) {
                    mSubLogger.wrn("skipping output timeout: [%d %s] => [%s]", timeout, timeUnit,
                            timeoutAction);
                    if (timeoutAction == TimeoutActionType.THROW) {
                        throw new ExecutionTimeoutException(
                                "timeout while waiting for outputs [" + timeout + " " + timeUnit
                                        + "]");

                    } else if (timeoutAction == TimeoutActionType.ABORT) {
                        abort(timeoutException);
                        throw AbortException.wrapIfNeeded(timeoutException);
                    }
                }
            }

            return this;
        }

        public void throwError() {

            final RoutineException error = getError();
            if (error != null) {
                throw error;
            }
        }

        @NotNull
        public Iterator<OUT> iterator() {

            final long timeout;
            final TimeUnit timeUnit;
            final TimeoutActionType timeoutAction;
            final Throwable timeoutException;
            synchronized (mMutex) {
                verifyBound();
                timeout = mExecutionTimeout;
                timeUnit = mExecutionTimeoutUnit;
                timeoutAction = mTimeoutActionType;
                timeoutException = mTimeoutException;
            }

            return new DefaultIterator(timeout, timeUnit, timeoutAction, timeoutException);
        }

        public void remove() {

            ConstantConditions.unsupported();
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

        public boolean isEmpty() {

            return DefaultResultChannel.this.isEmpty();
        }

        public boolean isOpen() {

            return DefaultResultChannel.this.isOpen();
        }

        public int size() {

            return DefaultResultChannel.this.size();
        }
    }

    /**
     * Default implementation of an output consumer pushing the data to consume into the output
     * channel queue.
     */
    private class DefaultOutputConsumer implements OutputConsumer<OUT> {

        private final long mDelay;

        private final TimeUnit mDelayUnit;

        private final OrderType mOrderType;

        private final NestedQueue<Object> mQueue;

        private final Logger mSubLogger = mLogger.subContextLogger(this);

        /**
         * Constructor.
         */
        private DefaultOutputConsumer() {

            mDelay = mResultDelay;
            mDelayUnit = mResultDelayUnit;
            final OrderType order = (mOrderType = mResultOrder);
            mQueue = (order == OrderType.BY_CALL) ? mOutputQueue.addNested() : mOutputQueue;
        }

        public void onComplete() {

            final boolean needsFlush;
            synchronized (mMutex) {
                needsFlush = mState.onConsumerComplete(mQueue);
                mSubLogger.dbg("closing output [%s]", needsFlush);
            }

            if (needsFlush) {
                runFlush(false);
            }
        }

        public void onError(@NotNull final RoutineException error) {

            final boolean needsAbort;
            synchronized (mMutex) {
                needsAbort = mState.onConsumerError(error);
            }

            if (needsAbort) {
                mHandler.onAbort(error, mDelay, mDelayUnit);
            }
        }

        public void onOutput(final OUT output) {

            final long delay = mDelay;
            final TimeUnit timeUnit = mDelayUnit;
            final Execution execution;
            synchronized (mMutex) {
                execution = mState.onConsumerOutput(mQueue, output, delay, timeUnit, mOrderType);
            }

            if (delay == 0) {
                runFlush(false);

            } else if (execution != null) {
                runExecution(execution, delay, timeUnit);
            }

            synchronized (mMutex) {
                if (!mHasOutputs.isTrue()) {
                    waitOutputs();
                }
            }
        }
    }

    /**
     * Implementation of an execution handling a delayed abortion.
     */
    private class DelayedAbortExecution implements Execution {

        private final RoutineException mAbortException;

        /**
         * Constructor.
         *
         * @param reason the reason of the abortion.
         */
        private DelayedAbortExecution(@NotNull final RoutineException reason) {

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
    private class DelayedListOutputExecution implements Execution {

        private final ArrayList<OUT> mOutputs;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue   the output queue.
         * @param outputs the list of output data.
         */
        private DelayedListOutputExecution(@NotNull final NestedQueue<Object> queue,
                final ArrayList<OUT> outputs) {

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

        private final OUT mOutput;

        private final NestedQueue<Object> mQueue;

        /**
         * Constructor.
         *
         * @param queue  the output queue.
         * @param output the output.
         */
        private DelayedOutputExecution(@NotNull final NestedQueue<Object> queue,
                @Nullable final OUT output) {

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
     * Implementation of an execution, wrapping a delayed one, used to handle the execution queue.
     */
    private class DelayedWrappedExecution implements Execution {

        private final Execution mExecution;

        /**
         * Constructor.
         *
         * @param execution the wrapped execution.
         */
        private DelayedWrappedExecution(@NotNull final Execution execution) {

            mExecution = execution;
        }

        public void run() {

            forceExecution(mExecution);
        }
    }

    /**
     * The invocation has successfully completed.
     */
    private class DoneChannelState extends FlushChannelState {

        @Nullable
        @Override
        RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {

            if (mOutputQueue.isEmpty()) {
                mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
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

        @NotNull
        @Override
        OutputChannelState toDoneState() {

            return this;
        }
    }

    /**
     * Default implementation of an output channel iterator.
     */
    private class EventualIterator implements Iterator<OUT> {

        private final TimeoutActionType mAction;

        private final Throwable mException;

        private final Object mMutex = new Object();

        private final long mTimeout;

        private final TimeUnit mTimeoutUnit;

        private long mEndTime = Long.MIN_VALUE;

        /**
         * Constructor.
         *
         * @param timeout   the output timeout.
         * @param timeUnit  the output timeout unit.
         * @param action    the timeout action.
         * @param exception the timeout exception.
         */
        private EventualIterator(final long timeout, @NotNull final TimeUnit timeUnit,
                @NotNull final TimeoutActionType action, @Nullable final Throwable exception) {

            mTimeout = timeout;
            mTimeoutUnit = timeUnit;
            mAction = action;
            mException = exception;
        }

        private long getTimeoutMillis() {

            synchronized (mMutex) {
                if (mEndTime == Long.MIN_VALUE) {
                    mEndTime = System.currentTimeMillis() + mTimeoutUnit.toMillis(mTimeout);
                }

                return Math.max(0, mEndTime - System.currentTimeMillis());
            }
        }

        public boolean hasNext() {

            return isNextAvailable(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
        }

        public OUT next() {

            return readNext(getTimeoutMillis(), TimeUnit.MILLISECONDS, mAction, mException);
        }

        public void remove() {

            ConstantConditions.unsupported();
        }
    }

    /**
     * The invocation has been aborted with an exception.
     */
    private class ExceptionChannelState extends FlushChannelState {

        @Nullable
        @Override
        RoutineException abortConsumer(@NotNull final Throwable reason) {

            return null;
        }

        @Override
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution onConsumerOutput(@NotNull final NestedQueue<Object> queue, final OUT output,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {

            throw abortException();
        }

        @Override
        boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue) {

            throw abortException();
        }

        @NotNull
        private RoutineException abortException() {

            final RoutineException abortException = mAbortException;
            mLogger.dbg(abortException, "abort exception");
            return mAbortException;
        }

        @Override
        void orderBy(@NotNull final OrderType orderType) {

            throw abortException();
        }

        @Nullable
        @Override
        RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {

            mLogger.dbg(reason, "avoiding aborting after delay since channel is closed");
            return null;
        }

        @Override
        boolean isReadyToComplete() {

            return false;
        }

        @Nullable
        @Override
        OutputConsumer<OUT> pass(@Nullable final OutputChannel<? extends OUT> channel) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends OUT> outputs) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUT output) {

            throw abortException();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUT... outputs) {

            throw abortException();
        }

        @NotNull
        @Override
        OutputChannelState toDoneState() {

            return new AbortedChannelState();
        }
    }

    /**
     * Invocation has completed and no output is pending.
     */
    private class FlushChannelState extends ResultChannelState {

        @NotNull
        private IllegalStateException exception() {

            mLogger.err("consumer invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Nullable
        @Override
        RoutineException abortInvocation(@Nullable final Throwable reason, final long delay,
                @NotNull final TimeUnit timeUnit) {

            mLogger.dbg(reason, "avoiding aborting since channel is closed");
            return null;
        }

        @Nullable
        @Override
        Execution onConsumerOutput(@NotNull final NestedQueue<Object> queue, final OUT output,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {

            throw exception();
        }

        @Nullable
        @Override
        RoutineException abortOutputChannel(@Nullable final Throwable reason) {

            mLogger.dbg(reason, "avoiding aborting output since result channel is closed");
            return null;
        }

        @Override
        boolean delayedOutput(@NotNull final NestedQueue<Object> queue,
                @Nullable final OUT output) {

            mLogger.dbg("avoiding delayed output execution since channel is closed: %s", output);
            return false;
        }

        @Override
        boolean delayedOutputs(@NotNull final NestedQueue<Object> queue, final List<OUT> outputs) {

            mLogger.dbg("avoiding delayed output execution since channel is closed: %s", outputs);
            return false;
        }

        @Override
        boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue) {

            throw exception();
        }

        @Override
        boolean onConsumerError(@NotNull final RoutineException error) {

            mLogger.dbg(error,
                    "avoiding aborting on consumer exception since result channel is closed");
            return false;
        }

        @Override
        boolean isReadyToComplete() {

            return true;
        }
    }

    /**
     * Execution flushing the output to the bound consumer.
     */
    private class FlushExecution implements Execution {

        private final boolean mForceClose;

        /**
         * Constructor.
         *
         * @param forceClose whether to forcedly close the consumer.
         */
        private FlushExecution(final boolean forceClose) {

            mForceClose = forceClose;
        }

        public void run() {

            flushOutput(mForceClose);
        }
    }

    /**
     * Result channel internal state (using "state" design pattern).
     */
    private class OutputChannelState {

        /**
         * Called when a consumer cause the invocation to abort by throwing an exception.
         *
         * @param reason the reason of the abortion.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException abortConsumer(@NotNull final Throwable reason) {

            final RoutineException abortException = InvocationException.wrapIfNeeded(reason);
            mLogger.wrn(reason, "aborting on consumer exception (%s)", mOutputConsumer);
            internalAbort(abortException);
            return abortException;
        }

        /**
         * Called when the invocation is aborted.
         *
         * @param reason   the reason of the abortion.
         * @param delay    the abortion delay.
         * @param timeUnit the abortion delay unit.
         * @return the abort exception or null.
         */
        @Nullable
        RoutineException abortInvocation(@Nullable final Throwable reason, final long delay,
                @NotNull final TimeUnit timeUnit) {

            final RoutineException abortException = AbortException.wrapIfNeeded(reason);
            if (delay == 0) {
                mLogger.dbg(reason, "aborting channel");
                internalAbort(abortException);
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
            mLogger.dbg(reason, "aborting output");
            internalAbort(abortException);
            return abortException;
        }

        /**
         * Called when a delay is set to the result channel.
         *
         * @param delay    the delay.
         * @param timeUnit the delay unit.
         */
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

            mResultDelay = ConstantConditions.notNegative("input delay", delay);
            mResultDelayUnit = ConstantConditions.notNull("input delay unit", timeUnit);
        }

        /**
         * Called when the specified consumer is closed.
         *
         * @param consumer the consumer instance.
         */
        void closeConsumer(@NotNull final OutputConsumer<? super OUT> consumer) {

            final Logger logger = mLogger;
            try {
                logger.dbg("closing consumer (%s)", consumer);
                consumer.onComplete();

            } catch (final RoutineException e) {
                InvocationInterruptedException.throwIfInterrupt(e);
                logger.wrn(e, "ignoring consumer exception (%s)", consumer);

            } catch (final Throwable t) {
                InvocationInterruptedException.throwIfInterrupt(t);
                logger.err(t, "ignoring consumer exception (%s)", consumer);
            }
        }

        /**
         * Called when the result channel is closed.
         *
         * @return whether the internal state has changed.
         */
        boolean closeResultChannel() {

            mLogger.dbg("closing result channel [#%d]", mPendingOutputCount);
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
        RoutineException delayedAbortInvocation(@NotNull final RoutineException reason) {

            mLogger.dbg(reason, "aborting channel after delay");
            internalAbort(reason);
            return reason;
        }

        /**
         * Called when an output is passed to the invocation after a delay.
         *
         * @param queue  the output queue.
         * @param output the output.
         * @return whether the queue content has changed.
         */
        boolean delayedOutput(@NotNull final NestedQueue<Object> queue,
                @Nullable final OUT output) {

            mLogger.dbg("delayed output execution: %s", output);
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
        boolean delayedOutputs(@NotNull final NestedQueue<Object> queue, final List<OUT> outputs) {

            mLogger.dbg("delayed output execution: %s", outputs);
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
        boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue) {

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
        boolean onConsumerError(@NotNull final RoutineException error) {

            mLogger.dbg(error, "aborting output on consumer exception");
            internalAbort(error);
            return true;
        }

        /**
         * Called when the feeding consumer receives an output.
         *
         * @param queue     the output queue.
         * @param output    the output.
         * @param delay     the output delay.
         * @param timeUnit  the output delay unit.
         * @param orderType the result order type.
         * @return the execution to run or null.
         */
        @Nullable
        Execution onConsumerOutput(@NotNull final NestedQueue<Object> queue, final OUT output,
                final long delay, @NotNull final TimeUnit timeUnit,
                @NotNull final OrderType orderType) {

            mLogger.dbg("consumer output [#%d+1]: %s [%d %s]", mOutputCount, output, delay,
                    timeUnit);
            ++mOutputCount;
            if (delay == 0) {
                queue.add(output);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedOutputExecution(
                    (orderType != OrderType.BY_DELAY) ? queue.addNested() : queue, output);
        }

        /**
         * Called to set the result delivery order.
         *
         * @param orderType the result order type.
         */
        void orderBy(@NotNull final OrderType orderType) {

            mResultOrder = orderType;
        }

        /**
         * Called when an output channel is passed to the result channel.
         *
         * @param channel the channel instance.
         * @return the output consumer to bind or null.
         */
        @Nullable
        OutputConsumer<OUT> pass(@Nullable final OutputChannel<? extends OUT> channel) {

            if (channel == null) {
                mLogger.wrn("passing null channel");
                return null;
            }

            mBoundChannels.add(channel);
            ++mPendingOutputCount;
            mLogger.dbg("passing channel: %s", channel);
            return new DefaultOutputConsumer();
        }

        /**
         * Called when some outputs are passed to the result channel.
         *
         * @param outputs the outputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final Iterable<? extends OUT> outputs) {

            if (outputs == null) {
                mLogger.wrn("passing null iterable");
                return null;
            }

            final ArrayList<OUT> list = new ArrayList<OUT>();
            for (final OUT output : outputs) {
                list.add(output);
            }

            final int size = list.size();
            final long delay = mResultDelay;
            mLogger.dbg("passing iterable [#%d+%d]: %s [%d %s]", mOutputCount, size, outputs, delay,
                    mResultDelayUnit);
            mOutputCount += size;
            checkMaxSize();
            if (delay == 0) {
                mOutputQueue.addAll(list);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedListOutputExecution(
                    (mResultOrder != OrderType.BY_DELAY) ? mOutputQueue.addNested() : mOutputQueue,
                    list);
        }

        /**
         * Called when an output is passed to the result channel.
         *
         * @param output the output.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final OUT output) {

            final long delay = mResultDelay;
            mLogger.dbg("passing output [#%d+1]: %s [%d %s]", mOutputCount, output, delay,
                    mResultDelayUnit);
            ++mOutputCount;
            checkMaxSize();
            if (delay == 0) {
                mOutputQueue.add(output);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedOutputExecution(
                    (mResultOrder != OrderType.BY_DELAY) ? mOutputQueue.addNested() : mOutputQueue,
                    output);
        }

        /**
         * Called when some outputs are passed to the result channel.
         *
         * @param outputs the outputs.
         * @return the execution to run or null.
         */
        @Nullable
        Execution pass(@Nullable final OUT... outputs) {

            if (outputs == null) {
                mLogger.wrn("passing null output array");
                return null;
            }

            final int size = outputs.length;
            final long delay = mResultDelay;
            mLogger.dbg("passing array [#%d+%d]: %s [%d %s]", mOutputCount, size, outputs, delay,
                    mResultDelayUnit);
            mOutputCount += size;
            checkMaxSize();
            final ArrayList<OUT> list = new ArrayList<OUT>(size);
            Collections.addAll(list, outputs);
            if (delay == 0) {
                mOutputQueue.addAll(list);
                return null;
            }

            ++mPendingOutputCount;
            return new DelayedListOutputExecution(
                    (mResultOrder != OrderType.BY_DELAY) ? mOutputQueue.addNested() : mOutputQueue,
                    list);
        }

        /**
         * Converts this state to done.
         *
         * @return the done state.
         */
        @NotNull
        OutputChannelState toDoneState() {

            return new DoneChannelState();
        }
    }

    /**
     * Invocation has completed but some outputs are still pending.
     */
    private class ResultChannelState extends OutputChannelState {

        @NotNull
        private IllegalStateException exception() {

            mLogger.err("invalid call on closed channel");
            return new IllegalStateException("the channel is closed");
        }

        @Override
        void after(final long delay, @NotNull final TimeUnit timeUnit) {

            throw exception();
        }

        @Override
        boolean closeResultChannel() {

            mLogger.dbg("avoiding closing result channel since already closed");
            return false;
        }

        @Override
        boolean delayedOutput(@NotNull final NestedQueue<Object> queue,
                @Nullable final OUT output) {

            mLogger.dbg("delayed output execution: %s", output);
            if (--mPendingOutputCount == 0) {
                mState = new FlushChannelState();
            }

            queue.add(output);
            queue.close();
            return true;
        }

        @Override
        boolean delayedOutputs(@NotNull final NestedQueue<Object> queue, final List<OUT> outputs) {

            mLogger.dbg("delayed output execution: %s", outputs);
            if (--mPendingOutputCount == 0) {
                mState = new FlushChannelState();
            }

            queue.addAll(outputs);
            queue.close();
            return true;
        }

        @Override
        void orderBy(@NotNull final OrderType orderType) {

            throw exception();
        }

        @Override
        boolean isOpen() {

            return false;
        }

        @Override
        boolean onConsumerComplete(@NotNull final NestedQueue<Object> queue) {

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
        OutputConsumer<OUT> pass(@Nullable final OutputChannel<? extends OUT> channel) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final Iterable<? extends OUT> outputs) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUT output) {

            throw exception();
        }

        @Nullable
        @Override
        Execution pass(@Nullable final OUT... outputs) {

            throw exception();
        }
    }

    /**
     * Implementation of an execution, wrapping another one, used to handle the execution queue.
     */
    private class WrappedExecution implements Execution {

        private final Execution mExecution;

        /**
         * Constructor.
         *
         * @param execution the wrapped execution.
         */
        private WrappedExecution(@NotNull final Execution execution) {

            mExecution = execution;
        }

        public void run() {

            sSyncRunner.run(mExecution, 0, TimeUnit.MILLISECONDS);
            nextExecution();
        }
    }

    public boolean abort(@Nullable final Throwable reason) {

        final long delay;
        final TimeUnit timeUnit;
        final RoutineException abortException;
        synchronized (mMutex) {
            delay = mResultDelay;
            timeUnit = mResultDelayUnit;
            abortException = mState.abortInvocation(reason, delay, timeUnit);
        }

        if (abortException != null) {
            if (delay == 0) {
                mHandler.onAbort(abortException, 0, TimeUnit.MILLISECONDS);

            } else {
                runExecution(new DelayedAbortExecution(abortException), delay, timeUnit);
            }

            return true;
        }

        return false;
    }

    public boolean isEmpty() {

        return size() == 0;
    }

    public boolean isOpen() {

        synchronized (mMutex) {
            return mState.isOpen();
        }
    }

    public int size() {

        synchronized (mMutex) {
            return mOutputCount;
        }
    }
}
