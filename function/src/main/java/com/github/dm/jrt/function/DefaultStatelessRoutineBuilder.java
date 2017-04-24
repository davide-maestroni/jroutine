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
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.builder.AbstractStatelessRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.ConsumerDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

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

  /**
   * Constructor.
   */
  DefaultStatelessRoutineBuilder() {
  }

  @NotNull
  public Routine<IN, OUT> buildRoutine() {
    final StatelessInvocation<IN, OUT> invocation =
        new StatelessInvocation<IN, OUT>(getOnNext(), getOnError(), getOnComplete());
    return JRoutineCore.with(invocation).apply(getConfiguration()).buildRoutine();
  }

  /**
   * Stateless invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StatelessInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
      implements Invocation<IN, OUT> {

    private final ConsumerDecorator<? super Channel<OUT, ?>> mOnComplete;

    private final ConsumerDecorator<? super RoutineException> mOnError;

    private final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> mOnNext;

    /**
     * Constructor.
     *
     * @param onNext     the next consumer.
     * @param onError    the error consumer.
     * @param onComplete the complete consumer.
     */
    private StatelessInvocation(
        @NotNull final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> onNext,
        @NotNull final ConsumerDecorator<? super RoutineException> onError,
        @NotNull final ConsumerDecorator<? super Channel<OUT, ?>> onComplete) {
      super(asArgs(onNext, onError, onComplete));
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
