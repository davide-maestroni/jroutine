package com.github.dm.jrt.stream.transform;

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
import com.github.dm.jrt.core.util.Backoff;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.Operators.replaceWith;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.transform.Transformations.backoffOn;
import static com.github.dm.jrt.stream.transform.Transformations.delay;
import static com.github.dm.jrt.stream.transform.Transformations.lag;
import static com.github.dm.jrt.stream.transform.Transformations.parallel;
import static com.github.dm.jrt.stream.transform.Transformations.parallelBy;
import static com.github.dm.jrt.stream.transform.Transformations.retry;
import static com.github.dm.jrt.stream.transform.Transformations.tryCatch;
import static com.github.dm.jrt.stream.transform.Transformations.tryCatchWith;
import static com.github.dm.jrt.stream.transform.Transformations.tryFinally;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream transformation unit tests.
 * <p>
 * Created by davide-maestroni on 07/07/2016.
 */
public class TransformationsTest {

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
                                 .map(replaceWith(range(1, 1000)))
                                 .let(Transformations.<Object, Integer>backoffOn(
                                         getSingleThreadRunner(), 2,
                                         Backoffs.linearDelay(seconds(10))))
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
                                 .map(replaceWith(range(1, 1000)))
                                 .let(Transformations.<Object, Integer>backoffOn(
                                         getSingleThreadRunner(), 2, 10, TimeUnit.SECONDS))
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
                                 .map(replaceWith(range(1, 1000)))
                                 .let(Transformations.<Object, Integer>backoffOn(
                                         getSingleThreadRunner(), 2, seconds(10)))
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
            backoffOn(null, 1, (Backoff) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            backoffOn(null, 1, 0, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new Transformations();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(delay(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(delay(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(delay(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(delay(seconds(1)))
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
            delay(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(lag(1, TimeUnit.SECONDS))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(lag(seconds(1)))
                                 .asyncCall("test")
                                 .after(seconds(3))
                                 .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(lag(1, TimeUnit.SECONDS))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.withStream()
                                 .let(lag(seconds(1)))
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
            lag(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testParallelConfiguration() {
        assertThat(
                JRoutineStream.<String>withStream().let(Transformations.<String, String>parallel(1))
                                                   .map(new Function<String, String>() {

                                                       public String apply(final String s) {
                                                           return s.toUpperCase();
                                                       }
                                                   })
                                                   .asyncCall("test1", "test2")
                                                   .after(seconds(3))
                                                   .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .let(Transformations.<String, String>parallel(
                                                              1))
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .let(Transformations.<String, String>parallel(
                                                              1))
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
                                 .map(replaceWith(range(1, 3)))
                                 .let(parallel(2, sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .map(replaceWith(range(1, 3)))
                                 .let(parallelBy(Functions.<Integer>identity(), sqr))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1L, 4L, 9L);
        assertThat(JRoutineStream.withStream()
                                 .map(replaceWith(range(1, 3)))
                                 .let(parallel(2, JRoutineCore.with(
                                         IdentityInvocation.<Integer>factoryOf())))
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .all()).containsOnly(1, 2, 3);
        assertThat(JRoutineStream.withStream()
                                 .map(replaceWith(range(1, 3)))
                                 .let(parallelBy(Functions.<Integer>identity(), JRoutineCore.with(
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
            parallel(1, (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            parallel(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            parallelBy(null, JRoutineStream.withStream().buildRoutine());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            parallelBy(null, JRoutineStream.withStream());
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            parallelBy(Functions.identity(), (Routine<Object, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            parallelBy(Functions.identity(), null);
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
                                               .let(Transformations.<String, Object>retry(2))
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
                                                      .let(Transformations.<String, Object>retry(1))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStream.<String>withStream().map(new AbortInvocation())
                                               .map(factoryOf(ThrowException.class, count3))
                                               .let(Transformations.<String, Object>retry(2))
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
            retry(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            retry(null);
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
                                                      .let(Transformations.<String,
                                                              Object>tryCatchWith(
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
                                                      .let(Transformations.<String,
                                                              Object>tryCatchWith(
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
        }).let(Transformations.<String, Object>tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {
                return "exception";
            }
        })).syncCall("test").next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        try {
            tryCatchWith(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            tryCatch(null);
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
            }).let(Transformations.<String, Object>tryFinally(new Action() {

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
        }).let(Transformations.<String, Object>tryFinally(new Action() {

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
            tryFinally(null);
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
