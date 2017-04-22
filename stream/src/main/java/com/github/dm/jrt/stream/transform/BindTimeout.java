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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Timeout binding function.
 * <p>
 * Created by davide-maestroni on 07/26/2016.
 *
 * @param <OUT> the output data type.
 */
class BindTimeout<OUT> implements Function<Channel<?, OUT>, Channel<?, OUT>> {

  private final ChannelConfiguration mConfiguration;

  private final long mOutputTimeout;

  private final TimeUnit mOutputTimeoutUnit;

  private final long mTotalTimeout;

  private final TimeUnit mTotalTimeoutUnit;

  /**
   * Constructor.
   *
   * @param configuration  the channel configuration.
   * @param outputTimeout  the new output timeout value.
   * @param outputTimeUnit the new output timeout unit.
   * @param totalTimeout   the total timeout value.
   * @param totalTimeUnit  the total timeout unit.
   */
  BindTimeout(@NotNull final ChannelConfiguration configuration, final long outputTimeout,
      @NotNull final TimeUnit outputTimeUnit, final long totalTimeout,
      @NotNull final TimeUnit totalTimeUnit) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mOutputTimeout = ConstantConditions.notNegative("output timeout value", outputTimeout);
    mOutputTimeoutUnit = ConstantConditions.notNull("output time unit", outputTimeUnit);
    mTotalTimeout = ConstantConditions.notNegative("total timeout value", totalTimeout);
    mTotalTimeoutUnit = ConstantConditions.notNull("total time unit", totalTimeUnit);
  }

  public Channel<?, OUT> apply(final Channel<?, OUT> channel) {
    final ChannelConfiguration configuration = mConfiguration;
    final Channel<OUT, OUT> outputChannel =
        JRoutineCore.<OUT>ofData().apply(configuration).buildChannel();
    channel.consume(
        new TimeoutChannelConsumer<OUT>(mOutputTimeout, mOutputTimeoutUnit, mTotalTimeout,
            mTotalTimeoutUnit, configuration.getExecutorOrElse(ScheduledExecutors.defaultExecutor()),
            outputChannel));
    return outputChannel;
  }
}
