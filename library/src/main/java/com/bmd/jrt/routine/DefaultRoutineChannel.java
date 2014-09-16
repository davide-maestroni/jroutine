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
import com.bmd.jrt.subroutine.SubRoutineLoop;
import com.bmd.jrt.time.TimeDuration;
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

    private final InputCall mCall;

    private final Object mChannelMutex = new Object();

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final Object mRoutineMutex = new Object();

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

    private SubRoutineLoop<INPUT, OUTPUT> mRoutine;

    private ChannelState mState = ChannelState.INPUT;

    public DefaultRoutineChannel(final SubRoutineProvider<INPUT, OUTPUT> provider,
            final Runner runner, final int maxParallel) {

        if (provider == null) {

            throw new IllegalArgumentException();
        }

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxParallel < 1) {

            throw new IllegalArgumentException();
        }

        mSubRoutineProvider = provider;
        mRunner = runner;
        mCall = new InputCall();
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

        channel.onResult(new ResultInterceptor<INPUT>() {

            @Override
            public void onReset(final Throwable throwable) {

                synchronized (mChannelMutex) {

                    mInputQueue.clear();

                    mResetException = throwable;
                    mState = ChannelState.EXCEPTION;
                }

                mRunner.onReset(mCall);
            }

            @Override
            public void onResult(final INPUT result) {

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
        });

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

            //TODO: exception...
            final ArrayList<Object> outputs;
            final HashSet<ResultInterceptor<OUTPUT>> interceptors;
            final ChannelState state;

            synchronized (mChannelMutex) {

                interceptors = mResultInterceptors;

                if (interceptors.isEmpty()) {

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

                        resultInterceptor.onReset(((RoutineExceptionWrapper) output).getCause());
                    }

                } else {

                    for (final ResultInterceptor<OUTPUT> resultInterceptor : interceptors) {

                        //noinspection unchecked
                        resultInterceptor.onResult((OUTPUT) output);
                    }
                }
            }

            if (state == ChannelState.RESULT) {

                for (final ResultInterceptor<OUTPUT> resultInterceptor : interceptors) {

                    resultInterceptor.onReturn();
                }
            }

        } catch (final Throwable t) {

            t.printStackTrace();
        }
    }

    private boolean isDone() {

        return ((mState == ChannelState.RESULT) && (mPendingOutputCount <= 0)) || (mState
                == ChannelState.RESET) || (mState == ChannelState.EXCEPTION);
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
        EXCEPTION,
        RESET
    }

    public interface SubRoutineProvider<INPUT, OUTPUT> {

        public SubRoutineLoop<INPUT, OUTPUT> create();

        public void recycle(SubRoutineLoop<INPUT, OUTPUT> routine);
    }

    private static class SynchronizedInterceptor<RESULT> implements ResultInterceptor<RESULT> {

        private final ResultInterceptor<RESULT> mInterceptor;

        private final Object mMutex = new Object();

        public SynchronizedInterceptor(final ResultInterceptor<RESULT> wrapped) {

            mInterceptor = wrapped;
        }

        @Override
        public void onReset(final Throwable throwable) {

            synchronized (mMutex) {

                mInterceptor.onReset(throwable);
            }
        }

        @Override
        public void onResult(final RESULT result) {

            synchronized (mMutex) {

                mInterceptor.onResult(result);
            }
        }

        @Override
        public void onReturn() {

            synchronized (mMutex) {

                mInterceptor.onReturn();
            }
        }
    }

    private class DefaultIterator implements Iterator<OUTPUT> {

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

                boolean isTimeout = false;

                try {

                    final long startNanos = System.nanoTime();

                    do {

                        if (!timeout.waitNanos(mChannelMutex, startNanos) && outputQueue.isEmpty()
                                && !isDone()) {

                            isTimeout = true;

                            break;
                        }

                    } while (outputQueue.isEmpty() && !isDone());

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

                boolean isTimeout = false;

                try {

                    final long startNanos = System.nanoTime();

                    do {

                        if (!timeout.waitNanos(mChannelMutex, startNanos)
                                && outputQueue.isEmpty()) {

                            isTimeout = true;

                            break;
                        }

                    } while (outputQueue.isEmpty());

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

                boolean isTimeout = false;

                try {

                    final long startNanos = System.nanoTime();

                    do {

                        if (!timeout.waitNanos(mChannelMutex, startNanos) && !isDone()) {

                            isTimeout = true;

                            break;
                        }

                    } while (!isDone());

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
        public OutputChannel<OUTPUT> onResult(final ResultInterceptor<OUTPUT> interceptor) {

            synchronized (mChannelMutex) {

                mResultInterceptors.add(new SynchronizedInterceptor<OUTPUT>(interceptor));
            }

            flushOutput();

            return this;
        }

        @Override
        public Iterator<OUTPUT> iterator() {

            return new DefaultIterator();
        }

        @Override
        public boolean isOpen() {

            synchronized (mChannelMutex) {

                return !mOutputQueue.isEmpty() || ((mState != ChannelState.RESULT) && (mState
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

                if ((mState == ChannelState.RESULT) || (mState == ChannelState.RESET)) {

                    return false;
                }

                if (mState == ChannelState.EXCEPTION) {

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable).raise());

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

            channel.onResult(new ResultInterceptor<OUTPUT>() {

                @Override
                public void onReset(final Throwable throwable) {

                    boolean isFlush = false;

                    synchronized (mChannelMutex) {

                        if (mState == ChannelState.EXCEPTION) {

                            mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable).raise());

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

                    synchronized (mChannelMutex) {

                        --mPendingOutputCount;

                        mChannelMutex.notifyAll();
                    }
                }
            });

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

                verifyOutput();

                mState = ChannelState.RESULT;
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

                return (mState != ChannelState.RESULT) && (mState != ChannelState.RESET);
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

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable).raise());

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

                --mPendingOutputCount;

                mOutputQueue.addAll(mOutputs);
            }

            flushOutput();
        }

        @Override
        public void onReset() {

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

                --mPendingOutputCount;

                mOutputQueue.add(mOutput);
            }

            flushOutput();
        }

        @Override
        public void onReset() {

        }
    }

    private class InputCall implements Call {

        private final DefaultResultChannel mResultChannel = new DefaultResultChannel();

        private SubRoutineLoop<INPUT, OUTPUT> initRoutine() {

            final SubRoutineLoop<INPUT, OUTPUT> routine;

            if (mRoutine != null) {

                routine = mRoutine;

            } else {

                routine = (mRoutine = mSubRoutineProvider.create());
                routine.onInit();
            }

            return routine;
        }

        @Override
        public void onInput() {

            synchronized (mRoutineMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;
                final SubRoutineLoop<INPUT, OUTPUT> routine = initRoutine();

                try {

                    synchronized (mChannelMutex) {

                        if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                            return;
                        }

                        --mPendingInputCount;
                    }

                    INPUT input;
                    final LinkedList<INPUT> inputQueue = mInputQueue;

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

        @Override
        public void onReset() {

            synchronized (mRoutineMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;
                final SubRoutineLoop<INPUT, OUTPUT> routine = initRoutine();

                try {

                    final Throwable exception;

                    synchronized (mChannelMutex) {

                        if (mState != ChannelState.EXCEPTION) {

                            return;
                        }

                        exception = mResetException;
                    }

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