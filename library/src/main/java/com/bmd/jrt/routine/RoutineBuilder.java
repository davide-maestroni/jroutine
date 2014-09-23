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
import com.bmd.jrt.util.ClassToken;

import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;

/**
 * Created by davide on 9/21/14.
 */
public class RoutineBuilder<INPUT, OUTPUT> {

    private static final TimeDuration DEFAULT_AVAIL_TIMEOUT = TimeDuration.seconds(3);

    private TimeDuration mAvailTimeout = DEFAULT_AVAIL_TIMEOUT;

    private static final int DEFAULT_RETAIN_COUNT = 10;

    private int mMaxRetained = DEFAULT_RETAIN_COUNT;

    private final ClassToken<? extends SubRoutine<INPUT, OUTPUT>> mClassToken;

    private Object[] mArgs = NO_ARGS;

    private Runner mAsyncRunner = Runners.shared();

    private int mMaxRunning = Integer.MAX_VALUE;

    private Runner mSyncRunner = Runners.queued();

    RoutineBuilder(final ClassToken<? extends SubRoutine<INPUT, OUTPUT>> classToken) {

        if (classToken == null) {

            throw new IllegalArgumentException();
        }

        mClassToken = classToken;
    }

    public RoutineBuilder<INPUT, OUTPUT> availableTimeout(final long timeout,
            final TimeUnit timeUnit) {

        return availableTimeout(TimeDuration.fromUnit(timeout, timeUnit));
    }

    public RoutineBuilder<INPUT, OUTPUT> availableTimeout(final TimeDuration timeout) {

        if (timeout == null) {

            throw new IllegalArgumentException();
        }

        mAvailTimeout = timeout;

        return this;
    }

    public RoutineBuilder<INPUT, OUTPUT> maxRetained(final int maxRetainedInstances) {

        if (maxRetainedInstances < 1) {

            throw new IllegalArgumentException();
        }

        mMaxRetained = maxRetainedInstances;

        return this;
    }

    public RoutineBuilder<INPUT, OUTPUT> maxRunning(final int maxRunningInstances) {

        if (maxRunningInstances < 1) {

            throw new IllegalArgumentException();
        }

        mMaxRunning = maxRunningInstances;

        return this;
    }

    public RoutineBuilder<INPUT, OUTPUT> queued() {

        mSyncRunner = Runners.queued();

        return this;
    }

    public Routine<INPUT, OUTPUT> routine() {

        return new DefaultRoutine<INPUT, OUTPUT>(mSyncRunner, mAsyncRunner, mMaxRunning,
                                                 mMaxRetained, mAvailTimeout,
                                                 mClassToken.getRawClass(), mArgs);
    }

    public RoutineBuilder<INPUT, OUTPUT> runningOn(final Runner runner) {

        mAsyncRunner = runner;

        return this;
    }

    public RoutineBuilder<INPUT, OUTPUT> sequential() {

        mSyncRunner = Runners.sequential();

        return this;
    }

    public RoutineBuilder<INPUT, OUTPUT> withArgs(final Object... args) {

        if (args == null) {

            throw new IllegalArgumentException();
        }

        mArgs = args;

        return this;
    }
}
