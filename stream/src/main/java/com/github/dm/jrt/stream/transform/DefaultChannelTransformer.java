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
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;

/**
 * Default implementation of a channel transformer.
 * <p>
 * Created by davide-maestroni on 05/01/2017.
 */
class DefaultChannelTransformer implements ChannelTransformer {

  private final ScheduledExecutor mExecutor;

  private ChannelConfiguration mConfiguration = ChannelConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param executor the executor instance.
   */
  DefaultChannelTransformer(@NotNull final ScheduledExecutor executor) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayInputsOf(
      @NotNull final DurationMeasure delay) {
    return delayInputsOf(delay.value, delay.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayInputsOf(final long delay,
      @NotNull final TimeUnit timeUnit) {
    return new LiftInputDelay<IN, OUT>(mExecutor, mConfiguration, delay, timeUnit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayOutputsOf(
      @NotNull final DurationMeasure delay) {
    return delayOutputsOf(delay.value, delay.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> delayOutputsOf(final long delay,
      @NotNull final TimeUnit timeUnit) {
    return new LiftOutputDelay<IN, OUT>(mExecutor, mConfiguration, delay, timeUnit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(
      @NotNull final BiFunction<? super Integer, ? super RoutineException, ? extends Long>
          backoffFunction) {
    return new LiftRetry<IN, OUT>(mExecutor, mConfiguration, backoffFunction);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(final int maxCount) {
    return retry(maxCount, BackoffBuilder.noDelay());
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> retry(final int maxCount,
      @NotNull final Backoff backoff) {
    return retry(new RetryBackoff(maxCount, backoff));
  }

  @NotNull
  public <IN, OUT, AFTER> LiftingFunction<IN, OUT, IN, AFTER> splitBy(
      @NotNull final Function<? super OUT, ?> keyFunction,
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    return new LiftSplitBy<IN, OUT, AFTER>(mExecutor, mConfiguration, keyFunction, routine);
  }

  @NotNull
  public <IN, OUT, AFTER> LiftingFunction<IN, OUT, IN, AFTER> splitIn(final int invocationCount,
      @NotNull final Routine<? super OUT, ? extends AFTER> routine) {
    return new LiftSplitIn<IN, OUT, AFTER>(mExecutor, mConfiguration, invocationCount, routine);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(final int maxInvocations) {
    return new LiftThrottle<IN, OUT>(mExecutor, mConfiguration, maxInvocations);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(final int maxInvocations,
      @NotNull final DurationMeasure range) {
    return throttle(maxInvocations, range.value, range.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> throttle(final int maxInvocations,
      final long range, @NotNull final TimeUnit timeUnit) {
    return new LiftTimeThrottle<IN, OUT>(mExecutor, mConfiguration, maxInvocations, range,
        timeUnit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(
      @NotNull final DurationMeasure outputTimeout) {
    return timeoutAfter(outputTimeout.value, outputTimeout.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(
      @NotNull final DurationMeasure outputTimeout, @NotNull final DurationMeasure totalTimeout) {
    return timeoutAfter(outputTimeout.value, outputTimeout.unit, totalTimeout.value,
        totalTimeout.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(final long outputTimeout,
      @NotNull final TimeUnit outputTimeUnit) {
    final DurationMeasure indefiniteTime = indefiniteTime();
    return timeoutAfter(outputTimeout, outputTimeUnit, indefiniteTime.value, indefiniteTime.unit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> timeoutAfter(final long outputTimeout,
      @NotNull final TimeUnit outputTimeUnit, final long totalTimeout,
      @NotNull final TimeUnit totalTimeUnit) {
    return new LiftTimeout<IN, OUT>(mExecutor, mConfiguration, outputTimeout, outputTimeUnit,
        totalTimeout, totalTimeUnit);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryCatch(
      @NotNull final Function<? super RoutineException, ? extends OUT> catchFunction) {
    return tryCatchAccept(new TryCatchBiConsumerFunction<OUT>(catchFunction));
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryCatchAccept(
      @NotNull final BiConsumer<? super RoutineException, ? super Channel<OUT, ?>> catchConsumer) {
    return new LiftTryCatch<IN, OUT>(mExecutor, mConfiguration, catchConsumer);
  }

  @NotNull
  public <IN, OUT> LiftingFunction<IN, OUT, IN, OUT> tryFinally(
      @NotNull final Action finallyAction) {
    return new LiftTryFinally<IN, OUT>(mExecutor, mConfiguration, finallyAction);
  }

  @NotNull
  public Builder<? extends ChannelTransformer> withChannel() {
    return new Builder<ChannelTransformer>(this, mConfiguration);
  }

  @NotNull
  public ChannelTransformer withConfiguration(@NotNull final ChannelConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }
}
