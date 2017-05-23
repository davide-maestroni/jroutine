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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
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
 * Created by davide-maestroni on 05/23/2017.
 *
 * @param <IN>    the input data type.
 * @param <OUT>   the output data type.
 * @param <STATE> the state data type.
 * @param <TYPE>  the builder type.
 */
public interface StatefulBuilder<IN, OUT, STATE, TYPE extends StatefulBuilder<IN, OUT, STATE,
    TYPE>> {

  /**
   * Sets the function to call when the invocation completes.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  TYPE onComplete(
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
  TYPE onCompleteArray(@NotNull Function<? super STATE, OUT[]> onComplete);

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
  TYPE onCompleteConsume(@NotNull BiConsumer<? super STATE, ? super Channel<OUT, ?>> onComplete);

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
  TYPE onCompleteIterable(
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
  TYPE onCompleteOutput(@NotNull Function<? super STATE, ? extends OUT> onComplete);

  /**
   * Sets the function to call when the invocation completes.
   *
   * @param onComplete the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onComplete(Channel) onComplete(Channel)
   */
  @NotNull
  TYPE onCompleteState(@NotNull Function<? super STATE, ? extends STATE> onComplete);

  /**
   * Sets the supplier to call when the invocation starts.
   * <br>
   * If a state object has been retained from the previous invocation, the function is not called.
   *
   * @param onCreate the supplier instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onStart() onStart()
   */
  @NotNull
  TYPE onCreate(@NotNull Supplier<? extends STATE> onCreate);

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
  TYPE onDestroy(@NotNull Consumer<? super STATE> onDestroy);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  TYPE onError(
      @NotNull BiFunction<? super STATE, ? super RoutineException, ? extends STATE> onError);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   * <br>
   * The state object is automatically retained.
   *
   * @param onError the consumer instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  TYPE onErrorConsume(@NotNull BiConsumer<? super STATE, ? super RoutineException> onError);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  TYPE onErrorException(@NotNull Function<? super RoutineException, ? extends STATE> onError);

  /**
   * Sets the function to call when the invocation is aborted with an error.
   *
   * @param onError the function instance.
   * @return this builder.
   * @see com.github.dm.jrt.core.invocation.Invocation#onAbort(RoutineException)
   * onAbort(RoutineException)
   */
  @NotNull
  TYPE onErrorState(@NotNull Function<? super STATE, ? extends STATE> onError);

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
  TYPE onFinalize(@NotNull Function<? super STATE, ? extends STATE> onFinalize);

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
  TYPE onFinalizeConsume(@NotNull Consumer<? super STATE> onFinalize);

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
  TYPE onNext(
      @NotNull TriFunction<? super STATE, ? super IN, ? super Channel<OUT, ?>, ? extends STATE>
          onNext);

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
  TYPE onNextArray(@NotNull BiFunction<? super STATE, ? super IN, OUT[]> onNext);

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
  TYPE onNextConsume(@NotNull BiConsumer<? super STATE, ? super IN> onNext);

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
  TYPE onNextIterable(
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
  TYPE onNextOutput(@NotNull BiFunction<? super STATE, ? super IN, ? extends OUT> onNext);

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
  TYPE onNextState(@NotNull BiFunction<? super STATE, ? super IN, ? extends STATE> onNext);
}
