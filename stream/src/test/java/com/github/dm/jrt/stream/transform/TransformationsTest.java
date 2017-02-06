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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static com.github.dm.jrt.stream.transform.Transformations.throttle;
import static com.github.dm.jrt.stream.transform.Transformations.timeoutAfter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream transformation unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class TransformationsTest {

  private static Runner sSingleThreadRunner;

  @NotNull
  private static Runner getSingleThreadRunner() {
    if (sSingleThreadRunner == null) {
      sSingleThreadRunner = Runners.poolRunner(1);
    }

    return sSingleThreadRunner;
  }

  @Test
  public void testBackoff() {
    Assertions.assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 1000)))
                              .invocationConfiguration()
                              .withRunner(getSingleThreadRunner())
                              .withInputBackoff(
                                  BackoffBuilder.afterCount(2).linearDelay(seconds(10)))
                              .apply()
                              .map(Functions.<Number>identity())
                              .map(new Function<Number, Double>() {

                                public Double apply(final Number number) {
                                  final double value = number.doubleValue();
                                  return Math.sqrt(value);
                                }
                              })
                              .sync()
                              .map(new Function<Double, SumData>() {

                                public SumData apply(final Double aDouble) {
                                  return new SumData(aDouble, 1);
                                }
                              })
                              .map(reduce(new BiFunction<SumData, SumData, SumData>() {

                                public SumData apply(final SumData data1, final SumData data2) {
                                  return new SumData(data1.sum + data2.sum,
                                      data1.count + data2.count);
                                }
                              }))
                              .map(new Function<SumData, Double>() {

                                public Double apply(final SumData data) {
                                  return data.sum / data.count;
                                }
                              })
                              .close()
                              .in(seconds(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));
    Assertions.assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 1000)))
                              .invocationConfiguration()
                              .withRunner(getSingleThreadRunner())
                              .withInputBackoff(
                                  BackoffBuilder.afterCount(2).constantDelay(seconds(10)))
                              .apply()
                              .map(Functions.<Number>identity())
                              .map(new Function<Number, Double>() {

                                public Double apply(final Number number) {
                                  final double value = number.doubleValue();
                                  return Math.sqrt(value);
                                }
                              })
                              .sync()
                              .map(new Function<Double, SumData>() {

                                public SumData apply(final Double aDouble) {
                                  return new SumData(aDouble, 1);
                                }
                              })
                              .map(reduce(new BiFunction<SumData, SumData, SumData>() {

                                public SumData apply(final SumData data1, final SumData data2) {
                                  return new SumData(data1.sum + data2.sum,
                                      data1.count + data2.count);
                                }
                              }))
                              .map(new Function<SumData, Double>() {

                                public Double apply(final SumData data) {
                                  return data.sum / data.count;
                                }
                              })
                              .close()
                              .in(seconds(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testConstructor() {
    boolean failed = false;
    try {
      new Transformations();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testDelay() {
    long startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.delay(1, TimeUnit.SECONDS))
                             .call("test")
                             .in(seconds(3))
                             .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.delay(seconds(1)))
                             .call("test")
                             .in(seconds(3))
                             .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.delay(1, TimeUnit.SECONDS))
                             .close()
                             .in(seconds(3))
                             .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.delay(seconds(1)))
                             .close()
                             .in(seconds(3))
                             .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testDelayNullPointerError() {
    try {
      Transformations.delay(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testLag() {
    long startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.lag(1, TimeUnit.SECONDS))
                             .call("test")
                             .in(seconds(3))
                             .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.lag(seconds(1)))
                             .call("test")
                             .in(seconds(3))
                             .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.lag(1, TimeUnit.SECONDS))
                             .close()
                             .in(seconds(3))
                             .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.withStream()
                             .lift(Transformations.lag(seconds(1)))
                             .close()
                             .in(seconds(3))
                             .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testLagNullPointerError() {
    try {
      Transformations.lag(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testParallelSplit() {
    final StreamBuilder<Integer, Long> sqr =
        JRoutineStream.<Integer>withStream().map(new Function<Integer, Long>() {

          public Long apply(final Integer number) {
            final long value = number.longValue();
            return value * value;
          }
        });
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Long>parallel(2,
                                  sqr.buildFactory()))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Long>parallel(2, sqr))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Integer>parallel(2,
                                  JRoutineCore.with(IdentityInvocation.<Integer>factoryOf())))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1, 2, 3);
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Long>parallelBy(
                                  Functions.<Integer>identity(), sqr.buildFactory()))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Long>parallelBy(
                                  Functions.<Integer>identity(), sqr))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1L, 4L, 9L);
    assertThat(JRoutineStream //
        .<Integer>withStream().map(appendAccept(range(1, 3)))
                              .lift(Transformations.<Integer, Integer, Integer>parallelBy(
                                  Functions.<Integer>identity(),
                                  JRoutineCore.with(IdentityInvocation.<Integer>factoryOf())))
                              .close()
                              .in(seconds(3))
                              .all()).containsOnly(1, 2, 3);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testParallelSplitNullPointerError() {
    try {
      Transformations.parallel(1, (InvocationFactory<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallel(1, (Routine<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallel(1, (RoutineBuilder<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallelBy(null, JRoutineStream.withStream().buildRoutine());
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallelBy(null, JRoutineStream.withStream());
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallelBy(Functions.identity(), (InvocationFactory<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallelBy(Functions.identity(), (Routine<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.parallelBy(Functions.identity(), (RoutineBuilder<Object, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testRetry() {
    final AtomicInteger count1 = new AtomicInteger();
    try {
      JRoutineStream.<String>withStream().map(new UpperCase())
                                         .map(factoryOf(ThrowException.class, count1))
                                         .lift(Transformations.<String, Object>retry(2))
                                         .call("test")
                                         .in(seconds(3))
                                         .throwError();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    final AtomicInteger count2 = new AtomicInteger();
    assertThat(JRoutineStream.<String>withStream().map(new UpperCase())
                                                  .map(factoryOf(ThrowException.class, count2, 1))
                                                  .lift(Transformations.<String, Object>retry(1))
                                                  .call("test")
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST");

    final AtomicInteger count3 = new AtomicInteger();
    try {
      JRoutineStream.<String>withStream().map(new AbortInvocation())
                                         .map(factoryOf(ThrowException.class, count3))
                                         .lift(Transformations.<String, Object>retry(2))
                                         .call("test")
                                         .in(seconds(3))
                                         .throwError();
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testRetryConsumerError() {
    final Channel<Object, Object> inputChannel = JRoutineCore.ofInputs().buildChannel();
    final Channel<Object, Object> outputChannel = JRoutineCore.ofInputs().buildChannel();
    new RetryChannelConsumer<Object, Object>(inputChannel, outputChannel, Runners.syncRunner(),
        new Function<Channel<?, Object>, Channel<?, Object>>() {

          public Channel<?, Object> apply(final Channel<?, Object> channel) throws Exception {
            throw new NullPointerException();
          }
        }, new BiFunction<Integer, RoutineException, Long>() {

      public Long apply(final Integer integer, final RoutineException e) {
        return 0L;
      }
    }).run();
    assertThat(inputChannel.getError()).isNotNull();
    assertThat(outputChannel.getError()).isNotNull();
  }

  @Test
  public void testRetryConsumerError2() {
    final Channel<Object, Object> inputChannel = JRoutineCore.ofInputs().buildChannel();
    final Channel<Object, Object> outputChannel = JRoutineCore.ofInputs().buildChannel();
    new RetryChannelConsumer<Object, Object>(inputChannel, outputChannel, Runners.syncRunner(),
        new Function<Channel<?, Object>, Channel<?, Object>>() {

          public Channel<?, Object> apply(final Channel<?, Object> channel) {
            return channel;
          }
        }, new BiFunction<Integer, RoutineException, Long>() {

      public Long apply(final Integer integer, final RoutineException e) throws Exception {
        throw new NullPointerException();
      }
    }).run();
    inputChannel.abort(new RoutineException());
    assertThat(inputChannel.getError()).isNotNull();
    assertThat(outputChannel.getError()).isNotNull();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testRetryNullPointerError() {
    try {
      Transformations.retry(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.retry(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testThrottle() throws InterruptedException {
    final Routine<Object, Object> routine = JRoutineStream.withStream()
                                                          .lift(throttle(1))
                                                          .invocationConfiguration()
                                                          .withRunner(Runners.poolRunner(1))
                                                          .apply()
                                                          .buildRoutine();
    final Channel<Object, Object> channel1 = routine.call().pass("test1");
    final Channel<Object, Object> channel2 = routine.call().pass("test2");
    seconds(0.5).sleepAtLeast();
    assertThat(channel1.close().in(seconds(1.5)).next()).isEqualTo("test1");
    assertThat(channel2.close().in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testThrottleAbort() throws InterruptedException {
    final Routine<Object, Object> routine = JRoutineStream.withStream()
                                                          .lift(throttle(1))
                                                          .invocationConfiguration()
                                                          .withRunner(Runners.poolRunner(1))
                                                          .apply()
                                                          .buildRoutine();
    final Channel<Object, Object> channel1 = routine.call().pass("test1");
    final Channel<Object, Object> channel2 = routine.call().pass("test2");
    seconds(0.5).sleepAtLeast();
    assertThat(channel1.abort()).isTrue();
    assertThat(channel2.close().in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testTimeThrottle() {
    final Routine<Object, Object> routine =
        JRoutineStream.withStream().lift(throttle(1, seconds(1))).buildRoutine();
    final Channel<Object, Object> channel1 = routine.call("test1");
    final Channel<Object, Object> channel2 = routine.call("test2");
    assertThat(channel1.in(seconds(1.5)).next()).isEqualTo("test1");
    assertThat(channel2.in(seconds(1.5)).next()).isEqualTo("test2");
  }

  @Test
  public void testTimeout() {
    assertThat(JRoutineStream.withStream()
                             .lift(timeoutAfter(seconds(1)))
                             .call("test")
                             .in(seconds(1))
                             .all()).containsExactly("test");
    final Channel<Object, Object> channel =
        JRoutineStream.withStream().lift(timeoutAfter(millis(1))).call().pass("test");
    assertThat(channel.in(seconds(1)).getError()).isExactlyInstanceOf(ResultTimeoutException.class);
  }

  @Test
  public void testTryCatch() {
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(new Function<Object, Object>() {

                                                    public Object apply(final Object o) {
                                                      throw new NullPointerException();
                                                    }
                                                  })
                                                  .lift(
                                                      Transformations.<String,
                                                          Object>tryCatchAccept(
                                                          new BiConsumer<RoutineException,
                                                              Channel<Object, ?>>() {

                                                            public void accept(
                                                                final RoutineException e,
                                                                final Channel<Object, ?> channel) {
                                                              channel.pass("exception");
                                                            }
                                                          }))
                                                  .call("test")
                                                  .next()).isEqualTo("exception");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(new Function<Object, Object>() {

                                                    public Object apply(final Object o) {
                                                      return o;
                                                    }
                                                  })
                                                  .lift(
                                                      Transformations.<String,
                                                          Object>tryCatchAccept(
                                                          new BiConsumer<RoutineException,
                                                              Channel<Object, ?>>() {

                                                            public void accept(
                                                                final RoutineException e,
                                                                final Channel<Object, ?> channel) {
                                                              channel.pass("exception");
                                                            }
                                                          }))
                                                  .call("test")
                                                  .next()).isEqualTo("test");
    assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

      public Object apply(final Object o) {
        throw new NullPointerException();
      }
    }).lift(Transformations.<String, Object>tryCatch(new Function<RoutineException, Object>() {

      public Object apply(final RoutineException e) {
        return "exception";
      }
    })).call("test").next()).isEqualTo("exception");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTryCatchNullPointerError() {
    try {
      Transformations.tryCatchAccept(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      Transformations.tryCatch(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testTryFinally() {
    final AtomicBoolean isRun = new AtomicBoolean(false);
    try {
      JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

        public Object apply(final Object o) {
          throw new NullPointerException();
        }
      }).lift(Transformations.<String, Object>tryFinally(new Action() {

        public void perform() {
          isRun.set(true);
        }
      })).call("test").next();

    } catch (final RoutineException ignored) {
    }

    assertThat(isRun.getAndSet(false)).isTrue();
    assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

      public Object apply(final Object o) {
        return o;
      }
    }).lift(Transformations.<String, Object>tryFinally(new Action() {

      public void perform() {
        isRun.set(true);
      }
    })).call("test").next()).isEqualTo("test");
    assertThat(isRun.getAndSet(false)).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTryFinallyNullPointerError() {
    try {
      Transformations.tryFinally(null);
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
    public boolean onRecycle(final boolean isReused) {
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
