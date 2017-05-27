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
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.transform.LiftingFunction;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Lifting function splitting the outputs produced by the stream, so that each subset will be
 * processed by a different routine invocation.
 * <p>
 * Created by davide-maestroni on 05/02/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <AFTER> the new output type.
 */
class LiftSplitBy<IN, OUT, AFTER> implements LiftingFunction<IN, OUT, IN, AFTER> {

  private final ChannelConfiguration mConfiguration;

  private final ScheduledExecutor mExecutor;

  private final Function<? super OUT, ?> mKeyFunction;

  private final Routine<? super OUT, ? extends AFTER> mRoutine;

  /**
   * Constructor.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   * @param keyFunction   the key function.
   * @param routine       the routine instance.
   */
  LiftSplitBy(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration,
      @NotNull final Function<? super OUT, ?> keyFunction,
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mKeyFunction = ConstantConditions.notNull("key function", keyFunction);
    mRoutine = ConstantConditions.notNull("routine instance", routine);
    mConfiguration = configuration.outputConfigurationBuilder().configuration();
  }

  public Supplier<? extends Channel<IN, AFTER>> apply(
      final Supplier<? extends Channel<IN, OUT>> supplier) {
    return wrapSupplier(supplier).andThen(new Function<Channel<IN, OUT>, Channel<IN, AFTER>>() {

      public Channel<IN, AFTER> apply(final Channel<IN, OUT> channel) {
        final Channel<AFTER, AFTER> outputChannel =
            JRoutineCore.channelOn(mExecutor).withConfiguration(mConfiguration).ofType();
        channel.consume(
            new SplitByChannelConsumer<OUT, AFTER>(mKeyFunction, mRoutine, outputChannel));
        return JRoutineCore.flatten(channel, JRoutineCore.readOnly(outputChannel));
      }
    });
  }
}
