/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.core.executor;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for creating and sharing executor instances.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
@SuppressWarnings("WeakerAccess")
public class ScheduledExecutors {

  private static final ImmediateExecutor sImmediateExecutor = new ImmediateExecutor();

  private static final Object sMutex = new Object();

  private static final QueuedExecutor sQueuedExecutor = new QueuedExecutor();

  private static ScheduledExecutor sDefaultExecutor;

  /**
   * Avoid explicit instantiation.
   */
  protected ScheduledExecutors() {
    ConstantConditions.avoid();
  }

  /**
   * Returns the default instance of a thread pool asynchronous executor.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor defaultExecutor() {
    synchronized (sMutex) {
      if (sDefaultExecutor == null) {
        final int processors = Runtime.getRuntime().availableProcessors();
        sDefaultExecutor = ServiceExecutor.of(
            new com.github.dm.jrt.core.executor.DynamicScheduledThreadExecutor(
                Math.max(2, processors >> 1), Math.max(2, (processors << 2) - 1), 10L,
                TimeUnit.SECONDS));
      }

      return sDefaultExecutor;
    }
  }

  /**
   * Returns an executor employing a dynamic pool of threads.
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
   * @return the executor instance.
   * @throws java.lang.IllegalArgumentException if one of the following holds:<ul>
   *                                            <li>{@code corePoolSize < 0}</li>
   *                                            <li>{@code maximumPoolSize <= 0}</li>
   *                                            <li>{@code keepAliveTime < 0}</li></ul>
   */
  @NotNull
  public static ScheduledExecutor dynamicPoolExecutor(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
    return ServiceExecutor.ofStoppable(
        new DynamicScheduledThreadExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
            keepAliveUnit));
  }

  /**
   * Returns the shared instance of an immediate executor.
   * <p>
   * The returned executor will immediately run any passed command.
   * <p>
   * Be careful when employing the returned executor, since it may lead to recursive calls, thus
   * causing the invocation lifecycle to be not strictly honored. In fact, it might happen, for
   * example, that the abortion method is called in the middle of the execution of another
   * invocation method.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor immediateExecutor() {
    return sImmediateExecutor;
  }

  /**
   * Returns an executor employing an optimum number of threads.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor poolExecutor() {
    return poolExecutor((Runtime.getRuntime().availableProcessors() << 1) - 1);
  }

  /**
   * Returns an executor employing the specified number of threads.
   *
   * @param poolSize the thread pool size.
   * @return the executor instance.
   * @throws java.lang.IllegalArgumentException if the pool size is less than 1.
   */
  @NotNull
  public static ScheduledExecutor poolExecutor(final int poolSize) {
    return ServiceExecutor.ofStoppable(Executors.newScheduledThreadPool(poolSize));
  }

  /**
   * Returns an executor providing ordering of executions based on priority.
   * <p>
   * Note that applying a priority to a synchronous executor might make command invocations happen
   * in different threads than the calling one, thus causing the results to be not immediately
   * available.
   *
   * @param wrapped the wrapped executor instance.
   * @return the executor instance.
   */
  @NotNull
  public static PriorityExecutor priorityExecutor(@NotNull final ScheduledExecutor wrapped) {
    return PriorityExecutor.of(wrapped);
  }

  /**
   * Returns an executor employing the specified executor service.
   * <p>
   * Be aware that the created executor will not fully comply with the interface contract. Java
   * executor services do not in fact publish the used threads, so that knowing in advance whether
   * a thread belongs to the managed pool is not feasible. This issue actually exposes routines
   * employing the executor to possible deadlocks, in case the specified service is not exclusively
   * accessed by the executor itself.
   * <br>
   * Be then careful when employing executors returned by this method.
   *
   * @param service the executor service.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor serviceExecutor(@NotNull final ScheduledExecutorService service) {
    return ServiceExecutor.of(service);
  }

  /**
   * Returns an executor employing the specified executor service.
   * <p>
   * Be aware that the created executor will not fully comply with the interface contract. Java
   * executor services do not in fact publish the used threads, so that knowing in advance whether
   * a thread belongs to the managed pool is not feasible. This issue actually exposes routines
   * employing the executor to possible deadlocks, in case the specified service is not exclusively
   * accessed by the executor itself.
   * <br>
   * Be then careful when employing executors returned by this method.
   *
   * @param service the executor service.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor serviceExecutor(@NotNull final ExecutorService service) {
    return serviceExecutor(new ScheduledThreadExecutor(service));
  }

  /**
   * Returns the shared instance of a synchronous executor.
   * <p>
   * The returned executor maintains an internal buffer of executions that are consumed only when
   * the last one completes, thus avoiding overflowing the call stack because of nested calls to
   * other routines.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor syncExecutor() {
    return sQueuedExecutor;
  }

  /**
   * Returns an executor throttling the number of running executions so to keep it under the
   * specified limit.
   * <p>
   * Note that applying throttling to a synchronous executor might make command invocations happen
   * in different threads than the calling one, thus causing the results to be not immediately
   * available.
   *
   * @param wrapped       the wrapped instance.
   * @param maxExecutions the maximum number of running executions.
   * @return the executor instance.
   * @throws java.lang.IllegalArgumentException if the specified max number is less than 1.
   */
  @NotNull
  public static ScheduledExecutor throttlingExecutor(@NotNull final ScheduledExecutor wrapped,
      final int maxExecutions) {
    return new ThrottlingExecutor(wrapped, maxExecutions);
  }

  /**
   * Returns an executor employing a shared synchronous one when executions are enqueued with a 0
   * delay on one of the managed threads.
   *
   * @param wrapped the wrapped instance.
   * @return the executor instance.
   */
  @NotNull
  public static ScheduledExecutor zeroDelayExecutor(@NotNull final ScheduledExecutor wrapped) {
    return ZeroDelayExecutor.of(wrapped);
  }
}
