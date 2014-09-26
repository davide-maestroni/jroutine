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

    private final InputIterator<INPUT> mInputIterator;

    private final Object mInvocationMutex = new Object();

    private final DefaultRoutineChannel.ExecutionProvider<INPUT, OUTPUT> mInvocationProvider;

    private final DefaultResultChannel<OUTPUT> mResultChannel;

    private Execution<INPUT, OUTPUT> mRoutine;

    /**
     * Constructor.
     *
     * @param provider the execution provider.
     * @param inputs   the input iterator.
     * @param results  the result channel.
     * @throws IllegalArgumentException if one of the parameters is null.
     */
    public DefaultInvocation(final DefaultRoutineChannel.ExecutionProvider<INPUT, OUTPUT> provider,
            final InputIterator<INPUT> inputs, final DefaultResultChannel<OUTPUT> results) {

        if (provider == null) {

            throw new IllegalArgumentException("the execution provider must not be null");
        }

        if (inputs == null) {

            throw new IllegalArgumentException("the input iterator must not be null");
        }

        if (results == null) {

            throw new IllegalArgumentException("the result channel must not be null");
        }

        mInvocationProvider = provider;
        mInputIterator = inputs;
        mResultChannel = results;
    }

    @Override
    public void abort() {

        synchronized (mInvocationMutex) {

            final InputIterator<INPUT> inputIterator = mInputIterator;
            final DefaultRoutineChannel.ExecutionProvider<INPUT, OUTPUT> provider =
                    mInvocationProvider;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
            Execution<INPUT, OUTPUT> invocation = null;

            if (!inputIterator.isAborting()) {

                return;
            }

            final Throwable exception = inputIterator.getAbortException();

            try {

                invocation = initInvocation();

                invocation.onAbort(exception);
                invocation.onReturn();

                provider.recycle(invocation);

                resultChannel.close(exception);

            } catch (final Throwable t) {

                if (invocation != null) {

                    provider.discard(invocation);
                }

                resultChannel.close(t);

            } finally {

                inputIterator.onAbortComplete();
            }
        }
    }

    @Override
    public void run() {

        synchronized (mInvocationMutex) {

            final InputIterator<INPUT> inputIterator = mInputIterator;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;

            try {

                if (!inputIterator.onConsumeInput()) {

                    return;
                }

                final Execution<INPUT, OUTPUT> invocation = initInvocation();

                while (inputIterator.hasNext()) {

                    invocation.onInput(inputIterator.next(), resultChannel);
                }

                if (inputIterator.isComplete()) {

                    invocation.onResult(resultChannel);
                    invocation.onReturn();

                    mInvocationProvider.recycle(invocation);

                    resultChannel.close();
                }

            } catch (final Throwable t) {

                resultChannel.abort(t);
            }
        }
    }

    private Execution<INPUT, OUTPUT> initInvocation() {

        final Execution<INPUT, OUTPUT> invocation;

        if (mRoutine != null) {

            invocation = mRoutine;

        } else {

            invocation = (mRoutine = mInvocationProvider.create());
            invocation.onInit();
        }

        return invocation;
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