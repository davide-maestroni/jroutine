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

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

/**
 * TODO: explain Loader invocations lifecycle
 * <p>
 * Created by davide-maestroni on 03/04/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
public interface StatefulLoaderRoutineBuilder<IN, OUT, STATE>
    extends StatefulRoutineBuilder<IN, OUT, STATE>, LoaderRoutineBuilder<IN, OUT> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> withConfiguration(
      @NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  InvocationConfiguration.Builder<? extends StatefulLoaderRoutineBuilder<IN, OUT, STATE>> withInvocation();

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onComplete(
      @NotNull BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCompleteArray(
      @NotNull Function<? super STATE, OUT[]> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCompleteConsume(
      @NotNull BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCompleteIterable(
      @NotNull Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCompleteOutput(
      @NotNull Function<? super STATE, ? extends OUT> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCompleteState(
      @NotNull Function<? super STATE, ? extends STATE> onComplete);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCreate(
      @NotNull Supplier<? extends STATE> onCreate);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onDestroy(
      @NotNull Consumer<? super STATE> onDestroy);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onError(
      @NotNull BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onErrorConsume(
      @NotNull BiConsumer<? super STATE, ? super RoutineException> onError);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onErrorException(
      @NotNull Function<? super RoutineException, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onErrorState(
      @NotNull Function<? super STATE, ? extends STATE> onError);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onFinalize(
      @NotNull Function<? super STATE, ? extends STATE> onFinalize);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onFinalizeConsume(
      @NotNull Consumer<? super STATE> onFinalize);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNext(
      @NotNull TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
          onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNextArray(
      @NotNull BiFunction<? super STATE, ? super IN, OUT[]> onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNextConsume(
      @NotNull BiConsumer<? super STATE, ? super IN> onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNextIterable(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNextOutput(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends OUT> onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onNextState(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends STATE> onNext);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> apply(@NotNull LoaderConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  LoaderConfiguration.Builder<? extends StatefulLoaderRoutineBuilder<IN, OUT, STATE>>
  loaderConfiguration();

  /**
   * Sets the function to call when the Context instance is passed to the invocation.
   * <p>
   * The Context is passed only once after the invocation has been instantiated.
   * <br>
   * The returned state object is retained and passed to any successive calls to the set functions.
   *
   * @param onContext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onContext(Context)
   * onContext(Context)
   */
  @NotNull
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onContext(
      @NotNull Function<? super Context, ? extends STATE> onContext);

  /**
   * Sets the consumer to call when the Context instance is passed to the invocation.
   * <p>
   * The Context is passed only once after the invocation has been instantiated.
   *
   * @param onContext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onContext(Context)
   * onContext(Context)
   */
  @NotNull
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onContextConsume(
      @NotNull Consumer<? super Context> onContext);

  /**
   * Sets the function to call when the invocation starts.
   * <br>
   * If a state object has been retained from the previous invocation or from the Context
   * notification, the same instance will be passed to the function.
   *
   * @param onCreate the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.android.core.invocation.ContextInvocation#onStart() onStart()
   */
  @NotNull
  StatefulLoaderRoutineBuilder<IN, OUT, STATE> onCreateState(
      @NotNull Function<? super STATE, ? extends STATE> onCreate);
}
