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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.builder.AbstractStatefulBuilder;
import com.github.dm.jrt.function.builder.StatefulFactoryBuilder;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.wrapBiFunction;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;
import static com.github.dm.jrt.function.util.FunctionDecorator.wrapFunction;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;
import static com.github.dm.jrt.function.util.TriFunctionDecorator.wrapTriFunction;

/**
 * Default implementation of a stateful factory builder.
 * <p>
 * Created by davide-maestroni on 05/23/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
class DefaultStatefulFactoryBuilder<IN, OUT, STATE>
    extends AbstractStatefulBuilder<IN, OUT, STATE, StatefulFactoryBuilder<IN, OUT, STATE>>
    implements StatefulFactoryBuilder<IN, OUT, STATE> {

  @NotNull
  public InvocationFactory<IN, OUT> create() {
    return new StatefulInvocationFactory<IN, OUT, STATE>(getOnCreate(), getOnNext(), getOnError(),
        getOnComplete(), getOnFinalize(), getOnDestroy());
  }

  /**
   * Stateful invocation implementation.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulInvocation<IN, OUT, STATE> implements Invocation<IN, OUT> {

    private final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final Supplier<? extends STATE> mOnCreate;

    private final Consumer<? super STATE> mOnDestroy;

    private final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> mOnError;

    private final Function<? super STATE, ? extends STATE> mOnFinalize;

    private final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
        mOnNext;

    private STATE mState;

    /**
     * Constructor.
     *
     * @param onCreate   the state supplier.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulInvocation(@NotNull final Supplier<? extends STATE> onCreate,
        @NotNull final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends
            STATE> onNext,
        @NotNull final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError,
        @NotNull final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
            onComplete,
        @NotNull final Function<? super STATE, ? extends STATE> onFinalize,
        @NotNull final Consumer<? super STATE> onDestroy) {
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnFinalize = onFinalize;
      mOnDestroy = onDestroy;
    }

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mState = mOnError.apply(mState, reason);
    }

    public void onComplete(@NotNull final Channel<OUT, ?> result) throws Exception {
      mState = mOnComplete.apply(mState, result);
    }

    public void onDestroy() throws Exception {
      mOnDestroy.accept(mState);
    }

    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) throws Exception {
      mState = mOnNext.apply(mState, input, result);
    }

    public boolean onRecycle() throws Exception {
      mState = mOnFinalize.apply(mState);
      return true;
    }

    public void onStart() throws Exception {
      if (mState == null) {
        mState = mOnCreate.get();
      }
    }
  }

  /**
   * Factory of stateful invocations.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulInvocationFactory<IN, OUT, STATE>
      extends InvocationFactory<IN, OUT> {

    private final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> mOnComplete;

    private final Supplier<? extends STATE> mOnCreate;

    private final Consumer<? super STATE> mOnDestroy;

    private final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> mOnError;

    private final Function<? super STATE, ? extends STATE> mOnFinalize;

    private final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
        mOnNext;

    /**
     * Constructor.
     *
     * @param onCreate   the state supplier.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulInvocationFactory(@NotNull final Supplier<? extends STATE> onCreate,
        @NotNull final TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends
            STATE> onNext,
        @NotNull final BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError,
        @NotNull final BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
            onComplete,
        @NotNull final Function<? super STATE, ? extends STATE> onFinalize,
        @NotNull final Consumer<? super STATE> onDestroy) {
      super(asArgs(wrapSupplier(onCreate), wrapTriFunction(onNext), wrapBiFunction(onError),
          wrapBiFunction(onComplete), wrapFunction(onFinalize), wrapConsumer(onDestroy)));
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnFinalize = onFinalize;
      mOnDestroy = onDestroy;
    }

    @NotNull
    public Invocation<IN, OUT> newInvocation() {
      return new StatefulInvocation<IN, OUT, STATE>(mOnCreate, mOnNext, mOnError, mOnComplete,
          mOnFinalize, mOnDestroy);
    }
  }
}
