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

import com.github.dm.jrt.core.builder.AbstractChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class implementing a builder of channel objects.
 * <p>
 * Created by davide-maestroni on 10/25/2014.
 */
class DefaultChannelBuilder extends AbstractChannelBuilder {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultChannelBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@Nullable final OUT... outputs) {
    return this.<OUT>ofType().pass(outputs).close();
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@Nullable final Iterable<OUT> outputs) {
    return this.<OUT>ofType().pass(outputs).close();
  }

  @NotNull
  public <OUT> Channel<?, OUT> of() {
    return this.<OUT>ofType().close();
  }

  @NotNull
  public <OUT> Channel<?, OUT> of(@Nullable final OUT output) {
    return this.<OUT>ofType().pass(output).close();
  }

  @NotNull
  public <DATA> Channel<DATA, DATA> ofType() {
    return new DefaultChannel<DATA>(mExecutor, getConfiguration());
  }
}
