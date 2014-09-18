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
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.ResultInterceptor;
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.runner.Call;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/8/14.
 */
class DefaultRoutineChannel<INPUT, OUTPUT> implements RoutineChannel<INPUT, OUTPUT> {

    private final DefaultCall mCall;

    private final Object mChannelMutex = new Object();

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final Runner mRunner;

    private final SubRoutineProvider<INPUT, OUTPUT> mSubRoutineProvider;

    private TimeDuration mInputDelay = TimeDuration.ZERO;

    private RuntimeException mOutputException;

    //TODO
    private TimeDuration mOutputTimeout = TimeDuration.seconds(3);

    private int mPendingInputCount;

    private int mPendingOutputCount;

    private Throwable mResetException;

    private HashSet<ResultInterceptor<OUTPUT>> mResultInterceptors =
            new HashSet<ResultInterceptor<OUTPUT>>();

    private ChannelState mState = ChannelState.INPUT;

    public DefaultRoutineChannel(final SubRoutineProvider<INPUT, OUTPUT> provider,
            final Runner runner) {

        if (provider == null) {

            throw new IllegalArgumentException();
        }

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        mSubRoutineProvider = provider;
        mRunner = runner;
        mCall = new DefaultCall();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> after(final TimeDuration delay) {

        synchronized (mChannelMutex) {

            verifyInput();

            if (delay == null) {

                throw new IllegalArgumentException();
            }

            mInputDelay = delay;
        }

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(TimeDuration.from(delay, timeUnit));
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> push(final OutputChannel<INPUT> channel) {

        final TimeDuration delay;

        synchronized (mChannelMutex) {

            verifyInput();

            if (channel == null) {

                return this;
            }

            delay = mInputDelay;

            ++mPendingInputCount;
        }

        channel.bind(new InputResultInterceptor(delay));

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> push(final Iterable<? extends INPUT> inputs) {

        final TimeDuration delay;

        synchronized (mChannelMutex) {

            verifyInput();

            if (inputs == null) {

                return this;
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

            mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedListInputCall(inputs), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> push(final INPUT input) {

        final TimeDuration delay;

        synchronized (mChannelMutex) {

            verifyInput();

            delay = mInputDelay;

            if (delay.isZero()) {

                mInputQueue.add(input);
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputCall(input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> push(final INPUT... inputs) {

        synchronized (mChannelMutex) {

            verifyInput();

            if (inputs == null) {

                return this;
            }
        }

        return push(Arrays.asList(inputs));
    }

    @Override
    public OutputChannel<OUTPUT> close() {

        synchronized (mChannelMutex) {

            verifyInput();

            mState = ChannelState.OUTPUT;

            ++mPendingInputCount;
        }

        mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

        return new DefaultOutputChannel();
    }

    @Override
    public boolean isOpen() {

        synchronized (mChannelMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    private void flushOutput() {

        try {

            final ArrayList<Object> outputs;
            final HashSet<ResultInterceptor<OUTPUT>> interceptors;
            final ChannelState state;

            synchronized (mChannelMutex) {

                interceptors = mResultInterceptors;

                if (interceptors.isEmpty()) {

                    mChannelMutex.notifyAll();

                    return;
                }

                outputs = new ArrayList<Object>(mOutputQueue);
                mOutputQueue.clear();
                state = mState;

                mChannelMutex.notifyAll();
            }

            for (final Object output : outputs) {

                if (output instanceof RoutineExceptionWrapper) {

                    for (final ResultInterceptor<OUTPUT> resultInterceptor : interceptors) {

                        try {

                            resultInterceptor.onReset(
                                    ((RoutineExceptionWrapper) output).getCause());

                        } catch (final RoutineInterruptedException e) {

                            Thread.currentThread().interrupt();

                            throw e;

                        } catch (final Throwable ignored) {

                        }
                    }

                    break;

                } else {

                    for (final ResultInterceptor<OUTPUT> resultInterceptor : interceptors) {

                        //noinspection unchecked
                        resultInterceptor.onResult((OUTPUT) output);
                    }
                }
            }

            if (state == ChannelState.DONE) {

                for (final ResultInterceptor<OUTPUT> resultInterceptor : interceptors) {

                    try {

                        resultInterceptor.onReturn();

                    } catch (final RoutineInterruptedException e) {

                        Thread.currentThread().interrupt();

                        throw e;

                    } catch (final Throwable ignored) {

                    }
                }
            }

        } catch (final Throwable t) {

            boolean isFlush = false;
            boolean isReset = false;

            synchronized (mChannelMutex) {

                if ((mState == ChannelState.DONE) || (mState == ChannelState.RESET)) {

                    isFlush = true;

                } else if (mState != ChannelState.EXCEPTION) {

                    isReset = true;

                    mOutputQueue.clear();

                    mResetException = t;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();
            }

            if (isReset) {

                mRunner.onReset(mCall);
            }
        }
    }

    private boolean isDone() {

        return (mState == ChannelState.DONE) || (mState == ChannelState.RESET);
    }

    private void verifyInput() {

        final Throwable throwable = mResetException;

        if (throwable != null) {

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isOpen()) {

            throw new IllegalStateException();
        }
    }

    private static enum ChannelState {

        INPUT,
        OUTPUT,
        RESULT,
        DONE,
        EXCEPTION,
        RESET
    }

    public interface SubRoutineProvider<INPUT, OUTPUT> {

        public SubRoutine<INPUT, OUTPUT> create();

        public void recycle(SubRoutine<INPUT, OUTPUT> routine);
    }

    private class DefaultCall implements Call {

        private final Object mMutex = new Object();

        private final DefaultResultChannel mResultChannel = new DefaultResultChannel();

        private SubRoutine<INPUT, OUTPUT> mRoutine;

        @Override
        public void onInput() {

            synchronized (mMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;

                try {

                    synchronized (mChannelMutex) {

                        if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                            return;
                        }

                        --mPendingInputCount;
                    }

                    final SubRoutine<INPUT, OUTPUT> routine = initRoutine();
                    final LinkedList<INPUT> inputQueue = mInputQueue;

                    INPUT input;

                    while (true) {

                        synchronized (mChannelMutex) {

                            if (!inputQueue.isEmpty()) {

                                input = inputQueue.removeFirst();

                            } else {

                                break;
                            }
                        }

                        routine.onInput(input, resultChannel);
                    }

                    final boolean isEnded;

                    synchronized (mChannelMutex) {

                        isEnded = (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
                    }

                    if (isEnded) {

                        routine.onResult(resultChannel);
                        resultChannel.close();

                        routine.onReturn();
                        mSubRoutineProvider.recycle(routine);
                    }

                } catch (final Throwable t) {

                    resultChannel.reset(t);

                } finally {

                    synchronized (mChannelMutex) {

                        mChannelMutex.notifyAll();
                    }
                }
            }
        }

        private SubRoutine<INPUT, OUTPUT> initRoutine() {

            final SubRoutine<INPUT, OUTPUT> routine;

            if (mRoutine != null) {

                routine = mRoutine;

            } else {

                routine = (mRoutine = mSubRoutineProvider.create());
                routine.onInit();
            }

            return routine;
        }

        @Override
        public void onReset() {

            synchronized (mMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;

                try {

                    final Throwable exception;

                    synchronized (mChannelMutex) {

                        if (mState != ChannelState.EXCEPTION) {

                            return;
                        }

                        exception = mResetException;
                    }

                    final SubRoutine<INPUT, OUTPUT> routine = initRoutine();

                    routine.onReset(exception);
                    resultChannel.reset(exception);

                    routine.onReturn();
                    mSubRoutineProvider.recycle(routine);

                } catch (final Throwable t) {

                    resultChannel.reset(t);

                } finally {

                    synchronized (mChannelMutex) {

                        mState = ChannelState.RESET;
                        mChannelMutex.notifyAll();
                    }
                }
            }
        }
    }

    private class DefaultIterator implements Iterator<OUTPUT> {

        private final Check mHasNext = new Check() {

            @Override
            public boolean isTrue() {

                return !mOutputQueue.isEmpty() || isDone();
            }
        };

        private final Check mOutputNotEmpty = new Check() {

            @Override
            public boolean isTrue() {

                return !mOutputQueue.isEmpty();
            }
        };

        private boolean mRemoved = true;

        @Override
        public boolean hasNext() {

            synchronized (mChannelMutex) {

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputException;

                if (timeout.isZero() || isDone()) {

                    return !outputQueue.isEmpty();
                }

                final boolean isTimeout;

                try {

                    isTimeout = !timeout.waitCondition(mChannelMutex, mHasNext);

                } catch (final InterruptedException e) {

                    Thread.currentThread().interrupt();

                    throw new RoutineInterruptedException(e);
                }

                if (isTimeout) {

                    if (timeoutException != null) {

                        throw timeoutException;
                    }
                }

                return !outputQueue.isEmpty();
            }
        }

        @Override
        public OUTPUT next() {

            synchronized (mChannelMutex) {

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputException;

                if (timeout.isZero() || !outputQueue.isEmpty()) {

                    if (outputQueue.isEmpty()) {

                        throw new NoSuchElementException();
                    }

                    final Object result = outputQueue.removeFirst();

                    RoutineExceptionWrapper.raise(result);

                    mRemoved = false;

                    //noinspection unchecked
                    return (OUTPUT) result;
                }

                final boolean isTimeout;

                try {

                    isTimeout = !timeout.waitCondition(mChannelMutex, mOutputNotEmpty);

                } catch (final InterruptedException e) {

                    Thread.currentThread().interrupt();

                    throw new RoutineInterruptedException(e);
                }

                if (isTimeout) {

                    if (timeoutException != null) {

                        throw timeoutException;
                    }
                }

                final Object result = outputQueue.removeFirst();

                RoutineExceptionWrapper.raise(result);

                mRemoved = false;

                //noinspection unchecked
                return (OUTPUT) result;
            }
        }

        @Override
        public void remove() {

            synchronized (mChannelMutex) {

                if (mRemoved) {

                    throw new IllegalStateException();
                }

                mRemoved = true;
            }
        }
    }

    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        @Override
        public OutputChannel<OUTPUT> afterMax(final TimeDuration timeout) {

            if (timeout == null) {

                throw new IllegalArgumentException();
            }

            synchronized (mChannelMutex) {

                mOutputTimeout = timeout;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> afterMax(final long timeout, final TimeUnit timeUnit) {

            return afterMax(TimeDuration.from(timeout, timeUnit));
        }

        @Override
        public List<OUTPUT> all() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            allInto(results);

            return results;
        }

        @Override
        public OutputChannel<OUTPUT> allInto(final List<OUTPUT> results) {

            synchronized (mChannelMutex) {

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mOutputTimeout;
                final RuntimeException timeoutException = mOutputException;

                if (timeout.isZero() || isDone()) {

                    final Iterator<Object> iterator = outputQueue.iterator();

                    while (iterator.hasNext()) {

                        final Object result = iterator.next();
                        iterator.remove();

                        RoutineExceptionWrapper.raise(result);

                        //noinspection unchecked
                        results.add((OUTPUT) result);
                    }

                    return this;
                }

                final boolean isTimeout;

                try {

                    isTimeout = !timeout.waitCondition(mChannelMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return isDone();
                        }
                    });

                } catch (final InterruptedException e) {

                    Thread.currentThread().interrupt();

                    throw new RoutineInterruptedException(e);
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

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> bind(final ResultInterceptor<OUTPUT> interceptor) {

            synchronized (mChannelMutex) {

                mResultInterceptors.add(new SynchronizedInterceptor<OUTPUT>(interceptor));
            }

            flushOutput();

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> eventually() {

            synchronized (mChannelMutex) {

                mOutputTimeout = TimeDuration.INFINITE;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> eventuallyThrow(final RuntimeException exception) {

            synchronized (mChannelMutex) {

                mOutputException = exception;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> immediately() {

            synchronized (mChannelMutex) {

                mOutputTimeout = TimeDuration.ZERO;
            }

            return this;
        }

        @Override
        public Iterator<OUTPUT> iterator() {

            return new DefaultIterator();
        }

        @Override
        public boolean isOpen() {

            synchronized (mChannelMutex) {

                return !mOutputQueue.isEmpty() || ((mState != ChannelState.DONE) && (mState
                        != ChannelState.RESET));
            }
        }

        @Override
        public boolean reset() {

            return reset(null);
        }

        @Override
        public boolean reset(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mChannelMutex) {

                if ((mState == ChannelState.DONE) || (mState == ChannelState.RESET)) {

                    return false;
                }

                if (mState == ChannelState.EXCEPTION) {

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                    isFlush = true;

                } else {

                    mOutputQueue.clear();

                    mResetException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else {

                mRunner.onReset(mCall);
            }

            return true;
        }
    }

    private class DefaultResultChannel implements ResultChannel<OUTPUT> {

        private TimeDuration mOutputDelay = TimeDuration.ZERO;

        @Override
        public ResultChannel<OUTPUT> after(final TimeDuration delay) {

            synchronized (mChannelMutex) {

                verifyOutput();

                if (delay == null) {

                    throw new IllegalArgumentException();
                }

                mOutputDelay = delay;
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> after(final long delay, final TimeUnit timeUnit) {

            return after(TimeDuration.from(delay, timeUnit));
        }

        @Override
        public ResultChannel<OUTPUT> push(final OutputChannel<OUTPUT> channel) {

            final TimeDuration delay;

            synchronized (mChannelMutex) {

                verifyOutput();

                if (channel == null) {

                    return this;
                }

                delay = mInputDelay;

                ++mPendingOutputCount;
            }

            channel.bind(new OutputResultInterceptor(delay));

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> push(final Iterable<? extends OUTPUT> outputs) {

            final TimeDuration delay;

            synchronized (mChannelMutex) {

                verifyOutput();

                if (outputs == null) {

                    return this;
                }

                delay = mOutputDelay;

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

                mRunner.onInput(new DelayedListOutputCall(outputs), delay.time, delay.unit);
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> push(final OUTPUT output) {

            final TimeDuration delay;

            synchronized (mChannelMutex) {

                verifyOutput();

                delay = mOutputDelay;

                if (delay.isZero()) {

                    mOutputQueue.add(output);

                } else {

                    ++mPendingOutputCount;
                }
            }

            if (delay.isZero()) {

                flushOutput();

            } else {

                mRunner.onInput(new DelayedOutputCall(output), delay.time, delay.unit);
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> push(final OUTPUT... outputs) {

            synchronized (mChannelMutex) {

                verifyOutput();

                if (outputs == null) {

                    return this;
                }
            }

            return push(Arrays.asList(outputs));
        }

        private void close() {

            synchronized (mChannelMutex) {

                if (mState == ChannelState.OUTPUT) {

                    mState = (mPendingOutputCount > 0) ? ChannelState.RESULT : ChannelState.DONE;
                }
            }

            flushOutput();
        }

        private void verifyOutput() {

            if (!isOpen()) {

                throw new IllegalStateException();
            }
        }

        @Override
        public boolean isOpen() {

            synchronized (mChannelMutex) {

                return (mState != ChannelState.DONE) && (mState != ChannelState.RESET);
            }
        }

        @Override
        public boolean reset() {

            return reset(null);
        }

        @Override
        public boolean reset(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mChannelMutex) {

                if (!isOpen()) {

                    return false;
                }

                if (mState == ChannelState.EXCEPTION) {

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                    isFlush = true;

                } else {

                    mResetException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

                return true;
            }

            mRunner.onReset(mCall);

            return false;
        }
    }

    private class DelayedInputCall implements Call {

        private final INPUT mInput;

        public DelayedInputCall(final INPUT input) {

            mInput = input;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.add(mInput);
            }

            mCall.onInput();
        }

        @Override
        public void onReset() {

            mCall.onReset();
        }
    }

    private class DelayedListInputCall implements Call {

        private final ArrayList<INPUT> mInputs;

        public DelayedListInputCall(final Iterable<? extends INPUT> inputs) {

            final ArrayList<INPUT> inputList = new ArrayList<INPUT>();

            for (final INPUT input : inputs) {

                inputList.add(input);
            }

            mInputs = inputList;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.addAll(mInputs);
            }

            mCall.onInput();
        }

        @Override
        public void onReset() {

            mCall.onReset();
        }
    }

    private class DelayedListOutputCall implements Call {

        private final ArrayList<OUTPUT> mOutputs;

        public DelayedListOutputCall(final Iterable<? extends OUTPUT> outputs) {

            final ArrayList<OUTPUT> outputList = new ArrayList<OUTPUT>();

            for (final OUTPUT output : outputs) {

                outputList.add(output);
            }

            mOutputs = outputList;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mOutputQueue.addAll(mOutputs);
            }

            flushOutput();
        }

        @Override
        public void onReset() {

            mCall.onReset();
        }
    }

    private class DelayedOutputCall implements Call {

        private final OUTPUT mOutput;

        public DelayedOutputCall(final OUTPUT output) {

            mOutput = output;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;
                }

                mOutputQueue.add(mOutput);
            }

            flushOutput();
        }

        @Override
        public void onReset() {

            mCall.onReset();
        }
    }

    private class InputResultInterceptor implements ResultInterceptor<INPUT> {

        private final TimeDuration mDelay;

        public InputResultInterceptor(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onReset(final Throwable throwable) {

            synchronized (mChannelMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.clear();

                mResetException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mRunner.onReset(mCall);
        }

        @Override
        public void onResult(final INPUT result) {

            final TimeDuration delay = mDelay;

            synchronized (mChannelMutex) {

                if (delay.isZero()) {

                    mInputQueue.add(result);
                }

                ++mPendingInputCount;
            }

            if (delay.isZero()) {

                mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.onInput(new DelayedInputCall(result), delay.time, delay.unit);
            }
        }

        @Override
        public void onReturn() {

            mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);
        }
    }

    private class OutputResultInterceptor implements ResultInterceptor<OUTPUT> {

        private final TimeDuration mDelay;

        public OutputResultInterceptor(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onReset(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mChannelMutex) {

                if (mState == ChannelState.EXCEPTION) {

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));

                    isFlush = true;

                } else {

                    mResetException = throwable;
                    mState = ChannelState.EXCEPTION;
                }
            }

            if (isFlush) {

                flushOutput();

            } else {

                mRunner.onReset(mCall);
            }
        }

        @Override
        public void onResult(final OUTPUT result) {

            final TimeDuration delay = mDelay;

            synchronized (mChannelMutex) {

                if (delay.isZero()) {

                    mOutputQueue.add(result);

                } else {

                    ++mPendingOutputCount;
                }
            }

            if (delay.isZero()) {

                flushOutput();

            } else {

                mRunner.onInput(new DelayedOutputCall(result), delay.time, delay.unit);
            }
        }

        @Override
        public void onReturn() {

            boolean isFlush = false;

            synchronized (mChannelMutex) {

                if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                    return;
                }

                if ((--mPendingOutputCount == 0) && (mState == ChannelState.RESULT)) {

                    mState = ChannelState.DONE;

                    isFlush = true;

                } else {

                    mChannelMutex.notifyAll();
                }
            }

            if (isFlush) {

                flushOutput();
            }
        }
    }

    @Override
    public boolean reset() {

        return reset(null);
    }

    @Override
    public boolean reset(final Throwable throwable) {

        synchronized (mChannelMutex) {

            if (!isOpen()) {

                return false;
            }

            mInputQueue.clear();

            mResetException = throwable;
            mState = ChannelState.EXCEPTION;
        }

        mRunner.onReset(mCall);

        return false;
    }
}