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

package com.github.dm.jrt.android.v11.stream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.HandlerThread;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.StreamLoaderRoutineBuilder.LoaderStreamConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
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
import com.github.dm.jrt.stream.StreamRoutineBuilder;

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

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.stream.StreamInputs.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android stream channel unit tests.
 * <p>
 * Created by davide-maestroni on 07/03/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class StreamLoaderRoutineBuilderTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public StreamLoaderRoutineBuilderTest() {
        super(TestActivity.class);
    }

    private static Function<Number, Double> sqrt() {
        return new Function<Number, Double>() {

            public Double apply(final Number number) {
                return Math.sqrt(number.doubleValue());
            }
        };
    }

    private static void testAppend(final Activity activity) {
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .append("test2")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .append("test2", "test3")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .append(Arrays.asList("test2", "test3"))
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .append(JRoutineCore.io().of("test2", "test3"))
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
    }

    private static void testAppend2(final Activity activity) {
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .appendGet(new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .syncCall("test1")
                                       .all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .sync()
                                                            .appendGetMore(
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .syncCall("test1")
                                                            .all()).containsExactly("test1",
                "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .appendGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(3))
                                       .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .sync()
                                                            .appendGetMore(3,
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .syncCall("test1")
                                                            .all()).containsExactly("test1",
                "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
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
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .async()
                                                            .appendGetMore(
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .asyncCall("test1")
                                                            .after(seconds(3))
                                                            .all()).containsExactly("test1",
                "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .async()
                                       .appendGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(3))
                                       .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .async()
                                                            .appendGetMore(3,
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .asyncCall("test1")
                                                            .after(seconds(3))
                                                            .all()).containsExactly("test1",
                "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
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
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .parallel()
                                                            .appendGetMore(
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .asyncCall("test1")
                                                            .after(seconds(3))
                                                            .all()).containsExactly("test1",
                "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .parallel()
                                       .appendGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(3))
                                       .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.<String>withStream().on(loaderFrom(activity))
                                                            .parallel()
                                                            .appendGetMore(3,
                                                                    new Consumer<Channel<String,
                                                                            ?>>() {

                                                                        public void accept(
                                                                                final
                                                                                Channel<String,
                                                                                        ?>
                                                                                        resultChannel) {
                                                                            resultChannel.pass(
                                                                                    "TEST2");
                                                                        }
                                                                    })
                                                            .asyncCall("test1")
                                                            .after(seconds(3))
                                                            .all()).containsExactly("test1",
                "TEST2", "TEST2", "TEST2");
    }

    private static void testCollect(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testCollectCollection(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testCollectSeed(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testConfiguration(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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
                new HandlerThread(StreamLoaderRoutineBuilderTest.class.getName()));
        assertThat(JRoutineStreamLoader.withStream()
                                       .async()
                                       .thenGetMore(range(1, 1000))
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
        assertThat(JRoutineStreamLoader.withStream()
                                       .async()
                                       .thenGetMore(range(1, 1000))
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
        assertThat(JRoutineStreamLoader.withStream()
                                       .async()
                                       .thenGetMore(range(1, 1000))
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

    private static void testConsume(@NotNull final Activity activity) {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testConsumeError(@NotNull final Activity activity) {
        try {
            JRoutineStreamLoader.withStream()
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

        assertThat(JRoutineStreamLoader.withStream()
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

    private static void testFlatMap(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .filter(Functions
                                                                                  .<String>isNotNull())
                                                                          .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .parallel()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sequential()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
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

    private static void testInvocationDeadlock(@NotNull final Activity activity) {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            JRoutineStreamLoader //
                    .<String>withStream().on(loaderFrom(activity))
                                         .invocationConfiguration()
                                         .withRunner(runner1)
                                         .applied()
                                         .map(new Function<String, Object>() {

                                             public Object apply(final String s) {
                                                 return JRoutineStreamLoader.withStream()
                                                                            .on(loaderFrom(
                                                                                    activity))
                                                                            .invocationConfiguration()
                                                                            .withRunner(runner1)
                                                                            .applied()
                                                                            .map(Functions
                                                                                    .identity())
                                                                            .invocationConfiguration()
                                                                            .withRunner(runner2)
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

    private static void testMapAllConsumer(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testMapAllFunction(@NotNull final Activity activity) {

        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testMapConsumer(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testMapFunction(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
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
    private static void testOnComplete(final Activity activity) {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(
                JRoutineStreamLoader.withStream().on(loaderFrom(activity)).onComplete(new Action() {

                    public void perform() {
                        isComplete.set(true);
                    }
                }).asyncCall("test").after(seconds(3)).all()).isEmpty();
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStreamLoader //
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

    private static void testOrElse(final Activity activity) {
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse("est")
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse("est1", "est2")
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse(Arrays.asList("est1", "est2"))
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseGetMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElseGet(new Supplier<String>() {

                                           public String get() {
                                               return "est";
                                           }
                                       })
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse("est")
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).containsExactly("est");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse("est1", "est2")
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).containsExactly("est1", "est2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .orElse(Arrays.asList("est1", "est2"))
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).containsExactly("est1", "est2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseGetMore(new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(seconds(10))
                                     .all()).containsExactly("est");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .orElseGetMore(2, new Consumer<Channel<String, ?>>() {

                                         public void accept(final Channel<String, ?> result) {
                                             result.pass("est");
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(seconds(10))
                                     .all()).containsExactly("est", "est");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity)).orElseGet(new Supplier<String>() {

                    public String get() {
                        return "est";
                    }
                }).asyncCall().close().after(seconds(10)).all()).containsExactly("est");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity)).orElseGet(2, new Supplier<String>() {

                    public String get() {
                        return "est";
                    }
                }).asyncCall().close().after(seconds(10)).all()).containsExactly("est", "est");
    }

    private static void testPeekComplete(final Activity activity) {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity)).peekComplete(new Action() {

                    public void perform() {
                        isComplete.set(true);
                    }
                }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1", "test2", "test3");
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        assertThat(JRoutineStreamLoader //
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

    private static void testPeekError(final Activity activity) {
        final AtomicBoolean isError = new AtomicBoolean(false);
        final Channel<String, String> channel = JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testPeekOutput(@NotNull final Activity activity) {
        final ArrayList<String> data = new ArrayList<String>();
        assertThat(JRoutineStreamLoader //
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

    private static void testReduce(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testReduceSeed(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
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

    private static void testThen(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .thenGetMore(new Consumer<Channel<String, ?>>() {

                                           public void accept(
                                                   final Channel<String, ?> resultChannel) {
                                               resultChannel.pass("TEST2");
                                           }
                                       })
                                       .syncCall("test1")
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .thenGet(new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .syncCall("test1")
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .thenGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .async()
                                       .thenGetMore(new Consumer<Channel<String, ?>>() {

                                           public void accept(
                                                   final Channel<String, ?> resultChannel) {
                                               resultChannel.pass("TEST2");
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .async()
                                       .thenGet(new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .async()
                                       .thenGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .parallel()
                                       .thenGetMore(3, new Consumer<Channel<String, ?>>() {

                                           public void accept(
                                                   final Channel<String, ?> resultChannel) {
                                               resultChannel.pass("TEST2");
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .parallel()
                                       .thenGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sequential()
                                       .thenGetMore(3, new Consumer<Channel<String, ?>>() {

                                           public void accept(
                                                   final Channel<String, ?> resultChannel) {
                                               resultChannel.pass("TEST2");
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sequential()
                                       .thenGet(3, new Supplier<String>() {

                                           public String get() {
                                               return "TEST2";
                                           }
                                       })
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    private static void testTryCatch(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .map(new Function<Object, Object>() {

                                           public Object apply(final Object o) {
                                               throw new NullPointerException();
                                           }
                                       })
                                       .tryCatchMore(
                                               new BiConsumer<RoutineException, Channel<Object,
                                                       ?>>() {

                                                   public void accept(final RoutineException e,
                                                           final Channel<Object, ?> channel) {
                                                       channel.pass("exception");
                                                   }
                                               })
                                       .syncCall("test")
                                       .next()).isEqualTo("exception");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .sync()
                                       .map(new Function<Object, Object>() {

                                           public Object apply(final Object o) {
                                               return o;
                                           }
                                       })
                                       .tryCatchMore(
                                               new BiConsumer<RoutineException, Channel<Object,
                                                       ?>>() {

                                                   public void accept(final RoutineException e,
                                                           final Channel<Object, ?> channel) {
                                                       channel.pass("exception");
                                                   }
                                               })
                                       .syncCall("test")
                                       .next()).isEqualTo("test");
        assertThat(JRoutineStreamLoader.withStream()
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

    private static void testTryFinally(@NotNull final Activity activity) {
        final AtomicBoolean isRun = new AtomicBoolean(false);
        try {
            JRoutineStreamLoader.withStream()
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
        assertThat(JRoutineStreamLoader.withStream()
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
    private static BiFunction<LoaderStreamConfiguration, Function<Channel<?, String>, Channel<?,
            String>>, Function<Channel<?, String>, Channel<?, String>>> transformBiFunction() {
        return new BiFunction<LoaderStreamConfiguration, Function<Channel<?, String>, Channel<?,
                String>>, Function<Channel<?, String>, Channel<?, String>>>() {

            public Function<Channel<?, String>, Channel<?, String>> apply(
                    final LoaderStreamConfiguration configuration,
                    final Function<Channel<?, String>, Channel<?, String>> function) {
                assertThat(configuration.asLoaderConfiguration()).isEqualTo(
                        LoaderConfiguration.defaultConfiguration());
                assertThat(configuration.getLoaderContext()).isInstanceOf(LoaderContext.class);
                return wrap(function).andThen(
                        new Function<Channel<?, String>, Channel<?, String>>() {

                            public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase()).asyncCall(channel);
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
                return wrap(function).andThen(
                        new Function<Channel<?, String>, Channel<?, String>>() {

                            public Channel<?, String> apply(final Channel<?, String> channel) {
                                return JRoutineCore.with(new UpperCase()).asyncCall(channel);
                            }
                        });
            }
        };
    }

    public void testAppend() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testAppend(getActivity());
    }

    public void testAppend2() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testAppend2(getActivity());
    }

    public void testCollect() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollect(getActivity());
    }

    public void testCollectCollection() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollectCollection(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .async()
                                .on(loaderFrom(getActivity()))
                                .collectInto((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .sync()
                                .on(loaderFrom(getActivity()))
                                .collectInto((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().collect(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testCollectSeed() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollectSeed(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectSeedNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testConfiguration() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testConfiguration(getActivity());
    }

    public void testConsume() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testConsume(getActivity());
    }

    public void testConsumeError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testConsumeError(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testConsumeErrorNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).onError(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Consumer<Object> consumer = null;
        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testDelay() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .delay(1, TimeUnit.SECONDS)
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .delay(seconds(1))
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .delay(1, TimeUnit.SECONDS)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .delay(seconds(1))
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testFilter() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .filter(Functions.isNotNull())
                                       .asyncCall(null, "test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .filter(Functions.isNotNull())
                                       .asyncCall(null, "test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .filter(Functions.isNotNull())
                                       .syncCall(null, "test")
                                       .all()).containsExactly("test");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .filter(Functions.isNotNull())
                                       .syncCall(null, "test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().parallel().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().sequential().filter(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testFlatMap() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testFlatMap(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFlatMapNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testFlatMapRetry() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<Object, String> routine =
                JRoutineCore.with(functionMapping(new Function<Object, String>() {

                    public String apply(final Object o) {
                        return o.toString();
                    }
                })).buildRoutine();
        try {
            JRoutineStreamLoader.withStream()
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .flatLift(
                                             new Function<StreamRoutineBuilder<String, String>,
                                                     StreamRoutineBuilder<String, String>>() {

                                                 public StreamRoutineBuilder<String, String> apply(
                                                         final StreamRoutineBuilder<String,
                                                                 String> builder) {
                                                     return builder.append("test2");
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .flatLift(
                                             new Function<StreamRoutineBuilder<String, String>,
                                                     StreamLoaderRoutineBuilder<String, String>>() {

                                                 public StreamLoaderRoutineBuilder<String,
                                                         String> apply(
                                                         final StreamRoutineBuilder<String,
                                                                 String> builder) {
                                                     return ((StreamLoaderRoutineBuilder<String,
                                                             String>) builder)
                                                             .append("test2");
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
    }

    public void testInvocationDeadlock() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testInvocationDeadlock(getActivity());
    }

    public void testInvocationMode() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.ASYNC)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.PARALLEL)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.SYNC)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.SEQUENTIAL)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLag() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .lag(1, TimeUnit.SECONDS)
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .lag(seconds(1))
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .lag(1, TimeUnit.SECONDS)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .lag(seconds(1))
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testLimit() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .limit(5)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .limit(0)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .limit(15)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEqualTo(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .limit(0)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
    }

    public void testMapAllConsumer() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapAllConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .mapAllMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapAllFunction() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapAllFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllFunctionNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().mapAll(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapConsumer() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().mapMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapContextFactory() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .sequential()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapContextFactoryIllegalState() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        try {
            JRoutineStreamLoader.<String>withStream().async().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().sync().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().parallel().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().sequential().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapContextFactoryNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFactory() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final InvocationFactory<String, String> factory =
                InvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFilter() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted(OrderType.BY_CALL)
                                     .parallel()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(new UpperCase())
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFunction() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapRoutine() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applied()
                                                            .buildRoutine();
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapRoutineBuilder() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        final RoutineBuilder<String, String> loaderBuilder =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(ContextInvocationFactory.factoryOf(UpperCase.class));
        assertThat(JRoutineStreamLoader //
                .<String>withStream().async()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().parallel()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().sync()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().sequential()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testOnComplete() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testOnComplete(getActivity());
    }

    public void testOrElse() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testOrElse(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testOrElseNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).orElseGetMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).orElseGetMore(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).orElseGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).orElseGet(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testPeekComplete() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testPeekComplete(getActivity());
    }

    public void testPeekError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testPeekError(getActivity());
    }

    public void testPeekOutput() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testPeekOutput(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testPeekOutputNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .peekOutput(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testReduce() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testReduce(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testReduceNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testReduceSeed() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testReduceSeed(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testReduceSeedNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testRetry() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final AtomicInteger count1 = new AtomicInteger();
        try {
            JRoutineStreamLoader //
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
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .map(new UpperCase())
                                     .map(factoryOf(ThrowException.class, count2, 1))
                                     .retry(2)
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST");

        final AtomicInteger count3 = new AtomicInteger();
        try {
            JRoutineStreamLoader //
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .skip(5)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .skip(15)
                                       .syncCall()
                                       .close()
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .sync()
                                       .thenGetMore(range(1, 10))
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .then("test1", "test2", "test3")
                                       .parallel(2, factory)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .then("test1", "test2", "test3")
                                       .parallelBy(Functions.<String>identity(), factory)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .then("test1", "test2", "test3")
                                       .parallel(2, builder)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .then("test1", "test2", "test3")
                                       .parallelBy(Functions.<String>identity(), builder)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
        final LoaderRoutineBuilder<String, String> loaderBuilder =
                JRoutineLoader.on(loaderFrom(getActivity())).with(factory);
        assertThat(JRoutineStreamLoader.withStream()
                                       .then("test1", "test2", "test3")
                                       .parallel(2, loaderBuilder)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .then("test1", "test2", "test3")
                                       .parallelBy(Functions.<String>identity(), loaderBuilder)
                                       .asyncCall()
                                       .close()
                                       .after(seconds(3))
                                       .all()).containsOnly("TEST1", "TEST2", "TEST3");
    }

    public void testStraight() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .straight()
                                       .thenGetMore(range(1, 1000))
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
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testThen(getActivity());
    }

    public void testThen2() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then((String) null)
                                       .syncCall("test1")
                                       .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then((String[]) null)
                                       .syncCall("test1")
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then()
                                       .syncCall("test1")
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then((List<String>) null)
                                       .syncCall("test1")
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then(Collections.<String>emptyList())
                                       .syncCall("test1")
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then("TEST2")
                                       .syncCall("test1")
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then("TEST2", "TEST2")
                                       .syncCall("test1")
                                       .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sync()
                                       .then(Collections.singletonList("TEST2"))
                                       .syncCall("test1")
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then((String) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then((String[]) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then()
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then((List<String>) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then(Collections.<String>emptyList())
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then("TEST2")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then("TEST2", "TEST2")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .async()
                                       .then(Collections.singletonList("TEST2"))
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then((String) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then((String[]) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then()
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then((List<String>) null)
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then(Collections.<String>emptyList())
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then("TEST2")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then("TEST2", "TEST2")
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .parallel()
                                       .then(Collections.singletonList("TEST2"))
                                       .asyncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then((String) null)
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly((String) null);
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then((String[]) null)
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then()
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then((List<String>) null)
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then(Collections.<String>emptyList())
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).isEmpty();
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then("TEST2")
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then("TEST2", "TEST2")
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2", "TEST2");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .sequential()
                                       .then(Collections.singletonList("TEST2"))
                                       .syncCall("test1")
                                       .after(seconds(10))
                                       .all()).containsOnly("TEST2");
    }

    public void testThenNegativeCount() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .thenGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .thenGetMore(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .thenGetMore(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testThenNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .thenGetMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .thenGetMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().thenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .thenGetMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .thenGetMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().thenGet(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .thenGetMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .thenGet(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .thenGetMore(3, null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testTransform() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .liftConfig(transformBiFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .lift(transformFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
    }

    public void testTryCatch() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testTryCatch(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).tryCatchMore(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).tryCatch(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testTryFinally() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testTryFinally(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testTryFinallyNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).tryFinally(null);
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

        private final Activity mActivity;

        private final Routine<Object, String> mRoutine;

        private RetryFunction(@NotNull final Activity activity,
                @NotNull final Routine<Object, String> routine) {
            mActivity = activity;
            mRoutine = routine;
        }

        private static Channel<Object, String> apply(final Object o,
                @NotNull final Activity activity, @NotNull final Routine<Object, String> routine,
                @NotNull final int[] count) {
            return JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .map(routine)
                                       .tryCatchMore(
                                               new BiConsumer<RoutineException, Channel<String,
                                                       ?>>() {

                                                   public void accept(final RoutineException e,
                                                           final Channel<String, ?> channel) {
                                                       if (++count[0] < 3) {
                                                           JRoutineStreamLoader.withStream()
                                                                               .on(loaderFrom(
                                                                                       activity))
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
