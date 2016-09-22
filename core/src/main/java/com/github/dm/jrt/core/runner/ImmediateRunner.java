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

import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Runner implementation just running the execution in the same call to the {@code run()} method.
 * <br>
 * In case of delay the thread will just sleep for the required time.
 * <br>
 * Note that such behavior is compliant with the interface contract, even if it might unnecessarily
 * slow down the calling thread. It's also true that this runner is not meant to be used with
 * delays.
 * <p>
 * Created by davide-maestroni on 05/13/2016.
 */
class ImmediateRunner extends SyncRunner {

  @Override
  public void run(@NotNull final Execution execution, final long delay,
      @NotNull final TimeUnit timeUnit) {
    if (delay > 0) {
      try {
        UnitDuration.sleepAtLeast(delay, timeUnit);

      } catch (final InterruptedException e) {
        throw new InvocationInterruptedException(e);
      }
    }

    execution.run();
  }
}
