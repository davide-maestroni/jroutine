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

import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Builder of stateless routines based on functions handling the invocation lifecycle.
 * <br>
 * The function instances must have a static scope in order to avoid undesired leaks.
 * <p>
 * For example, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * builder.onNextOutput(String::toUpperCase)
 *        .routine();
 * </code></pre>
 * <p>
 * Note that the passed instances are expected to behave like a function, that is, they must not
 * retain a mutable internal state.
 * <br>
 * Note also that any external object used inside the function must be synchronized in order to
 * avoid concurrency issues.
 * <p>
 * Created by davide-maestroni on 03/06/2017.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface StatelessLoaderRoutineBuilder<IN, OUT> extends StatelessRoutineBuilder<IN, OUT>,
    LoaderConfigurable<StatelessLoaderRoutineBuilder<IN, OUT>> {

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onComplete(
      @NotNull Consumer<? super Channel<OUT, ?>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onCompleteArray(@NotNull Supplier<OUT[]> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onCompleteIterable(
      @NotNull Supplier<? extends Iterable<? extends OUT>> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onCompleteOutput(
      @NotNull Supplier<? extends OUT> onComplete);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onError(
      @NotNull Consumer<? super RoutineException> onError);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onNext(
      @NotNull BiConsumer<? super IN, ? super Channel<OUT, ?>> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onNextArray(@NotNull Function<? super IN, OUT[]> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onNextConsume(@NotNull Consumer<? super IN> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onNextIterable(
      @NotNull Function<? super IN, ? extends Iterable<? extends OUT>> onNext);

  /**
   * {@inheritDoc}
   *
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  @Override
  StatelessLoaderRoutineBuilder<IN, OUT> onNextOutput(
      @NotNull Function<? super IN, ? extends OUT> onNext);

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
  StatelessLoaderRoutineBuilder<IN, OUT> withConfiguration(
      @NotNull InvocationConfiguration configuration);

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  Builder<? extends StatelessLoaderRoutineBuilder<IN, OUT>> withInvocation();
}
