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
import com.bmd.jrt.runner.Processing;
import com.bmd.jrt.runner.Runner;
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

    private final Object mChannelMutex = new Object();

    private final LinkedList<INPUT> mInputQueue = new LinkedList<INPUT>();

    private final LinkedList<Object> mOutputQueue = new LinkedList<Object>();

    private final RecyclableUnitProcessor<INPUT, OUTPUT> mProcessor;

    private final Object mProcessorMutex = new Object();

    private final Runner mRunner;

    private PositiveDuration mInputDelay = PositiveDuration.ZERO;

    private RuntimeException mOutputException;

    private PositiveDuration mOutputTimeout = PositiveDuration.INFINITE;

    private int mPendingCount;

    private Processing mProcessing;

    private Throwable mProcessorException;

    private RuntimeException mResetException;

    private ChannelState mState = ChannelState.INPUT;

    public DefaultRoutineChannel(final RecyclableUnitProcessor<INPUT, OUTPUT> processor,
            final Runner runner) {

        if (processor == null) {

            throw new IllegalArgumentException();
        }

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        mProcessor = processor;
        mRunner = runner;
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

        mRunner.onInput(mProcessing, 0, TimeUnit.MILLISECONDS);

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

                for (final INPUT input : inputs) {

                    mInputQueue.add(input);
                }

            } else {

                ++mPendingCount;
            }
        }

        if (delay.isZero()) {

            mRunner.onInput(mProcessing, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputsProcessing(inputs), delay.time, delay.unit);
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

                ++mPendingCount;
            }
        }

        if (delay.isZero()) {

            mRunner.onInput(mProcessing, 0, TimeUnit.MILLISECONDS);

        } else {

            mRunner.onInput(new DelayedInputProcessing(input), delay.time, delay.unit);
        }

        return this;
    }

    @Override
    public boolean isOpen() {

        synchronized (mChannelMutex) {

            return (mState == ChannelState.INPUT);
        }
    }

    protected Processing createProcessing() {

        return new DefaultProcessing(new DefaultPublisher());
    }

    private void verifyInput() {

        final Throwable throwable = mProcessorException;

        if (throwable != null) {

            throw RoutineExceptionWrapper.wrap(throwable).raise();
        }

        if (!isOpen()) {

            throw new IllegalStateException();
        }

        if (mProcessing == null) {

            final Processing processing = createProcessing();

            if (processing == null) {

                throw new IllegalStateException();
            }

            mProcessing = processing;
        }
    }

    private static enum ChannelState {

        INPUT,
        OUTPUT,
        RESULT,
        RESET,
        EXCEPTION
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

                    final Object mutex = mChannelMutex;
                    final long startNanos = System.nanoTime();

                    do {

                        if (!timeout.waitNanos(mutex, startNanos) && outputQueue.isEmpty()) {

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

                    final Object mutex = mChannelMutex;
                    final long startNanos = System.nanoTime();

                    do {

                        if (timeout.waitNanos(mutex, startNanos) && (mState
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

            mRunner.onReset(mProcessing);

            return true;
        }
    }

    private class DefaultProcessing implements Processing {

        private final ScopedPublisher<OUTPUT> mPublisher;

        public DefaultProcessing(final ScopedPublisher<OUTPUT> publisher) {

            if (publisher == null) {

                throw new IllegalArgumentException();
            }

            mPublisher = publisher;
        }

        @Override
        public void onInput() {

            final ScopedPublisher<OUTPUT> publisher = mPublisher;
            final RecyclableUnitProcessor<INPUT, OUTPUT> processor = mProcessor;

            boolean isEnded = false;

            synchronized (mProcessorMutex) {

                synchronized (mChannelMutex) {

                    if ((mState == ChannelState.RESET) || (mState == ChannelState.EXCEPTION)) {

                        return;
                    }
                }

                publisher.enter();

                try {

                    INPUT input;

                    while (true) {

                        synchronized (mChannelMutex) {

                            final boolean empty = mInputQueue.isEmpty();

                            if (!empty) {

                                input = mInputQueue.removeFirst();

                            } else {

                                break;
                            }
                        }

                        processor.onInput(input, publisher);
                    }

                    synchronized (mChannelMutex) {

                        isEnded = (mState == ChannelState.OUTPUT) && (mPendingCount <= 0);
                    }

                    if (isEnded) {

                        processor.onResult(publisher);

                        synchronized (mChannelMutex) {

                            mState = ChannelState.RESULT;
                        }

                        processor.recycle();
                    }

                } catch (final Throwable t) {

                    publisher.publishException(t);

                    synchronized (mChannelMutex) {

                        mInputQueue.clear();

                        mProcessorException = t;
                        mState = ChannelState.EXCEPTION;
                    }

                } finally {

                    publisher.exit();

                    if (isEnded) {

                        synchronized (mChannelMutex) {

                            mChannelMutex.notifyAll();
                        }
                    }
                }
            }
        }

        @Override
        public void onReset() {

            final ScopedPublisher<OUTPUT> publisher = mPublisher;
            final RecyclableUnitProcessor<INPUT, OUTPUT> processor = mProcessor;

            synchronized (mProcessorMutex) {

                synchronized (mChannelMutex) {

                    if (mState == ChannelState.EXCEPTION) {

                        return;
                    }
                }

                publisher.enter();

                try {

                    processor.onReset(publisher);
                    processor.recycle();

                    synchronized (mChannelMutex) {

                        if (mResetException != null) {

                            publisher.publishException(mOutputException);
                        }
                    }

                } catch (final Throwable t) {

                    publisher.publishException(t);

                    synchronized (mChannelMutex) {

                        mInputQueue.clear();

                        mProcessorException = t;
                        mState = ChannelState.EXCEPTION;
                    }

                } finally {

                    publisher.exit();
                }
            }
        }
    }

    private class DefaultPublisher implements ScopedPublisher<OUTPUT> {

        private boolean mIsOpen = false;

        @Override
        public void enter() {

            synchronized (mChannelMutex) {

                if (mIsOpen) {

                    throw new IllegalStateException();
                }

                mIsOpen = true;
            }
        }

        @Override
        public void exit() {

            synchronized (mChannelMutex) {

                mIsOpen = false;
            }
        }

        @Override
        public ResultPublisher<OUTPUT> publish(final OUTPUT result) {

            synchronized (mChannelMutex) {

                verifyOpen();

                mOutputQueue.add(result);
                mChannelMutex.notifyAll();
            }

            return this;
        }

        @Override
        public ResultPublisher<OUTPUT> publish(final OUTPUT... results) {

            synchronized (mChannelMutex) {

                verifyOpen();

                if (results == null) {

                    return this;
                }

                return publish(Arrays.asList(results));
            }
        }

        @Override
        public ResultPublisher<OUTPUT> publish(final Iterable<? extends OUTPUT> results) {

            synchronized (mChannelMutex) {

                verifyOpen();

                if (results != null) {

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

                verifyOpen();

                mOutputQueue.add(RoutineExceptionWrapper.wrap(throwable));
                mChannelMutex.notifyAll();
            }

            return this;
        }

        private void verifyOpen() {

            if (!mIsOpen) {

                throw new IllegalStateException();
            }
        }
    }

    private class DelayedInputProcessing implements Processing {

        private final INPUT mInput;

        public DelayedInputProcessing(final INPUT input) {

            mInput = input;
        }

        @Override
        public void onInput() {

            synchronized (mChannelMutex) {

                if (mState != ChannelState.INPUT) {

                    return;
                }

                --mPendingCount;

                mInputQueue.add(mInput);
            }

            mProcessing.onInput();
        }

        @Override
        public void onReset() {

            mProcessing.onReset();
        }
    }

    private class DelayedInputsProcessing implements Processing {

        private final ArrayList<INPUT> mInputs;

        public DelayedInputsProcessing(final Iterable<? extends INPUT> inputs) {

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

                --mPendingCount;

                mInputQueue.addAll(mInputs);
            }

            mProcessing.onInput();
        }

        @Override
        public void onReset() {

            mProcessing.onReset();
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

        mRunner.onReset(mProcessing);

        return false;
    }
}