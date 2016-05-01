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
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.OperationInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionOperation;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream output channel unit tests.
 * <p>
 * Created by davide-maestroni on 10/22/2015.
 */
public class StreamChannelTest {

    private final Runner mSingleThreadRunner = Runners.poolRunner(1);

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final StreamChannel<Object> streamChannel = Streams.streamOf(ioChannel);
        ioChannel.abort(new IllegalArgumentException());
        try {
            streamChannel.afterMax(seconds(3)).throwError();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    public void testApply() {

        assertThat(Streams.streamOf("test1")
                          .apply(new Function<StreamChannel<String>, StreamChannel<String>>() {

                              public StreamChannel<String> apply(
                                      final StreamChannel<String> stream) {

                                  return stream.concat("test2");
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
    }

    @Test
    public void testBuilder() {

        assertThat(Streams.streamOf().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test").afterMax(seconds(1)).all()).containsExactly("test");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(Arrays.asList("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            Streams.streamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testChannel() {

        StreamChannel<String> channel = Streams.streamOf("test");
        assertThat(channel.abort()).isFalse();
        assertThat(channel.abort(null)).isFalse();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        assertThat(channel.isBound()).isFalse();
        final ArrayList<String> results = new ArrayList<String>();
        assertThat(channel.afterMax(1, TimeUnit.SECONDS).hasNext()).isTrue();
        channel.immediately().allInto(results);
        assertThat(results).containsExactly("test");
        channel = Streams.streamOf("test1", "test2", "test3");

        try {

            channel.remove();

            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyExit().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyExit().nextOrElse("test4")).isEqualTo("test4");

        final Iterator<String> iterator = Streams.streamOf("test1", "test2", "test3").iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");

        try {

            iterator.remove();

            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        channel = Streams.streamOf(ioChannel.after(1, TimeUnit.DAYS).pass("test"));

        try {

            channel.eventuallyThrow().next();

            fail();

        } catch (final TimeoutException ignored) {

        }

        try {

            channel.eventuallyExit().next();

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
                          .afterMax(seconds(3))
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
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    public void testCollectCollection() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .async()
                          .collect(new Supplier<List<String>>() {

                              public List<String> get() {

                                  return new ArrayList<String>();
                              }
                          })
                          .afterMax(seconds(3))
                          .next()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .collect(new Supplier<List<String>>() {

                              public List<String> get() {

                                  return new ArrayList<String>();
                              }
                          })
                          .afterMax(seconds(3))
                          .next()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {

        try {
            Streams.streamOf().async().collect((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().collect((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {

        try {
            Streams.streamOf().async().collect((BiConsumer<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().collect((BiConsumer<Object, Object>) null);
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
                          .afterMax(seconds(3))
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
                          .afterMax(seconds(3))
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
    public void testConcat() {

        assertThat(Streams.streamOf("test1")
                          .concat("test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1")
                          .concat("test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1")
                          .concat(Arrays.asList("test2", "test3"))
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf("test1")
                          .concat(JRoutineCore.io().of("test2", "test3"))
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testConfiguration() {

        assertThat(Streams.streamOf("test1", "test2")
                          .parallel(1)
                          .map(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
                          .parallel(1)
                          .map(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
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
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf()
                          .async()
                          .thenGet(range(1, 1000))
                          .backPressureOn(mSingleThreadRunner, 2, 10, TimeUnit.SECONDS)
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
                          .runOnShared()
                          .afterMax(seconds(3))
                          .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testConstructor() {

        final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
        final TestStreamChannel streamChannel =
                new TestStreamChannel(channel, InvocationConfiguration.defaultConfiguration(),
                        InvocationMode.ASYNC, null);
        assertThat(streamChannel.getBinder()).isNotNull();
        assertThat(streamChannel.getConfiguration()).isNotNull();
        assertThat(streamChannel.getStreamConfiguration()).isNotNull();
        assertThat(streamChannel.getInvocationMode()).isNotNull();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorError() {

        try {
            new TestStreamChannel(null, InvocationConfiguration.defaultConfiguration(),
                    InvocationMode.ASYNC, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
        try {
            new TestStreamChannel(channel, null, InvocationMode.ASYNC, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            new TestStreamChannel(channel, InvocationConfiguration.defaultConfiguration(), null,
                    null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            new TestStreamChannel(channel, InvocationConfiguration.defaultConfiguration(),
                    InvocationMode.ASYNC, null).apply((InvocationConfiguration) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
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
                          .afterMax(seconds(3))
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
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .parallel()
                          .filter(Functions.isNotNull())
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .sync()
                          .filter(Functions.isNotNull())
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .serial()
                          .filter(Functions.isNotNull())
                          .afterMax(seconds(3))
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

            Streams.streamOf().serial().filter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFlatMap() {

        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .sync()
                          .flatMap(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .async()
                          .flatMap(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .parallel()
                          .flatMap(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .serial()
                          .flatMap(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .sync()
                                                .filter(Functions.<String>isNotNull());
                              }
                          })
                          .afterMax(seconds(3))
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

            Streams.streamOf().serial().flatMap(null);

            fail();

        } catch (final NullPointerException ignored) {

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
                   .applyConfiguration()
                   .map(new Function<String, Object>() {

                       public Object apply(final String s) {

                           return Streams.streamOf(s)
                                         .invocationConfiguration()
                                         .withRunner(runner1)
                                         .applyConfiguration()
                                         .map(Functions.identity())
                                         .invocationConfiguration()
                                         .withRunner(runner2)
                                         .applyConfiguration()
                                         .map(Functions.identity())
                                         .afterMax(minutes(3))
                                         .next();
                       }
                   })
                   .afterMax(minutes(3))
                   .next();

            fail();

        } catch (final ExecutionDeadlockException ignored) {

        }
    }

    @Test
    public void testLazyBuilder() {

        assertThat(Streams.lazyStreamOf().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.lazyStreamOf("test").afterMax(seconds(1)).all()).containsExactly("test");
        assertThat(Streams.lazyStreamOf("test1", "test2", "test3")
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLazyBuilderNullPointerError() {

        try {

            Streams.lazyStreamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLimit() {

        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .limit(5)
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .limit(0)
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .limit(15)
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .limit(0)
                          .afterMax(seconds(3))
                          .all()).isEmpty();
    }

    @Test
    public void testMapAllConsumer() {

        assertThat(Streams.streamOf("test1", "test2", "test3").async().mapAll(new BiConsumer<List<?
                extends String>, ResultChannel<String>>() {

            public void accept(final List<?
                    extends
                    String> strings, final ResultChannel<String> result) {

                final StringBuilder builder = new StringBuilder();

                for (final String string : strings) {

                    builder.append(string);
                }

                result.pass(builder.toString());
            }
        }).afterMax(seconds(3)).all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .sync()
                          .mapAll(new BiConsumer<List<? extends String>, ResultChannel<String>>() {

                              public void accept(final List<? extends String> strings,
                                      final ResultChannel<String> result) {

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
            Streams.streamOf().async().mapAll((BiConsumer<List<?>, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().sync().mapAll((BiConsumer<List<?>, ResultChannel<Object>>) null);
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
                          .afterMax(seconds(3))
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

            Streams.streamOf().async().mapAll((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().mapAll((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(Streams.streamOf("test1", "test2")
                          .map(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .map(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sync()
                          .map(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .serial()
                          .map(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {

        try {

            Streams.streamOf().async().map((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().map((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().map((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().serial().map((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory =
                InvocationFactory.factoryOf(UpperCase.class);
        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(factory)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .map(factory)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(factory).all()).containsExactly(
                "TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").serial().map(factory).all()).containsExactly(
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

            Streams.streamOf().serial().map((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(new UpperCase())
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .map(new UpperCase())
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .sync()
                          .map(new UpperCase())
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .serial()
                          .map(new UpperCase())
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {

            Streams.streamOf().async().map((OperationInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().map((OperationInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().map((OperationInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().serial().map((OperationInvocation<Object, Object>) null);

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
        }).afterMax(seconds(3)).all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .map(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").serial().map(new Function<String, String>() {

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

            Streams.streamOf().serial().map((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutineCore.on(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applyConfiguration()
                                                            .buildRoutine();
        assertThat(Streams.streamOf("test1", "test2")
                          .async()
                          .map(routine)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .parallel()
                          .map(routine)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").sync().map(routine).all()).containsExactly(
                "TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").serial().map(routine).all()).containsExactly(
                "TEST1", "TEST2");
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

            Streams.streamOf().serial().map((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMaxSizeDeadlock() {

        try {

            assertThat(Streams.streamOf()
                              .thenGet(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(mSingleThreadRunner)
                              .withInputLimit(2)
                              .withInputMaxDelay(seconds(3))
                              .withOutputLimit(2)
                              .withOutputMaxDelay(seconds(3))
                              .applyConfiguration()
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
                              .runOnShared()
                              .afterMax(minutes(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));

            fail();

        } catch (final InputDeadlockException ignored) {

        }

        try {

            assertThat(Streams.streamOf()
                              .thenGet(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(mSingleThreadRunner)
                              .withOutputLimit(2)
                              .withOutputMaxDelay(seconds(3))
                              .applyConfiguration()
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
                              .runOnShared()
                              .afterMax(minutes(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));

        } catch (final OutputDeadlockException ignored) {

        }

        try {

            assertThat(Streams.streamOf()
                              .thenGet(range(1, 1000))
                              .streamInvocationConfiguration()
                              .withRunner(mSingleThreadRunner)
                              .withInputLimit(2)
                              .withInputMaxDelay(seconds(3))
                              .applyConfiguration()
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
                              .runOnShared()
                              .afterMax(minutes(3))
                              .next()).isCloseTo(21, Offset.offset(0.1));

            fail();

        } catch (final InputDeadlockException ignored) {

        }
    }

    @Test
    public void testOrElse() {

        assertThat(
                Streams.streamOf("test").orElse("est").afterMax(seconds(3)).all()).containsExactly(
                "test");
        assertThat(Streams.streamOf("test")
                          .orElse("est1", "est2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf("test")
                          .orElse(Arrays.asList("est1", "est2"))
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf("test").orElseGet(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("est");
            }
        }).afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamOf("test").orElseGet(new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamOf().orElse("est").afterMax(seconds(3)).all()).containsExactly(
                "est");
        assertThat(Streams.streamOf()
                          .orElse("est1", "est2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("est1", "est2");
        assertThat(
                Streams.streamOf().orElse(Arrays.asList("est1", "est2")).afterMax(seconds(3)).all())
                .containsExactly("est1", "est2");
        assertThat(Streams.<String>streamOf().orElseGet(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("est");
            }
        }).afterMax(seconds(3)).all()).containsExactly("est");
        assertThat(Streams.<String>streamOf().orElseGet(2, new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("est");
            }
        }).afterMax(seconds(3)).all()).containsExactly("est", "est");
        assertThat(Streams.<String>streamOf().orElseGet(new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).afterMax(seconds(3)).all()).containsExactly("est");
        assertThat(Streams.<String>streamOf().orElseGet(2, new Supplier<String>() {

            public String get() {

                return "est";
            }
        }).afterMax(seconds(3)).all()).containsExactly("est", "est");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {

        try {
            Streams.streamOf().orElseGet((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGet(1, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGet((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.streamOf().orElseGet(1, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(Streams.streamOf(channel)
                          .toSelectable(33)
                          .afterMax(seconds(1))
                          .all()).containsExactly(new Selectable<String>("test1", 33),
                new Selectable<String>("test2", 33), new Selectable<String>("test3", 33));
    }

    @Test
    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            Streams.streamOf(channel).toSelectable(33).afterMax(seconds(1)).all();
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
        }).afterMax(seconds(3)).all()).containsExactly("test1", "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
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
                          .afterMax(seconds(3))
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
                          .afterMax(seconds(3))
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
    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Streams.streamOf(ioChannel).repeat();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        ioChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    @Test
    public void testRepeatAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Streams.streamOf(ioChannel).repeat();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        ioChannel.abort();

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

        final Routine<Object, String> routine =
                JRoutineCore.on(functionOperation(new Function<Object, String>() {

                    public String apply(final Object o) {

                        return o.toString();
                    }
                })).buildRoutine();
        final Function<Object, StreamChannel<String>> retryFunction =
                new Function<Object, StreamChannel<String>>() {

                    public StreamChannel<String> apply(final Object o) {

                        final int[] count = {0};
                        return Streams.streamOf(o)
                                      .map(routine)
                                      .tryCatch(
                                              new BiConsumer<RoutineException,
                                                      InputChannel<String>>() {

                                                  public void accept(final RoutineException e,
                                                          final InputChannel<String> channel) {

                                                      if (++count[0] < 3) {

                                                          Streams.streamOf(o)
                                                                 .map(routine)
                                                                 .tryCatch(this)
                                                                 .bind(channel);

                                                      } else {

                                                          throw e;
                                                      }
                                                  }
                                              });

                    }
                };

        try {

            Streams.streamOf((Object) null)
                   .async()
                   .flatMap(retryFunction)
                   .afterMax(seconds(3))
                   .all();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testSkip() {

        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .skip(5)
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .skip(15)
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGet(range(1, 10))
                          .async()
                          .skip(0)
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testThen() {

        assertThat(Streams.streamOf("test1").sync().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").sync().thenGet(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").sync().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(
                Streams.streamOf("test1").sync().thenGet(3, new Consumer<ResultChannel<String>>() {

                    public void accept(final ResultChannel<String> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").async().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").async().thenGet(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").async().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(
                Streams.streamOf("test1").async().thenGet(3, new Consumer<ResultChannel<String>>() {

                    public void accept(final ResultChannel<String> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).afterMax(seconds(3)).all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").parallel().thenGet(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2");
        assertThat(
                Streams.streamOf("test1").parallel().thenGet(new Consumer<ResultChannel<String>>() {

                    public void accept(final ResultChannel<String> resultChannel) {

                        resultChannel.pass("TEST2");
                    }
                }).afterMax(seconds(3)).all()).containsExactly("TEST2");
        assertThat(Streams.streamOf("test1").parallel().thenGet(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .thenGet(3, new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .afterMax(seconds(3))
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
        assertThat(Streams.streamOf("test1").async().then((String) null).afterMax(seconds(1)).all())
                .containsOnly((String) null);
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then((String[]) null)
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1").async().then().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then((List<String>) null)
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then(Collections.<String>emptyList())
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then("TEST2")
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(
                Streams.streamOf("test1").async().then("TEST2", "TEST2").afterMax(seconds(1)).all())
                .containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .async()
                          .then(Collections.singletonList("TEST2"))
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(
                Streams.streamOf("test1").parallel().then((String) null).afterMax(seconds(1)).all())
                .containsOnly((String) null);
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then((String[]) null)
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(
                Streams.streamOf("test1").parallel().then().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then((List<String>) null)
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then(Collections.<String>emptyList())
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then("TEST2")
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then("TEST2", "TEST2")
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallel()
                          .then(Collections.singletonList("TEST2"))
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then((String) null)
                          .afterMax(seconds(1))
                          .all()).containsOnly((String) null);
        assertThat(
                Streams.streamOf("test1").serial().then((String[]) null).afterMax(seconds(1)).all())
                .isEmpty();
        assertThat(Streams.streamOf("test1").serial().then().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then((List<String>) null)
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then(Collections.<String>emptyList())
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then("TEST2")
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then("TEST2", "TEST2")
                          .afterMax(seconds(1))
                          .all()).containsOnly("TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .serial()
                          .then(Collections.singletonList("TEST2"))
                          .afterMax(seconds(1))
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

            Streams.streamOf().parallel().thenGet(-1, Functions.sink());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {

        try {

            Streams.streamOf().sync().thenGet((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGet(3, (Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGet((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().sync().thenGet(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet(3, (Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().async().thenGet(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(3, (Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallel().thenGet(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testTryCatch() {

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

            public void accept(final RoutineException e, final InputChannel<Object> channel) {

                channel.pass("exception");
            }
        }).next()).isEqualTo("exception");

        assertThat(Streams.streamOf("test").sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return o;
            }
        }).tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

            public void accept(final RoutineException e, final InputChannel<Object> channel) {

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

            Streams.streamOf().tryCatch((BiConsumer<RoutineException, InputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().tryCatch((Function<RoutineException, ?>) null);

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

    private static class SumData {

        private final int count;

        private final double sum;

        private SumData(final double sum, final int count) {

            this.sum = sum;
            this.count = count;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class TestStreamChannel extends AbstractStreamChannel<Object> {

        /**
         * Constructor.
         *
         * @param channel        the wrapped output channel.
         * @param configuration  the initial invocation configuration.
         * @param invocationMode the delegation type.
         * @param binder         the binding runnable.
         */
        protected TestStreamChannel(@NotNull final OutputChannel<Object> channel,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

            super(channel, configuration, invocationMode, binder);
        }

        @NotNull
        public StreamChannel<Object> concat(@NotNull final OutputChannel<?> channel) {

            return null;
        }

        @NotNull
        @Override
        protected <AFTER> StreamChannel<AFTER> newChannel(
                @NotNull final OutputChannel<AFTER> channel,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationMode invocationMode, @Nullable final Binder binder) {

            return null;
        }

        @NotNull
        @Override
        protected <AFTER> Routine<? super Object, ? extends AFTER> newRoutine(
                @NotNull final InvocationConfiguration configuration,
                @NotNull final InvocationFactory<? super Object, ? extends AFTER> factory) {

            return null;
        }
    }

    private static class UpperCase extends OperationInvocation<String, String> {

        /**
         * Constructor.
         */
        protected UpperCase() {

            super(null);
        }

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input.toUpperCase());
        }
    }
}
