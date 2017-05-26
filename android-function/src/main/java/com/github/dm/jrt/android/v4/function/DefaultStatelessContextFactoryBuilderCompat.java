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

package com.github.dm.jrt.android.v4.function;

import android.content.Context;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.function.builder.AbstractStatelessContextBuilder;
import com.github.dm.jrt.android.function.builder.StatelessContextFactoryBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiConsumerDecorator.wrapBiConsumer;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;

/**
 * Default implementation of a stateless Context factory builder.
 * <p>
 * Created by davide-maestroni on 05/25/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessContextFactoryBuilderCompat<IN, OUT>
    extends AbstractStatelessContextBuilder<IN, OUT, StatelessContextFactoryBuilder<IN, OUT>>
    implements StatelessContextFactoryBuilder<IN, OUT> {

  @NotNull
  @Override
  public ContextInvocationFactory<IN, OUT> create() {
    return new StatelessContextInvocation<IN, OUT>(getOnNext(), getOnError(), getOnComplete());
  }

  /**
   * Stateless Context invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StatelessContextInvocation<IN, OUT> extends ContextInvocationFactory<IN, OUT>
      implements ContextInvocation<IN, OUT> {

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
    private StatelessContextInvocation(
        @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext,
        @NotNull final Consumer<? super RoutineException> onError,
        @NotNull final Consumer<? super Channel<OUT, ?>> onComplete) {
      super(asArgs(wrapBiConsumer(onNext), wrapConsumer(onError), wrapConsumer(onComplete)));
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() {
      return this;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mOnError.accept(reason);
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      mOnComplete.accept(result);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
      mOnNext.accept(input, result);
    }

    @Override
    public boolean onRecycle() {
      return false;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onContext(@NotNull final Context context) {
    }
  }
}
