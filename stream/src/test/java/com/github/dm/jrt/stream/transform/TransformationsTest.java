package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.Backoffs;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.stream.JRoutineStream;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.Operators.replaceWith;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.transform.Transformations.delay;
import static com.github.dm.jrt.stream.transform.Transformations.lag;
import static com.github.dm.jrt.stream.transform.Transformations.onComplete;
import static com.github.dm.jrt.stream.transform.Transformations.onError;
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
                                 .asyncMap(null)
                                 .asyncCall()
                                 .close()
                                 .after(seconds(3))
                                 .next()).isCloseTo(21, Offset.offset(0.1));
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
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testOnComplete() {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStream.withStream().let(onComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        })).asyncCall("test").after(seconds(3)).all()).isEmpty();
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStream.withStream().map(new Function<Object, Object>() {

            public Object apply(final Object o) throws Exception {
                throw new NoSuchElementException();
            }
        }).let(onComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        })).asyncCall("test").after(seconds(3)).getError()).isExactlyInstanceOf(
                InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    public void testOnError() {
        try {
            JRoutineStream.withStream().sync().map(new Function<Object, Object>() {

                public Object apply(final Object o) {
                    throw new NullPointerException();
                }
            }).let(onError(new Consumer<RoutineException>() {

                public void accept(final RoutineException e) {
                    throw new IllegalArgumentException();
                }
            })).call("test").next();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(JRoutineStream.withStream().sync().map(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).let(onError(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {
            }
        })).syncCall("test").all()).isEmpty();
    }

    @Test
    public void testOnOutput() {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .let(Transformations.<String, String>onOutput(
                                                              new Consumer<String>() {

                                                                  public void accept(
                                                                          final String s) {
                                                                      list.add(s);
                                                                  }
                                                              }))
                                                      .syncCall("test1", "test2", "test3")
                                                      .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .let(Transformations.<String, String>onOutput(
                                                              new Consumer<String>() {

                                                                  public void accept(
                                                                          final String s) {
                                                                      list.add(s);
                                                                  }
                                                              }))
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(3))
                                                      .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
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
        assertThat(JRoutineStream.<String>withStream().let(
                Transformations.<String, String>order(OrderType.BY_CALL))
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
        assertThat(JRoutineStream.<String>withStream().let(
                Transformations.<String, String>order(OrderType.BY_CALL))
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

    private static class SumData {

        private final int count;

        private final double sum;

        private SumData(final double sum, final int count) {
            this.sum = sum;
            this.count = count;
        }
    }
}
