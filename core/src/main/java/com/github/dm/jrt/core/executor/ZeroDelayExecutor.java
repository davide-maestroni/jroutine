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

import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Executor decorator employing a shared synchronous executor when commands are enqueued with a 0
 * delay on one of the managed threads.
 * <p>
 * Created by davide-maestroni on 04/09/2016.
 */
class ZeroDelayExecutor extends ScheduledExecutorDecorator {

  private static final WeakIdentityHashMap<ScheduledExecutor, WeakReference<ZeroDelayExecutor>>
      sExecutors = new WeakIdentityHashMap<ScheduledExecutor, WeakReference<ZeroDelayExecutor>>();

  private static final QueuedExecutor sSyncExecutor = new QueuedExecutor();

  private final WeakIdentityHashMap<Runnable, WeakReference<RunnableDecorator>> mCommands =
      new WeakIdentityHashMap<Runnable, WeakReference<RunnableDecorator>>();

  /**
   * Constructor.
   *
   * @param wrapped the wrapped instance.
   */
  private ZeroDelayExecutor(@NotNull final ScheduledExecutor wrapped) {
    super(wrapped);
  }

  /**
   * Returns an instance wrapping the specified executor.
   *
   * @param wrapped the wrapped instance.
   * @return the zero delay executor.
   */
  @NotNull
  static ZeroDelayExecutor of(@NotNull final ScheduledExecutor wrapped) {
    if (wrapped instanceof ZeroDelayExecutor) {
      return (ZeroDelayExecutor) wrapped;
    }

    ZeroDelayExecutor zeroDelayExecutor;
    synchronized (sExecutors) {
      final WeakIdentityHashMap<ScheduledExecutor, WeakReference<ZeroDelayExecutor>> executors =
          sExecutors;
      final WeakReference<ZeroDelayExecutor> executor = executors.get(wrapped);
      zeroDelayExecutor = (executor != null) ? executor.get() : null;
      if (zeroDelayExecutor == null) {
        zeroDelayExecutor = new ZeroDelayExecutor(wrapped);
        executors.put(wrapped, new WeakReference<ZeroDelayExecutor>(zeroDelayExecutor));
      }
    }

    return zeroDelayExecutor;
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    final RunnableDecorator decorator;
    synchronized (mCommands) {
      final WeakReference<RunnableDecorator> reference = mCommands.remove(command);
      decorator = (reference != null) ? reference.get() : null;
    }

    if (decorator != null) {
      sSyncExecutor.cancel(decorator);
    }

    super.cancel(command);
  }

  @Override
  public void execute(@NotNull final Runnable command) {
    if (getThreadManager().isManagedThread()) {
      sSyncExecutor.execute(getRunnableDecorator(command));

    } else {
      super.execute(command);
    }
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    if ((delay == 0) && getThreadManager().isManagedThread()) {
      sSyncExecutor.execute(getRunnableDecorator(command));

    } else {
      super.execute(command, delay, timeUnit);
    }
  }

  @NotNull
  private RunnableDecorator getRunnableDecorator(@NotNull final Runnable command) {
    RunnableDecorator decorator;
    synchronized (mCommands) {
      final WeakIdentityHashMap<Runnable, WeakReference<RunnableDecorator>> commands = mCommands;
      final WeakReference<RunnableDecorator> reference = commands.get(command);
      decorator = (reference != null) ? reference.get() : null;
      if (decorator == null) {
        decorator = new RunnableDecorator(command);
        commands.put(command, new WeakReference<RunnableDecorator>(decorator));
      }
    }

    return decorator;
  }
}
