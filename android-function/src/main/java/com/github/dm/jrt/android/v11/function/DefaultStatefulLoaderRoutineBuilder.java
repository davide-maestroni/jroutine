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

package com.github.dm.jrt.android.v11.function;

import android.content.Context;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.function.builder.AbstractStatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.TriFunctionDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Default implementation of a stateful Loader routine builder.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
class DefaultStatefulLoaderRoutineBuilder<IN, OUT, STATE> extends
    AbstractStatefulLoaderRoutineBuilder<IN, OUT, STATE, StatefulLoaderRoutineBuilder<IN, OUT,
        STATE>> {

  private final LoaderContext mLoaderContext;

  /**
   * Constructor.
   *
   * @param loaderContext the Loader context.
   */
  DefaultStatefulLoaderRoutineBuilder(@NotNull final LoaderContext loaderContext) {
    mLoaderContext = ConstantConditions.notNull("Loader context", loaderContext);
  }

  @NotNull
  @Override
  public LoaderRoutine<IN, OUT> buildRoutine() {
    return JRoutineLoader.on(mLoaderContext)
                         .with(new StatefulContextInvocationFactory<IN, OUT, STATE>(getOnContext(),
                             getOnCreateState(), getOnNext(), getOnError(), getOnComplete(),
                             getOnFinalize(), getOnDestroy()))
                         .apply(getConfiguration())
                         .apply(getLoaderConfiguration())
                         .buildRoutine();
  }

  /**
   * Stateful Context invocation implementation.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulContextInvocation<IN, OUT, STATE>
      implements ContextInvocation<IN, OUT> {

    private final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
        mOnComplete;

    private final FunctionDecorator<? super Context, ? extends STATE> mOnContext;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super RoutineException, ? extends STATE>
        mOnError;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnFinalize;

    private final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
        extends STATE>
        mOnNext;

    private STATE mState;

    /**
     * Constructor.
     *
     * @param onContext  the Context function.
     * @param onCreate   the state function.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulContextInvocation(
        @NotNull final FunctionDecorator<? super Context, ? extends STATE> onContext,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onCreate,
        @NotNull final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
            extends STATE> onNext,
        @NotNull final BiFunctionDecorator<? super STATE, ? super RoutineException, ? extends
            STATE> onError,
        @NotNull final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends
            STATE> onComplete,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onFinalize,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      mOnContext = onContext;
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

    public void onRestart() throws Exception {
      mState = mOnCreate.apply(mState);
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      mState = mOnContext.apply(context);
    }
  }

  /**
   * Factory of stateful Context invocations.
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   */
  private static class StatefulContextInvocationFactory<IN, OUT, STATE>
      extends ContextInvocationFactory<IN, OUT> {

    private final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends STATE>
        mOnComplete;

    private final FunctionDecorator<? super Context, ? extends STATE> mOnContext;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnCreate;

    private final ConsumerDecorator<? super STATE> mOnDestroy;

    private final BiFunctionDecorator<? super STATE, ? super RoutineException, ? extends STATE>
        mOnError;

    private final FunctionDecorator<? super STATE, ? extends STATE> mOnFinalize;

    private final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
        extends STATE>
        mOnNext;

    /**
     * Constructor.
     *
     * @param onContext  the Context function.
     * @param onCreate   the state function.
     * @param onNext     the next function.
     * @param onError    the error function.
     * @param onComplete the completion function.
     * @param onFinalize the finalization function.
     * @param onDestroy  the destroy consumer.
     */
    private StatefulContextInvocationFactory(
        @NotNull final FunctionDecorator<? super Context, ? extends STATE> onContext,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onCreate,
        @NotNull final TriFunctionDecorator<? super STATE, ? super IN, ? super Channel<OUT, ?>, ?
            extends STATE> onNext,
        @NotNull final BiFunctionDecorator<? super STATE, ? super RoutineException, ? extends
            STATE> onError,
        @NotNull final BiFunctionDecorator<? super STATE, ? super Channel<OUT, ?>, ? extends
            STATE> onComplete,
        @NotNull final FunctionDecorator<? super STATE, ? extends STATE> onFinalize,
        @NotNull final ConsumerDecorator<? super STATE> onDestroy) {
      super(asArgs(onContext, onCreate, onNext, onError, onComplete, onFinalize, onDestroy));
      mOnContext = onContext;
      mOnCreate = onCreate;
      mOnNext = onNext;
      mOnError = onError;
      mOnComplete = onComplete;
      mOnFinalize = onFinalize;
      mOnDestroy = onDestroy;
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() throws Exception {
      return new StatefulContextInvocation<IN, OUT, STATE>(mOnContext, mOnCreate, mOnNext, mOnError,
          mOnComplete, mOnFinalize, mOnDestroy);
    }
  }
}
