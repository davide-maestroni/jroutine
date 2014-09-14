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
import com.bmd.jrt.runner.Call;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.subroutine.ResultPublisher;
import com.bmd.jrt.subroutine.SubRoutineLoop;
import com.bmd.jrt.time.PositiveDuration;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/8/14.
 */
class DefaultRoutineChannel<INPUT, OUTPUT> implements InputChannel<INPUT, OUTPUT> {

    private static final ThreadLocal<Runner> sCurrentRunner = new ThreadLocal<Runner>();

    private final DefaultCall mCall;

    private final Object mChannelMutex = new Object();

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final Object mRoutineMutex = new Object();

    private final Runner mRunner;

    private final SubRoutineProvider<INPUT, OUTPUT> mSubRoutineProvider;

    private PositiveDuration mInputDelay = PositiveDuration.ZERO;

    private RuntimeException mOutputException;

    private PositiveDuration mOutputTimeout = PositiveDuration.INFINITE;

    private int mPendingInputCount;

    private RuntimeException mResetException;

    private SubRoutineLoop<INPUT, OUTPUT> mRoutine;

    private Throwable mRoutineException;

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
        mCall = new DefaultCall();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> after(final PositiveDuration delay) {

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
    public InputChannel<INPUT, OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(PositiveDuration.time(delay, timeUnit));
    }

