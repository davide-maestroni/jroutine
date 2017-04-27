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
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.executor.SyncExecutor;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuildingException;
import com.github.dm.jrt.stream.config.StreamConfiguration;
import com.github.dm.jrt.stream.transform.ConvertFunction;
import com.github.dm.jrt.stream.transform.Transformations;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.minutes;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.Functions.constant;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.JRoutineFunction.onOutput;
import static com.github.dm.jrt.operator.Operators.append;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.Operators.filter;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static com.github.dm.jrt.stream.transform.Transformations.tryCatchAccept;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
public class StreamBuilderTest {

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
    final Channel<Object, Object> channel = JRoutineCore.ofData().buildChannel();
    final Channel<Object, Object> streamChannel =
        JRoutineStream.withStream().invoke().pass(channel).close();
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
      JRoutineStream.withStream().sync().invoke().all();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().invoke().next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().invoke().next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().invoke().nextOrElse(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().invoke().skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream().sync().invoke().abort()).isTrue();
    assertThat(
        JRoutineStream.withStream().sync().invoke().abort(new IllegalStateException())).isTrue();
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .after(seconds(3))
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .after(3, TimeUnit.SECONDS)
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    try {
      final ArrayList<String> results = new ArrayList<String>();
      JRoutineStream.<String>withStream().sync()
                                         .map(append("test"))
                                         .invoke()
                                         .allInto(results)
                                         .getComplete();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream()
                    .sync()
                    .map(append((Object) "test"))
                    .invoke()
                    .pipe(JRoutineCore.ofData().buildChannel())
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(append("test"))
                                                  .invoke()
                                                  .consume(onOutput(new Consumer<String>() {

                                                    public void accept(final String s) {
                                                      assertThat(s).isEqualTo("test");
                                                    }
                                                  }))
                                                  .close()
                                                  .getError()).isNull();
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .close()
                             .next()).isEqualTo("test");
    try {
      JRoutineStream.withStream()
                    .sync()
                    .map(append((Object) "test"))
                    .invoke()
                    .expiringIterator()
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map(append((Object) "test")).invoke().iterator().next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .eventuallyAbort()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .eventuallyAbort(new IllegalStateException())
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .eventuallyContinue()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .eventuallyFail()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().invoke().getError()).isNull();
    assertThat(JRoutineStream.withStream().sync().invoke().getComplete()).isFalse();
    try {
      JRoutineStream.withStream().sync().invoke().hasNext();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .invoke()
                             .afterNoDelay()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().invoke().inputSize()).isZero();
    assertThat(JRoutineStream.withStream().sync().invoke().outputSize()).isZero();
    assertThat(JRoutineStream.withStream().sync().invoke().size()).isZero();
    assertThat(JRoutineStream.withStream().sync().invoke().isBound()).isFalse();
    assertThat(JRoutineStream.withStream().sync().invoke().isEmpty()).isTrue();
    assertThat(JRoutineStream.withStream().sync().invoke().isOpen()).isTrue();
    assertThat(JRoutineStream.withStream().sync().invoke().pass("test").next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().invoke().pass("test", "test").next()).isEqualTo(
        "test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .invoke()
                             .pass(Arrays.asList("test", "test"))
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .invoke()
                             .pass(JRoutineCore.of("test").buildChannel())
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().invoke().sorted().pass("test").next()).isEqualTo(
        "test");
    assertThat(
        JRoutineStream.withStream().sync().invoke().unsorted().pass("test").next()).isEqualTo(
        "test");
    JRoutineStream.withStream().sync().invoke().throwError();
    try {
      JRoutineStream.withStream().sync().invoke().remove();
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
  public void testConvert() {
    assertThat(JRoutineStream.<String>withStream().convert(
        new ConvertFunction<String, String, String, String>() {

          public StreamBuilder<String, String> apply(final StreamConfiguration streamConfiguration,
              final StreamBuilder<String, String> builder) {
            return builder.map(append("test2"));
          }
        }).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.withStream().convert(new ConvertFunction<Object, Object, Object, Object>() {

        public StreamBuilder<Object, Object> apply(final StreamConfiguration streamConfiguration,
            final StreamBuilder<Object, Object> builder) {
          throw new NullPointerException();
        }
      });
      fail();

    } catch (final StreamBuildingException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    assertThat(JRoutineStream.<String>withStream().convert(
        new ConvertFunction<String, String, String, String>() {

          public StreamBuilder<String, String> apply(final StreamConfiguration streamConfiguration,
              final StreamBuilder<String, String> builder) {
            return builder.map(append("test2"));
          }
        }).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.withStream().convert(new ConvertFunction<Object, Object, Object, Object>() {

        public StreamBuilder<Object, Object> apply(final StreamConfiguration streamConfiguration,
            final StreamBuilder<Object, Object> builder) {
          throw new NullPointerException();
        }
      });
      fail();

    } catch (final StreamBuildingException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testFlatMap() {
    assertThat(JRoutineStream //
        .<String>withStream().sync().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .invoke()
                                                      .pass(s)
                                                      .close();
          }
        }).invoke().pass("test1", null, "test2", null).close().all()).containsExactly("test1",
        "test2");
    assertThat(JRoutineStream //
        .<String>withStream().async()
                             .flatMap(new Function<String, Channel<?, String>>() {

                               public Channel<?, String> apply(final String s) {
                                 return JRoutineStream.<String>withStream().sync()
                                                                           .map(filter(
                                                                               Functions
                                                                                   .<String>isNotNull()))
                                                                           .invoke()
                                                                           .pass(s)
                                                                           .close();
                               }
                             })
                             .invoke()
                             .pass("test1", null, "test2", null)
                             .close()
                             .in(seconds(3))
                             .all()).containsExactly("test1", "test2");
    assertThat(JRoutineStream //
        .<String>withStream().asyncParallel(2).flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .invoke()
                                                      .pass(s)
                                                      .close();
          }
        }).invoke().pass("test1", null, "test2", null).close().in(seconds(3)).all()).containsOnly(
        "test1", "test2");
    assertThat(JRoutineStream //
        .<String>withStream().syncParallel(2).flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .invoke()
                                                      .pass(s)
                                                      .close();
          }
        }).invoke().pass("test1", null, "test2", null).close().in(seconds(3)).all()).containsOnly(
        "test1", "test2");
    assertThat(JRoutineStream //
        .<String>withStream().immediateParallel(2)
                             .flatMap(new Function<String, Channel<?, String>>() {

                               public Channel<?, String> apply(final String s) {
                                 return JRoutineStream.<String>withStream().immediate()
                                                                           .map(filter(
                                                                               Functions
                                                                                   .<String>isNotNull()))
                                                                           .invoke()
                                                                           .pass(s)
                                                                           .close();
                               }
                             })
                             .invoke()
                             .pass("test1", null, "test2", null)
                             .close()
                             .in(seconds(3))
                             .all()).containsOnly("test1", "test2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFlatMapNullPointerError() {
    try {
      JRoutineStream.withStream().sync().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().async().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testFlatMapRetry() {
    final Routine<Object, String> routine =
        JRoutineCore.with(functionMapping(new Function<Object, String>() {

          public String apply(final Object o) {
            return o.toString();
          }
        })).buildRoutine();
    final Function<Object, Channel<Object, String>> retryFunction =
        new Function<Object, Channel<Object, String>>() {

          public Channel<Object, String> apply(final Object o) {
            final int[] count = {0};
            return JRoutineStream.withStream()
                                 .map(routine)
                                 .lift(tryCatchAccept(
                                     new BiConsumer<RoutineException, Channel<String, ?>>() {

                                       public void accept(final RoutineException e,
                                           final Channel<String, ?> channel) {
                                         if (++count[0] < 3) {
                                           JRoutineStream.withStream()
                                                         .map(routine)
                                                         .lift(tryCatchAccept(this))
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
      JRoutineStream.withStream()
                    .async()
                    .flatMap(retryFunction)
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
  public void testImmediate() {
    assertThat(JRoutineStream.<Integer>withStream().immediate()
                                                   .map(appendAccept(range(1, 1000)))
                                                   .streamInvocationConfiguration()
                                                   .withInputMaxSize(1)
                                                   .withOutputMaxSize(1)
                                                   .configured()
                                                   .map(new Function<Number, Double>() {

                                                     public Double apply(final Number number) {
                                                       return Math.sqrt(number.doubleValue());
                                                     }
                                                   })
                                                   .map(Operators.average(Double.class))
                                                   .invoke()
                                                   .close()
                                                   .next()).isCloseTo(21, Offset.offset(0.1));
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
                                                   .map(appendAccept(range(1, 1000)))
                                                   .streamInvocationConfiguration()
                                                   .withInputMaxSize(1)
                                                   .withOutputMaxSize(1)
                                                   .configured()
                                                   .map(new Function<Number, Double>() {

                                                     public Double apply(final Number number) {
                                                       return Math.sqrt(number.doubleValue());
                                                     }
                                                   })
                                                   .immediate()
                                                   .map(Operators.average(Double.class))
                                                   .invoke()
                                                   .close()
                                                   .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testImmediateOptimization() {
    assertThat(
        JRoutineStream.<Integer>withStream().immediate().map(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            return integer << 1;
          }
        }).invoke().pass(1, 2, 3).close().all()).containsExactly(2, 4, 6);
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
                                                   .map(new Function<Integer, Integer>() {

                                                     public Integer apply(final Integer integer) {
                                                       return integer << 1;
                                                     }
                                                   })
                                                   .invoke()
                                                   .pass(1, 2, 3)
                                                   .close()
                                                   .all()).containsExactly(2, 4, 6);
    assertThat(JRoutineStream.<Integer>withStream().immediate()
                                                   .mapAccept(
                                                       new BiConsumer<Integer, Channel<Integer,
                                                           ?>>() {

                                                         public void accept(final Integer integer,
                                                             final Channel<Integer, ?> result) {
                                                           result.pass(integer << 1);
                                                         }
                                                       })
                                                   .invoke()
                                                   .pass(1, 2, 3)
                                                   .close()
                                                   .all()).containsExactly(2, 4, 6);
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
                                                   .mapAccept(
                                                       new BiConsumer<Integer, Channel<Integer,
                                                           ?>>() {

                                                         public void accept(final Integer integer,
                                                             final Channel<Integer, ?> result) {
                                                           result.pass(integer << 1);
                                                         }
                                                       })
                                                   .invoke()
                                                   .pass(1, 2, 3)
                                                   .close()
                                                   .all()).containsExactly(2, 4, 6);
    assertThat(JRoutineStream.<Integer>withStream().immediate()
                                                   .mapAll(new Function<List<Integer>, Integer>() {

                                                     public Integer apply(
                                                         final List<Integer> integers) {
                                                       int sum = 0;
                                                       for (final Integer integer : integers) {
                                                         sum += integer;
                                                       }

                                                       return sum;
                                                     }
                                                   })
                                                   .invoke()
                                                   .pass(1, 2, 3)
                                                   .close()
                                                   .all()).containsExactly(6);
    assertThat(JRoutineStream.<Integer>withStream().immediate()
                                                   .mapAllAccept(
                                                       new BiConsumer<List<Integer>,
                                                           Channel<Integer, ?>>() {

                                                         public void accept(
                                                             final List<Integer> integers,
                                                             final Channel<Integer, ?> result) {
                                                           int sum = 0;
                                                           for (final Integer integer : integers) {
                                                             sum += integer;
                                                           }

                                                           result.pass(sum);
                                                         }
                                                       })
                                                   .invoke()
                                                   .pass(1, 2, 3)
                                                   .close()
                                                   .all()).containsExactly(6);
  }

  @Test
  public void testImmediateOptimizationAbort() {
    Channel<Integer, Integer> channel = JRoutineStream //
        .<Integer>withStream().immediate().map(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            return integer << 1;
          }
        }).invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediateParallel().map(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            return integer << 1;
          }
        }).invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediate()
                              .mapAccept(new BiConsumer<Integer, Channel<Integer, ?>>() {

                                public void accept(final Integer integer,
                                    final Channel<Integer, ?> result) {
                                  result.pass(integer << 1);
                                }
                              })
                              .invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediateParallel()
                              .mapAccept(new BiConsumer<Integer, Channel<Integer, ?>>() {

                                public void accept(final Integer integer,
                                    final Channel<Integer, ?> result) {
                                  result.pass(integer << 1);
                                }
                              })
                              .invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediate().mapAll(new Function<List<Integer>, Integer>() {

          public Integer apply(final List<Integer> integers) {
            int sum = 0;
            for (final Integer integer : integers) {
              sum += integer;
            }

            return sum;
          }
        }).invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediateParallel().mapAll(new Function<List<Integer>, Integer>() {

          public Integer apply(final List<Integer> integers) {
            int sum = 0;
            for (final Integer integer : integers) {
              sum += integer;
            }

            return sum;
          }
        }).invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediate()
                              .mapAllAccept(new BiConsumer<List<Integer>, Channel<Integer, ?>>() {

                                public void accept(final List<Integer> integers,
                                    final Channel<Integer, ?> result) {
                                  int sum = 0;
                                  for (final Integer integer : integers) {
                                    sum += integer;
                                  }

                                  result.pass(sum);
                                }
                              })
                              .invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediateParallel()
                              .mapAllAccept(new BiConsumer<List<Integer>, Channel<Integer, ?>>() {

                                public void accept(final List<Integer> integers,
                                    final Channel<Integer, ?> result) {
                                  int sum = 0;
                                  for (final Integer integer : integers) {
                                    sum += integer;
                                  }

                                  result.pass(sum);
                                }
                              })
                              .invoke();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
  }

  @Test
  public void testInvocationDeadlock() {
    try {
      final ScheduledExecutor executor1 = ScheduledExecutors.poolExecutor(1);
      final ScheduledExecutor executor2 = ScheduledExecutors.poolExecutor(1);
      final Function<String, Object> function = new Function<String, Object>() {

        public Object apply(final String s) {
          return JRoutineStream.<String>withStream().nextInvocationConfiguration()
                                                    .withExecutor(executor1)
                                                    .configured()
                                                    .map(Functions.identity())
                                                    .nextInvocationConfiguration()
                                                    .withExecutor(executor2)
                                                    .configured()
                                                    .map(Functions.identity())
                                                    .withInvocation()
                                                    .withOutputTimeout(minutes(3))
                                                    .configured()
                                                    .invoke()
                                                    .pass(s)
                                                    .close()
                                                    .next();
        }
      };
      JRoutineStream.<String>withStream().nextInvocationConfiguration()
                                         .withExecutor(executor1)
                                         .configured()
                                         .map(function)
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
  public void testLag() {
    long startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.<String>withStream().lift(
        Transformations.<String, String>lag(1, TimeUnit.SECONDS))
                                                  .invoke()
                                                  .pass("test")
                                                  .close()
                                                  .in(seconds(3))
                                                  .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.<String>withStream().lift(Transformations.<String, String>lag(seconds(1)))
                                           .invoke()
                                           .pass("test")
                                           .close()
                                           .in(seconds(3))
                                           .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.<String>withStream().lift(
        Transformations.<String, String>lag(1, TimeUnit.SECONDS))
                                                  .invoke()
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.<String>withStream().lift(Transformations.<String, String>lag(seconds(1)))
                                           .invoke()
                                           .close()
                                           .in(seconds(3))
                                           .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  public void testLift() {
    assertThat(JRoutineStream.<String>withStream().lift(
        new BiFunction<StreamConfiguration, Function<Channel<?, String>, Channel<?, String>>,
            Function<Channel<?, String>, Channel<?, String>>>() {

          public Function<Channel<?, String>, Channel<?, String>> apply(
              final StreamConfiguration configuration,
              final Function<Channel<?, String>, Channel<?, String>> function) {
            assertThat(configuration.toChannelConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
            assertThat(configuration.toInvocationConfiguration()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
            return Functions.decorate(function)
                            .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                              public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase())
                                                   .invoke()
                                                   .pass(channel)
                                                   .close();
                              }
                            });
          }
        }).invoke().pass("test").close().in(seconds(3)).next()).isEqualTo("TEST");
    assertThat(JRoutineStream.<String>withStream().lift(
        new BiFunction<StreamConfiguration, Function<Channel<?, String>, Channel<?, String>>,
            Function<Channel<?, String>, Channel<?, String>>>() {

          public Function<Channel<?, String>, Channel<?, String>> apply(
              final StreamConfiguration streamConfiguration,
              final Function<Channel<?, String>, Channel<?, String>> function) {
            return Functions.decorate(function)
                            .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                              public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase())
                                                   .invoke()
                                                   .pass(channel)
                                                   .close();
                              }
                            });
          }
        }).invoke().pass("test").close().in(seconds(3)).next()).isEqualTo("TEST");
    try {
      JRoutineStream.withStream()
                    .lift(
                        new BiFunction<StreamConfiguration, Function<Channel<?, Object>,
                            Channel<?, Object>>, Function<Channel<?, Object>, Channel<?,
                            Object>>>() {

                          public Function<Channel<?, Object>, Channel<?, Object>> apply(
                              final StreamConfiguration configuration,
                              final Function<Channel<?, Object>, Channel<?, Object>> function) {
                            throw new NullPointerException();
                          }
                        });
      fail();

    } catch (final StreamBuildingException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    try {
      JRoutineStream.withStream()
                    .lift(
                        new BiFunction<StreamConfiguration, Function<Channel<?, Object>,
                            Channel<?, Object>>, Function<Channel<?, Object>, Channel<?,
                            Object>>>() {

                          public Function<Channel<?, Object>, Channel<?, Object>> apply(
                              final StreamConfiguration streamConfiguration,
                              final Function<Channel<?, Object>, Channel<?, Object>> function) {
                            throw new NullPointerException();
                          }
                        });
      fail();

    } catch (final StreamBuildingException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    final StreamBuilder<Object, Object> builder = //
        JRoutineStream.withStream()
                      .lift(
                          new BiFunction<StreamConfiguration, Function<Channel<?, Object>,
                              Channel<?, Object>>, Function<Channel<?, Object>, Channel<?,
                              Object>>>() {

                            public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                final StreamConfiguration streamConfiguration,
                                final Function<Channel<?, Object>, Channel<?, Object>> function) {
                              return new Function<Channel<?, Object>, Channel<?, Object>>() {

                                public Channel<?, Object> apply(final Channel<?, Object> objects) {
                                  throw new NullPointerException();
                                }
                              };
                            }
                          });
    try {
      builder.sync().invoke().close().throwError();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testMapAllConsumer() {
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .mapAllAccept(
                                                      new BiConsumer<List<? extends String>,
                                                          Channel<String, ?>>() {

                                                        public void accept(
                                                            final List<? extends String> strings,
                                                            final Channel<String, ?> result) {
                                                          final StringBuilder builder =
                                                              new StringBuilder();
                                                          for (final String string : strings) {
                                                            builder.append(string);
                                                          }

                                                          result.pass(builder.toString());
                                                        }
                                                      })
                                                  .invoke()
                                                  .pass("test1", "test2", "test3")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("test1test2test3");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .mapAllAccept(
                                                      new BiConsumer<List<? extends String>,
                                                          Channel<String, ?>>() {

                                                        public void accept(
                                                            final List<? extends String> strings,
                                                            final Channel<String, ?> result) {
                                                          final StringBuilder builder =
                                                              new StringBuilder();
                                                          for (final String string : strings) {
                                                            builder.append(string);
                                                          }

                                                          result.pass(builder.toString());
                                                        }
                                                      })
                                                  .invoke()
                                                  .pass("test1", "test2", "test3")
                                                  .close()
                                                  .all()).containsExactly("test1test2test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapAllConsumerNullPointerError() {
    try {
      JRoutineStream.withStream().async().mapAllAccept(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapAllFunction() {
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .mapAll(
                                                      new Function<List<? extends String>,
                                                          String>() {

                                                        public String apply(
                                                            final List<? extends String> strings) {
                                                          final StringBuilder builder =
                                                              new StringBuilder();
                                                          for (final String string : strings) {
                                                            builder.append(string);
                                                          }

                                                          return builder.toString();
                                                        }
                                                      })
                                                  .invoke()
                                                  .pass("test1", "test2", "test3")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("test1test2test3");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .mapAll(
                                                      new Function<List<? extends String>,
                                                          String>() {

                                                        public String apply(
                                                            final List<? extends String> strings) {
                                                          final StringBuilder builder =
                                                              new StringBuilder();
                                                          for (final String string : strings) {
                                                            builder.append(string);
                                                          }

                                                          return builder.toString();
                                                        }
                                                      })
                                                  .invoke()
                                                  .pass("test1", "test2", "test3")
                                                  .close()
                                                  .all()).containsExactly("test1test2test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapAllFunctionNullPointerError() {
    try {
      JRoutineStream.withStream().async().mapAll(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapConsumer() {
    assertThat(
        JRoutineStream.<String>withStream().mapAccept(new BiConsumer<String, Channel<String, ?>>() {

          public void accept(final String s, final Channel<String, ?> result) {
            result.pass(s.toUpperCase());
          }
        }).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly("TEST1",
        "TEST2");
    assertThat(JRoutineStream //
        .<String>withStream().sorted()
                             .asyncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .invoke()
                             .pass("test1", "test2")
                             .close()
                             .in(seconds(3))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream//
        .<String>withStream().sync().mapAccept(new BiConsumer<String, Channel<String, ?>>() {

          public void accept(final String s, final Channel<String, ?> result) {
            result.pass(s.toUpperCase());
          }
        }).invoke().pass("test1", "test2").close().all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream//
        .<String>withStream().syncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .invoke()
                             .pass("test1", "test2")
                             .close()
                             .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapConsumerNullPointerError() {
    try {
      JRoutineStream.withStream().async().mapAccept(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapFactory() {
    final InvocationFactory<String, String> factory = factoryOf(UpperCase.class);
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(factory)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(factory)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(factory)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(factory)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapFactoryNullPointerError() {
    try {
      JRoutineStream.withStream().async().map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapFilter() {
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(new UpperCase())
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(new UpperCase())
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(new UpperCase())
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(new UpperCase())
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapFilterNullPointerError() {
    try {
      JRoutineStream.withStream().async().map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapFunction() {
    assertThat(JRoutineStream.<String>withStream().async().map(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }
    }).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly("TEST1",
        "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(new Function<String, String>() {

                                                    public String apply(final String s) {
                                                      return s.toUpperCase();
                                                    }
                                                  })
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync().map(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }
    }).invoke().pass("test1", "test2").close().all()).containsExactly("TEST1", "TEST2");
    assertThat(
        JRoutineStream.<String>withStream().syncParallel().map(new Function<String, String>() {

          public String apply(final String s) {
            return s.toUpperCase();
          }
        }).invoke().pass("test1", "test2").close().all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapFunctionNullPointerError() {
    try {
      JRoutineStream.withStream().async().map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMapOn() {
    final AtomicBoolean isCalled = new AtomicBoolean();
    final ScheduledExecutor testExecutor = new SyncExecutor() {

      @Override
      public void execute(@NotNull final Runnable command, final long delay,
          @NotNull final TimeUnit timeUnit) {
        isCalled.set(true);
        command.run();
      }
    };
    assertThat(JRoutineStream.<String>withStream().unsorted()
                                                  .consumeOn(testExecutor)
                                                  .invoke()
                                                  .pass("test")
                                                  .close()
                                                  .in(seconds(1))
                                                  .all()).containsExactly("test");
    assertThat(isCalled.get()).isTrue();
  }

  @Test
  public void testMapRoutine() {
    final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                        .invocationConfiguration()
                                                        .withOutputOrder(OrderType.SORTED)
                                                        .apply()
                                                        .buildRoutine();
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(routine)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().asyncParallel()
                                                  .map(routine)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(routine)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(routine)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  public void testMapRoutineBuilder() {
    final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(builder)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().asyncParallel()
                                                  .map(builder)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .in(seconds(3))
                                                  .all()).containsOnly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(builder)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(builder)
                                                  .invoke()
                                                  .pass("test1", "test2")
                                                  .close()
                                                  .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapRoutineBuilderNullPointerError() {
    try {
      JRoutineStream.withStream().async().map((RoutineBuilder<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().map((RoutineBuilder<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map((RoutineBuilder<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().map((RoutineBuilder<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapRoutineNullPointerError() {
    try {
      JRoutineStream.withStream().async().map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().asyncParallel().map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineStream.withStream().syncParallel().map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testMaxSizeDeadlock() {
    try {
      assertThat(JRoutineStream //
          .<Integer>withStream().streamInvocationConfiguration()
                                .withExecutor(getSingleThreadExecutor())
                                .configured()
                                .map(appendAccept(range(1, 1000)))
                                .streamInvocationConfiguration()
                                .withInputBackoff(afterCount(2).linearDelay(seconds(3)))
                                .withOutputBackoff(afterCount(2).linearDelay(seconds(3)))
                                .configured()
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
                                .invoke()
                                .close()
                                .in(minutes(3))
                                .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }

    try {
      assertThat(JRoutineStream //
          .<Integer>withStream().streamInvocationConfiguration()
                                .withExecutor(getSingleThreadExecutor())
                                .configured()
                                .map(appendAccept(range(1, 1000)))
                                .streamInvocationConfiguration()
                                .withInputBackoff(afterCount(2).linearDelay(seconds(3)))
                                .configured()
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
                                .invoke()
                                .close()
                                .in(minutes(3))
                                .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }
  }

  @Test
  public void testStreamAccept() {
    assertThat(JRoutineStream.withStreamAccept(range(0, 3))
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly(0, 1, 2, 3);
    assertThat(JRoutineStream.withStreamAccept(2, range(1, 0))
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly(1, 0, 1, 0);
  }

  @Test
  public void testStreamAcceptAbort() {
    Channel<Integer, Integer> channel =
        JRoutineStream.withStreamAccept(range(0, 3)).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamAccept(2, range(1, 0)).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamAcceptError() {
    assertThat(JRoutineStream.withStreamAccept(range(0, 3))
                             .immediate()
                             .invoke()
                             .pass(31)
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamAccept(2, range(1, 0))
                             .immediate()
                             .invoke()
                             .pass(-17)
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamGet() {
    assertThat(JRoutineStream.withStreamGet(constant("test"))
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly("test");
    assertThat(JRoutineStream.withStreamGet(2, constant("test2"))
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly("test2", "test2");
  }

  @Test
  public void testStreamGetAbort() {
    Channel<String, String> channel =
        JRoutineStream.withStreamGet(constant("test")).immediate().immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamGet(2, constant("test2")).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamGetError() {
    assertThat(JRoutineStream.withStreamGet(constant("test"))
                             .immediate()
                             .invoke()
                             .pass("test")
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamGet(2, constant("test2"))
                             .immediate()
                             .invoke()
                             .pass("test")
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamOf() {
    assertThat(
        JRoutineStream.withStreamOf("test").immediate().invoke().close().all()).containsExactly(
        "test");
    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                             .immediate()
                             .invoke()
                             .close()
                             .all()).containsExactly("test1", "test2", "test3");
    assertThat(
        JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                      .immediate()
                      .invoke()
                      .close()
                      .all()).containsExactly("test1", "test2", "test3");
  }

  @Test
  public void testStreamOfAbort() {
    Channel<String, String> channel = JRoutineStream.withStreamOf("test").immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamOf("test1", "test2", "test3").immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel =
        JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3")).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                            .immediate()
                            .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamOfError() {
    assertThat(JRoutineStream.withStreamOf("test")
                             .immediate()
                             .invoke()
                             .pass("test")
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .invoke()
                             .pass("test")
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                             .immediate()
                             .invoke()
                             .pass("test")
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(
        JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                      .immediate()
                      .invoke()
                      .pass("test")
                      .close()
                      .getError()
                      .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf(
        JRoutineCore.ofData().buildChannel().consume(new TemplateChannelConsumer<Object>() {}))
                             .immediate()
                             .invoke()
                             .close()
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
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
