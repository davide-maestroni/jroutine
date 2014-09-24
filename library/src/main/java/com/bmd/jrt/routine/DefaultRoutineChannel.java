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
import com.bmd.jrt.channel.ResultConsumer;
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.subroutine.SubRoutine;
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
 * Created by davide on 9/8/14.
 */
class DefaultRoutineChannel<INPUT, OUTPUT> implements RoutineChannel<INPUT, OUTPUT> {

    //TODO: context class with shared fields + lock => in channel, out channel, res channel, invocation...

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final DefaultInvocation mInvocation;

    private final Object mMutex = new Object();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final Runner mRunner;

    private Throwable mAbortException;

    private ArrayList<OutputChannel<?>> mBoundChannels = new ArrayList<OutputChannel<?>>();

    private TimeDuration mInputDelay = TimeDuration.ZERO;

    private int mPendingInputCount;

    private int mPendingOutputCount;

    private ResultConsumer<OUTPUT> mResultConsumer;

    private ChannelState mState = ChannelState.INPUT;

    public DefaultRoutineChannel(final SubRoutineProvider<INPUT, OUTPUT> provider,
            final Runner runner) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mInvocation = new DefaultInvocation(provider);
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> after(final TimeDuration delay) {

        synchronized (mMutex) {

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

        return after(TimeDuration.fromUnit(delay, timeUnit));
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final OutputChannel<INPUT> channel) {

        final TimeDuration delay;

        synchronized (mMutex) {

            verifyInput();

            if (channel == null) {

                return this;
            }

            mBoundChannels.add(channel);

            delay = mInputDelay;

            ++mPendingInputCount;
        }

        channel.bind(new InputResultConsumer(delay));

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final Iterable<? extends INPUT> inputs) {

        final TimeDuration delay;

        synchronized (mMutex) {

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

            mRunner.onInput(mInvocation, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedListInputInvocation(inputs), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final INPUT input) {

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

            mRunner.onInput(mInvocation, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputInvocation(input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final INPUT... inputs) {

        synchronized (mMutex) {

            verifyInput();

            if (inputs == null) {

                return this;
            }
        }

        return pass(Arrays.asList(inputs));
    }

    @Override
    public OutputChannel<OUTPUT> close() {

        synchronized (mMutex) {

            verifyInput();

            mState = ChannelState.OUTPUT;

            ++mPendingInputCount;
        }

        mRunner.onInput(mInvocation, 0, TimeUnit.MILLISECONDS);

        return new DefaultOutputChannel();
    }

    private void flushOutput() {

        try {

            final ArrayList<Object> outputs;
            final ResultConsumer<OUTPUT> consumer;
            final ChannelState state;

            synchronized (mMutex) {

                consumer = mResultConsumer;

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
                    consumer.onResult((OUTPUT) output);
                }
            }

            if (state == ChannelState.DONE) {

                try {

                    consumer.onReturn();

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

                mRunner.onAbort(mInvocation);
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

        if (mResultConsumer != null) {

            throw new IllegalStateException();
        }
    }

    private void verifyInput() {

        final Throwable throwable = mAbortException;

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
        ABORT
    }

    public interface SubRoutineProvider<INPUT, OUTPUT> {

        public SubRoutine<INPUT, OUTPUT> create();

        public void discard(SubRoutine<INPUT, OUTPUT> routine);

        public void recycle(SubRoutine<INPUT, OUTPUT> routine);
    }

    private class DefaultInvocation implements Invocation {

        private final Object mInvocationMutex = new Object();

        private final DefaultResultChannel mResultChannel = new DefaultResultChannel();

        private final SubRoutineProvider<INPUT, OUTPUT> mSubRoutineProvider;

        private SubRoutine<INPUT, OUTPUT> mRoutine;

        public DefaultInvocation(final SubRoutineProvider<INPUT, OUTPUT> provider) {

            if (provider == null) {

                throw new IllegalArgumentException();
            }

            mSubRoutineProvider = provider;
        }

        @Override
        public void onAbort() {

            final Throwable exception;

            synchronized (mInvocationMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;
                SubRoutine<INPUT, OUTPUT> routine = null;

                synchronized (mMutex) {

                    if (mState != ChannelState.EXCEPTION) {

                        return;
                    }

                    exception = mAbortException;
                }

                try {

                    routine = initRoutine();

                    routine.onAbort(exception);
                    resultChannel.abort(exception);

                    routine.onReturn();
                    mSubRoutineProvider.recycle(routine);

                } catch (final Throwable t) {

                    if (routine != null) {

                        mSubRoutineProvider.discard(routine);
                    }

                    resultChannel.abort(t);

                } finally {

                    final ArrayList<OutputChannel<?>> channels;

                    synchronized (mMutex) {

                        channels = new ArrayList<OutputChannel<?>>(mBoundChannels);
                        mBoundChannels.clear();

                        mState = ChannelState.ABORT;
                        mMutex.notifyAll();
                    }

                    for (final OutputChannel<?> channel : channels) {

                        channel.abort(exception);
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
        public void onInput() {

            synchronized (mInvocationMutex) {

                final DefaultResultChannel resultChannel = mResultChannel;

                try {

                    synchronized (mMutex) {

                        if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                            return;
                        }

                        --mPendingInputCount;
                    }

                    final SubRoutine<INPUT, OUTPUT> routine = initRoutine();
                    final LinkedList<INPUT> inputQueue = mInputQueue;

                    INPUT input;

                    while (true) {

                        synchronized (mMutex) {

                            if (!inputQueue.isEmpty()) {

                                input = inputQueue.removeFirst();

                            } else {

                                break;
                            }
                        }

                        routine.onInput(input, resultChannel);
                    }

                    final boolean isEnded;

                    synchronized (mMutex) {

                        isEnded = (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
                    }

                    if (isEnded) {

                        routine.onResult(resultChannel);
                        resultChannel.close();

                        routine.onReturn();
                        mSubRoutineProvider.recycle(routine);
                    }

                } catch (final Throwable t) {

                    resultChannel.abort(t);
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

        private final TimeDuration mTimeout;

        private final RuntimeException mTimeoutException;

        private boolean mRemoved = true;

        private DefaultIterator(final TimeDuration timeout, final RuntimeException exception) {

            mTimeout = timeout;
            mTimeoutException = exception;
        }

        @Override
        public boolean hasNext() {

            synchronized (mMutex) {

                verifyBound();

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;

                if (timeout.isZero() || isDone()) {

                    return !outputQueue.isEmpty();
                }

                boolean isTimeout = false;

                try {

                    isTimeout = !timeout.waitTrue(mMutex, mHasNext);

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

        @Override
        public OUTPUT next() {

            synchronized (mMutex) {

                verifyBound();

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;

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

                mRemoved = false;

                //noinspection unchecked
                return (OUTPUT) result;
            }
        }

        @Override
        public void remove() {

            synchronized (mMutex) {

                verifyBound();

                if (mRemoved) {

                    throw new IllegalStateException();
                }

                mRemoved = true;
            }
        }
    }

    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        private TimeDuration mTimeout = seconds(3);

        private RuntimeException mTimeoutException;

        @Override
        public boolean abort() {

            return abort(null);
        }

        @Override
        public OutputChannel<OUTPUT> afterMax(final TimeDuration timeout) {

            synchronized (mMutex) {

                verifyBound();

                if (timeout == null) {

                    throw new IllegalArgumentException();
                }

                mTimeout = timeout;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> afterMax(final long timeout, final TimeUnit timeUnit) {

            return afterMax(TimeDuration.fromUnit(timeout, timeUnit));
        }

        @Override
        public OutputChannel<OUTPUT> bind(final ResultConsumer<OUTPUT> consumer) {

            synchronized (mMutex) {

                verifyBound();

                if (consumer == null) {

                    throw new IllegalArgumentException();
                }

                mResultConsumer = new SynchronizedConsumer<OUTPUT>(consumer);
            }

            flushOutput();

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> eventuallyThrow(final RuntimeException exception) {

            synchronized (mMutex) {

                verifyBound();

                mTimeoutException = exception;
            }

            return this;
        }

        @Override
        public OutputChannel<OUTPUT> immediately() {

            synchronized (mMutex) {

                verifyBound();

                mTimeout = TimeDuration.ZERO;
            }

            return this;
        }

        @Override
        public List<OUTPUT> readAll() {

            final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
            readAllInto(results);

            return results;
        }

        @Override
        public OutputChannel<OUTPUT> readAllInto(final List<OUTPUT> results) {

            synchronized (mMutex) {

                verifyBound();

                final LinkedList<Object> outputQueue = mOutputQueue;
                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;

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

            return this;
        }

        @Override
        public boolean waitDone() {

            boolean isDone = false;

            synchronized (mMutex) {

                final TimeDuration timeout = mTimeout;
                final RuntimeException timeoutException = mTimeoutException;

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

        @Override
        public Iterator<OUTPUT> iterator() {

            final TimeDuration timeout;
            final RuntimeException exception;

            synchronized (mMutex) {

                verifyBound();

                timeout = mTimeout;
                exception = mTimeoutException;
            }

            return new DefaultIterator(timeout, exception);
        }

        @Override
        public boolean abort(final Throwable throwable) {

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

                mRunner.onAbort(mInvocation);
            }

            return true;
        }

        @Override
        public boolean isOpen() {

            synchronized (mMutex) {

                return !mOutputQueue.isEmpty() || ((mState != ChannelState.DONE) && (mState
                        != ChannelState.ABORT));
            }
        }
    }

    private class DefaultResultChannel implements ResultChannel<OUTPUT> {

        private TimeDuration mOutputDelay = TimeDuration.ZERO;

        @Override
        public ResultChannel<OUTPUT> after(final TimeDuration delay) {

            if (delay == null) {

                throw new IllegalArgumentException();
            }

            synchronized (mMutex) {

                verifyOutput();

                mOutputDelay = delay;
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> after(final long delay, final TimeUnit timeUnit) {

            return after(TimeDuration.fromUnit(delay, timeUnit));
        }

        @Override
        public ResultChannel<OUTPUT> pass(final OutputChannel<OUTPUT> channel) {

            final TimeDuration delay;

            synchronized (mMutex) {

                verifyOutput();

                if (channel == null) {

                    return this;
                }

                mBoundChannels.add(channel);

                delay = mInputDelay;

                ++mPendingOutputCount;
            }

            channel.bind(new OutputResultConsumer(delay));

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> pass(final Iterable<? extends OUTPUT> outputs) {

            final TimeDuration delay;

            synchronized (mMutex) {

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

                mRunner.onInput(new DelayedListOutputInvocation(outputs), delay.time, delay.unit);
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> pass(final OUTPUT output) {

            final TimeDuration delay;

            synchronized (mMutex) {

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

                mRunner.onInput(new DelayedOutputInvocation(output), delay.time, delay.unit);
            }

            return this;
        }

        @Override
        public ResultChannel<OUTPUT> pass(final OUTPUT... outputs) {

            synchronized (mMutex) {

                verifyOutput();

                if (outputs == null) {

                    return this;
                }
            }

            return pass(Arrays.asList(outputs));
        }

        private void close() {

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

        private void verifyOutput() {

            if (!isOpen()) {

                throw new IllegalStateException();
            }
        }

        @Override
        public boolean abort() {

            return abort(null);
        }

        @Override
        public boolean abort(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mMutex) {

                if (!isOpen()) {

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

            mRunner.onAbort(mInvocation);

            return false;
        }

        @Override
        public boolean isOpen() {

            synchronized (mMutex) {

                return !isDone();
            }
        }
    }

    private class DelayedInputInvocation implements Invocation {

        private final INPUT mInput;

        public DelayedInputInvocation(final INPUT input) {

            mInput = input;
        }

        @Override
        public void onInput() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.add(mInput);
            }

            mInvocation.onInput();
        }

        @Override
        public void onAbort() {

            mInvocation.onAbort();
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
        public void onInput() {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.addAll(mInputs);
            }

            mInvocation.onInput();
        }

        @Override
        public void onAbort() {

            mInvocation.onAbort();
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
        public void onInput() {

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

        @Override
        public void onAbort() {

            mInvocation.onAbort();
        }
    }

    private class DelayedOutputInvocation implements Invocation {

        private final OUTPUT mOutput;

        public DelayedOutputInvocation(final OUTPUT output) {

            mOutput = output;
        }

        @Override
        public void onInput() {

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

        @Override
        public void onAbort() {

            mInvocation.onAbort();
        }
    }

    private class InputResultConsumer implements ResultConsumer<INPUT> {

        private final TimeDuration mDelay;

        public InputResultConsumer(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onAbort(final Throwable throwable) {

            synchronized (mMutex) {

                if ((mState != ChannelState.INPUT) && (mState != ChannelState.OUTPUT)) {

                    return;
                }

                mInputQueue.clear();

                mAbortException = throwable;
                mState = ChannelState.EXCEPTION;
            }

            mRunner.onAbort(mInvocation);
        }

        @Override
        public void onResult(final INPUT result) {

            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                if (delay.isZero()) {

                    mInputQueue.add(result);
                }

                ++mPendingInputCount;
            }

            if (delay.isZero()) {

                mRunner.onInput(mInvocation, 0, TimeUnit.MILLISECONDS);

            } else {

                mRunner.onInput(new DelayedInputInvocation(result), delay.time, delay.unit);
            }
        }

        @Override
        public void onReturn() {

            mRunner.onInput(mInvocation, 0, TimeUnit.MILLISECONDS);
        }
    }

    private class OutputResultConsumer implements ResultConsumer<OUTPUT> {

        private final TimeDuration mDelay;

        public OutputResultConsumer(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onAbort(final Throwable throwable) {

            boolean isFlush = false;

            synchronized (mMutex) {

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

                mRunner.onAbort(mInvocation);
            }
        }

        @Override
        public void onResult(final OUTPUT result) {

            final TimeDuration delay = mDelay;

            synchronized (mMutex) {

                if (delay.isZero()) {

                    mOutputQueue.add(result);

                } else {

                    ++mPendingOutputCount;
                }
            }

            if (delay.isZero()) {

                flushOutput();

            } else {

                mRunner.onInput(new DelayedOutputInvocation(result), delay.time, delay.unit);
            }
        }

        @Override
        public void onReturn() {

            boolean isFlush = false;

            synchronized (mMutex) {

                if (isError()) {

                    return;
                }

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
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Override
    public boolean abort(final Throwable throwable) {

        synchronized (mMutex) {

            if (!isOpen()) {

                return false;
            }

            mInputQueue.clear();

            mAbortException = throwable;
            mState = ChannelState.EXCEPTION;
        }

        mRunner.onAbort(mInvocation);

        return false;
    }

    @Override
    public boolean isOpen() {

        synchronized (mMutex) {

            return (mState == ChannelState.INPUT);
        }
    }
}