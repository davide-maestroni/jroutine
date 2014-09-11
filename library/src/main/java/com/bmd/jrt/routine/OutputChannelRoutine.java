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
import com.bmd.jrt.process.ResultPublisher;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.util.RoutineInterruptedException;

/**
 * Created by davide on 9/11/14.
 */
class OutputChannelRoutine<INPUT, OUTPUT, TRANSFORMED> extends AbstractRoutine<INPUT, TRANSFORMED> {

    private final InputChannel<OUTPUT, TRANSFORMED> mInputChannel;

    private final Object mMutex = new Object();

    private final InputChannelResultPublisher<OUTPUT> mResultPublisher;

    private final AbstractRoutine<INPUT, OUTPUT> mRoutine;

    private OutputChannel<TRANSFORMED> mOutputChannel;

    public OutputChannelRoutine(final AbstractRoutine<INPUT, OUTPUT> wrapped,
            final InputChannel<OUTPUT, TRANSFORMED> channel) {

        super(wrapped.getRunner());

        mRoutine = wrapped;
        mInputChannel = channel;
        mResultPublisher = new InputChannelResultPublisher<OUTPUT>(channel);
    }

    @Override
    protected RecyclableUnitProcessor<INPUT, TRANSFORMED> createProcessor() {

        return new OutputChannelUnitProcessor(mRoutine.createProcessor());
    }

    @Override
    protected InputChannel<INPUT, TRANSFORMED> start(final Runner runner) {

        return new InputRoutineChannel(createProcessor(), runner);
    }

    private class InputRoutineChannel extends DefaultRoutineChannel<INPUT, TRANSFORMED> {

        public InputRoutineChannel(final RecyclableUnitProcessor<INPUT, TRANSFORMED> processor,
                final Runner runner) {

            super(processor, runner);
        }

        @Override
        public OutputChannel<TRANSFORMED> end() {

            super.end();

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
    }

    private class OutputChannelUnitProcessor
            implements RecyclableUnitProcessor<INPUT, TRANSFORMED> {

        private final RecyclableUnitProcessor<INPUT, OUTPUT> mProcessor;

        public OutputChannelUnitProcessor(final RecyclableUnitProcessor<INPUT, OUTPUT> processor) {

            mProcessor = processor;
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<TRANSFORMED> results) {

            mProcessor.onInput(input, mResultPublisher);
        }

        @Override
        public void onReset(final ResultPublisher<TRANSFORMED> results) {

            try {

                mProcessor.onReset(mResultPublisher);

            } finally {

                mInputChannel.reset();
            }
        }

        @Override
        public void onResult(final ResultPublisher<TRANSFORMED> results) {

            try {

                mProcessor.onResult(mResultPublisher);

            } finally {

                final OutputChannel<TRANSFORMED> outputChannel = mInputChannel.end();

                synchronized (mMutex) {

                    mOutputChannel = outputChannel;
                    mMutex.notifyAll();
                }
            }
        }

        @Override
        public void recycle() {

            mProcessor.recycle();
        }
    }
}