/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.stream;

import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.ExecutionDeadlockException;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InputDeadlockException;
import com.github.dm.jrt.channel.OutputDeadlockException;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.channel.TimeoutException;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.Invocations;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.util.TimeDuration.minutes;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream output channel unit tests.
 * <p/>
 * Created by davide-maestroni on 10/22/2015.
 */
public class StreamOutputChannelTest {

    private final Runner mSingleThreadRunner = Runners.poolRunner(1);

    @Test
    public void testBuilder() {

        assertThat(Streams.streamOf("test").afterMax(seconds(1)).all()).containsExactly("test");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(Arrays.asList("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(JRoutine.io().of("test1", "test2", "test3"))
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

        StreamOutputChannel<String> channel = Streams.streamOf("test");
        assertThat(channel.abort()).isFalse();
        assertThat(channel.abort(null)).isFalse();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(channel.checkComplete()).isTrue();
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

        assertThat(channel.skip(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyExit().next(4)).containsExactly("test3");

        final Iterator<String> iterator = Streams.streamOf("test1", "test2", "test3").iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");

        try {

            iterator.remove();

            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        final IOChannel<String> ioChannel = JRoutine.io().buildChannel();
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
    public void testCollectConsumer() {

        assertThat(Streams.streamOf("test1", "test2", "test3").asyncCollect(new BiConsumer<List<?
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
                          .syncCollect(
                                  new BiConsumer<List<? extends String>, ResultChannel<String>>() {

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
    public void testCollectConsumerNullPointerError() {

        try {

            Streams.streamOf().asyncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCollectFunction() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .asyncCollect(new Function<List<? extends String>, String>() {

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
                          .syncCollect(new Function<List<? extends String>, String>() {

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
    public void testCollectFunctionNullPointerError() {

        try {

            Streams.streamOf().asyncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfiguration() {

        assertThat(Streams.streamOf("test1", "test2")
                          .maxParallelInvocations(1)
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .maxParallelInvocations(1)
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .maxParallelInvocations(1)
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toLowerCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf()
                          .asyncRange(1, 1000)
                          .backPressureOn(mSingleThreadRunner, 2, 10, TimeUnit.SECONDS)
                          .asyncMap(Functions.<Number>identity())
                          .asyncMap(new Function<Number, Double>() {

                              public Double apply(final Number number) {

                                  final double value = number.doubleValue();
                                  return Math.sqrt(value);
                              }
                          })
                          .syncMap(new Function<Double, SumData>() {

                              public SumData apply(final Double aDouble) {

                                  return new SumData(aDouble, 1);
                              }
                          })
                          .syncReduce(new BiFunction<SumData, SumData, SumData>() {

                              public SumData apply(final SumData data1, final SumData data2) {

                                  return new SumData(data1.sum + data2.sum,
                                                     data1.count + data2.count);
                              }
                          })
                          .syncMap(new Function<SumData, Double>() {

                              public Double apply(final SumData data) {

                                  return data.sum / data.count;
                              }
                          })
                          .runOnShared()
                          .afterMax(seconds(3))
                          .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testFilter() {

        assertThat(Streams.streamOf(null, "test")
                          .asyncFilter(Functions.notNull())
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .parallelFilter(Functions.notNull())
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf(null, "test")
                          .syncFilter(Functions.notNull())
                          .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {

            Streams.streamOf().asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testForEach() {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(Streams.streamOf("test1", "test2", "test3").syncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.streamOf("test1", "test2", "test3").asyncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).afterMax(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testForEachNullPointerError() {

        try {

            Streams.streamOf().syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testGenerate() {

        assertThat(Streams.streamOf("test1").syncGenerate(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").syncGenerate(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").syncGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").asyncGenerate(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").asyncGenerate(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.streamOf("test1").asyncGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1")
                          .parallelGenerate(3, new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.streamOf("test1").parallelGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    @Test
    public void testGenerateNegativeCount() {

        try {

            Streams.streamOf().syncGenerate(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().asyncGenerate(0, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallelGenerate(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallelGenerate(-1, Functions.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallelGenerate(-1, Functions.sink());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testGenerateNullPointerError() {

        try {

            Streams.streamOf().syncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncGenerate(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncGenerate(3, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelGenerate(3, (Consumer<ResultChannel<?>>) null);

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
                   .withInvocations()
                   .withRunner(runner1)
                   .set()
                   .asyncMap(new Function<String, Object>() {

                       public Object apply(final String s) {

                           return Streams.streamOf(s)
                                         .withInvocations()
                                         .withRunner(runner1)
                                         .set()
                                         .asyncMap(Functions.identity())
                                         .withInvocations()
                                         .withRunner(runner2)
                                         .set()
                                         .asyncMap(Functions.identity())
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
    public void testLift() {

        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .syncLift(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .syncFilter(Functions.<String>notNull());
                              }
                          })
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .asyncLift(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .syncFilter(Functions.<String>notNull());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamOf("test1", null, "test2", null)
                          .parallelLift(new Function<String, OutputChannel<String>>() {

                              public OutputChannel<String> apply(final String s) {

                                  return Streams.streamOf(s)
                                                .syncFilter(Functions.<String>notNull());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLiftNullPointerError() {

        try {

            Streams.streamOf().syncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(Streams.streamOf("test1", "test2")
                          .asyncMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .syncMap(new BiConsumer<String, ResultChannel<String>>() {

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

            Streams.streamOf().asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory = Invocations.factoryOf(UpperCase.class);
        assertThat(Streams.streamOf("test1", "test2")
                          .asyncMap(factory)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .parallelMap(factory)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").syncMap(factory).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        try {

            Streams.streamOf().asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(Streams.streamOf("test1", "test2")
                          .asyncMap(new UpperCase())
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .parallelMap(new UpperCase())
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(
                Streams.streamOf("test1", "test2").syncMap(new UpperCase()).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {

            Streams.streamOf().asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        assertThat(Streams.streamOf("test1", "test2").asyncMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).afterMax(seconds(3)).all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .ordered()
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").syncMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {

        try {

            Streams.streamOf().asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase())
                                                        .withInvocations()
                                                        .withOutputOrder(OrderType.BY_CALL)
                                                        .set()
                                                        .buildRoutine();
        assertThat(Streams.streamOf("test1", "test2")
                          .asyncMap(routine)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2")
                          .parallelMap(routine)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamOf("test1", "test2").syncMap(routine).all()).containsExactly(
                "TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        try {

            Streams.streamOf().asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMaxSizeDeadlock() {

        try {

            assertThat(Streams.streamOf()
                              .asyncRange(1, 1000)
                              .withStreamInvocations()
                              .withRunner(mSingleThreadRunner)
                              .withInputLimit(2)
                              .withInputMaxDelay(seconds(10))
                              .withOutputLimit(2)
                              .withOutputMaxDelay(seconds(10))
                              .set()
                              .asyncMap(Functions.<Number>identity())
                              .asyncMap(new Function<Number, Double>() {

                                  public Double apply(final Number number) {

                                      final double value = number.doubleValue();
                                      return Math.sqrt(value);
                                  }
                              })
                              .syncMap(new Function<Double, SumData>() {

                                  public SumData apply(final Double aDouble) {

                                      return new SumData(aDouble, 1);
                                  }
                              })
                              .syncReduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

                                      return new SumData(data1.sum + data2.sum,
                                                         data1.count + data2.count);
                                  }
                              })
                              .syncMap(new Function<SumData, Double>() {

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
                              .asyncRange(1, 1000)
                              .withStreamInvocations()
                              .withRunner(mSingleThreadRunner)
                              .withOutputLimit(2)
                              .withOutputMaxDelay(seconds(10))
                              .set()
                              .asyncMap(Functions.<Number>identity())
                              .asyncMap(new Function<Number, Double>() {

                                  public Double apply(final Number number) {

                                      final double value = number.doubleValue();
                                      return Math.sqrt(value);
                                  }
                              })
                              .syncMap(new Function<Double, SumData>() {

                                  public SumData apply(final Double aDouble) {

                                      return new SumData(aDouble, 1);
                                  }
                              })
                              .syncReduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

                                      return new SumData(data1.sum + data2.sum,
                                                         data1.count + data2.count);
                                  }
                              })
                              .syncMap(new Function<SumData, Double>() {

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
                              .asyncRange(1, 1000)
                              .withStreamInvocations()
                              .withRunner(mSingleThreadRunner)
                              .withInputLimit(2)
                              .withInputMaxDelay(seconds(10))
                              .set()
                              .asyncMap(Functions.<Number>identity())
                              .asyncMap(new Function<Number, Double>() {

                                  public Double apply(final Number number) {

                                      final double value = number.doubleValue();
                                      return Math.sqrt(value);
                                  }
                              })
                              .syncMap(new Function<Double, SumData>() {

                                  public SumData apply(final Double aDouble) {

                                      return new SumData(aDouble, 1);
                                  }
                              })
                              .syncReduce(new BiFunction<SumData, SumData, SumData>() {

                                  public SumData apply(final SumData data1, final SumData data2) {

                                      return new SumData(data1.sum + data2.sum,
                                                         data1.count + data2.count);
                                  }
                              })
                              .syncMap(new Function<SumData, Double>() {

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
    public void testRange() {

        assertThat(Streams.streamOf().asyncRange('a', 'e', new Function<Character, Character>() {

            public Character apply(final Character character) {

                return (char) (character + 1);
            }
        }).afterMax(seconds(3)).all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .asyncRange(0, -10, -2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, -2, -4, -6, -8, -10);
        assertThat(Streams.streamOf()
                          .asyncRange(0, 2, 0.7)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0d, 0.7d, 1.4d);
        assertThat(Streams.streamOf()
                          .asyncRange(0, 2, 0.7f)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0f, 0.7f, 1.4f);
        assertThat(Streams.streamOf()
                          .asyncRange(0L, -9, -2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0L, -2L, -4L, -6L, -8L);
        assertThat(Streams.streamOf()
                          .asyncRange(0, (short) 9, 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, 2, 4, 6, 8);
        assertThat(Streams.streamOf()
                          .asyncRange((byte) 0, (short) 9, (byte) 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly((short) 0, (short) 2, (short) 4, (short) 6,
                                                  (short) 8);
        assertThat(Streams.streamOf()
                          .asyncRange((byte) 0, (byte) 10, (byte) 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8,
                                                  (byte) 10);
        assertThat(Streams.streamOf().asyncRange(0, -5).afterMax(seconds(3)).all()).containsExactly(
                0, -1, -2, -3, -4, -5);
        assertThat(
                Streams.streamOf().asyncRange(0, 2.1).afterMax(seconds(3)).all()).containsExactly(
                0d, 1d, 2d);
        assertThat(
                Streams.streamOf().asyncRange(0, 1.9f).afterMax(seconds(3)).all()).containsExactly(
                0f, 1f);
        assertThat(
                Streams.streamOf().asyncRange(0L, -4).afterMax(seconds(3)).all()).containsExactly(
                0L, -1L, -2L, -3L, -4L);
        assertThat(Streams.streamOf()
                          .asyncRange(0, (short) 4)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, 1, 2, 3, 4);
        assertThat(Streams.streamOf()
                          .asyncRange((byte) 0, (short) 4)
                          .afterMax(seconds(3))
                          .all()).containsExactly((short) 0, (short) 1, (short) 2, (short) 3,
                                                  (short) 4);
        assertThat(Streams.streamOf()
                          .asyncRange((byte) 0, (byte) 5)
                          .afterMax(seconds(3))
                          .all()).containsExactly((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4,
                                                  (byte) 5);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, -10, -2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, -2, -4, -6, -8, -10);
        assertThat(Streams.streamOf().ordered().parallelRange(0, 2, 0.7).afterMax(seconds(3)).all())
                .containsExactly(0d, 0.7d, 1.4d);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, 2, 0.7f)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0f, 0.7f, 1.4f);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0L, -9, -2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0L, -2L, -4L, -6L, -8L);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, (short) 9, 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, 2, 4, 6, 8);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange((byte) 0, (short) 9, (byte) 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly((short) 0, (short) 2, (short) 4, (short) 6,
                                                  (short) 8);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange((byte) 0, (byte) 10, (byte) 2)
                          .afterMax(seconds(3))
                          .all()).containsExactly((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8,
                                                  (byte) 10);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, -5)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0, -1, -2, -3, -4, -5);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, 2.1)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0d, 1d, 2d);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0, 1.9f)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0f, 1f);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange(0L, -4)
                          .afterMax(seconds(3))
                          .all()).containsExactly(0L, -1L, -2L, -3L, -4L);
        assertThat(
                Streams.streamOf().ordered().parallelRange(0, (short) 4).afterMax(seconds(3)).all())
                .containsExactly(0, 1, 2, 3, 4);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange((byte) 0, (short) 4)
                          .afterMax(seconds(3))
                          .all()).containsExactly((short) 0, (short) 1, (short) 2, (short) 3,
                                                  (short) 4);
        assertThat(Streams.streamOf()
                          .ordered()
                          .parallelRange((byte) 0, (byte) 5)
                          .afterMax(seconds(3))
                          .all()).containsExactly((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4,
                                                  (byte) 5);
        assertThat(Streams.streamOf().syncRange('a', 'e', new Function<Character, Character>() {

            public Character apply(final Character character) {

                return (char) (character + 1);
            }
        }).all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf().syncRange(0, -10, -2).all()).containsExactly(0, -2, -4, -6,
                                                                                   -8, -10);
        assertThat(Streams.streamOf().syncRange(0, 2, 0.7).all()).containsExactly(0d, 0.7d, 1.4d);
        assertThat(Streams.streamOf().syncRange(0, 2, 0.7f).all()).containsExactly(0f, 0.7f, 1.4f);
        assertThat(Streams.streamOf().syncRange(0L, -9, -2).all()).containsExactly(0L, -2L, -4L,
                                                                                   -6L, -8L);
        assertThat(Streams.streamOf().syncRange(0, (short) 9, 2).all()).containsExactly(0, 2, 4, 6,
                                                                                        8);
        assertThat(
                Streams.streamOf().syncRange((byte) 0, (short) 9, (byte) 2).all()).containsExactly(
                (short) 0, (short) 2, (short) 4, (short) 6, (short) 8);
        assertThat(
                Streams.streamOf().syncRange((byte) 0, (byte) 10, (byte) 2).all()).containsExactly(
                (byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10);
        assertThat(Streams.streamOf().syncRange(0, -5).all()).containsExactly(0, -1, -2, -3, -4,
                                                                              -5);
        assertThat(Streams.streamOf().syncRange(0, 2.1).all()).containsExactly(0d, 1d, 2d);
        assertThat(Streams.streamOf().syncRange(0, 1.9f).all()).containsExactly(0f, 1f);
        assertThat(Streams.streamOf().syncRange(0L, -4).all()).containsExactly(0L, -1L, -2L, -3L,
                                                                               -4L);
        assertThat(Streams.streamOf().syncRange(0, (short) 4).all()).containsExactly(0, 1, 2, 3, 4);
        assertThat(Streams.streamOf().syncRange((byte) 0, (short) 4).all()).containsExactly(
                (short) 0, (short) 1, (short) 2, (short) 3, (short) 4);
        assertThat(Streams.streamOf().syncRange((byte) 0, (byte) 5).all()).containsExactly((byte) 0,
                                                                                           (byte) 1,
                                                                                           (byte) 2,
                                                                                           (byte) 3,
                                                                                           (byte) 4,
                                                                                           (byte) 5);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRangeError() {

        try {

            Streams.streamOf().asyncRange(null, 'f', new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncRange('a', null, new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncRange('a', 'f', null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncRange(null, 1, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncRange(1, null, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().asyncRange(1, 1, (Number) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(null, 'f', new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange('a', null, new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange('a', 'f', null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(null, 1, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(1, null, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(1, 1, (Number) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange(null, 'f', new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange('a', null, new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange('a', 'f', null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange(null, 1, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange(1, null, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncRange(1, 1, (Number) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final Number number = new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        };

        try {

            Streams.streamOf().asyncRange(number, number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().asyncRange(number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(number, number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().parallelRange(number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().syncRange(number, number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.streamOf().syncRange(number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testReduce() {

        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .asyncReduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .syncReduce(new BiFunction<String, String, String>() {

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

            Streams.streamOf().asyncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().syncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testRetry() {

        final Routine<Object, String> routine =
                JRoutine.on(functionFilter(new Function<Object, String>() {

                    public String apply(final Object o) {

                        return o.toString();
                    }
                })).buildRoutine();
        final Function<Object, StreamOutputChannel<String>> retryFunction =
                new Function<Object, StreamOutputChannel<String>>() {

                    public StreamOutputChannel<String> apply(final Object o) {

                        final int[] count = {0};
                        return Streams.streamOf(o)
                                      .asyncMap(routine)
                                      .tryCatch(
                                              new BiConsumer<RoutineException,
                                                      InputChannel<String>>() {

                                                  public void accept(final RoutineException e,
                                                          final InputChannel<String> channel) {

                                                      if (++count[0] < 3) {

                                                          Streams.streamOf(o)
                                                                 .asyncMap(routine)
                                                                 .tryCatch(this)
                                                                 .passTo(channel);

                                                      } else {

                                                          throw e;
                                                      }
                                                  }
                                              });

                    }
                };

        try {

            Streams.streamOf((Object) null).asyncLift(retryFunction).afterMax(seconds(3)).all();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testTryCatch() {

        assertThat(Streams.streamOf("test").syncMap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

            public void accept(final RoutineException e, final InputChannel<Object> channel) {

                channel.pass("exception");
            }
        }).next()).isEqualTo("exception");

        assertThat(Streams.streamOf("test").syncMap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return o;
            }
        }).tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

            public void accept(final RoutineException e, final InputChannel<Object> channel) {

                channel.pass("exception");
            }
        }).next()).isEqualTo("test");

        try {

            Streams.streamOf("test").syncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    throw new NullPointerException();
                }
            }).tryCatch(new Consumer<RoutineException>() {

                public void accept(final RoutineException e) {

                    throw new IllegalArgumentException();
                }
            }).next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(Streams.streamOf("test").syncMap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {

            }
        }).all()).isEmpty();

        assertThat(Streams.streamOf("test").syncMap(new Function<Object, Object>() {

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

            Streams.streamOf().tryCatch((Consumer<RoutineException>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamOf().tryCatch((Function<RoutineException, ?>) null);

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

    private static class UpperCase extends FilterInvocation<String, String> {

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input.toUpperCase());
        }
    }
}
