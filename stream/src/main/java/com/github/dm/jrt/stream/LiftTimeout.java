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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Lifting function making the stream abort if a new result is not produced, or the invocation does
 * not complete, before the specified timeouts elapse.
 * <p>
 * Created by davide-maestroni on 07/29/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LiftTimeout<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

  private final ChannelConfiguration mConfiguration;

  private final ScheduledExecutor mExecutor;

  private final long mOutputTimeout;

  private final TimeUnit mOutputTimeoutUnit;

  private final long mTotalTimeout;

  private final TimeUnit mTotalTimeoutUnit;

  /**
   * Constructor.
   *
   * @param executor       the executor instance.
   * @param configuration  the channel configuration.
   * @param outputTimeout  the new output timeout value.
   * @param outputTimeUnit the new output timeout unit.
   * @param totalTimeout   the total timeout value.
   * @param totalTimeUnit  the total timeout unit.
   */
  LiftTimeout(@NotNull final ScheduledExecutor executor,
      @NotNull final ChannelConfiguration configuration, final long outputTimeout,
      @NotNull final TimeUnit outputTimeUnit, final long totalTimeout,
      @NotNull final TimeUnit totalTimeUnit) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    mOutputTimeout = ConstantConditions.notNegative("output timeout value", outputTimeout);
    mOutputTimeoutUnit = ConstantConditions.notNull("output time unit", outputTimeUnit);
    mTotalTimeout = ConstantConditions.notNegative("total timeout value", totalTimeout);
    mTotalTimeoutUnit = ConstantConditions.notNull("total time unit", totalTimeUnit);
  }

  public Supplier<? extends Channel<IN, OUT>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) throws Exception {
    return wrapSupplier(supplier).andThen(new Function<Channel<IN, OUT>, Channel<IN, OUT>>() {

      public Channel<IN, OUT> apply(final Channel<IN, OUT> channel) throws Exception {
        final ScheduledExecutor executor = mExecutor;
        final Channel<OUT, OUT> outputChannel = JRoutineCore.channelOn(executor).ofType();
        channel.consume(new TimeoutChannelConsumer<OUT>(executor, mConfiguration, mOutputTimeout,
            mOutputTimeoutUnit, mTotalTimeout, mTotalTimeoutUnit, outputChannel));
        return JRoutineCore.flattenChannels(channel, outputChannel);
      }
    });
  }
}
