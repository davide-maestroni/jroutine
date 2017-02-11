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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.ObservableBuilder;

import org.jetbrains.annotations.NotNull;

import rx.Observable;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Observable instance from a channel:
 * <pre><code>
 * JRoutineObservable.from(myChannel).buildObservable().subscribe(getAction());
 * </code></pre>
 * <p>
 * In a dual way, a channel can be created from an Observable:
 * <pre><code>
 * JRoutineObservable.with(myObservable).buildChannel().bind(getConsumer());
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 12/09/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineObservable {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineObservable() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of Observables fed by the specified channel.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <OUT> ObservableBuilder<?, OUT> from(@NotNull final Channel<?, OUT> channel) {
    return new ChannelObservableBuilder<OUT>(channel);
  }

  /**
   * Returns a builder of Observables fed by an invocation of the specified routine.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <IN, OUT> ObservableBuilder<IN, OUT> from(@NotNull final Routine<IN, OUT> routine) {
    return new RoutineObservableBuilder<IN, OUT>(routine);
  }

  /**
   * Returns a builder of Observables fed by an invocation of a routine created through the
   * specified builder.
   *
   * @param builder the routine builder.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Observable builder.
   */
  @NotNull
  public static <IN, OUT> ObservableBuilder<IN, OUT> from(
      @NotNull final RoutineBuilder<IN, OUT> builder) {
    return from(builder.buildRoutine());
  }

  /**
   * Returns a builder of channels fed by the specified Observable.
   *
   * @param observable the Observable instance.
   * @param <OUT>      the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Observable<OUT> observable) {
    return new ObservableChannelBuilder<OUT>(observable);
  }
}
