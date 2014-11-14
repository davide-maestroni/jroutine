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

import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.fromUnit;
import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
public class InvocationRoutineBuilder<INPUT, OUTPUT> implements RoutineBuilder {

    private final Class<? extends Invocation<INPUT, OUTPUT>> mInvocationClass;

    private Object[] mArgs = NO_ARGS;

    private Runner mAsyncRunner = Runners.poolRunner();

    private TimeDuration mAvailTimeout = seconds(5);

    private TimeDuration mInputTimeout = ZERO;

    private Log mLog = Logger.getDefaultLog();

    private LogLevel mLogLevel = Logger.getDefaultLogLevel();

    private int mMaxInputSize = Integer.MAX_VALUE;

    private int mMaxOutputSize = Integer.MAX_VALUE;

    private int mMaxRetained = 10;

    private int mMaxRunning = Integer.MAX_VALUE;

    private boolean mOrderedInput;

    private boolean mOrderedOutput;

    private TimeDuration mOutputTimeout = ZERO;

    private Runner mSyncRunner = Runners.queuedRunner();

    /**
     * Constructor.
     *
     * @param classToken the invocation class token.
     * @throws NullPointerException if the class token is null.
     */
    InvocationRoutineBuilder(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        mInvocationClass = classToken.getRawClass();
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return availableTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> availableTimeout(
            @Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mAvailTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return inputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> inputTimeout(
            @Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the input timeout must not be null");
        }

        mInputTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> logLevel(@Nonnull final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        mLogLevel = level;

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxInputSize(final int maxInputSize) {

        if (maxInputSize <= 0) {

            throw new IllegalArgumentException("the input buffer size cannot be 0 or negative");
        }

        mMaxInputSize = maxInputSize;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxOutputSize(final int maxOutputSize) {

        if (maxOutputSize <= 0) {

            throw new IllegalArgumentException("the output buffer size cannot be 0 or negative");
        }

        mMaxOutputSize = maxOutputSize;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxRetained(final int maxRetainedInstances) {

        if (maxRetainedInstances < 0) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mMaxRetained = maxRetainedInstances;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> maxRunning(final int maxRunningInstances) {

        if (maxRunningInstances < 1) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxRunning = maxRunningInstances;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> orderedInput() {

        mOrderedInput = true;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> orderedOutput() {

        mOrderedOutput = true;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return outputTimeout(fromUnit(timeout, timeUnit));
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> outputTimeout(
            @Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the output timeout must not be null");
        }

        mOutputTimeout = timeout;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> queued() {

        mSyncRunner = Runners.queuedRunner();

        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> runBy(@Nonnull final Runner runner) {

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mAsyncRunner = runner;

        return this;
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> sequential() {

        mSyncRunner = Runners.sequentialRunner();

        return this;
    }

    /**
     * Builds and returns the routine instance.
     *
     * @return the newly created routine.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new DefaultRoutine<INPUT, OUTPUT>(mSyncRunner, mAsyncRunner, mMaxRunning,
                                                 mMaxRetained, mAvailTimeout, mMaxInputSize,
                                                 mInputTimeout, mOrderedInput, mMaxOutputSize,
                                                 mOutputTimeout, mOrderedOutput, mLog, mLogLevel,
                                                 mInvocationClass, mArgs);
    }

    /**
     * Sets the arguments to be passed to the invocation constructor.
     *
     * @param args the arguments.
     * @return this builder.
     * @throws NullPointerException if the specified arguments array is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public InvocationRoutineBuilder<INPUT, OUTPUT> withArgs(@Nonnull final Object... args) {

        if (args == null) {

            throw new NullPointerException("the arguments array must not be null");
        }

        mArgs = args;

        return this;
    }
}
