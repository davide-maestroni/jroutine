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

package com.github.dm.jrt.stream.modifier;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
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

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.modifier.Modifiers.outputAccept;
import static com.github.dm.jrt.stream.modifier.Modifiers.outputGet;
import static com.github.dm.jrt.stream.modifier.Modifiers.throttle;
import static com.github.dm.jrt.stream.modifier.Modifiers.timeoutAfter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream transformation unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class ModifiersTest {

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
        Assertions.assertThat(JRoutineStream.withStream()
                                            .async()
                                            .let(outputAccept(range(1, 1000)))
                                            .applyInvocationConfiguration()
                                            .withRunner(getSingleThreadRunner())
                                            .withInputBackoff(
                                                    Backoffs.afterCount(2).linearDelay(seconds(10)))
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
                                            .map(reduce(
                                                    new BiFunction<SumData, SumData, SumData>() {

                                                        public SumData apply(final SumData data1,
                                                                final SumData data2) {
                                                            return new SumData(
                                                                    data1.sum + data2.sum,
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
        Assertions.assertThat(JRoutineStream.withStream()
                                            .async()
                                            .let(outputAccept(range(1, 1000)))
                                            .applyInvocationConfiguration()
                                            .withRunner(getSingleThreadRunner())
                                            .withInputBackoff(Backoffs.afterCount(2)
                                                                      .constantDelay(seconds(10)))
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
                                            .map(reduce(
                                                    new BiFunction<SumData, SumData, SumData>() {

                                                        public SumData apply(final SumData data1,
                                                                final SumData data2) {
                                                            return new SumData(
                                                                    data1.sum + data2.sum,
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
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new Modifiers();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.delay(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.delay(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.delay(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.delay(seconds(1)))
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
            Modifiers.delay(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.lag(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.lag(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.lag(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.lag(seconds(1)))
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
            Modifiers.lag(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testOutput() {
        assertThat(JRoutineStream.withStream().sync().let(outputGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(outputAccept(new Consumer<Channel<String, ?>>() {

                                     public void accept(final Channel<String, ?> resultChannel) {
                                         resultChannel.pass("TEST2");
                                     }
                                 }))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream().let(outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(outputAccept(3, new Consumer<Channel<String, ?>>() {

                                     public void accept(final Channel<String, ?> resultChannel) {
                                         resultChannel.pass("TEST2");
                                     }
                                 }))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStream.withStream().let(outputGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream().let(outputAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream().let(outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(
                JRoutineStream.withStream().let(outputAccept(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                })).asyncCall("test1").after(seconds(3)).all()).containsOnly("TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStream.withStream().let(outputGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineStream.withStream().let(outputAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineStream.withStream().let(outputGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(
                JRoutineStream.withStream().let(outputAccept(3, new Consumer<Channel<String, ?>>() {

                    public void accept(final Channel<String, ?> resultChannel) {
                        resultChannel.pass("TEST2");
                    }
                })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
    }

    @Test
    public void testOutput2() {
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output((String) null))
                                 .syncCall("test1")
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output((String[]) null))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output())
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output((List<String>) null))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output(Collections.<String>emptyList()))
                                 .syncCall("test1")
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output("TEST2"))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output("TEST2", "TEST2"))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output(Collections.singletonList("TEST2")))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .let(Modifiers.output(JRoutineCore.io().of("TEST2", "TEST2")))
                                 .syncCall("test1")
                                 .all()).containsOnly("TEST2");

        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((String) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((String[]) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output())
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((List<String>) null))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(Collections.<String>emptyList()))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output("TEST2"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output("TEST2", "TEST2"))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(Collections.singletonList("TEST2")))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(JRoutineCore.io().of("TEST2", "TEST2")))
                                 .asyncCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");

        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((String) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly((String) null);
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((String[]) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output())
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output((List<String>) null))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(Collections.<String>emptyList()))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output("TEST2"))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output("TEST2", "TEST2"))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(Collections.singletonList("TEST2")))
                                 .parallelCall("test1")
                                 .after(seconds(3))
                                 .all()).containsOnly("TEST2");
        assertThat(JRoutineStream.withStream()
                                 .let(Modifiers.output(JRoutineCore.io().of("TEST2", "TEST2")))
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
                                 .let(Modifiers.parallel(2, sqr.buildFactory()))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Modifiers.parallel(2, sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Modifiers.parallel(2, JRoutineCore.with(
                                         IdentityInvocation.<Integer>factoryOf())))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Modifiers.parallelBy(Functions.<Integer>identity(),
                                         sqr.buildFactory()))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Modifiers.parallelBy(Functions.<Integer>identity(), sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .let(outputAccept(range(1, 3)))
                                 .let(Modifiers.parallelBy(Functions.<Integer>identity(),
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
            Modifiers.parallel(1, (InvocationFactory<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallel(1, (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallel(1, (RoutineBuilder<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallelBy(null, JRoutineStream.withStream().buildRoutine());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallelBy(null, JRoutineStream.withStream());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallelBy(Functions.identity(), (InvocationFactory<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallelBy(Functions.identity(), (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.parallelBy(Functions.identity(), (RoutineBuilder<Object, ?>) null);
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
                                               .let(Modifiers.<String, Object>retry(2))
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
                                                      .let(Modifiers.<String, Object>retry(1))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStream.<String>withStream().map(new AbortInvocation())
                                               .map(factoryOf(ThrowException.class, count3))
                                               .let(Modifiers.<String, Object>retry(2))
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
            Modifiers.retry(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.retry(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testThrottle() throws InterruptedException {
        final Routine<Object, Object> routine = JRoutineStream.withStream()
                                                              .let(throttle(1))
                                                              .applyInvocationConfiguration()
                                                              .withRunner(Runners.poolRunner(1))
                                                              .configured()
                                                              .buildRoutine();
        final Channel<Object, Object> channel1 = routine.asyncCall().pass("test1");
        final Channel<Object, Object> channel2 = routine.asyncCall().pass("test2");
        seconds(0.5).sleepAtLeast();
        assertThat(channel1.close().after(seconds(1.5)).next()).isEqualTo("test1");
        assertThat(channel2.close().after(seconds(1.5)).next()).isEqualTo("test2");
    }

    @Test
    public void testThrottleAbort() throws InterruptedException {
        final Routine<Object, Object> routine = JRoutineStream.withStream()
                                                              .let(throttle(1))
                                                              .applyInvocationConfiguration()
                                                              .withRunner(Runners.poolRunner(1))
                                                              .configured()
                                                              .buildRoutine();
        final Channel<Object, Object> channel1 = routine.asyncCall().pass("test1");
        final Channel<Object, Object> channel2 = routine.asyncCall().pass("test2");
        seconds(0.5).sleepAtLeast();
        assertThat(channel1.abort()).isTrue();
        assertThat(channel2.close().after(seconds(1.5)).next()).isEqualTo("test2");
    }

    @Test
    public void testTimeThrottle() {
        final Routine<Object, Object> routine =
                JRoutineStream.withStream().let(throttle(1, seconds(1))).buildRoutine();
        final Channel<Object, Object> channel1 = routine.asyncCall("test1");
        final Channel<Object, Object> channel2 = routine.asyncCall("test2");
        assertThat(channel1.after(seconds(1.5)).next()).isEqualTo("test1");
        assertThat(channel2.after(seconds(1.5)).next()).isEqualTo("test2");
    }

    @Test
    public void testTimeout() {
        assertThat(JRoutineStream.withStream()
                                 .let(timeoutAfter(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(1))
                                 .all()).containsExactly("test");
        final Channel<Object, Object> channel =
                JRoutineStream.withStream().let(timeoutAfter(millis(1))).asyncCall().pass("test");
        assertThat(channel.after(seconds(1)).getError()).isExactlyInstanceOf(
                ResultTimeoutException.class);
    }

    @Test
    public void testTryCatch() {
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(new Function<Object, Object>() {

                                                          public Object apply(final Object o) {
                                                              throw new NullPointerException();
                                                          }
                                                      })
                                                      .let(Modifiers.<String, Object>tryCatchAccept(
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
                                                      .let(Modifiers.<String, Object>tryCatchAccept(
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
        }).let(Modifiers.<String, Object>tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {
                return "exception";
            }
        })).syncCall("test").next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        try {
            Modifiers.tryCatchAccept(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            Modifiers.tryCatch(null);
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
            }).let(Modifiers.<String, Object>tryFinally(new Action() {

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
        }).let(Modifiers.<String, Object>tryFinally(new Action() {

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
            Modifiers.tryFinally(null);
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
