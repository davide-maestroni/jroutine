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
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.invocation.RoutineInvocation;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;
import com.bmd.jrt.util.RoutineInterruptedException;
import com.bmd.jrt.util.RoutineNotAvailableException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public abstract class AbstractRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailTimeout;

    private final int mMaxRetained;

    private final int mMaxRunning;

    private final Object mMutex = new Object();

    private final Runner mSyncRunner;

    private LinkedList<RoutineInvocation<INPUT, OUTPUT>> mRoutineInvocations =
            new LinkedList<RoutineInvocation<INPUT, OUTPUT>>();

    private int mRunningCount;

    public AbstractRoutine(final Runner syncRunner, final Runner asyncRunner, final int maxRunning,
            final int maxRetained, final TimeDuration availTimeout) {

        if (syncRunner == null) {

            throw new IllegalArgumentException();
        }

        if (asyncRunner == null) {

            throw new IllegalArgumentException();
        }

        if (maxRunning < 1) {

            throw new IllegalArgumentException();
        }

        if (maxRetained < 0) {

            throw new IllegalArgumentException();
        }

        if (availTimeout == null) {

            throw new IllegalArgumentException();
        }

        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxRunning = maxRunning;
        mMaxRetained = maxRetained;
        mAvailTimeout = availTimeout;
    }

    @Override
    public List<OUTPUT> call() {

        return invoke().readAll();
    }

    @Override
    public List<OUTPUT> call(final INPUT input) {

        return invoke(input).readAll();
    }

    @Override
    public List<OUTPUT> call(final INPUT... inputs) {

        return invoke(inputs).readAll();
    }

    @Override
    public List<OUTPUT> call(final Iterable<? extends INPUT> inputs) {

        return invoke(inputs).readAll();
    }

    @Override
    public List<OUTPUT> call(final OutputChannel<? extends INPUT> inputs) {

        return invoke(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callAsyn() {

        return invokeAsyn().readAll();
    }

    @Override
    public List<OUTPUT> callAsyn(final INPUT input) {

        return invokeAsyn(input).readAll();
    }

    @Override
    public List<OUTPUT> callAsyn(final INPUT... inputs) {

        return invokeAsyn(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callAsyn(final Iterable<? extends INPUT> inputs) {

        return invokeAsyn(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callAsyn(final OutputChannel<? extends INPUT> inputs) {

        return invokeAsyn(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callParall() {

        return invokeParall().readAll();
    }

    @Override
    public List<OUTPUT> callParall(final INPUT input) {

        return invokeParall(input).readAll();
    }

    @Override
    public List<OUTPUT> callParall(final INPUT... inputs) {

        return invokeParall(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callParall(final Iterable<? extends INPUT> inputs) {

        return invokeParall(inputs).readAll();
    }

    @Override
    public List<OUTPUT> callParall(final OutputChannel<? extends INPUT> inputs) {

        return invokeParall(inputs).readAll();
    }

    @Override
    public OutputChannel<OUTPUT> invoke() {

        return launch().close();
    }

    @Override
    public OutputChannel<OUTPUT> invoke(final INPUT input) {

        return launch().pass(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> invoke(final INPUT... inputs) {

        return launch().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invoke(final Iterable<? extends INPUT> inputs) {

        return launch().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invoke(final OutputChannel<? extends INPUT> inputs) {

        return launch().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeAsyn() {

        return launchAsyn().close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeAsyn(final INPUT input) {

        return launchAsyn().pass(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeAsyn(final INPUT... inputs) {

        return launchAsyn().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeAsyn(final Iterable<? extends INPUT> inputs) {

        return launchAsyn().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeAsyn(final OutputChannel<? extends INPUT> inputs) {

        return launchAsyn().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeParall() {

        return launchParall().close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeParall(final INPUT input) {

        return launchParall().pass(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeParall(final INPUT... inputs) {

        return launchParall().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeParall(final Iterable<? extends INPUT> inputs) {

        return launchParall().pass(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> invokeParall(final OutputChannel<? extends INPUT> inputs) {

        return launchParall().pass(inputs).close();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launch() {

        return launch(false);
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launchAsyn() {

        return launch(true);
    }

    protected abstract RoutineInvocation<INPUT, OUTPUT> createRoutineInvocation(
            final boolean async);

    protected Runner getAsyncRunner() {

        return mAsyncRunner;
    }

    protected TimeDuration getAvailTimeout() {

        return mAvailTimeout;
    }

    protected int getMaxRetained() {

        return mMaxRetained;
    }

    protected int getMaxRunning() {

        return mMaxRunning;
    }

    protected Runner getSyncRunner() {

        return mSyncRunner;
    }

    protected RoutineChannel<INPUT, OUTPUT> launch(final boolean async) {

        final InvocationHandler<INPUT, OUTPUT> invocationHandler =
                new InvocationHandler<INPUT, OUTPUT>(new DefaultRoutineInvocationProvider(async),
                                                     (async) ? mAsyncRunner : mSyncRunner);

        return new DefaultRoutineChannel<INPUT, OUTPUT>(invocationHandler);
    }

    private class DefaultRoutineInvocationProvider
            implements RoutineInvocationProvider<INPUT, OUTPUT> {

        private final boolean mAsync;

        private DefaultRoutineInvocationProvider(final boolean async) {

            mAsync = async;
        }

        @Override
        public RoutineInvocation<INPUT, OUTPUT> create() {

            synchronized (mMutex) {

                boolean isTimeout = false;

                try {

                    final int maxRunning = mMaxRunning;

                    isTimeout = !mAvailTimeout.waitTrue(mMutex, new Check() {

                        @Override
                        public boolean isTrue() {

                            return mRunningCount < maxRunning;
                        }
                    });

                } catch (final InterruptedException e) {

                    RoutineInterruptedException.interrupt(e);
                }

                if (isTimeout) {

                    throw new RoutineNotAvailableException();
                }

                ++mRunningCount;

                final LinkedList<RoutineInvocation<INPUT, OUTPUT>> routines = mRoutineInvocations;

                if (!routines.isEmpty()) {

                    return routines.removeFirst();
                }

                return createRoutineInvocation(mAsync);
            }
        }

        @Override
        public void discard(final RoutineInvocation<INPUT, OUTPUT> invocation) {

            synchronized (mMutex) {

                --mRunningCount;
                mMutex.notify();
            }
        }

        @Override
        public void recycle(final RoutineInvocation<INPUT, OUTPUT> invocation) {

            synchronized (mMutex) {

                final LinkedList<RoutineInvocation<INPUT, OUTPUT>> invocations =
                        mRoutineInvocations;

                if (invocations.size() < mMaxRetained) {

                    invocations.add(invocation);
                }

                --mRunningCount;
                mMutex.notify();
            }
        }
    }
}