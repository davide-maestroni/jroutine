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
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a stateless functional builder.
 * <p>
 * Created by davide-maestroni on 05/23/2017.
 *
 * @param <IN>   the input data type.
 * @param <OUT>  the output data type.
 * @param <TYPE> the builder type.
 */
public interface StatelessBuilder<IN, OUT, TYPE extends StatelessBuilder<IN, OUT, TYPE>> {

  /**
   * Sets the consumer to call when the invocation completes.
   *
   * @param onComplete the consumer instance.
   * @return this builder.
   */
  @NotNull
  TYPE onComplete(@NotNull Consumer<? super Channel<OUT, ?>> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  TYPE onCompleteArray(@NotNull Supplier<OUT[]> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  TYPE onCompleteIterable(@NotNull Supplier<? extends Iterable<? extends OUT>> onComplete);

  /**
   * Sets the supplier to call when the invocation completes.
   * <br>
   * The returned output is automatically passed to the result channel.
   *
   * @param onComplete the supplier instance.
   * @return this builder.
   */
  @NotNull
  TYPE onCompleteOutput(@NotNull Supplier<? extends OUT> onComplete);

  /**
   * Sets the consumer to call when the invocation is aborted with an error.
   *
   * @param onError the consumer instance.
   * @return this builder.
   */
  @NotNull
  TYPE onError(@NotNull Consumer<? super RoutineException> onError);

  /**
   * Sets the consumer to call when a new input is received.
   *
   * @param onNext the consumer instance.
   * @return this builder.
   */
  @NotNull
  TYPE onNext(@NotNull BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  TYPE onNextArray(@NotNull Function<? super IN, OUT[]> onNext);

  /**
   * Sets the consumer to call when a new input is received.
   *
   * @param onNext the consumer instance.
   * @return this builder.
   */
  @NotNull
  TYPE onNextConsume(@NotNull Consumer<? super IN> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned outputs are automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  TYPE onNextIterable(@NotNull Function<? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * Sets the function to call when a new input is received.
   * <br>
   * The returned output is automatically passed to the result channel.
   *
   * @param onNext the function instance.
   * @return this builder.
   */
  @NotNull
  TYPE onNextOutput(@NotNull Function<? super IN, ? extends OUT> onNext);
}
