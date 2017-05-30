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

import com.github.dm.jrt.android.function.builder.StatefulContextFactoryBuilder;
import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessContextFactoryBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v11.core.LoaderSource;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * This utility class provides a few ways to easily implement Loader routines through functional
 * interfaces.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * JRoutineLoaderFunction.&lt;String, String, StringBuilder&gt;statefulRoutineOn(loaderOf(activity))
 *                       .onCreate(StringBuilder::new)
 *                       .onNextState(StringBuilder::append)
 *                       .onCompleteOutput(StringBuilder::toString)
 *                       .routine();
 * </code></pre>
 * <p>
 * In a similar way, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * JRoutineLoaderFunction.&lt;String, String,&gt;statelessRoutineOn(loaderOf(activity))
 *                       .onNextOutput(String::toUpperCase)
 *                       .routine();
 * </code></pre>
 * <p>
 * See
 * {@link com.github.dm.jrt.android.v4.function.JRoutineLoaderFunctionCompat JRoutineLoaderFunctionCompat}
 * for support of API levels lower than {@value android.os.Build.VERSION_CODES#HONEYCOMB}.
 * <p>
 * Created by davide-maestroni on 03/07/2017.
 */
public class JRoutineLoaderFunction {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineLoaderFunction() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of stateful Context invocation factories.
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
  public static <IN, OUT, STATE> StatefulContextFactoryBuilder<IN, OUT, STATE>
  statefulContextFactory() {
    return new DefaultStatefulContextFactoryBuilder<IN, OUT, STATE>();
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
   * Note that, it is advisable to set a specific invocation ID to uniquely identify the built
   * routine, so to make the invocations survive configuration changes.
   *
   * @param loaderSource the Loader source.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @param <STATE>      the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulLoaderRoutineBuilder<IN, OUT, STATE> statefulRoutineOn(
      @NotNull final LoaderSource loaderSource) {
    return new DefaultStatefulLoaderRoutineBuilder<IN, OUT, STATE>(loaderSource);
  }

  /**
   * Returns a builder of stateless Context invocation factories.
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
  public static <IN, OUT> StatelessContextFactoryBuilder<IN, OUT> statelessContextFactory() {
    return new DefaultStatelessContextFactoryBuilder<IN, OUT>();
  }

  /**
   * Returns a builder of stateless Loader routines.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless routine is the one processing each input separately (for
   * instance, computing the square of input numbers).
   * <p>
   * Note that, it is advisable to set a specific invocation ID to uniquely identify the built
   * routine, so to make the invocations survive configuration changes.
   *
   * @param loaderSource the Loader source.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessLoaderRoutineBuilder<IN, OUT> statelessRoutineOn(
      @NotNull final LoaderSource loaderSource) {
    return new DefaultStatelessLoaderRoutineBuilder<IN, OUT>(loaderSource);
  }
}
