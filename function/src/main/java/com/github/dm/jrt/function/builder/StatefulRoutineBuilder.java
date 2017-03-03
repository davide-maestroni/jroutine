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

package com.github.dm.jrt.function.builder;

import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Builder of stateful routines based on functions handling the invocation lifecycle.
 * <p>
 * The state object is created when the invocation starts and modified during the execution.
 * <br>
 * The last instance returned by the finalization function is retained and re-used during the next
 * invocation execution, unless null, in which case a new instance is created.
 * <p>
 * By default, the same state is retained through the whole invocation lifecycle and automatically
 * nulled during the finalization step. Hence, it is advisable to call the
 * {@link #onFinalizeRetain()} method, or to customize the finalization function, in order to be
 * able to re-use the same state instances through successive invocation executions.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * builder.onCreate(StringBuilder::new)
 *        .onNextState(StringBuilder::append)
 *        .onCompleteOutput(StringBuilder::toString)
 *        .buildRoutine();
 * </code></pre>
 * <p>
 * Note that the passed instances are expected to behave like a function, that is, they must not
 * retain a mutable internal state.
 * <br>
 * Note also that any external object used inside the function must be synchronized in order to
 * avoid concurrency issues.
 * <p>
 * Created by davide-maestroni on 02/23/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 */
public interface StatefulRoutineBuilder<IN, OUT, STATE> extends RoutineBuilder<IN, OUT> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> apply(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  Builder<? extends StatefulRoutineBuilder<IN, OUT, STATE>> invocationConfiguration();

  /**
   * Sets the function to call when the invocation completes.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onComplete(
      @NotNull BiFunction<? super STATE, ? super Channel<OUT, ?>, ? extends STATE> onComplete);

  /**
   * Sets the function to call when the invocation completes.
   * <br>
   * The returned outputs are passed to the result channel, while the state object is automatically
   * retained.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCompleteArray(
      @NotNull Function<? super STATE, OUT[]> onComplete);

  /**
   * Sets the consumer to call when the invocation completes.
   * <br>
   * The state object is automatically retained.
   *
   * @param onComplete the consumer instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCompleteConsume(
      @NotNull BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete);

  /**
   * Sets the function to call when the invocation completes.
   * <br>
   * The returned outputs are passed to the result channel, while the state object is automatically
   * retained.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCompleteIterable(
      @NotNull Function<? super STATE, ? extends Iterable<? extends OUT>> onComplete);

  /**
   * Sets the function to call when the invocation completes.
   * <br>
   * The returned output is passed to the result channel, while the state object is automatically
   * retained.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCompleteOutput(
      @NotNull Function<? super STATE, ? extends OUT> onComplete);

  /**
   * Sets the function to call when the invocation completes.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCompleteState(
      @NotNull Function<? super STATE, ? extends STATE> onComplete);

  /**
   * Sets the supplier to call when the invocation starts.
   * <br>
   * If a state object has been retained from the previous invocation, the function is not called.
   *
   * @param onCreate the supplier instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onRestart() onRestart()
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onCreate(@NotNull Supplier<? extends STATE> onCreate);

  /**
   * Sets the consumer to call when the invocation is destroyed.
   * <br>
   * The consumer is called even if the stored state is null.
   *
   * @param onDestroy the consumer instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onDestroy() onDestroy()
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onDestroy(@NotNull Consumer<? super STATE> onDestroy);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onError(
      @NotNull BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onErrorException(
      @NotNull Function<? super RoutineException, ? extends STATE> onError);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onErrorState(
      @NotNull Function<? super STATE, ? extends STATE> onError);

  /**
   * Sets the function to call after the invocation has completed.
   * <br>
   * The returned state object is stored for the next invocation unless null is returned.
   * <br>
   * The function is called even if the stored state is null.
   *
   * @param onFinalize the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onRecycle() onRecycle()
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onFinalize(
      @NotNull Function<? super STATE, ? extends STATE> onFinalize);

  /**
   * Sets the consumer to call after the invocation has completed.
   * <br>
   * The state object is automatically set to null.
   *
   * @param onFinalize the consumer instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onRecycle() onRecycle()
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onFinalizeConsume(
      @NotNull Consumer<? super STATE> onFinalize);

  /**
   * Sets the function to call after the invocation has completed, so to retain the last state
   * instance.
   *
   * @return this builder.
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onFinalizeRetain();

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned state object is retained and passed to any successive calls to the set functions.
   *
   * @param onNext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNext(
      @NotNull TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends
          STATE> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are passed to the result channel, while the state object is automatically
   * retained and passed to any successive calls to the set functions.
   *
   * @param onNext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNextArray(
      @NotNull BiFunction<? super STATE, ? super IN, OUT[]> onNext);

  /**
   * Sets the consumer to call when a new input is received.
   * <br>
   * The same state object is retained and passed to any successive calls to the set functions.
   *
   * @param onNext the consumer instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNextConsume(
      @NotNull BiConsumer<? super STATE, ? super IN> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are passed to the result channel, while the state object is automatically
   * retained and passed to any successive calls to the set functions.
   *
   * @param onNext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNextIterable(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned output is passed to the result channel, while the state object is automatically
   * retained and passed to any successive calls to the set functions.
   *
   * @param onNext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNextOutput(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends OUT> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned state object is retained and passed to any successive calls to the set functions.
   *
   * @param onNext the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onInput(Object, Channel)
   * onInput(Object, Channel)
   */
  @NotNull
  StatefulRoutineBuilder<IN, OUT, STATE> onNextState(
      @NotNull BiFunction<? super STATE, ? super IN, ? extends
          STATE> onNext);
}
