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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.AbstractFlowableBuilder;
import com.github.dm.jrt.rx.config.FlowableConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

/**
 * Builder of Flowables emitting a routine invocation output data.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class RoutineFlowableBuilder<IN, OUT> extends AbstractFlowableBuilder<IN, OUT> {

  private final Routine<IN, OUT> mRoutine;

  /**
   * Constructor.
   *
   * @param routine the routine instance.
   */
  RoutineFlowableBuilder(@NotNull final Routine<IN, OUT> routine) {
    mRoutine = ConstantConditions.notNull("routine instance", routine);
  }

  @NotNull
  public Flowable<OUT> buildFlowable() {
    final FlowableConfiguration<IN> configuration = getConfiguration();
    return Flowable.create(
        new RoutineOnSubscribe<IN, OUT>(mRoutine, configuration.getInputsOrElse(null)),
        configuration.getBackpressureOrElse(BackpressureStrategy.BUFFER));
  }

  /**
   * Function binding an invocation channel to an Emitter.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineOnSubscribe<IN, OUT> implements FlowableOnSubscribe<OUT> {

    private final Iterable<? extends IN> mInputs;

    private final Routine<? super IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     * @param inputs  an iterable returning the invocation inputs.
     */
    private RoutineOnSubscribe(@NotNull final Routine<? super IN, OUT> routine,
        @Nullable final Iterable<? extends IN> inputs) {
      mRoutine = routine;
      mInputs = inputs;
    }

    public void subscribe(final FlowableEmitter<OUT> emitter) {
      final Channel<? super IN, OUT> channel = mRoutine.call();
      emitter.setCancellable(new ChannelOnSubscribe<OUT>(channel));
      channel.bind(new EmitterConsumer<OUT>(emitter)).pass(mInputs).close();
    }
  }
}
