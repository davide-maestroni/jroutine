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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.routine.StreamRoutine;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.function.util.ActionDecorator.wrapAction;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Default implementation of a stream routine.
 * <p>
 * Created by davide-maestroni on 04/28/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStreamRoutine<IN, OUT> implements StreamRoutine<IN, OUT> {

  private final Supplier<Channel<IN, OUT>> mChannelSupplier;

  private final Action mClearAction;

  /**
   * Constructor.
   *
   * @param routine the wrapped routine.
   */
  DefaultStreamRoutine(@NotNull final Routine<? super IN, ? extends OUT> routine) {
    mChannelSupplier =
        new RoutineSupplier<IN, OUT>(ConstantConditions.notNull("routine instance", routine));
    mClearAction = new RoutineAction(routine);
  }

  /**
   * Constructor.
   *
   * @param channelSupplier the supplier of the invocation channel.
   */
  private DefaultStreamRoutine(@NotNull final Supplier<Channel<IN, OUT>> channelSupplier,
      @NotNull final Action clearAction) {
    mChannelSupplier = channelSupplier;
    mClearAction = clearAction;
  }

  public void clear() {
    try {
      mClearAction.perform();

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  public Channel<IN, OUT> invoke() {
    try {
      return mChannelSupplier.get();

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> lift(
      @NotNull final Function<? super Supplier<? extends Channel<IN, OUT>>, ? extends Supplier<?
          extends Channel<BEFORE, AFTER>>> liftingFunction) {
    try {
      return new DefaultStreamRoutine<BEFORE, AFTER>(
          (Supplier<Channel<BEFORE, AFTER>>) liftingFunction.apply(mChannelSupplier), mClearAction);

    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @NotNull
  public <AFTER> StreamRoutine<IN, AFTER> map(
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    ConstantConditions.notNull("routine instance", routine);
    return new DefaultStreamRoutine<IN, AFTER>(
        wrapSupplier(mChannelSupplier).andThen(new RoutineFunction<IN, OUT, AFTER>(routine)),
        wrapAction(new RoutineAction(routine)).andThen(mClearAction));
  }

  @NotNull
  public <BEFORE, AFTER> StreamRoutine<BEFORE, AFTER> transform(
      @NotNull final Function<? super StreamRoutine<IN, OUT>, ? extends StreamRoutine<BEFORE,
          AFTER>> transformingFunction) {
    try {
      return ConstantConditions.notNull("stream routine", transformingFunction.apply(this));

    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Action clearing a routine.
   */
  private static class RoutineAction implements Action {

    private final Routine<?, ?> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to clear.
     */
    private RoutineAction(@NotNull final Routine<?, ?> routine) {
      mRoutine = routine;
    }

    public void perform() {
      mRoutine.clear();
    }
  }

  /**
   * Implementation of a function concatenating a routine to the passed channel.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <AFTER> the output type of the resulting routine.
   */
  private static class RoutineFunction<IN, OUT, AFTER>
      implements Function<Channel<IN, OUT>, Channel<IN, AFTER>> {

    private final Routine<? super OUT, ? extends AFTER> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to concatenate.
     */
    private RoutineFunction(@NotNull final Routine<? super OUT, ? extends AFTER> routine) {
      mRoutine = routine;
    }

    @SuppressWarnings("unchecked")
    public Channel<IN, AFTER> apply(final Channel<IN, OUT> channel) {
      return (Channel<IN, AFTER>) channel.pipe(mRoutine.invoke());
    }
  }

  /**
   * Implementation of a supplier invoking a routine.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineSupplier<IN, OUT> implements Supplier<Channel<IN, OUT>> {

    private final Routine<? super IN, ? extends OUT> mRoutine;

    /**
     * Constructor.
     *
     * @param routine the routine to invoke.
     */
    private RoutineSupplier(@NotNull final Routine<? super IN, ? extends OUT> routine) {
      mRoutine = routine;
    }

    @SuppressWarnings("unchecked")
    public Channel<IN, OUT> get() {
      return (Channel<IN, OUT>) mRoutine.invoke();
    }
  }
}
