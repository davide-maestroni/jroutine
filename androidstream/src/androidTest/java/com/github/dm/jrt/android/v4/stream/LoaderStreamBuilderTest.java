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

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamBuilderCompat
        .LoaderStreamConfigurationCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
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
import com.github.dm.jrt.stream.StreamBuilder;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android stream channel unit tests.
 * <p>
 * Created by davide-maestroni on 07/04/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderStreamBuilderTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderStreamBuilderTest() {
        super(TestActivity.class);
    }

    private static Function<Number, Double> sqrt() {
        return new Function<Number, Double>() {

            public Double apply(final Number number) {
                return Math.sqrt(number.doubleValue());
            }
        };
    }

    private static void testAppend(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .append("test2")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .append("test2", "test3")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .append(Arrays.asList("test2", "test3"))
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .append(JRoutineCore.io().of("test2", "test3"))
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
    }

    private static void testAppend2(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .appendGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .syncCall("test1")
                                             .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .appendMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(
                                                 final Channel<String, ?> resultChannel) {
                                             resultChannel.pass("TEST2");
                                         }
                                     })
                                     .syncCall("test1")
                                     .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .appendGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(3))
                                             .all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .appendMore(3, new Consumer<Channel<String, ?>>() {

                                         public void accept(
                                                 final Channel<String, ?> resultChannel) {
                                             resultChannel.pass("TEST2");
                                         }
                                     })
                                     .syncCall("test1")
                                     .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .async()
                                             .appendGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(3))
                                             .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .appendMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(
                                                 final Channel<String, ?> resultChannel) {
                                             resultChannel.pass("TEST2");
                                         }
                                     })
                                     .asyncCall("test1")
                                     .after(seconds(3))
                                     .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .async()
                                             .appendGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(3))
                                             .all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .appendMore(3, new Consumer<Channel<String, ?>>() {

                                         public void accept(
                                                 final Channel<String, ?> resultChannel) {
                                             resultChannel.pass("TEST2");
                                         }
                                     })
                                     .asyncCall("test1")
                                     .after(seconds(3))
                                     .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .parallel()
                                             .appendGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(3))
                                             .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .parallel()
                                     .appendMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(
                                                 final Channel<String, ?> resultChannel) {
                                             resultChannel.pass("TEST2");
                                         }
                                     })
                                     .asyncCall("test1")
                                     .after(seconds(3))
                                     .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .parallel()
                                             .appendGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(3))
                                             .all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineStreamLoaderCompat.<String>withStream().on(loaderFrom(activity))
                                                                  .parallel()
                                                                  .appendMore(3,
                                                                          new Consumer<Channel<String, ?>>() {

                                                                              public void accept(
                                                                                      final
                                                                                      Channel<String, ?> resultChannel) {
                                                                                  resultChannel
                                                                                          .pass(
                                                                                          "TEST2");
                                                                              }
                                                                          })
                                                                  .asyncCall("test1")
                                                                  .after(seconds(3))
                                                                  .all()).containsExactly("test1",
                "TEST2", "TEST2", "TEST2");
    }

    private static void testCollect(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<StringBuilder>withStream().on(loaderFrom(activity))
                                            .async()
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
                                            .after(seconds(10))
                                            .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<StringBuilder>withStream().on(loaderFrom(activity))
                                            .sync()
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
                                            .after(seconds(10))
                                            .all()).containsExactly("test1test2test3");
    }

    private static void testCollectCollection(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .collectInto(new Supplier<List<String>>() {

                                         public List<String> get() {
                                             return new ArrayList<String>();
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .next()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .collectInto(new Supplier<List<String>>() {

                                         public List<String> get() {
                                             return new ArrayList<String>();
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .next()).containsExactly("test1", "test2", "test3");
    }

    private static void testCollectSeed(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testConfiguration(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .parallel(1)
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {
                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel(1)
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {
                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
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
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        final Runner handlerRunner = AndroidRunners.handlerRunner(
                new HandlerThread(LoaderStreamBuilderTest.class.getName()));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .async()
                                             .andThenMore(range(1, 1000))
                                             .backoffOn(handlerRunner, 2,
                                                     Backoffs.linearDelay(seconds(10)))
                                             .map(Functions.<Number>identity())
                                             .on(loaderFrom(activity))
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
                                             .after(seconds(10))
                                             .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .async()
                                             .andThenMore(range(1, 1000))
                                             .backoffOn(handlerRunner, 2, 10, TimeUnit.SECONDS)
                                             .map(Functions.<Number>identity())
                                             .on(loaderFrom(activity))
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
                                             .after(seconds(10))
                                             .next()).isCloseTo(21, Offset.offset(0.1));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .async()
                                             .andThenMore(range(1, 1000))
                                             .backoffOn(handlerRunner, 2, seconds(10))
                                             .map(Functions.<Number>identity())
                                             .on(loaderFrom(activity))
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
                                             .after(seconds(10))
                                             .next()).isCloseTo(21, Offset.offset(0.1));
    }

    private static void testConsume(@NotNull final FragmentActivity activity) {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .onOutput(new Consumer<String>() {

                                         public void accept(final String s) {
                                             list.add(s);
                                         }
                                     })
                                     .syncCall("test1", "test2", "test3")
                                     .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .onOutput(new Consumer<String>() {

                                         public void accept(final String s) {
                                             list.add(s);
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    private static void testConsumeError(@NotNull final FragmentActivity activity) {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(activity))
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
                                      .syncCall("test")
                                      .next();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
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
                                             .syncCall("test")
                                             .all()).isEmpty();
    }

    private static void testFlatMap(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoaderCompat //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .filter(Functions
                                                                                  .<String>isNotNull())
                                                                          .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoaderCompat //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .filter(Functions
                                                                                  .<String>isNotNull())
                                                                          .syncCall(s);
                                         }
                                     })
                                     .asyncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .parallel()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoaderCompat //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .filter(Functions
                                                                                  .<String>isNotNull())
                                                                          .syncCall(s);
                                         }
                                     })
                                     .asyncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsOnly("test1", "test2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sequential()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoaderCompat //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .filter(Functions
                                                                                  .<String>isNotNull())
                                                                          .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsOnly("test1", "test2");
    }

    private static void testInvocationDeadlock(@NotNull final FragmentActivity activity) {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            JRoutineStreamLoaderCompat //
                    .<String>withStream().on(loaderFrom(activity))
                                         .invocationConfiguration()
                                         .withRunner(runner1)
                                         .applied()
                                         .map(new Function<String, Object>() {

                                             public Object apply(final String s) {
                                                 return JRoutineStreamLoaderCompat.withStream()
                                                                                  .on(loaderFrom(
                                                                                          activity))
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
                                                                                  .asyncCall()
                                                                                  .close()
                                                                                  .after(minutes(3))
                                                                                  .next();
                                             }
                                         })
                                         .asyncCall("tests")
                                         .after(minutes(3))
                                         .next();
            fail();

        } catch (final ExecutionDeadlockException ignored) {
        }
    }

    private static void testMapAllConsumer(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .mapAllMore(new BiConsumer<List<?
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
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .mapAllMore(
                                             new BiConsumer<List<? extends String>,
                                                     Channel<String, ?>>() {

                                                 public void accept(
                                                         final List<? extends String> strings,
                                                         final Channel<String, ?> result) {
                                                     final StringBuilder builder =
                                                             new StringBuilder();
                                                     for (final String string : strings) {
                                                         builder.append(string);
                                                     }

                                                     result.pass(builder.toString());
                                                 }
                                             })
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testMapAllFunction(@NotNull final FragmentActivity activity) {

        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testMapConsumer(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .mapMore(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    private static void testMapFunction(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {
                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private static void testOnComplete(@NotNull final FragmentActivity activity) {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .onComplete(new Action() {

                                                 public void perform() {
                                                     isComplete.set(true);
                                                 }
                                             })
                                             .asyncCall("test")
                                             .after(seconds(3))
                                             .all()).isEmpty();
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity)).map(new Function<String, String>() {

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

    private static void testOrElse(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse("est")
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse("est1", "est2")
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse(Arrays.asList("est1", "est2"))
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElseGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "est";
                                                 }
                                             })
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse("est")
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).containsExactly("est");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse("est1", "est2")
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).containsExactly("est1", "est2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .orElse(Arrays.asList("est1", "est2"))
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).containsExactly("est1", "est2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(seconds(10))
                                     .all()).containsExactly("est");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseMore(2, new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(seconds(10))
                                     .all()).containsExactly("est", "est");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity)).orElseGet(new Supplier<String>() {

                    public String get() {
                        return "est";
                    }
                }).asyncCall().close().after(seconds(10)).all()).containsExactly("est");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity)).orElseGet(2, new Supplier<String>() {

                    public String get() {
                        return "est";
                    }
                }).asyncCall().close().after(seconds(10)).all()).containsExactly("est", "est");
    }

    private static void testPeekComplete(@NotNull final FragmentActivity activity) {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity)).peekComplete(new Action() {

                    public void perform() {
                        isComplete.set(true);
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1", "test2", "test3");
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) throws Exception {
                                             throw new NoSuchElementException();
                                         }
                                     })
                                     .peekComplete(new Action() {

                                         public void perform() {
                                             isComplete.set(true);
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(3))
                                     .getError()).isExactlyInstanceOf(InvocationException.class);
        assertThat(isComplete.get()).isFalse();
    }

    private static void testPeekError(@NotNull final FragmentActivity activity) {
        final AtomicBoolean isError = new AtomicBoolean(false);
        final Channel<String, String> channel = JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .peekError(new Consumer<RoutineException>() {

                                         public void accept(final RoutineException e) {
                                             isError.set(true);
                                         }
                                     })
                                     .asyncCall();
        assertThat(channel.abort()).isTrue();
        assertThat(channel.after(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
        assertThat(isError.get()).isTrue();
        isError.set(false);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .peekError(new Consumer<RoutineException>() {

                                         public void accept(final RoutineException e) {
                                             isError.set(true);
                                         }
                                     })
                                     .asyncCall("test")
                                     .after(seconds(3))
                                     .all()).containsExactly("test");
        assertThat(isError.get()).isFalse();
    }

    private static void testPeekOutput(@NotNull final FragmentActivity activity) {
        final ArrayList<String> data = new ArrayList<String>();
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .peekOutput(new Consumer<String>() {

                                         public void accept(final String s) {
                                             data.add(s);
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
    }

    private static void testReduce(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .reduce(new BiFunction<String, String, String>() {

                                         public String apply(final String s, final String s2) {
                                             return s + s2;
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .reduce(new BiFunction<String, String, String>() {

                                         public String apply(final String s, final String s2) {
                                             return s + s2;
                                         }
                                     })
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testReduceSeed(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(activity))
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
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testThen(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .andThenMore(new Consumer<Channel<String, ?>>() {

                                                 public void accept(
                                                         final Channel<String, ?> resultChannel) {
                                                     resultChannel.pass("TEST2");
                                                 }
                                             })
                                             .syncCall("test1")
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .andThenGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .syncCall("test1")
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .andThenGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .async()
                                             .andThenMore(new Consumer<Channel<String, ?>>() {

                                                 public void accept(
                                                         final Channel<String, ?> resultChannel) {
                                                     resultChannel.pass("TEST2");
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .async()
                                             .andThenGet(new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .async()
                                             .andThenGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .parallel()
                                             .andThenMore(3, new Consumer<Channel<String, ?>>() {

                                                 public void accept(
                                                         final Channel<String, ?> resultChannel) {
                                                     resultChannel.pass("TEST2");
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .parallel()
                                             .andThenGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sequential()
                                             .andThenMore(3, new Consumer<Channel<String, ?>>() {

                                                 public void accept(
                                                         final Channel<String, ?> resultChannel) {
                                                     resultChannel.pass("TEST2");
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sequential()
                                             .andThenGet(3, new Supplier<String>() {

                                                 public String get() {
                                                     return "TEST2";
                                                 }
                                             })
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    private static void testTryCatch(@NotNull final FragmentActivity activity) {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .map(new Function<Object, Object>() {

                                                 public Object apply(final Object o) {
                                                     throw new NullPointerException();
                                                 }
                                             })
                                             .tryCatchMore(
                                                     new BiConsumer<RoutineException,
                                                             Channel<Object, ?>>() {

                                                         public void accept(
                                                                 final RoutineException e,
                                                                 final Channel<Object, ?> channel) {
                                                             channel.pass("exception");
                                                         }
                                                     })
                                             .syncCall("test")
                                             .next()).isEqualTo("exception");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .map(new Function<Object, Object>() {

                                                 public Object apply(final Object o) {
                                                     return o;
                                                 }
                                             })
                                             .tryCatchMore(
                                                     new BiConsumer<RoutineException,
                                                             Channel<Object, ?>>() {

                                                         public void accept(
                                                                 final RoutineException e,
                                                                 final Channel<Object, ?> channel) {
                                                             channel.pass("exception");
                                                         }
                                                     })
                                             .syncCall("test")
                                             .next()).isEqualTo("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
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
                                             .syncCall("test")
                                             .next()).isEqualTo("exception");
    }

    private static void testTryFinally(@NotNull final FragmentActivity activity) {
        final AtomicBoolean isRun = new AtomicBoolean(false);
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(activity))
                                      .sync()
                                      .map(new Function<Object, Object>() {

                                          public Object apply(final Object o) {
                                              throw new NullPointerException();
                                          }
                                      })
                                      .tryFinally(new Action() {

                                          public void perform() {
                                              isRun.set(true);
                                          }
                                      })
                                      .syncCall("test")
                                      .next();
        } catch (final RoutineException ignored) {
        }

        assertThat(isRun.getAndSet(false)).isTrue();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .sync()
                                             .map(new Function<Object, Object>() {

                                                 public Object apply(final Object o) {
                                                     return o;
                                                 }
                                             })
                                             .tryFinally(new Action() {

                                                 public void perform() {
                                                     isRun.set(true);
                                                 }
                                             })
                                             .syncCall("test")
                                             .next()).isEqualTo("test");
        assertThat(isRun.getAndSet(false)).isTrue();
    }

    @NotNull
    private static BiFunction<LoaderStreamConfigurationCompat, Function<Channel<?, String>,
            Channel<?, String>>, Function<Channel<?, String>, Channel<?, String>>>
    transformBiFunction() {
        return new BiFunction<LoaderStreamConfigurationCompat, Function<Channel<?, String>,
                Channel<?, String>>, Function<Channel<?, String>, Channel<?, String>>>() {

            public Function<Channel<?, String>, Channel<?, String>> apply(
                    final LoaderStreamConfigurationCompat configuration,
                    final Function<Channel<?, String>, Channel<?, String>> function) {
                assertThat(configuration.asLoaderConfiguration()).isEqualTo(
                        LoaderConfiguration.defaultConfiguration());
                assertThat(configuration.getLoaderContext()).isInstanceOf(
                        LoaderContextCompat.class);
                return Functions.decorate(function)
                                .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                                    public Channel<?, String> apply(
                                            final Channel<?, String> channel) {
                                        return JRoutineCore.with(new UpperCase())
                                                           .asyncCall(channel);
                                    }
                                });
            }
        };
    }

    @NotNull
    private static Function<Function<Channel<?, String>, Channel<?, String>>, Function<Channel<?,
            String>, Channel<?, String>>> transformFunction() {
        return new Function<Function<Channel<?, String>, Channel<?, String>>, Function<Channel<?,
                String>, Channel<?, String>>>() {

            public Function<Channel<?, String>, Channel<?, String>> apply(
                    final Function<Channel<?, String>, Channel<?, String>> function) {
                return Functions.decorate(function)
                                .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                                    public Channel<?, String> apply(
                                            final Channel<?, String> channel) {
                                        return JRoutineCore.with(new UpperCase())
                                                           .asyncCall(channel);
                                    }
                                });
            }
        };
    }

    public void testAppend() {
        testAppend(getActivity());
    }

    public void testAppend2() {
        testAppend2(getActivity());
    }

    public void testAsync() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async(null)
                                             .map(IdentityInvocation.factoryOf())
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .asyncMap(null)
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
    }

    public void testCache() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .loaderId(0)
                                             .cache(CacheStrategyType.CACHE)
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .hasCompleted()).isTrue();
        assertThat(JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                       .withId(0)
                                       .buildChannel()
                                       .after(seconds(10))
                                       .next()).isEqualTo("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .loaderId(0)
                                             .staleAfter(1, TimeUnit.MILLISECONDS)
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .hasCompleted()).isTrue();
        assertThat(JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                       .withId(0)
                                       .buildChannel()
                                       .after(seconds(10))
                                       .getError()).isExactlyInstanceOf(
                MissingLoaderException.class);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .async()
                                      .on(loaderFrom(getActivity()))
                                      .collectInto((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .sync()
                                      .on(loaderFrom(getActivity()))
                                      .collectInto((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .collect(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
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
            JRoutineStreamLoaderCompat.withStream().on(loaderFrom(getActivity())).onError(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {
        final Consumer<Object> consumer = null;
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testDelay() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .delay(1, TimeUnit.SECONDS)
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .delay(seconds(1))
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .delay(1, TimeUnit.SECONDS)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .delay(seconds(1))
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testFilter() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .filter(Functions.isNotNull())
                                             .asyncCall(null, "test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .filter(Functions.isNotNull())
                                             .asyncCall(null, "test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .filter(Functions.isNotNull())
                                             .syncCall(null, "test")
                                             .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .filter(Functions.isNotNull())
                                             .syncCall(null, "test")
                                             .after(seconds(10))
                                             .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream().parallel().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream().sequential().filter(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testFlatMapRetry() {
        final Routine<Object, String> routine =
                JRoutineCore.with(functionMapping(new Function<Object, String>() {

                    public String apply(final Object o) {
                        return o.toString();
                    }
                })).buildRoutine();
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .flatMap(new RetryFunction(getActivity(), routine))
                                      .asyncCall((Object) null)
                                      .after(seconds(10))
                                      .all();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    public void testFlatTransform() {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .flatLift(
                                             new Function<StreamBuilder<String, String>,
                                                     StreamBuilder<String, String>>() {

                                                 public StreamBuilder<String, String> apply(
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return builder.append("test2");
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .flatLift(
                                             new Function<StreamBuilder<String, String>,
                                                     LoaderStreamBuilderCompat<String, String>>() {

                                                 public LoaderStreamBuilderCompat<String, String>
                                                 apply(
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return ((LoaderStreamBuilderCompat<String,
                                                             String>) builder)
                                                             .append("test2");
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
    }

    public void testInvocationDeadlock() {
        testInvocationDeadlock(getActivity());
    }

    public void testInvocationMode() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .invocationMode(InvocationMode.ASYNC)
                                             .asyncCall("test1", "test2", "test3")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .invocationMode(InvocationMode.PARALLEL)
                                             .asyncCall("test1", "test2", "test3")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .invocationMode(InvocationMode.SYNC)
                                             .asyncCall("test1", "test2", "test3")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .invocationMode(InvocationMode.SEQUENTIAL)
                                             .asyncCall("test1", "test2", "test3")
                                             .after(seconds(10))
                                             .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .lag(1, TimeUnit.SECONDS)
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .lag(seconds(1))
                                             .asyncCall("test")
                                             .after(seconds(10))
                                             .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .lag(1, TimeUnit.SECONDS)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .lag(seconds(1))
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testLimit() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .limit(5)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .limit(0)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .limit(15)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEqualTo(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .limit(0)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
    }

    public void testMapAllConsumer() {
        testMapAllConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .mapAllMore(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .mapAll(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .mapMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapContextFactory() {
        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapContextFactoryIllegalState() {
        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        try {
            JRoutineStreamLoaderCompat.<String>withStream().async().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.<String>withStream().sync().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.<String>withStream().parallel().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.<String>withStream().sequential().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapContextFactoryNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFactory() {
        final InvocationFactory<String, String> factory =
                InvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFilter() {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(new UpperCase())
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .map(new UpperCase())
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .map((MappingInvocation<Object, Object>) null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapRoutine() {
        final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applied()
                                                            .buildRoutine();
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapRoutineBuilder() {
        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        final RoutineBuilder<String, String> loaderBuilder =
                JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                    .with(ContextInvocationFactory.factoryOf(UpperCase.class));
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().async()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().parallel()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().sync()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().sequential()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testOnComplete() {
        testOnComplete(getActivity());
    }

    public void testOrElse() {
        testOrElse(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .orElseMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .orElseMore(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream().on(loaderFrom(getActivity())).orElseGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .orElseGet(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testOrElseThrow() {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .orElseThrow(new IllegalStateException())
                                     .asyncCall("test")
                                     .after(seconds(3))
                                     .all()).containsExactly("test");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .orElseThrow(new IllegalStateException())
                                     .asyncCall()
                                     .close()
                                     .after(seconds(3))
                                     .getError()
                                     .getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    public void testPeekComplete() {
        testPeekComplete(getActivity());
    }

    public void testPeekError() {
        testPeekError(getActivity());
    }

    public void testPeekOutput() {
        testPeekOutput(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testPeekOutputNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .peekOutput(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .reduce(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .reduce(null);
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
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testRetry() {
        final AtomicInteger count1 = new AtomicInteger();
        try {
            JRoutineStreamLoaderCompat //
                    .<String>withStream().on(loaderFrom(getActivity()))
                                         .map(new UpperCase())
                                         .map(factoryOf(ThrowException.class, count1))
                                         .retry(2)
                                         .asyncCall("test")
                                         .after(seconds(10))
                                         .throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        final AtomicInteger count2 = new AtomicInteger();
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .map(new UpperCase())
                                     .map(factoryOf(ThrowException.class, count2, 1))
                                     .retry(2)
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStreamLoaderCompat //
                    .<String>withStream().on(loaderFrom(getActivity()))
                                         .map(new AbortInvocation())
                                         .map(factoryOf(ThrowException.class, count3))
                                         .retry(2)
                                         .asyncCall("test")
                                         .after(seconds(10))
                                         .throwError();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }

    public void testSkip() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .skip(5)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .skip(15)
                                             .syncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .sync()
                                             .andThenMore(range(1, 10))
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .skip(0)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(10))
                                             .all()).isEqualTo(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testSplit() {
        final LoaderStreamBuilderCompat<String, String> builder =
                JRoutineStreamLoaderCompat.<String>withStream().map(new UpperCase());
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallel(2, builder.buildFactory())
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallel(2, builder.buildContextFactory())
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallelBy(Functions.<String>identity(),
                                                     builder.buildFactory())
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallelBy(Functions.<String>identity(),
                                                     builder.buildContextFactory())
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        final RoutineBuilder<String, String> routineBuilder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallel(2, routineBuilder)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .andThen("test1", "test2", "test3")
                                             .parallelBy(Functions.<String>identity(),
                                                     routineBuilder)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        final LoaderRoutineBuilder<String, String> loaderBuilder =
                JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                    .with(builder.buildContextFactory());
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .andThen("test1", "test2", "test3")
                                             .parallel(2, loaderBuilder)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .andThen("test1", "test2", "test3")
                                             .parallelBy(Functions.<String>identity(),
                                                     loaderBuilder)
                                             .asyncCall()
                                             .close()
                                             .after(seconds(3))
                                             .all()).containsOnly("TEST1", "TEST2", "TEST3");
    }

    public void testStraight() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .straight()
                                             .andThenMore(range(1, 1000))
                                             .streamInvocationConfiguration()
                                             .withInputMaxSize(1)
                                             .withOutputMaxSize(1)
                                             .applied()
                                             .map(sqrt())
                                             .map(Operators.<Double>averageDouble())
                                             .syncCall()
                                             .close()
                                             .next()).isCloseTo(21, Offset.offset(0.1));
    }

    public void testThen() {
        testThen(getActivity());
    }

    public void testThen2() {
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen((String) null)
                                             .syncCall("test1")
                                             .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen((String[]) null)
                                             .syncCall("test1")
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen()
                                             .syncCall("test1")
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen((List<String>) null)
                                             .syncCall("test1")
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen(Collections.<String>emptyList())
                                             .syncCall("test1")
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen("TEST2")
                                             .syncCall("test1")
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen("TEST2", "TEST2")
                                             .syncCall("test1")
                                             .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sync()
                                             .andThen(Collections.singletonList("TEST2"))
                                             .syncCall("test1")
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen((String) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen((String[]) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen()
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen((List<String>) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen(Collections.<String>emptyList())
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen("TEST2")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen("TEST2", "TEST2")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .async()
                                             .andThen(Collections.singletonList("TEST2"))
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen((String) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen((String[]) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen()
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen((List<String>) null)
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen(Collections.<String>emptyList())
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen("TEST2")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen("TEST2", "TEST2")
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .parallel()
                                             .andThen(Collections.singletonList("TEST2"))
                                             .asyncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen((String) null)
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen((String[]) null)
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen()
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen((List<String>) null)
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen(Collections.<String>emptyList())
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).isEmpty();
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen("TEST2")
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen("TEST2", "TEST2")
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(getActivity()))
                                             .sequential()
                                             .andThen(Collections.singletonList("TEST2"))
                                             .syncCall("test1")
                                             .after(seconds(10))
                                             .all()).containsOnly("TEST2");
    }

    public void testThenNegativeCount() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .andThenGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .andThenMore(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .andThenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .andThenMore(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .andThenMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sync()
                                      .andThenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .andThenMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .andThenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .async()
                                      .andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .parallel()
                                      .andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .andThenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .sequential()
                                      .andThenMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testTransform() {
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .liftConfig(transformBiFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
        assertThat(JRoutineStreamLoaderCompat //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .lift(transformFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
    }

    public void testTryCatch() {
        testTryCatch(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        try {
            JRoutineStreamLoaderCompat.withStream()
                                      .on(loaderFrom(getActivity()))
                                      .tryCatchMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoaderCompat.withStream().on(loaderFrom(getActivity())).tryCatch(null);
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
            JRoutineStreamLoaderCompat.withStream().on(loaderFrom(getActivity())).tryFinally(null);
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

    private static class RetryFunction implements Function<Object, Channel<Object, String>> {

        private final FragmentActivity mActivity;

        private final Routine<Object, String> mRoutine;

        private RetryFunction(@NotNull final FragmentActivity activity,
                @NotNull final Routine<Object, String> routine) {
            mActivity = activity;
            mRoutine = routine;
        }

        private static Channel<Object, String> apply(final Object o,
                @NotNull final FragmentActivity activity,
                @NotNull final Routine<Object, String> routine, @NotNull final int[] count) {
            return JRoutineStreamLoaderCompat.withStream()
                                             .on(loaderFrom(activity))
                                             .map(routine)
                                             .tryCatchMore(
                                                     new BiConsumer<RoutineException,
                                                             Channel<String, ?>>() {

                                                         public void accept(
                                                                 final RoutineException e,
                                                                 final Channel<String, ?> channel) {
                                                             if (++count[0] < 3) {
                                                                 JRoutineStreamLoaderCompat
                                                                         .withStream()
                                                                                           .on(loaderFrom(
                                                                                                   activity))
                                                                                           .map(routine)
                                                                                           .tryCatchMore(
                                                                                                   this)
                                                                                           .asyncCall(
                                                                                                   o)
                                                                                           .bind(channel);

                                                             } else {
                                                                 throw e;
                                                             }
                                                         }
                                                     })
                                             .asyncCall(o);

        }

        public Channel<Object, String> apply(final Object o) {
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
