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

import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.runner.Runners;

import org.jetbrains.annotations.NotNull;

/**
 * A runner decorator ensuring that only one execution is run at one time.
 * <p>
 * Created by davide-maestroni on 11/24/2016.
 */
class SingleExecutionRunner extends RunnerDecorator {

  private final Runner mRunner;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped runner instance.
   */
  SingleExecutionRunner(@NotNull final Runner wrapped) {
    super(wrapped.isSynchronous() ? new SynchronizedRunner(wrapped)
        : Runners.throttlingRunner(wrapped, 1));
    mRunner = wrapped;
  }

  /**
   * Returns the decorated runner.
   *
   * @return the runner instance.
   */
  @NotNull
  Runner decorated() {
    return mRunner;
  }
}
