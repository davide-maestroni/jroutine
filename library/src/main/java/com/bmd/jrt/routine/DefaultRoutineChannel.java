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
import com.bmd.jrt.runner.Call;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.PositiveDuration;
import com.bmd.jrt.util.RoutineInterruptedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 9/8/14.
 */
class DefaultRoutineChannel<INPUT, OUTPUT> implements InputChannel<INPUT, OUTPUT> {

    private final Call mCall;

    private final Object mChannelMutex = new Object();

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final HashMap<LoopProcedure<INPUT, OUTPUT>, ReentrantLock> mLockMap =
            new HashMap<LoopProcedure<INPUT, OUTPUT>, ReentrantLock>();

    private final int mMaxInstancePerCall;

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final HashMap<Thread, LoopProcedure<INPUT, OUTPUT>> mProcedureMap =
            new HashMap<Thread, LoopProcedure<INPUT, OUTPUT>>();

    private final Object mProcedureMutex = new Object();

    private final LinkedList<LoopProcedure<INPUT, OUTPUT>> mProcedures =
            new LinkedList<LoopProcedure<INPUT, OUTPUT>>();

    private final ProcedureProvider<INPUT, OUTPUT> mProvider;

    private final Runner mRunner;

    private int mActiveInstanceCount;

    private PositiveDuration mInputDelay = PositiveDuration.ZERO;

    private RuntimeException mOutputException;

    private PositiveDuration mOutputTimeout = PositiveDuration.INFINITE;

    private int mPendingInputCount;

    private Throwable mProcedureException;

    private RuntimeException mResetException;

    private ChannelState mState = ChannelState.INPUT;

    public DefaultRoutineChannel(final ProcedureProvider<INPUT, OUTPUT> provider,
            final Runner runner, final int maxPerCall) {

        if (provider == null) {

            throw new IllegalArgumentException();
        }

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxPerCall < 1) {

            throw new IllegalArgumentException();
        }

        mProvider = provider;
        mRunner = runner;
        mMaxInstancePerCall = maxPerCall;
        mCall = new DefaultCall(new DefaultPublisher());
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

        mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

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

            } else {

                ++mPendingInputCount;
            }
        }

        if (delay.isZero()) {

            mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputsCall(inputs), delay.time, delay.unit);
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

            } else {

                ++mPendingInputCount;
            }
        }

        if (delay.isZero()) {

            mRunner.onInput(mCall, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputCall(input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public boolean isOpen() {

        synchronized (mChannelMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    private void verifyInput() {

        final Throwable throwable = mProcedureException;

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

    public interface ProcedureProvider<INPUT, OUTPUT> {

        public LoopProcedure<INPUT, OUTPUT> create();
    }

    private class DefaultCall implements Call {

        private final ResultPublisher<OUTPUT> mPublisher;

        public DefaultCall(final ResultPublisher<OUTPUT> publisher) {

            if (publisher == null) {

                throw new IllegalArgumentException();
            }

            mPublisher = publisher;
        }

        @Override
        public void onInput() {

            final ResultPublisher<OUTPUT> publisher = mPublisher;
            final LoopProcedure<INPUT, OUTPUT> procedure = acquireProcedure();

            boolean isEnded = false;

            try {

                synchronized (mChannelMutex) {

                    if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                        return;
                    }
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

                    procedure.onInput(input, publisher);
                }

                synchronized (mChannelMutex) {

                    isEnded = (mState == ChannelState.OUTPUT) && (mPendingInputCount <= 0);
                }

                if (isEnded) {

                    procedure.onResult(publisher);

                    synchronized (mChannelMutex) {

                        mState = ChannelState.RESULT;
                    }
                }

            } catch (final Throwable t) {

                publisher.publishException(t);

            } finally {

                releaseProcedure(procedure);

                if (isEnded) {

                    synchronized (mChannelMutex) {

                        mChannelMutex.notifyAll();
                    }
                }
            }
        }

        private LoopProcedure<INPUT, OUTPUT> acquireProcedure() {

            synchronized (mProcedureMutex) {

                final Thread currentThread = Thread.currentThread();

                final HashMap<Thread, LoopProcedure<INPUT, OUTPUT>> procedureMap = mProcedureMap;
                LoopProcedure<INPUT, OUTPUT> procedure = procedureMap.get(currentThread);

                if (procedure == null) {

                    final int maxCount = mMaxInstancePerCall;

                    try {

                        while (mActiveInstanceCount >= maxCount) {

                            mProcedureMutex.wait();
                        }

                    } catch (final InterruptedException e) {

                        Thread.currentThread().interrupt();

                        throw new RoutineInterruptedException(e);
                    }

                    final LinkedList<LoopProcedure<INPUT, OUTPUT>> procedures = mProcedures;

                    if (!procedures.isEmpty()) {

                        procedure = procedures.removeFirst();

                    } else {

                        procedure = mProvider.create();
                    }

                    procedureMap.put(currentThread, procedure);

                    ++mActiveInstanceCount;
                }

                final HashMap<LoopProcedure<INPUT, OUTPUT>, ReentrantLock> lockMap = mLockMap;
                ReentrantLock lock = lockMap.get(procedure);

                if (lock == null) {

                    lock = new ReentrantLock();
                    lockMap.put(procedure, lock);
                }

                lock.lock();

                return procedure;
            }
        }

        private void releaseProcedure(final LoopProcedure<INPUT, OUTPUT> procedure) {

            synchronized (mProcedureMutex) {

                final Thread currentThread = Thread.currentThread();

                final HashMap<Thread, LoopProcedure<INPUT, OUTPUT>> procedureMap = mProcedureMap;

                if (procedureMap.get(currentThread) != procedure) {

                    throw new IllegalStateException();
                }

                final ReentrantLock lock = mLockMap.get(procedure);
                final boolean isLast = (lock.getHoldCount() == 1);

                lock.unlock();

                if (isLast) {

                    procedureMap.remove(currentThread);
                    mProcedures.add(procedure);

                    --mActiveInstanceCount;
                    mProcedureMutex.notify();
                }
            }
        }

        @Override
        public void onReset() {

            final ResultPublisher<OUTPUT> publisher = mPublisher;
            final LoopProcedure<INPUT, OUTPUT> procedure = acquireProcedure();

            try {

                synchronized (mChannelMutex) {

                    if (mState == ChannelState.EXCEPTION) {

                        return;
                    }
                }

                procedure.onReset(publisher);

                synchronized (mChannelMutex) {

                    final RuntimeException resetException = mResetException;

                    if (resetException != null) {

                        publisher.publishException(resetException);
                    }
                }

            } catch (final Throwable t) {

                publisher.publishException(t);

            } finally {

                releaseProcedure(procedure);
            }
        }
    }

    private class DefaultIterator implements Iterator<OUTPUT> {

        private boolean mRemoved = true;

        @Override
        public boolean hasNext() {

            synchronized (mChannelMutex) {

                return !mOutputQueue.isEmpty() || (mState == ChannelState.OUTPUT);
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

                    } while (!outputQueue.isEmpty());

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

                if (timeout.isZero() || (mState != ChannelState.OUTPUT)) {

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

                        if (timeout.waitNanos(mChannelMutex, startNanos) && (mState
                                == ChannelState.OUTPUT)) {

                            isTimeout = true;

                            break;
                        }

                    } while (mState == ChannelState.OUTPUT);

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

            mRunner.onReset(mCall);

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

                    mProcedureException = throwable;
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

                --mPendingInputCount;
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

                --mPendingInputCount;
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

        mRunner.onReset(mCall);

        return false;
    }
}