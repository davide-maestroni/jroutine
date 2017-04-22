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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.executor.RunnableDecorator;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Executor implementation synchronizing the passed commands.
 * <p>
 * Created by davide-maestroni on 09/05/2016.
 */
class SynchronizedExecutor extends ScheduledExecutorDecorator {

  private final Object mCommandMutex = new Object();

  private final WeakIdentityHashMap<Runnable, WeakReference<SynchronizedRunnable>> mCommands =
      new WeakIdentityHashMap<Runnable, WeakReference<SynchronizedRunnable>>();

  private final Object mMutex = new Object();

  /**
   * Constructor.
   *
   * @param wrapped the wrapped instance.
   */
  SynchronizedExecutor(@NotNull final ScheduledExecutor wrapped) {
    super(wrapped);
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    SynchronizedRunnable synchronizedRunnable = null;
    synchronized (mMutex) {
      final WeakReference<SynchronizedRunnable> commandReference = mCommands.get(command);
      if (commandReference != null) {
        synchronizedRunnable = commandReference.get();
      }
    }

    if (synchronizedRunnable != null) {
      super.cancel(synchronizedRunnable);
    }
  }

  @Override
  public void execute(@NotNull final Runnable command) {
    super.execute(getSynchronizedRunnable(command));
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    super.execute(getSynchronizedRunnable(command), delay, timeUnit);
  }

  @NotNull
  private SynchronizedRunnable getSynchronizedRunnable(@NotNull final Runnable command) {
    SynchronizedRunnable synchronizedRunnable;
    synchronized (mMutex) {
      final WeakIdentityHashMap<Runnable, WeakReference<SynchronizedRunnable>> commands = mCommands;
      final WeakReference<SynchronizedRunnable> commandReference = commands.get(command);
      synchronizedRunnable = (commandReference != null) ? commandReference.get() : null;
      if (synchronizedRunnable == null) {
        synchronizedRunnable = new SynchronizedRunnable(command);
        commands.put(command, new WeakReference<SynchronizedRunnable>(synchronizedRunnable));
      }
    }

    return synchronizedRunnable;
  }

  /**
   * Synchronized runnable decorator.
   */
  private class SynchronizedRunnable extends RunnableDecorator {

    /**
     * Constructor.
     *
     * @param wrapped the wrapped instance.
     */
    private SynchronizedRunnable(@NotNull final Runnable wrapped) {
      super(wrapped);
    }

    @Override
    public void run() {
      synchronized (mCommandMutex) {
        super.run();
      }
    }
  }
}
