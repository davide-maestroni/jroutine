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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Class implementing a queued synchronous runner.
 * <p>
 * The runner maintains an internal buffer of executions that are consumed only when the last one
 * completes, thus avoiding overflowing the call stack because of nested calls to other routines.
 * <p>
 * Created by davide-maestroni on 09/18/2014.
 */
class QueuedRunner extends SyncRunner {

  @Override
  public void cancel(@NotNull final Execution execution) {
    LocalRunner.cancel(execution);
  }

  @Override
  public void run(@NotNull final Execution execution, final long delay,
      @NotNull final TimeUnit timeUnit) {
    LocalRunner.run(execution, delay, timeUnit);
  }
}
