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

import org.jetbrains.annotations.NotNull;

/**
 * Base abstract implementation of an asynchronous executor.
 * <br>
 * For an asynchronous executor the execution threads are the same as the managed ones.
 * <p>
 * Created by davide-maestroni on 06/06/2016.
 */
public abstract class AsyncExecutor extends ScheduledExecutor {

  /**
   * Constructor.
   *
   * @param manager the manager of threads.
   */
  protected AsyncExecutor(@NotNull final ThreadManager manager) {
    super(manager);
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
  }

  @Override
  public boolean isExecutionThread() {
    return getThreadManager().isManagedThread();
  }

  @Override
  public boolean isSynchronous() {
    return false;
  }

  @Override
  public void stop() {
  }
}
