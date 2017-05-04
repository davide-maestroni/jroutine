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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.Backoff;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfigurable;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Interface defining a builder of stream lifting functions.
 * <p>
 * Created by davide-maestroni on 05/01/2017.
 *
 * @see com.github.dm.jrt.stream.routine.StreamRoutine#lift(Function)
 */
@SuppressWarnings("WeakerAccess")
public interface StreamLifter extends ChannelConfigurable<StreamLifter> {

  /**
   * Returns a function adding a delay at the beginning of the stream, so that any data, exception
   * or completion notification coming from the source will be dispatched to the stream after the
   * specified time.
   *
   * @param delay the delay.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayInputsOf(@NotNull DurationMeasure delay);

  /**
   * Returns a function adding a delay at the beginning of the stream, so that any data, exception
   * or completion notification coming from the source will be dispatched to the stream after the
   * specified time.
   *
   * @param delay    the delay value.
   * @param timeUnit the delay time unit.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the lifting function.
   * @throws java.lang.IllegalArgumentException if the specified delay is negative.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayInputsOf(long delay, @NotNull TimeUnit timeUnit);

  /**
   * Returns a function adding a delay at the end of the stream, so that any data, exception or
   * completion notification will be dispatched to the next concatenated routine after the
   * specified time.
   *
   * @param delay the delay.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayOutputsOf(@NotNull DurationMeasure delay);

  /**
   * Returns a function adding a delay at the end of the stream, so that any data, exception or
   * completion notification will be dispatched to the next concatenated routine after the
   * specified time.
   *
   * @param delay    the delay value.
   * @param timeUnit the delay time unit.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the lifting function.
   * @throws java.lang.IllegalArgumentException if the specified delay is negative.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayOutputsOf(long delay,
      @NotNull TimeUnit timeUnit);

  /**
   * Returns a function making the stream retry the whole flow of data until the specified function
   * does not return a null value.
   * <br>
   * For each retry the function is called passing the retry count (starting from 1) and the error
   * which caused the failure. If the function returns a non-null value, it will represent the
   * number of milliseconds to wait before a further retry. While, in case the function returns
   * null, the flow of data will be aborted with the passed error as reason.
   * <p>
   * Note that no retry will be attempted in case of an explicit abortion, that is, if the error
   * is an instance of {@link com.github.dm.jrt.core.channel.AbortException}.
   *
   * @param backoffFunction the retry function.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(
      @NotNull BiFunction<? super Integer, ? super RoutineException, ? extends Long>
          backoffFunction);

  /**
   * Returns a function making the stream retry the whole flow of data at maximum for the
   * specified number of times.
   *
   * @param maxCount the maximum number of retries.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the lifting function.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(int maxCount);

  /**
   * Returns a function making the stream retry the whole flow of data at maximum for the
   * specified number of times.
   * <br>
   * For each retry the specified backoff policy will be applied before re-starting the flow.
   *
   * @param maxCount the maximum number of retries.
   * @param backoff  the backoff policy.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the lifting function.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(int maxCount, @NotNull Backoff backoff);

  /**
   * Returns a function splitting the outputs produced by the stream, so that each subset will be
   * processed by a different routine invocation.
   * <br>
   * Each output will be assigned to a specific set based on the key returned by the specified
   * function.
   * <br>
   * Note that the order in which the outputs are dispatched cannot be guaranteed.
   *
   * @param keyFunction the function assigning a key to each output.
   * @param routine     the processing routine instance
   * @param <IN>        the input data type.
   * @param <OUT>       the output data type.
   * @param <AFTER>     the new output type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT, AFTER> LiftingFunction<IN, OUT, IN, AFTER> splitBy(
      @NotNull Function<? super OUT, ?> keyFunction,
      @NotNull Routine<? super OUT, ? extends AFTER> routine);

  /**
   * Returns a function splitting the outputs produced by the stream, so that each subset will be
   * processed by a different routine invocation.
   * <br>
   * Each output will be assigned to a specific invocation based on the load of the available
   * invocations.
   * <br>
   * Note that the order in which the outputs are dispatched cannot be guaranteed.
   *
   * @param invocationCount the number of processing invocations.
   * @param routine         the processing routine instance.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @param <AFTER>         the new output type.
   * @return the lifting function.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  <IN, OUT, AFTER> LiftingFunction<IN, OUT, IN, AFTER> splitIn(int invocationCount,
      @NotNull Routine<? super OUT, ? extends AFTER> routine);

  /**
   * Returns a function making the stream throttle the invocation instances so that only the
   * specified maximum number are concurrently running at any given time.
   * <br>
   * Note that the same function instance can be used with several streams, so that the total
   * number of invocations will not exceed the specified limit.
   *
   * @param maxInvocations the maximum number of invocations.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(int maxInvocations);

  /**
   * Returns a function making the stream throttle the invocation instances so that only the
   * specified maximum number are started in the passed time range.
   * <br>
   * Note that the same function instance can be used with several streams, so that the total
   * number of started invocations will not exceed the specified limit.
   *
   * @param maxInvocations the maximum number of invocations.
   * @param range          the time range.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(int maxInvocations,
      @NotNull DurationMeasure range);

  /**
   * Returns a function making the stream throttle the invocation instances so that only the
   * specified maximum number are started in the passed time range.
   * <br>
   * Note that the same function instance can be used with several streams, so that the total
   * number of started invocations will not exceed the specified limit.
   *
   * @param maxInvocations the maximum number.
   * @param range          the time range value.
   * @param timeUnit       the time range unit.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(int maxInvocations, long range,
      @NotNull TimeUnit timeUnit);

  /**
   * Returns a function making the stream abort with a
   * {@link com.github.dm.jrt.stream.transform.ResultTimeoutException ResultTimeoutException} if
   * a new result is not produced before the specified timeout elapsed.
   *
   * @param outputTimeout the new output timeout.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(@NotNull DurationMeasure outputTimeout);

  /**
   * Returns a function making the stream abort with a
   * {@link com.github.dm.jrt.stream.transform.ResultTimeoutException ResultTimeoutException} if
   * a new result is not produced, or the invocation does not complete, before the specified
   * timeouts elapse.
   *
   * @param outputTimeout the new output timeout.
   * @param totalTimeout  the total timeout.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(@NotNull DurationMeasure outputTimeout,
      @NotNull DurationMeasure totalTimeout);

  /**
   * Returns a function making the stream abort with a
   * {@link com.github.dm.jrt.stream.transform.ResultTimeoutException ResultTimeoutException} if
   * a new result is not produced before the specified timeout elapses.
   *
   * @param outputTimeout  the new output timeout value.
   * @param outputTimeUnit the new output timeout unit.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(long outputTimeout,
      @NotNull TimeUnit outputTimeUnit);

  /**
   * Returns a function making the stream abort with a
   * {@link com.github.dm.jrt.stream.transform.ResultTimeoutException ResultTimeoutException} if
   * a new result is not produced, or the invocation does not complete, before the specified
   * timeouts elapse.
   *
   * @param outputTimeout  the new output timeout value.
   * @param outputTimeUnit the new output timeout unit.
   * @param totalTimeout   the total timeout value.
   * @param totalTimeUnit  the total timeout unit.
   * @param <IN>           the input data type.
   * @param <OUT>          the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(long outputTimeout,
      @NotNull TimeUnit outputTimeUnit, long totalTimeout, @NotNull TimeUnit totalTimeUnit);

  /**
   * Returns a function concatenating to the stream a consumer handling invocation exceptions.
   * <br>
   * The errors will not be automatically further propagated.
   *
   * @param catchFunction the function instance.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryCatch(
      @NotNull Function<? super RoutineException, ? extends OUT> catchFunction);

  /**
   * Returns a function concatenating to the stream a consumer handling invocation exceptions.
   * <br>
   * The result channel of the backing routine will be passed to the consumer, so that multiple
   * or no results may be generated.
   * <br>
   * The errors will not be automatically further propagated.
   *
   * @param catchConsumer the bi-consumer instance.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryCatchAccept(
      @NotNull BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer);

  /**
   * Returns a function concatenating to the stream an action always performed when outputs
   * complete, even if an error occurred.
   * <br>
   * Both outputs and errors will be automatically passed on.
   *
   * @param finallyAction the action instance.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the lifting function.
   */
  @NotNull
  <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryFinally(@NotNull Action finallyAction);
}
