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
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * The runner class defines an object responsible for executing routine invocations inside
 * specifically managed threads.
 * <p>
 * The implementation can both be synchronous or asynchronous, it can allocate specialized threads
 * or share a pool of them between different instances.
 * <br>
 * The only requirement is that the specified execution is called each time a run method is invoked,
 * even if the same execution instance is passed several times as input parameter.
 * <p>
 * Note that, a proper asynchronous runner implementation will never synchronously run an execution,
 * unless the run method is called inside one of the managed thread. While, a proper synchronous
 * runner, will always run executions on the very same caller thread.
 * <br>
 * Note also that the runner methods might be called from different threads, so, it is up to the
 * implementing class to ensure synchronization when required.
 * <p>
 * The implementing class can optionally support the cancellation of executions not yet run
 * (waiting, for example, in a consuming queue).
 * <p>
 * The class {@link com.github.dm.jrt.core.runner.Runners Runners} provides a few implementations
 * employing concurrent Java classes.
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 */
public abstract class Runner {

  private static final Object sMutex = new Object();

  private static volatile WeakIdentityHashMap<ThreadManager, Void> sManagers =
      new WeakIdentityHashMap<ThreadManager, Void>();

  private final ThreadManager mManager;

  /**
   * Constructor.
   *
   * @param manager the manager of threads.
   */
  protected Runner(@NotNull final ThreadManager manager) {
    registerManager(ConstantConditions.notNull("thread manager", manager));
    mManager = manager;
  }

  /**
   * Checks if the calling thread belongs to the ones managed by a runner.
   *
   * @return whether the calling thread is managed by a runner.
   */
  public static boolean isManagedThread() {
    for (final ThreadManager manager : sManagers.keySet()) {
      if ((manager != null) && manager.isManagedThread()) {
        return true;
      }
    }

    return false;
  }

  private static void registerManager(@NotNull final ThreadManager manager) {
    synchronized (sMutex) {
      // Copy-on-write pattern
      sManagers = new WeakIdentityHashMap<ThreadManager, Void>(sManagers) {{
        put(manager, null);
      }};
    }
  }

  /**
   * Cancels the specified execution if not already run.
   * <p>
   * Note that the method will have no effect in case the runner does not maintain a queue or the
   * specified execution has been already processed at the moment of the call.
   * <br>
   * Note also that, in case the same execution has been added more than once to the runner queue,
   * when the method returns, the queue will not contain the execution instance anymore, with the
   * consequence that the {@link Execution#run()} method will never be called.
   * <p>
   * The implementation of this method is optional, still, it may greatly increase the performance
   * by avoiding to start invocations which are already aborted.
   *
   * @param execution the execution.
   */
  public abstract void cancel(@NotNull Execution execution);

  /**
   * Checks if the calling thread may be employed to run executions.
   * <p>
   * The implementation of this method is not strictly mandatory, even if, the classes always
   * returning false effectively prevent the correct detection of possible deadlocks.
   * <br>
   * A synchronous runner implementation will always return true.
   *
   * @return whether the calling thread is employed by the runner.
   */
  public abstract boolean isExecutionThread();

  /**
   * Checks if this runner instance is synchronous, that is, all the executions are run in the
   * calling thread.
   * <p>
   * Note that, even if the implementation of this method is not strictly mandatory, it will be
   * used to optimize the invocation executions.
   * <p>
   * Consider inheriting from {@link com.github.dm.jrt.core.runner.AsyncRunner AsyncRunner} or
   * {@link com.github.dm.jrt.core.runner.SyncRunner SyncRunner} class for a default
   * implementation of most of the abstract methods.
   *
   * @return whether this runner is synchronous.
   */
  public abstract boolean isSynchronous();

  /**
   * Runs the specified execution (that is, it calls the {@link Execution#run()} method inside the
   * runner thread).
   *
   * @param execution the execution.
   * @param delay     the execution delay.
   * @param timeUnit  the delay time unit.
   * @throws java.lang.IllegalStateException it the runner is currently unable to fulfill the
   *                                         execution (for instance, after being stopped).
   */
  public abstract void run(@NotNull Execution execution, long delay, @NotNull TimeUnit timeUnit);

  /**
   * Stops the runner.
   * <br>
   * The method is meant to signal that the runner is no more needed. In fact, as a consequence of
   * the call, the runner itself may become unusable.
   * <br>
   * The specific implementation can leverage the method to eventually free allocated resources.
   */
  public abstract void stop();

  /**
   * Returns this runner thread manager.
   *
   * @return the thread manager.
   */
  @NotNull
  protected ThreadManager getThreadManager() {
    return mManager;
  }

  /**
   * Interface defining a manager of the runner threads.
   */
  protected interface ThreadManager {

    /**
     * Checks if the calling thread belongs to the ones managed by the runner implementation.
     * <p>
     * The implementation of this method is not strictly mandatory, even if, the classes always
     * returning false effectively prevent the correct detection of possible deadlocks.
     * <br>
     * A synchronous runner implementation will always return false.
     *
     * @return whether the thread is managed by the runner.
     */
    boolean isManagedThread();
  }
}
