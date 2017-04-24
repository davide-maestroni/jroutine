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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class implementing an executor employing an executor service.
 * <p>
 * Created by davide-maestroni on 10/14/2014.
 */
class ServiceExecutor extends AsyncExecutor {

  private static final WeakHashMap<ScheduledExecutorService, WeakReference<ServiceExecutor>>
      sExecutors = new WeakHashMap<ScheduledExecutorService, WeakReference<ServiceExecutor>>();

  private final WeakIdentityHashMap<Runnable, WeakHashMap<ScheduledFuture<?>, Void>> mFutures =
      new WeakIdentityHashMap<Runnable, WeakHashMap<ScheduledFuture<?>, Void>>();

  private final ScheduledExecutorService mService;

  /**
   * Constructor.
   *
   * @param service the executor service.
   */
  private ServiceExecutor(@NotNull final ScheduledExecutorService service) {
    super(new ScheduledThreadManager());
    mService = ConstantConditions.notNull("executor service", service);
  }

  /**
   * Returns an executor instance employing the specified service.
   *
   * @param service the executor service.
   * @return the executor.
   */
  @NotNull
  static ServiceExecutor executorOf(@NotNull final ScheduledExecutorService service) {
    ServiceExecutor serviceExecutor;
    synchronized (sExecutors) {
      final WeakHashMap<ScheduledExecutorService, WeakReference<ServiceExecutor>> executors =
          sExecutors;
      final WeakReference<ServiceExecutor> executor = executors.get(service);
      serviceExecutor = (executor != null) ? executor.get() : null;
      if (serviceExecutor == null) {
        serviceExecutor = new ServiceExecutor(service);
        executors.put(service, new WeakReference<ServiceExecutor>(serviceExecutor));
      }
    }

    return serviceExecutor;
  }

  /**
   * Returns an executor instance employing the specified service.
   * <br>
   * The returned executor will shut down the service as soon as it is stopped.
   *
   * @param service the executor service.
   * @return the executor.
   */
  @NotNull
  static ServiceExecutor executorOfStoppable(@NotNull final ScheduledExecutorService service) {
    return new StoppableServiceExecutor(service);
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    final WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures;
    synchronized (mFutures) {
      scheduledFutures = mFutures.remove(command);
    }

    if (scheduledFutures != null) {
      for (final ScheduledFuture<?> future : scheduledFutures.keySet()) {
        future.cancel(false);
      }
    }
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    final ScheduledFuture<?> future =
        mService.schedule(new RunnableWrapper(command), delay, timeUnit);
    synchronized (mFutures) {
      final WeakIdentityHashMap<Runnable, WeakHashMap<ScheduledFuture<?>, Void>> futures = mFutures;
      WeakHashMap<ScheduledFuture<?>, Void> scheduledFutures = futures.get(command);
      if (scheduledFutures == null) {
        scheduledFutures = new WeakHashMap<ScheduledFuture<?>, Void>();
        futures.put(command, scheduledFutures);
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
   * Implementation of a scheduled executor shutting down the backing service as soon as the
   * executor is stopped.
   */
  private static class StoppableServiceExecutor extends ServiceExecutor {

    private final ScheduledExecutorService mService;

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    private StoppableServiceExecutor(final ScheduledExecutorService service) {
      super(service);
      mService = service;
    }

    @Override
    public void stop() {
      mService.shutdown();
    }
  }

  /**
   * Class used to keep track of the threads employed by this executor.
   */
  private class RunnableWrapper implements Runnable {

    private final Runnable mCommand;

    private final long mCurrentThreadId;

    /**
     * Constructor.
     *
     * @param wrapped the wrapped command.
     */
    private RunnableWrapper(@NotNull final Runnable wrapped) {
      mCommand = wrapped;
      mCurrentThreadId = Thread.currentThread().getId();
    }

    public void run() {
      final Thread currentThread = Thread.currentThread();
      if (currentThread.getId() != mCurrentThreadId) {
        getThreadManager().setManaged();
      }

      mCommand.run();
    }
  }
}
