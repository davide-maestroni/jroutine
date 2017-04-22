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
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * The executor class defines an object responsible for executing routine invocations inside
 * specifically managed threads.
 * <p>
 * The implementation can both be synchronous or asynchronous, it can allocate specialized threads
 * or share a pool of them between different instances.
 * <br>
 * The only requirement is that the specified command is called each time a run method is invoked,
 * even if the same command instance is passed several times as input parameter.
 * <p>
 * Note that, a proper asynchronous executor implementation will never synchronously run a command,
 * unless the run method is called inside one of the managed thread. While, a proper synchronous
 * executor, will always run commands on the very same caller thread.
 * <br>
 * Note also that the executor methods might be called from different threads, so, it is up to the
 * implementing class to ensure synchronization when required.
 * <p>
 * The implementing class can optionally support the cancellation of commands not yet run
 * (waiting, for example, in a consuming queue).
 * <p>
 * The class {@link com.github.dm.jrt.core.executor.ScheduledExecutors ScheduledExecutor} provides a
 * few implementations employing concurrent Java classes.
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 */
public abstract class ScheduledExecutor implements Executor {

  private static final Object sMutex = new Object();

  private static volatile WeakIdentityHashMap<ThreadManager, Void> sManagers =
      new WeakIdentityHashMap<ThreadManager, Void>();

  private final ThreadManager mManager;

  /**
   * Constructor.
   *
   * @param manager the manager of threads.
   */
  protected ScheduledExecutor(@NotNull final ThreadManager manager) {
    registerManager(ConstantConditions.notNull("thread manager", manager));
    mManager = manager;
  }

  /**
   * Checks if the calling thread belongs to the ones managed by an executor.
   *
   * @return whether the calling thread is managed by an executor.
   */
  public static boolean isManagedThread() {
    for (final ThreadManager manager : sManagers.keySet()) {
      if ((manager != null) && manager.isManagedThread()) {
        return true;
      }
    }

    return false;
  }

  // TODO: 21/04/2017 remove
  public static ThreadManager getManager() {
    for (final ThreadManager manager : sManagers.keySet()) {
      if ((manager != null) && manager.isManagedThread()) {
        return manager;
      }
    }

    return null;
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
   * Cancels the specified command if not already run.
   * <p>
   * Note that the method will have no effect in case the executor does not maintain a queue or the
   * specified command has been already processed at the moment of the call.
   * <br>
   * Note also that, in case the same command has been added more than once to the executor queue,
   * when the method returns, the queue will not contain the command instance anymore, with the
   * consequence that the {@link Runnable#run()} method will never be called.
   * <p>
   * The implementation of this method is optional, still, it may greatly increase the performance
   * by avoiding to start invocations which are already aborted.
   *
   * @param command the command.
   */
  public abstract void cancel(@NotNull Runnable command);

  public void execute(@NotNull final Runnable command) {
    execute(command, 0, TimeUnit.MILLISECONDS);
  }

  /**
   * Executes the specified command (that is, it calls the {@link Runnable#run()} method inside
   * the executor thread) after the specified delay.
   *
   * @param command  the command.
   * @param delay    the command delay.
   * @param timeUnit the delay time unit.
   * @throws java.util.concurrent.RejectedExecutionException it the executor is currently unable to
   *                                                         fulfill the command (for instance,
   *                                                         after being stopped).
   */
  public abstract void execute(@NotNull Runnable command, long delay, @NotNull TimeUnit timeUnit);

  /**
   * Checks if the calling thread may be employed to run commands.
   * <p>
   * The implementation of this method is not strictly mandatory, even if, the classes always
   * returning false effectively prevent the correct detection of possible deadlocks.
   * <br>
   * A synchronous executor implementation will always return true.
   *
   * @return whether the calling thread is employed by the executor.
   */
  public abstract boolean isExecutionThread();

  /**
   * Checks if this executor instance is synchronous, that is, all the commands are run in the
   * calling thread.
   * <p>
   * Note that, even if the implementation of this method is not strictly mandatory, it will be
   * used to optimize the invocation commands.
   * <p>
   * Consider inheriting from {@link com.github.dm.jrt.core.executor.AsyncExecutor AsyncExecutor} or
   * {@link com.github.dm.jrt.core.executor.SyncExecutor SyncExecutor} class for a default
   * implementation of most of the abstract methods.
   *
   * @return whether this executor is synchronous.
   */
  public abstract boolean isSynchronous();

  /**
   * Stops the executor.
   * <br>
   * The method is meant to signal that the executor is no more needed. In fact, as a consequence of
   * the call, pending commands might get discarded and the executor itself may become unusable.
   * <br>
   * The specific implementation can leverage the method to eventually free allocated resources.
   */
  public abstract void stop();

  /**
   * Returns this executor thread manager.
   *
   * @return the thread manager.
   */
  @NotNull
  protected ThreadManager getThreadManager() {
    return mManager;
  }

  /**
   * Interface defining a manager of the executor threads.
   */
  protected interface ThreadManager {

    /**
     * Checks if the calling thread belongs to the ones managed by the executor implementation.
     * <p>
     * The implementation of this method is not strictly mandatory, even if, the classes always
     * returning false effectively prevent the correct detection of possible deadlocks.
     * <br>
     * A synchronous executor implementation will always return false.
     *
     * @return whether the thread is managed by the executor.
     */
    boolean isManagedThread();
  }
}
