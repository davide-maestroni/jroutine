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
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;
import com.github.dm.jrt.stream.builder.StreamBuildingException;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.onOutput;
import static com.github.dm.jrt.stream.input.Streams.range;
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
        assertThat(TransformationType.values()).containsOnly(TransformationType.MAP,
                TransformationType.REDUCE, TransformationType.COLLECT, TransformationType.CONFIG);
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
        assertThat(JRoutineStream //
                .<String>withStream().sync().appendMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).syncCall("test1").all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.withStream().sync().appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().sync().appendMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).syncCall("test1").all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async().appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().async().appendMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async().appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().async().appendMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel().appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel().appendMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(
                JRoutineStream.<String>withStream().parallel().appendGet(3, new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel().appendMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
    }

    @Test
    public void testCall() {
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .call()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .call("test")
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream().async().call("test", "test").after(seconds(3)).all())
                .containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .call(Arrays.asList("test", "test"))
                                 .after(seconds(3))
                                 .all()).containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .call(JRoutineCore.io().of("test"))
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream()
                                 .parallel()
                                 .call()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .parallel()
                                 .call("test")
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(
                JRoutineStream.withStream().parallel().call("test", "test").after(seconds(3)).all())
                .containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .parallel()
                                 .call(Arrays.asList("test", "test"))
                                 .after(seconds(3))
                                 .all()).containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .parallel()
                                 .call(JRoutineCore.io().of("test"))
                                 .after(seconds(3))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream().sync().call().close().all()).isEmpty();
        assertThat(JRoutineStream.withStream().sync().call("test").all()).containsExactly("test");
        assertThat(JRoutineStream.withStream().sync().call("test", "test").all()).containsExactly(
                "test", "test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .call(Arrays.asList("test", "test"))
                                 .all()).containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .call(JRoutineCore.io().of("test"))
                                 .all()).containsExactly("test");
        assertThat(JRoutineStream.withStream().sequential().call().close().all()).isEmpty();
        assertThat(JRoutineStream.withStream().sequential().call("test").all()).containsExactly(
                "test");
        assertThat(JRoutineStream.withStream()
                                 .sequential()
                                 .call("test", "test")
                                 .all()).containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .sequential()
                                 .call(Arrays.asList("test", "test"))
                                 .all()).containsExactly("test", "test");
        assertThat(JRoutineStream.withStream()
                                 .sequential()
                                 .call(JRoutineCore.io().of("test"))
                                 .all()).containsExactly("test");
    }

    @Test
    public void testChannel() {
        try {
            JRoutineStream.withStream().sync().all();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().next(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().nextOrElse(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().skipNext(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream().sync().abort()).isTrue();
        assertThat(JRoutineStream.withStream().sync().abort(new IllegalStateException())).isTrue();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .after(seconds(3))
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .after(3, TimeUnit.SECONDS)
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        try {
            final ArrayList<String> results = new ArrayList<String>();
            JRoutineStream.withStream().sync().andThen("test").allInto(results).hasCompleted();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream()
                          .sync()
                          .andThen("test")
                          .bind(JRoutineCore.io().buildChannel())
                          .next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .bind(onOutput(new Consumer<String>() {

                                     public void accept(final String s) {
                                         assertThat(s).isEqualTo("test");
                                     }
                                 }))
                                 .close()
                                 .getError()).isNull();
        assertThat(JRoutineStream.withStream().sync().andThen("test").close().next()).isEqualTo(
                "test");
        try {
            JRoutineStream.withStream().sync().andThen("test").eventualIterator().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().andThen("test").iterator().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(
                JRoutineStream.withStream().sync().andThen("test").eventuallyAbort().close().next())
                .isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .eventuallyAbort(new IllegalStateException())
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(
                JRoutineStream.withStream().sync().andThen("test").eventuallyBreak().close().next())
                .isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .eventuallyFail()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().getError()).isNull();
        assertThat(JRoutineStream.withStream().sync().hasCompleted()).isFalse();
        try {
            JRoutineStream.withStream().sync().hasNext();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThen("test")
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().inputCount()).isZero();
        assertThat(JRoutineStream.withStream().sync().outputCount()).isZero();
        assertThat(JRoutineStream.withStream().sync().size()).isZero();
        assertThat(JRoutineStream.withStream().sync().isBound()).isFalse();
        assertThat(JRoutineStream.withStream().sync().isEmpty()).isTrue();
        assertThat(JRoutineStream.withStream().sync().isOpen()).isTrue();
        assertThat(JRoutineStream.withStream().sync().pass("test").next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().pass("test", "test").next()).isEqualTo(
                "test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .pass(Arrays.asList("test", "test"))
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .pass(JRoutineCore.io().of("test"))
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().sortedByCall().pass("test").next()).isEqualTo(
                "test");
        assertThat(
                JRoutineStream.withStream().sync().sortedByDelay().pass("test").next()).isEqualTo(
                "test");
        JRoutineStream.withStream().sync().throwError();
        try {
            JRoutineStream.withStream().sync().remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testCollect() {
        assertThat(JRoutineStream //
                .<StringBuilder>withStream().async()
                                            .collect(
                                                    new BiConsumer<StringBuilder, StringBuilder>() {

                                                        public void accept(
                                                                final StringBuilder builder,
                                                                final StringBuilder builder2) {
                                                            builder.append(builder2);
                                                        }
                                                    })
                                            .map(new Function<StringBuilder, String>() {

                                                public String apply(final StringBuilder builder) {
                                                    return builder.toString();
                                                }
                                            })
                                            .asyncCall(new StringBuilder("test1"),
                                                    new StringBuilder("test2"),
                                                    new StringBuilder("test3"))
                                            .after(seconds(3))
                                            .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStream //
                .<StringBuilder>withStream().sync()
                                            .collect(
                                                    new BiConsumer<StringBuilder, StringBuilder>() {

                                                        public void accept(
                                                                final StringBuilder builder,
                                                                final StringBuilder builder2) {
                                                            builder.append(builder2);
                                                        }
                                                    })
                                            .map(new Function<StringBuilder, String>() {

                                                public String apply(final StringBuilder builder) {
                                                    return builder.toString();
                                                }
                                            })
                                            .asyncCall(new StringBuilder("test1"),
                                                    new StringBuilder("test2"),
                                                    new StringBuilder("test3"))
                                            .after(seconds(3))
                                            .all()).containsExactly("test1test2test3");
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
                                 .andThen("test1", "test2", "test3")
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
        assertThat(JRoutineStream.<String>withStream().sorted(OrderType.BY_CALL)
                                                      .parallel(1)
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted(OrderType.BY_CALL)
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
                                 .andThenMore(range(1, 1000))
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
                                 .andThenMore(range(1, 1000))
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
                                 .andThenMore(range(1, 1000))
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
        assertThat(JRoutineStream //
                .<String>withStream().sync().flatMap(new Function<String, Channel<?, String>>() {

                    public Channel<?, String> apply(final String s) {
                        return JRoutineStream.<String>withStream().sync()
                                                                  .filter(Functions
                                                                          .<String>isNotNull())
                                                                  .syncCall(s);
                    }
                }).syncCall("test1", null, "test2", null).all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().async().flatMap(new Function<String, Channel<?, String>>() {

                    public Channel<?, String> apply(final String s) {
                        return JRoutineStream.<String>withStream().sync()
                                                                  .filter(Functions
                                                                          .<String>isNotNull())
                                                                  .syncCall(s);
                    }
                }).syncCall("test1", null, "test2", null).after(seconds(3)).all()).containsExactly(
                "test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStream.<String>withStream().sync()
                                                                                       .filter(Functions.<String>isNotNull())
                                                                                       .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .after(seconds(3))
                                     .all()).containsOnly("test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().sequential()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStream.<String>withStream().sync()
                                                                                       .filter(Functions.<String>isNotNull())
                                                                                       .syncCall(s);
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
                                                                 JRoutineStream.withStream()
                                                                               .map(routine)
                                                                               .tryCatchMore(this)
                                                                               .asyncCall(o)
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
                new Function<StreamBuilder<String, String>, StreamBuilder<String, String>>() {

                    public StreamBuilder<String, String> apply(
                            final StreamBuilder<String, String> builder) {
                        return builder.append("test2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "test2");

        try {
            JRoutineStream.withStream()
                          .flatLift(
                                  new Function<StreamBuilder<Object, Object>,
                                          StreamBuilder<Object, Object>>() {

                                      public StreamBuilder<Object, Object> apply(
                                              final StreamBuilder<Object, Object> builder) {
                                          throw new NullPointerException();
                                      }
                                  });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        assertThat(JRoutineStream.<String>withStream().flatLiftWithConfig(
                new BiFunction<StreamConfiguration, StreamBuilder<String, String>,
                        StreamBuilder<String, String>>() {

                    public StreamBuilder<String, String> apply(
                            final StreamConfiguration streamConfiguration,
                            final StreamBuilder<String, String> builder) {
                        return builder.append("test2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "test2");

        try {
            JRoutineStream.withStream()
                          .flatLiftWithConfig(
                                  new BiFunction<StreamConfiguration, StreamBuilder<Object,
                                          Object>, StreamBuilder<Object, Object>>() {

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
    public void testInvocationDeadlock() {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            final Function<String, Object> function = new Function<String, Object>() {

                public Object apply(final String s) {
                    return JRoutineStream.<String>withStream().invocationConfiguration()
                                                              .withRunner(runner1)
                                                              .applied()
                                                              .map(Functions.identity())
                                                              .invocationConfiguration()
                                                              .withRunner(runner2)
                                                              .applied()
                                                              .map(Functions.identity())
                                                              .asyncCall(s)
                                                              .after(minutes(3))
                                                              .next();
                }
            };
            JRoutineStream.<String>withStream().invocationConfiguration()
                                               .withRunner(runner1)
                                               .applied()
                                               .map(function)
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
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .limit(5)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .limit(0)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .limit(15)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
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
        assertThat(JRoutineStream //
                .<String>withStream().sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(3))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream//
                .<String>withStream().sync().mapMore(new BiConsumer<String, Channel<String, ?>>() {

                    public void accept(final String s, final Channel<String, ?> result) {
                        result.pass(s.toUpperCase());
                    }
                }).syncCall("test1", "test2").all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream//
                .<String>withStream().sequential()
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
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
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(factory)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted(OrderType.BY_CALL)
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
        assertThat(JRoutineStream.<String>withStream().sorted(OrderType.BY_CALL)
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
        assertThat(JRoutineStream.<String>withStream().sorted(OrderType.BY_CALL)
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

    @Test
    public void testMaxSizeDeadlock() {
        try {
            assertThat(JRoutineStream.withStream()
                                     .andThenMore(range(1, 1000))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withInputLimit(2)
                                     .withInputBackoff(seconds(3))
                                     .withOutputLimit(2)
                                     .withOutputBackoff(seconds(3))
                                     .applied()
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
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));
            fail();

        } catch (final InputDeadlockException ignored) {
        }

        try {
            assertThat(JRoutineStream.withStream()
                                     .andThenMore(range(1, 1000))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withOutputLimit(2)
                                     .withOutputBackoff(seconds(3))
                                     .applied()
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
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));

        } catch (final OutputDeadlockException ignored) {
        }

        try {
            assertThat(JRoutineStream.withStream()
                                     .andThenMore(range(1, 1000))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withInputLimit(2)
                                     .withInputBackoff(seconds(3))
                                     .applied()
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
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));
            fail();

        } catch (final InputDeadlockException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testOnComplete() {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStream.<String>withStream().onComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        }).asyncCall("test").after(seconds(3)).all()).isEmpty();
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStream.<String>withStream().map(new Function<String, String>() {

            public String apply(final String s) throws Exception {
                throw new NoSuchElementException();
            }
        }).onComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        }).asyncCall("test").after(seconds(3)).getError()).isExactlyInstanceOf(
                InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    public void testOrElse() {
        assertThat(JRoutineStream.<String>withStream().orElse("est")
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test");
        assertThat(JRoutineStream.<String>withStream().orElse("est1", "est2")
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test");
        assertThat(JRoutineStream.<String>withStream().orElse(Arrays.asList("est1", "est2"))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test");
        assertThat(
                JRoutineStream.<String>withStream().orElseMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> result) {
                        result.pass("est");
                    }
                }).asyncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineStream.<String>withStream().orElseGet(new Supplier<String>() {

            public String get() {
                return "est";
            }
        }).asyncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineStream.<String>withStream().orElse("est")
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).containsExactly("est");
        assertThat(JRoutineStream.<String>withStream().orElse("est1", "est2")
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).containsExactly("est1", "est2");
        assertThat(JRoutineStream.<String>withStream().orElse(Arrays.asList("est1", "est2"))
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).containsExactly("est1", "est2");
        assertThat(
                JRoutineStream.<String>withStream().orElseMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> result) {
                        result.pass("est");
                    }
                }).asyncCall().close().after(seconds(3)).all()).containsExactly("est");
        assertThat(JRoutineStream.<String>withStream().orElseMore(2,
                new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> result) {
                        result.pass("est");
                    }
                }).asyncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
        assertThat(JRoutineStream.<String>withStream().orElseGet(new Supplier<String>() {

            public String get() {
                return "est";
            }
        }).asyncCall().close().after(seconds(3)).all()).containsExactly("est");
        assertThat(JRoutineStream.<String>withStream().orElseGet(2, new Supplier<String>() {

            public String get() {
                return "est";
            }
        }).asyncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {
        try {
            JRoutineStream.withStream().orElseMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().orElseMore(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().orElseGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().orElseGet(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testOrElseThrow() {
        assertThat(JRoutineStream.<String>withStream().orElseThrow(new IllegalStateException())
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test");
        assertThat(JRoutineStream.<String>withStream().orElseThrow(new IllegalStateException())
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .getError()
                                                      .getCause()).isExactlyInstanceOf(
                IllegalStateException.class);
    }

    @Test
    public void testPeekComplete() {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStream.<String>withStream().async().peekComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test2", "test3");
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStream.<String>withStream().map(new Function<String, String>() {

            public String apply(final String s) throws Exception {
                throw new NoSuchElementException();
            }
        }).peekComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        }).asyncCall("test1").after(seconds(3)).getError()).isExactlyInstanceOf(
                InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    public void testPeekError() {
        final AtomicBoolean isError = new AtomicBoolean(false);
        final Channel<String, String> channel = //
                JRoutineStream.<String>withStream().sync()
                                                   .peekError(new Consumer<RoutineException>() {

                                                       public void accept(
                                                               final RoutineException e) {
                                                           isError.set(true);
                                                       }
                                                   })
                                                   .asyncCall();
        assertThat(channel.abort()).isTrue();
        assertThat(channel.after(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
        assertThat(isError.get()).isTrue();
        isError.set(false);
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .peekError(new Consumer<RoutineException>() {

                                                          public void accept(
                                                                  final RoutineException e) {
                                                              isError.set(true);
                                                          }
                                                      })
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("test");
        assertThat(isError.get()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testPeekNullPointerError() {
        try {
            JRoutineStream.withStream().async().peekOutput(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().peekComplete(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().peekError(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testPeekOutput() {
        final ArrayList<String> data = new ArrayList<String>();
        assertThat(JRoutineStream.<String>withStream().async().peekOutput(new Consumer<String>() {

            public void accept(final String s) {
                data.add(s);
            }
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testReduce() {
        assertThat(JRoutineStream //
                .<String>withStream().async().reduce(new BiFunction<String, String, String>() {

                    public String apply(final String s, final String s2) {
                        return s + s2;
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineStream //
                .<String>withStream().sync().reduce(new BiFunction<String, String, String>() {

                    public String apply(final String s, final String s2) {
                        return s + s2;
                    }
                }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceNullPointerError() {
        try {
            JRoutineStream.withStream().async().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testReduceSeed() {
        assertThat(
                JRoutineStream.<String>withStream().async().reduce(new Supplier<StringBuilder>() {

                    public StringBuilder get() {
                        return new StringBuilder();
                    }
                }, new BiFunction<StringBuilder, String, StringBuilder>() {

                    public StringBuilder apply(final StringBuilder b, final String s) {
                        return b.append(s);
                    }
                }).map(new Function<StringBuilder, String>() {

                    public String apply(final StringBuilder builder) {
                        return builder.toString();
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineStream.<String>withStream().sync().reduce(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiFunction<StringBuilder, String, StringBuilder>() {

            public StringBuilder apply(final StringBuilder b, final String s) {
                return b.append(s);
            }
        }).map(new Function<StringBuilder, String>() {

            public String apply(final StringBuilder builder) {
                return builder.toString();
            }
        }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceSeedNullPointerError() {
        try {
            JRoutineStream.withStream().async().reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().reduce(null, null);
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
                                               .retry(2)
                                               .asyncCall("test")
                                               .after(seconds(3))
                                               .throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        final AtomicInteger count2 = new AtomicInteger();
        assertThat(JRoutineStream.<String>withStream().map(new UpperCase())
                                                      .map(factoryOf(ThrowException.class, count2,
                                                              1))
                                                      .retry(1)
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStream.<String>withStream().map(new AbortInvocation())
                                               .map(factoryOf(ThrowException.class, count3))
                                               .retry(2)
                                               .asyncCall("test")
                                               .after(seconds(3))
                                               .throwError();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void testSkip() {
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .skip(5)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .skip(15)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .andThenMore(range(1, 10))
                                 .async()
                                 .skip(0)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testSplit() {
        final StreamBuilder<Integer, Long> sqr =
                JRoutineStream.<Integer>withStream().map(new Function<Integer, Long>() {

                    public Long apply(final Integer number) {
                        final long value = number.longValue();
                        return value * value;
                    }
                });
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallel(2, sqr.buildFactory())
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallel(2, sqr)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallelBy(Functions.<Integer>identity(), sqr.buildFactory())
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallelBy(Functions.<Integer>identity(), sqr)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallel(2,
                                         JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
        assertThat(JRoutineStream.withStream()
                                 .andThenMore(range(1, 3))
                                 .parallelBy(Functions.<Integer>identity(),
                                         JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
    }

    @Test
    public void testStraight() {
        assertThat(JRoutineStream.withStream()
                                 .straight()
                                 .andThenMore(range(1, 1000))
                                 .streamInvocationConfiguration()
                                 .withInputMaxSize(1)
                                 .withOutputMaxSize(1)
                                 .applied()
                                 .map(new Function<Number, Double>() {

                                     public Double apply(final Number number) {
                                         return Math.sqrt(number.doubleValue());
                                     }
                                 })
                                 .map(Operators.averageDouble())
                                 .syncCall()
                                 .close()
                                 .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testThen() {
        assertThat(JRoutineStream.<String>withStream().sync().andThenGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).syncCall("test1").all()).containsOnly("TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().sync().andThenMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).syncCall("test1").all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sync().andThenGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().sync().andThenMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).syncCall("test1").all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async().andThenGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        }).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().async().andThenMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(
                JRoutineStream.<String>withStream().async().andThenGet(3, new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().async().andThenMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2", "TEST2",
                "TEST2");
        assertThat(
                JRoutineStream.<String>withStream().parallel().andThenGet(new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel().andThenMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel().andThenGet(3, new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel().andThenMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
    }

    @Test
    public void testThen2() {
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen((String) null)
                                                      .call("test1")
                                                      .all()).containsOnly((String) null);
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen((String[]) null)
                                                      .call("test1")
                                                      .all()).isEmpty();
        assertThat(
                JRoutineStream.<String>withStream().sync().andThen().call("test1").all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen((List<String>) null)
                                                      .call("test1")
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen(Collections.<String>emptyList())
                                                      .call("test1")
                                                      .all()).isEmpty();
        assertThat(
                JRoutineStream.<String>withStream().sync().andThen("TEST2").syncCall("test1").all())
                .containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen("TEST2", "TEST2")
                                                      .call("test1")
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .andThen(JRoutineCore.io()
                                                                           .of("TEST2", "TEST2"))
                                                      .call("test1")
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen((String) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly((String) null);
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen((String[]) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen()
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen((List<String>) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen(Collections.<String>emptyList())
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen("TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen("TEST2", "TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .andThen(JRoutineCore.io()
                                                                           .of("TEST2", "TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen((String) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly((String) null);
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen((String[]) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen()
                                                      .asyncCall("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen((List<String>) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen(Collections.<String>emptyList())
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen("TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen("TEST2", "TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .andThen(JRoutineCore.io()
                                                                           .of("TEST2", "TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen((String) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly((String) null);
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen((String[]) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen()
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen((List<String>) null)
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen(Collections.<String>emptyList())
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).isEmpty();
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen("TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen("TEST2", "TEST2")
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen(Collections.singletonList("TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .andThen(JRoutineCore.io()
                                                                           .of("TEST2", "TEST2"))
                                                      .call("test1")
                                                      .after(seconds(1))
                                                      .all()).containsOnly("TEST2");
    }

    @Test
    public void testThenNegativeCount() {
        try {
            JRoutineStream.withStream().sync().andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStream.withStream().async().andThenGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenMore(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {
        try {
            JRoutineStream.withStream().sync().andThenMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().andThenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().andThenMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().andThenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testTransform() {
        assertThat(JRoutineStream.<String>withStream().liftWithConfig(
                new BiFunction<StreamConfiguration, Function<Channel<?, String>, Channel<?,
                        String>>, Function<Channel<?, String>, Channel<?, String>>>() {

                    public Function<Channel<?, String>, Channel<?, String>> apply(
                            final StreamConfiguration configuration,
                            final Function<Channel<?, String>, Channel<?, String>> function) {
                        assertThat(configuration.asChannelConfiguration()).isEqualTo(
                                ChannelConfiguration.defaultConfiguration());
                        assertThat(configuration.asInvocationConfiguration()).isEqualTo(
                                InvocationConfiguration.defaultConfiguration());
                        assertThat(configuration.getInvocationMode()).isEqualTo(
                                InvocationMode.ASYNC);
                        return Functions.decorate(function)
                                        .andThen(
                                                new Function<Channel<?, String>, Channel<?,
                                                        String>>() {

                                                    public Channel<?, String> apply(
                                                            final Channel<?, String> channel) {
                                                        return JRoutineCore.with(new UpperCase())
                                                                           .asyncCall(channel);
                                                    }
                                                });
                    }
                }).asyncCall("test").after(seconds(3)).next()).isEqualTo("TEST");
        assertThat(JRoutineStream.<String>withStream().lift(
                new Function<Function<Channel<?, String>, Channel<?, String>>,
                        Function<Channel<?, String>, Channel<?, String>>>() {

                    public Function<Channel<?, String>, Channel<?, String>> apply(
                            final Function<Channel<?, String>, Channel<?, String>> function) {
                        return Functions.decorate(function)
                                        .andThen(
                                                new Function<Channel<?, String>, Channel<?,
                                                        String>>() {

                                                    public Channel<?, String> apply(
                                                            final Channel<?, String> channel) {
                                                        return JRoutineCore.with(new UpperCase())
                                                                           .asyncCall(channel);
                                                    }
                                                });
                    }
                }).asyncCall("test").after(seconds(3)).next()).isEqualTo("TEST");
        try {
            JRoutineStream.withStream()
                          .liftWithConfig(
                                  new BiFunction<StreamConfiguration, Function<Channel<?,
                                          Object>, Channel<?, Object>>, Function<Channel<?,
                                          Object>, Channel<?, Object>>>() {

                                      public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                              final StreamConfiguration configuration,
                                              final Function<Channel<?, Object>, Channel<?,
                                                      Object>> function) {
                                          throw new NullPointerException();
                                      }
                                  });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        try {
            JRoutineStream.withStream()
                          .lift(new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                                  Function<Channel<?, Object>, Channel<?, Object>>>() {

                              public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                      final Function<Channel<?, Object>, Channel<?, Object>>
                                              function) {
                                  throw new NullPointerException();
                              }
                          });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        final StreamBuilder<Object, Object> builder = //
                JRoutineStream.withStream()
                              .lift(new Function<Function<Channel<?, Object>, Channel<?,
                                      Object>>, Function<Channel<?, Object>, Channel<?, Object>>>
                                      () {

                                  public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                          final Function<Channel<?, Object>, Channel<?, Object>>
                                                  function) {
                                      return new Function<Channel<?, Object>, Channel<?, Object>>
                                              () {

                                          public Channel<?, Object> apply(
                                                  final Channel<?, Object> objects) {
                                              throw new NullPointerException();
                                          }
                                      };
                                  }
                              });
        try {
            builder.syncCall().close().throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testTryCatch() {
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                throw new NullPointerException();
            }
        }).tryCatchMore(new BiConsumer<RoutineException, Channel<Object, ?>>() {

            public void accept(final RoutineException e, final Channel<Object, ?> channel) {
                channel.pass("exception");
            }
        }).syncCall("test").next()).isEqualTo("exception");
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                return o;
            }
        }).tryCatchMore(new BiConsumer<RoutineException, Channel<Object, ?>>() {

            public void accept(final RoutineException e, final Channel<Object, ?> channel) {
                channel.pass("exception");
            }
        }).syncCall("test").next()).isEqualTo("test");
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                throw new NullPointerException();
            }
        }).tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {
                return "exception";
            }
        }).syncCall("test").next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        try {
            JRoutineStream.withStream().tryCatchMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().tryCatch(null);
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
            }).tryFinally(new Action() {

                public void perform() {
                    isRun.set(true);
                }
            }).syncCall("test").next();

        } catch (final RoutineException ignored) {
        }

        assertThat(isRun.getAndSet(false)).isTrue();
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                return o;
            }
        }).tryFinally(new Action() {

            public void perform() {
                isRun.set(true);
            }
        }).syncCall("test").next()).isEqualTo("test");
        assertThat(isRun.getAndSet(false)).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryFinallyNullPointerError() {
        try {
            JRoutineStream.withStream().tryFinally(null);
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
