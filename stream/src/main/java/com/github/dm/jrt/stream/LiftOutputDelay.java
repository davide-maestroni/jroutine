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
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Lifting function adding a delay at the end of the stream.
 * <p>
 * Created by davide-maestroni on 05/02/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LiftOutputDelay<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

  private final ChannelConfiguration mConfiguration;

  private final long mDelay;

  private final ScheduledExecutor mExecutor;

  private final TimeUnit mTimeUnit;

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   * @param delay         the delay value.
   * @param timeUnit      the delay time unit.
   */
  LiftOutputDelay(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, final long delay,
      @NotNull final TimeUnit timeUnit) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mTimeUnit = ConstantConditions.notNull("delay time unit", timeUnit);
    mConfiguration = configuration.outputConfigurationBuilder().configuration();
    mDelay = delay;
  }

  public Supplier<? extends Channel<IN, OUT>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) {
    return wrapSupplier(supplier).andThen(new Function<Channel<IN, OUT>, Channel<IN, OUT>>() {

      public Channel<IN, OUT> apply(final Channel<IN, OUT> channel) {
        return channel.pipe(
            JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).<OUT>ofType().after(
                mDelay, mTimeUnit));
      }
    });
  }
}
