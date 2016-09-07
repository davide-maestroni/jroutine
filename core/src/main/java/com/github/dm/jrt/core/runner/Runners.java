/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.runner;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for creating and sharing runner instances.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
public class Runners {

    private static final ImmediateRunner sImmediateRunner = new ImmediateRunner();

    private static final Object sMutex = new Object();

    private static final QueuedRunner sQueuedRunner = new QueuedRunner();

    private static Runner sSharedRunner;

    /**
     * Avoid explicit instantiation.
     */
    protected Runners() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a runner employing a dynamic pool of threads.
     * <br>
     * The number of threads may increase when needed from the core to the maximum pool size. The
     * number of threads exceeding the core size are kept alive when idle for the specified time.
     * If they stay idle for a longer time they will be destroyed.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime   when the number of threads is greater than the core one, this is the
     *                        maximum time that excess idle threads will wait for new tasks before
     *                        terminating.
     * @param keepAliveUnit   the time unit for the keep alive time.
     * @return the runner instance.
     * @throws java.lang.IllegalArgumentException if one of the following holds:<ul>
     *                                            <li>{@code corePoolSize < 0}</li>
     *                                            <li>{@code maximumPoolSize <= 0}</li>
     *                                            <li>{@code keepAliveTime < 0}</li></ul>
     */
    @NotNull
    public static Runner dynamicPoolRunner(final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
        return scheduledRunner(
                new DynamicScheduledThreadExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                        keepAliveUnit));
    }

    /**
     * Returns the shared instance of an immediate runner.
     * <p>
     * The returned runner will immediately run any passed execution.
     * <p>
     * Be careful when employing the returned runner, since it may lead to recursive calls, thus
     * causing the invocation lifecycle to be not strictly honored. In fact, it might happen, for
     * example, that the abortion method is called in the middle of the execution of another
     * invocation method.
     *
     * @return the runner instance.
     */
    @NotNull
    public static Runner immediateRunner() {
        return sImmediateRunner;
    }

    /**
     * Returns a runner employing an optimum number of threads.
     *
     * @return the runner instance.
     */
    @NotNull
    public static Runner poolRunner() {
        return poolRunner((Runtime.getRuntime().availableProcessors() << 1) - 1);
    }

    /**
     * Returns a runner employing the specified number of threads.
     *
     * @param poolSize the thread pool size.
     * @return the runner instance.
     * @throws java.lang.IllegalArgumentException if the pool size is less than 1.
     */
    @NotNull
    public static Runner poolRunner(final int poolSize) {
        return scheduledRunner(Executors.newScheduledThreadPool(poolSize));
    }

    /**
     * Returns a runner providing ordering of executions based on priority.
     * <p>
     * Note that applying a priority to a synchronous runner might make execution invocations happen
     * in different threads than the calling one, thus causing the results to be not immediately
     * available.
     *
     * @param wrapped the wrapped runner instance.
     * @return the runner instance.
     */
    @NotNull
    public static PriorityRunner priorityRunner(@NotNull final Runner wrapped) {
        return PriorityRunner.getInstance(wrapped);
    }

    /**
     * Returns a runner employing the specified executor service.
     * <p>
     * Be aware that the created runner will not fully comply with the interface contract. Java
     * executor services do not in fact publish the used threads, so that knowing in advance whether
     * a thread belongs to the managed pool is not feasible. This issue actually exposes routines
     * employing the runner to possible deadlocks, in case the specified service is not exclusively
     * accessed by the runner itself.
     * <br>
     * Be then careful when employing runners returned by this method.
     *
     * @param service the executor service.
     * @return the runner instance.
     */
    @NotNull
    public static Runner scheduledRunner(@NotNull final ScheduledExecutorService service) {
        return ScheduledRunner.getInstance(service);
    }

    /**
     * Returns a runner employing the specified executor service.
     * <p>
     * Be aware that the created runner will not fully comply with the interface contract. Java
     * executor services do not in fact publish the used threads, so that knowing in advance whether
     * a thread belongs to the managed pool is not feasible. This issue actually exposes routines
     * employing the runner to possible deadlocks, in case the specified service is not exclusively
     * accessed by the runner itself.
     * <br>
     * Be then careful when employing runners returned by this method.
     *
     * @param service the executor service.
     * @return the runner instance.
     */
    @NotNull
    public static Runner scheduledRunner(@NotNull final ExecutorService service) {
        return scheduledRunner(new ScheduledThreadExecutor(service));
    }

    /**
     * Returns the shared instance of a thread pool asynchronous runner.
     *
     * @return the runner instance.
     */
    @NotNull
    public static Runner sharedRunner() {
        synchronized (sMutex) {
            if (sSharedRunner == null) {
                final int processors = Runtime.getRuntime().availableProcessors();
                sSharedRunner = dynamicPoolRunner(Math.max(2, processors >> 1),
                        Math.max(2, (processors << 2) - 1), 10L, TimeUnit.SECONDS);
            }

            return sSharedRunner;
        }
    }

    /**
     * Returns the shared instance of a synchronous runner.
     * <p>
     * The returned runner maintains an internal buffer of executions that are consumed only when
     * the last one completes, thus avoiding overflowing the call stack because of nested calls to
     * other routines.
     *
     * @return the runner instance.
     */
    @NotNull
    public static Runner syncRunner() {
        return sQueuedRunner;
    }

    /**
     * Returns a runner throttling the number of running executions so to keep it under the
     * specified limit.
     * <p>
     * Note that applying throttling to a synchronous runner might make execution invocations happen
     * in different threads than the calling one, thus causing the results to be not immediately
     * available.
     *
     * @param wrapped       the wrapped instance.
     * @param maxExecutions the maximum number of running executions.
     * @return the runner instance.
     * @throws java.lang.IllegalArgumentException if the specified max number is less than 1.
     */
    @NotNull
    public static Runner throttlingRunner(@NotNull final Runner wrapped, final int maxExecutions) {
        return new ThrottlingRunner(wrapped, maxExecutions);
    }

    /**
     * Returns a runner employing a shared synchronous one when executions are enqueued with a 0
     * delay on one of the managed threads.
     *
     * @param wrapped the wrapped instance.
     * @return the runner instance.
     */
    @NotNull
    public static Runner zeroDelayRunner(@NotNull final Runner wrapped) {
        return ZeroDelayRunner.getInstance(wrapped);
    }
}
