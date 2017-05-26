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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.builder.StatelessBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a stateless functional builder.
 * <p>
 * Created by davide-maestroni on 05/25/2017.
 *
 * @param <IN>   the input data type.
 * @param <OUT>  the output data type.
 * @param <TYPE> the builder type.
 */
public interface StatelessContextBuilder<IN, OUT, TYPE extends StatelessContextBuilder<IN, OUT,
    TYPE>>
    extends StatelessBuilder<IN, OUT, TYPE> {

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onComplete(@NotNull Consumer<? super Channel<OUT, ?>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteArray(@NotNull Supplier<OUT[]> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteIterable(@NotNull Supplier<? extends Iterable<? extends OUT>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onCompleteOutput(@NotNull Supplier<? extends OUT> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onError(@NotNull Consumer<? super RoutineException> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNext(@NotNull BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextArray(@NotNull Function<? super IN, OUT[]> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextConsume(@NotNull Consumer<? super IN> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextIterable(@NotNull Function<? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  TYPE onNextOutput(@NotNull Function<? super IN, ? extends OUT> onNext);
}
