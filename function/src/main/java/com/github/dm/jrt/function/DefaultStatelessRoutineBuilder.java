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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.builder.AbstractStatelessRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiConsumerDecorator.wrapBiConsumer;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;

/**
 * Default implementation of a stateless routine builder.
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessRoutineBuilder<IN, OUT>
    extends AbstractStatelessRoutineBuilder<IN, OUT, StatelessRoutineBuilder<IN, OUT>> {

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultStatelessRoutineBuilder(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public Routine<IN, OUT> routine() {
    final StatelessInvocation<IN, OUT> invocation =
        new StatelessInvocation<IN, OUT>(getOnNext(), getOnError(), getOnComplete());
    return JRoutineCore.routineOn(mExecutor).withConfiguration(getConfiguration()).of(invocation);
  }

  /**
   * Stateless invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StatelessInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
      implements Invocation<IN, OUT> {

    private final Consumer<? super Channel<OUT, ?>> mOnComplete;

    private final Consumer<? super RoutineException> mOnError;

    private final BiConsumer<? super IN, ? super Channel<OUT, ?>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext     the next consumer.
     * @param onError    the error consumer.
     * @param onComplete the complete consumer.
     */
    private StatelessInvocation(
        @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext,
        @NotNull final Consumer<? super RoutineException> onError,
        @NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
      super(asArgs(wrapBiConsumer(onNext), wrapConsumer(onError), wrapConsumer(onComplete)));
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return this;
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mOnError.accept(reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      mOnComplete.accept(result);
    }

    public void onDestroy() {
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
      mOnNext.accept(input, result);
    }

    public boolean onRecycle() {
      return true;
    }

    public void onStart() {
    }
  }
}
