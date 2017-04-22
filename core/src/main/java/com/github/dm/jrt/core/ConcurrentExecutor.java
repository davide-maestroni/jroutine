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

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.executor.ScheduledExecutors;

import org.jetbrains.annotations.NotNull;

/**
 * A executor decorator ensuring that only one command is run at one time.
 * <p>
 * Created by davide-maestroni on 11/24/2016.
 */
class ConcurrentExecutor extends ScheduledExecutorDecorator {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped executor instance.
   */
  ConcurrentExecutor(@NotNull final ScheduledExecutor wrapped) {
    super(wrapped.isSynchronous() ? new SynchronizedExecutor(wrapped)
        : ScheduledExecutors.throttlingExecutor(wrapped, 1));
    mExecutor = wrapped;
  }

  /**
   * Returns the decorated executor.
   *
   * @return the executor instance.
   */
  @NotNull
  ScheduledExecutor decorated() {
    return mExecutor;
  }
}
