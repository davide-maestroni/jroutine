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

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Created by davide on 9/24/14.
 */
class ExecutionHandler<INPUT, OUTPUT> {

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final DefaultInvocation<INPUT, OUTPUT> mInvocationInstruction;

    private final Object mMutex = new Object();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final Runner mRunner;

    private Throwable mAbortException;

    private ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private TimeDuration mInputDelay = TimeDuration.ZERO;

    private OutputConsumer<OUTPUT> mOutputConsumer;

    private Check mOutputHasNext;

    private Check mOutputNotEmpty;

    private TimeDuration mOutputTimeout = seconds(3);

    private RuntimeException mOutputTimeoutException;

    private int mPendingInputCount;

    private int mPendingOutputCount;

    private TimeDuration mResultDelay = TimeDuration.ZERO;

    private ChannelState mState = ChannelState.INPUT;

    public ExecutionHandler(final RoutineInvocationProvider<INPUT, OUTPUT> provider,
            final Runner runner) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mInvocationInstruction = new DefaultInvocation<INPUT, OUTPUT>(this, provider);
    }

    public Throwable getAbortException() {

        synchronized (mMutex) {

            return mAbortException;
        }
    }

    public boolean hasInput() {

        synchronized (mMutex) {

            return !mInputQueue.isEmpty();
        }
    }

    public boolean hasOutput(final TimeDuration timeout, final RuntimeException timeoutException) {

        synchronized (mMutex) {

            verifyBound();

            final LinkedList<Object> outputQueue = mOutputQueue;

            if (timeout.isZero() || isDone()) {

                return !outputQueue.isEmpty();
            }

            if (mOutputHasNext == null) {

                mOutputHasNext = new Check() {

                    @Override
                    public boolean isTrue() {

                        return !mOutputQueue.isEmpty() || isDone();
                    }
                };
            }

            boolean isTimeout = false;

            try {

                isTimeout = !timeout.waitTrue(mMutex, mOutputHasNext);

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            if (isTimeout) {

                if (timeoutException != null) {

                    throw timeoutException;
                }
            }

            return !outputQueue.isEmpty();
        }
    }

    public boolean inputAbort(final Throwable throwable) {

        synchronized (mMutex) {

            if (!isInputOpen()) {

                return false;
            }

            mInputQueue.clear();

            mAbortException = throwable;
            mState = ChannelState.EXCEPTION;
        }

        mRunner.runAbort(mInvocationInstruction);

        return false;
    }

    public void inputAfter(final TimeDuration delay) {

        synchronized (mMutex) {

            verifyInput();

            if (delay == null) {

                throw new IllegalArgumentException();
            }

            mInputDelay = delay;
        }
    }

    public OutputChannel<OUTPUT> inputClose() {

        synchronized (mMutex) {

            verifyInput();

            mState = ChannelState.OUTPUT;

            ++mPendingInputCount;
        }

        mRunner.run(mInvocationInstruction, 0, TimeUnit.MILLISECONDS);

        return new DefaultOutputChannel<OUTPUT>(this);
    }

    public void inputPass(final INPUT input) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            delay = mInputDelay;

            if (delay.isZero()) {

                mInputQueue.add(input);
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.run(mInvocationInstruction, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedInputInvocation(input), delay.time, delay.unit);
        }
    }

    public void inputPass(final INPUT... inputs) {

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                return;
            }
        }

        inputPass(Arrays.asList(inputs));
    }

    public void inputPass(final OutputChannel<INPUT> channel) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (channel == null) {

                return;
            }

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingInputCount;
        }

        channel.bind(new InputOutputConsumer(delay));
    }

    public void inputPass(final Iterable<? extends INPUT> inputs) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                return;
            }

            delay = mInputDelay;

            if (delay.isZero()) {

                final LinkedList<INPUT> inputQueue = mInputQueue;

                for (final INPUT input : inputs) {

                    inputQueue.add(input);
                }
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.run(mInvocationInstruction, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.run(new DelayedListInputInvocation(inputs), delay.time, delay.unit);
        }
    }

    public boolean isAborting() {

        synchronized (mMutex) {

            return (mState == ChannelState.EXCEPTION);
        }
    }

    public boolean isInputOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    public boolean isOutputOpen() {

        synchronized (mMutex) {

            return !mOutputQueue.isEmpty() || ((mState != ChannelState.DONE) && (mState
                    != ChannelState.ABORT));
        }
    }

    public boolean isResult() {

        synchronized (mMutex) {

            return (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
        }
    }

    public boolean isResultOpen() {

        synchronized (mMutex) {

            return !isDone() && (mState != ChannelState.EXCEPTION);
        }
    }

    public INPUT nextInput() {

        synchronized (mMutex) {

            return mInputQueue.removeFirst();
        }
    }

    public OUTPUT nextOutput(final TimeDuration timeout, final RuntimeException timeoutException) {

        synchronized (mMutex) {

            verifyBound();

            final LinkedList<Object> outputQueue = mOutputQueue;

            if (timeout.isZero() || !outputQueue.isEmpty()) {

                if (outputQueue.isEmpty()) {

                    throw new NoSuchElementException();
                }

                final Object result = outputQueue.removeFirst();

                RoutineExceptionWrapper.raise(result);

                //noinspection unchecked
                return (OUTPUT) result;
            }

            if (mOutputNotEmpty == null) {

                mOutputNotEmpty = new Check() {

                    @Override
                    public boolean isTrue() {

                        return !mOutputQueue.isEmpty();
                    }
                };
            }

            boolean isTimeout = false;

            try {

                isTimeout = !timeout.waitTrue(mMutex, mOutputNotEmpty);

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            if (isTimeout) {

                if (timeoutException != null) {

                    throw timeoutException;
                }
            }

            final Object result = outputQueue.removeFirst();

            RoutineExceptionWrapper.raise(result);

            //noinspection unchecked
            return (OUTPUT) result;
        }
    }

    public void onAbortComplete() {

        final Throwable exception;
        final ArrayList<OutputChannel<?>> channels;

        synchronized (mMutex) {

            exception = mAbortException;

            channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
            mBoundChannels.clear();

            mState = ChannelState.ABORT;
            mMutex.notifyAll();
        }

        for (final OutputChannel<?> channel : channels) {

            channel.abort(exception);
        }
    }

    public boolean onProcessInput() {

        synchronized (mMutex) {

            if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                return false;
            }

            --mPendingInputCount;
        }

        return true;
    }

    public boolean outputAbort(final Throwable throwable) {

        boolean isFlush = false;

        synchronized (mMutex) {

            if (isDone()) {

                return false;
            }

            if (mState == ChannelState.EXCEPTION) {

                mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                isFlush = true;

            } else {

                mOutputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }
        }

        if (isFlush) {

            flushOutput();

        } else {

            mRunner.runAbort(mInvocationInstruction);
        }

        return true;
    }

    public void outputAfterMax(final TimeDuration timeout) {

        synchronized (mMutex) {

            verifyBound();

            if (timeout == null) {

                throw new IllegalArgumentException();
            }

            mOutputTimeout = timeout;
        }
    }

    public void outputBind(final OutputConsumer<OUTPUT> consumer) {

        synchronized (mMutex) {

            verifyBound();

            if (consumer == null) {

                throw new IllegalArgumentException();
            }

            mOutputConsumer = new SynchronizedConsumer<OUTPUT>(consumer);
        }

        flushOutput();
    }

    public Iterator<OUTPUT> outputIterator() {

        final TimeDuration timeout;
        final RuntimeException exception;

        synchronized (mMutex) {

            verifyBound();

            timeout = mOutputTimeout;
            exception = mOutputTimeoutException;
        }

        return new DefaultIterator<OUTPUT>(this, timeout, exception);
    }

    public void outputReadInto(final List<OUTPUT> results) {

        synchronized (mMutex) {

            verifyBound();

            final LinkedList<Object> outputQueue = mOutputQueue;
            final TimeDuration timeout = mOutputTimeout;
            final RuntimeException timeoutException = mOutputTimeoutException;

            if (timeout.isZero() || isDone()) {

                final Iterator<Object> iterator = outputQueue.iterator();

                while (iterator.hasNext()) {

                    final Object result = iterator.next();
                    iterator.remove();

                    RoutineExceptionWrapper.raise(result);

                    //noinspection unchecked
                    results.add((OUTPUT) result);
                }

                return;
            }

            boolean isTimeout = false;

            try {

                isTimeout = !timeout.waitTrue(mMutex, new Check() {

                    @Override
                    public boolean isTrue() {

                        return isDone();
                    }
                });

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            if (isTimeout) {

                if (timeoutException != null) {

                    throw timeoutException;
                }

            } else {

                final Iterator<Object> iterator = outputQueue.iterator();

                while (iterator.hasNext()) {

                    final Object result = iterator.next();
                    iterator.remove();

                    RoutineExceptionWrapper.raise(result);

                    //noinspection unchecked
                    results.add((OUTPUT) result);
                }
            }
        }
    }

    public void outputTimeoutException(final RuntimeException exception) {

        synchronized (mMutex) {

            verifyBound();

            mOutputTimeoutException = exception;
        }
    }

    public boolean outputWaitDone() {

        boolean isDone = false;

        synchronized (mMutex) {

            final TimeDuration timeout = mOutputTimeout;
            final RuntimeException timeoutException = mOutputTimeoutException;

            try {

                isDone = timeout.waitTrue(mMutex, new Check() {

                    @Override
                    public boolean isTrue() {

                        return isDone();
                    }
                });

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            if (!isDone && (timeoutException != null)) {

                throw timeoutException;
            }
        }

        return isDone;
    }

    public boolean resultAbort(final Throwable throwable) {

        boolean isFlush = false;

        synchronized (mMutex) {

            if (isDone()) {

                return false;
            }

            if (mState == ChannelState.EXCEPTION) {

                mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                isFlush = true;

            } else {

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }
        }

        if (isFlush) {

            flushOutput();

            return true;
        }

        mRunner.runAbort(mInvocationInstruction);

        return false;
    }

    public void resultAfter(final TimeDuration delay) {

        synchronized (mMutex) {

            verifyOutput();

            if (delay == null) {

                throw new IllegalArgumentException();
            }

            mResultDelay = delay;
        }
    }

    public void resultClose() {

        synchronized (mMutex) {

            if (mState == ChannelState.OUTPUT) {

                if (mPendingOutputCount > 0) {

                    mState = ChannelState.RESULT;

                } else {

                    mState = ChannelState.DONE;
                }
            }
        }

        flushOutput();
    }

    public void resultPass(final OutputChannel<OUTPUT> channel) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (channel == null) {

                return;
            }

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingOutputCount;
        }

        channel.bind(new OutputOutputConsumer(delay));
    }

    public void resultPass(final Iterable<? extends OUTPUT> outputs) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                return;
            }

            delay = mResultDelay;

            if (delay.isZero()) {

                final LinkedList<Object> outputQueue = mOutputQueue;

                for (final OUTPUT output : outputs) {

                    outputQueue.add(output);
                }

            } else {

                ++mPendingOutputCount;
            }
        }

        if (delay.isZero()) {

            flushOutput();

        } else {

            mRunner.run(new DelayedListOutputInvocation(outputs), delay.time, delay.unit);
        }
    }

    public void resultPass(final OUTPUT output) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyOutput();

            delay = mResultDelay;

            if (delay.isZero()) {

                mOutputQueue.add(output);

            } else {

                ++mPendingOutputCount;
            }
        }

        if (delay.isZero()) {

            flushOutput();

        } else {

            mRunner.run(new DelayedOutputInvocation(output), delay.time, delay.unit);
        }
    }

    public void resultPass(final OUTPUT... outputs) {

        synchronized (mMutex) {

            verifyOutput();

            if (outputs == null) {

                return;
            }
        }

        resultPass(Arrays.asList(outputs));
    }

    public void validateOutput() {

        synchronized (mMutex) {

            verifyBound();
        }
    }

    private void flushOutput() {

        try {

            final ArrayList<Object> outputs;
            final OutputConsumer<OUTPUT> consumer;
            final ChannelState state;

            synchronized (mMutex) {

                consumer = mOutputConsumer;

                if (consumer == null) {

                    mMutex.notifyAll();

                    return;
                }

                outputs = new ArrayList<Object>(mOutputQueue);
                mOutputQueue.clear();
                state = mState;

                mMutex.notifyAll();
            }

            for (final Object output : outputs) {

                if (output instanceof RoutineExceptionWrapper) {

                    try {

                        consumer.onAbort(((RoutineExceptionWrapper) output).getCause());

                    } catch (final RoutineInterruptedException e) {

                        throw e;

                    } catch (final Throwable ignored) {

                    }

                    break;

                } else {

                    //noinspection unchecked
                    consumer.onOutput((OUTPUT) output);
                }
            }

            if (state == ChannelState.DONE) {

                try {

                    consumer.onClose();

                } catch (final RoutineInterruptedException e) {

                    throw e;

                } catch (final Throwable ignored) {

                }
            }

        } catch (final Throwable t) {

            boolean isFlush = false;
            boolean isAbort = false;

            synchronized (mMutex) {

                if (isDone()) {

                    isFlush = true;

                } else if (mState != ChannelState.EXCEPTION) {

                    isAbort = true;

                    mOutputQueue.clear();

                    mAbortException = t;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else if (isAbort) {

                mRunner.runAbort(mInvocationInstruction);
            }
        }
    }

    private boolean isDone() {

        return (mState == ChannelState.DONE) || (mState == ChannelState.ABORT);
    }

    private boolean isError() {

        return (mState == ChannelState.ABORT) || (mState == ChannelState.EXCEPTION);
    }

    private void verifyBound() {

        if (mOutputConsumer != null) {

            throw new IllegalStateException();
        }
    }

    private void verifyInput() {

        final Throwable throwable = mAbortException;

        if (throwable != null) {

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isInputOpen()) {

            throw new IllegalStateException();
        }
    }

    private void verifyOutput() {

        final Throwable throwable = mAbortException;

        if (throwable != null) {

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isResultOpen() || (mState == ChannelState.EXCEPTION)) {

            throw new IllegalStateException();
        }
    }

    private static enum ChannelState {

        INPUT,
        OUTPUT,
        RESULT,
        DONE,
        EXCEPTION,
        ABORT
    }

    private class DelayedInputInvocation implements Invocation {

        private final INPUT mInput;

        public DelayedInputInvocation(final INPUT input) {

            mInput = input;
        }

        @Override
        public void abort() {

            mInvocationInstruction.abort();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.add(mInput);
            }

            mInvocationInstruction.run();
        }
    }

    private class DelayedListInputInvocation implements Invocation {

        private final ArrayList<INPUT> mInputs;

        public DelayedListInputInvocation(final Iterable<? extends INPUT> inputs) {

            final ArrayList<INPUT> inputList = new ArrayList<INPUT>();

            for (final INPUT input : inputs) {

                inputList.add(input);
            }

            mInputs = inputList;
        }

        @Override
        public void abort() {

            mInvocationInstruction.abort();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.addAll(mInputs);
            }

            mInvocationInstruction.run();
        }
    }

    private class DelayedListOutputInvocation implements Invocation {

        private final ArrayList<OUTPUT> mOutputs;

        public DelayedListOutputInvocation(final Iterable<? extends OUTPUT> outputs) {

            final ArrayList<OUTPUT> outputList = new ArrayList<OUTPUT>();

            for (final OUTPUT output : outputs) {

                outputList.add(output);
            }

            mOutputs = outputList;
        }

        @Override
        public void abort() {

            mInvocationInstruction.abort();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isError()) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mOutputQueue.addAll(mOutputs);
            }

            flushOutput();
        }
    }

    private class DelayedOutputInvocation implements Invocation {

        private final OUTPUT mOutput;

        public DelayedOutputInvocation(final OUTPUT output) {

            mOutput = output;
        }

        @Override
        public void abort() {

            mInvocationInstruction.abort();
        }

        @Override
        public void run() {

            synchronized (mMutex) {

                if (isError()) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mOutputQueue.add(mOutput);
            }

            flushOutput();
        }
    }

    private class InputOutputConsumer implements OutputConsumer<INPUT> {

        private final TimeDuration mDelay;

        public InputOutputConsumer(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onAbort(final Throwable throwable) {

            synchronized (mMutex) {

                if (!isInputOpen() && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mRunner.runAbort(mInvocationInstruction);
        }

        @Override
        public void onClose() {

            mRunner.run(mInvocationInstruction, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onOutput(final INPUT output) {

            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                final Throwable throwable = mAbortException;

                if (throwable != null) {

                    throw RoutineExceptionWrapper.wrap(throwable).raise();
                }

                if (!isInputOpen() && (mState != ChannelState.OUTPUT)) {

                    throw new IllegalStateException();
                }

                if (delay.isZero()) {

                    mInputQueue.add(output);
                }

                ++mPendingInputCount;
            }

            if (delay.isZero()) {

                mRunner.run(mInvocationInstruction, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.run(new DelayedInputInvocation(output), delay.time, delay.unit);
            }
        }
    }

    private class OutputOutputConsumer implements OutputConsumer<OUTPUT> {

        private final TimeDuration mDelay;

        public OutputOutputConsumer(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onAbort(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mMutex) {

                if (!isResultOpen()) {

                    return;
                }

                if (mState == ChannelState.EXCEPTION) {

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                    isFlush = true;

                } else {

                    mAbortException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else {

                mRunner.runAbort(mInvocationInstruction);
            }
        }

        @Override
        public void onClose() {

            boolean isFlush = false;

            synchronized (mMutex) {

                verifyOutput();

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;

                    isFlush = true;

                } else {

                    mMutex.notifyAll();
                }
            }

            if (isFlush) {

                flushOutput();
            }
        }

        @Override
        public void onOutput(final OUTPUT output) {

            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                verifyOutput();

                if (delay.isZero()) {

                    mOutputQueue.add(output);

                } else {

                    ++mPendingOutputCount;
                }
            }

            if (delay.isZero()) {

                flushOutput();

            } else {

                mRunner.run(new DelayedOutputInvocation(output), delay.time, delay.unit);
            }
        }
    }
}