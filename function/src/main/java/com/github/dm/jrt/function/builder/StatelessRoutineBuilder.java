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
import com.github.dm.jrt.function.lambda.BiConsumer;
import com.github.dm.jrt.function.lambda.Consumer;
import com.github.dm.jrt.function.lambda.Function;
import com.github.dm.jrt.function.lambda.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Builder of stateless routines based on functions handling the invocation lifecycle.
 * <p>
 * For example, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * builder.onNextOutput(String::toUpperCase)
 *        .buildRoutine();
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/24/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StatelessRoutineBuilder<IN, OUT> extends RoutineBuilder<IN, OUT> {

  /**
   * {@inheritDoc}
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> apply(@NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  Builder<? extends StatelessRoutineBuilder<IN, OUT>> invocationConfiguration();

  /**
   * Sets the consumer to call when the invocation completes.
   *
   * @param onComplete the consumer instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onComplete(
      @NotNull Consumer<? super Channel<OUT, ?>> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onCompleteArray(@NotNull Supplier<OUT[]> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onCompleteIterable(
      @NotNull Supplier<? extends Iterable<? extends OUT>> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned output is automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onCompleteOutput(@NotNull Supplier<OUT> onComplete);

  /**
   * Sets the consumer to call when the invocation is aborted with an error.
   *
   * @param onError the consumer instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onError(@NotNull Consumer<? super RoutineException> onError);

  /**
   * Sets the consumer to call when a new input is received.
   *
   * @param onNext the consumer instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onNext(
      @NotNull BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onNextArray(@NotNull Function<? super IN, OUT[]> onNext);

  /**
   * Sets the consumer to call when a new input is received.
   *
   * @param onNext the consumer instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onNextConsume(@NotNull Consumer<? super IN> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onNextIterable(
      @NotNull Function<? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned output is automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  StatelessRoutineBuilder<IN, OUT> onNextOutput(
      @NotNull Function<? super IN, ? extends OUT> onNext);
}
