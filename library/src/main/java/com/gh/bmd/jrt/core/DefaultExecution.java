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

import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationManager;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationObserver;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.TemplateExecution;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an execution object.
 * <p/>
 * Created by davide-maestroni on 9/24/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultExecution<INPUT, OUTPUT> implements Execution, InvocationObserver<INPUT, OUTPUT> {

    private final Object mAbortMutex = new Object();

    private final InputIterator<INPUT> mInputIterator;

    private final InvocationManager<INPUT, OUTPUT> mInvocationManager;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChannel;

    private AbortExecution mAbortExecution;

    private int mExecutionCount = 1;

    private Invocation<INPUT, OUTPUT> mInvocation;

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
    @SuppressWarnings("ConstantConditions")
    DefaultExecution(@Nonnull final InvocationManager<INPUT, OUTPUT> manager,
            @Nonnull final InputIterator<INPUT> inputs,
            @Nonnull final DefaultResultChannel<OUTPUT> result, @Nonnull final Logger logger) {

        if (manager == null) {

            throw new NullPointerException("the invocation manager must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the input iterator must not be null");
        }

        if (result == null) {

            throw new NullPointerException("the result channel must not be null");
        }

        mInvocationManager = manager;
        mInputIterator = inputs;
        mResultChannel = result;
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Returns the abort execution.
     *
     * @return the execution.
     */
    @Nonnull
    public Execution abort() {

        synchronized (mAbortMutex) {

            if (mAbortExecution == null) {

                mAbortExecution = new AbortExecution();
            }

            return mAbortExecution;
        }
    }

    public boolean mayBeCanceled() {

        return true;
    }

    public void onCreate(@Nonnull final Invocation<INPUT, OUTPUT> invocation) {

        synchronized (mMutex) {

            mIsWaitingInvocation = false;
            final InputIterator<INPUT> inputIterator = mInputIterator;
            final InvocationManager<INPUT, OUTPUT> manager = mInvocationManager;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
            resultChannel.stopWaitingInvocation();
            final int count = mExecutionCount;
            mExecutionCount = 1;

            try {

                for (int i = 0; i < count; i++) {

                    try {

                        inputIterator.onConsumeStart();
                        mLogger.dbg("running execution");
                        final boolean isComplete;

                        try {

                            if (mInvocation == null) {

                                mInvocation = invocation;
                                mLogger.dbg("initializing invocation: %s", invocation);
                                invocation.onInitialize();
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

                final AbortExecution abortExecution = mAbortExecution;

                if (mIsWaitingAbortInvocation && (abortExecution != null)) {

                    abortExecution.onCreate(invocation);
                }
            }
        }
    }

    public void run() {

        final Invocation<INPUT, OUTPUT> invocation;

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
     * @param <INPUT> the input data type.
     */
    interface InputIterator<INPUT> {

        /**
         * Returns the exception identifying the abortion reason.
         *
         * @return the reason of the abortion.
         */
        @Nullable
        RoutineException getAbortException();

        /**
         * Checks if an input is available.
         *
         * @return whether an input is available.
         */
        boolean hasInput();

        /**
         * Checks if the input channel is aborting the execution.
         *
         * @return whether the execution is aborting.
         */
        boolean isAborting();

        /**
         * Gets the next input.
         *
         * @return the input.
         * @throws java.util.NoSuchElementException if no more inputs are available.
         */
        @Nullable
        INPUT nextInput();

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
    private class AbortExecution extends TemplateExecution
            implements InvocationObserver<INPUT, OUTPUT> {

        private int mAbortExecutionCount = 1;

        public void onCreate(@Nonnull final Invocation<INPUT, OUTPUT> invocation) {

            synchronized (mMutex) {

                mIsWaitingAbortInvocation = false;
                final InputIterator<INPUT> inputIterator = mInputIterator;
                final InvocationManager<INPUT, OUTPUT> manager = mInvocationManager;
                final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
                final int count = mAbortExecutionCount;
                mAbortExecutionCount = 1;

                try {

                    for (int i = 0; i < count; i++) {

                        if (!inputIterator.isAborting()) {

                            mLogger.wrn("avoiding aborting since input is already aborted");
                            return;
                        }

                        final RoutineException exception = inputIterator.getAbortException();
                        mLogger.dbg(exception, "aborting invocation");

                        try {

                            if (mInvocation == null) {

                                mInvocation = invocation;
                                mLogger.dbg("initializing invocation: %s", invocation);
                                invocation.onInitialize();
                            }

                            invocation.onAbort(exception);
                            invocation.onTerminate();
                            manager.recycle(invocation);
                            resultChannel.close(exception);

                        } catch (final Throwable t) {

                            manager.discard(invocation);
                            resultChannel.close(t);
                        }
                    }

                } finally {

                    inputIterator.onAbortComplete();

                    if (mIsWaitingInvocation) {

                        DefaultExecution.this.onCreate(invocation);
                    }
                }
            }
        }

        public void onError(@Nonnull final Throwable error) {

            synchronized (mMutex) {

                mResultChannel.close(error);
            }
        }

        public void run() {

            final Invocation<INPUT, OUTPUT> invocation;

            synchronized (mMutex) {

                if (mIsWaitingAbortInvocation) {

                    ++mAbortExecutionCount;
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

    public void onError(@Nonnull final Throwable error) {

        synchronized (mMutex) {

            mResultChannel.close(error);
        }
    }
}
