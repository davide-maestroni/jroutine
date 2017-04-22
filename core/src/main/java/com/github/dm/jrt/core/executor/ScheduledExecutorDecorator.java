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

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a decorator of an executor object.
 * <p>
 * Created by davide-maestroni on 09/22/2014.
 */
public class ScheduledExecutorDecorator extends ScheduledExecutor {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped instance.
   */
  public ScheduledExecutorDecorator(@NotNull final ScheduledExecutor wrapped) {
    super(wrapped.getThreadManager());
    mExecutor = wrapped;
  }

  @Override
  public void cancel(@NotNull final Runnable command) {
    mExecutor.cancel(command);
  }

  @Override
  public void execute(@NotNull final Runnable command) {
    mExecutor.execute(command);
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    mExecutor.execute(command, delay, timeUnit);
  }

  @Override
  public boolean isExecutionThread() {
    return mExecutor.isExecutionThread();
  }

  @Override
  public boolean isSynchronous() {
    return mExecutor.isSynchronous();
  }

  @Override
  public void stop() {
    mExecutor.stop();
  }
}
