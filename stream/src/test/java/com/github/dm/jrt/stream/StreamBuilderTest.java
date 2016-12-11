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
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.runner.SyncRunner;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;
import com.github.dm.jrt.stream.builder.StreamBuildingException;
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
import static com.github.dm.jrt.function.Functions.onOutput;
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

  private static Runner sSingleThreadRunner;

  @NotNull
  private static Runner getSingleThreadRunner() {
    if (sSingleThreadRunner == null) {
      sSingleThreadRunner = Runners.poolRunner(1);
    }

    return sSingleThreadRunner;
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort() {
    final Channel<Object, Object> channel = JRoutineCore.ofInputs().buildChannel();
    final Channel<Object, Object> streamChannel = JRoutineStream.withStream().call(channel);
    channel.abort(new IllegalArgumentException());
    try {
      streamChannel.inMax(seconds(3)).throwError();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  public void testChannel() {
    try {
      JRoutineStream.withStream().sync().call().all();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().call().next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().call().next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().call().nextOrElse(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().call().skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream().sync().call().abort()).isTrue();
    assertThat(
        JRoutineStream.withStream().sync().call().abort(new IllegalStateException())).isTrue();
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .after(seconds(3))
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .after(3, TimeUnit.SECONDS)
                             .inNoTime()
                             .close()
                             .next()).isEqualTo("test");
    try {
      final ArrayList<String> results = new ArrayList<String>();
      JRoutineStream.<String>withStream().sync()
                                         .map(append("test"))
                                         .call()
                                         .allInto(results)
                                         .getComplete();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream()
                    .sync()
                    .map(append((Object) "test"))
                    .call()
                    .bind(JRoutineCore.ofInputs().buildChannel())
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(append("test"))
                                                  .call()
                                                  .bind(onOutput(new Consumer<String>() {

                                                    public void accept(final String s) {
                                                      assertThat(s).isEqualTo("test");
                                                    }
                                                  }))
                                                  .close()
                                                  .getError()).isNull();
    assertThat(
        JRoutineStream.withStream().sync().map(append((Object) "test")).close().next()).isEqualTo(
        "test");
    try {
      JRoutineStream.withStream()
                    .sync()
                    .map(append((Object) "test"))
                    .call()
                    .expiringIterator()
                    .next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    try {
      JRoutineStream.withStream().sync().map(append((Object) "test")).call().iterator().next();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .eventuallyAbort()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .eventuallyAbort(new IllegalStateException())
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .eventuallyContinue()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .eventuallyFail()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().call().getError()).isNull();
    assertThat(JRoutineStream.withStream().sync().call().getComplete()).isFalse();
    try {
      JRoutineStream.withStream().sync().call().hasNext();
      fail();

    } catch (final TimeoutException ignored) {
    }

    assertThat(JRoutineStream.withStream()
                             .sync()
                             .map(append((Object) "test"))
                             .call()
                             .afterNoDelay()
                             .close()
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().call().inputSize()).isZero();
    assertThat(JRoutineStream.withStream().sync().call().outputSize()).isZero();
    assertThat(JRoutineStream.withStream().sync().call().size()).isZero();
    assertThat(JRoutineStream.withStream().sync().call().isBound()).isFalse();
    assertThat(JRoutineStream.withStream().sync().call().isEmpty()).isTrue();
    assertThat(JRoutineStream.withStream().sync().call().isOpen()).isTrue();
    assertThat(JRoutineStream.withStream().sync().call().pass("test").next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().call().pass("test", "test").next()).isEqualTo(
        "test");
    assertThat(JRoutineStream.withStream().sync().call().pass(Arrays.asList("test", "test")).next())
        .isEqualTo("test");
    assertThat(JRoutineStream.withStream()
                             .sync()
                             .call()
                             .pass(JRoutineCore.of("test").buildChannel())
                             .next()).isEqualTo("test");
    assertThat(JRoutineStream.withStream().sync().call().sorted().pass("test").next()).isEqualTo(
        "test");
    assertThat(JRoutineStream.withStream().sync().call().unsorted().pass("test").next()).isEqualTo(
        "test");
    JRoutineStream.withStream().sync().call().throwError();
    try {
      JRoutineStream.withStream().sync().call().remove();
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
  public void testFlatMap() {
    assertThat(JRoutineStream //
        .<String>withStream().sync().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .call(s);
          }
        }).call("test1", null, "test2", null).all()).containsExactly("test1", "test2");
    assertThat(JRoutineStream //
        .<String>withStream().async().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .call(s);
          }
        }).call("test1", null, "test2", null).inMax(seconds(3)).all()).containsExactly("test1",
        "test2");
    assertThat(JRoutineStream //
        .<String>withStream().asyncParallel().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .call(s);
          }
        }).call("test1", null, "test2", null).inMax(seconds(3)).all()).containsOnly("test1",
        "test2");
    assertThat(JRoutineStream //
        .<String>withStream().syncParallel().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineStream.<String>withStream().sync()
                                                      .map(filter(Functions.<String>isNotNull()))
                                                      .call(s);
          }
        }).call("test1", null, "test2", null).inMax(seconds(3)).all()).containsOnly("test1",
        "test2");
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
                                 .let(tryCatchAccept(
                                     new BiConsumer<RoutineException, Channel<String, ?>>() {

                                       public void accept(final RoutineException e,
                                           final Channel<String, ?> channel) {
                                         if (++count[0] < 3) {
                                           JRoutineStream.withStream()
                                                         .map(routine)
                                                         .let(tryCatchAccept(this))
                                                         .call(o)
                                                         .bind(channel);

                                         } else {
                                           throw e;
                                         }
                                       }
                                     }))
                                 .call(o);

          }
        };

    try {
      JRoutineStream.withStream()
                    .async()
                    .flatMap(retryFunction)
                    .call((Object) null)
                    .inMax(seconds(3))
                    .all();
      fail();

    } catch (final RoutineException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testFlatTransform() {
    assertThat(JRoutineStream.<String>withStream().let(
        new Function<StreamBuilder<String, String>, StreamBuilder<String, String>>() {

          public StreamBuilder<String, String> apply(final StreamBuilder<String, String> builder) {
            return builder.map(append("test2"));
          }
        }).call("test1").inMax(seconds(3)).all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.withStream()
                    .let(
                        new Function<StreamBuilder<Object, Object>, StreamBuilder<Object,
                            Object>>() {

                          public StreamBuilder<Object, Object> apply(
                              final StreamBuilder<Object, Object> builder) {
                            throw new NullPointerException();
                          }
                        });
      fail();

    } catch (final StreamBuildingException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }

    assertThat(JRoutineStream.<String>withStream().letWithConfig(
        new BiFunction<StreamConfiguration, StreamBuilder<String, String>, StreamBuilder<String,
            String>>() {

          public StreamBuilder<String, String> apply(final StreamConfiguration streamConfiguration,
              final StreamBuilder<String, String> builder) {
            return builder.map(append("test2"));
          }
        }).call("test1").inMax(seconds(3)).all()).containsExactly("test1", "test2");

    try {
      JRoutineStream.withStream()
                    .letWithConfig(
                        new BiFunction<StreamConfiguration, StreamBuilder<Object, Object>,
                            StreamBuilder<Object, Object>>() {

                          public StreamBuilder<Object, Object> apply(
                              final StreamConfiguration streamConfiguration,
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
  public void testImmediate() {
    assertThat(JRoutineStream.<Integer>withStream().immediate()
                                                   .map(appendAccept(range(1, 1000)))
                                                   .applyStreamInvocationConfiguration()
                                                   .withInputMaxSize(1)
                                                   .withOutputMaxSize(1)
                                                   .configured()
                                                   .map(new Function<Number, Double>() {

                                                     public Double apply(final Number number) {
                                                       return Math.sqrt(number.doubleValue());
                                                     }
                                                   })
                                                   .map(Operators.averageDouble())
                                                   .close()
                                                   .next()).isCloseTo(21, Offset.offset(0.1));
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
                                                   .map(appendAccept(range(1, 1000)))
                                                   .applyStreamInvocationConfiguration()
                                                   .withInputMaxSize(1)
                                                   .withOutputMaxSize(1)
                                                   .configured()
                                                   .map(new Function<Number, Double>() {

                                                     public Double apply(final Number number) {
                                                       return Math.sqrt(number.doubleValue());
                                                     }
                                                   })
                                                   .immediate()
                                                   .map(Operators.averageDouble())
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
        }).call(1, 2, 3).all()).containsExactly(2, 4, 6);
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
                                                   .map(new Function<Integer, Integer>() {

                                                     public Integer apply(final Integer integer) {
                                                       return integer << 1;
                                                     }
                                                   })
                                                   .call(1, 2, 3)
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
                                                   .call(1, 2, 3)
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
                                                   .call(1, 2, 3)
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
                                                   .call(1, 2, 3)
                                                   .all()).containsExactly(6);
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
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
                                                   .call(1, 2, 3)
                                                   .all()).containsExactly(1, 2, 3);
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
                                                   .call(1, 2, 3)
                                                   .all()).containsExactly(6);
    assertThat(JRoutineStream.<Integer>withStream().immediateParallel()
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
                                                   .call(1, 2, 3)
                                                   .all()).containsExactly(1, 2, 3);
  }

  @Test
  public void testImmediateOptimizationAbort() {
    Channel<Integer, Integer> channel = JRoutineStream //
        .<Integer>withStream().immediate().map(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            return integer << 1;
          }
        }).call();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
    channel = JRoutineStream //
        .<Integer>withStream().immediateParallel().map(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            return integer << 1;
          }
        }).call();
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
                              .call();
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
                              .call();
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
        }).call();
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
        }).call();
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
                              .call();
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
                              .call();
    assertThat(channel.pass(1).abort()).isTrue();
    assertThat(channel.getError()).isNotNull();
  }

  @Test
  public void testInvocationDeadlock() {
    try {
      final Runner runner1 = Runners.poolRunner(1);
      final Runner runner2 = Runners.poolRunner(1);
      final Function<String, Object> function = new Function<String, Object>() {

        public Object apply(final String s) {
          return JRoutineStream.<String>withStream().applyInvocationConfiguration()
                                                    .withRunner(runner1)
                                                    .configured()
                                                    .map(Functions.identity())
                                                    .applyInvocationConfiguration()
                                                    .withRunner(runner2)
                                                    .configured()
                                                    .map(Functions.identity())
                                                    .call(s)
                                                    .inMax(minutes(3))
                                                    .next();
        }
      };
      JRoutineStream.<String>withStream().applyInvocationConfiguration()
                                         .withRunner(runner1)
                                         .configured()
                                         .map(function)
                                         .call("test")
                                         .inMax(minutes(3))
                                         .next();
      fail();

    } catch (final ExecutionDeadlockException ignored) {
    }
  }

  @Test
  public void testLag() {
    long startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.<String>withStream().let(
        Transformations.<String, String>lag(1, TimeUnit.SECONDS))
                                                  .call("test")
                                                  .inMax(seconds(3))
                                                  .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.<String>withStream().let(Transformations.<String, String>lag(seconds(1)))
                                           .call("test")
                                           .inMax(seconds(3))
                                           .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineStream.<String>withStream().let(
        Transformations.<String, String>lag(1, TimeUnit.SECONDS))
                                                  .close()
                                                  .inMax(seconds(3))
                                                  .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    startTime = System.currentTimeMillis();
    assertThat(
        JRoutineStream.<String>withStream().let(Transformations.<String, String>lag(seconds(1)))
                                           .close()
                                           .inMax(seconds(3))
                                           .all()).isEmpty();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
  }

  @Test
  public void testLift() {
    assertThat(JRoutineStream.<String>withStream().liftWithConfig(
        new BiFunction<StreamConfiguration, Function<Channel<?, String>, Channel<?, String>>,
            Function<Channel<?, String>, Channel<?, String>>>() {

          public Function<Channel<?, String>, Channel<?, String>> apply(
              final StreamConfiguration configuration,
              final Function<Channel<?, String>, Channel<?, String>> function) {
            assertThat(configuration.toChannelConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
            assertThat(configuration.toInvocationConfiguration()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
            assertThat(configuration.getInvocationMode()).isEqualTo(InvocationMode.ASYNC);
            return Functions.decorate(function)
                            .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                              public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase()).call(channel);
                              }
                            });
          }
        }).call("test").inMax(seconds(3)).next()).isEqualTo("TEST");
    assertThat(JRoutineStream.<String>withStream().lift(
        new Function<Function<Channel<?, String>, Channel<?, String>>, Function<Channel<?,
            String>, Channel<?, String>>>() {

          public Function<Channel<?, String>, Channel<?, String>> apply(
              final Function<Channel<?, String>, Channel<?, String>> function) {
            return Functions.decorate(function)
                            .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                              public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase()).call(channel);
                              }
                            });
          }
        }).call("test").inMax(seconds(3)).next()).isEqualTo("TEST");
    try {
      JRoutineStream.withStream()
                    .liftWithConfig(
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
                        new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                            Function<Channel<?, Object>, Channel<?, Object>>>() {

                          public Function<Channel<?, Object>, Channel<?, Object>> apply(
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
                          new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                              Function<Channel<?, Object>, Channel<?, Object>>>() {

                            public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                final Function<Channel<?, Object>, Channel<?, Object>> function) {
                              return new Function<Channel<?, Object>, Channel<?, Object>>() {

                                public Channel<?, Object> apply(final Channel<?, Object> objects) {
                                  throw new NullPointerException();
                                }
                              };
                            }
                          });
    try {
      builder.sync().close().throwError();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testMapAllConsumer() {
    assertThat(JRoutineStream.<String>withStream().async().mapAllAccept(new BiConsumer<List<?
        extends String>, Channel<String, ?>>() {

      public void accept(final List<?
          extends
          String> strings, final Channel<String, ?> result) {
        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
          builder.append(string);
        }

        result.pass(builder.toString());
      }
    }).call("test1", "test2", "test3").inMax(seconds(3)).all()).containsExactly("test1test2test3");
    assertThat(JRoutineStream.<String>withStream().sync().mapAllAccept(new BiConsumer<List<? extends
        String>, Channel<String, ?>>() {

      public void accept(final List<? extends
          String> strings, final Channel<String, ?> result) {
        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
          builder.append(string);
        }

        result.pass(builder.toString());
      }
    }).call("test1", "test2", "test3").all()).containsExactly("test1test2test3");
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
    assertThat(JRoutineStream.<String>withStream().async().mapAll(new Function<List<? extends
        String>, String>() {

      public String apply(final List<? extends String> strings) {
        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
          builder.append(string);
        }

        return builder.toString();
      }
    }).call("test1", "test2", "test3").inMax(seconds(3)).all()).containsExactly("test1test2test3");
    assertThat(JRoutineStream.<String>withStream().sync().mapAll(new Function<List<? extends
        String>, String>() {

      public String apply(final List<? extends String> strings) {
        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
          builder.append(string);
        }

        return builder.toString();
      }
    }).call("test1", "test2", "test3").all()).containsExactly("test1test2test3");
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
        }).call("test1", "test2").inMax(seconds(3)).all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream //
        .<String>withStream().sorted()
                             .asyncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
                             .inMax(seconds(3))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream//
        .<String>withStream().sync().mapAccept(new BiConsumer<String, Channel<String, ?>>() {

          public void accept(final String s, final Channel<String, ?> result) {
            result.pass(s.toUpperCase());
          }
        }).call("test1", "test2").all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream//
        .<String>withStream().syncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
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
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(factory)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync().map(factory).call("test1", "test2").all())
        .containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(factory)
                                                  .call("test1", "test2")
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
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(new UpperCase())
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(new UpperCase())
                                                  .call("test1", "test2")
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(new UpperCase())
                                                  .call("test1", "test2")
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
    }).call("test1", "test2").inMax(seconds(3)).all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sorted()
                                                  .asyncParallel()
                                                  .map(new Function<String, String>() {

                                                    public String apply(final String s) {
                                                      return s.toUpperCase();
                                                    }
                                                  })
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync().map(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }
    }).call("test1", "test2").all()).containsExactly("TEST1", "TEST2");
    assertThat(
        JRoutineStream.<String>withStream().syncParallel().map(new Function<String, String>() {

          public String apply(final String s) {
            return s.toUpperCase();
          }
        }).call("test1", "test2").all()).containsExactly("TEST1", "TEST2");
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
    final Runner testRunner = new SyncRunner() {

      @Override
      public void run(@NotNull final Execution execution, final long delay,
          @NotNull final TimeUnit timeUnit) {
        isCalled.set(true);
        execution.run();
      }
    };
    assertThat(JRoutineStream.<String>withStream().unsorted()
                                                  .mapOn(testRunner)
                                                  .call("test")
                                                  .inMax(seconds(1))
                                                  .all()).containsExactly("test");
    assertThat(isCalled.get()).isTrue();
  }

  @Test
  public void testMapRoutine() {
    final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                        .applyInvocationConfiguration()
                                                        .withOutputOrder(OrderType.SORTED)
                                                        .configured()
                                                        .buildRoutine();
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(routine)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().asyncParallel()
                                                  .map(routine)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync()
                                                  .map(routine)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(routine)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
  }

  @Test
  public void testMapRoutineBuilder() {
    final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
    assertThat(JRoutineStream.<String>withStream().async()
                                                  .map(builder)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().asyncParallel()
                                                  .map(builder)
                                                  .call("test1", "test2")
                                                  .inMax(seconds(3))
                                                  .all()).containsOnly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().sync().map(builder).call("test1", "test2").all())
        .containsExactly("TEST1", "TEST2");
    assertThat(JRoutineStream.<String>withStream().syncParallel()
                                                  .map(builder)
                                                  .call("test1", "test2")
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
          .<Integer>withStream().applyStreamInvocationConfiguration()
                                .withRunner(getSingleThreadRunner())
                                .configured()
                                .map(appendAccept(range(1, 1000)))
                                .applyStreamInvocationConfiguration()
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
                                .close()
                                .inMax(minutes(3))
                                .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }

    try {
      assertThat(JRoutineStream //
          .<Integer>withStream().applyStreamInvocationConfiguration()
                                .withRunner(getSingleThreadRunner())
                                .configured()
                                .map(appendAccept(range(1, 1000)))
                                .applyStreamInvocationConfiguration()
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
                                .close()
                                .inMax(minutes(3))
                                .next()).isCloseTo(21, Offset.offset(0.1));
      fail();

    } catch (final InputDeadlockException ignored) {
    }
  }

  @Test
  public void testStreamAccept() {
    assertThat(
        JRoutineStream.withStreamAccept(range(0, 3)).immediate().close().all()).containsExactly(0,
        1, 2, 3);
    assertThat(
        JRoutineStream.withStreamAccept(2, range(1, 0)).immediate().close().all()).containsExactly(
        1, 0, 1, 0);
  }

  @Test
  public void testStreamAcceptAbort() {
    Channel<Integer, Integer> channel =
        JRoutineStream.withStreamAccept(range(0, 3)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamAccept(2, range(1, 0)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamAcceptError() {
    assertThat(JRoutineStream.withStreamAccept(range(0, 3))
                             .immediate()
                             .call(31)
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(
        JRoutineStream.withStreamAccept(2, range(1, 0)).immediate().call(-17).getError().getCause())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamGet() {
    assertThat(
        JRoutineStream.withStreamGet(constant("test")).immediate().close().all()).containsExactly(
        "test");
    assertThat(JRoutineStream.withStreamGet(2, constant("test2"))
                             .immediate()
                             .close()
                             .all()).containsExactly("test2", "test2");
  }

  @Test
  public void testStreamGetAbort() {
    Channel<String, String> channel =
        JRoutineStream.withStreamGet(constant("test")).immediate().immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamGet(2, constant("test2")).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamGetError() {
    assertThat(JRoutineStream.withStreamGet(constant("test"))
                             .immediate()
                             .call("test")
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamGet(2, constant("test2"))
                             .immediate()
                             .call("test")
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamOf() {
    assertThat(JRoutineStream.withStreamOf("test").immediate().close().all()).containsExactly(
        "test");
    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .close()
                             .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                             .immediate()
                             .close()
                             .all()).containsExactly("test1", "test2", "test3");
    assertThat(
        JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                      .immediate()
                      .close()
                      .all()).containsExactly("test1", "test2", "test3");
  }

  @Test
  public void testStreamOfAbort() {
    Channel<String, String> channel = JRoutineStream.withStreamOf("test").immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamOf("test1", "test2", "test3").immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel =
        JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3")).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                            .immediate()
                            .call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamOfError() {
    assertThat(JRoutineStream.withStreamOf("test")
                             .immediate()
                             .call("test")
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .call("test")
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                             .immediate()
                             .call("test")
                             .getError()
                             .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(
        JRoutineStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                      .immediate()
                      .call("test")
                      .getError()
                      .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineStream.withStreamOf(
        JRoutineCore.ofInputs().buildChannel().bind(new TemplateChannelConsumer<Object>() {}))
                             .immediate()
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
    protected UpperCase() {
      super(null);
    }

    public void onInput(final String input, @NotNull final Channel<String, ?> result) {
      result.pass(input.toUpperCase());
    }
  }
}
