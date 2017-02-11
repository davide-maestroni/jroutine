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
import com.github.dm.jrt.rx.builder.AbstractObservableBuilder;
import com.github.dm.jrt.rx.config.ObservableConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;

/**
 * Builder of Observables emitting a routine invocation output data.
 * <p>
 * Created by davide-maestroni on 02/11/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class RoutineObservableBuilder<IN, OUT> extends AbstractObservableBuilder<IN, OUT> {

  private final Routine<IN, OUT> mRoutine;

  /**
   * Constructor.
   *
   * @param routine the routine instance.
   */
  RoutineObservableBuilder(@NotNull final Routine<IN, OUT> routine) {
    mRoutine = ConstantConditions.notNull("routine instance", routine);
  }

  @NotNull
  public Observable<OUT> buildObservable() {
    final ObservableConfiguration<IN> configuration = getConfiguration();
    return Observable.fromEmitter(
        new RoutineEmitter<IN, OUT>(mRoutine, configuration.getInputsOrElse(null)),
        configuration.getBackpressureOrElse(BackpressureMode.BUFFER));
  }

  /**
   * Cancellable aborting a channel.
   */
  private static class ChannelCancellable implements Cancellable {

    private final Channel<?, ?> mChannel;

    /**
     * Constrcutor.
     *
     * @param channel the channel instance.
     */
    private ChannelCancellable(@NotNull final Channel<?, ?> channel) {
      mChannel = channel;
    }

    public void cancel() {
      mChannel.abort();
    }
  }

  /**
   * Function binding an invocation channel to an Emitter.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineEmitter<IN, OUT> implements Action1<Emitter<OUT>> {

    private final Iterable<? extends IN> mInputs;

    private final Routine<? super IN, OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine instance.
     * @param inputs  an iterable returning the invocation inputs.
     */
    private RoutineEmitter(@NotNull final Routine<? super IN, OUT> routine,
        @Nullable final Iterable<? extends IN> inputs) {
      mRoutine = routine;
      mInputs = inputs;
    }

    public void call(final Emitter<OUT> outEmitter) {
      final Channel<? super IN, OUT> channel = mRoutine.call();
      outEmitter.setCancellation(new ChannelCancellable(channel));
      channel.bind(new EmitterConsumer<OUT>(outEmitter)).pass(mInputs).close();
    }
  }
}
