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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.operator.JRoutineOperators;
import com.github.dm.jrt.stream.routine.StreamRoutine;
import com.github.dm.jrt.stream.transform.ResultTimeoutException;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.operator.JRoutineOperators.appendOutputsOf;
import static com.github.dm.jrt.operator.JRoutineOperators.identity;
import static com.github.dm.jrt.operator.JRoutineOperators.reduce;
import static com.github.dm.jrt.operator.JRoutineOperators.unary;
import static com.github.dm.jrt.operator.sequence.Sequence.range;
import static com.github.dm.jrt.stream.JRoutineStream.streamLifter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream transformation unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class TransformationsTest {

  private static ScheduledExecutor sSingleThreadExecutor;

  @NotNull
  private static ScheduledExecutor getSingleThreadExecutor() {
    if (sSingleThreadExecutor == null) {
      sSingleThreadExecutor = ScheduledExecutors.poolExecutor(1);
    }

    return sSingleThreadExecutor;
  }

  @Test
  public void testBackoff() {
    Assertions.assertThat( //
        JRoutineStream.streamOf(
            JRoutineCore.routineOn(getSingleThreadExecutor()).of(appendOutputsOf(range(1, 1000))))
                      .map(JRoutineCore.routine()
                                       .withInvocation()
                                       .withInputBackoff(
                                           BackoffBuilder.afterCount(2).linearDelay(seconds(10)))
                                       .configuration()
                                       .of(JRoutineOperators.<Number>identity()))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(unary(new Function<Number, Double>() {

                                         public Double apply(final Number number) {
                                           final double value = number.doubleValue();
                                           return Math.sqrt(value);
                                         }
                                       })))
                      .map(JRoutineCore.routine().of(unary(new Function<Double, SumData>() {

                        public SumData apply(final Double aDouble) {
                          return new SumData(aDouble, 1);
                        }
                      })))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(reduce(new BiFunction<SumData, SumData, SumData>() {

                                         public SumData apply(final SumData data1,
                                             final SumData data2) {
                                           return new SumData(data1.sum + data2.sum,
                                               data1.count + data2.count);
                                         }
                                       })))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(unary(new Function<SumData, Double>() {

                                         public Double apply(final SumData data) {
                                           return data.sum / data.count;
                                         }
                                       })))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .next()).isCloseTo(21, Offset.offset(0.1));
    Assertions.assertThat( //
        JRoutineStream.streamOf(JRoutineCore.routineOn(getSingleThreadExecutor())
                                            .withInvocation()
                                            .withInputBackoff(BackoffBuilder.afterCount(2)
                                                                            .constantDelay(
                                                                                seconds(10)))
                                            .configuration()
                                            .of(appendOutputsOf(range(1, 1000))))
                      .map(JRoutineCore.routine().of(JRoutineOperators.<Number>identity()))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(unary(new Function<Number, Double>() {

                                         public Double apply(final Number number) {
                                           final double value = number.doubleValue();
                                           return Math.sqrt(value);
                                         }
                                       })))
                      .map(JRoutineCore.routine().of(unary(new Function<Double, SumData>() {

                        public SumData apply(final Double aDouble) {
                          return new SumData(aDouble, 1);
                        }
                      })))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(reduce(new BiFunction<SumData, SumData, SumData>() {

                                         public SumData apply(final SumData data1,
                                             final SumData data2) {
                                           return new SumData(data1.sum + data2.sum,
                                               data1.count + data2.count);
                                         }
                                       })))
                      .map(JRoutineCore.routineOn(syncExecutor())
                                       .of(unary(new Function<SumData, Double>() {

                                         public Double apply(final SumData data) {
                                           return data.sum / data.count;
                                         }
                                       })))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testDelayInputs() {
    long startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayInputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayInputsOf(seconds(1)))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayInputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayInputsOf(seconds(1)))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testDelayNullPointerError() {
    try {
      streamLifter().delayInputsOf(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testDelayOutputs() {
    long startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayOutputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayOutputsOf(seconds(1)))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayOutputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(JRoutineOperators.<String>identity()))
                      .lift(streamLifter().<String, String>delayOutputsOf(seconds(1)))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testLagNullPointerError() {
    try {
      streamLifter().delayOutputsOf(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testParallelSplit() {
    final StreamRoutine<Integer, Long> sqr =
        JRoutineStream.streamOf(JRoutineCore.routine().of(unary(new Function<Integer, Long>() {

          public Long apply(final Integer number) {
            final long value = number.longValue();
            return value * value;
          }
        })));
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(appendOutputsOf(range(1, 3))))
                             .lift(streamLifter().<Integer, Integer, Long>splitIn(2, sqr))
                             .invoke()
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(appendOutputsOf(range(1, 3))))
                             .lift(streamLifter().<Integer, Integer, Long>splitIn(2, sqr))
                             .invoke()
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(appendOutputsOf(range(1, 3))))
                             .lift(streamLifter().<Integer, Integer, Integer>splitIn(2,
                                 JRoutineCore.routine().of(JRoutineOperators.<Integer>identity())))
                             .invoke()
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly(1, 2, 3);
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(appendOutputsOf(range(1, 3))))
                             .lift(streamLifter().<Integer, Integer, Long>splitBy(
                                 FunctionDecorator.<Integer>identity(), sqr))
                             .invoke()
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(appendOutputsOf(range(1, 3))))
                             .lift(streamLifter().<Integer, Integer, Integer>splitBy(
                                 FunctionDecorator.<Integer>identity(),
                                 JRoutineCore.routine().of(JRoutineOperators.<Integer>identity())))
                             .invoke()
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly(1, 2, 3);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testParallelSplitNullPointerError() {
    try {
      streamLifter().splitIn(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      streamLifter().splitBy(null, JRoutineCore.routine().of(identity()));
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      streamLifter().splitBy(FunctionDecorator.identity(), null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testRetry() {
    final AtomicInteger count1 = new AtomicInteger();
    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(new UpperCase()))
                    .map(JRoutineCore.routine().of(factoryOf(ThrowException.class, count1)))
                    .lift(streamLifter().<String, Object>retry(2))
                    .invoke()
                    .pass("test")
                    .close()
                    .in(seconds(3))
                    .throwError();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    final AtomicInteger count2 = new AtomicInteger();
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(new UpperCase()))
                             .map(JRoutineCore.routine()
                                              .of(factoryOf(ThrowException.class, count2, 1)))
                             .lift(streamLifter().<String, Object>retry(1))
                             .invoke()
                             .pass("test")
                             .close()
                             .in(seconds(3))
                             .all()).containsExactly("TEST");

    final AtomicInteger count3 = new AtomicInteger();
    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(new AbortInvocation()))
                    .map(JRoutineCore.routine().of(factoryOf(ThrowException.class, count3)))
                    .lift(streamLifter().retry(2))
                    .invoke()
                    .pass("test")
                    .close()
                    .in(seconds(3))
                    .throwError();
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testRetryConsumerError() {
    final Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    new RetryChannelConsumer<Object, Object>(syncExecutor(),
        new Supplier<Channel<Object, Object>>() {

          public Channel<Object, Object> get() throws Exception {
            throw new NullPointerException();
          }
        }, new BiFunction<Integer, RoutineException, Long>() {

      public Long apply(final Integer integer, final RoutineException e) {
        return 0L;
      }
    }, inputChannel, outputChannel).run();
    assertThat(inputChannel.getError()).isNotNull();
    assertThat(outputChannel.getError()).isNotNull();
  }

  @Test
  public void testRetryConsumerError2() {
    final Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    new RetryChannelConsumer<Object, Object>(syncExecutor(),
        new Supplier<Channel<Object, Object>>() {

          public Channel<Object, Object> get() {
            return JRoutineCore.channel().ofType();
          }
        }, new BiFunction<Integer, RoutineException, Long>() {

      public Long apply(final Integer integer, final RoutineException e) throws Exception {
        throw new NullPointerException();
      }
    }, inputChannel, outputChannel).run();
    inputChannel.abort(new RoutineException());
    assertThat(inputChannel.getError()).isNotNull();
    assertThat(outputChannel.getError()).isNotNull();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testRetryNullPointerError() {
    try {
      streamLifter().retry(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      streamLifter().retry(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testThrottle() throws InterruptedException {
    final Routine<Object, Object> routine = JRoutineStream.streamOf(
        JRoutineCore.routineOn(ScheduledExecutors.poolExecutor(1)).of(identity()))
                                                          .lift(streamLifter().throttle(1));
    final Channel<Object, Object> channel1 = routine.invoke().pass("test1");
    final Channel<Object, Object> channel2 = routine.invoke().pass("test2");
    seconds(0.5).sleepAtLeast();
    assertThat(channel1.close().in(seconds(1.5)).next()).isEqualTo("test1");
    assertThat(channel2.close().in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testThrottleAbort() throws InterruptedException {
    final Routine<Object, Object> routine = JRoutineStream.streamOf(
        JRoutineCore.routineOn(ScheduledExecutors.poolExecutor(1)).of(identity()))
                                                          .lift(streamLifter().throttle(1));
    final Channel<Object, Object> channel1 = routine.invoke().pass("test1");
    final Channel<Object, Object> channel2 = routine.invoke().pass("test2");
    seconds(0.5).sleepAtLeast();
    assertThat(channel1.abort()).isTrue();
    assertThat(channel2.close().in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testTimeThrottle() {
    final Routine<Object, Object> routine =
        JRoutineStream.streamOf(JRoutineCore.routine().of(identity()))
                      .lift(streamLifter().throttle(1, seconds(1)));
    final Channel<Object, Object> channel1 = routine.invoke().pass("test1").close();
    final Channel<Object, Object> channel2 = routine.invoke().pass("test2").close();
    assertThat(channel1.in(seconds(1.5)).next()).isEqualTo("test1");
    assertThat(channel2.in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testTimeout() {
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(identity()))
                             .lift(streamLifter().timeoutAfter(seconds(1)))
                             .invoke()
                             .pass("test")
                             .close()
                             .in(seconds(1))
                             .all()).containsExactly("test");
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(identity()))
                             .lift(streamLifter().timeoutAfter(millis(1)))
                             .invoke()
                             .pass("test")
                             .in(seconds(1))
                             .getError()).isExactlyInstanceOf(ResultTimeoutException.class);
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(identity()))
                             .lift(streamLifter().timeoutAfter(indefiniteTime(), millis(1)))
                             .invoke()
                             .pass("test")
                             .in(seconds(1))
                             .getError()).isExactlyInstanceOf(ResultTimeoutException.class);
  }

  @Test
  public void testTryCatch() {
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

          public Object apply(final Object o) {
            throw new NullPointerException();
          }
        })))
                             .lift(streamLifter().tryCatchAccept(
                                 new BiConsumer<RoutineException, Channel<Object, ?>>() {

                                   public void accept(final RoutineException e,
                                       final Channel<Object, ?> channel) {
                                     channel.pass("exception");
                                   }
                                 }))
                             .invoke()
                             .pass("test")
                             .close()
                             .next()).isEqualTo("exception");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

          public Object apply(final Object o) {
            return o;
          }
        })))
                             .lift(streamLifter().tryCatchAccept(
                                 new BiConsumer<RoutineException, Channel<Object, ?>>() {

                                   public void accept(final RoutineException e,
                                       final Channel<Object, ?> channel) {
                                     channel.pass("exception");
                                   }
                                 }))
                             .invoke()
                             .pass("test")
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

          public Object apply(final Object o) {
            throw new NullPointerException();
          }
        }))).lift(streamLifter().tryCatch(new Function<RoutineException, Object>() {

      public Object apply(final RoutineException e) {
        return "exception";
      }
    })).invoke().pass("test").close().next()).isEqualTo("exception");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTryCatchNullPointerError() {
    try {
      streamLifter().tryCatchAccept(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      streamLifter().tryCatch(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testTryFinally() {
    final AtomicBoolean isRun = new AtomicBoolean(false);
    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

            public Object apply(final Object o) {
              throw new NullPointerException();
            }
          }))).lift(streamLifter().tryFinally(new Action() {

        public void perform() {
          isRun.set(true);
        }
      })).invoke().pass("test").close().next();

    } catch (final RoutineException ignored) {
    }

    assertThat(isRun.getAndSet(false)).isTrue();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

          public Object apply(final Object o) {
            return o;
          }
        }))).lift(streamLifter().tryFinally(new Action() {

      public void perform() {
        isRun.set(true);
      }
    })).invoke().pass("test").close().next()).isEqualTo("test");
    assertThat(isRun.getAndSet(false)).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTryFinallyNullPointerError() {
    try {
      streamLifter().tryFinally(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  private static class AbortInvocation extends MappingInvocation<Object, Object> {

    private AbortInvocation() {
      super(null);
    }

    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
      result.abort(new UnsupportedOperationException());
    }
  }

  private static class SumData {

    private final int count;

    private final double sum;

    private SumData(final double sum, final int count) {
      this.sum = sum;
      this.count = count;
    }
  }

  @SuppressWarnings("unused")
  private static class ThrowException extends TemplateInvocation<Object, Object> {

    private final AtomicInteger mCount;

    private final int mMaxCount;

    private ThrowException(@NotNull final AtomicInteger count) {
      this(count, Integer.MAX_VALUE);
    }

    private ThrowException(@NotNull final AtomicInteger count, final int maxCount) {
      mCount = count;
      mMaxCount = maxCount;
    }

    @Override
    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
      if (mCount.getAndIncrement() < mMaxCount) {
        throw new IllegalStateException();
      }

      result.pass(input);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class UpperCase extends MappingInvocation<String, String> {

    /**
     * Constructor.
     */
    protected UpperCase() {
      super(null);
    }

    public void onInput(final String input, @NotNull final Channel<String, ?> result) {
      result.pass(input.toUpperCase());
    }
  }
}
