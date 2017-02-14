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

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.FlowableBuilder;

import org.jetbrains.annotations.NotNull;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Flowable instance from a channel:
 * <pre><code>
 * JRoutineFlowable.from(myChannel).buildFlowable().subscribe(getAction());
 * </code></pre>
 * <p>
 * In a dual way, a channel can be created from an Flowable:
 * <pre><code>
 * JRoutineFlowable.with(myFlowable).buildChannel().consume(getConsumer());
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineFlowable {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineFlowable() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of Flowables fed by the specified channel.
   *
   * @param channel the channel instance.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <OUT> FlowableBuilder<?, OUT> from(@NotNull final Channel<?, OUT> channel) {
    return new ChannelFlowableBuilder<OUT>(channel);
  }

  /**
   * Returns a builder of Flowables fed by an invocation of the specified routine.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <IN, OUT> FlowableBuilder<IN, OUT> from(@NotNull final Routine<IN, OUT> routine) {
    return new RoutineFlowableBuilder<IN, OUT>(routine);
  }

  /**
   * Returns a builder of Flowables fed by an invocation of a routine created through the
   * specified builder.
   *
   * @param builder the routine builder.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the Flowable builder.
   */
  @NotNull
  public static <IN, OUT> FlowableBuilder<IN, OUT> from(
      @NotNull final RoutineBuilder<IN, OUT> builder) {
    return from(builder.buildRoutine());
  }

  /**
   * Returns a builder of channels fed by the specified Completable.
   *
   * @param completable the Completable instance.
   * @return the channel builder.
   */
  @NotNull
  public static ChannelBuilder<?, ?> with(@NotNull final Completable completable) {
    return new FlowableChannelBuilder<Object>(completable.toFlowable());
  }

  /**
   * Returns a builder of channels fed by the specified Flowable.
   *
   * @param flowable the Flowable instance.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Flowable<OUT> flowable) {
    return new FlowableChannelBuilder<OUT>(flowable);
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
    return new FlowableChannelBuilder<OUT>(observable.toFlowable(BackpressureStrategy.MISSING));
  }

  /**
   * Returns a builder of channels fed by the specified Single.
   *
   * @param single the Single instance.
   * @param <OUT>  the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> with(@NotNull final Single<OUT> single) {
    return new FlowableChannelBuilder<OUT>(single.toFlowable());
  }
}
