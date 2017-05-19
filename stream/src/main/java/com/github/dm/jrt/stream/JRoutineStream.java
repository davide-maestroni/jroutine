/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.stream.routine.StreamRoutine;
import com.github.dm.jrt.stream.transform.StreamLifter;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p>
 * A stream routine builder allows to easily build a concatenation of invocations as a single
 * routine.
 * <p>
 * For instance, a routine computing the root mean square of a number of integers can be defined as:
 * <pre><code>
 * final Routine&lt;Integer, Double&gt; rms =
 *         JRoutineStream.&lt;Integer, Integer&gt;streamOf(routine().of(unary(i -&gt; i * i))
 *                       .map(routine().of(average(Float.class)))
 *                       .map(unary(Math::sqrt));
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineStream {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineStream() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of lifting functions.
   *
   * @return the builder instance.
   */
  @NotNull
  public static StreamLifter streamLifter() {
    return streamLifterOn(defaultExecutor());
  }

  /**
   * Returns a builder of lifting functions employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the builder instance.
   */
  @NotNull
  public static StreamLifter streamLifterOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultStreamLifter(executor);
  }

  /**
   * Returns a stream routine wrapping the specified factory of invocations.
   * <br>
   * The invocations will be executed synchronously on the same thread as the routine invocation.
   *
   * @param factory the invocation factory instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the stream routine.
   */
  @NotNull
  public static <IN, OUT> StreamRoutine<IN, OUT> streamOf(
      @NotNull final InvocationFactory<IN, OUT> factory) {
    return new DefaultStreamRoutine<IN, OUT>(factory);
  }

  /**
   * Returns a stream routine wrapping the specified one.
   *
   * @param routine the routine instance.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the stream routine.
   */
  @NotNull
  public static <IN, OUT> StreamRoutine<IN, OUT> streamOf(@NotNull final Routine<IN, OUT> routine) {
    if (routine instanceof StreamRoutine) {
      return (StreamRoutine<IN, OUT>) routine;
    }

    return new DefaultStreamRoutine<IN, OUT>(routine);
  }

  /**
   * Returns a stream routine wrapping the specified invocation.
   * <br>
   * The invocations will be executed synchronously on the same thread as the routine invocation.
   * <br>
   * Note that the returned routine can be called only once.
   *
   * @param invocation the invocation instance.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the stream routine.
   */
  @NotNull
  public static <IN, OUT> StreamRoutine<IN, OUT> streamOfSingleton(
      @NotNull final Invocation<IN, OUT> invocation) {
    return new DefaultStreamRoutine<IN, OUT>(invocation);
  }
}
