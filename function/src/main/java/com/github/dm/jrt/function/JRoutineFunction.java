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
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.builder.ChannelConsumerBuilder;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.builder.StatelessRoutineBuilder;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This utility class provides a few way to easily implement routines and channel consumer through
 * functional interfaces.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * JRoutineFunction.&lt;String, String, StringBuilder&gt;stateful()
 *                 .onCreate(StringBuilder::new)
 *                 .onNextState(StringBuilder::append)
 *                 .onCompleteOutput(StringBuilder::toString)
 *                 .buildRoutine();
 * </code></pre>
 * <p>
 * In a similar way, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * JRoutineFunction.&lt;String, String,&gt;stateless()
 *                 .onNextOutput(String::toUpperCase)
 *                 .buildRoutine();
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/27/2017.
 */
public class JRoutineFunction {

  private static final BiConsumerDecorator<? extends List<?>, ?> sListConsumer =
      BiConsumerDecorator.decorate(new BiConsumer<List<Object>, Object>() {

        public void accept(final List<Object> list, final Object input) {
          list.add(input);
        }
      });

  private static final SupplierDecorator<? extends List<?>> sListSupplier =
      SupplierDecorator.decorate(new Supplier<List<?>>() {

        public List<?> get() {
          return new ArrayList<Object>();
        }
      });

  /**
   * Avoid explicit instantiation.
   */
  private JRoutineFunction() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a channel consumer builder employing the specified action to handle the invocation
   * completion.
   *
   * @param onComplete the action instance.
   * @return the channel consumer builder.
   */
  @NotNull
  public static ChannelConsumerBuilder<Object> onComplete(@NotNull final Action onComplete) {
    return DefaultChannelConsumerBuilder.onComplete(onComplete);
  }

  /**
   * Returns a channel consumer builder employing the specified consumer function to handle the
   * invocation errors.
   *
   * @param onError the consumer function.
   * @return the channel consumer builder.
   */
  @NotNull
  public static ChannelConsumerBuilder<Object> onError(
      @NotNull final Consumer<? super RoutineException> onError) {
    return DefaultChannelConsumerBuilder.onError(onError);
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
  public static <OUT> ChannelConsumerBuilder<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError) {
    return DefaultChannelConsumerBuilder.onOutput(onOutput, onError);
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
  public static <OUT> ChannelConsumerBuilder<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput,
      @NotNull final Consumer<? super RoutineException> onError, @NotNull final Action onComplete) {
    return DefaultChannelConsumerBuilder.onOutput(onOutput, onError, onComplete);
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
  public static <OUT> ChannelConsumerBuilder<OUT> onOutput(
      @NotNull final Consumer<? super OUT> onOutput) {
    return DefaultChannelConsumerBuilder.onOutput(onOutput);
  }

  /**
   * Returns a builder of stateful routines.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful routine is the one computing a final result by accumulating the
   * input data (for instance, computing the sum of input numbers).
   *
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @param <STATE> the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulRoutineBuilder<IN, OUT, STATE> stateful() {
    return new DefaultStatefulRoutineBuilder<IN, OUT, STATE>();
  }

  /**
   * Returns a builder of stateful routines already configured to accumulate the inputs into a list.
   * <br>
   * In order to finalize the invocation implementation, it will be sufficient to set the function
   * to call when the invocation completes by calling the proper {@code onComplete} method.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN, OUT> StatefulRoutineBuilder<IN, OUT, ? extends List<IN>> statefulList() {
    return JRoutineFunction.<IN, OUT, List<IN>>stateful().onCreate(
        (Supplier<? extends List<IN>>) sListSupplier)
                                                         .onNextConsume(
                                                             (BiConsumer<? super List<IN>, ?
                                                                 super IN>) sListConsumer);
  }

  /**
   * Returns a builder of stateless routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless routine is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessRoutineBuilder<IN, OUT> stateless() {
    return new DefaultStatelessRoutineBuilder<IN, OUT>();
  }
}
