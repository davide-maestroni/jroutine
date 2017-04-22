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

import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Executor implementation just running the command in the same call to the {@code execute()}
 * method.
 * <br>
 * In case of delay the thread will just sleep for the required time.
 * <br>
 * Note that such behavior is compliant with the interface contract, even if it might unnecessarily
 * slow down the calling thread. It's also true that this executor is not meant to be used with
 * delays.
 * <p>
 * Created by davide-maestroni on 05/13/2016.
 */
class ImmediateExecutor extends SyncExecutor {

  @Override
  public void execute(@NotNull final Runnable command) {
    command.run();
  }

  @Override
  public void execute(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit timeUnit) {
    if (delay > 0) {
      try {
        DurationMeasure.sleepAtLeast(delay, timeUnit);

      } catch (final InterruptedException e) {
        throw new InterruptedInvocationException(e);
      }
    }

    command.run();
  }
}
