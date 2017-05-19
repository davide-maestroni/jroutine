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

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Builder of stateful routines based on functions handling the invocation lifecycle.
 * <br>
 * The function instances must have a static scope in order to avoid undesired leaks.
 * <br>
 * Be also aware that the state instance will not be retained between invocations, since a Loader
 * routine destroys its invocations as soon as they complete.
 * <p>
 * The state object is created when the invocation starts and modified during the execution.
 * <br>
 * The last instance returned by the finalization function is retained and re-used during the next
 * invocation execution, unless null, in which case a new instance is created.
 * <p>
 * By default, the same state is retained through the whole invocation lifecycle and automatically
 * nulled during the finalization step. Hence, it is advisable to customize the finalization
 * function, in order to be able to re-use the same state instances through successive invocation
 * executions.
 * <br>
 * Note, however, that the state object should be reset on finalization in order to avoid
 * unpredictable behaviors during different invocations.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * builder.onCreate(StringBuilder::new)
 *        .onNextState(StringBuilder::append)
 *        .onCompleteOutput(StringBuilder::toString)
 *        .routine();
 * </code></pre>
 * <p>
 * Note that the passed instances are expected to behave like a function, that is, they must not
 * retain a mutable internal state.
 * <br>
 * Note also that any external object used inside the function must be synchronized in order to
 * avoid concurrency issues.
 * <p>
 * Created by davide-maestroni on 03/04/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
public interface StatefulLoaderRoutineBuilder<IN, OUT, STATE>
    extends StatefulRoutineBuilder<IN, OUT, STATE>,
    LoaderConfigurable<StatefulLoaderRoutineBuilder<IN, OUT, STATE>> {

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
  LoaderRoutine<IN, OUT> routine();

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
  Builder<? extends StatefulLoaderRoutineBuilder<IN, OUT, STATE>> withInvocation();

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
