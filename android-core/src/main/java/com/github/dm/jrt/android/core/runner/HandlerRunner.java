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

package com.github.dm.jrt.android.core.runner;

import android.os.Handler;
import android.os.Looper;

import com.github.dm.jrt.core.runner.AsyncRunner;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.ExecutionDecorator;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a runner employing an Android {@link android.os.Handler} queue to execute the
 * routine invocations.
 * <p>
 * Created by davide-maestroni on 04/09/2016.
 */
class HandlerRunner extends AsyncRunner {

  private final WeakIdentityHashMap<Execution, WeakReference<ExecutionDecorator>> mExecutions =
      new WeakIdentityHashMap<Execution, WeakReference<ExecutionDecorator>>();

  private final Handler mHandler;

  /**
   * Constructor.
   *
   * @param handler the handler to employ.
   */
  HandlerRunner(@NotNull final Handler handler) {
    super(new HandlerThreadManager(handler.getLooper()));
    mHandler = handler;
  }

  @Override
  public void cancel(@NotNull final Execution execution) {
    final ExecutionDecorator decorator;
    synchronized (mExecutions) {
      final WeakReference<ExecutionDecorator> reference = mExecutions.remove(execution);
      decorator = (reference != null) ? reference.get() : null;
    }

    mHandler.removeCallbacks(decorator);
  }

  @Override
  public void run(@NotNull final Execution execution, final long delay,
      @NotNull final TimeUnit timeUnit) {
    ExecutionDecorator decorator;
    synchronized (mExecutions) {
      final WeakIdentityHashMap<Execution, WeakReference<ExecutionDecorator>> executions =
          mExecutions;
      final WeakReference<ExecutionDecorator> reference = mExecutions.remove(execution);
      decorator = (reference != null) ? reference.get() : null;
      if (decorator == null) {
        decorator = new ExecutionDecorator(execution);
        executions.put(execution, new WeakReference<ExecutionDecorator>(decorator));
      }
    }

    if (delay > 0) {
      mHandler.postDelayed(decorator, timeUnit.toMillis(delay));

    } else {
      mHandler.post(decorator);
    }
  }

  /**
   * Thread manager implementation.
   */
  private static class HandlerThreadManager implements ThreadManager {

    private final Looper mLooper;

    /**
     * Constructor.
     *
     * @param looper the handler looper.
     */
    private HandlerThreadManager(@NotNull final Looper looper) {
      mLooper = looper;
    }

    @Override
    public boolean isManagedThread() {
      return (mLooper == Looper.myLooper());
    }
  }
}
