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

import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultParameterChannel.ExecutionProvider;
import com.bmd.jrt.runner.Invocation;

import java.util.Iterator;

/**
 * Default implementation of an invocation object.
 * <p/>
 * Created by davide on 9/24/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
class DefaultInvocation<INPUT, OUTPUT> implements Invocation {

    private final ExecutionProvider<INPUT, OUTPUT> mExecutionProvider;

    private final InputIterator<INPUT> mInputIterator;

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final DefaultResultChannel<OUTPUT> mResultChannel;

    private Execution<INPUT, OUTPUT> mExecution;

    /**
     * Constructor.
     *
     * @param provider the execution provider.
     * @param inputs   the input iterator.
     * @param results  the result channel.
     * @param logger   the logger instance.
     * @throws NullPointerException if one of the parameters is null.
     */
    DefaultInvocation(final ExecutionProvider<INPUT, OUTPUT> provider,
            final InputIterator<INPUT> inputs, final DefaultResultChannel<OUTPUT> results,
            final Logger logger) {

        if (provider == null) {

            throw new NullPointerException("the execution provider must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the input iterator must not be null");
        }

        if (results == null) {

            throw new NullPointerException("the result channel must not be null");
        }

        if (logger == null) {

            throw new NullPointerException("the logger instance must not be null");
        }

        mExecutionProvider = provider;
        mInputIterator = inputs;
        mResultChannel = results;
        mLogger = logger;
    }

    @Override
    public void abort() {

        synchronized (mMutex) {

            final InputIterator<INPUT> inputIterator = mInputIterator;
            final ExecutionProvider<INPUT, OUTPUT> provider = mExecutionProvider;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
            Execution<INPUT, OUTPUT> execution = null;

            if (!inputIterator.isAborting()) {

                mLogger.wrn("%s - avoiding aborting since input is already aborted", this);

                return;
            }

            final Throwable exception = inputIterator.getAbortException();

            mLogger.dbg(exception, "%s - aborting invocation", this);

            try {

                execution = initExecution();

                execution.onAbort(exception);
                execution.onReturn();

                provider.recycle(execution);

                resultChannel.close(exception);

            } catch (final Throwable t) {

                if (execution != null) {

                    provider.discard(execution);
                }

                resultChannel.close(t);

            } finally {

                inputIterator.onAbortComplete();
            }
        }
    }

    @Override
    public void run() {

        synchronized (mMutex) {

            final InputIterator<INPUT> inputIterator = mInputIterator;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;

            try {

                if (!inputIterator.onConsumeInput()) {

                    mLogger.wrn("%s - avoiding running invocation", this);

                    return;
                }

                mLogger.dbg("%s - running invocation", this);

                final Execution<INPUT, OUTPUT> execution = initExecution();

                while (inputIterator.hasNext()) {

                    execution.onInput(inputIterator.next(), resultChannel);
                }

                if (inputIterator.isComplete()) {

                    execution.onResult(resultChannel);
                    execution.onReturn();

                    mExecutionProvider.recycle(execution);

                    resultChannel.close();
                }

            } catch (final Throwable t) {

                resultChannel.abort(t);
            }
        }
    }

    private Execution<INPUT, OUTPUT> initExecution() {

        final Execution<INPUT, OUTPUT> execution;

        if (mExecution != null) {

            execution = mExecution;

        } else {

            execution = (mExecution = mExecutionProvider.create());

            mLogger.dbg("%s - initializing execution: %s", this, execution);

            execution.onInit();
        }

        return execution;
    }

    /**
     * Interface defining an iterator of input data.
     *
     * @param <INPUT>the input type.
     */
    public interface InputIterator<INPUT> extends Iterator<INPUT> {

        /**
         * Returns the exception identifying the abortion reason.
         *
         * @return the reason of the abortion.
         */
        public Throwable getAbortException();

        /**
         * Checks if the input channel is aborting the execution.
         *
         * @return whether the execution is aborting.
         */
        public boolean isAborting();

        /**
         * Checks if the input has completed, that is, all the inputs have been consumed.
         *
         * @return whether the input has completed.
         */
        public boolean isComplete();

        /**
         * Notifies that the execution abortion is complete.
         */
        public void onAbortComplete();

        /**
         * Notifies that the available inputs are about to be consumed.
         *
         * @return whether at least one input is available.
         */
        public boolean onConsumeInput();
    }
}