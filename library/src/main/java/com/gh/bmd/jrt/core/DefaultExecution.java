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

import com.gh.bmd.jrt.core.DefaultRoutineChannel.InvocationManager;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;

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
class DefaultExecution<INPUT, OUTPUT> implements Execution {

    private final Object mAbortMutex = new Object();

    private final InputIterator<INPUT> mInputIterator;

    private final InvocationManager<INPUT, OUTPUT> mInvocationManager;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChannel;

    private AbortExecution mAbortExecution;

    private Invocation<INPUT, OUTPUT> mInvocation;

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

    public void run() {

        synchronized (mMutex) {

            final InputIterator<INPUT> inputIterator = mInputIterator;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;

            try {

                inputIterator.onConsumeStart();
                mLogger.dbg("running execution");
                final boolean isComplete;
                final Invocation<INPUT, OUTPUT> invocation;

                try {

                    invocation = initInvocation();

                    while (inputIterator.hasInput()) {

                        invocation.onInput(inputIterator.nextInput(), resultChannel);
                    }

                } finally {

                    isComplete = inputIterator.onConsumeComplete();
                }

                if (isComplete) {

                    final InvocationManager<INPUT, OUTPUT> manager = mInvocationManager;
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
    }

    @Nonnull
    private Invocation<INPUT, OUTPUT> initInvocation() {

        final Invocation<INPUT, OUTPUT> invocation;

        if (mInvocation != null) {

            invocation = mInvocation;

        } else {

            invocation = (mInvocation = mInvocationManager.create());
            mLogger.dbg("initializing invocation: %s", invocation);
            invocation.onInitialize();
        }

        return invocation;
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
        Throwable getAbortException();

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
         * @throws java.util.NoSuchElementException if no more input is available.
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
    private class AbortExecution implements Execution {

        public void run() {

            synchronized (mMutex) {

                final InputIterator<INPUT> inputIterator = mInputIterator;
                final InvocationManager<INPUT, OUTPUT> manager = mInvocationManager;
                final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
                Invocation<INPUT, OUTPUT> invocation = null;

                if (!inputIterator.isAborting()) {

                    mLogger.wrn("avoiding aborting since input is already aborted");
                    return;
                }

                final Throwable exception = inputIterator.getAbortException();
                mLogger.dbg(exception, "aborting invocation");

                try {

                    invocation = initInvocation();
                    invocation.onAbort(exception);
                    invocation.onTerminate();
                    manager.recycle(invocation);
                    resultChannel.close(exception);

                } catch (final Throwable t) {

                    if (invocation != null) {

                        manager.discard(invocation);
                    }

                    resultChannel.close(t);

                } finally {

                    inputIterator.onAbortComplete();
                }
            }
        }
    }
}
