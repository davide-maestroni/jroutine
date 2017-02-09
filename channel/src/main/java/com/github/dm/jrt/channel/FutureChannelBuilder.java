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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

/**
 * Builder implementation returning a channel wrapping a Future instance.
 * <p>
 * Created by davide-maestroni on 08/31/2016.
 *
 * @param <OUT> the output data type.
 */
class FutureChannelBuilder<OUT> extends AbstractChannelBuilder<OUT, OUT> {

  private final Future<OUT> mFuture;

  private final boolean mInterruptIfRunning;

  /**
   * Constructor.
   *
   * @param future                the future instance.
   * @param mayInterruptIfRunning if the thread executing the task should be interrupted.
   */
  FutureChannelBuilder(@NotNull final Future<OUT> future, final boolean mayInterruptIfRunning) {
    mFuture = ConstantConditions.notNull("future instance", future);
    mInterruptIfRunning = mayInterruptIfRunning;
  }

  @NotNull
  public Channel<OUT, OUT> buildChannel() {
    return new FutureChannel<OUT>(getConfiguration(), mFuture, mInterruptIfRunning);
  }
}
