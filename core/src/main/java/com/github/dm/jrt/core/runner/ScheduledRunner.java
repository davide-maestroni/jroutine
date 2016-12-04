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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class implementing a runner employing an executor service.
 * <p>
 * Created by davide-maestroni on 10/14/2014.
 */
class ScheduledRunner extends AsyncRunner {

  private static final WeakHashMap<ScheduledExecutorService, WeakReference<ScheduledRunner>>
      sRunners = new WeakHashMap<ScheduledExecutorService, WeakReference<ScheduledRunner>>();

  private final WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>> mFutures =
      new WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>>();

  private final ScheduledExecutorService mService;

  /**
   * Constructor.
   *
   * @param service the executor service.
   */
  private ScheduledRunner(@NotNull final ScheduledExecutorService service) {
    super(new ScheduledThreadManager());
    mService = ConstantConditions.notNull("executor service", service);
  }

  /**
   * Returns a runner instance employing the specified service.
   *
   * @param service the executor service.
   * @return the runner.
   */
  @NotNull
  static ScheduledRunner getInstance(@NotNull final ScheduledExecutorService service) {
    ScheduledRunner scheduledRunner;
    synchronized (sRunners) {
      final WeakHashMap<ScheduledExecutorService, WeakReference<ScheduledRunner>> runners =
          sRunners;
      final WeakReference<ScheduledRunner> runner = runners.get(service);
      scheduledRunner = (runner != null) ? runner.get() : null;
      if (scheduledRunner == null) {
        scheduledRunner = new ScheduledRunner(service);
        runners.put(service, new WeakReference<ScheduledRunner>(scheduledRunner));
      }
    }

    return scheduledRunner;
  }

  /**
   * Returns a runner instance employing the specified service.
   * <br>
   * The returned runner will shut down the service as soon as it is stopped.
   *
   * @param service the executor service.
   * @return the runner.
   */
  @NotNull
  static ScheduledRunner getStoppableInstance(@NotNull final ScheduledExecutorService service) {
    return new StoppableScheduledRunner(service);
  }

  @Override
  public void cancel(@NotNull final Execution execution) {
    final WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures;
    synchronized (mFutures) {
      scheduledFutures = mFutures.remove(execution);
    }

    if (scheduledFutures != null) {
      for (final ScheduledFuture<?> future : scheduledFutures.keySet()) {
        future.cancel(false);
      }
    }
  }

  @Override
  public void run(@NotNull final Execution execution, final long delay,
      @NotNull final TimeUnit timeUnit) {
    final ScheduledFuture<?> future =
        mService.schedule(new ExecutionWrapper(execution), delay, timeUnit);
    synchronized (mFutures) {
      final WeakIdentityHashMap<Execution, WeakHashMap<ScheduledFuture<?>, Void>> futures =
          mFutures;
      WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures = futures.get(execution);
      if (scheduledFutures == null) {
        scheduledFutures = new WeakHashMap<ScheduledFuture<?>, Void>();
        futures.put(execution, scheduledFutures);
      }

      scheduledFutures.put(future, null);
    }
  }

  @NotNull
  @Override
  protected ScheduledThreadManager getThreadManager() {
    return (ScheduledThreadManager) super.getThreadManager();
  }

  /**
   * Thread manager implementation.
   */
  private static class ScheduledThreadManager implements ThreadManager {

    private final ThreadLocal<Boolean> mIsManaged = new ThreadLocal<Boolean>();

    public boolean isManagedThread() {
      final Boolean isManaged = mIsManaged.get();
      return (isManaged != null) && isManaged;
    }

    private void setManaged() {
      mIsManaged.set(true);
    }
  }

  /**
   * Implementation of a scheduled runner shutting down the backing service as soon as the runnner
   * is stopped.
   */
  private static class StoppableScheduledRunner extends ScheduledRunner {

    private final ScheduledExecutorService mService;

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    private StoppableScheduledRunner(final ScheduledExecutorService service) {
      super(service);
      mService = service;
    }

    @Override
    public void stop() {
      mService.shutdown();
    }
  }

  /**
   * Class used to keep track of the threads employed by this runner.
   */
  private class ExecutionWrapper implements Runnable {

    private final long mCurrentThreadId;

    private final Execution mExecution;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped execution.
     */
    private ExecutionWrapper(@NotNull final Execution wrapped) {
      mExecution = wrapped;
      mCurrentThreadId = Thread.currentThread().getId();
    }

    public void run() {
      final Thread currentThread = Thread.currentThread();
      if (currentThread.getId() != mCurrentThreadId) {
        getThreadManager().setManaged();
      }

      mExecution.run();
    }
  }
}
