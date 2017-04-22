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

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Runnable decorator implementation.
 * <p>
 * Created by davide-maestroni on 04/09/2016.
 */
public class RunnableDecorator implements Runnable {

  private final Runnable mCommand;

  /**
   * Constructor.
   *
   * @param wrapped the wrapped instance.
   */
  public RunnableDecorator(@NotNull final Runnable wrapped) {
    mCommand = ConstantConditions.notNull("runnable instance", wrapped);
  }

  public void run() {
    mCommand.run();
  }
}
