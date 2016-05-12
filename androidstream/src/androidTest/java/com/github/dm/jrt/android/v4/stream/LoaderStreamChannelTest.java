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

package com.github.dm.jrt.android.v4.stream;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.HandlerThread;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.IdentityContextInvocation;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamChannelCompat
        .LoaderStreamConfigurationCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.invocation.TransformInvocation;
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
import com.github.dm.jrt.stream.StreamChannel;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionOperation;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android stream channel unit tests.
 * <p>
 * Created by davide-maestroni on 03/10/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderStreamChannelTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderStreamChannelTest() {

        super(TestActivity.class);
    }

    @NotNull
    private static Function<StreamChannel<Integer, Integer>, StreamChannel<Integer, Long>>
    sqrFunction() {

        return new Function<StreamChannel<Integer, Integer>, StreamChannel<Integer, Long>>() {

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
    }

    private static void testCollect(@NotNull final FragmentActivity activity) {

        assertThat(
                LoaderStreamsCompat.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
                        new StringBuilder("test3"))
                                   .with(loaderFrom(activity))
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
                                   .afterMax(seconds(10))
                                   .all()).containsExactly("test1test2test3");
        assertThat(
                LoaderStreamsCompat.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
                        new StringBuilder("test3"))
                                   .with(loaderFrom(activity))
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
                                   .afterMax(seconds(10))
                                   .all()).containsExactly("test1test2test3");
    }

    private static void testCollectCollection(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .collectIn(new Supplier<List<String>>() {

                                          public List<String> get() {

                                              return new ArrayList<String>();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .next()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .collectIn(new Supplier<List<String>>() {

                                          public List<String> get() {

                                              return new ArrayList<String>();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .next()).containsExactly("test1", "test2", "test3");
    }

    private static void testCollectSeed(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .collect(new Supplier<StringBuilder>() {

                                          public StringBuilder get() {

                                              return new StringBuilder();
                                          }
                                      }, new BiConsumer<StringBuilder, String>() {

                                          public void accept(final StringBuilder b,
                                                  final String s) {

                                              b.append(s);
                                          }
                                      })
                                      .map(new Function<StringBuilder, String>() {

                                          public String apply(final StringBuilder builder) {

                                              return builder.toString();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .collect(new Supplier<StringBuilder>() {

                                          public StringBuilder get() {

                                              return new StringBuilder();
                                          }
                                      }, new BiConsumer<StringBuilder, String>() {

                                          public void accept(final StringBuilder b,
                                                  final String s) {

                                              b.append(s);
                                          }
                                      })
                                      .map(new Function<StringBuilder, String>() {

                                          public String apply(final StringBuilder builder) {

                                              return builder.toString();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
    }

    private static void testConcat(final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .concat("test2")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .concat("test2", "test3")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .concat(Arrays.asList("test2", "test3"))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .concat(JRoutineCore.io().of("test2", "test3"))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
    }

    private static void testConcat2(final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .concatGet(new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .concatGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .concatGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .concatGetN(3, new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .concatGet(new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .concatGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .concatGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .concatGetN(3, new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .concatGet(new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .concatGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .concatGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .concatGetN(3, new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
    }

    private static void testConfiguration(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .parallel(1)
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel(1)
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
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
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2");
        final Runner handlerRunner = AndroidRunners.handlerRunner(
                new HandlerThread(LoaderStreamChannelTest.class.getName()));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .async()
                                      .thenGetN(range(1, 1000))
                                      .backoffOn(handlerRunner, 2,
                                              Backoffs.linearDelay(seconds(10)))
                                      .map(Functions.<Number>identity())
                                      .with(loaderFrom(activity))
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
                                      .runOnShared()
                                      .afterMax(seconds(10))
                                      .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .async()
                                      .thenGetN(range(1, 1000))
                                      .backoffOn(handlerRunner, 2, 10, TimeUnit.SECONDS)
                                      .map(Functions.<Number>identity())
                                      .with(loaderFrom(activity))
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
                                      .runOnShared()
                                      .afterMax(seconds(10))
                                      .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .async()
                                      .thenGetN(range(1, 1000))
                                      .backoffOn(handlerRunner, 2, seconds(10))
                                      .map(Functions.<Number>identity())
                                      .with(loaderFrom(activity))
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
                                      .runOnShared()
                                      .afterMax(seconds(10))
                                      .next()).isCloseTo(21, Offset.offset(0.1));
    }

    private static void testConsume(@NotNull final FragmentActivity activity) {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .onOutput(new Consumer<String>() {

                                          public void accept(final String s) {

                                              list.add(s);
                                          }
                                      })
                                      .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .onOutput(new Consumer<String>() {

                                          public void accept(final String s) {

                                              list.add(s);
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    private static void testConsumeError(@NotNull final FragmentActivity activity) {

        try {
            LoaderStreamsCompat.streamOf("test")
                               .with(loaderFrom(activity))
                               .sync()
                               .map(new Function<Object, Object>() {

                                   public Object apply(final Object o) {

                                       throw new NullPointerException();
                                   }
                               })
                               .onError(new Consumer<RoutineException>() {

                                   public void accept(final RoutineException e) {

                                       throw new IllegalArgumentException();
                                   }
                               })
                               .next();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {

                                              throw new NullPointerException();
                                          }
                                      })
                                      .onError(new Consumer<RoutineException>() {

                                          public void accept(final RoutineException e) {

                                          }
                                      })
                                      .all()).isEmpty();
    }

    private static void testFlatMap(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", null, "test2", null)
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .flatMap(new Function<String, OutputChannel<String>>() {

                                          public OutputChannel<String> apply(final String s) {

                                              return LoaderStreamsCompat.streamOf(s)
                                                                        .with(loaderFrom(activity))
                                                                        .sync()
                                                                        .filter(Functions
                                                                                .<String>isNotNull());
                                          }
                                      })
                                      .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreamsCompat.streamOf("test1", null, "test2", null)
                                      .with(loaderFrom(activity))
                                      .async()
                                      .flatMap(new Function<String, OutputChannel<String>>() {

                                          public OutputChannel<String> apply(final String s) {

                                              return LoaderStreamsCompat.streamOf(s)
                                                                        .with(loaderFrom(activity))
                                                                        .sync()
                                                                        .filter(Functions
                                                                                .<String>isNotNull());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreamsCompat.streamOf("test1", null, "test2", null)
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .flatMap(new Function<String, OutputChannel<String>>() {

                                          public OutputChannel<String> apply(final String s) {

                                              return LoaderStreamsCompat.streamOf(s)
                                                                        .with(loaderFrom(activity))
                                                                        .sync()
                                                                        .filter(Functions
                                                                                .<String>isNotNull());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("test1", "test2");
        assertThat(LoaderStreamsCompat.streamOf("test1", null, "test2", null)
                                      .with(loaderFrom(activity))
                                      .serial()
                                      .flatMap(new Function<String, OutputChannel<String>>() {

                                          public OutputChannel<String> apply(final String s) {

                                              return LoaderStreamsCompat.streamOf(s)
                                                                        .with(loaderFrom(activity))
                                                                        .sync()
                                                                        .filter(Functions
                                                                                .<String>isNotNull());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("test1", "test2");
    }

    private static void testInvocationDeadlock(@NotNull final FragmentActivity activity) {

        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            LoaderStreamsCompat.streamOf("test")
                               .with(loaderFrom(activity))
                               .invocationConfiguration()
                               .withRunner(runner1)
                               .apply()
                               .map(new Function<String, Object>() {

                                   public Object apply(final String s) {

                                       return LoaderStreamsCompat.streamOf(s)
                                                                 .with(loaderFrom(activity))
                                                                 .invocationConfiguration()
                                                                 .withRunner(runner1)
                                                                 .apply()
                                                                 .map(Functions.identity())
                                                                 .invocationConfiguration()
                                                                 .withRunner(runner2)
                                                                 .apply()
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

    private static void testMapAllConsumer(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .mapAllN(new BiConsumer<List<?
                                              extends String>, ResultChannel<String>>() {

                                          public void accept(final List<?
                                                  extends
                                                  String> strings,
                                                  final ResultChannel<String> result) {

                                              final StringBuilder builder = new StringBuilder();
                                              for (final String string : strings) {
                                                  builder.append(string);
                                              }

                                              result.pass(builder.toString());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .mapAllN(
                                              new BiConsumer<List<? extends String>,
                                                      ResultChannel<String>>() {

                                                  public void accept(
                                                          final List<? extends String> strings,
                                                          final ResultChannel<String> result) {

                                                      final StringBuilder builder =
                                                              new StringBuilder();
                                                      for (final String string : strings) {
                                                          builder.append(string);
                                                      }

                                                      result.pass(builder.toString());
                                                  }
                                              })
                                      .all()).containsExactly("test1test2test3");
    }

    private static void testMapAllFunction(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .mapAll(new Function<List<? extends String>, String>() {

                                          public String apply(
                                                  final List<? extends String> strings) {

                                              final StringBuilder builder = new StringBuilder();
                                              for (final String string : strings) {
                                                  builder.append(string);
                                              }

                                              return builder.toString();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .mapAll(new Function<List<? extends String>, String>() {

                                          public String apply(
                                                  final List<? extends String> strings) {

                                              final StringBuilder builder = new StringBuilder();
                                              for (final String string : strings) {
                                                  builder.append(string);
                                              }

                                              return builder.toString();
                                          }
                                      })
                                      .all()).containsExactly("test1test2test3");
    }

    private static void testMapConsumer(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .mapN(new BiConsumer<String, ResultChannel<String>>() {

                                          public void accept(final String s,
                                                  final ResultChannel<String> result) {

                                              result.pass(s.toUpperCase());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel()
                                      .mapN(new BiConsumer<String, ResultChannel<String>>() {

                                          public void accept(final String s,
                                                  final ResultChannel<String> result) {

                                              result.pass(s.toUpperCase());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .mapN(new BiConsumer<String, ResultChannel<String>>() {

                                          public void accept(final String s,
                                                  final ResultChannel<String> result) {

                                              result.pass(s.toUpperCase());
                                          }
                                      })
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .ordered(OrderType.BY_CALL)
                                      .serial()
                                      .mapN(new BiConsumer<String, ResultChannel<String>>() {

                                          public void accept(final String s,
                                                  final ResultChannel<String> result) {

                                              result.pass(s.toUpperCase());
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    private static void testMapFunction(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel()
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(activity))
                                      .ordered(OrderType.BY_CALL)
                                      .serial()
                                      .map(new Function<String, String>() {

                                          public String apply(final String s) {

                                              return s.toUpperCase();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    private static void testOrElse(final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .orElse("est")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .orElse("est1", "est2")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .orElse(Arrays.asList("est1", "est2"))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .orElseGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(final ResultChannel<String> result) {

                                              result.pass("est");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .orElseGet(new Supplier<String>() {

                                          public String get() {

                                              return "est";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(activity))
                                      .orElse("est")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("est");
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(activity))
                                      .orElse("est1", "est2")
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("est1", "est2");
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(activity))
                                      .orElse(Arrays.asList("est1", "est2"))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("est1", "est2");
        assertThat(LoaderStreamsCompat.<String>streamOf().with(loaderFrom(activity))
                                                         .orElseGetN(
                                                                 new Consumer<ResultChannel<String>>() {

                                                                     public void accept(
                                                                             final
                                                                             ResultChannel<String> result) {

                                                                         result.pass("est");
                                                                     }
                                                                 })
                                                         .afterMax(seconds(10))
                                                         .all()).containsExactly("est");
        assertThat(LoaderStreamsCompat.<String>streamOf().with(loaderFrom(activity))
                                                         .orElseGetN(2,
                                                                 new Consumer<ResultChannel<String>>() {

                                                                     public void accept(
                                                                             final
                                                                             ResultChannel<String> result) {

                                                                         result.pass("est");
                                                                     }
                                                                 })
                                                         .afterMax(seconds(10))
                                                         .all()).containsExactly("est", "est");
        assertThat(LoaderStreamsCompat.<String>streamOf().with(loaderFrom(activity))
                                                         .orElseGet(new Supplier<String>() {

                                                             public String get() {

                                                                 return "est";
                                                             }
                                                         })
                                                         .afterMax(seconds(10))
                                                         .all()).containsExactly("est");
        assertThat(LoaderStreamsCompat.<String>streamOf().with(loaderFrom(activity))
                                                         .orElseGet(2, new Supplier<String>() {

                                                             public String get() {

                                                                 return "est";
                                                             }
                                                         })
                                                         .afterMax(seconds(10))
                                                         .all()).containsExactly("est", "est");
    }

    private static void testPeek(@NotNull final FragmentActivity activity) {

        final ArrayList<String> data = new ArrayList<String>();
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .peek(new Consumer<String>() {

                                          public void accept(final String s) {

                                              data.add(s);
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
    }

    private static void testReduce(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .reduce(new BiFunction<String, String, String>() {

                                          public String apply(final String s, final String s2) {

                                              return s + s2;
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .reduce(new BiFunction<String, String, String>() {

                                          public String apply(final String s, final String s2) {

                                              return s + s2;
                                          }
                                      })
                                      .all()).containsExactly("test1test2test3");
    }

    private static void testReduceSeed(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .reduce(new Supplier<StringBuilder>() {

                                          public StringBuilder get() {

                                              return new StringBuilder();
                                          }
                                      }, new BiFunction<StringBuilder, String, StringBuilder>() {

                                          public StringBuilder apply(final StringBuilder b,
                                                  final String s) {

                                              return b.append(s);
                                          }
                                      })
                                      .map(new Function<StringBuilder, String>() {

                                          public String apply(final StringBuilder builder) {

                                              return builder.toString();
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .reduce(new Supplier<StringBuilder>() {

                                          public StringBuilder get() {

                                              return new StringBuilder();
                                          }
                                      }, new BiFunction<StringBuilder, String, StringBuilder>() {

                                          public StringBuilder apply(final StringBuilder b,
                                                  final String s) {

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

    private static void testThen(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .thenGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .thenGet(new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .thenGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .thenGetN(new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .thenGet(new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .async()
                                      .thenGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .thenGetN(3, new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .parallel()
                                      .thenGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .serial()
                                      .thenGetN(3, new Consumer<ResultChannel<String>>() {

                                          public void accept(
                                                  final ResultChannel<String> resultChannel) {

                                              resultChannel.pass("TEST2");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(activity))
                                      .serial()
                                      .thenGet(3, new Supplier<String>() {

                                          public String get() {

                                              return "TEST2";
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    private static void testTryCatch(@NotNull final FragmentActivity activity) {

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {

                                              throw new NullPointerException();
                                          }
                                      })
                                      .tryCatchN(
                                              new BiConsumer<RoutineException,
                                                      InputChannel<Object>>() {

                                                  public void accept(final RoutineException e,
                                                          final InputChannel<Object> channel) {

                                                      channel.pass("exception");
                                                  }
                                              })
                                      .next()).isEqualTo("exception");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {

                                              return o;
                                          }
                                      })
                                      .tryCatchN(
                                              new BiConsumer<RoutineException,
                                                      InputChannel<Object>>() {

                                                  public void accept(final RoutineException e,
                                                          final InputChannel<Object> channel) {

                                                      channel.pass("exception");
                                                  }
                                              })
                                      .next()).isEqualTo("test");

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {

                                              throw new NullPointerException();
                                          }
                                      })
                                      .tryCatch(new Function<RoutineException, Object>() {

                                          public Object apply(final RoutineException e) {

                                              return "exception";
                                          }
                                      })
                                      .next()).isEqualTo("exception");
    }

    private static void testTryFinally(@NotNull final FragmentActivity activity) {

        final AtomicBoolean isRun = new AtomicBoolean(false);
        try {
            LoaderStreamsCompat.streamOf("test")
                               .with(loaderFrom(activity))
                               .sync()
                               .map(new Function<Object, Object>() {

                                   public Object apply(final Object o) {

                                       throw new NullPointerException();
                                   }
                               })
                               .tryFinally(new Runnable() {

                                   public void run() {

                                       isRun.set(true);
                                   }
                               })
                               .next();

        } catch (final RoutineException ignored) {

        }

        assertThat(isRun.getAndSet(false)).isTrue();

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {

                                              return o;
                                          }
                                      })
                                      .tryFinally(new Runnable() {

                                          public void run() {

                                              isRun.set(true);
                                          }
                                      })
                                      .next()).isEqualTo("test");
        assertThat(isRun.getAndSet(false)).isTrue();
    }

    @NotNull
    private static BiFunction<LoaderStreamConfigurationCompat, Function<OutputChannel<String>,
            OutputChannel<String>>, Function<OutputChannel<String>, OutputChannel<String>>>
    transformBiFunction() {

        return new BiFunction<LoaderStreamConfigurationCompat, Function<OutputChannel<String>,
                OutputChannel<String>>, Function<OutputChannel<String>, OutputChannel<String>>>() {

            public Function<OutputChannel<String>, OutputChannel<String>> apply(
                    final LoaderStreamConfigurationCompat configuration,
                    final Function<OutputChannel<String>, OutputChannel<String>> function) {

                assertThat(configuration.asLoaderConfiguration()).isEqualTo(
                        LoaderConfiguration.defaultConfiguration());
                assertThat(configuration.getLoaderContext()).isInstanceOf(
                        LoaderContextCompat.class);
                return wrap(function).andThen(
                        new Function<OutputChannel<String>, OutputChannel<String>>() {

                            public OutputChannel<String> apply(
                                    final OutputChannel<String> channel) {

                                return JRoutineCore.on(new UpperCase()).asyncCall(channel);
                            }
                        });
            }
        };
    }

    @NotNull
    private static Function<Function<OutputChannel<String>, OutputChannel<String>>,
            Function<OutputChannel<String>, OutputChannel<String>>> transformFunction() {

        return new Function<Function<OutputChannel<String>, OutputChannel<String>>,
                Function<OutputChannel<String>, OutputChannel<String>>>() {

            public Function<OutputChannel<String>, OutputChannel<String>> apply(
                    final Function<OutputChannel<String>, OutputChannel<String>> function) {

                return wrap(function).andThen(
                        new Function<OutputChannel<String>, OutputChannel<String>>() {

                            public OutputChannel<String> apply(
                                    final OutputChannel<String> channel) {

                                return JRoutineCore.on(new UpperCase()).asyncCall(channel);
                            }
                        });
            }
        };
    }

    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final LoaderStreamChannelCompat<Object, Object> streamChannel =
                LoaderStreamsCompat.streamOf(ioChannel).with(loaderFrom(getActivity()));
        ioChannel.abort(new IllegalArgumentException());
        try {
            streamChannel.afterMax(seconds(10)).throwError();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
                IllegalArgumentException.class);
    }

    public void testApply() {

        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .apply(new Function<StreamChannel<String, String>,
                                              StreamChannel<String, String>>() {

                                          public StreamChannel<String, String> apply(
                                                  final StreamChannel<String, String> stream) {

                                              return stream.concat("test2");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .apply(new Function<StreamChannel<String, String>,
                                              LoaderStreamChannelCompat<String, String>>() {

                                          public LoaderStreamChannelCompat<String, String> apply(
                                                  final StreamChannel<String, String> stream) {

                                              return ((LoaderStreamChannelCompat<String, String>)
                                                      stream)
                                                      .concat("test2");
                                          }
                                      })
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2");
    }

    public void testBuilder() {

        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(getActivity()))
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(getActivity()))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(loaderFrom(getActivity()))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(Arrays.asList("test1", "test2", "test3"))
                                      .with(loaderFrom(getActivity()))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                      .with(loaderFrom(getActivity()))
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf((OutputChannel<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testChannel() {

        StreamChannel<String, String> channel =
                LoaderStreamsCompat.streamOf("test").with(loaderFrom(getActivity()));
        assertThat(channel.abort()).isFalse();
        assertThat(channel.abort(null)).isFalse();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        assertThat(channel.isBound()).isFalse();
        final ArrayList<String> results = new ArrayList<String>();
        assertThat(channel.afterMax(10, TimeUnit.SECONDS).hasNext()).isTrue();
        channel.immediately().allInto(results);
        assertThat(results).containsExactly("test");
        channel = LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                     .with(loaderFrom(getActivity()));
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyExit().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyExit().nextOrElse("test4")).isEqualTo("test4");
        final Iterator<String> iterator = LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                                             .with(loaderFrom(getActivity()))
                                                             .iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");
        try {
            iterator.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        channel =
                LoaderStreamsCompat.streamOf(ioChannel.after(1000, TimeUnit.SECONDS).pass("test"));
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

    public void testCollect() {

        testCollect(getActivity());
    }

    public void testCollectCollection() {

        testCollectCollection(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .async()
                               .with(loaderFrom(getActivity()))
                               .collectIn((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .sync()
                               .with(loaderFrom(getActivity()))
                               .collectIn((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().collect(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testCollectSeed() {

        testCollectSeed(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectSeedNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConcat() {

        testConcat(getActivity());
    }

    public void testConcat2() {

        testConcat2(getActivity());
    }

    public void testConfiguration() {

        testConfiguration(getActivity());
    }

    public void testConsume() {

        testConsume(getActivity());
    }

    public void testConsumeError() {

        testConsumeError(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testConsumeErrorNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).onError(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {

        final Consumer<Object> consumer = null;
        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFilter() {

        assertThat(LoaderStreamsCompat.streamOf(null, "test")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .filter(Functions.isNotNull())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf(null, "test")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .filter(Functions.isNotNull())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf(null, "test")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .filter(Functions.isNotNull())
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf(null, "test")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .filter(Functions.isNotNull())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().parallel().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().serial().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFlatMap() {

        testFlatMap(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFlatMapNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).parallel().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).serial().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFlatMapRetry() {

        final Routine<Object, String> routine =
                JRoutineCore.on(functionOperation(new Function<Object, String>() {

                    public String apply(final Object o) {

                        return o.toString();
                    }
                })).buildRoutine();
        try {
            LoaderStreamsCompat.streamOf((Object) null)
                               .with(loaderFrom(getActivity()))
                               .async()
                               .flatMap(new RetryFunction(getActivity(), routine))
                               .afterMax(seconds(10))
                               .all();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    public void testInvocationDeadlock() {

        testInvocationDeadlock(getActivity());
    }

    public void testLimit() {

        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .limit(5)
                                      .afterMax(seconds(10))
                                      .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .limit(0)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .limit(15)
                                      .afterMax(seconds(10))
                                      .all()).isEqualTo(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .limit(0)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
    }

    public void testMapAllConsumer() {

        testMapAllConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().mapAllN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapAllFunction() {

        testMapAllFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllFunctionNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().mapAll(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapConsumer() {

        testMapConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().mapN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapContextFactory() {

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .map(factory)
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .serial()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapContextFactoryIllegalState() {

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        try {
            LoaderStreamsCompat.streamOf("test").async().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf("test").sync().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf("test").parallel().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf("test").serial().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapContextFactoryNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapFactory() {

        final InvocationFactory<String, String> factory = factoryOf(UpperCase.class);
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .map(factory)
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .serial()
                                      .map(factory)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapFilter() {

        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .map(new UpperCase())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .parallel()
                                      .map(new UpperCase())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .map(new UpperCase())
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .ordered(OrderType.BY_CALL)
                                      .serial()
                                      .map(new UpperCase())
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .map((TransformInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .map((TransformInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .map((TransformInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .map((TransformInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapFunction() {

        testMapFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutineCore.on(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .apply()
                                                            .buildRoutine();
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .map(routine)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .map(routine)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .map(routine)
                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .map(routine)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testOrElse() {

        testOrElse(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).orElseGetN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).orElseGetN(1, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).orElseGet(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).orElseGet(1, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(LoaderStreamsCompat.streamOf(channel)
                                      .with(loaderFrom(getActivity()))
                                      .toSelectable(33)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();
        try {
            LoaderStreamsCompat.streamOf(channel)
                               .with(loaderFrom(getActivity()))
                               .toSelectable(33)
                               .afterMax(seconds(10))
                               .all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testPeek() {

        testPeek(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testPeekNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().peek(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testReduce() {

        testReduce(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testReduceNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testReduceSeed() {

        testReduceSeed(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testReduceSeedNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testReplay() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel =
                LoaderStreamsCompat.streamOf(ioChannel).with(loaderFrom(getActivity())).replay();
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

    public void testReplayAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel =
                LoaderStreamsCompat.streamOf(ioChannel).with(loaderFrom(getActivity())).replay();
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

    public void testRetry() {

        ThrowException.reset();
        try {
            LoaderStreamsCompat.streamOf("test")
                               .with(loaderFrom(getActivity()))
                               .map(new UpperCase())
                               .map(factoryOf(ThrowException.class))
                               .retry(2)
                               .afterMax(seconds(10))
                               .throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        ThrowException.reset();
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(getActivity()))
                                      .map(new UpperCase())
                                      .map(factoryOf(ThrowException.class, 1))
                                      .retry(2)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("TEST");

        ThrowException.reset();
        try {
            LoaderStreamsCompat.streamOf("test")
                               .with(loaderFrom(getActivity()))
                               .map(new AbortInvocation())
                               .map(factoryOf(ThrowException.class))
                               .retry(2)
                               .afterMax(seconds(10))
                               .throwError();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }

    public void testSize() {

        final InvocationChannel<Object, Object> channel =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.factoryOf())
                                    .asyncInvoke();
        assertThat(channel.size()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.size()).isEqualTo(1);
        final OutputChannel<Object> result = LoaderStreamsCompat.streamOf(channel.result());
        assertThat(result.afterMax(seconds(10)).hasCompleted()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).size()).isEqualTo(0);
    }

    public void testSkip() {

        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .skip(5)
                                      .afterMax(seconds(10))
                                      .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .skip(15)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetN(range(1, 10))
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .skip(0)
                                      .afterMax(seconds(10))
                                      .all()).isEqualTo(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testSplit() {

        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(getActivity()))
                                      .thenGetN(range(1, 3))
                                      .splitBy(2, sqrFunction())
                                      .afterMax(seconds(3))
                                      .all()).containsOnly(1L, 4L, 9L);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(getActivity()))
                                      .thenGetN(range(1, 3))
                                      .splitBy(Functions.<Integer>identity(), sqrFunction())
                                      .afterMax(seconds(3))
                                      .all()).containsOnly(1L, 4L, 9L);
        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(getActivity()))
                                      .then("test1", "test2", "test3")
                                      .splitBy(2, factory)
                                      .afterMax(seconds(3))
                                      .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(loaderFrom(getActivity()))
                                      .then("test1", "test2", "test3")
                                      .splitBy(Functions.<String>identity(), factory)
                                      .afterMax(seconds(3))
                                      .all()).containsOnly("TEST1", "TEST2", "TEST3");
    }

    public void testThen() {

        testThen(getActivity());
    }

    public void testThen2() {

        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then((String) null)
                                      .all()).containsOnly((String) null);
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then((String[]) null)
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then()
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then((List<String>) null)
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then(Collections.<String>emptyList())
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then("TEST2")
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then("TEST2", "TEST2")
                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .sync()
                                      .then(Collections.singletonList("TEST2"))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then((String) null)
                                      .afterMax(seconds(10))
                                      .all()).containsOnly((String) null);
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then((String[]) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then()
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then((List<String>) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then(Collections.<String>emptyList())
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then("TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then("TEST2", "TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .async()
                                      .then(Collections.singletonList("TEST2"))
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then((String) null)
                                      .afterMax(seconds(10))
                                      .all()).containsOnly((String) null);
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then((String[]) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then()
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then((List<String>) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then(Collections.<String>emptyList())
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then("TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then("TEST2", "TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .parallel()
                                      .then(Collections.singletonList("TEST2"))
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then((String) null)
                                      .afterMax(seconds(10))
                                      .all()).containsOnly((String) null);
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then((String[]) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then()
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then((List<String>) null)
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then(Collections.<String>emptyList())
                                      .afterMax(seconds(10))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then("TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then("TEST2", "TEST2")
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreamsCompat.streamOf("test1")
                                      .with(loaderFrom(getActivity()))
                                      .serial()
                                      .then(Collections.singletonList("TEST2"))
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("TEST2");
    }

    public void testThenNegativeCount() {

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .sync()
                               .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .thenGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .thenGetN(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .thenGetN(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().thenGetN(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().thenGetN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).sync().thenGet(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .async()
                               .thenGetN(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().thenGetN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().thenGet(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).async().thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .parallel()
                               .thenGetN(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf()
                               .with(loaderFrom(getActivity()))
                               .serial()
                               .thenGetN(3, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testTransform() {

        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(getActivity()))
                                      .transform(transformBiFunction())
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("TEST");
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(loaderFrom(getActivity()))
                                      .transformSimple(transformFunction())
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("TEST");
    }

    public void testTryCatch() {

        testTryCatch(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).tryCatchN(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).tryCatch(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testTryFinally() {

        testTryFinally(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testTryFinallyNullPointerError() {

        try {
            LoaderStreamsCompat.streamOf().with(loaderFrom(getActivity())).tryFinally(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class AbortInvocation extends TransformInvocation<Object, Object> {

        private AbortInvocation() {

            super(null);
        }

        public void onInput(final Object input, @NotNull final ResultChannel<Object> result) {

            result.abort(new UnsupportedOperationException());
        }
    }

    private static class RetryFunction implements Function<Object, StreamChannel<Object, String>> {

        private final FragmentActivity mActivity;

        private final Routine<Object, String> mRoutine;

        private RetryFunction(@NotNull final FragmentActivity activity,
                @NotNull final Routine<Object, String> routine) {

            mActivity = activity;
            mRoutine = routine;
        }

        private static StreamChannel<Object, String> apply(final Object o,
                @NotNull final FragmentActivity activity,
                @NotNull final Routine<Object, String> routine, @NotNull final int[] count) {

            return LoaderStreamsCompat.streamOf(o)
                                      .with(loaderFrom(activity))
                                      .map(routine)
                                      .tryCatchN(
                                              new BiConsumer<RoutineException,
                                                      InputChannel<String>>() {

                                                  public void accept(final RoutineException e,
                                                          final InputChannel<String> channel) {

                                                      if (++count[0] < 3) {
                                                          LoaderStreamsCompat.streamOf(o)
                                                                             .with(loaderFrom(
                                                                                     activity))
                                                                             .map(routine)
                                                                             .tryCatchN(this)
                                                                             .bind(channel);

                                                      } else {
                                                          throw e;
                                                      }
                                                  }
                                              });

        }

        public StreamChannel<Object, String> apply(final Object o) {

            final int[] count = {0};
            return apply(o, mActivity, mRoutine, count);
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

        private static int sCount;

        private final int mMaxCount;

        private ThrowException() {

            this(Integer.MAX_VALUE);
        }

        private ThrowException(final int maxCount) {

            mMaxCount = maxCount;
        }

        private static void reset() {

            sCount = 0;
        }

        @Override
        public void onInput(final Object input, @NotNull final ResultChannel<Object> result) throws
                Exception {

            if (sCount++ < mMaxCount) {
                throw new IllegalStateException();
            }

            result.pass(input);
        }
    }

    private static class UpperCase extends TransformInvocation<String, String> {

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
