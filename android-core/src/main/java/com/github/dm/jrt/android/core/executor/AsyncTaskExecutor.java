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

package com.github.dm.jrt.android.core.executor;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;

import com.github.dm.jrt.core.executor.AsyncExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of an executor employing {@link android.os.AsyncTask} instances to execute the
 * routine invocations.
 * <p>
 * Created by davide-maestroni on 09/28/2014.
 */
class AsyncTaskExecutor extends AsyncExecutor {

  private static final Void[] NO_PARAMS = new Void[0];

  private static final ScheduledExecutor sMainExecutor =
      ScheduledExecutors.zeroDelayExecutor(new MainExecutor());

  private final Executor mExecutor;

  private final WeakIdentityHashMap<Runnable, WeakHashMap<CommandTask, Void>> mTasks =
      new WeakIdentityHashMap<Runnable, WeakHashMap<CommandTask, Void>>();

  /**
   * Constructor.
   * <p>
   * Note that, in case a null executor is passed as parameter, the default one will be used.
   *
   * @param executor the executor.
   */
  AsyncTaskExecutor(@Nullable final Executor executor) {
    super(new AsyncTaskThreadManager());
    mExecutor = executor;
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    synchronized (mTasks) {
      final WeakHashMap<CommandTask, Void> commandTasks = mTasks.remove(command);
      if (commandTasks != null) {
        for (final CommandTask task : commandTasks.keySet()) {
          sMainExecutor.cancel(task);
          task.cancel(false);
        }
      }
    }
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    final CommandTask task = new CommandTask(command, mExecutor);
    synchronized (mTasks) {
      final WeakIdentityHashMap<Runnable, WeakHashMap<CommandTask, Void>> tasks = mTasks;
      WeakHashMap<CommandTask, Void> commandTasks = tasks.get(command);
      if (commandTasks == null) {
        commandTasks = new WeakHashMap<CommandTask, Void>();
        tasks.put(command, commandTasks);
      }

      commandTasks.put(task, null);
    }

    // We need to ensure that a task is always started from the main thread
    sMainExecutor.execute(task, delay, timeUnit);
  }

  @NotNull
  @Override
  protected AsyncTaskThreadManager getThreadManager() {
    return (AsyncTaskThreadManager) super.getThreadManager();
  }

  /**
   * Thread manager implementation.
   */
  private static class AsyncTaskThreadManager implements ThreadManager {

    private final ThreadLocal<Boolean> mIsManaged = new ThreadLocal<Boolean>();

    @Override
    public boolean isManagedThread() {
      final Boolean isManaged = mIsManaged.get();
      return (isManaged != null) && isManaged;
    }

    private void setManaged() {
      mIsManaged.set(true);
    }
  }

  /**
   * Implementation of an async task whose command starts in a runnable.
   */
  private class CommandTask extends AsyncTask<Void, Void, Void> implements Runnable {

    private final Runnable mCommand;

    private final Executor mExecutor;

    /**
     * Constructor.
     *
     * @param command  the command instance.
     * @param executor the executor.
     */
    private CommandTask(@NotNull final Runnable command, @Nullable final Executor executor) {
      mCommand = command;
      mExecutor = executor;
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    @Override
    public void run() {
      final Executor executor = mExecutor;
      if ((executor != null) && (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)) {
        executeOnExecutor(executor, NO_PARAMS);

      } else {
        execute(NO_PARAMS);
      }
    }

    @Override
    protected Void doInBackground(@NotNull final Void... voids) {
      final Looper looper = Looper.myLooper();
      if (looper != Looper.getMainLooper()) {
        getThreadManager().setManaged();
      }

      mCommand.run();
      return null;
    }
  }
}
