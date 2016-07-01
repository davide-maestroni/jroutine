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
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.stream.StreamChannels.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
public class StreamRoutineBuilderTest {

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
        final Channel<Object, Object> channel = JRoutineCore.io().buildChannel();
        final Channel<Object, Object> streamChannel =
                JRoutineStream.withStream().asyncCall(channel);
        channel.abort(new IllegalArgumentException());
        try {
            streamChannel.after(seconds(3)).throwError();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    public void testAnnotation() {
        // Just for coverage...
        assertThat(TransformationType.values()).containsOnly(TransformationType.START,
                // TODO: 01/07/16 remove START
                TransformationType.MAP, TransformationType.REDUCE, TransformationType.CACHE,
                TransformationType.COLLECT, TransformationType.CONFIG);
    }

    @Test
    public void testAppend() {
        assertThat(JRoutineStream.withStream()
                                 .append("test2")
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream.withStream()
                                 .append("test2", "test3")
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStream.withStream()
                                 .append(Arrays.asList("test2", "test3"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStream.withStream()
                                 .append(JRoutineCore.io().of("test2", "test3"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testAppend2() {
        assertThat(JRoutineStream.withStream().sync().appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).syncCall("test1").all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .appendGetMore(
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .syncCall("test1")
                                                      .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.withStream().sync().appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .appendGetMore(3,
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .syncCall("test1")
                                                      .all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async().appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .appendGetMore(
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .asyncCall("test1")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async().appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .appendGetMore(3,
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .asyncCall("test1")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel().appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .appendGetMore(
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .asyncCall("test1")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "TEST2");
        assertThat(
                JRoutineStream.<String>withStream().parallel().appendGet(3, new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .appendGetMore(3,
                                                              new Consumer<Channel<String, ?>>() {

                                                                  public void accept(
                                                                          final Channel<String,
                                                                                  ?>
                                                                                  resultChannel) {
                                                                      resultChannel.pass("TEST2");
                                                                  }
                                                              })
                                                      .asyncCall("test1")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
    }

    @Test
    public void testCollect() {
        assertThat(JRoutineStream.<StringBuilder>withStream().async()
                                                             .collect(
                                                                     new BiConsumer<StringBuilder, StringBuilder>() {

                                                                         public void accept(
                                                                                 final
                                                                                 StringBuilder
                                                                                         builder,
                                                                                 final
                                                                                 StringBuilder
                                                                                         builder2) {
                                                                             builder.append(
                                                                                     builder2);
                                                                         }
                                                                     })
                                                             .map(new Function<StringBuilder,
                                                                     String>() {

                                                                 public String apply(
                                                                         final StringBuilder
                                                                                 builder) {
                                                                     return builder.toString();
                                                                 }
                                                             })
                                                             .asyncCall(new StringBuilder("test1"),
                                                                     new StringBuilder("test2"),
                                                                     new StringBuilder("test3"))
                                                             .after(seconds(3))
                                                             .all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineStream.<StringBuilder>withStream().sync()
                                                             .collect(
                                                                     new BiConsumer<StringBuilder, StringBuilder>() {

                                                                         public void accept(
                                                                                 final
                                                                                 StringBuilder
                                                                                         builder,
                                                                                 final
                                                                                 StringBuilder
                                                                                         builder2) {
                                                                             builder.append(
                                                                                     builder2);
                                                                         }
                                                                     })
                                                             .map(new Function<StringBuilder,
                                                                     String>() {

                                                                 public String apply(
                                                                         final StringBuilder
                                                                                 builder) {
                                                                     return builder.toString();
                                                                 }
                                                             })
                                                             .asyncCall(new StringBuilder("test1"),
                                                                     new StringBuilder("test2"),
                                                                     new StringBuilder("test3"))
                                                             .after(seconds(3))
                                                             .all()).containsExactly(
                "test1test2test3");
    }

    @Test
    public void testCollectCollection() {
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .collectInto(new Supplier<List<String>>() {

                                                          public List<String> get() {
                                                              return new ArrayList<String>();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(3))
                                                      .next()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .collectInto(new Supplier<List<String>>() {

                                                          public List<String> get() {
                                                              return new ArrayList<String>();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(3))
                                                      .next()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .collectInto(new Supplier<List<String>>() {

                                                          public List<String> get() {
                                                              return new ArrayList<String>();
                                                          }
                                                      })
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .next()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .then("test1", "test2", "test3")
                                 .sync()
                                 .collectInto(new Supplier<List<String>>() {

                                     public List<String> get() {
                                         return new ArrayList<String>();
                                     }
                                 })
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {
        try {
            JRoutineStream.withStream().async().collectInto(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {
        try {
            JRoutineStream.withStream().async().collect(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testCollectSeed() {
        assertThat(
                JRoutineStream.<String>withStream().async().collect(new Supplier<StringBuilder>() {

                    public StringBuilder get() {
                        return new StringBuilder();
                    }
                }, new BiConsumer<StringBuilder, String>() {

                    public void accept(final StringBuilder b, final String s) {
                        b.append(s);
                    }
                }).map(new Function<StringBuilder, String>() {

                    public String apply(final StringBuilder builder) {
                        return builder.toString();
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(
                JRoutineStream.<String>withStream().sync().collect(new Supplier<StringBuilder>() {

                    public StringBuilder get() {
                        return new StringBuilder();
                    }
                }, new BiConsumer<StringBuilder, String>() {

                    public void accept(final StringBuilder b, final String s) {
                        b.append(s);
                    }
                }).map(new Function<StringBuilder, String>() {

                    public String apply(final StringBuilder builder) {
                        return builder.toString();
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(
                JRoutineStream.<String>withStream().sync().collect(new Supplier<StringBuilder>() {

                    public StringBuilder get() {
                        return new StringBuilder();
                    }
                }, new BiConsumer<StringBuilder, String>() {

                    public void accept(final StringBuilder b, final String s) {
                        b.append(s);
                    }
                }).map(new Function<StringBuilder, String>() {

                    public String apply(final StringBuilder builder) {
                        return builder.toString();
                    }
                }).asyncCall().close().after(seconds(3)).all()).containsExactly("");
        assertThat(JRoutineStream.<String>withStream().sync().collect(new Supplier<List<Object>>() {

            public List<Object> get() {
                return new ArrayList<Object>();
            }
        }, new BiConsumer<List<Object>, Object>() {

            public void accept(final List<Object> l, final Object o) {

                l.add(o);
            }
        }).asyncCall().close().after(seconds(3)).next()).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectSeedNullPointerError() {
        try {
            JRoutineStream.withStream().async().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testConfiguration() {
        assertThat(
                JRoutineStream.<String>withStream().parallel(1).map(new Function<String, String>() {

                    public String apply(final String s) {
                        return s.toUpperCase();
                    }
                }).asyncCall("test1", "test2").after(seconds(3)).all()).containsOnly("TEST1",
                "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel(1)
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel(1)
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toLowerCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .thenGetMore(range(1, 1000))
                                 .backoffOn(getSingleThreadRunner(), 2,
                                         Backoffs.linearDelay(seconds(10)))
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
                                 .reduce(new BiFunction<SumData, SumData, SumData>() {

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 })
                                 .map(new Function<SumData, Double>() {

                                     public Double apply(final SumData data) {
                                         return data.sum / data.count;
                                     }
                                 })
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .thenGetMore(range(1, 1000))
                                 .backoffOn(getSingleThreadRunner(), 2, 10, TimeUnit.SECONDS)
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
                                 .reduce(new BiFunction<SumData, SumData, SumData>() {

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 })
                                 .map(new Function<SumData, Double>() {

                                     public Double apply(final SumData data) {
                                         return data.sum / data.count;
                                     }
                                 })
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .thenGetMore(range(1, 1000))
                                 .backoffOn(getSingleThreadRunner(), 2, seconds(10))
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
                                 .reduce(new BiFunction<SumData, SumData, SumData>() {

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 })
                                 .map(new Function<SumData, Double>() {

                                     public Double apply(final SumData data) {
                                         return data.sum / data.count;
                                     }
                                 })
                                 .asyncMap(null)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testConsume() {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(JRoutineStream.<String>withStream().sync().onOutput(new Consumer<String>() {

            public void accept(final String s) {
                list.add(s);
            }
        }).syncCall("test1", "test2", "test3").all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(JRoutineStream.<String>withStream().async().onOutput(new Consumer<String>() {

            public void accept(final String s) {
                list.add(s);
            }
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testConsumeError() {
        try {
            JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

                public Object apply(final Object o) {
                    throw new NullPointerException();
                }
            }).onError(new Consumer<RoutineException>() {

                public void accept(final RoutineException e) {
                    throw new IllegalArgumentException();
                }
            }).syncCall("test").next();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).onError(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {
            }
        }).syncCall("test").all()).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumeErrorNullPointerError() {
        try {
            JRoutineStream.withStream().onError(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {
        final Consumer<Object> consumer = null;
        try {
            JRoutineStream.withStream().sync().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().delay(1, TimeUnit.SECONDS)
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().delay(seconds(1))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .delay(1, TimeUnit.SECONDS)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .delay(seconds(1))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testFilter() {
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .filter(Functions.isNotNull())
                                 .asyncCall(null, "test")
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream()
                                 .parallel()
                                 .filter(Functions.isNotNull())
                                 .asyncCall(null, "test")
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .filter(Functions.isNotNull())
                                 .syncCall(null, "test")
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream()
                                 .sequential()
                                 .filter(Functions.isNotNull())
                                 .asyncCall(null, "test")
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {
        try {
            JRoutineStream.withStream().async().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testFlatMap() {
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .flatMap(
                                                              new Function<String, Channel<?,
                                                                      String>>() {

                                                                  public Channel<?, String> apply(
                                                                          final String s) {

                                                                      return StreamChannels.of(s)
                                                                                           .sync()
                                                                                           .filter(Functions.<String>isNotNull());
                                                                  }
                                                              })
                                                      .syncCall("test1", null, "test2", null)
                                                      .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .flatMap(
                                                              new Function<String, Channel<?,
                                                                      String>>() {

                                                                  public Channel<?, String> apply(
                                                                          final String s) {

                                                                      return StreamChannels.of(s)
                                                                                           .sync()
                                                                                           .filter(Functions.<String>isNotNull());
                                                                  }
                                                              })
                                                      .syncCall("test1", null, "test2", null)
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .flatMap(
                                                              new Function<String, Channel<?,
                                                                      String>>() {

                                                                  public Channel<?, String> apply(
                                                                          final String s) {

                                                                      return StreamChannels.of(s)
                                                                                           .sync()
                                                                                           .filter(Functions.<String>isNotNull());
                                                                  }
                                                              })
                                                      .syncCall("test1", null, "test2", null)
                                                      .after(seconds(3))
                                                      .all()).containsOnly("test1", "test2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .flatMap(
                                                              new Function<String, Channel<?,
                                                                      String>>() {

                                                                  public Channel<?, String> apply(
                                                                          final String s) {

                                                                      return StreamChannels.of(s)
                                                                                           .sync()
                                                                                           .filter(Functions.<String>isNotNull());
                                                                  }
                                                              })
                                                      .syncCall("test1", null, "test2", null)
                                                      .after(seconds(3))
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
            JRoutineStream.withStream().parallel().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().flatMap(null);
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
                                             .tryCatchMore(
                                                     new BiConsumer<RoutineException,
                                                             Channel<String, ?>>() {

                                                         public void accept(
                                                                 final RoutineException e,
                                                                 final Channel<String, ?> channel) {
                                                             if (++count[0] < 3) {
                                                                 StreamChannels.of(o)
                                                                               .map(routine)
                                                                               .tryCatchMore(this)
                                                                               .bind(channel);

                                                             } else {
                                                                 throw e;
                                                             }
                                                         }
                                                     })
                                             .asyncCall(o);

                    }
                };

        try {
            JRoutineStream.withStream()
                          .async()
                          .flatMap(retryFunction)
                          .asyncCall((Object) null)
                          .after(seconds(3))
                          .all();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testFlatTransform() {
        assertThat(JRoutineStream.<String>withStream().flatLift(
                new Function<StreamRoutineBuilder<String, String>, StreamRoutineBuilder<String,
                        String>>() {

                    public StreamRoutineBuilder<String, String> apply(
                            final StreamRoutineBuilder<String, String> builder) {
                        return builder.append("test2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "test2");

        try {
            JRoutineStream.withStream()
                          .flatLift(
                                  new Function<StreamRoutineBuilder<Object, Object>,
                                          StreamRoutineBuilder<Object, Object>>() {

                                      public StreamRoutineBuilder<Object, Object> apply(
                                              final StreamRoutineBuilder<Object, Object> builder) {
                                          throw new NullPointerException();
                                      }
                                  });
            fail();

        } catch (final StreamException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testInvocationDeadlock() {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            JRoutineStream.<String>withStream().invocationConfiguration()
                                               .withRunner(runner1)
                                               .applied()
                                               .map(new Function<String, Object>() {

                                                   public Object apply(final String s) {
                                                       return JRoutineStream.<String>withStream()
                                                               .invocationConfiguration()
                                                                                                 .withRunner(
                                                                                                         runner1)
                                                                                                 .applied()
                                                                                                 .map(Functions
                                                                                                         .identity())
                                                                                                 .invocationConfiguration()
                                                                                                 .withRunner(
                                                                                                         runner2)
                                                                                                 .applied()
                                                                                                 .map(Functions
                                                                                                         .identity())
                                                                                                 .asyncCall(
                                                                                                         s)
                                                                                                 .after(minutes(
                                                                                                         3))
                                                                                                 .next();
                                                   }
                                               })
                                               .asyncCall("test")
                                               .after(minutes(3))
                                               .next();
            fail();

        } catch (final ExecutionDeadlockException ignored) {
        }
    }

    @Test
    public void testInvocationMode() {
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.ASYNC)
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(1))
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.PARALLEL)
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(1))
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.SYNC)
                                                      .syncCall("test1", "test2", "test3")
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.SEQUENTIAL)
                                                      .syncCall("test1", "test2", "test3")
                                                      .all()).containsExactly("test1", "test2",
                "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {
        try {
            JRoutineStream.withStream().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().lag(1, TimeUnit.SECONDS)
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().lag(seconds(1))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().lag(1, TimeUnit.SECONDS)
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().lag(seconds(1))
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testLimit() {
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .thenGetMore(range(1, 10))
                                 .async()
                                 .limit(5)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .thenGetMore(range(1, 10))
                                 .async()
                                 .limit(0)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .thenGetMore(range(1, 10))
                                 .async()
                                 .limit(15)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .thenGetMore(range(1, 10))
                                 .async()
                                 .limit(0)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
    }

    @Test
    public void testMapAllConsumer() {
        assertThat(JRoutineStream.<String>withStream().async().mapAllMore(new BiConsumer<List<?
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
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(
                JRoutineStream.<String>withStream().sync().mapAllMore(new BiConsumer<List<? extends
                        String>, Channel<String, ?>>() {

                    public void accept(final List<? extends
                            String> strings, final Channel<String, ?> result) {
                        final StringBuilder builder = new StringBuilder();
                        for (final String string : strings) {
                            builder.append(string);
                        }

                        result.pass(builder.toString());
                    }
                }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {
        try {
            JRoutineStream.withStream().async().mapAllMore(null);
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
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineStream.<String>withStream().sync().mapAll(new Function<List<? extends
                String>, String>() {

            public String apply(final List<? extends String> strings) {
                final StringBuilder builder = new StringBuilder();
                for (final String string : strings) {
                    builder.append(string);
                }

                return builder.toString();
            }
        }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
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
        assertThat(JRoutineStream.<String>withStream().mapMore(
                new BiConsumer<String, Channel<String, ?>>() {

                    public void accept(final String s, final Channel<String, ?> result) {
                        result.pass(s.toUpperCase());
                    }
                }).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly("TEST1",
                "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel()
                                                      .mapMore(
                                                              new BiConsumer<String,
                                                                      Channel<String, ?>>() {

                                                                  public void accept(final String s,
                                                                          final Channel<String,
                                                                                  ?> result) {
                                                                      result.pass(s.toUpperCase());
                                                                  }
                                                              })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .mapMore(
                                                              new BiConsumer<String,
                                                                      Channel<String, ?>>() {

                                                                  public void accept(final String s,
                                                                          final Channel<String,
                                                                                  ?> result) {
                                                                      result.pass(s.toUpperCase());
                                                                  }
                                                              })
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .mapMore(
                                                              new BiConsumer<String,
                                                                      Channel<String, ?>>() {

                                                                  public void accept(final String s,
                                                                          final Channel<String,
                                                                                  ?> result) {
                                                                      result.pass(s.toUpperCase());
                                                                  }
                                                              })
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {
        try {
            JRoutineStream.withStream().async().mapMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapFactory() {
        final InvocationFactory<String, String> factory = factoryOf(UpperCase.class);
        assertThat(StreamChannels.of("test1", "test2").async().map(factory).after(seconds(3)).all())
                .containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel()
                                                      .map(factory)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(factory)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(factory)
                                                      .syncCall("test1", "test2")
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
            JRoutineStream.withStream().parallel().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapFilter() {
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(new UpperCase())
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel()
                                                      .map(new UpperCase())
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(new UpperCase())
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(new UpperCase())
                                                      .syncCall("test1", "test2")
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
            JRoutineStream.withStream().parallel().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((MappingInvocation<Object, Object>) null);
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
        }).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sort(OrderType.BY_CALL)
                                                      .parallel()
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<String, String>() {

            public String apply(final String s) {
                return s.toUpperCase();
            }
        }).syncCall("test1", "test2").all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
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
            JRoutineStream.withStream().parallel().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapRoutine() {
        final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applied()
                                                            .buildRoutine();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(routine)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .map(routine)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(routine)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(routine)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    public void testMapRoutineBuilder() {
        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(builder)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .map(builder)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(builder)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(builder)
                                                      .syncCall("test1", "test2")
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
            JRoutineStream.withStream().parallel().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((RoutineBuilder<Object, Object>) null);
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
            JRoutineStream.withStream().parallel().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
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
        protected UpperCase() {
            super(null);
        }

        public void onInput(final String input, @NotNull final Channel<String, ?> result) {
            result.pass(input.toUpperCase());
        }
    }
}
