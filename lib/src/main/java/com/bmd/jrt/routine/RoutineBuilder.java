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
import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.bmd.jrt.routine.ReflectionUtils.NO_ARGS;
import static com.bmd.jrt.time.TimeDuration.fromUnit;
import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Class implementing a builder of a routine object.
 * <p/>
 * A routine has a synchronous and an asynchronous runner associated. The synchronous
 * implementation can only be chosen between queued (the default one) and sequential.<br/>
 * The queued one maintains an internal buffer of invocations that are consumed only when the
 * last one completes, thus avoiding overflowing the call stack because of nested calls to other
 * routines.<br/>
 * The sequential one simply executes the invocations as soon as they are run.<br/>
 * While the latter is less memory and CPU consuming, it might greatly increase the depth of the
 * call stack, and blocks execution of the calling thread during delayed invocations.<br/>
 * In both cases the invocations are run inside the calling thread.<br/>
 * The default asynchronous runner is shared among all the routines, but a custom one can be set
 * through the builder.
 * <p/>
 * The built routine is based on an execution implementation specified by a class token.<br/>
 * The execution instance is created only when needed, by passing the specified arguments to the
 * constructor. Note that the arguments objects should be immutable or, at least, never shared
 * inside and outside the routine in order to avoid concurrency issues.<br/>
 * Additionally, a recycling mechanism is provided so that, when an execution successfully
 * completes, the instance is retained for future invocations. Moreover, the maximum running
 * execution instances at one time can be limited by calling the specific builder method. When the
 * limit is reached and an additional instance is needed, the call is blocked until one become
 * available or the timeout set through the builder elapses.<br/>
 * By default the timeout is set to a few seconds to avoid unexpected deadlocks.<br/>
 * In case the timeout elapses before an execution instance becomes available, a
 * {@link RoutineNotAvailableException} will be thrown.
 * <p/>
 * Finally, by default the order of input and output data is not guaranteed unless delay is set to
 * 0 and the sources are synchronous, that is, no output channel is passed. Nevertheless, it is
 * possible to force data to be delivered in insertion order, at the cost of a slightly increased
 * memory usage and computation, by calling the proper methods.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 * @see com.bmd.jrt.runner.Runner
 */
public class RoutineBuilder<INPUT, OUTPUT> {

    private final Class<? extends Execution<INPUT, OUTPUT>> mExecutionClass;

    private Object[] mArgs = NO_ARGS;

    private Runner mAsyncRunner = Runners.pool();

    private TimeDuration mAvailTimeout = seconds(5);

    private Log mLog = Logger.getDefaultLog();

    private LogLevel mLogLevel = Logger.getDefaultLogLevel();

    private int mMaxRetained = 10;

    private int mMaxRunning = Integer.MAX_VALUE;

    private boolean mOrderedInput;

    private boolean mOrderedOutput;

    private Runner mSyncRunner = Runners.queued();

    /**
     * Constructor.
     *
     * @param classToken the execution class token.
     * @throws NullPointerException if the class token is null.
     */
    RoutineBuilder(@Nonnull final ClassToken<? extends Execution<INPUT, OUTPUT>> classToken) {

        mExecutionClass = classToken.getRawClass();
    }

    /**
     * Sets the timeout for an execution instance to become available.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws IllegalArgumentException if the specified timeout is negative.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> availableTimeout(final long timeout,
            @Nonnull final TimeUnit timeUnit) {

        return availableTimeout(fromUnit(timeout, timeUnit));
    }

    /**
     * Sets the timeout for an execution instance to become available.
     *
     * @param timeout the timeout.
     * @return this builder.
     * @throws NullPointerException if the specified timeout is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> availableTimeout(@Nonnull final TimeDuration timeout) {

        if (timeout == null) {

            throw new NullPointerException("the timeout must not be null");
        }

        mAvailTimeout = timeout;

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
                                                 mMaxRetained, mAvailTimeout, mOrderedInput,
                                                 mOrderedOutput, mLog, mLogLevel, mExecutionClass,
                                                 mArgs);
    }

    /**
     * Sets the log level.
     *
     * @param level the log level.
     * @return this builder.
     * @throws NullPointerException if the log level is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> logLevel(@Nonnull final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        mLogLevel = level;

        return this;
    }

    /**
     * Sets the log instance.
     *
     * @param log the log instance.
     * @return this builder.
     * @throws NullPointerException if the log is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> loggedWith(@Nonnull final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;

        return this;
    }

    /**
     * Sets the max number of retained instances.
     *
     * @param maxRetainedInstances the max number of instances.
     * @return this builder.
     * @throws IllegalArgumentException if the number is negative.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> maxRetained(final int maxRetainedInstances) {

        if (maxRetainedInstances < 0) {

            throw new IllegalArgumentException(
                    "the maximum number of retained instances cannot be negative");
        }

        mMaxRetained = maxRetainedInstances;

        return this;
    }

    /**
     * Sets the max number of concurrently running instances.
     *
     * @param maxRunningInstances the max number of instances.
     * @return this builder.
     * @throws IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> maxRunning(final int maxRunningInstances) {

        if (maxRunningInstances < 1) {

            throw new IllegalArgumentException(
                    "the maximum number of concurrently running instances cannot be less than 1");
        }

        mMaxRunning = maxRunningInstances;

        return this;
    }

    /**
     * Forces the inputs to be ordered as they are passed to the input channel, independently from
     * the source or the input delay.
     *
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> orderedInput() {

        mOrderedInput = true;

        return this;
    }

    /**
     * Forces the outputs to be ordered as they are passed to the result channel, independently
     * from the source or the result delay.
     *
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> orderedOutput() {

        mOrderedOutput = true;

        return this;
    }

    /**
     * Sets the synchronous runner to the queued one.<br/>
     * The queued runner maintains an internal buffer of invocations that are consumed only when
     * the last one complete, thus avoiding overflowing the call stack because of nested calls to
     * other routines.<br/>
     * The invocations are run inside the calling thread.
     *
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> queued() {

        mSyncRunner = Runners.queued();

        return this;
    }

    /**
     * Sets the asynchronous runner instance.
     *
     * @param runner the runner instance.
     * @return this builder.
     * @throws NullPointerException if the specified runner is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> runBy(@Nonnull final Runner runner) {

        if (runner == null) {

            throw new NullPointerException("the runner instance must not be null");
        }

        mAsyncRunner = runner;

        return this;
    }

    /**
     * Sets the synchronous runner to the sequential one.<br/>
     * The sequential one simply executes the invocations as soon as they are run.<br/>
     * The invocations are run inside the calling thread.
     *
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder<INPUT, OUTPUT> sequential() {

        mSyncRunner = Runners.sequential();

        return this;
    }

    /**
     * Sets the arguments to be passed to the execution constructor.
     *
     * @param args the arguments.
     * @return this builder.
     * @throws NullPointerException if the specified arguments array is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public RoutineBuilder<INPUT, OUTPUT> withArgs(@Nonnull final Object... args) {

        if (args == null) {

            throw new NullPointerException("the arguments array must not be null");
        }

        mArgs = args;

        return this;
    }
}