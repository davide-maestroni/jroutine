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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.builder.FunctionalChannelConsumer;
import com.github.dm.jrt.function.builder.StatefulFactoryBuilder;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessFactoryBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Supplier;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * This utility class provides a few ways to easily implement routines and channel consumer through
 * functional interfaces.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * JRoutineFunction.&lt;String, String, StringBuilder&gt;statefulRoutine()
 *                 .onCreate(StringBuilder::new)
 *                 .onNextState(StringBuilder::append)
 *                 .onCompleteOutput(StringBuilder::toString)
 *                 .create();
 * </code></pre>
 * <p>
 * In a similar way, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * JRoutineFunction.&lt;String, String,&gt;statelessRoutine()
 *                 .onNextOutput(String::toUpperCase)
 *                 .create();
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 */
public class JRoutineFunction {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineFunction() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a new invocation factory based on the specified supplier instance.
   * <br>
   * It's up to the caller to prevent undesired leaks.
   * <p>
   * Note that the passed object is expected to behave like a function, that is, it must not retain
   * a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the invocation factory.
   */
  @NotNull
  public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
      @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {
    return new SupplierInvocationFactory<IN, OUT>(supplier);
  }

  /**
   * Returns a channel consumer builder employing the specified action to handle the invocation
   * completion.
   *
   * @param onComplete the action instance.
   * @return the channel consumer builder.
   */
  @NotNull
  public static FunctionalChannelConsumer<Object> onComplete(@NotNull final Action onComplete) {
    return DefaultFunctionalChannelConsumer.onComplete(onComplete);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation errors.
   *
   * @param onError the consumer function.
   * @return the channel consumer builder.
   */
  @NotNull
  public static FunctionalChannelConsumer<Object> onError(
      @NotNull final Consumer<? super RoutineException> onError) {
    return DefaultFunctionalChannelConsumer.onError(onError);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation outputs.
   *
   * @param onOutput the consumer function.
   * @param onError  the consumer function.
   * @param <OUT>    the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError) {
    return DefaultFunctionalChannelConsumer.onOutput(onOutput, onError);
  }

  /**
   * Returns a channel consumer builder employing the specified functions to handle the invocation
   * outputs, errors adn completion.
   *
   * @param onOutput   the consumer function.
   * @param onError    the consumer function.
   * @param onComplete the action instance.
   * @param <OUT>      the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError, @NotNull final Action onComplete) {
    return DefaultFunctionalChannelConsumer.onOutput(onOutput, onError, onComplete);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation outputs.
   *
   * @param onOutput the consumer function.
   * @param <OUT>    the output data type.
   * @return the channel consumer builder.
   */
  @NotNull
  public static <OUT> FunctionalChannelConsumer<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput) {
    return DefaultFunctionalChannelConsumer.onOutput(onOutput);
  }

  /**
   * Returns a builder of stateful invocation factories.
   * <p>
   * This invocations retain a mutable state during their lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulFactoryBuilder<IN, OUT, STATE> statefulFactory() {
    return new DefaultStatefulFactoryBuilder<IN, OUT, STATE>();
  }

  /**
   * Returns a builder of stateful routines running on the default executor.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulRoutineBuilder<IN, OUT, STATE> statefulRoutine() {
    return statefulRoutineOn(defaultExecutor());
  }

  /**
   * Returns a builder of stateful routines.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful invocation is the one computing a final result by accumulating
   * the input data (for instance, computing the sum of input numbers).
   *
   * @param executor the executor instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @param <STATE>  the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulRoutineBuilder<IN, OUT, STATE> statefulRoutineOn(
      @NotNull final ScheduledExecutor executor) {
    return new DefaultStatefulRoutineBuilder<IN, OUT, STATE>(executor);
  }

  /**
   * Returns a builder of stateless invocation factories.
   * <p>
   * This invocations do not retain a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessFactoryBuilder<IN, OUT> statelessFactory() {
    return new DefaultStatelessFactoryBuilder<IN, OUT>();
  }

  /**
   * Returns a builder of stateless routines running on the default executor.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessRoutineBuilder<IN, OUT> statelessRoutine() {
    return statelessRoutineOn(defaultExecutor());
  }

  /**
   * Returns a builder of stateless routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless invocation is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param executor the executor instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessRoutineBuilder<IN, OUT> statelessRoutineOn(
      @NotNull final ScheduledExecutor executor) {
    return new DefaultStatelessRoutineBuilder<IN, OUT>(executor);
  }

  /**
   * Implementation of an invocation factory based on a supplier function.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class SupplierInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final Supplier<? extends Invocation<? super IN, ? extends OUT>> mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the supplier function.
     */
    private SupplierInvocationFactory(
        @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {
      super(asArgs(wrapSupplier(supplier)));
      mSupplier = supplier;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Invocation<IN, OUT> newInvocation() throws Exception {
      return (Invocation<IN, OUT>) mSupplier.get();
    }
  }
}
