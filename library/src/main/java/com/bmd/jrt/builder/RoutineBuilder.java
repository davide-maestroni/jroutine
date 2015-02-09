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
package com.bmd.jrt.builder;

import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routine objects.
 * <p/>
 * A routine has a synchronous and an asynchronous runner associated. The synchronous
 * implementation can only be chosen between queued (the default one) and sequential.<br/>
 * The queued one maintains an internal buffer of executions that are consumed only when the
 * last one completes, thus avoiding overflowing the call stack because of nested calls to other
 * routines.<br/>
 * The sequential one simply runs the executions as soon as they are invoked.<br/>
 * While the latter is less memory and CPU consuming, it might greatly increase the depth of the
 * call stack, and blocks execution of the calling thread during delayed executions.<br/>
 * In both cases the executions are run inside the calling thread.<br/>
 * The default asynchronous runner is shared among all the routines, but a custom one can be set
 * through the builder.
 * <p/>
 * The built routine is based on an invocation implementation specified by a class token.<br/>
 * The invocation instance is created only when needed, by passing the specified arguments to the
 * constructor. Note that the arguments objects should be immutable or, at least, never shared
 * inside and outside the routine in order to avoid concurrency issues.<br/>
 * Additionally, a recycling mechanism is provided so that, when an invocation successfully
 * completes, the instance is retained for future executions. Moreover, the maximum running
 * invocation instances at one time can be limited by calling the specific builder method. When the
 * limit is reached and an additional instance is requires, the call is blocked until one becomes
 * available or the timeout set through the builder elapses.<br/>
 * By default the timeout is set to 0 to avoid unexpected deadlocks.<br/>
 * In case the timeout elapses before an invocation instance becomes available, a
 * {@link com.bmd.jrt.routine.RoutineDeadlockException} will be thrown.
 * <p/>
 * Created by davide on 11/11/14.
 */
public interface RoutineBuilder {

    /**
     * Applies the specified configuration to this builder.<br/>
     * Note that the configuration options not supported by the builder methods will be ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     * @throws java.lang.NullPointerException if the specified configuration is null.
     */
    @Nonnull
    public RoutineBuilder apply(@Nonnull RoutineConfiguration configuration);

    /**
     * Sets the action to be taken if the timeout elapses before a result can be read from the
     * output channel.
     *
     * @param action the action type.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder onReadTimeout(@Nullable TimeoutAction action);

    /**
     * Sets the timeout for an invocation instance to become available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public RoutineBuilder withAvailableTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Sets the timeout for an invocation instance to become available. A null value means that
     * it is up to the framework to chose a default duration.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withAvailableTimeout(@Nullable TimeDuration timeout);

    /**
     * Sets the max number of retained invocation instances. A {@link RoutineConfiguration#DEFAULT}
     * value means that it is up to the framework to chose a default number.
     *
     * @param coreInvocations the max number of instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is negative.
     */
    @Nonnull
    public RoutineBuilder withCoreInvocations(int coreInvocations);

    /**
     * Sets the log instance. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param log the log instance.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withLog(@Nullable Log log);

    /**
     * Sets the log level. A null value means that it is up to the framework to chose a default
     * level.
     *
     * @param level the log level.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withLogLevel(@Nullable LogLevel level);

    /**
     * Sets the max number of concurrently running invocation instances. A
     * {@link RoutineConfiguration#DEFAULT} value means that it is up to the framework to chose a
     * default number.
     *
     * @param maxInvocations the max number of instances.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @Nonnull
    public RoutineBuilder withMaxInvocations(int maxInvocations);

    /**
     * Sets the timeout for an invocation instance to produce a readable result.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the timeout.
     * @param timeUnit the timeout time unit.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the specified timeout is negative.
     * @throws java.lang.NullPointerException     if the specified time unit is null.
     */
    @Nonnull
    public RoutineBuilder withReadTimeout(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Sets the timeout for an invocation instance to produce a readable result. A null value means
     * that it is up to the framework to chose a default duration.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout the timeout.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withReadTimeout(@Nullable TimeDuration timeout);

    /**
     * Sets the asynchronous runner instance. A null value means that it is up to the framework
     * to chose a default instance.
     *
     * @param runner the runner instance.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withRunner(@Nullable Runner runner);

    /**
     * Sets the type of the synchronous runner to be used by the routine. A null value means that it
     * is up to the framework to chose a default order type.
     *
     * @param type the runner type.
     * @return this builder.
     */
    @Nonnull
    public RoutineBuilder withSyncRunner(@Nullable RunnerType type);

    /**
     * Synchronous runner type enumeration.
     */
    public enum RunnerType {

        /**
         * Sequential runner.<br/>
         * The sequential one simply runs the executions as soon as they are invoked.<br/>
         * The executions are run inside the calling thread.
         */
        SEQUENTIAL,
        /**
         * Queued runner.<br/>
         * The queued runner maintains an internal buffer of executions that are consumed only when
         * the last one complete, thus avoiding overflowing the call stack because of nested calls
         * to other routines.<br/>
         * The executions are run inside the calling thread.
         */
        QUEUED
    }

    /**
     * Enumeration indicating the action to take on output channel timeout.
     */
    public enum TimeoutAction {

        /**
         * Deadlock.<br/>
         * If no result is available after the specified timeout, the called method will throw a
         * {@link com.bmd.jrt.channel.ReadDeadlockException}.
         */
        DEADLOCK,
        /**
         * Break execution.<br/>
         * If no result is available after the specified timeout, the called method will stop its
         * execution and exit immediately.
         */
        EXIT,
        /**
         * Abort invocation.<br/>
         * If no result is available after the specified timeout, the invocation will be aborted and
         * the method will immediately exit.
         */
        ABORT
    }
}
