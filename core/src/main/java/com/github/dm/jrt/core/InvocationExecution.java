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

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.TemplateExecution;
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

    private final Object mAbortMutex = new Object();

    private final InputIterator<IN> mInputIterator;

    private final InvocationManager<IN, OUT> mInvocationManager;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUT> mResultChannel;

    private AbortExecution mAbortExecution;

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
            @NotNull final InputIterator<IN> inputs,
            @NotNull final DefaultResultChannel<OUT> result, @NotNull final Logger logger) {

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

        synchronized (mAbortMutex) {
            if (mAbortExecution == null) {
                mAbortExecution = new AbortExecution();
            }

            return mAbortExecution;
        }
    }

    public boolean canBeCancelled() {

        return true;
    }

    public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {

        synchronized (mMutex) {
            mIsWaitingInvocation = false;
            final InputIterator<IN> inputIterator = mInputIterator;
            final InvocationManager<IN, OUT> manager = mInvocationManager;
            final DefaultResultChannel<OUT> resultChannel = mResultChannel;
            resultChannel.stopWaitingInvocation();
            final int count = mExecutionCount;
            mExecutionCount = 1;
            try {
                resultChannel.enterInvocation();
                for (int i = 0; i < count; ++i) {
                    try {
                        inputIterator.onConsumeStart();
                        mLogger.dbg("running execution");
                        final boolean isComplete;
                        try {
                            if (mInvocation == null) {
                                mInvocation = invocation;
                                mLogger.dbg("initializing invocation: %s", invocation);
                                invocation.onInitialize();
                                mIsInitialized = true;
                            }

                            while (inputIterator.hasInput()) {
                                invocation.onInput(inputIterator.nextInput(), resultChannel);
                            }

                        } finally {
                            isComplete = inputIterator.onConsumeComplete();
                        }

                        if (isComplete) {
                            invocation.onResult(resultChannel);
                            try {
                                mIsTerminated = true;
                                invocation.onTerminate();
                                manager.recycle(invocation);

                            } catch (final Throwable ignored) {
                                manager.discard(invocation);

                            } finally {
                                resultChannel.close();
                                inputIterator.onInvocationComplete();
                            }
                        }

                    } catch (final Throwable t) {
                        resultChannel.abortImmediately(t);
                    }
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
    private class AbortExecution extends TemplateExecution implements InvocationObserver<IN, OUT> {

        public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {

            synchronized (mMutex) {
                mIsWaitingAbortInvocation = false;
                final InputIterator<IN> inputIterator = mInputIterator;
                final InvocationManager<IN, OUT> manager = mInvocationManager;
                final DefaultResultChannel<OUT> resultChannel = mResultChannel;
                try {
                    resultChannel.enterInvocation();
                    final RoutineException exception = inputIterator.getAbortException();
                    mLogger.dbg(exception, "aborting invocation");
                    try {
                        if (!mIsTerminated) {
                            if (mInvocation == null) {
                                mInvocation = invocation;
                                mLogger.dbg("initializing invocation: %s", invocation);
                                invocation.onInitialize();
                                mIsInitialized = true;
                            }

                            if (mIsInitialized) {
                                invocation.onAbort(exception);
                                mIsTerminated = true;
                                invocation.onTerminate();
                                manager.recycle(invocation);

                            } else {
                                // Initialization failed, so just discard the invocation
                                manager.discard(invocation);
                            }
                        }

                        resultChannel.close(exception);

                    } catch (final Throwable t) {
                        manager.discard(invocation);
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
                mResultChannel.close(error);
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
            mResultChannel.close(error);
        }
    }
}
