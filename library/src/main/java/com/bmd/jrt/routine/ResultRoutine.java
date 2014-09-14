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
import com.bmd.jrt.subroutine.ResultPublisher;
import com.bmd.jrt.subroutine.SubRoutineLoop;
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
    protected SubRoutineLoop<INPUT, TRANSFORMED> createSubRoutine(final boolean async) {

        final Routine<OUTPUT, TRANSFORMED> resultRoutine = mResultRoutine;

        return new OutputChannelSubRoutineLoop(mRoutine.createSubRoutine(async),
                                               (async) ? resultRoutine.asynStart()
                                                       : resultRoutine.start());
    }

    @Override
    protected InputChannel<INPUT, TRANSFORMED> start(final boolean async) {

        return new InputChannelWrapper(super.start(async));
    }

    private class InputChannelWrapper implements InputChannel<INPUT, TRANSFORMED> {

        private final InputChannel<INPUT, TRANSFORMED> mInputChannel;

        public InputChannelWrapper(final InputChannel<INPUT, TRANSFORMED> channel) {

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

    //TODO: cannot be reused...
    private class OutputChannelSubRoutineLoop implements SubRoutineLoop<INPUT, TRANSFORMED> {

        private final InputChannel<OUTPUT, TRANSFORMED> mInputChannel;

        private final InputChannelResultPublisher<OUTPUT> mResultPublisher;

        private final SubRoutineLoop<INPUT, OUTPUT> mRoutine;

        public OutputChannelSubRoutineLoop(final SubRoutineLoop<INPUT, OUTPUT> routine,
                final InputChannel<OUTPUT, TRANSFORMED> channel) {

            mRoutine = routine;
            mInputChannel = channel;
            mResultPublisher = new InputChannelResultPublisher<OUTPUT>(channel);
        }

        @Override
        public void onInit() {

            mRoutine.onInit();
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<TRANSFORMED> results) {

            mRoutine.onInput(input, mResultPublisher);
        }

        @Override
        public void onReset(final ResultPublisher<TRANSFORMED> results) {

            try {

                mRoutine.onReset(mResultPublisher);

            } finally {

                mInputChannel.reset();
            }
        }

        @Override
        public void onResult(final ResultPublisher<TRANSFORMED> results) {

            try {

                mRoutine.onResult(mResultPublisher);

            } finally {

                final OutputChannel<TRANSFORMED> outputChannel = mInputChannel.end();

                synchronized (mMutex) {

                    mOutputChannel = outputChannel;
                    mMutex.notifyAll();
                }
            }
        }

        @Override
        public void onReturn() {

            mRoutine.onReturn();
        }
    }
}