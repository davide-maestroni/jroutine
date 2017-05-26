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

package com.github.dm.jrt.android.function.builder;

import android.content.Context;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.builder.StatefulBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a stateful functional builder.
 * <p>
 * Created by davide-maestroni on 05/25/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 * @param <TYPE>  the builder type.
 */
public interface StatefulContextBuilder<IN, OUT, STATE, TYPE extends StatefulContextBuilder<IN,
    OUT, STATE, TYPE>>
    extends StatefulBuilder<IN, OUT, STATE, TYPE> {

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onComplete(
      @NotNull BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteArray(@NotNull Function<? super STATE, OUT[]> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteConsume(@NotNull BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteIterable(
      @NotNull Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteOutput(@NotNull Function<? super STATE, ? extends OUT> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteState(@NotNull Function<? super STATE, ? extends STATE> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCreate(@NotNull Supplier<? extends STATE> onCreate);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onDestroy(@NotNull Consumer<? super STATE> onDestroy);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onError(
      @NotNull BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onErrorConsume(@NotNull BiConsumer<? super STATE, ? super RoutineException> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onErrorException(@NotNull Function<? super RoutineException, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onErrorState(@NotNull Function<? super STATE, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onFinalize(@NotNull Function<? super STATE, ? extends STATE> onFinalize);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onFinalizeConsume(@NotNull Consumer<? super STATE> onFinalize);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNext(
      @NotNull TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
          onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextArray(@NotNull BiFunction<? super STATE, ? super IN, OUT[]> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextConsume(@NotNull BiConsumer<? super STATE, ? super IN> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextIterable(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextOutput(@NotNull BiFunction<? super STATE, ? super IN, ? extends OUT> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextState(@NotNull BiFunction<? super STATE, ? super IN, ? extends STATE> onNext);

  /**
   * Sets the function to call when the Context instance is passed to the invocation.
   * <p>
   * The Context is passed only once after the invocation has been instantiated.
   * <br>
   * The returned state object is retained and passed to any successive calls to the set functions.
   *
   * @param onContext the function instance.
   * @return this builder.
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onContext(Context)
   * onContext(Context)
   */
  @NotNull
  TYPE onContext(@NotNull Function<? super Context, ? extends STATE> onContext);

  /**
   * Sets the consumer to call when the Context instance is passed to the invocation.
   * <p>
   * The Context is passed only once after the invocation has been instantiated.
   *
   * @param onContext the function instance.
   * @return this builder.
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onContext(Context)
   * onContext(Context)
   */
  @NotNull
  TYPE onContextConsume(@NotNull Consumer<? super Context> onContext);

  /**
   * Sets the function to call when the invocation starts.
   * <br>
   * If a state object has been retained from the previous invocation or from the Context
   * notification, the same instance will be passed to the function.
   *
   * @param onCreate the function instance.
   * @return this builder.
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onStart() onStart()
   */
  @NotNull
  TYPE onCreateState(@NotNull Function<? super STATE, ? extends STATE> onCreate);
}
