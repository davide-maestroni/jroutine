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

package com.github.dm.jrt.android.v11.function;

import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide-maestroni on 03/07/2017.
 */
public class JRoutineLoaderFunction {

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

  private JRoutineLoaderFunction() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of stateful Loader routines.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful routine is the one computing a final result by accumulating the
   * input data (for instance, computing the sum of input numbers).
   * <p>
   * TODO: explain invocationId
   *
   * @param context      the Loader context.
   * @param invocationId the invocation ID.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @param <STATE>      the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulLoaderRoutineBuilder<IN, OUT, STATE> stateful(
      @NotNull final LoaderContext context, final int invocationId) {
    return new DefaultStatefulLoaderRoutineBuilder<IN, OUT, STATE>(context).loaderConfiguration()
                                                                           .withInvocationId(
                                                                               invocationId)
                                                                           .apply();
  }

  /**
   * Returns a builder of stateful Loader routines already configured to accumulate the inputs into
   * a list.
   * <br>
   * In order to finalize the invocation implementation, it will be sufficient to set the function
   * to call when the invocation completes by calling the proper {@code onComplete} method.
   * <p>
   * TODO: explain invocationId
   *
   * @param context      the Loader context.
   * @param invocationId the invocation ID.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatefulLoaderRoutineBuilder<IN, OUT, ? extends List<IN>> statefulList(
      @NotNull final LoaderContext context, final int invocationId) {
    final Supplier<? extends List<IN>> onCreate = listSupplier();
    final BiConsumer<? super List<IN>, ? super IN> onNext = listConsumer();
    final DefaultStatefulLoaderRoutineBuilder<IN, OUT, List<IN>> builder =
        new DefaultStatefulLoaderRoutineBuilder<IN, OUT, List<IN>>(context);
    return builder.onCreate(onCreate)
                  .onNextConsume(onNext)
                  .loaderConfiguration()
                  .withInvocationId(invocationId)
                  .apply();
  }

  /**
   * Returns a builder of stateless Loader routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless routine is the one processing each input separately (for
   * instance, computing the square of input numbers).
   * <p>
   * TODO: explain invocationId
   *
   * @param context      the Loader context.
   * @param invocationId the invocation ID.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessLoaderRoutineBuilder<IN, OUT> stateless(
      @NotNull final LoaderContext context, final int invocationId) {
    return new DefaultStatelessLoaderRoutineBuilder<IN, OUT>(context).loaderConfiguration()
                                                                     .withInvocationId(invocationId)
                                                                     .apply();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <IN> BiConsumer<? super List<IN>, ? super IN> listConsumer() {
    return (BiConsumer<? super List<IN>, ? super IN>) sListConsumer;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <IN> Supplier<? extends List<IN>> listSupplier() {
    return (Supplier<? extends List<IN>>) sListSupplier;
  }
}
