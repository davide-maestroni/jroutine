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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.JRoutineFunction;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.stream.routine.StreamRoutine;
import com.github.dm.jrt.stream.transform.TransformingFunction;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.immediateExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOfParallel;
import static com.github.dm.jrt.core.util.DurationMeasure.minutes;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.JRoutineFunction.onOutput;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;
import static com.github.dm.jrt.operator.JRoutineOperators.append;
import static com.github.dm.jrt.operator.JRoutineOperators.appendAccept;
import static com.github.dm.jrt.operator.JRoutineOperators.average;
import static com.github.dm.jrt.operator.JRoutineOperators.reduce;
import static com.github.dm.jrt.operator.JRoutineOperators.unary;
import static com.github.dm.jrt.operator.sequence.JRoutineSequences.range;
import static com.github.dm.jrt.stream.JRoutineStream.streamLifter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
public class StreamRoutineTest {

  private static ScheduledExecutor sSingleThreadExecutor;

  @NotNull
  private static ScheduledExecutor getSingleThreadExecutor() {
    if (sSingleThreadExecutor == null) {
      sSingleThreadExecutor = ScheduledExecutors.poolExecutor(1);
    }

    return sSingleThreadExecutor;
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort() {
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    final Channel<Object, Object> streamChannel =
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.factory()))
                      .invoke()
                      .pass(channel)
                      .close();
    channel.abort(new IllegalArgumentException());
    try {
      streamChannel.in(seconds(3)).throwError();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  public void testChannel() {
    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory())).invoke().all();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory())).invoke().next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory())).invoke().next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .invoke()
                    .nextOrElse(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .invoke()
                    .skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .abort()).isTrue();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .abort(new IllegalStateException())).isTrue();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .after(seconds(3))
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .after(3, TimeUnit.SECONDS)
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    try {
      final ArrayList<String> results = new ArrayList<String>();
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.<String>factory()))
                    .map(JRoutineCore.routineOn(syncExecutor()).of(append("test")))
                    .invoke()
                    .allInto(results)
                    .getComplete();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .map(JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                    .invoke()
                    .pipe(JRoutineCore.channel().ofType())
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.<String>factory()))
                             .map(JRoutineCore.routineOn(syncExecutor()).of(append("test")))
                             .invoke()
                             .consume(onOutput(new Consumer<String>() {

                               public void accept(final String s) {
                                 assertThat(s).isEqualTo("test");
                               }
                             }))
                             .close()
                             .getError()).isNull();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .close()
                             .next()).isEqualTo("test");
    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .map(JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                    .invoke()
                    .expiringIterator()
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .map(JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                    .invoke()
                    .iterator()
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .eventuallyAbort()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .eventuallyAbort(new IllegalStateException())
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .eventuallyContinue()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .eventuallyFail()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .getError()).isNull();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .getComplete()).isFalse();
    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .invoke()
                    .hasNext();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .map(
                                 JRoutineCore.routineOn(syncExecutor()).of(append((Object) "test")))
                             .invoke()
                             .afterNoDelay()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .inputSize()).isZero();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .outputSize()).isZero();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .size()).isZero();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory())).invoke().isBound())
        .isFalse();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory())).invoke().isEmpty())
        .isTrue();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .isOpen()).isTrue();
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .pass("test")
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .pass("test", "test")
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .pass(Arrays.asList("test", "test"))
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .pass(JRoutineCore.channel().of("test"))
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .sorted()
                             .pass("test")
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                             .invoke()
                             .unsorted()
                             .pass("test")
                             .next()).isEqualTo("test");
    JRoutineStream.streamOf(JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                  .invoke()
                  .throwError();
    try {
      JRoutineStream.streamOf(
          JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory()))
                    .invoke()
                    .remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {
    }
  }

  @Test
  public void testConstructor() {
    boolean failed = false;
    try {
      new JRoutineStream();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testDelay() {
    long startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .lift(streamLifter().<String, String>delayInputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .lift(streamLifter().<String, String>delayInputsOf(seconds(1)))
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .lift(streamLifter().<String, String>delayOutputsOf(1, TimeUnit.SECONDS))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .lift(streamLifter().<String, String>delayOutputsOf(seconds(1)))
                      .invoke()
                      .close()
                      .in(seconds(3))
                      .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  public void testImmediate() {
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(immediateExecutor()).of(appendAccept(range(1, 1000))))
                             .map(JRoutineCore.routineOn(immediateExecutor())
                                              .withInvocation()
                                              .withInputMaxSize(1)
                                              .withOutputMaxSize(1)
                                              .configuration()
                                              .of(unary(new Function<Number, Double>() {

                                                public Double apply(final Number number) {
                                                  return Math.sqrt(number.doubleValue());
                                                }
                                              })))
                             .map(JRoutineCore.routineOn(immediateExecutor())
                                              .withInvocation()
                                              .withInputMaxSize(1)
                                              .withOutputMaxSize(1)
                                              .configuration()
                                              .of(average(Double.class)))
                             .invoke()
                             .close()
                             .next()).isCloseTo(21, Offset.offset(0.1));
    assertThat(JRoutineStream.streamOf(
        JRoutineCore.routineOn(immediateExecutor()).of((appendAccept(range(1, 1000)))))
                             .map(JRoutineCore.routineOn(immediateExecutor())
                                              .withInvocation()
                                              .withInputMaxSize(1)
                                              .withOutputMaxSize(1)
                                              .configuration()
                                              .of(factoryOfParallel(
                                                  JRoutineCore.routineOn(immediateExecutor())
                                                              .withInvocation()
                                                              .withInputMaxSize(1)
                                                              .withOutputMaxSize(1)
                                                              .configuration()
                                                              .of(unary(
                                                                  new Function<Number, Double>() {

                                                                    public Double apply(
                                                                        final Number number) {
                                                                      return Math.sqrt(
                                                                          number.doubleValue());
                                                                    }
                                                                  })))))
                             .map(JRoutineCore.routineOn(immediateExecutor())
                                              .withInvocation()
                                              .withInputMaxSize(1)
                                              .withOutputMaxSize(1)
                                              .configuration()
                                              .of(average(Double.class)))
                             .invoke()
                             .close()
                             .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testInvocationDeadlock() {
    try {
      final ScheduledExecutor executor1 = ScheduledExecutors.poolExecutor(1);
      final ScheduledExecutor executor2 = ScheduledExecutors.poolExecutor(1);
      final Function<String, Object> function = new Function<String, Object>() {

        public Object apply(final String s) {
          return JRoutineStream.streamOf(
              JRoutineCore.routineOn(executor1).of(IdentityInvocation.factory()))
                               .map(JRoutineCore.routineOn(executor2)
                                                .of(IdentityInvocation.factory()))
                               .invoke()
                               .pass(s)
                               .close()
                               .in(minutes(3))
                               .next();
        }
      };
      JRoutineStream.streamOf(JRoutineCore.routineOn(executor1).of(unary(function)))
                    .invoke()
                    .pass("test")
                    .close()
                    .in(minutes(3))
                    .next();
      fail();

    } catch (final ExecutionDeadlockException ignored) {
    }
  }

  @Test
  public void testLift() {
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .lift(
                          new Function<Supplier<? extends Channel<String, String>>, Supplier<?
                              extends Channel<String, String>>>() {

                            public Supplier<? extends Channel<String, String>> apply(
                                final Supplier<? extends Channel<String, String>> supplier) {
                              return wrapSupplier(supplier).andThen(
                                  new Function<Channel<String, String>, Channel<String, String>>() {

                                    public Channel<String, String> apply(
                                        final Channel<String, String> channel) {
                                      return JRoutineCore.routine()
                                                         .of(new UpperCase())
                                                         .invoke()
                                                         .pipe(channel);
                                    }
                                  });
                            }
                          })
                      .invoke()
                      .pass("test")
                      .close()
                      .in(seconds(3))
                      .next()).isEqualTo("TEST");
    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                    .lift(
                        new Function<Supplier<? extends Channel<String, String>>, Supplier<?
                            extends Channel<String, String>>>() {

                          public Supplier<? extends Channel<String, String>> apply(
                              final Supplier<? extends Channel<String, String>> supplier) {
                            throw new NullPointerException();
                          }
                        });
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    final StreamRoutine<String, String> routine = //
        JRoutineStream.streamOf(
            JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.<String>factory()))
                      .lift(
                          new Function<Supplier<? extends Channel<String, String>>, Supplier<?
                              extends Channel<String, String>>>() {

                            public Supplier<? extends Channel<String, String>> apply(
                                final Supplier<? extends Channel<String, String>> supplier) {
                              return new Supplier<Channel<String, String>>() {

                                public Channel<String, String> get() throws Exception {
                                  throw new NullPointerException();
                                }
                              };
                            }
                          });
    try {
      routine.invoke();
      fail();

    } catch (final IllegalStateException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testMapRetry() {
    final Routine<Object, String> routine =
        JRoutineCore.routine().of(unary(new Function<Object, String>() {

          public String apply(final Object o) {
            return o.toString();
          }
        }));
    final Function<Object, Channel<Object, String>> retryFunction =
        new Function<Object, Channel<Object, String>>() {

          public Channel<Object, String> apply(final Object o) {
            final int[] count = {0};
            return JRoutineStream.streamOf(routine)
                                 .lift(streamLifter().tryCatchAccept(
                                     new BiConsumer<RoutineException, Channel<String, ?>>() {

                                       public void accept(final RoutineException e,
                                           final Channel<String, ?> channel) {
                                         if (++count[0] < 3) {
                                           JRoutineStream.streamOf(routine)
                                                         .lift(streamLifter().tryCatchAccept(this))
                                                         .invoke()
                                                         .pass(o)
                                                         .close()
                                                         .pipe(channel);

                                         } else {
                                           throw e;
                                         }
                                       }
                                     }))
                                 .invoke()
                                 .pass(o)
                                 .close();

          }
        };

    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(unary(retryFunction)))
                    .map(JRoutineFunction.<Channel<Object, String>, Object>stateless().onNext(
                        new BiConsumer<Channel<Object, String>, Channel<Object, ?>>() {

                          public void accept(final Channel<Object, String> channel,
                              final Channel<Object, ?> result) throws Exception {
                            result.pass(channel);
                          }
                        }).routine())
                    .invoke()
                    .pass((Object) null)
                    .close()
                    .in(seconds(3))
                    .all();
      fail();

    } catch (final RoutineException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testMapRoutine() {
    final Routine<String, String> routine = JRoutineCore.routine()
                                                        .withInvocation()
                                                        .withOutputOrder(OrderType.SORTED)
                                                        .configuration()
                                                        .of(new UpperCase());
    assertThat(JRoutineStream.streamOf(routine)
                             .invoke()
                             .pass("test1", "test2")
                             .close()
                             .in(seconds(3))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.streamOf(JRoutineCore.routine().of(factoryOfParallel(routine)))
                             .invoke()
                             .pass("test1", "test2")
                             .close()
                             .in(seconds(3))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapRoutineNullPointerError() {
    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.factory())).map(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMaxSizeDeadlock() {
    try {
      final InvocationConfiguration configuration = //
          InvocationConfiguration.builder()
                                 .withInputBackoff(afterCount(2).linearDelay(seconds(3)))
                                 .withOutputBackoff(afterCount(2).linearDelay(seconds(3)))
                                 .configuration();
      assertThat(JRoutineStream.streamOf(
          JRoutineCore.routineOn(getSingleThreadExecutor()).of(appendAccept(range(1, 1000))))
                               .map(JRoutineCore.routineOn(getSingleThreadExecutor())
                                                .withConfiguration(configuration)
                                                .of(IdentityInvocation.<Number>factory()))
                               .map(JRoutineCore.routineOn(getSingleThreadExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<Number, Double>() {

                                                  public Double apply(final Number number) {
                                                    final double value = number.doubleValue();
                                                    return Math.sqrt(value);
                                                  }
                                                })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<Double, SumData>() {

                                                  public SumData apply(final Double aDouble) {
                                                    return new SumData(aDouble, 1);
                                                  }
                                                })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(reduce(
                                                    new BiFunction<SumData, SumData, SumData>() {

                                                      public SumData apply(final SumData data1,
                                                          final SumData data2) {
                                                        return new SumData(data1.sum + data2.sum,
                                                            data1.count + data2.count);
                                                      }
                                                    })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<SumData, Double>() {

                                                  public Double apply(final SumData data) {
                                                    return data.sum / data.count;
                                                  }
                                                })))
                               .invoke()
                               .close()
                               .in(minutes(3))
                               .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }

    try {
      final InvocationConfiguration configuration = //
          InvocationConfiguration.builder()
                                 .withInputBackoff(afterCount(2).linearDelay(seconds(3)))
                                 .configuration();
      assertThat(JRoutineStream.streamOf(
          JRoutineCore.routineOn(getSingleThreadExecutor()).of(appendAccept(range(1, 1000))))
                               .map(JRoutineCore.routineOn(getSingleThreadExecutor())
                                                .withConfiguration(configuration)
                                                .of(IdentityInvocation.<Number>factory()))
                               .map(JRoutineCore.routineOn(getSingleThreadExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<Number, Double>() {

                                                  public Double apply(final Number number) {
                                                    final double value = number.doubleValue();
                                                    return Math.sqrt(value);
                                                  }
                                                })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<Double, SumData>() {

                                                  public SumData apply(final Double aDouble) {
                                                    return new SumData(aDouble, 1);
                                                  }
                                                })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(reduce(
                                                    new BiFunction<SumData, SumData, SumData>() {

                                                      public SumData apply(final SumData data1,
                                                          final SumData data2) {
                                                        return new SumData(data1.sum + data2.sum,
                                                            data1.count + data2.count);
                                                      }
                                                    })))
                               .map(JRoutineCore.routineOn(syncExecutor())
                                                .withConfiguration(configuration)
                                                .of(unary(new Function<SumData, Double>() {

                                                  public Double apply(final SumData data) {
                                                    return data.sum / data.count;
                                                  }
                                                })))
                               .invoke()
                               .close()
                               .in(minutes(3))
                               .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }
  }

  @Test
  public void testTransform() {
    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .transform(new TransformingFunction<String, String, String, String>() {

                        public StreamRoutine<String, String> apply(
                            final StreamRoutine<String, String> routine) throws Exception {
                          return routine.map(JRoutineCore.routine().of(append("test2")));
                        }
                      })
                      .invoke()
                      .pass("test1")
                      .close()
                      .in(seconds(3))
                      .all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.factory()))
                    .transform(new TransformingFunction<Object, Object, Object, Object>() {

                      public StreamRoutine<Object, Object> apply(
                          final StreamRoutine<Object, Object> objectObjectStreamRoutine) throws
                          Exception {
                        throw new NullPointerException();
                      }
                    });
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    assertThat(
        JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.<String>factory()))
                      .transform(new TransformingFunction<String, String, String, String>() {

                        public StreamRoutine<String, String> apply(
                            final StreamRoutine<String, String> routine) throws Exception {
                          return routine.map(JRoutineCore.routine().of(append("test2")));
                        }
                      })
                      .invoke()
                      .pass("test1")
                      .close()
                      .in(seconds(3))
                      .all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.streamOf(JRoutineCore.routine().of(IdentityInvocation.factory()))
                    .transform(new TransformingFunction<Object, Object, Object, Object>() {

                      public StreamRoutine<Object, Object> apply(
                          final StreamRoutine<Object, Object> objectObjectStreamRoutine) throws
                          Exception {
                        throw new NullPointerException();
                      }
                    });
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
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

  private static class UpperCase extends MappingInvocation<String, String> {

    /**
     * Constructor.
     */
    UpperCase() {
      super(null);
    }

    public void onInput(final String input, @NotNull final Channel<String, ?> result) {
      result.pass(input.toUpperCase());
    }
  }
}
