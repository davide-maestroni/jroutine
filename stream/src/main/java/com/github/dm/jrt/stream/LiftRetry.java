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
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Lifting function making the stream retry the whole flow of data.
 * <p>
 * Created by davide-maestroni on 05/02/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class LiftRetry<IN, OUT> implements LiftingFunction<IN, OUT, IN, OUT> {

  private final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
      mBackoffFunction;

  private final InvocationConfiguration mConfiguration;

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor        the executor instance.
   * @param configuration   the invocation configuration.
   * @param backoffFunction the backoff function.
   */
  LiftRetry(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration,
      @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
          backoffFunction) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    mBackoffFunction = ConstantConditions.notNull("backoff function", backoffFunction);
  }

  public Supplier<? extends Channel<IN, OUT>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) {
    return new Supplier<Channel<IN, OUT>>() {

      public Channel<IN, OUT> get() {
        final ScheduledExecutor executor = mExecutor;
        final InvocationConfiguration configuration = mConfiguration;
        final Channel<IN, IN> inputChannel = JRoutineCore.channelOn(executor)
                                                         .withConfiguration(
                                                             configuration
                                                                 .inputConfigurationBuilder()
                                                                          .configuration())
                                                         .ofType();
        final Channel<OUT, OUT> outputChannel = JRoutineCore.channelOn(executor)
                                                            .withConfiguration(
                                                                configuration
                                                                    .outputConfigurationBuilder()
                                                                             .configuration())
                                                            .ofType();
        new RetryChannelConsumer<IN, OUT>(executor, supplier, mBackoffFunction, inputChannel,
            outputChannel).run();
        return JRoutineCore.flatten(inputChannel, JRoutineCore.readOnly(outputChannel));
      }
    };
  }
}
