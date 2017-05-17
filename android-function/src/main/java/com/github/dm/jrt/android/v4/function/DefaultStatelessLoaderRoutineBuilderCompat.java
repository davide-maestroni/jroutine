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
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.function.builder.AbstractStatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.ConsumerDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a stateless Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultStatelessLoaderRoutineBuilderCompat<IN, OUT>
    extends AbstractStatelessLoaderRoutineBuilder<IN, OUT, StatelessLoaderRoutineBuilder<IN, OUT>> {

  private final LoaderSourceCompat mLoaderSource;

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultStatelessLoaderRoutineBuilderCompat(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @NotNull
  @Override
  public LoaderRoutine<IN, OUT> buildRoutine() {
    return JRoutineLoaderCompat.on(mLoaderSource)
                               .with(new StatelessContextInvocation<IN, OUT>(getOnNext(),
                                   getOnError(), getOnComplete()))
                               .withConfiguration(getConfiguration())
                               .withConfiguration(getLoaderConfiguration())
                               .buildRoutine();
  }

  /**
   * Stateless Context invocation implementation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class StatelessContextInvocation<IN, OUT> extends ContextInvocationFactory<IN, OUT>
      implements ContextInvocation<IN, OUT> {

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
    private StatelessContextInvocation(
        @NotNull final BiConsumerDecorator<? super IN, ? super Channel<OUT, ?>> onNext,
        @NotNull final ConsumerDecorator<? super RoutineException> onError,
        @NotNull final ConsumerDecorator<? super Channel<OUT, ?>> onComplete) {
      super(asArgs(onNext, onError, onComplete));
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
