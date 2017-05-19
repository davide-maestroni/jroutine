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

package com.github.dm.jrt.android.v4.function;

import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.function.builder.StatelessLoaderRoutineBuilder;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * This utility class provides a few ways to easily implement Loader routines through functional
 * interfaces.
 * <p>
 * For example, a routine concatenating strings through a {@code StringBuilder} can be implemented
 * as follows:
 * <pre><code>
 * JRoutineLoaderFunctionCompat.&lt;String, String, StringBuilder&gt;statefulOn(loaderOf(activity))
 *                             .onCreate(StringBuilder::new)
 *                             .onNextState(StringBuilder::append)
 *                             .onCompleteOutput(StringBuilder::toString)
 *                             .routine();
 * </code></pre>
 * <p>
 * In a similar way, a routine switching strings to upper-case can be implemented as follows:
 * <pre><code>
 * JRoutineLoaderFunctionCompat.&lt;String, String,&gt;statelessOn(loaderOf(activity))
 *                             .onNextOutput(String::toUpperCase)
 *                             .routine();
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 03/07/2017.
 */
public class JRoutineLoaderFunctionCompat {

  /**
   * Avoid explicit instantiation.
   */
  private JRoutineLoaderFunctionCompat() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of stateful Loader routines.
   * <br>
   * The specified invocation ID will be used to uniquely identify the built routine, so to make an
   * invocation survive configuration changes.
   * <p>
   * This type of routines are based on invocations retaining a mutable state during their
   * lifecycle.
   * <br>
   * A typical example of stateful routine is the one computing a final result by accumulating the
   * input data (for instance, computing the sum of input numbers).
   *
   * @param loaderSource the Loader source.
   * @param invocationId the invocation ID.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @param <STATE>      the state data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT, STATE> StatefulLoaderRoutineBuilder<IN, OUT, STATE> statefulOn(
      @NotNull final LoaderSourceCompat loaderSource, final int invocationId) {
    return new DefaultStatefulLoaderRoutineBuilderCompat<IN, OUT, STATE>(loaderSource).withLoader()
                                                                                      .withInvocationId(
                                                                                          invocationId)
                                                                                      .configuration();
  }

  /**
   * Returns a builder of stateless Loader routines.
   * <br>
   * The specified invocation ID will be used to uniquely identify the built routine, so to make an
   * invocation survive configuration changes.
   * <p>
   * This type of routines are based on invocations not retaining a mutable internal state.
   * <br>
   * A typical example of stateless routine is the one processing each input separately (for
   * instance, computing the square of input numbers).
   *
   * @param context      the Loader context.
   * @param invocationId the invocation ID.
   * @param <IN>         the input data type.
   * @param <OUT>        the output data type.
   * @return the routine builder.
   */
  @NotNull
  public static <IN, OUT> StatelessLoaderRoutineBuilder<IN, OUT> statelessOn(
      @NotNull final LoaderSourceCompat context, final int invocationId) {
    return new DefaultStatelessLoaderRoutineBuilderCompat<IN, OUT>(context).withLoader()
                                                                           .withInvocationId(
                                                                               invocationId)
                                                                           .configuration();
  }
}
