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

import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
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
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.StreamChannel.StreamConfiguration;
import com.github.dm.jrt.stream.annotation.StreamFlow.TransformationType;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.days;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream output channel unit tests.
 * <p>
 * Created by davide-maestroni on 10/22/2015.
 */
public class StreamChannelTest {

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
        final StreamChannel<Object, Object> streamChannel = Streams.streamOf(channel);
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
                TransformationType.MAP, TransformationType.REDUCE, TransformationType.CACHE,
                TransformationType.COLLECT, TransformationType.CONFIG);
    }

    @Test
    public void testAppend() {

        assertThat(
                Streams.streamOf("test1").append("test2").after(seconds(3)).all()).containsExactly(
                "test1", "test2");
        assertThat(Streams.streamOf("test1")
                          .append("test2", "test3")
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1")
                          .append(Arrays.asList("test2", "test3"))
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1")
                          .append(JRoutineCore.io().of("test2", "test3"))
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testAppend2() {

        assertThat(Streams.streamOf("test1").sync().appendGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).all()).containsExactly("test1", "TEST2");
        assertThat(
                Streams.streamOf("test1").sync().appendGetMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).all()).containsExactly("test1", "TEST2");
        assertThat(Streams.streamOf("test1").sync().appendGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .sync()
                          .appendGetMore(3, new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").async().appendGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(
                Streams.streamOf("test1").async().appendGetMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(Streams.streamOf("test1").async().appendGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .async()
                          .appendGetMore(3, new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").parallel().appendGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .appendGetMore(new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1", "TEST2");
        assertThat(Streams.streamOf("test1").parallel().appendGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .appendGetMore(3, new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
    }

    @Test
    public void testBind() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        Streams.streamOf("test").bind(new TemplateOutputConsumer<String>() {

            @Override
            public void onOutput(final String s) throws Exception {
                semaphore.release();
            }
        });
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
        Streams.streamOf("test").onOutput(new Consumer<String>() {

            public void accept(final String s) throws Exception {

                semaphore.release();
            }
        }).bind();
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
        Streams.streamOf("test")
               .bindAfter(10, TimeUnit.MILLISECONDS, new TemplateOutputConsumer<String>() {

                   @Override
                   public void onOutput(final String s) throws Exception {
                       semaphore.release();
                   }
               });
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
        Streams.streamOf("test").onOutput(new Consumer<String>() {

            public void accept(final String s) throws Exception {

                semaphore.release();
            }
        }).bindAfter(10, TimeUnit.MILLISECONDS);
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
        Streams.streamOf("test").bindAfter(millis(10), new TemplateOutputConsumer<String>() {

            @Override
            public void onOutput(final String s) throws Exception {
                semaphore.release();
            }
        });
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
        Streams.streamOf("test").onOutput(new Consumer<String>() {

            public void accept(final String s) throws Exception {

                semaphore.release();
            }
        }).bindAfter(millis(10));
        assertThat(semaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testBuilder() {

        assertThat(Streams.streamOf().after(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test").after(seconds(1)).all()).containsExactly("test");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(Arrays.asList("test1", "test2", "test3"))
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testChannel() {

        StreamChannel<String, String> channel = Streams.streamOf("test");
        assertThat(channel.abort()).isFalse();
        assertThat(channel.abort(null)).isFalse();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        assertThat(channel.isBound()).isFalse();
        final ArrayList<String> results = new ArrayList<String>();
        assertThat(channel.after(1, TimeUnit.SECONDS).hasNext()).isTrue();
        channel.immediately().allInto(results);
        assertThat(results).containsExactly("test");
        channel = Streams.streamOf("test1", "test2", "test3");

        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyBreak().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyBreak().nextOrElse("test4")).isEqualTo("test4");

        final Iterator<String> iterator = Streams.streamOf("test1", "test2", "test3").iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");

        try {
            iterator.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel = Streams.streamOf(
                JRoutineCore.io().<String>buildChannel().after(days(1)).pass("test"));

        try {
            channel.eventuallyThrow().next();
            fail();

        } catch (final TimeoutException ignored) {

        }

        try {
            channel.eventuallyBreak().next();
            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {
            channel.eventuallyAbort().next();
            fail();

        } catch (final AbortException ignored) {

        }

        try {
            channel.eventuallyAbort(new IllegalArgumentException()).next();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isNull();
        }

        channel = Streams.streamOf(
                JRoutineCore.io().<String>buildChannel().after(seconds(1)).pass("test"));

        try {
            channel.eventuallyAbort(new IllegalArgumentException()).next();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testCollect() {

        assertThat(Streams.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
                new StringBuilder("test3"))
                          .async()
                          .collect(new BiConsumer<StringBuilder, StringBuilder>() {

                              public void accept(final StringBuilder builder,
                                      final StringBuilder builder2) {

                                  builder.append(builder2);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
                new StringBuilder("test3"))
                          .sync()
                          .collect(new BiConsumer<StringBuilder, StringBuilder>() {

                              public void accept(final StringBuilder builder,
                                      final StringBuilder builder2) {

                                  builder.append(builder2);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    public void testCollectCollection() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .collectInto(new Supplier<List<String>>() {

                              public List<String> get() {

                                  return new ArrayList<String>();
                              }
                          })
                          .after(seconds(3))
                          .next()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .collectInto(new Supplier<List<String>>() {

                              public List<String> get() {

                                  return new ArrayList<String>();
                              }
                          })
                          .after(seconds(3))
                          .next()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {

        try {
            Streams.streamOf().async().collectInto(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {

        try {
            Streams.streamOf().async().collect(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCollectSeed() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .collect(new Supplier<StringBuilder>() {

                              public StringBuilder get() {

                                  return new StringBuilder();
                              }
                          }, new BiConsumer<StringBuilder, String>() {

                              public void accept(final StringBuilder b, final String s) {

                                  b.append(s);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .collect(new Supplier<StringBuilder>() {

                              public StringBuilder get() {

                                  return new StringBuilder();
                              }
                          }, new BiConsumer<StringBuilder, String>() {

                              public void accept(final StringBuilder b, final String s) {

                                  b.append(s);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectSeedNullPointerError() {

        try {
            Streams.streamOf().async().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfiguration() {

        assertThat(
                Streams.streamOf("test1", "test2").parallel(1).map(new Function<String, String>() {

                    public String apply(final String s) {

                        return s.toUpperCase();
                    }
                }).after(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
                          .parallel(1)
                          .map(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
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
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(1, 1000))
                          .backoffOn(getSingleThreadRunner(), 2, Backoffs.linearDelay(seconds(10)))
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

                              public SumData apply(final SumData data1, final SumData data2) {

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
                          .after(seconds(3))
                          .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(Streams.streamOf()
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

                              public SumData apply(final SumData data1, final SumData data2) {

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
                          .after(seconds(3))
                          .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(Streams.streamOf()
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

                              public SumData apply(final SumData data1, final SumData data2) {

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
                          .after(seconds(3))
                          .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testConsume() {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(
                Streams.streamOf("test1", "test2", "test3").sync().onOutput(new Consumer<String>() {

                    public void accept(final String s) {

                        list.add(s);
                    }
                }).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .onOutput(new Consumer<String>() {

                              public void accept(final String s) {

                                  list.add(s);
                              }
                          })
                          .after(seconds(3))
                          .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testConsumeError() {

        try {
            Streams.streamOf("test").sync().map(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    throw new NullPointerException();
                }
            }).onError(new Consumer<RoutineException>() {

                public void accept(final RoutineException e) {

                    throw new IllegalArgumentException();
                }
            }).next();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).onError(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {

            }
        }).all()).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumeErrorNullPointerError() {

        try {
            Streams.streamOf().onError(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {

        final Consumer<Object> consumer = null;
        try {
            Streams.streamOf().sync().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().async().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(Streams.streamOf(null, "test")
                          .async()
                          .filter(Functions.isNotNull())
                          .after(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .parallel()
                          .filter(Functions.isNotNull())
                          .after(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .sync()
                          .filter(Functions.isNotNull())
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .sequential()
                          .filter(Functions.isNotNull())
                          .after(seconds(3))
                          .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {

            Streams.streamOf().async().filter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().filter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().filter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sequential().filter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFlatMap() {

        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .sync()
                          .flatMap(new Function<String, Channel<?, String>>() {

                              public Channel<?, String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .async()
                          .flatMap(new Function<String, Channel<?, String>>() {

                              public Channel<?, String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .parallel()
                          .flatMap(new Function<String, Channel<?, String>>() {

                              public Channel<?, String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .after(seconds(3))
                          .all()).containsOnly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .sequential()
                          .flatMap(new Function<String, Channel<?, String>>() {

                              public Channel<?, String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .after(seconds(3))
                          .all()).containsOnly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFlatMapNullPointerError() {

        try {

            Streams.streamOf().sync().flatMap(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().flatMap(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().flatMap(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sequential().flatMap(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFlatMapRetry() {

        final Routine<Object, String> routine =
                JRoutineCore.on(functionMapping(new Function<Object, String>() {

                    public String apply(final Object o) {

                        return o.toString();
                    }
                })).buildRoutine();
        final Function<Object, StreamChannel<Object, String>> retryFunction =
                new Function<Object, StreamChannel<Object, String>>() {

                    public StreamChannel<Object, String> apply(final Object o) {

                        final int[] count = {0};
                        return Streams.streamOf(o)
                                      .map(routine)
                                      .tryCatchMore(
                                              new BiConsumer<RoutineException, Channel<String,
                                                      ?>>() {

                                                  public void accept(final RoutineException e,
                                                          final Channel<String, ?> channel) {

                                                      if (++count[0] < 3) {

                                                          Streams.streamOf(o)
                                                                 .map(routine)
                                                                 .tryCatchMore(this)
                                                                 .bind(channel);

                                                      } else {

                                                          throw e;
                                                      }
                                                  }
                                              });

                    }
                };

        try {

            Streams.streamOf((Object) null).async().flatMap(retryFunction).after(seconds(3)).all();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testFlatTransform() {

        assertThat(Streams.streamOf("test1")
                          .flatLift(
                                  new Function<StreamChannel<String, String>,
                                          StreamChannel<String, String>>() {

                                      public StreamChannel<String, String> apply(
                                              final StreamChannel<String, String> stream) {

                                          return stream.append("test2");
                                      }
                                  })
                          .after(seconds(3))
                          .all()).containsExactly("test1", "test2");
        try {
            Streams.streamOf()
                   .flatLift(
                           new Function<StreamChannel<Object, Object>, StreamChannel<Object,
                                   Object>>() {

                               public StreamChannel<Object, Object> apply(
                                       final StreamChannel<Object, Object> objects) {

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
            Streams.streamOf("test")
                   .invocationConfiguration()
                   .withRunner(runner1)
                   .apply()
                   .map(new Function<String, Object>() {

                       public Object apply(final String s) {

                           return Streams.streamOf(s)
                                         .invocationConfiguration()
                                         .withRunner(runner1)
                                         .apply()
                                         .map(Functions.identity())
                                         .invocationConfiguration()
                                         .withRunner(runner2)
                                         .apply()
                                         .map(Functions.identity())
                                         .after(minutes(3))
                                         .next();
                       }
                   })
                   .after(minutes(3))
                   .next();

            fail();

        } catch (final ExecutionDeadlockException ignored) {

        }
    }

    @Test
    public void testInvocationMode() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .invocationMode(InvocationMode.ASYNC)
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .invocationMode(InvocationMode.PARALLEL)
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .invocationMode(InvocationMode.SYNC)
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .invocationMode(InvocationMode.SEQUENTIAL)
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {

        try {
            Streams.streamOf().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLimit() {

        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .limit(5)
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .limit(0)
                          .after(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .limit(15)
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .limit(0)
                          .after(seconds(3))
                          .all()).isEmpty();
    }

    @Test
    public void testMapAllConsumer() {

        assertThat(
                Streams.streamOf("test1", "test2", "test3").async().mapAllMore(new BiConsumer<List<?
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
                }).after(seconds(3)).all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .mapAllMore(new BiConsumer<List<? extends String>, Channel<String, ?>>() {

                              public void accept(final List<? extends String> strings,
                                      final Channel<String, ?> result) {

                                  final StringBuilder builder = new StringBuilder();

                                  for (final String string : strings) {

                                      builder.append(string);
                                  }

                                  result.pass(builder.toString());
                              }
                          })
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {

        try {
            Streams.streamOf().async().mapAllMore(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapAllFunction() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .mapAll(new Function<List<? extends String>, String>() {

                              public String apply(final List<? extends String> strings) {

                                  final StringBuilder builder = new StringBuilder();

                                  for (final String string : strings) {

                                      builder.append(string);
                                  }

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .mapAll(new Function<List<? extends String>, String>() {

                              public String apply(final List<? extends String> strings) {

                                  final StringBuilder builder = new StringBuilder();

                                  for (final String string : strings) {

                                      builder.append(string);
                                  }

                                  return builder.toString();
                              }
                          })
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapAllFunctionNullPointerError() {

        try {

            Streams.streamOf().async().mapAll(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(Streams.streamOf("test1", "test2")
                          .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                              public void accept(final String s, final Channel<String, ?> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                              public void accept(final String s, final Channel<String, ?> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sync()
                          .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                              public void accept(final String s, final Channel<String, ?> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sequential()
                          .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                              public void accept(final String s, final Channel<String, ?> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {

        try {

            Streams.streamOf().async().mapMore(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory = factoryOf(UpperCase.class);
        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(factory)
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .map(factory)
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(factory).all()).containsExactly(
                "TEST1", "TEST2");
        assertThat(
                Streams.streamOf("test1", "test2").sequential().map(factory).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        try {

            Streams.streamOf().async().map((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().map((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().map((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sequential().map((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(new UpperCase())
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .map(new UpperCase())
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sync()
                          .map(new UpperCase())
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sequential()
                          .map(new UpperCase())
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {

            Streams.streamOf().async().map((MappingInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().map((MappingInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().map((MappingInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sequential().map((MappingInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        assertThat(Streams.streamOf("test1", "test2").async().map(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).after(seconds(3)).all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .map(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).all()).containsExactly("TEST1", "TEST2");
        assertThat(
                Streams.streamOf("test1", "test2").sequential().map(new Function<String, String>() {

                    public String apply(final String s) {

                        return s.toUpperCase();
                    }
                }).all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {

        try {

            Streams.streamOf().async().map((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().map((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().map((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sequential().map((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutineCore.on(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .apply()
                                                            .buildRoutine();
        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(routine)
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .parallel()
                          .map(routine)
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(routine).all()).containsExactly(
                "TEST1", "TEST2");
        assertThat(
                Streams.streamOf("test1", "test2").sequential().map(routine).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    public void testMapRoutineBuilder() {

        final RoutineBuilder<String, String> builder = JRoutineCore.on(new UpperCase());
        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(builder)
                          .after(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .parallel()
                          .map(builder)
                          .after(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(builder).all()).containsExactly(
                "TEST1", "TEST2");
        assertThat(
                Streams.streamOf("test1", "test2").sequential().map(builder).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineBuilderNullPointerError() {

        try {
            Streams.streamOf().async().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().parallel().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sequential().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        try {
            Streams.streamOf().async().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().parallel().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sequential().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMaxSizeDeadlock() {

        try {

            assertThat(Streams.streamOf()
                              .thenGetMore(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(getSingleThreadRunner())
                              .withInputLimit(2)
                              .withInputBackoff(seconds(3))
                              .withOutputLimit(2)
                              .withOutputBackoff(seconds(3))
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
                              .reduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

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
                              .after(minutes(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));

            fail();

        } catch (final InputDeadlockException ignored) {

        }

        try {

            assertThat(Streams.streamOf()
                              .thenGetMore(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(getSingleThreadRunner())
                              .withOutputLimit(2)
                              .withOutputBackoff(seconds(3))
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
                              .reduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

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
                              .after(minutes(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));

        } catch (final OutputDeadlockException ignored) {

        }

        try {

            assertThat(Streams.streamOf()
                              .thenGetMore(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(getSingleThreadRunner())
                              .withInputLimit(2)
                              .withInputBackoff(seconds(3))
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
                              .reduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

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
        assertThat(Streams.streamOf("test").onComplete(new Runnable() {

            public void run() {
                isComplete.set(true);
            }
        }).after(seconds(3)).all()).isEmpty();
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(Streams.streamOf("test").map(new Function<String, String>() {

            public String apply(final String s) throws Exception {
                throw new NoSuchElementException();
            }
        }).onComplete(new Runnable() {

            public void run() {
                isComplete.set(true);
            }
        }).after(seconds(3)).getError()).isExactlyInstanceOf(InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    public void testOrElse() {

        assertThat(Streams.streamOf("test").orElse("est").after(seconds(3)).all()).containsExactly(
                "test");
        assertThat(Streams.streamOf("test")
                          .orElse("est1", "est2")
                          .after(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf("test")
                          .orElse(Arrays.asList("est1", "est2"))
                          .after(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf("test").orElseGetMore(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {

                result.pass("est");
            }
        }).after(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamOf("test").orElseGet(new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).after(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamOf().orElse("est").after(seconds(3)).all()).containsExactly("est");
        assertThat(
                Streams.streamOf().orElse("est1", "est2").after(seconds(3)).all()).containsExactly(
                "est1", "est2");
        assertThat(Streams.streamOf().orElse(Arrays.asList("est1", "est2")).after(seconds(3)).all())
                .containsExactly("est1", "est2");
        assertThat(Streams.<String>streamOf().orElseGetMore(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {

                result.pass("est");
            }
        }).after(seconds(3)).all()).containsExactly("est");
        assertThat(Streams.<String>streamOf().orElseGetMore(2, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {

                result.pass("est");
            }
        }).after(seconds(3)).all()).containsExactly("est", "est");
        assertThat(Streams.<String>streamOf().orElseGet(new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).after(seconds(3)).all()).containsExactly("est");
        assertThat(Streams.<String>streamOf().orElseGet(2, new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).after(seconds(3)).all()).containsExactly("est", "est");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {

        try {
            Streams.streamOf().orElseGetMore(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGetMore(1, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGet(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGet(1, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(
                Streams.streamOf(channel).selectable(33).after(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 33), new Selectable<String>("test2", 33),
                new Selectable<String>("test3", 33));
    }

    @Test
    public void testOutputToSelectableAbort() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            Streams.streamOf(channel).selectable(33).after(seconds(1)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testPeek() {

        final ArrayList<String> data = new ArrayList<String>();
        assertThat(Streams.streamOf("test1", "test2", "test3").async().peek(new Consumer<String>() {

            public void accept(final String s) {

                data.add(s);
            }
        }).after(seconds(3)).all()).containsExactly("test1", "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testPeekComplete() {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(Streams.streamOf("test1", "test2", "test3").async().peekComplete(new Runnable() {

            public void run() {
                isComplete.set(true);
            }
        }).after(seconds(3)).all()).containsExactly("test1", "test2", "test3");
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(Streams.streamOf("test").map(new Function<String, String>() {

            public String apply(final String s) throws Exception {
                throw new NoSuchElementException();
            }
        }).peekComplete(new Runnable() {

            public void run() {
                isComplete.set(true);
            }
        }).after(seconds(3)).getError()).isExactlyInstanceOf(InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testPeekNullPointerError() {

        try {
            Streams.streamOf().async().peek(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduce() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .reduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .reduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceNullPointerError() {

        try {
            Streams.streamOf().async().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceSeed() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .reduce(new Supplier<StringBuilder>() {

                              public StringBuilder get() {

                                  return new StringBuilder();
                              }
                          }, new BiFunction<StringBuilder, String, StringBuilder>() {

                              public StringBuilder apply(final StringBuilder b, final String s) {

                                  return b.append(s);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .reduce(new Supplier<StringBuilder>() {

                              public StringBuilder get() {

                                  return new StringBuilder();
                              }
                          }, new BiFunction<StringBuilder, String, StringBuilder>() {

                              public StringBuilder apply(final StringBuilder b, final String s) {

                                  return b.append(s);
                              }
                          })
                          .map(new Function<StringBuilder, String>() {

                              public String apply(final StringBuilder builder) {

                                  return builder.toString();
                              }
                          })
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceSeedNullPointerError() {

        try {
            Streams.streamOf().async().reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReplay() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Streams.streamOf(inputChannel).replay();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        inputChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    @Test
    public void testReplayAbort() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Streams.streamOf(inputChannel).replay();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        inputChannel.abort();

        try {
            output1.all();
            fail();

        } catch (final AbortException ignored) {

        }

        try {
            output2.all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testRetry() {

        final AtomicInteger count1 = new AtomicInteger();
        try {
            Streams.streamOf("test")
                   .map(new UpperCase())
                   .map(factoryOf(ThrowException.class, count1))
                   .retry(2)
                   .after(seconds(3))
                   .throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        final AtomicInteger count2 = new AtomicInteger();
        assertThat(Streams.streamOf("test") // BUG
                          .map(new UpperCase())
                          .map(factoryOf(ThrowException.class, count2, 1))
                          .retry(1)
                          .after(seconds(3))
                          .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            Streams.streamOf("test")
                   .map(new AbortInvocation())
                   .map(factoryOf(ThrowException.class, count3))
                   .retry(2)
                   .after(seconds(3))
                   .throwError();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void testSequential() {

        assertThat(Streams.streamOf()
                          .immediate()
                          .thenGetMore(range(1, 1000))
                          .streamInvocationConfiguration()
                          .withInputMaxSize(1)
                          .withOutputMaxSize(1)
                          .apply()
                          .map(new Function<Number, Double>() {

                              public Double apply(final Number number) {

                                  return Math.sqrt(number.doubleValue());
                              }
                          })
                          .map(Streams.averageDouble())
                          .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testSize() {

        final Channel<Object, Object> channel =
                JRoutineCore.on(IdentityInvocation.factoryOf()).async();
        assertThat(channel.inputCount()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.inputCount()).isEqualTo(1);
        final Channel<?, Object> result = Streams.streamOf(channel.close());
        assertThat(result.after(seconds(1)).hasCompleted()).isTrue();
        assertThat(result.outputCount()).isEqualTo(1);
        assertThat(result.skipNext(1).outputCount()).isEqualTo(0);
    }

    @Test
    public void testSkip() {

        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .skip(5)
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .skip(15)
                          .after(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range(1, 10))
                          .async()
                          .skip(0)
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testSplit() {

        final Function<StreamChannel<Integer, Integer>, StreamChannel<Integer, Long>> sqr =
                new Function<StreamChannel<Integer, Integer>, StreamChannel<Integer, Long>>() {

                    public StreamChannel<Integer, Long> apply(
                            final StreamChannel<Integer, Integer> stream) {

                        return stream.map(new Function<Integer, Long>() {

                            public Long apply(final Integer number) {

                                final long value = number.longValue();
                                return value * value;
                            }
                        });
                    }
                };
        assertThat(Streams.streamOf()
                          .thenGetMore(range(1, 3))
                          .parallel(2, sqr)
                          .after(seconds(3))
                          .all()).containsOnly(1L, 4L, 9L);
        assertThat(Streams.streamOf()
                          .thenGetMore(range(1, 3))
                          .parallelBy(Functions.<Integer>identity(), sqr)
                          .after(seconds(3))
                          .all()).containsOnly(1L, 4L, 9L);
        assertThat(Streams.streamOf()
                          .thenGetMore(range(1, 3))
                          .parallel(2, JRoutineCore.on(IdentityInvocation.<Integer>factoryOf()))
                          .after(seconds(3))
                          .all()).containsOnly(1, 2, 3);
        assertThat(Streams.streamOf()
                          .thenGetMore(range(1, 3))
                          .parallelBy(Functions.<Integer>identity(),
                                  JRoutineCore.on(IdentityInvocation.<Integer>factoryOf()))
                          .after(seconds(3))
                          .all()).containsOnly(1, 2, 3);
    }

    @Test
    public void testThen() {

        assertThat(Streams.streamOf("test1").sync().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").sync().thenGetMore(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").sync().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(
                Streams.streamOf("test1").sync().thenGetMore(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").async().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(
                Streams.streamOf("test1").async().thenGetMore(new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").async().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .async()
                          .thenGetMore(3, new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").parallel().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .thenGetMore(new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST2");
        assertThat(Streams.streamOf("test1").parallel().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .thenGetMore(3, new Consumer<Channel<String, ?>>() {

                              public void accept(final Channel<String, ?> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .after(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    @Test
    public void testThen2() {

        assertThat(Streams.streamOf("test1").sync().then((String) null).all()).containsOnly(
                (String) null);
        assertThat(Streams.streamOf("test1").sync().then((String[]) null).all()).isEmpty();
        assertThat(Streams.streamOf("test1").sync().then().all()).isEmpty();
        assertThat(Streams.streamOf("test1").sync().then((List<String>) null).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .sync()
                          .then(Collections.<String>emptyList())
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1").sync().then("TEST2").all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").sync().then("TEST2", "TEST2").all()).containsOnly(
                "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .sync()
                          .then(Collections.singletonList("TEST2"))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then((String) null)
                          .after(seconds(1))
                          .all()).containsOnly((String) null);
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then((String[]) null)
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1").async().then().after(seconds(1)).all()).isEmpty();
        assertThat(
                Streams.streamOf("test1").async().then((List<String>) null).after(seconds(1)).all())
                .isEmpty();
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then(Collections.<String>emptyList())
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then("TEST2")
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").async().then("TEST2", "TEST2").after(seconds(1)).all())
                .containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then(Collections.singletonList("TEST2"))
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").parallel().then((String) null).after(seconds(1)).all())
                .containsOnly((String) null);
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then((String[]) null)
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1").parallel().then().after(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then((List<String>) null)
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then(Collections.<String>emptyList())
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then("TEST2")
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(
                Streams.streamOf("test1").parallel().then("TEST2", "TEST2").after(seconds(1)).all())
                .containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then(Collections.singletonList("TEST2"))
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then((String) null)
                          .after(seconds(1))
                          .all()).containsOnly((String) null);
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then((String[]) null)
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1").sequential().then().after(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then((List<String>) null)
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then(Collections.<String>emptyList())
                          .after(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then("TEST2")
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then("TEST2", "TEST2")
                          .after(seconds(1))
                          .all()).containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .sequential()
                          .then(Collections.singletonList("TEST2"))
                          .after(seconds(1))
                          .all()).containsOnly("TEST2");
    }

    @Test
    public void testThenNegativeCount() {

        try {

            Streams.streamOf().sync().thenGet(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet(0, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGetMore(-1, Functions.sink());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {

        try {

            Streams.streamOf().sync().thenGetMore(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGetMore(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGet(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGet(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGetMore(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGetMore(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGetMore(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGetMore(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testTransform() {

        assertThat(Streams.streamOf("test")
                          .liftConfig(
                                  new BiFunction<StreamConfiguration, Function<Channel<?,
                                          String>, Channel<?, String>>, Function<Channel<?,
                                          String>, Channel<?, String>>>() {

                                      public Function<Channel<?, String>, Channel<?, String>> apply(
                                              final StreamConfiguration configuration,
                                              final Function<Channel<?, String>, Channel<?,
                                                      String>> function) {

                                          assertThat(
                                                  configuration.asChannelConfiguration()).isEqualTo(
                                                  ChannelConfiguration.defaultConfiguration());
                                          assertThat(
                                                  configuration.asInvocationConfiguration())
                                                  .isEqualTo(
                                                  InvocationConfiguration.defaultConfiguration());
                                          assertThat(configuration.getInvocationMode()).isEqualTo(
                                                  InvocationMode.ASYNC);
                                          return wrap(function).andThen(
                                                  new Function<Channel<?, String>, Channel<?,
                                                          String>>() {

                                                      public Channel<?, String> apply(
                                                              final Channel<?, String> channel) {

                                                          return JRoutineCore.on(new UpperCase())
                                                                             .async(channel);
                                                      }
                                                  });
                                      }
                                  })
                          .after(seconds(3))
                          .next()).isEqualTo("TEST");
        assertThat(Streams.streamOf("test")
                          .lift(new Function<Function<Channel<?, String>, Channel<?, String>>,
                                  Function<Channel<?, String>, Channel<?, String>>>() {

                              public Function<Channel<?, String>, Channel<?, String>> apply(
                                      final Function<Channel<?, String>, Channel<?, String>>
                                              function) {

                                  return wrap(function).andThen(
                                          new Function<Channel<?, String>, Channel<?, String>>() {

                                              public Channel<?, String> apply(
                                                      final Channel<?, String> channel) {

                                                  return JRoutineCore.on(new UpperCase())
                                                                     .async(channel);
                                              }
                                          });
                              }
                          })
                          .after(seconds(3))
                          .next()).isEqualTo("TEST");
        try {
            Streams.streamOf()
                   .liftConfig(
                           new BiFunction<StreamConfiguration, Function<Channel<?, Object>,
                                   Channel<?, Object>>, Function<Channel<?, Object>, Channel<?,
                                   Object>>>() {

                               public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                       final StreamConfiguration configuration,
                                       final Function<Channel<?, Object>, Channel<?, Object>>
                                               function) {

                                   throw new NullPointerException();
                               }
                           });
            fail();

        } catch (final StreamException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        try {
            Streams.streamOf()
                   .lift(new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                           Function<Channel<?, Object>, Channel<?, Object>>>() {

                       public Function<Channel<?, Object>, Channel<?, Object>> apply(
                               final Function<Channel<?, Object>, Channel<?, Object>> function) {

                           throw new NullPointerException();
                       }
                   });
            fail();

        } catch (final StreamException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        final StreamChannel<Object, Object> stream = //
                Streams.streamOf()
                       .lift(new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                               Function<Channel<?, Object>, Channel<?, Object>>>() {

                           public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                   final Function<Channel<?, Object>, Channel<?, Object>>
                                           function) {

                               return new Function<Channel<?, Object>, Channel<?, Object>>() {

                                   public Channel<?, Object> apply(
                                           final Channel<?, Object> objects) {

                                       throw new NullPointerException();
                                   }
                               };
                           }
                       });
        try {
            stream.bind();
            fail();

        } catch (final StreamException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testTryCatch() {

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatchMore(new BiConsumer<RoutineException, Channel<Object, ?>>() {

            public void accept(final RoutineException e, final Channel<Object, ?> channel) {

                channel.pass("exception");
            }
        }).next()).isEqualTo("exception");

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return o;
            }
        }).tryCatchMore(new BiConsumer<RoutineException, Channel<Object, ?>>() {

            public void accept(final RoutineException e, final Channel<Object, ?> channel) {

                channel.pass("exception");
            }
        }).next()).isEqualTo("test");

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {

                return "exception";
            }
        }).next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {

        try {

            Streams.streamOf().tryCatchMore(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().tryCatch(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testTryFinally() {

        final AtomicBoolean isRun = new AtomicBoolean(false);
        try {
            Streams.streamOf("test").sync().map(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    throw new NullPointerException();
                }
            }).tryFinally(new Runnable() {

                public void run() {

                    isRun.set(true);
                }
            }).next();

        } catch (final RoutineException ignored) {

        }

        assertThat(isRun.getAndSet(false)).isTrue();

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return o;
            }
        }).tryFinally(new Runnable() {

            public void run() {

                isRun.set(true);
            }
        }).next()).isEqualTo("test");
        assertThat(isRun.getAndSet(false)).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryFinallyNullPointerError() {

        try {
            Streams.streamOf().tryFinally(null);
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
        public void onInput(final Object input, @NotNull final Channel<Object, ?> result) throws
                Exception {

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
