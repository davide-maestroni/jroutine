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

package com.github.dm.jrt.stream.processor;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
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
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.processor.Processors.outputAccept;
import static com.github.dm.jrt.stream.processor.Processors.outputGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream transformation unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class ProcessorsTest {

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
        assertThat(JRoutineStream.withStream()
                                 .async()
                                 .let(outputAccept(range(1, 1000)))
                                 .let(Processors.<Object, Integer>backoffOn(getSingleThreadRunner(),
                                         Backoffs.afterCount(2).linearDelay(seconds(10))))
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

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 }))
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
                                 .let(outputAccept(range(1, 1000)))
                                 .let(Processors.<Object, Integer>backoffOn(getSingleThreadRunner(),
                                         2, 10, TimeUnit.SECONDS))
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

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 }))
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
                                 .let(outputAccept(range(1, 1000)))
                                 .let(Processors.<Object, Integer>backoffOn(getSingleThreadRunner(),
                                         2, seconds(10)))
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

                                     public SumData apply(final SumData data1,
                                             final SumData data2) {
                                         return new SumData(data1.sum + data2.sum,
                                                 data1.count + data2.count);
                                     }
                                 }))
                                 .map(new Function<SumData, Double>() {

                                     public Double apply(final SumData data) {
                                         return data.sum / data.count;
                                     }
                                 })
                                 .mapOn(null)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBackoffNullPointerError() {
        try {
            Processors.backoffOn(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.backoffOn(null, 1, 0, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.backoffOn(null, 1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new Processors();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.delay(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.delay(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.delay(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.delay(seconds(1)))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDelayNullPointerError() {
        try {
            Processors.delay(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.lag(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.lag(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.lag(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.lag(seconds(1)))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLagNullPointerError() {
        try {
            Processors.lag(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testOutput() {
        assertThat(
                JRoutineStream.withStream().sync().let(Processors.outputGet(new Supplier<String>() {

                    public String get() {
                        return "TEST2";
                    }
                })).syncCall("test1").all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.outputAccept(new Consumer<Channel<String, ?>>() {

                                     public void accept(final Channel<String, ?> resultChannel) {
                                         resultChannel.pass("TEST2");
                                     }
                                 }))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream().let(Processors.outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.outputAccept(3,
                                         new Consumer<Channel<String, ?>>() {

                                             public void accept(
                                                     final Channel<String, ?> resultChannel) {
                                                 resultChannel.pass("TEST2");
                                             }
                                         }))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream().let(Processors.outputGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.outputAccept(new Consumer<Channel<String, ?>>() {

                                     public void accept(final Channel<String, ?> resultChannel) {
                                         resultChannel.pass("TEST2");
                                     }
                                 }))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream().let(Processors.outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.outputAccept(3,
                                         new Consumer<Channel<String, ?>>() {

                                             public void accept(
                                                     final Channel<String, ?> resultChannel) {
                                                 resultChannel.pass("TEST2");
                                             }
                                         }))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream().let(Processors.outputGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.outputAccept(new Consumer<Channel<String, ?>>() {

                                     public void accept(final Channel<String, ?> resultChannel) {
                                         resultChannel.pass("TEST2");
                                     }
                                 }))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("TEST2");
        assertThat(JRoutineStream.withStream().let(Processors.outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.outputAccept(3,
                                         new Consumer<Channel<String, ?>>() {

                                             public void accept(
                                                     final Channel<String, ?> resultChannel) {
                                                 resultChannel.pass("TEST2");
                                             }
                                         }))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    @Test
    public void testOutput2() {
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output((String) null))
                                 .syncCall("test1")
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output((String[]) null))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(
                JRoutineStream.withStream().sync().let(Processors.output()).syncCall("test1").all())
                .isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output((List<String>) null))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output(Collections.<String>emptyList()))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output("TEST2"))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output("TEST2", "TEST2"))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output(Collections.singletonList("TEST2")))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Processors.output(JRoutineCore.io().of("TEST2", "TEST2")))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");

        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((String) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((String[]) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output())
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((List<String>) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(Collections.<String>emptyList()))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output("TEST2"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output("TEST2", "TEST2"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(Collections.singletonList("TEST2")))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(JRoutineCore.io().of("TEST2", "TEST2")))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");

        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((String) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((String[]) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output())
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output((List<String>) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(Collections.<String>emptyList()))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output("TEST2"))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output("TEST2", "TEST2"))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(Collections.singletonList("TEST2")))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Processors.output(JRoutineCore.io().of("TEST2", "TEST2")))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
    }

    @Test
    public void testOutputNegativeCount() {
        try {
            outputGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            outputGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            outputAccept(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            outputAccept(0, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testParallelConfiguration() {
        assertThat(JRoutineStream.<String>withStream().let(Processors.<String, String>parallel(1))
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .let(Processors.<String, String>parallel(1))
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .let(Processors.<String, String>parallel(1))
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
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Processors.parallel(2, sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Processors.parallelBy(Functions.<Integer>identity(), sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Processors.parallel(2, JRoutineCore.with(
                                         IdentityInvocation.<Integer>factoryOf())))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Processors.parallelBy(Functions.<Integer>identity(),
                                         JRoutineCore.with(
                                                 IdentityInvocation.<Integer>factoryOf())))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParallelSplitNullPointerError() {
        try {
            Processors.parallel(1, (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.parallel(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.parallelBy(null, JRoutineStream.withStream().buildRoutine());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.parallelBy(null, JRoutineStream.withStream());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.parallelBy(Functions.identity(), (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.parallelBy(Functions.identity(), null);
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
                                               .let(Processors.<String, Object>retry(2))
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
                                                      .let(Processors.<String, Object>retry(1))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStream.<String>withStream().map(new AbortInvocation())
                                               .map(factoryOf(ThrowException.class, count3))
                                               .let(Processors.<String, Object>retry(2))
                                               .asyncCall("test")
                                               .after(seconds(3))
                                               .throwError();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRetryNullPointerError() {
        try {
            Processors.retry(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.retry(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testTryCatch() {
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(new Function<Object, Object>() {

                                                          public Object apply(final Object o) {
                                                              throw new NullPointerException();
                                                          }
                                                      })
                                                      .let(Processors.<String,
                                                              Object>tryCatchAccept(
                                                              new BiConsumer<RoutineException,
                                                                      Channel<Object, ?>>() {

                                                                  public void accept(
                                                                          final RoutineException e,
                                                                          final Channel<Object,
                                                                                  ?> channel) {
                                                                      channel.pass("exception");
                                                                  }
                                                              }))
                                                      .syncCall("test")
                                                      .next()).isEqualTo("exception");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(new Function<Object, Object>() {

                                                          public Object apply(final Object o) {
                                                              return o;
                                                          }
                                                      })
                                                      .let(Processors.<String,
                                                              Object>tryCatchAccept(
                                                              new BiConsumer<RoutineException,
                                                                      Channel<Object, ?>>() {

                                                                  public void accept(
                                                                          final RoutineException e,
                                                                          final Channel<Object,
                                                                                  ?> channel) {
                                                                      channel.pass("exception");
                                                                  }
                                                              }))
                                                      .syncCall("test")
                                                      .next()).isEqualTo("test");
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                throw new NullPointerException();
            }
        }).let(Processors.<String, Object>tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {
                return "exception";
            }
        })).syncCall("test").next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        try {
            Processors.tryCatchAccept(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Processors.tryCatch(null);
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
            }).let(Processors.<String, Object>tryFinally(new Action() {

                public void perform() {
                    isRun.set(true);
                }
            })).syncCall("test").next();

        } catch (final RoutineException ignored) {
        }

        assertThat(isRun.getAndSet(false)).isTrue();
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {
                return o;
            }
        }).let(Processors.<String, Object>tryFinally(new Action() {

            public void perform() {
                isRun.set(true);
            }
        })).syncCall("test").next()).isEqualTo("test");
        assertThat(isRun.getAndSet(false)).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryFinallyNullPointerError() {
        try {
            Processors.tryFinally(null);
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
