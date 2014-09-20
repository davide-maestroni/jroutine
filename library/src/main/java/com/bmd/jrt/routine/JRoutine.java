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

import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.util.ClassAdapter;

import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;

/**
 * Created by davide on 9/7/14.
 */
public class JRoutine {

    private static final TimeDuration DEFAULT_AVAIL_TIMEOUT = TimeDuration.seconds(3);

    private static final int DEFAULT_RETAIN_COUNT = 10;

    private final Object[] mArgs;

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailTimeout;

    private final int mMaxRetained;

    private final int mMaxRunning;

    private final Runner mSyncRunner;

    private JRoutine(final Runner syncRunner, final Runner asyncRunner, final int maxRunning,
            final int maxRetained, final TimeDuration availTimeout, final Object[] args) {

        if (syncRunner == null) {

            throw new IllegalArgumentException();
        }

        if (asyncRunner == null) {

            throw new IllegalArgumentException();
        }

        if (maxRunning < 1) {

            throw new IllegalArgumentException();
        }

        if (maxRetained < 1) {

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
        mArgs = args;
    }

    public static JRoutine jrt() {

        return new JRoutine(Runners.queued(), Runners.shared(), Integer.MAX_VALUE,
                            DEFAULT_RETAIN_COUNT, DEFAULT_AVAIL_TIMEOUT, NO_ARGS);
    }

    public JRoutine availableTimeout(final long timeout, final TimeUnit timeUnit) {

        return availableTimeout(TimeDuration.fromUnit(timeout, timeUnit));
    }

    public JRoutine availableTimeout(final TimeDuration timeout) {

        return new JRoutine(Runners.queued(), mAsyncRunner, mMaxRunning, mMaxRetained, timeout,
                            mArgs);
    }

    public JRoutine inside(final Runner runner) {

        return new JRoutine(mSyncRunner, runner, mMaxRunning, mMaxRetained, mAvailTimeout, mArgs);
    }

    public JRoutine maxRetained(final int maxRetainedInstances) {

        return new JRoutine(mSyncRunner, mAsyncRunner, mMaxRunning, maxRetainedInstances,
                            mAvailTimeout, mArgs);
    }

    public JRoutine maxRunning(final int maxRunningInstances) {

        return new JRoutine(mSyncRunner, mAsyncRunner, maxRunningInstances, mMaxRetained,
                            mAvailTimeout, mArgs);
    }

    public JRoutine queued() {

        return new JRoutine(Runners.queued(), mAsyncRunner, mMaxRunning, mMaxRetained,
                            mAvailTimeout, mArgs);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> routineOf(
            final ClassAdapter<? extends SubRoutine<INPUT, OUTPUT>> classAdapter) {

        if (classAdapter == null) {

            throw new IllegalArgumentException();
        }

        return new DefaultRoutine<INPUT, OUTPUT>(mSyncRunner, mAsyncRunner, mMaxRunning,
                                                 mMaxRetained, mAvailTimeout,
                                                 classAdapter.getRawClass(), mArgs);
    }

    public JRoutine sequential() {

        return new JRoutine(Runners.sequential(), mAsyncRunner, mMaxRunning, mMaxRetained,
                            mAvailTimeout, mArgs);
    }

    public JRoutine withArgs(final Object... args) {

        if (args == null) {

            throw new IllegalArgumentException();
        }

        return new JRoutine(mSyncRunner, mAsyncRunner, mMaxRunning, mMaxRetained, mAvailTimeout,
                            args.clone());
    }
}