    @Override
    public OutputChannel<OUTPUT> end() {

        synchronized (mChannelMutex) {

            verifyInput();

            mState = ChannelState.OUTPUT;
        }

        getRunner().onInput(mCall, 0, TimeUnit.MILLISECONDS);

        return new DefaultOutputChannel();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> push(final INPUT... inputs) {

        if (inputs == null) {

            return this;
        }

        return push(Arrays.asList(inputs));
    }

    @Override
    public InputChannel<INPUT, OUTPUT> push(final Iterable<? extends INPUT> inputs) {

        if (inputs == null) {

            return this;
        }

        final PositiveDuration delay;

        synchronized (mChannelMutex) {

            verifyInput();

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

            getRunner().onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            getRunner().onInput(new DelayedInputsCall(inputs), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public InputChannel<INPUT, OUTPUT> push(final INPUT input) {

        final PositiveDuration delay;

        synchronized (mChannelMutex) {

            verifyInput();

            delay = mInputDelay;

            if (delay.isZero()) {

                mInputQueue.add(input);
            }

            ++mPendingInputCount;
        }

        if (delay.isZero()) {

            getRunner().onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            getRunner().onInput(new DelayedInputCall(input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public boolean isOpen() {

        synchronized (mChannelMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    private Runner getRunner() {

        final Runner runner = mRunner;

        if (runner.equals(sCurrentRunner.get())) {

            return Runners.sync();
        }

        return runner;
    }

    private void verifyInput() {

        final Throwable throwable = mRoutineException;

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
        RESET,
        EXCEPTION
    }

    public interface SubRoutineProvider<INPUT, OUTPUT> {

        public SubRoutineLoop<INPUT, OUTPUT> create();

        public void recycle(SubRoutineLoop<INPUT, OUTPUT> routine);
    }

    private class DefaultCall implements Call {

        private final DefaultPublisher mPublisher = new DefaultPublisher();

        @Override
        public void onInput() {

            synchronized (mRoutineMutex) {

                final ThreadLocal<Runner> localRunner = sCurrentRunner;
                final Runner currentRunner = localRunner.get();

                if (currentRunner == null) {

                    localRunner.set(mRunner);
                }

                final DefaultPublisher publisher = mPublisher;
                final SubRoutineProvider<INPUT, OUTPUT> provider = mSubRoutineProvider;
                final SubRoutineLoop<INPUT, OUTPUT> routine;

                if (mRoutine != null) {

                    routine = mRoutine;

                } else {

                    routine = (mRoutine = provider.create());
                    routine.onInit();
                }

                boolean isEnded = false;

                try {

                    synchronized (mChannelMutex) {

                        if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                            return;
                        }

                        --mPendingInputCount;
                    }

                    INPUT input;

                    while (true) {

                        synchronized (mChannelMutex) {

                            final LinkedList<INPUT> inputQueue = mInputQueue;
                            final boolean empty = inputQueue.isEmpty();

                            if (!empty) {

                                input = inputQueue.removeFirst();

                            } else {

                                break;
                            }
                        }

                        routine.onInput(input, publisher);
                    }

                    synchronized (mChannelMutex) {

                        isEnded = (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
                    }

                    if (isEnded) {

                        routine.onResult(publisher);

                        synchronized (mChannelMutex) {

                            mState = ChannelState.RESULT;
                        }

                        routine.onReturn();
                        provider.recycle(routine);
                    }

                } catch (final Throwable t) {

                    publisher.publishException(t);

                } finally {

                    if (isEnded) {

                        synchronized (mChannelMutex) {

                            mChannelMutex.notifyAll();
                        }
                    }

                    if (currentRunner == null) {

                        localRunner.remove();
                    }
                }
            }
        }

        @Override
        public void onReset() {

            synchronized (mRoutineMutex) {

                final ThreadLocal<Runner> localRunner = sCurrentRunner;
                final Runner currentRunner = localRunner.get();

                if (currentRunner == null) {

                    localRunner.set(mRunner);
                }

                final DefaultPublisher publisher = mPublisher;
                final SubRoutineProvider<INPUT, OUTPUT> provider = mSubRoutineProvider;
                final SubRoutineLoop<INPUT, OUTPUT> routine;

                if (mRoutine != null) {

                    routine = mRoutine;

                } else {

                    routine = (mRoutine = provider.create());
                    routine.onInit();
                }

                try {

                    synchronized (mChannelMutex) {

                        if (mState == ChannelState.EXCEPTION) {

                            return;
                        }
                    }

                    routine.onReset(publisher);

                    synchronized (mChannelMutex) {

                        final RuntimeException resetException = mResetException;

                        if (resetException != null) {

                            publisher.publishException(resetException);
                        }
                    }

                    routine.onReturn();
                    provider.recycle(routine);

                } catch (final Throwable t) {

                    publisher.publishException(t);

                } finally {

                    if (currentRunner == null) {

                        localRunner.remove();
                    }
                }
            }
        }
    }

    private class DefaultIterator implements Iterator<OUTPUT> {

        private boolean mRemoved = true;

        @Override
        public boolean hasNext() {

            synchronized (mChannelMutex) {

                final LinkedList<Object> outputQueue = mOutputQueue;
                final PositiveDuration timeout = mOutputTimeout;
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
                final PositiveDuration timeout = mOutputTimeout;
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

        private boolean isDone() {

            return (mState != ChannelState.OUTPUT);
        }
    }

    private class DefaultOutputChannel implements OutputChannel<OUTPUT> {

        @Override
        public OutputChannel<OUTPUT> afterMax(final PositiveDuration timeout) {

            if (timeout == null) {

                throw new IllegalArgumentException();
            }

            synchronized (mChannelMutex) {

                mOutputTimeout = timeout;
            }

            return null;
        }

        @Override
        public OutputChannel<OUTPUT> afterMax(final long timeout, final TimeUnit timeUnit) {

            return afterMax(PositiveDuration.time(timeout, timeUnit));
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
                final PositiveDuration timeout = mOutputTimeout;
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

                mOutputTimeout = PositiveDuration.INFINITE;
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

                mOutputTimeout = PositiveDuration.ZERO;
            }

            return this;
        }

        @Override
        public Iterator<OUTPUT> iterator() {

            return new DefaultIterator();
        }

        private boolean isDone() {

            return (mState != ChannelState.OUTPUT);
        }

        @Override
        public boolean isOpen() {

            synchronized (mChannelMutex) {

                return !mOutputQueue.isEmpty() || (mState == ChannelState.OUTPUT);
            }
        }

        @Override
        public boolean reset() {

            return reset(null);
        }

        @Override
        public boolean reset(final RuntimeException exception) {

            synchronized (mChannelMutex) {

                if (mState != ChannelState.OUTPUT) {

                    return false;
                }

                mResetException = exception;
                mState = ChannelState.RESET;
            }

            getRunner().onReset(mCall);

            return true;
        }
    }

    private class DefaultPublisher implements ResultPublisher<OUTPUT> {

        @Override
        public ResultPublisher<OUTPUT> publish(final OUTPUT result) {

            synchronized (mChannelMutex) {

                if (mState != ChannelState.EXCEPTION) {

                    mOutputQueue.add(result);
                    mChannelMutex.notifyAll();
                }
            }

            return this;
        }

        @Override
        public ResultPublisher<OUTPUT> publish(final OUTPUT... results) {

            synchronized (mChannelMutex) {

                if (results == null) {

                    return this;
                }

                return publish(Arrays.asList(results));
            }
        }

        @Override
        public ResultPublisher<OUTPUT> publish(final Iterable<? extends OUTPUT> results) {

            synchronized (mChannelMutex) {

                if ((results != null) && (mState != ChannelState.EXCEPTION)) {

                    for (final OUTPUT result : results) {

                        mOutputQueue.add(result);
                    }

                    mChannelMutex.notifyAll();
                }
            }

            return this;
        }

        @Override
        public ResultPublisher<OUTPUT> publishException(final Throwable throwable) {

            synchronized (mChannelMutex) {

                if (mState != ChannelState.EXCEPTION) {

                    mInputQueue.clear();

                    mRoutineException = throwable;
                    mState = ChannelState.EXCEPTION;

                    mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));
                    mChannelMutex.notifyAll();
                }
            }

            return this;
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

                if (mState != ChannelState.INPUT) {

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

    private class DelayedInputsCall implements Call {

        private final ArrayList<INPUT> mInputs;

        public DelayedInputsCall(final Iterable<? extends INPUT> inputs) {

            final ArrayList<INPUT> inputList = new ArrayList<INPUT>();

            for (final INPUT input : inputs) {

                inputList.add(input);
            }

            mInputs = inputList;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if (mState != ChannelState.INPUT) {

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

    @Override
    public boolean reset() {

        return reset(null);
    }

    @Override
    public boolean reset(final RuntimeException exception) {

        synchronized (mChannelMutex) {

            if (!isOpen()) {

                return false;
            }

            mInputQueue.clear();

            mResetException = exception;
            mState = ChannelState.RESET;
        }

        getRunner().onReset(mCall);

        return false;
    }
}