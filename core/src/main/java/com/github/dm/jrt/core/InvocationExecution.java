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

import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of an invocation execution.
 * <p>
 * Created by davide-maestroni on 09/24/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class InvocationExecution<IN, OUT> implements Execution, InvocationObserver<IN, OUT> {

    private final InputIterator<IN> mInputIterator;

    private final InvocationManager<IN, OUT> mInvocationManager;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final ResultChannel<OUT> mResultChannel;

    private volatile AbortExecution mAbortExecution;

    private int mExecutionCount = 1;

    private Invocation<IN, OUT> mInvocation;

    private boolean mIsInitialized;

    private boolean mIsTerminated;

    private boolean mIsWaitingAbortInvocation;

    private boolean mIsWaitingInvocation;

    /**
     * Constructor.
     *
     * @param manager the invocation manager.
     * @param inputs  the input iterator.
     * @param result  the result channel.
     * @param logger  the logger instance.
     */
    InvocationExecution(@NotNull final InvocationManager<IN, OUT> manager,
            @NotNull final InputIterator<IN> inputs, @NotNull final ResultChannel<OUT> result,
            @NotNull final Logger logger) {
        mInvocationManager = ConstantConditions.notNull("invocation manager", manager);
        mInputIterator = ConstantConditions.notNull("input iterator", inputs);
        mResultChannel = ConstantConditions.notNull("result channel", result);
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Returns the abort execution.
     *
     * @return the execution.
     */
    @NotNull
    public Execution abort() {
        if (mAbortExecution == null) {
            mAbortExecution = new AbortExecution();
        }

        return mAbortExecution;
    }

    public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {
        synchronized (mMutex) {
            mIsWaitingInvocation = false;
            final ResultChannel<OUT> resultChannel = mResultChannel;
            resultChannel.stopWaitingInvocation();
            final int count = mExecutionCount;
            mExecutionCount = 1;
            resultChannel.enterInvocation();
            try {
                for (int i = 0; i < count; ++i) {
                    execute(invocation);
                }

            } finally {
                resultChannel.exitInvocation();
                final AbortExecution abortExecution = mAbortExecution;
                if (mIsWaitingAbortInvocation && (abortExecution != null)) {
                    abortExecution.onCreate(invocation);
                }
            }
        }
    }

    /**
     * Forces the recycling of the invocation.
     *
     * @param reason the reason.
     */
    public void recycle(@NotNull final Throwable reason) {
        synchronized (mMutex) {
            final Invocation<IN, OUT> invocation = mInvocation;
            if ((invocation != null) && !mIsTerminated) {
                mIsTerminated = true;
                final InvocationManager<IN, OUT> invocationManager = mInvocationManager;
                if (mIsInitialized) {
                    try {
                        invocation.onAbort(InvocationException.wrapIfNeeded(reason));
                        invocation.onRecycle(true);
                        invocationManager.recycle(invocation);

                    } catch (final Throwable t) {
                        invocationManager.discard(invocation);
                    }

                } else {
                    // Initialization failed, so just discard the invocation
                    invocationManager.discard(invocation);
                }
            }
        }
    }

    public void run() {
        final Invocation<IN, OUT> invocation;
        synchronized (mMutex) {
            if (mIsWaitingInvocation) {
                ++mExecutionCount;
                return;
            }

            invocation = mInvocation;
            mIsWaitingInvocation = (invocation == null);
            if (mIsWaitingAbortInvocation) {
                return;
            }
        }

        if (invocation != null) {
            onCreate(invocation);

        } else {
            mInvocationManager.create(this);
            synchronized (mMutex) {
                if (mInvocation == null) {
                    mResultChannel.startWaitingInvocation();
                }
            }
        }
    }

    private void execute(@NotNull final Invocation<IN, OUT> invocation) {
        final Logger logger = mLogger;
        final InputIterator<IN> inputIterator = mInputIterator;
        final InvocationManager<IN, OUT> manager = mInvocationManager;
        final ResultChannel<OUT> resultChannel = mResultChannel;
        try {
            inputIterator.onConsumeStart();
            logger.dbg("running execution");
            final boolean isComplete;
            try {
                if (mInvocation == null) {
                    mInvocation = invocation;
                    logger.dbg("initializing invocation: %s", invocation);
                    invocation.onRestart();
                    mIsInitialized = true;
                }

                while (inputIterator.hasInput()) {
                    invocation.onInput(inputIterator.nextInput(), resultChannel);
                }

            } finally {
                isComplete = inputIterator.onConsumeComplete();
            }

            if (isComplete) {
                invocation.onComplete(resultChannel);
                try {
                    mIsTerminated = true;
                    invocation.onRecycle(true);
                    manager.recycle(invocation);

                } catch (final Throwable t) {
                    logger.wrn(t, "Discarding invocation since it failed to be recycled");
                    manager.discard(invocation);

                } finally {
                    resultChannel.close();
                    inputIterator.onInvocationComplete();
                }
            }

        } catch (final Throwable t) {
            if (!resultChannel.abortImmediately(t)) {
                // Needed if the result channel is explicitly closed by the invocation
                recycle(t);
            }
        }
    }

    /**
     * Interface defining an iterator of input data.
     *
     * @param <IN> the input data type.
     */
    interface InputIterator<IN> {

        /**
         * Returns the exception identifying the abortion reason.
         *
         * @return the reason of the abortion.
         */
        @NotNull
        RoutineException getAbortException();

        /**
         * Checks if an input is available.
         *
         * @return whether an input is available.
         */
        boolean hasInput();

        /**
         * Gets the next input.
         *
         * @return the input.
         * @throws java.util.NoSuchElementException if no more inputs are available.
         */
        @Nullable
        IN nextInput();

        /**
         * Notifies that the execution abortion is complete.
         */
        void onAbortComplete();

        /**
         * Checks if the input has completed, that is, all the inputs have been consumed.
         *
         * @return whether the input has completed.
         */
        boolean onConsumeComplete();

        /**
         * Notifies that the available inputs are about to be consumed.
         */
        void onConsumeStart();

        /**
         * Notifies that the invocation execution is complete.
         */
        void onInvocationComplete();
    }

    /**
     * Abort execution implementation.
     */
    private class AbortExecution implements Execution, InvocationObserver<IN, OUT> {

        public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {
            synchronized (mMutex) {
                mIsWaitingAbortInvocation = false;
                final Logger logger = mLogger;
                final InputIterator<IN> inputIterator = mInputIterator;
                final InvocationManager<IN, OUT> manager = mInvocationManager;
                final ResultChannel<OUT> resultChannel = mResultChannel;
                resultChannel.enterInvocation();
                try {
                    final RoutineException exception = inputIterator.getAbortException();
                    logger.dbg(exception, "aborting invocation");
                    try {
                        if (!mIsTerminated) {
                            if (mInvocation == null) {
                                mInvocation = invocation;
                                logger.dbg("initializing invocation: %s", invocation);
                                invocation.onRestart();
                                mIsInitialized = true;
                            }

                            if (mIsInitialized) {
                                mIsTerminated = true;
                                try {
                                    invocation.onAbort(exception);
                                    invocation.onRecycle(true);

                                } catch (final Throwable t) {
                                    manager.discard(invocation);
                                    throw t;
                                }

                                manager.recycle(invocation);

                            } else {
                                // Initialization failed, so just discard the invocation
                                mIsTerminated = true;
                                manager.discard(invocation);
                            }
                        }

                        resultChannel.close(exception);

                    } catch (final Throwable t) {
                        if (!mIsTerminated) {
                            mIsTerminated = true;
                            manager.discard(invocation);
                        }

                        resultChannel.close(t);
                    }

                } finally {
                    resultChannel.exitInvocation();
                    inputIterator.onAbortComplete();
                    if (mIsWaitingInvocation) {
                        InvocationExecution.this.onCreate(invocation);
                    }
                }
            }
        }

        public void onError(@NotNull final Throwable error) {
            synchronized (mMutex) {
                final ResultChannel<OUT> resultChannel = mResultChannel;
                resultChannel.stopWaitingInvocation();
                resultChannel.close(error);
            }
        }

        public void run() {
            final Invocation<IN, OUT> invocation;
            synchronized (mMutex) {
                if (mIsWaitingAbortInvocation) {
                    return;
                }

                invocation = mInvocation;
                mIsWaitingAbortInvocation = (invocation == null);
                if (mIsWaitingInvocation) {
                    return;
                }
            }

            if (invocation != null) {
                onCreate(invocation);

            } else {
                mInvocationManager.create(this);
                synchronized (mMutex) {
                    if (mInvocation == null) {
                        mResultChannel.startWaitingInvocation();
                    }
                }
            }
        }
    }

    public void onError(@NotNull final Throwable error) {
        synchronized (mMutex) {
            final ResultChannel<OUT> resultChannel = mResultChannel;
            resultChannel.stopWaitingInvocation();
            resultChannel.close(error);
        }
    }
}
