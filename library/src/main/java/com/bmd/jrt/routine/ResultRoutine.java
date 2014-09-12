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

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.procedure.LoopProcedure;
import com.bmd.jrt.procedure.ResultPublisher;
import com.bmd.jrt.time.PositiveDuration;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/11/14.
 */
class ResultRoutine<INPUT, OUTPUT, TRANSFORMED> extends AbstractRoutine<INPUT, TRANSFORMED> {

    private final Object mMutex = new Object();

    private final Routine<OUTPUT, TRANSFORMED> mResultRoutine;

    private final AbstractRoutine<INPUT, OUTPUT> mRoutine;

    private OutputChannel<TRANSFORMED> mOutputChannel;

    public ResultRoutine(final AbstractRoutine<INPUT, OUTPUT> wrapped,
            final Routine<OUTPUT, TRANSFORMED> routine) {

        super(wrapped);

        mRoutine = wrapped;
        mResultRoutine = routine;
    }

    @Override
    protected LoopProcedure<INPUT, TRANSFORMED> createProcedure(final boolean async) {

        final Routine<OUTPUT, TRANSFORMED> resultRoutine = mResultRoutine;

        return new OutputChannelLoopProcedure(mRoutine.createProcedure(async),
                                              (async) ? resultRoutine.asynStart()
                                                      : resultRoutine.start());
    }

    @Override
    protected InputChannel<INPUT, TRANSFORMED> start(final boolean async) {

        return new InputRoutineChannel(super.start(async));
    }

    private class InputRoutineChannel implements InputChannel<INPUT, TRANSFORMED> {

        private final InputChannel<INPUT, TRANSFORMED> mInputChannel;

        public InputRoutineChannel(final InputChannel<INPUT, TRANSFORMED> channel) {

            mInputChannel = channel;
        }

        @Override
        public InputChannel<INPUT, TRANSFORMED> after(final PositiveDuration delay) {

            mInputChannel.after(delay);

            return this;
        }

        @Override
        public InputChannel<INPUT, TRANSFORMED> after(final long delay, final TimeUnit timeUnit) {

            mInputChannel.after(delay, timeUnit);

            return this;
        }

        @Override
        public OutputChannel<TRANSFORMED> end() {

            mInputChannel.end();

            try {

                synchronized (mMutex) {

                    while (mOutputChannel == null) {

                        mMutex.wait();
                    }

                    return mOutputChannel;
                }

            } catch (final InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new RoutineInterruptedException(e);
            }
        }

        @Override
        public InputChannel<INPUT, TRANSFORMED> push(final INPUT... inputs) {

            mInputChannel.push(inputs);

            return this;
        }

        @Override
        public InputChannel<INPUT, TRANSFORMED> push(final Iterable<? extends INPUT> inputs) {

            mInputChannel.push(inputs);

            return this;
        }

        @Override
        public InputChannel<INPUT, TRANSFORMED> push(final INPUT input) {

            mInputChannel.push(input);

            return this;
        }

        @Override
        public boolean isOpen() {

            return mInputChannel.isOpen();
        }

        @Override
        public boolean reset() {

            return mInputChannel.reset();
        }

        @Override
        public boolean reset(final RuntimeException exception) {

            return mInputChannel.reset(exception);
        }
    }

    private class OutputChannelLoopProcedure implements LoopProcedure<INPUT, TRANSFORMED> {

        private final InputChannel<OUTPUT, TRANSFORMED> mInputChannel;

        private final LoopProcedure<INPUT, OUTPUT> mProcedure;

        private final InputChannelResultPublisher<OUTPUT> mResultPublisher;

        public OutputChannelLoopProcedure(final LoopProcedure<INPUT, OUTPUT> procedure,
                final InputChannel<OUTPUT, TRANSFORMED> channel) {

            mProcedure = procedure;
            mInputChannel = channel;
            mResultPublisher = new InputChannelResultPublisher<OUTPUT>(channel);
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<TRANSFORMED> results) {

            mProcedure.onInput(input, mResultPublisher);
        }

        @Override
        public void onReset(final ResultPublisher<TRANSFORMED> results) {

            try {

                mProcedure.onReset(mResultPublisher);

            } finally {

                mInputChannel.reset();
            }
        }

        @Override
        public void onResult(final ResultPublisher<TRANSFORMED> results) {

            try {

                mProcedure.onResult(mResultPublisher);

            } finally {

                final OutputChannel<TRANSFORMED> outputChannel = mInputChannel.end();

                synchronized (mMutex) {

                    mOutputChannel = outputChannel;
                    mMutex.notifyAll();
                }
            }
        }
    }
}